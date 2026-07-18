package com.example.libraryfinder;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BookApiResponse {
    @SerializedName("items")
    public List<BookItem> items;
}