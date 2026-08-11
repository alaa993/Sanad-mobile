package com.brightpath.sanad.data;

import java.util.List;
import java.util.Map;

public class LibraryModels {
    public static class Category {
        public int id;
        public Map<String,String> title; // {ar,en,tr}
        public List<ArticleListItem> articles;
    }
    public static class ArticleListItem {
        public int id;
        public Map<String,String> title; // {ar,en,tr}
        public String image;
        public String type;
        public String duration;
        public String author_name;
        public String author_title;
        public String author_avatar;
        public int category_id; // optional on index
        public String video_url;
        public String thumbnail;
        public java.util.List<String> tags;
    }
    public static class ArticleDetail {
        public int id;
        public Map<String,String> title;
        public Map<String,String> body;
        public String image;
        public String type;
        public String duration;
        public String author_name;
        public String author_title;
        public String author_avatar;
        public int category_id;
        public String video_url;
        public String thumbnail;
        public java.util.List<String> tags;
        public Boolean favorited;
    }
    public static class FavoriteResponse {
        public Boolean favorited;
    }
    public static class TagsResponse {
        public java.util.List<String> data;
    }
    public static class DailyTip {
        public String title;
        public String body;
        public Integer article_id;
        public String author_name;
    }

    public static class CuratedResponse {
        public java.util.List<ArticleListItem> data;
        public java.util.List<String> tags;
    }
}
