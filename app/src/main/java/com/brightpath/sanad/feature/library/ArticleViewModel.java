package com.brightpath.sanad.feature.library;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.brightpath.sanad.data.LibraryModels;
import com.brightpath.sanad.data.LibraryRepository;

public class ArticleViewModel extends AndroidViewModel {
    private final LibraryRepository repo;
    public ArticleViewModel(@NonNull Application app){ super(app); repo=new LibraryRepository(app); }

    public static class UIState {
        public boolean loading;
        public String error;
        public LibraryModels.ArticleDetail article;
        public static UIState loading(){ UIState s=new UIState(); s.loading=true; return s; }
        public static UIState error(String e){ UIState s=new UIState(); s.error=e; return s; }
        public static UIState data(LibraryModels.ArticleDetail a){ UIState s=new UIState(); s.article=a; return s; }
    }

    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.loading());
    public LiveData<UIState> getState(){ return state; }

    public void load(int id){
        state.postValue(UIState.loading());
        repo.fetchArticle(id, new LibraryRepository.ArticleListener() {
            @Override public void onSuccess(LibraryModels.ArticleDetail d) { state.postValue(UIState.data(d)); }
            @Override public void onError(Throwable t) { state.postValue(UIState.error(t.getMessage())); }
        });
    }
}
