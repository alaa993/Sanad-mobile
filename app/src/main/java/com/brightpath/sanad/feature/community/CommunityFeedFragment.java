
package com.brightpath.sanad.feature.community;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle; import android.view.*; import android.widget.EditText; import android.widget.ImageButton; import android.widget.LinearLayout; import android.widget.ProgressBar; import android.widget.TextView; import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*; import androidx.fragment.app.Fragment; import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton; import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.brightpath.sanad.R; import java.util.*;
import com.brightpath.sanad.data.auth.TokenStore;
import com.bumptech.glide.Glide;
public class CommunityFeedFragment extends Fragment {
  private CommunityViewModels.CommunityFeedVM vm; private RecyclerView rv; private EditText et; private ImageButton send, attach;
  private TextView heroTitle, heroMeta; private MaterialButton btnJoin, attachmentPreview; private MaterialCardView composerCard;
  private ProgressBar feedLoading;
  private TextView emptyFeed;
  private ActivityResultLauncher<String[]> attachmentPicker;
  private boolean composerEnabled = false;
  private String communitySlug = "";
  private int communityId = -1;
  private boolean isQaCommunity = false;
  private CommunityRolePolicy policy;
  private boolean canAnswerQa = false;
  private int currentUserId = 0;

  @Override public void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    attachmentPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
      if(uri!=null && vm!=null){
        try{
          requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }catch (Exception ignored){}
        vm.uploadMedia(uri);
      }
    });
  }
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){ return inflater.inflate(R.layout.fragment_community_feed, container, false); }
  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
    super.onViewCreated(v,s);
    TokenStore tokenStore = new TokenStore(requireContext());
    policy = new CommunityRolePolicy(tokenStore.getRole());
    canAnswerQa = policy.canAnswerQa();
    currentUserId = tokenStore.getUserId();
    communityId = getArguments()!=null? getArguments().getInt("communityId",-1): -1;
    String title = getArguments()!=null? getArguments().getString("communityTitle",""): "";
    communitySlug = getArguments()!=null? getArguments().getString("communitySlug",""): "";
    requireActivity().setTitle(title);
    heroTitle = v.findViewById(R.id.tvCommunityTitle);
    heroMeta = v.findViewById(R.id.tvCommunityMeta);
    feedLoading = v.findViewById(R.id.feedLoading);
    emptyFeed = v.findViewById(R.id.tvFeedEmpty);
    btnJoin = v.findViewById(R.id.btnJoinToggle);
    composerCard = v.findViewById(R.id.postBar);
    heroTitle.setText(title);
    rv = v.findViewById(R.id.rvFeed); et = v.findViewById(R.id.etPost); send = v.findViewById(R.id.btnSend);
    attach = v.findViewById(R.id.btnAttach);
    attachmentPreview = v.findViewById(R.id.btnAttachmentPreview);
    updateComposerState(null);
    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    Adapter ad = new Adapter(canAnswerQa, currentUserId, policy, new Adapter.Listener() {
      @Override public void onLike(CommunityModels.Post post) { vm.toggleLike(post); }
      @Override public void onOpenMedia(String url) { openMedia(url); }
      @Override public void onComment(CommunityModels.Post post) { openPostThread(post); }
      @Override public void onAnswer(CommunityModels.Post post) { promptAnswer(post); }
      @Override public void onAccept(CommunityModels.Post question, CommunityModels.Post answer) { vm.acceptAnswer(question.id, answer.id); }
    });
    rv.setAdapter(ad);
    vm = new ViewModelProvider(this).get(CommunityViewModels.CommunityFeedVM.class);
    vm.feed.observe(getViewLifecycleOwner(), list -> {
      ad.submit(list);
      if (feedLoading != null) feedLoading.setVisibility(View.GONE);
      if (emptyFeed != null) emptyFeed.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
    });
    vm.details.observe(getViewLifecycleOwner(), this::renderCommunity);
    vm.attachment.observe(getViewLifecycleOwner(), this::renderAttachment);
    vm.attachmentUploading.observe(getViewLifecycleOwner(), uploading -> {
      boolean up = uploading!=null && uploading;
      if(up){
        attachmentPreview.setVisibility(View.VISIBLE);
        attachmentPreview.setText(R.string.community_attachment_uploading);
        attachmentPreview.setEnabled(false);
      } else if(vm.attachment.getValue()==null){
        attachmentPreview.setVisibility(View.GONE);
      }
      refreshComposerState();
    });
    vm.attachmentError.observe(getViewLifecycleOwner(), err -> {
      if(err!=null){
        Toast.makeText(requireContext(), R.string.community_attachment_error, Toast.LENGTH_SHORT).show();
        vm.resetAttachmentError();
      }
    });
    vm.actionError.observe(getViewLifecycleOwner(), err -> {
      if (err != null && !err.isEmpty()) {
        Toast.makeText(requireContext(), R.string.error_load_failed, Toast.LENGTH_SHORT).show();
      }
    });
    if(communityId>0) {
      if (feedLoading != null) feedLoading.setVisibility(View.VISIBLE);
      vm.open(communityId);
    }
    send.setOnClickListener(x -> {
      String t = et.getText().toString().trim();
      boolean hasAttachment = vm.attachment.getValue()!=null;
      if(!composerEnabled){
        vm.toggleMembership();
        Toast.makeText(requireContext(), R.string.community_join, Toast.LENGTH_SHORT).show();
        return;
      }
      if(t.isEmpty() && !hasAttachment){
        Toast.makeText(requireContext(), R.string.community_post_hint, Toast.LENGTH_SHORT).show();
        return;
      }
      vm.post(t);
      et.setText("");
    });
    btnJoin.setOnClickListener(v1 -> vm.toggleMembership());
    attach.setOnClickListener(v1 -> pickMedia());
    attachmentPreview.setOnClickListener(v1 -> {
      vm.clearAttachment();
      attachmentPreview.setVisibility(View.GONE);
    });
  }
  private void openMedia(String url){
    if(url==null || url.isEmpty()) return;
    try{
      String resolved = com.brightpath.sanad.data.AppConfig.storageUrl(url);
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(resolved != null ? resolved : url));
      startActivity(intent);
    }catch (Exception ex){
      Toast.makeText(requireContext(), R.string.community_media_error, Toast.LENGTH_SHORT).show();
    }
  }
  private void promptAnswer(CommunityModels.Post post){
    if(post==null) return;
    final EditText input = new EditText(requireContext());
    input.setHint(R.string.community_qa_answer_hint);
    input.setMinLines(2);
    input.setPadding(32,32,32,32);
    new MaterialAlertDialogBuilder(requireContext())
      .setTitle(R.string.community_qa_answer)
      .setView(input)
      .setPositiveButton(R.string.community_comment_send, (dialog, which) -> {
        String text = input.getText().toString().trim();
        if(!text.isEmpty()) {
          vm.startAnswer(post.id);
          vm.post(text);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }
  private void openPostThread(CommunityModels.Post post){
    if(post==null || !isAdded()) return;
    View sheet = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_post_thread, null, false);
    TextView author = sheet.findViewById(R.id.tvThreadAuthor);
    TextView body = sheet.findViewById(R.id.tvThreadBody);
    TextView empty = sheet.findViewById(R.id.tvThreadCommentsEmpty);
    LinearLayout commentsContainer = sheet.findViewById(R.id.threadCommentsContainer);
    LinearLayout answersContainer = sheet.findViewById(R.id.threadAnswersContainer);
    EditText input = sheet.findViewById(R.id.etThreadComment);
    MaterialButton sendComment = sheet.findViewById(R.id.btnThreadSend);
    author.setText(post.author!=null? post.author.name : "—");
    body.setText(post.body!=null? post.body : "");
    if(answersContainer!=null){
      answersContainer.removeAllViews();
      if(post.answers!=null && !post.answers.isEmpty()){
        answersContainer.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for(CommunityModels.Post ans : post.answers){
          View row = inflater.inflate(R.layout.item_qa_answer_inline, answersContainer, false);
          TextView aBody = row.findViewById(R.id.tvAnswerBody);
          TextView aMeta = row.findViewById(R.id.tvAnswerMeta);
          MaterialButton accept = row.findViewById(R.id.btnAcceptAnswer);
          aBody.setText(ans.body!=null? ans.body : "");
          String aAuthor = ans.author!=null? ans.author.name : "—";
          aMeta.setText(aAuthor + (ans.accepted_at!=null? " ✓" : ""));
          if(accept!=null) accept.setVisibility(View.GONE);
          answersContainer.addView(row);
        }
      } else {
        answersContainer.setVisibility(View.GONE);
      }
    }
    commentsContainer.removeAllViews();
    List<CommunityModels.Comment> comments = post.comments;
    if(comments!=null && !comments.isEmpty()){
      empty.setVisibility(View.GONE);
      List<CommunityModels.Comment> sorted = new ArrayList<>(comments);
      sorted.sort((a, b) -> {
        String left = a != null && a.created_at != null ? a.created_at : "";
        String right = b != null && b.created_at != null ? b.created_at : "";
        return right.compareTo(left);
      });
      LayoutInflater inflater = LayoutInflater.from(requireContext());
      for(CommunityModels.Comment c : sorted){
        View row = inflater.inflate(R.layout.item_comment_inline, commentsContainer, false);
        TextView cAuthor = row.findViewById(R.id.tvCommentAuthor);
        TextView cBody = row.findViewById(R.id.tvCommentBody);
        TextView cMeta = row.findViewById(R.id.tvCommentMeta);
        cAuthor.setText(c.author!=null? c.author.name : "—");
        cBody.setText(c.body!=null? c.body : "");
        cMeta.setText(c.created_at!=null? c.created_at : "");
        commentsContainer.addView(row);
      }
    } else {
      empty.setVisibility(View.VISIBLE);
    }
    com.google.android.material.bottomsheet.BottomSheetDialog dialog =
        new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
    dialog.setContentView(sheet);
    sendComment.setOnClickListener(v -> {
      String text = input.getText()!=null? input.getText().toString().trim() : "";
      if(text.isEmpty()) return;
      vm.comment(post, text);
      dialog.dismiss();
    });
    dialog.show();
  }
  private void promptComment(CommunityModels.Post post){
    openPostThread(post);
  }
  private void renderAttachment(String url){
    if(url!=null){
      attachmentPreview.setVisibility(View.VISIBLE);
      attachmentPreview.setEnabled(true);
      attachmentPreview.setText(R.string.community_attachment_ready);
    } else if(!isAttachmentUploading()){
      attachmentPreview.setVisibility(View.GONE);
    }
    refreshComposerState();
  }
  private void renderCommunity(CommunityModels.Community c){
    if(c==null) return;
    if (c.slug != null) communitySlug = c.slug;
    isQaCommunity = "qa".equalsIgnoreCase(c.kind);
    heroTitle.setText(preferLocalized(c.name, heroTitle.getText()!=null? heroTitle.getText().toString():""));
    String about = preferLocalized(c.about, getString(R.string.community_feed_meta));
    String members = getString(R.string.community_members_count, c.members_count);
    if(about==null || about.isEmpty()) heroMeta.setText(members);
    else heroMeta.setText(getString(R.string.community_meta_template, members, about));
    btnJoin.setVisibility(policy.canJoinFreely() ? View.VISIBLE : View.GONE);
    btnJoin.setText(c.joined? R.string.community_leave : R.string.community_join);
    updateComposerState(c);
  }
  private void updateComposerState(@Nullable CommunityModels.Community community){
    boolean canPost = policy != null && policy.canPost(community);
    composerEnabled = canPost;
    et.setEnabled(canPost);
    composerCard.setAlpha(canPost ? 1f : 0.5f);
    boolean joined = community != null && community.joined;
    et.setHint(canPost
        ? (isQaCommunity ? getString(R.string.community_qa_question_hint) : getString(R.string.community_post_hint))
        : (joined ? getString(R.string.community_post_hint_locked) : getString(R.string.community_post_hint_locked)));
    if(!canPost && vm!=null){
      attachmentPreview.setVisibility(View.GONE);
      vm.clearAttachment();
    }
    refreshComposerState();
  }
  private void refreshComposerState(){
    boolean uploading = isAttachmentUploading();
    boolean enabled = composerEnabled && !uploading;
    send.setEnabled(enabled);
    attach.setEnabled(composerEnabled && !uploading);
  }
  private boolean isAttachmentUploading(){
    return vm!=null && Boolean.TRUE.equals(vm.attachmentUploading.getValue());
  }
  private void pickMedia(){
    if(!composerEnabled) return;
    String[] types = new String[]{"image/*","video/*"};
    attachmentPicker.launch(types);
  }
  private String preferLocalized(Map<String,String> map, String fallback){
    if(map==null || map.isEmpty()) return fallback!=null? fallback : "";
    Locale l = Locale.getDefault();
    String lang = l.getLanguage();
    if(map.containsKey(lang)) return map.get(lang);
    if(map.containsKey("ar")) return map.get("ar");
    if(map.containsKey("en")) return map.get("en");
    return map.values().iterator().hasNext()? map.values().iterator().next() : fallback;
  }
  static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
    interface Listener {
      void onLike(CommunityModels.Post post);
      void onOpenMedia(String url);
      void onComment(CommunityModels.Post post);
      void onAnswer(CommunityModels.Post post);
      void onAccept(CommunityModels.Post question, CommunityModels.Post answer);
    }
    private final boolean allowAnswer;
    private final int currentUserId;
    private final CommunityRolePolicy policy;
    private final Listener listener;
    private final List<CommunityModels.Post> data = new ArrayList<>();
    Adapter(boolean allowAnswer, int currentUserId, CommunityRolePolicy policy, Listener l){
      this.allowAnswer = allowAnswer;
      this.currentUserId = currentUserId;
      this.policy = policy;
      this.listener = l;
    }
    void submit(List<CommunityModels.Post> d){ data.clear(); if(d!=null) data.addAll(d); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){ return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_post, p, false)); }
    @Override public void onBindViewHolder(@NonNull VH h, int i){
      CommunityModels.Post p = data.get(i);
      h.author.setText(p.author!=null? p.author.name : "—");
      h.body.setText(p.body!=null? p.body : "");
      h.meta.setText(p.created_at!=null? p.created_at : "");
      if(p.media_url!=null && !p.media_url.isEmpty()){
        h.media.setVisibility(View.VISIBLE);
        String mediaUrl = com.brightpath.sanad.data.AppConfig.storageUrl(p.media_url);
        Glide.with(h.media.getContext())
                .load(mediaUrl != null ? mediaUrl : p.media_url)
                .centerCrop()
                .into(h.media);
        h.media.setOnClickListener(v -> { if(listener!=null) listener.onOpenMedia(p.media_url); });
      } else {
        h.media.setVisibility(View.GONE);
        h.media.setOnClickListener(null);
      }
      int count = Math.max(0, p.likes_count);
      h.likes.setText(h.itemView.getContext().getString(R.string.community_like_count, count));
      h.likeButton.setText(p.liked? R.string.community_liked : R.string.community_like);
      h.likeButton.setOnClickListener(v -> { if(listener!=null) listener.onLike(p); });
      h.commentButton.setOnClickListener(v -> { if(listener!=null) listener.onComment(p); });
      if(h.answerButton!=null){
        boolean isQuestion = p.post_kind==null || "question".equals(p.post_kind) || "post".equals(p.post_kind);
        h.answerButton.setVisibility(allowAnswer && isQuestion? View.VISIBLE : View.GONE);
        h.answerButton.setOnClickListener(v -> { if(listener!=null) listener.onAnswer(p); });
      }
      bindAnswers(h, p);
      // Comments live in the post thread sheet — keep feed cards lean.
      if (h.commentSection != null) h.commentSection.setVisibility(View.GONE);
    }
    private void bindAnswers(VH h, CommunityModels.Post p){
      if(h.answersContainer==null) return;
      h.answersContainer.removeAllViews();
      if(p.answers==null || p.answers.isEmpty()) {
        h.answersContainer.setVisibility(View.GONE);
        return;
      }
      h.answersContainer.setVisibility(View.GONE);
    }
    private void bindComments(VH h, CommunityModels.Post p){
      // Intentionally empty: comments are shown in the thread sheet.
    }
    private boolean canAcceptAnswer(CommunityModels.Post question){
      if (question == null || policy == null) return false;
      String role = policy.role;
      if ("admin".equals(role) || "organization".equals(role)) return true;
      return question.author != null && question.author.id == currentUserId;
    }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder {
      TextView author, body, meta, likes, commentsEmpty; MaterialButton likeButton, commentButton, answerButton; android.widget.ImageView media;
      LinearLayout commentsContainer, answersContainer, commentSection;
      VH(@NonNull View v){
        super(v);
        author=v.findViewById(R.id.tvAuthor);
        body=v.findViewById(R.id.tvBody);
        meta=v.findViewById(R.id.tvMeta);
        likes=v.findViewById(R.id.tvLikes);
        likeButton=v.findViewById(R.id.btnLike);
        media=v.findViewById(R.id.imgMedia);
        commentButton=v.findViewById(R.id.btnComment);
        answerButton=v.findViewById(R.id.btnAnswer);
        commentsContainer=v.findViewById(R.id.commentsContainer);
        answersContainer=v.findViewById(R.id.answersContainer);
        commentsEmpty=v.findViewById(R.id.tvCommentsEmpty);
        commentSection=v.findViewById(R.id.commentSection);
      }
    }
  }
}
