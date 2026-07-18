package com.example.libraryfinder;

import com.example.libraryfinder.data.Book;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface CrudApiService {
    @GET("api/book")
    Call<List<Book>> getAllBooks();

    @PUT("api/book")
    Call<Book> createBook(@Body Book book);

    @POST("api/book/{id}")
    Call<Book> updateBook(@Path("id") Long id, @Body Book book);

    @DELETE("api/book/{id}")
    Call<Void> deleteBook(@Path("id") Long id);
}