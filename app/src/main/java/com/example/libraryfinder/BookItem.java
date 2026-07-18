package com.example.libraryfinder;

import com.google.gson.annotations.SerializedName;

public class BookItem {
    @SerializedName("volumeInfo")
    public VolumeInfo volumeInfo;
}