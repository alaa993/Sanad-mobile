package com.brightpath.sanad.feature.groups;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class GroupDetailViewModel extends AndroidViewModel {
    private final GroupRepository repo;
    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.loading());
    private int groupId = -1;

    public static class UIState {
        public boolean loading;
        public String error;
        public GroupModels.GroupSession data;
        public static UIState loading(){ UIState s=new UIState(); s.loading=true; return s; }
        public static UIState error(String e){ UIState s=new UIState(); s.error=e; return s; }
        public static UIState data(GroupModels.GroupSession d){ UIState s=new UIState(); s.data=d; return s; }
    }

    public GroupDetailViewModel(@NonNull Application app){ super(app); repo = new GroupRepository(app); }

    public LiveData<UIState> getState(){ return state; }

    public void setGroupId(int id){ this.groupId = id; }

    public void load(){
        if (groupId < 0){ state.postValue(UIState.error("missing_group_id")); return; }
        state.postValue(UIState.loading());
        repo.detail(groupId, new GroupRepository.DetailCb() {
            @Override public void ok(GroupModels.GroupSession g) { state.postValue(UIState.data(g)); }
            @Override public void err(Throwable t) { state.postValue(UIState.error(t.getMessage())); }
        });
    }

    public void join(){
        if (groupId < 0) return;
        state.postValue(UIState.loading());
        repo.join(groupId, new GroupRepository.DetailCb() {
            @Override public void ok(GroupModels.GroupSession g) { state.postValue(UIState.data(g)); }
            @Override public void err(Throwable t) { state.postValue(UIState.error(t.getMessage())); }
        });
    }

    public void leave(){
        if (groupId < 0) return;
        state.postValue(UIState.loading());
        repo.leave(groupId, new GroupRepository.DetailCb() {
            @Override public void ok(GroupModels.GroupSession g) { state.postValue(UIState.data(g)); }
            @Override public void err(Throwable t) { state.postValue(UIState.error(t.getMessage())); }
        });
    }
}
