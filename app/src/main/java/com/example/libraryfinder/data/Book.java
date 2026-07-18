package com.example.libraryfinder.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "book")
public class Book implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public Long id = 0L;  // Changed to Long (wrapper) to allow null

    public String title;
    public String author;
    public String category;
    public String description;
    public boolean isFav = false;

    // No-argument constructor (added to fix the error)
    public Book() {}

    public Book(Long id, String title, String author, String category, String description, boolean isFav) {  // Changed id to Long
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.description = description;
        this.isFav = isFav;
    }

    // Getters
    public Long getId() { return id; }  // Changed to Long
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public boolean isFav() { return isFav; }

    // Setters
    public void setId(Long id) { this.id = id; }  // Changed to Long
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setFav(boolean isFav) { this.isFav = isFav; }

    public void setFavorite(boolean isFav) { this.isFav = isFav; }
}