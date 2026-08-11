package com.brightpath.sanad.feature.sessions

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.brightpath.sanad.R
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import livekit.org.webrtc.EglBase
import livekit.org.webrtc.SurfaceViewRenderer
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class LiveKitCallFragment : Fragment() {
    private val tag = "LiveKitCall"
    private lateinit var loading: View
    private lateinit var error: View
    private lateinit var content: View
    private lateinit var tvError: TextView
    private lateinit var tvStatus: TextView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnMic: MaterialButton
    private lateinit var btnCam: MaterialButton
    private lateinit var btnSwitch: MaterialButton
    private lateinit var btnEnd: MaterialButton
    private lateinit var btnRetry: MaterialButton
    private lateinit var gridRemote: GridLayout
    private lateinit var localRenderer: SurfaceViewRenderer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val remoteRenderers = linkedMapOf<String, SurfaceViewRenderer>()
    private var eglBase: EglBase? = null
    private var room: Room? = null
    private lateinit var repo: LiveKitRepository
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private var sessionId = -1
    private var groupId = -1
    private var title: String? = null
    private var micEnabled = true
    private var camEnabled = true
    private var wantsVideo = true
    private var sessionEndsAt: Long = -1L
    private val endHandler = Handler(Looper.getMainLooper())
    private val endRunnable = Runnable { endCall() }
    private var localRendererInitialized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_livekit_call, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val ok = if (wantsVideo) {
                result[Manifest.permission.CAMERA] == true && result[Manifest.permission.RECORD_AUDIO] == true
            } else {
                result[Manifest.permission.RECORD_AUDIO] == true
            }
            if (ok) connect() else showError(getString(R.string.call_permissions_needed))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loading = view.findViewById(R.id.liveLoading)
        error = view.findViewById(R.id.liveError)
        content = view.findViewById(R.id.liveContent)
        tvError = view.findViewById(R.id.tvLiveError)
        tvStatus = view.findViewById(R.id.tvLiveStatus)
        toolbar = view.findViewById(R.id.toolbarLive)
        btnMic = view.findViewById(R.id.btnToggleMic)
        btnCam = view.findViewById(R.id.btnToggleCam)
        btnSwitch = view.findViewById(R.id.btnSwitchCam)
        btnEnd = view.findViewById(R.id.btnEndCall)
        btnRetry = view.findViewById(R.id.btnLiveRetry)
        gridRemote = view.findViewById(R.id.gridRemote)
        localRenderer = view.findViewById(R.id.viewLocal)

        repo = LiveKitRepository(requireContext())
        sessionId = arguments?.getInt("sessionId", -1) ?: -1
        groupId = arguments?.getInt("groupId", -1) ?: -1
        title = arguments?.getString("title")
        wantsVideo = arguments?.getBoolean("videoEnabled", true) ?: true
        camEnabled = wantsVideo
        sessionEndsAt = parseEndAt(arguments?.getString("sessionEndsAt"))

        toolbar.setNavigationOnClickListener { endCall() }
        if (!title.isNullOrBlank()) toolbar.title = title

        btnRetry.setOnClickListener { connect() }
        btnEnd.setOnClickListener { endCall() }
        btnMic.setOnClickListener { toggleMic() }
        btnCam.setOnClickListener { toggleCam() }
        btnSwitch.setOnClickListener { switchCamera() }

        if (!wantsVideo) {
            btnCam.visibility = View.GONE
            btnSwitch.visibility = View.GONE
            localRenderer.visibility = View.GONE
        }

        ensurePermissions()
    }

    private fun ensurePermissions() {
        val cam = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (wantsVideo) {
            if (cam && mic) connect()
            else permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        } else {
            if (mic) connect()
            else permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun connect() {
        if (sessionId <= 0 && groupId <= 0) {
            showError(getString(R.string.call_error))
            return
        }
        show(loading)
        tvStatus.setText(R.string.call_connecting)
        if (sessionId > 0) {
            repo.fetchSessionToken(sessionId, tokenCb)
        } else {
            repo.fetchGroupToken(groupId, tokenCb)
        }
    }

    private val tokenCb = object : LiveKitRepository.TokenCb {
        override fun ok(resp: LiveKitRepository.TokenResponse) {
            if (resp.token.isNullOrBlank() || resp.url.isNullOrBlank()) {
                Log.e(tag, "token response missing fields: token=${resp.token?.length ?: 0} url=${resp.url}")
                showError(getString(R.string.call_error))
                return
            }
            Log.i(tag, "token ok: url=${resp.url} tokenLen=${resp.token.length} room=${resp.room}")
            connectRoom(resp.url, resp.token)
        }

        override fun err(t: Throwable) {
            Log.e(tag, "token fetch failed", t)
            showError(errorMessage(t))
        }
    }

    private fun connectRoom(url: String, token: String) {
        try {
            eglBase = EglBase.create()
            if (localRendererInitialized) {
                localRenderer.release()
                localRendererInitialized = false
            }
            // Room.initVideoRenderer will initialize the SurfaceViewRenderer.
            localRenderer.setZOrderMediaOverlay(true)
            LiveKit.init(requireContext().applicationContext)
            room = LiveKit.create(requireContext())
            room?.initVideoRenderer(localRenderer)
            localRendererInitialized = true
            configureAudioSession()

            scope.launch {
                val r = room ?: return@launch
                // Collect events before connect so Connected / TrackSubscribed are not missed.
                val eventsJob = launch {
                    r.events.events.collect { event: RoomEvent ->
                        when (event) {
                            is RoomEvent.Connected -> {
                                show(content)
                                enableLocalTracks()
                                scheduleEndIfNeeded()
                            }
                            is RoomEvent.Disconnected -> {
                                Log.e(tag, "room disconnected: reason=${event.reason}", event.error)
                                showError(errorMessage(event.error))
                            }
                            is RoomEvent.FailedToConnect -> {
                                Log.e(tag, "room failed to connect", event.error)
                                showError(errorMessage(event.error))
                            }
                            is RoomEvent.TrackSubscribed -> {
                                val track = event.track
                                if (track is VideoTrack) {
                                    attachRemoteVideo(participantKey(event.participant), track)
                                }
                            }
                            is RoomEvent.TrackUnsubscribed -> {
                                val track = event.track
                                if (track is VideoTrack) {
                                    removeRemoteRenderer(participantKey(event.participant))
                                }
                            }
                            is RoomEvent.ParticipantDisconnected -> {
                                removeRemoteRenderer(participantKey(event.participant))
                            }
                            else -> Unit
                        }
                    }
                }
                try {
                    r.connect(url, token)
                    // Ensure UI/tracks update even if Connected event raced.
                    show(content)
                    enableLocalTracks()
                    scheduleEndIfNeeded()
                } catch (e: Exception) {
                    eventsJob.cancel()
                    Log.e(tag, "room connect failed", e)
                    showError(errorMessage(e))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "connectRoom failed", e)
            showError(errorMessage(e))
        }
    }

    private fun configureAudioSession() {
        try {
            val am = requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            am.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
            am.isSpeakerphoneOn = true
        } catch (e: Exception) {
            Log.w(tag, "audio session configure failed", e)
        }
    }

    private fun enableLocalTracks() {
        val r = room ?: return
        scope.launch {
            r.localParticipant.setMicrophoneEnabled(true)
            r.localParticipant.setCameraEnabled(camEnabled)
            if (camEnabled) {
                bindLocalVideo()
            }
            micEnabled = true
            updateButtons()
        }
    }

    private fun bindLocalVideo() {
        val r = room ?: return
        val pub = r.localParticipant.getTrackPublication(Track.Source.CAMERA)
        val track = pub?.track
        if (track is VideoTrack) {
            track.addRenderer(localRenderer)
        }
    }

    private fun attachRemoteVideo(key: String, track: VideoTrack) {
        if (remoteRenderers.containsKey(key)) return
        val renderer = SurfaceViewRenderer(requireContext())
        renderer.init(eglBase?.eglBaseContext, null)
        renderer.setZOrderMediaOverlay(false)
        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = 0
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        renderer.layoutParams = params
        gridRemote.addView(renderer)
        track.addRenderer(renderer)
        remoteRenderers[key] = renderer
    }

    private fun removeRemoteRenderer(key: String) {
        val renderer = remoteRenderers.remove(key) ?: return
        renderer.release()
        gridRemote.removeView(renderer)
    }

    private fun participantKey(participant: Participant): String {
        val identity = participant.identity?.toString()
        if (!identity.isNullOrBlank()) return identity
        return participant.sid.toString()
    }

    private fun toggleMic() {
        val r = room ?: return
        micEnabled = !micEnabled
        scope.launch {
            r.localParticipant.setMicrophoneEnabled(micEnabled)
            updateButtons()
        }
    }

    private fun toggleCam() {
        if (!wantsVideo) return
        val r = room ?: return
        camEnabled = !camEnabled
        scope.launch {
            r.localParticipant.setCameraEnabled(camEnabled)
            localRenderer.visibility = if (camEnabled) View.VISIBLE else View.GONE
            updateButtons()
            if (camEnabled) {
                bindLocalVideo()
            }
        }
    }

    private fun switchCamera() {
        val r = room ?: return
        val pub = r.localParticipant.getTrackPublication(Track.Source.CAMERA)
        val track = pub?.track
        if (track is LocalVideoTrack) {
            track.switchCamera()
        } else {
            Toast.makeText(requireContext(), R.string.call_switch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateButtons() {
        btnMic.setIconResource(if (micEnabled) R.drawable.ic_mic else R.drawable.ic_mic_off)
        if (wantsVideo) {
            btnCam.setIconResource(if (camEnabled) R.drawable.ic_cam else R.drawable.ic_cam_off)
            btnSwitch.visibility = if (camEnabled) View.VISIBLE else View.GONE
        }
    }

    private fun endCall() {
        room?.disconnect()
        room?.release()
        NavHostFragment.findNavController(this).popBackStack()
    }

    private fun scheduleEndIfNeeded() {
        if (sessionEndsAt <= 0L) return
        val delay = sessionEndsAt - System.currentTimeMillis()
        if (delay <= 0L) {
            endCall()
            return
        }
        endHandler.removeCallbacks(endRunnable)
        endHandler.postDelayed(endRunnable, delay)
    }

    private fun parseEndAt(raw: String?): Long {
        if (raw.isNullOrBlank()) return -1L
        return try {
            OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            -1L
        }
    }

    private fun show(target: View) {
        loading.visibility = if (target == loading) View.VISIBLE else View.GONE
        error.visibility = if (target == error) View.VISIBLE else View.GONE
        content.visibility = if (target == content) View.VISIBLE else View.GONE
    }

    private fun showError(msg: String) {
        tvError.text = msg
        show(error)
    }

    private fun errorMessage(t: Throwable?): String {
        val debuggable = (requireContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) return getString(R.string.call_error)
        val base = getString(R.string.call_error)
        val detail = t?.message?.takeIf { it.isNotBlank() } ?: t?.javaClass?.simpleName ?: "unknown"
        return "$base ($detail)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        endHandler.removeCallbacks(endRunnable)
        room?.disconnect()
        room?.release()
        room = null
        if (localRendererInitialized) {
            localRenderer.release()
            localRendererInitialized = false
        }
        remoteRenderers.values.forEach { it.release() }
        remoteRenderers.clear()
        eglBase?.release()
        eglBase = null
    }
}
