package com.brightpath.sanad.data;

import android.content.Context;
import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatalogRepository {
    private final CatalogApi api;

    public CatalogRepository(Context ctx) {
        api = ApiClient.get(ctx).create(CatalogApi.class);
    }

    public interface Cb { void ok(CatalogModels.Catalog c); void err(Throwable t); }

    public void load(Cb cb) {
        api.getCatalog().enqueue(new Callback<CatalogModels.Catalog>() {
            @Override public void onResponse(@NonNull Call<CatalogModels.Catalog> call, @NonNull Response<CatalogModels.Catalog> r) {
                if (!r.isSuccessful() || r.body() == null) { cb.err(new RuntimeException("HTTP " + r.code())); return; }
                cb.ok(r.body());
            }
            @Override public void onFailure(@NonNull Call<CatalogModels.Catalog> call, @NonNull Throwable t) { cb.err(t); }
        });
    }
}
