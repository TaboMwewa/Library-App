package com.example.libraryfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.libraryfinder.data.Book;
import com.example.libraryfinder.data.BookDao;
import com.example.libraryfinder.data.LibraryDatabase;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BookAdapter adapter;
    private List<Book> favoriteBooks;
    private BookDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        LibraryDatabase database = LibraryDatabase.getDatabase(this);
        dao = database.bookDao();

        recyclerView = findViewById(R.id.rv_favorites);  // Fixed: Matches layout ID
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new BookAdapter(book -> {
                Intent intent = new Intent(this, BookDetailsActivity.class);
                intent.putExtra("book", book);
                intent.putExtra("isApiBook", false);
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }

        loadFavorites();
    }

    private void loadFavorites() {
        List<String> favoriteIds = new ArrayList<>(getIntent().getStringArrayListExtra("favorite_ids"));
        if (favoriteIds != null && !favoriteIds.isEmpty()) {
            favoriteBooks = new ArrayList<>();
            for (String idStr : favoriteIds) {
                try {
                    long id = Long.parseLong(idStr);  // id is long
                    Book book = dao.getBookById(id);  // Now accepts long
                    if (book != null) {
                        favoriteBooks.add(book);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid IDs
                }
            }
            if (adapter != null) {
                adapter.updateBooks(favoriteBooks);
            }
        } else {
            Toast.makeText(this, "No favorites found", Toast.LENGTH_SHORT).show();
        }
    }
}