package com.brightpath.sanad.data;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CatalogApi {
    @GET("api/v1/catalog")
    Call<CatalogModels.Catalog> getCatalog();
}
