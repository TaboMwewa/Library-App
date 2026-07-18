package com.example.libraryfinder.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookDao {
    @Query("SELECT * FROM book")
    List<Book> getAll();  // New method to get all books

    @Query("SELECT * FROM book WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    List<Book> search(String query);

    @Query("SELECT * FROM book WHERE category = :category")
    List<Book> byCategory(String category);

    @Query("SELECT * FROM book WHERE isFav = 1")
    List<Book> favorites();

    @Query("SELECT * FROM book WHERE id = :id")
    Book getBookById(long id);  // Changed from int to long

    @Insert
    void insert(Book book);

    @Insert
    void insertAll(Book[] books);  // Updated to Book[] for array insertion

    @Update
    void update(Book book);

    @Delete
    void delete(Book book);
}