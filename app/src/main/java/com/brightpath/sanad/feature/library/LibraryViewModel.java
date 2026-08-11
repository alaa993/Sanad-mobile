package com.brightpath.sanad.feature.library;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.brightpath.sanad.data.LibraryModels;
import com.brightpath.sanad.data.LibraryRepository;

import java.util.List;

public class LibraryViewModel extends AndroidViewModel {
    private final LibraryRepository repo;
    private final LibraryRealtimeClient realtime;
    private String lastTag;
    private final LibraryRealtimeClient.Listener realtimeListener = () -> load(lastTag);

    public LibraryViewModel(@NonNull Application app){
        super(app);
        repo=new LibraryRepository(app);
        realtime = LibraryRealtimeClient.get(app);
        realtime.addListener(realtimeListener);
    }

    public static class UIState {
        public boolean loading;
        public String error;
        public List<LibraryModels.Category> categories;
        public static UIState loading(){ UIState s=new UIState(); s.loading=true; return s; }
        public static UIState error(String e){ UIState s=new UIState(); s.error=e; return s; }
        public static UIState data(List<LibraryModels.Category> c){ UIState s=new UIState(); s.categories=c; return s; }
    }

    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.loading());
    public LiveData<UIState> getState(){ return state; }

    public void load(){
        load(null);
    }

    public void load(String tag){
        lastTag = tag;
        // Avoid full-screen loading flash on realtime refreshes when data already exists.
        UIState current = state.getValue();
        if (current == null || current.categories == null) {
            state.postValue(UIState.loading());
        }
        repo.fetchLibrary(tag, new LibraryRepository.ListListener() {
            @Override public void onSuccess(List<LibraryModels.Category> data) { state.postValue(UIState.data(data)); }
            @Override public void onError(Throwable t) { state.postValue(UIState.error(t.getMessage())); }
        });
    }

    @Override
    protected void onCleared() {
        realtime.removeListener(realtimeListener);
        super.onCleared();
    }
}
