package com.brightpath.sanad.feature.groups;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class GroupsViewModel extends AndroidViewModel {
    private final GroupRepository repo;
    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.loading());

    public static class UIState {
        public boolean loading;
        public String error;
        public GroupModels.GroupSessionList data;
        public static UIState loading(){ UIState s=new UIState(); s.loading=true; return s; }
        public static UIState error(String e){ UIState s=new UIState(); s.error=e; return s; }
        public static UIState data(GroupModels.GroupSessionList d){ UIState s=new UIState(); s.data=d; return s; }
    }

    public GroupsViewModel(@NonNull Application app){
        super(app); repo = new GroupRepository(app);
    }

    public LiveData<UIState> getState(){ return state; }

    public void load(){
        load(null, null);
    }

    public void load(String ageCategory, String disorderTag){
        state.postValue(UIState.loading());
        repo.list(ageCategory, disorderTag, new GroupRepository.ListCb() {
            @Override public void ok(GroupModels.GroupSessionList list) { state.postValue(UIState.data(list)); }
            @Override public void err(Throwable t) { state.postValue(UIState.error(t.getMessage())); }
        });
    }
}
