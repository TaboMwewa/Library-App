package com.example.libraryfinder;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.libraryfinder.data.Book;
import com.example.libraryfinder.data.BookDao;
import com.example.libraryfinder.data.LibraryDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BookDetailsActivity extends AppCompatActivity {
    private Book book;
    private SwitchMaterial favoriteToggle;
    private MaterialButton shareButton;
    private TextView bookTitle;
    private TextView bookAuthor;
    private TextView bookDescription;
    private SharedPreferences prefs;
    private boolean isFavorite;
    private BookDao dao;
    private boolean isApiBook;
    private CrudApiService crudApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_details);

        LibraryDatabase database = LibraryDatabase.getDatabase(this);
        dao = database.bookDao();
        prefs = getSharedPreferences("LibraryPrefs", MODE_PRIVATE);

        // CRUD API Retrofit
        Retrofit crudRetrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        crudApiService = crudRetrofit.create(CrudApiService.class);

        book = (Book) getIntent().getSerializableExtra("book");
        isApiBook = getIntent().getBooleanExtra("isApiBook", false);

        if (book == null) {
            Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bookTitle = findViewById(R.id.bookTitle);
        if (bookTitle != null) bookTitle.setText(book.getTitle());

        bookAuthor = findViewById(R.id.bookAuthor);
        if (bookAuthor != null) bookAuthor.setText(book.getAuthor());

        bookDescription = findViewById(R.id.bookDescription);
        if (bookDescription != null && book.getDescription() != null) {
            bookDescription.setText(book.getDescription());
        } else if (bookDescription != null) {
            bookDescription.setText("No description available");
        }

        favoriteToggle = findViewById(R.id.favoriteToggle);
        if (favoriteToggle != null) {
            loadFavoriteState();
            favoriteToggle.setOnCheckedChangeListener((buttonView, isChecked) -> toggleFavorite(isChecked));
        }

        shareButton = findViewById(R.id.btn_share);
        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, book.getTitle() + " by " + book.getAuthor());
                startActivity(Intent.createChooser(shareIntent, "Share Book"));
            });
        }

        // Admin buttons (visible only for local books)
        Button editButton = findViewById(R.id.btn_edit);
        Button deleteButton = findViewById(R.id.btn_delete);
        if (!isApiBook) {
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            editButton.setOnClickListener(v -> showAdminDialog(() -> showEditDialog()));
            deleteButton.setOnClickListener(v -> showAdminDialog(() -> confirmDelete()));
        } else {
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }

        Button addToCatalogButton = findViewById(R.id.btn_add_to_catalog);
        if (addToCatalogButton != null) {
            if (isApiBook) {
                addToCatalogButton.setVisibility(View.VISIBLE);
                addToCatalogButton.setOnClickListener(v -> {
                    dao.insert(book);
                    Toast.makeText(this, "Book added to catalog!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                addToCatalogButton.setVisibility(View.GONE);
            }
        }
    }

    private void showAdminDialog(Runnable onSuccess) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Only");
        final EditText input = new EditText(this);
        input.setHint("Password");
        builder.setView(input);
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            if ("1234".equals(input.getText().toString())) {
                onSuccess.run();
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Book");

        // Create EditTexts for fields
        final EditText inputTitle = new EditText(this);
        inputTitle.setText(book.getTitle());
        inputTitle.setHint("Title");

        final EditText inputAuthor = new EditText(this);
        inputAuthor.setText(book.getAuthor());
        inputAuthor.setHint("Author");

        final EditText inputCategory = new EditText(this);
        inputCategory.setText(book.getCategory());
        inputCategory.setHint("Category");

        final EditText inputDescription = new EditText(this);
        inputDescription.setText(book.getDescription());
        inputDescription.setHint("Description");

        // Layout for inputs
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(inputTitle);
        layout.addView(inputAuthor);
        layout.addView(inputCategory);
        layout.addView(inputDescription);
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTitle = inputTitle.getText().toString().trim();
            String newAuthor = inputAuthor.getText().toString().trim();
            String newCategory = inputCategory.getText().toString().trim();
            String newDescription = inputDescription.getText().toString().trim();

            if (newTitle.isEmpty() || newAuthor.isEmpty()) {
                Toast.makeText(this, "Title and Author are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update book
            book.setTitle(newTitle);
            book.setAuthor(newAuthor);
            book.setCategory(newCategory);
            book.setDescription(newDescription);
            dao.update(book);

            // Update API if synced
            Call<Book> call = crudApiService.updateBook((long) book.getId(), book);
            call.enqueue(new Callback<Book>() {
                @Override
                public void onResponse(Call<Book> call, Response<Book> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(BookDetailsActivity.this, "Book updated!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BookDetailsActivity.this, "Local update OK, API failed", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Book> call, Throwable t) {
                    Toast.makeText(BookDetailsActivity.this, "Local update OK, API connection failed", Toast.LENGTH_SHORT).show();
                }
            });

            // Refresh UI
            bookTitle.setText(newTitle);
            bookAuthor.setText(newAuthor);
            bookDescription.setText(newDescription);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Book");
        builder.setMessage("Are you sure you want to delete this book?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            dao.delete(book);

            // Delete from API if synced
            Call<Void> call = crudApiService.deleteBook((long) book.getId());
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(BookDetailsActivity.this, "Book deleted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BookDetailsActivity.this, "Local delete OK, API failed", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(BookDetailsActivity.this, "Local delete OK, API connection failed", Toast.LENGTH_SHORT).show();
                }
            });

            finish();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadFavoriteState() {
        Set<String> favoriteIdsFromPrefs = prefs.getStringSet("favorites", new HashSet<>());
        String bookIdStr = String.valueOf(book.getId());
        isFavorite = favoriteIdsFromPrefs.contains(bookIdStr);
        if (favoriteToggle != null) {
            favoriteToggle.setChecked(isFavorite);
            favoriteToggle.setText(isFavorite ? "Remove from Favorites" : "Add to Favorites");
        }
    }

    private void toggleFavorite(boolean isChecked) {
        Set<String> favoriteIds = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
        String bookIdStr = String.valueOf(book.getId());
        if (isChecked) favoriteIds.add(bookIdStr);
        else favoriteIds.remove(bookIdStr);
        prefs.edit().putStringSet("favorites", favoriteIds).apply();
        book.setFavorite(isChecked);
        if (favoriteToggle != null) favoriteToggle.setText(isChecked ? "Remove from Favorites" : "Add to Favorites");
    }

    @Override
    public void finish() {
        setResult(RESULT_OK);
        super.finish();
    }
}