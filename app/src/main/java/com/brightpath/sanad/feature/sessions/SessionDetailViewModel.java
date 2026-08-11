package com.brightpath.sanad.feature.sessions;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;

public class SessionDetailViewModel extends AndroidViewModel {
    private final SessionsRepository repo;
    public SessionDetailViewModel(@NonNull Application app){ super(app); repo=new SessionsRepository(app); }

    public static class UIState {
        public boolean loading; public String error; public SessionModels.Session data;
        public static UIState loading(){ UIState s=new UIState(); s.loading=true; return s; }
        public static UIState error(String e){ UIState s=new UIState(); s.error=e; return s; }
        public static UIState data(SessionModels.Session d){ UIState s=new UIState(); s.data=d; return s; }
    }

    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.loading());
    public LiveData<UIState> getState(){ return state; }

    public void load(int id){
        state.postValue(UIState.loading());
        repo.show(id, new SessionsRepository.OneListener() {
            @Override public void onSuccess(SessionModels.Session d){ state.postValue(UIState.data(d)); }
            @Override public void onError(Throwable t){ state.postValue(UIState.error(t.getMessage())); }
        });
    }
}
