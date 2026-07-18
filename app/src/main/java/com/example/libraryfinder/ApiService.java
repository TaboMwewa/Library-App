package com.example.libraryfinder;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import java.util.List;

public interface ApiService {
    @GET("volumes")
    Call<BookApiResponse> searchBooks(@Query("q") String query, @Query("key") String apiKey);
}