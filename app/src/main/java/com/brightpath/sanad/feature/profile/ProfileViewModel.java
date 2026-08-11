package com.brightpath.sanad.feature.profile;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.auth.AuthRepository;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.models.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileViewModel extends AndroidViewModel {
    private final AuthRepository repo;
    private final TokenStore tokens;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UIState> state = new MutableLiveData<>();
    private final MutableLiveData<String> toast = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application app){
        super(app);
        repo = new AuthRepository(app, AppConfig.BASE_URL);
        tokens = new TokenStore(app);
        User cached = cachedUser();
        state.setValue(cached != null ? UIState.data(cached) : UIState.loading());
    }

    public LiveData<UIState> getState(){ return state; }
    public LiveData<String> getToast(){ return toast; }

    public void load(){
        load(false);
    }

    /** Soft refresh keeps last profile frame visible while me() refreshes. */
    public void load(boolean force){
        UIState current = state.getValue();
        User cached = cachedUser();
        boolean keepFrame = !force
                && ((current != null && current.data != null) || cached != null);
        if (keepFrame) {
            if (current == null || current.data == null) {
                state.postValue(UIState.data(cached));
            }
        } else {
            state.postValue(UIState.loading());
        }
        executor.execute(() -> {
            try {
                User user = repo.me();
                if (user != null) {
                    if (user.name != null) tokens.saveUserName(user.name);
                    if (user.email != null) tokens.saveUserEmail(user.email);
                    if (user.role != null) tokens.saveRole(user.role);
                    if (user.id > 0) tokens.saveUserId(user.id);
                }
                state.postValue(UIState.data(user));
            } catch (com.brightpath.sanad.data.auth.AuthException authEx) {
                // Keep cached profile on screen for active users; SessionGuard /me
                // owns hard logout. Clearing mid-profile caused MIUI force-closes.
                User fallback = current != null && current.data != null ? current.data : cachedUser();
                if (fallback != null) {
                    state.postValue(UIState.data(fallback));
                } else if (authEx.code == 401 || authEx.code == 403 || authEx.code == 404) {
                    tokens.clear();
                    state.postValue(UIState.error(authEx.getMessage()));
                } else {
                    state.postValue(UIState.error(authEx.getMessage()));
                }
            } catch (Exception e){
                User fallback = current != null && current.data != null ? current.data : cachedUser();
                if (fallback != null) {
                    state.postValue(UIState.data(fallback));
                } else {
                    state.postValue(UIState.error(e.getMessage()));
                }
            }
        });
    }

    public void changePassword(String current, String password, String confirm){
        executor.execute(() -> {
            try {
                repo.updatePassword(current, password, confirm);
                toast.postValue("تم تحديث كلمة المرور");
            } catch (Exception e){
                toast.postValue("تعذر تحديث كلمة المرور");
            }
        });
    }

    public void updateProfile(@Nullable String name, @Nullable String locale, @Nullable String phone) {
        executor.execute(() -> {
            try {
                repo.updateProfile(name, locale, phone);
                if (name != null && !name.trim().isEmpty()) {
                    tokens.saveUserName(name.trim());
                }
                toast.postValue("تم حفظ التغييرات");
                load(true);
            } catch (Exception e) {
                toast.postValue("تعذر حفظ التغييرات");
            }
        });
    }

    public void resubmit(@Nullable String role) {
        if (role == null) return;
        executor.execute(() -> {
            try {
                ProfileApi api = com.brightpath.sanad.data.ApiClient.get(getApplication()).create(ProfileApi.class);
                retrofit2.Response<java.util.Map<String, Object>> r;
                if (role.toLowerCase().contains("specialist")) {
                    r = api.resubmitSpecialist().execute();
                } else if (role.toLowerCase().contains("organization")) {
                    r = api.resubmitOrg().execute();
                } else {
                    return;
                }
                if (r.isSuccessful()) {
                    toast.postValue(getApplication().getString(com.brightpath.sanad.R.string.profile_resubmitted));
                    load(true);
                } else {
                    toast.postValue(getApplication().getString(com.brightpath.sanad.R.string.profile_resubmit_failed));
                }
            } catch (Exception e) {
                toast.postValue(getApplication().getString(com.brightpath.sanad.R.string.profile_resubmit_failed));
            }
        });
    }

    @Override protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

    private User cachedUser() {
        String name = tokens.getUserName();
        String email = tokens.getUserEmail();
        String role = tokens.getRole();
        if ((name == null || name.isEmpty()) &&
            (email == null || email.isEmpty()) &&
            (role == null || role.isEmpty())) {
            return null;
        }
        User u = new User();
        u.name = name;
        u.email = email;
        u.role = role;
        return u;
    }

    static class UIState {
        boolean loading;
        String error;
        User data;
        static UIState loading(){ UIState s = new UIState(); s.loading = true; return s; }
        static UIState error(String e){ UIState s = new UIState(); s.error = e; return s; }
        static UIState data(User u){ UIState s = new UIState(); s.data = u; return s; }
    }
}
