package com.brightpath.sanad.feature.sessions;

import android.content.Context;
import androidx.annotation.NonNull;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.ApiClient;
import com.brightpath.sanad.data.ApiErrorParser;
import retrofit2.*;

public class SessionsRepository_Book {
    public static class BookingException extends RuntimeException {
        public final String code;

        public BookingException(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    private final SessionsApi api;
    private final Context context;
    public SessionsRepository_Book(Context ctx){
        context = ctx.getApplicationContext();
        api = ApiClient.get(ctx).create(SessionsApi.class);
    }
    public interface BookListener { void onSuccess(SessionModels.Session d); void onError(Throwable t); }
    public void book(BookRequest req, BookListener l){
        api.book(req).enqueue(new Callback<SessionModels.Session>() {
            @Override public void onResponse(@NonNull Call<SessionModels.Session> c, @NonNull Response<SessionModels.Session> r){
                if (!r.isSuccessful()) {
                    l.onError(readError(r));
                    return;
                }
                SessionModels.Session session = r.body();
                if (session == null) {
                    session = new SessionModels.Session();
                }
                l.onSuccess(session);
            }
            @Override public void onFailure(@NonNull Call<SessionModels.Session> c, @NonNull Throwable t){ l.onError(t); }
        });
    }

    private BookingException readError(Response<?> response) {
        String raw = null;
        try {
            if (response.errorBody() != null) {
                raw = response.errorBody().string();
            }
        } catch (Exception ignored) {
        }
        ApiErrorParser.ParsedError parsed = ApiErrorParser.parseDetailed(
                context,
                R.string.book_session_failed,
                raw,
                response.code()
        );
        return new BookingException(parsed.code, parsed.message);
    }
}
