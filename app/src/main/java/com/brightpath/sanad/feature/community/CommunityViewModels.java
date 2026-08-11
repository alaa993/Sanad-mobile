
package com.brightpath.sanad.feature.community;
import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.*;
import java.util.*;
public class CommunityViewModels {
  public static class CommunityListVM extends AndroidViewModel {
    private final CommunityRepository repo; public final MutableLiveData<List<CommunityModels.Community>> list = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<String> createError = new MutableLiveData<>();
    public final MutableLiveData<Boolean> createSuccess = new MutableLiveData<>();
    public CommunityListVM(@NonNull Application app){ super(app); repo = new CommunityRepository(app); }
    public void load(){
      load(null);
    }
    public void load(@Nullable String category){
      error.postValue(null);
      repo.communities(category, new CommunityRepository.Cb<CommunityModels.ListResponse<CommunityModels.Community>>(){
      @Override public void ok(CommunityModels.ListResponse<CommunityModels.Community> t){ list.postValue(t.data); }
      @Override public void err(Throwable e){ error.postValue(e.getMessage()!=null? e.getMessage() : "error"); } }); }
    public void create(String slug, String nameAr, String about, String visibility, String kind){
      createError.postValue(null);
      repo.createCommunity(slug, nameAr, about, visibility, kind, new CommunityRepository.Cb<CommunityModels.SimpleResponse>(){
        @Override public void ok(CommunityModels.SimpleResponse t){ createSuccess.postValue(true); load(); }
        @Override public void err(Throwable e){ createError.postValue(e.getMessage()!=null? e.getMessage() : "error"); }
      });
    }
    public void toggle(CommunityModels.Community community){
      if(community==null) return;
      int id = community.id;
      boolean join = !community.joined;
      CommunityRepository.Cb<CommunityModels.SimpleResponse> cb = new CommunityRepository.Cb<CommunityModels.SimpleResponse>(){
        @Override public void ok(CommunityModels.SimpleResponse t){
          updateCommunity(id, join, t.members_count);
        }
        @Override public void err(Throwable e){ error.postValue(e.getMessage()!=null? e.getMessage() : "error"); }
      };
      if(join) repo.join(id, cb); else repo.leave(id, cb);
    }
    private void updateCommunity(int id, boolean joined, Integer membersCount){
      List<CommunityModels.Community> current = list.getValue();
      if(current==null) return;
      List<CommunityModels.Community> updated = new ArrayList<>(current.size());
      for(CommunityModels.Community item : current){
        if(item!=null && item.id == id){
          CommunityModels.Community clone = clone(item);
          clone.joined = joined;
          if(membersCount!=null) clone.members_count = membersCount;
          updated.add(clone);
        } else {
          updated.add(item);
        }
      }
      list.postValue(updated);
    }
    private CommunityModels.Community clone(CommunityModels.Community src){
      CommunityModels.Community c = new CommunityModels.Community();
      c.id = src.id; c.slug = src.slug; c.name = src.name; c.about = src.about; c.visibility = src.visibility;
      c.kind = src.kind; c.category = src.category; c.members_count = src.members_count; c.joined = src.joined;
      c.organization_owned = src.organization_owned;
      return c;
    }
  }
  public static class CommunityFeedVM extends AndroidViewModel {
    private final CommunityRepository repo;
    private final CommunityRealtimeClient realtime;
    private final CommunityRealtimeClient.Listener realtimeListener = new CommunityRealtimeClient.Listener() {
      @Override public void onPost(int communityId, CommunityModels.Post post) {
        if(communityId == id) addPost(post);
      }
      @Override public void onComment(CommunityRealtimeClient.CommentPayload payload) {
        if(payload!=null && payload.communityId == id){
          appendComment(payload.postId, payload.comment);
        }
      }
      @Override public void onLike(CommunityRealtimeClient.LikePayload payload) {
        if(payload!=null && payload.communityId == id){
          Integer count = payload.likesCount >=0 ? payload.likesCount : null;
          updatePost(payload.postId, payload.liked, count);
        }
      }
    };
    public final MutableLiveData<List<CommunityModels.Post>> feed = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<CommunityModels.Community> details = new MutableLiveData<>();
    public final MutableLiveData<String> attachment = new MutableLiveData<>(null);
    public final MutableLiveData<Boolean> attachmentUploading = new MutableLiveData<>(false);
    public final MutableLiveData<String> attachmentError = new MutableLiveData<>();
    public final MutableLiveData<String> actionError = new MutableLiveData<>();
    private int id=-1;
    public CommunityFeedVM(@NonNull Application app){
      super(app);
      repo = new CommunityRepository(app);
      realtime = CommunityRealtimeClient.get(app);
      realtime.addListener(realtimeListener);
    }
    public void open(int id){
      this.id=id;
      feed.postValue(new ArrayList<>());
      details.postValue(null);
      attachment.postValue(null);
      attachmentUploading.postValue(false);
      fetch();
      load();
    }
    private void fetch(){ if(id<=0) return;
      repo.community(id, new CommunityRepository.Cb<CommunityModels.ItemResponse<CommunityModels.Community>>(){
        @Override public void ok(CommunityModels.ItemResponse<CommunityModels.Community> t){ details.postValue(t.data); }
        @Override public void err(Throwable e){ actionError.postValue(e.getMessage()!=null? e.getMessage() : "error"); } });
    }
    public void load(){ if(id<=0) return; repo.feed(id, new CommunityRepository.Cb<CommunityModels.ListResponse<CommunityModels.Post>>(){ @Override public void ok(CommunityModels.ListResponse<CommunityModels.Post> t){ feed.postValue(sortPostsNewestFirst(t!=null? t.data : null)); } @Override public void err(Throwable e){ actionError.postValue(e.getMessage()!=null? e.getMessage() : "error"); } }); }
    public void post(String text){
      if(id<=0) return;
      final String media = attachment.getValue();
      String kind = null;
      CommunityModels.Community c = details.getValue();
      if(c!=null && "qa".equalsIgnoreCase(c.kind) && answerQuestionId<=0){
        kind = "question";
      } else if(answerQuestionId>0){
        kind = "answer";
      }
      final Integer qId = answerQuestionId > 0 ? answerQuestionId : null;
      final String postKind = kind;
      repo.post(id, text, media, postKind, qId, new CommunityRepository.Cb<CommunityModels.SimpleResponse>(){
        @Override public void ok(CommunityModels.SimpleResponse t){
          attachment.postValue(null);
          answerQuestionId = -1;
          load();
        }
        @Override public void err(Throwable e){ actionError.postValue(e.getMessage()!=null? e.getMessage() : "error"); }
      });
    }
    private int answerQuestionId = -1;
    public void startAnswer(int questionId){ this.answerQuestionId = questionId; }
    public void cancelAnswer(){ this.answerQuestionId = -1; }
    public void acceptAnswer(int questionId, int answerId){
      if(id<=0) return;
      repo.acceptAnswer(id, questionId, answerId, new CommunityRepository.Cb<CommunityModels.SimpleResponse>(){
        @Override public void ok(CommunityModels.SimpleResponse t){ load(); }
        @Override public void err(Throwable e){ actionError.postValue(e.getMessage()!=null? e.getMessage() : "error"); }
      });
    }
    public void toggleMembership(){
      if(id<=0) return;
      CommunityModels.Community c = details.getValue();
      if(c==null) return;
      boolean join = !c.joined;
      CommunityRepository.Cb<CommunityModels.SimpleResponse> cb = new CommunityRepository.Cb<CommunityModels.SimpleResponse>(){
        @Override public void ok(CommunityModels.SimpleResponse t){
          CommunityModels.Community current = details.getValue();
          if(current!=null){
            current.joined = join;
            if(t.members_count!=null) current.members_count = t.members_count;
            details.postValue(current);
          }
          if(join) load();
        }
        @Override public void err(Throwable e){ actionError.postValue(e.getMessage()!=null? e.getMessage() : "error"); }
      };
      if(join) repo.join(id, cb); else repo.leave(id, cb);
    }
    public void toggleLike(CommunityModels.Post post){
      if(id<=0 || post==null) return;
      repo.toggleLike(id, post.id, new CommunityRepository.Cb<CommunityModels.SimpleResponse>(){
        @Override public void ok(CommunityModels.SimpleResponse t){
          if(t==null) return;
          Boolean liked = t.liked;
          Integer likes = t.likes_count;
          if(liked==null && likes==null) return;
          updatePost(post.id, liked, likes);
        }
        @Override public void err(Throwable e){ actionError.postValue(e.getMessage()!=null? e.getMessage() : "error"); }
      });
    }
    private void updatePost(int postId, Boolean liked, Integer likesCount){
      List<CommunityModels.Post> current = feed.getValue();
      if(current==null) return;
      List<CommunityModels.Post> updated = new ArrayList<>(current.size());
      for(CommunityModels.Post item : current){
        if(item!=null && item.id == postId){
          CommunityModels.Post copy = clonePost(item);
          if(liked!=null) copy.liked = liked;
          if(likesCount!=null) copy.likes_count = likesCount;
          updated.add(copy);
        } else {
          updated.add(item);
        }
      }
      feed.postValue(updated);
    }
    private void addPost(CommunityModels.Post post){
      if(post==null) return;
      List<CommunityModels.Post> current = feed.getValue();
      List<CommunityModels.Post> updated = new ArrayList<>();
      updated.add(normalizePost(post));
      if(current!=null){
        for(CommunityModels.Post item : current){
          if(item!=null && item.id == post.id) continue;
          updated.add(item);
        }
      }
      feed.postValue(sortPostsNewestFirst(updated));
    }
    private void appendComment(int postId, CommunityModels.Comment comment){
      if(comment==null) return;
      List<CommunityModels.Post> current = feed.getValue();
      if(current==null) return;
      List<CommunityModels.Post> updated = new ArrayList<>(current.size());
      for(CommunityModels.Post item : current){
        if(item!=null && item.id == postId){
          CommunityModels.Post copy = clonePost(item);
          List<CommunityModels.Comment> comments = copy.comments!=null? new ArrayList<>(copy.comments) : new ArrayList<>();
          boolean exists = false;
          for(CommunityModels.Comment existing : comments){
            if(existing!=null && existing.id == comment.id){ exists = true; break; }
          }
          if(!exists) comments.add(0, comment);
          comments.sort((a, b) -> {
            String left = a != null && a.created_at != null ? a.created_at : "";
            String right = b != null && b.created_at != null ? b.created_at : "";
            return right.compareTo(left);
          });
          copy.comments = comments;
          updated.add(copy);
        } else {
          updated.add(item);
        }
      }
      feed.postValue(updated);
    }
    private List<CommunityModels.Post> sortPostsNewestFirst(List<CommunityModels.Post> source){
      List<CommunityModels.Post> sorted = new ArrayList<>();
      if(source!=null){
        for(CommunityModels.Post post : source){
          if(post==null) continue;
          sorted.add(normalizePost(post));
        }
      }
      sorted.sort((a, b) -> {
        String left = a != null && a.created_at != null ? a.created_at : "";
        String right = b != null && b.created_at != null ? b.created_at : "";
        return right.compareTo(left);
      });
      return sorted;
    }
    private CommunityModels.Post normalizePost(CommunityModels.Post src){
      CommunityModels.Post copy = clonePost(src);
      if(copy.media_url!=null && !copy.media_url.isEmpty()){
        String resolved = com.brightpath.sanad.data.AppConfig.storageUrl(copy.media_url);
        if(resolved!=null) copy.media_url = resolved;
      }
      if(copy.comments!=null && !copy.comments.isEmpty()){
        List<CommunityModels.Comment> comments = new ArrayList<>(copy.comments);
        comments.sort((a, b) -> {
          String left = a != null && a.created_at != null ? a.created_at : "";
          String right = b != null && b.created_at != null ? b.created_at : "";
          return right.compareTo(left);
        });
        copy.comments = comments;
      }
      return copy;
    }
    private CommunityModels.Post clonePost(CommunityModels.Post src){
      CommunityModels.Post p = new CommunityModels.Post();
      p.id = src.id; p.body = src.body; p.author = src.author; p.created_at = src.created_at;
      p.media_url = src.media_url; p.likes_count = src.likes_count; p.liked = src.liked; p.comments = src.comments;
      p.post_kind = src.post_kind; p.question_id = src.question_id; p.accepted_at = src.accepted_at;
      p.answers = src.answers; p.answers_count = src.answers_count; p.accepted_answer_id = src.accepted_answer_id;
      return p;
    }
    public void comment(CommunityModels.Post post, String text){
      if(id<=0 || post==null || text==null || text.trim().isEmpty()) return;
      repo.comment(id, post.id, text.trim(), new CommunityRepository.Cb<CommunityModels.SimpleResponse>(){
        @Override public void ok(CommunityModels.SimpleResponse t){ load(); }
        @Override public void err(Throwable e){ actionError.postValue(e.getMessage()!=null? e.getMessage() : "error"); }
      });
    }
    public void clearAttachment(){ attachment.postValue(null); }
    public void uploadMedia(Uri uri){
      if(uri==null) return;
      attachmentUploading.postValue(true);
      repo.uploadMedia(uri, new CommunityRepository.Cb<CommunityModels.UploadResponse>(){
        @Override public void ok(CommunityModels.UploadResponse t){
          attachmentUploading.postValue(false);
          String raw = t!=null? (t.url!=null? t.url : t.media_url) : null;
          String resolved = com.brightpath.sanad.data.AppConfig.storageUrl(raw);
          attachment.postValue(resolved!=null? resolved : raw);
        }
        @Override public void err(Throwable e){
          attachmentUploading.postValue(false);
          attachmentError.postValue(e!=null? e.getMessage():null);
        }
      });
    }
    public void resetAttachmentError(){ attachmentError.postValue(null); }
    @Override protected void onCleared(){
      super.onCleared();
      realtime.removeListener(realtimeListener);
    }
  }
}
