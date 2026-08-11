package com.brightpath.sanad.feature.library;

import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.LibraryRepository;
import com.brightpath.sanad.data.LocaleHelper;
import com.bumptech.glide.Glide;

import java.util.Map;

public class ArticleFragment extends Fragment {
    private ArticleViewModel vm;
    @Nullable private VideoView videoPlayer;
    @Nullable private String boundVideoUrl;
    private int savedPositionMs;
    @Nullable private com.google.android.material.button.MaterialButton btnFavorite;
    @Nullable private ImageView imgCover;
    @Nullable private ImageView imgCoverFallback;
    private boolean favorited;
    private int articleId = -1;
    private LibraryRepository libraryRepo;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_article, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        articleId = getArguments() != null ? getArguments().getInt("articleId", -1) : -1;

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvAuthor = v.findViewById(R.id.tvAuthor);
        TextView tvBody  = v.findViewById(R.id.tvBody);
        videoPlayer = v.findViewById(R.id.videoPlayer);
        btnFavorite = v.findViewById(R.id.btnFavorite);
        imgCover = v.findViewById(R.id.imgCover);
        imgCoverFallback = v.findViewById(R.id.imgCoverFallback);
        libraryRepo = new LibraryRepository(requireContext());

        if (btnFavorite != null && articleId > 0) {
            btnFavorite.setOnClickListener(x -> toggleFavorite());
        }

        vm = new ViewModelProvider(this).get(ArticleViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), st -> {
            if (st==null || st.loading) return;
            if (st.error!=null){ tvTitle.setText(R.string.error_load_failed); tvBody.setText(st.error); return; }
            tvTitle.setText(pick(st.article.title));
            String authorLine = buildAuthorLine(st.article.author_name, st.article.author_title);
            if (authorLine.isEmpty()) {
                tvAuthor.setVisibility(View.GONE);
            } else {
                tvAuthor.setVisibility(View.VISIBLE);
                tvAuthor.setText(authorLine);
            }
            tvBody.setText(pick(st.article.body));
            favorited = st.article.favorited != null && st.article.favorited;
            updateFavoriteButton();
            bindCover(st.article.image != null && !st.article.image.isEmpty() ? st.article.image : st.article.thumbnail);
            bindVideo(st.article.video_url);
        });
        if (articleId > 0) vm.load(articleId);
    }

    private void bindCover(@Nullable String raw) {
        if (imgCover == null) return;
        String url = AppConfig.storageUrl(raw);
        if (url == null || url.isEmpty()) {
            imgCover.setImageDrawable(null);
            if (imgCoverFallback != null) imgCoverFallback.setVisibility(View.VISIBLE);
            return;
        }
        if (imgCoverFallback != null) imgCoverFallback.setVisibility(View.GONE);
        Glide.with(imgCover.getContext())
                .load(url)
                .centerCrop()
                .into(imgCover);
    }

    private void toggleFavorite() {
        if (articleId <= 0 || libraryRepo == null) return;
        LibraryRepository.FavoriteListener cb = new LibraryRepository.FavoriteListener() {
            @Override public void onSuccess(boolean fav) {
                if (!isAdded() || btnFavorite == null) return;
                favorited = fav;
                updateFavoriteButton();
            }
            @Override public void onError(Throwable t) { }
        };
        if (favorited) {
            libraryRepo.unfavoriteArticle(articleId, cb);
        } else {
            libraryRepo.favoriteArticle(articleId, cb);
        }
    }

    private void updateFavoriteButton() {
        if (btnFavorite == null) return;
        btnFavorite.setText(favorited ? R.string.library_unfavorite : R.string.library_favorite);
        btnFavorite.setIconResource(favorited ? R.drawable.ic_star : R.drawable.ic_star);
        btnFavorite.setAlpha(favorited ? 1f : 0.85f);
    }

    private String pick(@Nullable Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        String tag = LocaleHelper.resolveActiveTag(requireContext());
        if (map.get(tag) != null && !map.get(tag).isEmpty()) return map.get(tag);
        for (String key : new String[]{"ar", "en", "tr"}) {
            if (map.get(key) != null && !map.get(key).isEmpty()) return map.get(key);
        }
        return map.values().iterator().hasNext() ? map.values().iterator().next() : "";
    }

    private void bindVideo(@Nullable String url) {
        if (videoPlayer == null) return;
        if (url == null || url.isEmpty()) {
            boundVideoUrl = null;
            videoPlayer.stopPlayback();
            videoPlayer.setVisibility(View.GONE);
            return;
        }
        videoPlayer.setVisibility(View.VISIBLE);
        if (url.equals(boundVideoUrl)) {
            if (!videoPlayer.isPlaying() && savedPositionMs > 0) {
                videoPlayer.seekTo(savedPositionMs);
            }
            return;
        }
        boundVideoUrl = url;
        final int resumeAt = savedPositionMs;
        videoPlayer.setVideoURI(Uri.parse(url));
        videoPlayer.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            if (resumeAt > 0) {
                videoPlayer.seekTo(resumeAt);
                savedPositionMs = 0;
            }
            if (isResumed()) {
                videoPlayer.start();
            }
        });
        if (isResumed()) {
            videoPlayer.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (videoPlayer != null && boundVideoUrl != null && !videoPlayer.isPlaying()) {
            if (savedPositionMs > 0) {
                videoPlayer.seekTo(savedPositionMs);
                savedPositionMs = 0;
            }
            videoPlayer.start();
        }
    }

    @Override
    public void onPause() {
        if (videoPlayer != null) {
            if (videoPlayer.isPlaying()) {
                savedPositionMs = videoPlayer.getCurrentPosition();
                videoPlayer.pause();
            }
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (videoPlayer != null) {
            videoPlayer.stopPlayback();
            videoPlayer.setOnPreparedListener(null);
        }
        videoPlayer = null;
        boundVideoUrl = null;
        imgCover = null;
        imgCoverFallback = null;
        btnFavorite = null;
        super.onDestroyView();
    }

    private String buildAuthorLine(String name, String title) {
        String safeName = name != null ? name.trim() : "";
        String safeTitle = title != null ? title.trim() : "";
        if (safeName.isEmpty()) return "";
        if (safeTitle.isEmpty()) return getString(R.string.library_author_format, safeName);
        return getString(R.string.library_author_with_title, safeName, safeTitle);
    }
}
