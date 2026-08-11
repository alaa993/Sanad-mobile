
package com.brightpath.sanad.feature.community;
import java.util.List; import java.util.Map;
public class CommunityModels {
  public static class Community { public int id; public String slug; public Map<String,String> name; public Map<String,String> about; public String visibility; public String kind; public String category; public int members_count; public boolean joined; public boolean organization_owned; }
  public static class Author { public int id; public String name; }
  public static class Comment { public int id; public String body; public Author author; public String created_at; }
  public static class Post { public int id; public String body; public Author author; public String created_at; public String media_url; public int likes_count; public boolean liked; public List<Comment> comments; public String post_kind; public Integer question_id; public String accepted_at; public List<Post> answers; public int answers_count; public Integer accepted_answer_id; }
  public static class Article { public int id; public String slug; public Map<String,String> title; public List<String> tags; public String created_at; public Map<String,String> body; }
  public static class Journal { public int id; public String entry; public String created_at; }
  public static class ListResponse<T> { public List<T> data; }
  public static class ItemResponse<T> { public T data; }
  public static class SimpleResponse { public Boolean joined; public Boolean favorited; public Boolean deleted; public Boolean liked; public Integer id; public Integer members_count; public Integer likes_count; public String created_at; }
  public static class UploadResponse { public String url; public String media_url; public String type; }
}
