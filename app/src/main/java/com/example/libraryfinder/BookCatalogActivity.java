package com.example.libraryfinder;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.libraryfinder.data.Book;  // Updated to repository
import com.example.libraryfinder.data.BookDao;  // Updated to repository
import com.example.libraryfinder.data.LibraryDatabase;  // Updated to repository

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.libraryfinder.BuildConfig;

public class BookCatalogActivity extends AppCompatActivity {
    private List<Book> allBooks = new ArrayList<>();
    private List<Book> displayedBooks = new ArrayList<>();
    private BookAdapter adapter;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private SharedPreferences prefs;
    private Set<String> favoriteIds;
    private String currentFilter = "";
    private ActivityResultLauncher<Intent> resultLauncher;
    private BookDao dao;
    private ApiService apiService;
    private CrudApiService crudApiService;
    private String API_KEY = BuildConfig.GOOGLE_BOOKS_API_KEY;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_catalog);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Log.d("Admin", "Toolbar set successfully");
        } else {
            Log.e("Admin", "Toolbar not found in layout");
        }

        LibraryDatabase database = LibraryDatabase.getDatabase(this);
        dao = database.bookDao();

        Retrofit googleRetrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/books/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = googleRetrofit.create(ApiService.class);

        Retrofit crudRetrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        crudApiService = crudRetrofit.create(CrudApiService.class);

        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Log.d("LibraryCatalog", "Result OK from AddBook, reloading books");
                loadBooksFromRoom();
                updateDisplayedBooks();
            }
        });

        prefs = getSharedPreferences("LibraryPrefs", MODE_PRIVATE);
        isAdmin = prefs.getBoolean("isAdmin", false);
        Log.d("Admin", "onCreate - isAdmin: " + isAdmin);  // Debug log

        searchView = findViewById(R.id.searchView);
        if (searchView != null) {
            searchView.setIconified(false);
            searchView.setIconifiedByDefault(false);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (isAdmin) {
                        searchBooksAPI(query);
                    } else {
                        Toast.makeText(BookCatalogActivity.this, "Admin only feature", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText.length() > 2 && isAdmin) {
                        searchBooksAPI(newText);
                    } else if (!isAdmin) {
                        loadBooksFromRoom();
                        updateDisplayedBooks();
                        Toast.makeText(BookCatalogActivity.this, "Admin only feature", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
        }

        loadBooksFromRoom();
        if (allBooks.isEmpty()) {
            Book sample1 = new Book(1L, "The Great Gatsby", "F. Scott Fitzgerald", "Fiction", "Description", false);
            Book sample2 = new Book(2L, "Sapiens", "Yuval Noah Harari", "Non-fiction", "Description", false);
            Book sample3 = new Book(3L, "Dune", "Frank Herbert", "Sci-Fi", "Description", false);
            dao.insertAll(new Book[]{sample1, sample2, sample3});
            loadBooksFromRoom();
        }

        recyclerView = findViewById(R.id.bookRecyclerView);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new BookAdapter(book -> {
                Intent intent = new Intent(this, BookDetailsActivity.class);
                intent.putExtra("book", book);
                intent.putExtra("isApiBook", book.getId() == 0);
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
            updateDisplayedBooks();
        }

        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.categoryChipGroup);
        if (chipGroup != null) {
            chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chip_fiction) currentFilter = "Fiction";
                else if (checkedId == R.id.chip_nonfiction) currentFilter = "Non-fiction";
                else if (checkedId == R.id.chip_scifi) currentFilter = "Sci-Fi";
                else currentFilter = "";
                String currentQuery = searchView != null && searchView.getQuery() != null ? searchView.getQuery().toString() : "";
                filterBooks(currentQuery, currentFilter);
            });
        }

        // FABs - Visibility controlled by isAdmin
        FloatingActionButton fabAddBook = findViewById(R.id.fab_add_book);
        if (fabAddBook != null) {
            fabAddBook.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            Log.d("Admin", "fab_add_book visibility: " + (isAdmin ? "VISIBLE" : "GONE"));  // Debug log
            fabAddBook.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddBookActivity.class);
                resultLauncher.launch(intent);
            });
        }

        FloatingActionButton fabFavorites = findViewById(R.id.fab_favorites);
        if (fabFavorites != null) {
            fabFavorites.setOnClickListener(v -> {
                loadFavorites();
                Intent intent = new Intent(this, FavoritesActivity.class);
                intent.putExtra("favorite_ids", new ArrayList<>(favoriteIds));
                startActivity(intent);
            });
        }

        FloatingActionButton fabSync = findViewById(R.id.fab_sync);
        if (fabSync != null) {
            fabSync.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            Log.d("Admin", "fab_sync visibility: " + (isAdmin ? "VISIBLE" : "GONE"));  // Debug log
            fabSync.setOnClickListener(v -> fetchBooksFromAPI());
        }

        FloatingActionButton fabSaveToServer = findViewById(R.id.fab_save_to_server);
        if (fabSaveToServer != null) {
            fabSaveToServer.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            Log.d("Admin", "fab_save_to_server visibility: " + (isAdmin ? "VISIBLE" : "GONE"));  // Debug log
            fabSaveToServer.setOnClickListener(v -> saveAllToServer());
        }

        loadFavorites();
    }

    private void loadBooksFromRoom() {
        allBooks = dao.getAll();
    }

    private void filterBooks(String query, String category) {
        if (query == null) query = "";
        String finalQuery = query.toLowerCase();
        displayedBooks = allBooks.stream()
                .filter(book -> book.getTitle().toLowerCase().contains(finalQuery) || book.getAuthor().toLowerCase().contains(finalQuery))
                .filter(book -> category.isEmpty() || book.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
        if (adapter != null) adapter.updateBooks(displayedBooks);
    }

    private void updateDisplayedBooks() {
        String currentQuery = searchView != null && searchView.getQuery() != null ? searchView.getQuery().toString() : "";
        filterBooks(currentQuery, currentFilter);
    }

    private void loadFavorites() {
        favoriteIds = prefs.getStringSet("favorites", new HashSet<>());
        for (Book book : allBooks) book.setFavorite(favoriteIds.contains(String.valueOf(book.getId())));
        updateDisplayedBooks();
    }

    private void searchBooksAPI(String query) {
        Call<BookApiResponse> call = apiService.searchBooks(query, API_KEY);
        call.enqueue(new Callback<BookApiResponse>() {
            @Override
            public void onResponse(Call<BookApiResponse> call, Response<BookApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    List<Book> apiBooks = new ArrayList<>();
                    for (BookItem item : response.body().items) {
                        if (item.volumeInfo != null) {
                            String title = item.volumeInfo.title != null ? item.volumeInfo.title : "Unknown Title";
                            String author = (item.volumeInfo.authors != null && item.volumeInfo.authors.length > 0)
                                    ? item.volumeInfo.authors[0] : "Unknown Author";
                            String description = item.volumeInfo.description != null ? item.volumeInfo.description : "No description available";
                            String category = (item.volumeInfo.categories != null && item.volumeInfo.categories.length > 0)
                                    ? item.volumeInfo.categories[0] : "Uncategorized";
                            Book book = new Book(0L, title, author, category, description, false);
                            apiBooks.add(book);
                        }
                    }
                    displayedBooks = new ArrayList<>(allBooks);
                    displayedBooks.addAll(apiBooks);
                    if (adapter != null) adapter.updateBooks(displayedBooks);
                    Toast.makeText(BookCatalogActivity.this, "Search results loaded (tap to view/add)", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("API Error", "Response not successful: " + response.message());
                    Toast.makeText(BookCatalogActivity.this, "API search failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BookApiResponse> call, Throwable t) {
                Log.e("API Error", "Network failure: " + t.getMessage());
                Toast.makeText(BookCatalogActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchBooksFromAPI() {
        Call<List<Book>> call = crudApiService.getAllBooks();
        call.enqueue(new Callback<List<Book>>() {
            @Override
            public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Book> remoteBooks = response.body();
                    new Thread(() -> {
                        for (Book remoteBook : remoteBooks) {
                            // Check if book already exists by title and author to avoid duplicates
                            boolean existsLocally = allBooks.stream().anyMatch(local ->
                                    local.getTitle().equals(remoteBook.getTitle()) &&
                                            local.getAuthor().equals(remoteBook.getAuthor()));
                            if (!existsLocally) {
                                remoteBook.setId(null);  // Set to null for auto-generation
                                dao.insert(remoteBook);
                            }
                        }
                        loadBooksFromRoom();
                        runOnUiThread(() -> {
                            updateDisplayedBooks();
                            Toast.makeText(BookCatalogActivity.this, "Synced " + remoteBooks.size() + " books from server", Toast.LENGTH_SHORT).show();
                        });
                    }).start();

                    new Thread(() -> {
                        List<Book> localBooks = dao.getAll();
                        for (Book localBook : localBooks) {
                            Call<Book> pushCall = crudApiService.createBook(localBook);
                            pushCall.enqueue(new Callback<Book>() {
                                @Override
                                public void onResponse(Call<Book> call, Response<Book> response) {
                                    if (response.isSuccessful()) {
                                        Log.d("Sync", "Pushed book to server: " + localBook.getTitle());
                                    } else {
                                        Log.e("Sync", "Failed to push: " + localBook.getTitle() + " - " + response.message());
                                    }
                                }
                                @Override
                                public void onFailure(Call<Book> call, Throwable t) {
                                    Log.e("Sync", "Push failed for: " + localBook.getTitle());
                                }
                            });
                        }
                    }).start();
                } else {
                    runOnUiThread(() -> Toast.makeText(BookCatalogActivity.this, "Failed to fetch from server", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<List<Book>> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(BookCatalogActivity.this, "Server connection failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveAllToServer() {
        List<Book> localBooks = dao.getAll();
        for (Book book : localBooks) {
            Book apiBook = new Book();  // No-arg constructor
            apiBook.setId(null);  // Now works with Long
            apiBook.setTitle(book.getTitle());
            apiBook.setAuthor(book.getAuthor());
            apiBook.setCategory(book.getCategory());
            apiBook.setDescription(book.getDescription());
            apiBook.setFav(book.isFav());

            Call<Book> call = crudApiService.createBook(apiBook);
            call.enqueue(new Callback<Book>() {
                @Override
                public void onResponse(Call<Book> call, Response<Book> response) {
                    if (response.isSuccessful()) {
                        Log.d("Save", "Book saved to server: " + book.getTitle());
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            Log.e("Save", "Failed to save: " + book.getTitle() + " - Code: " + response.code() + " - Message: " + response.message() + " - Body: " + errorBody);
                        } catch (Exception e) {
                            Log.e("Save", "Failed to read error body: " + e.getMessage());
                        }
                    }
                }
                @Override
                public void onFailure(Call<Book> call, Throwable t) {
                    Log.e("Save", "Connection failed for: " + book.getTitle());
                }
            });
        }
        Toast.makeText(this, "Saving all books to server...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        Log.d("Admin", "Menu inflated");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_admin) {
            Log.d("Admin", "Admin button clicked");
            toggleAdminMode();
            return true;
        } else if (item.getItemId() == R.id.action_favorites) {
            loadFavorites();
            Intent intent = new Intent(this, FavoritesActivity.class);
            intent.putExtra("favorite_ids", new ArrayList<>(favoriteIds));
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleAdminMode() {
        if (isAdmin) {
            prefs.edit().putBoolean("isAdmin", false).apply();
            isAdmin = false;
            recreate();
            Log.d("Admin", "Switched to User mode");
            Toast.makeText(this, "Switched to User mode", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter Admin Password");
            final EditText input = new EditText(this);
            input.setHint("Password");
            builder.setView(input);
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                if ("1234".equals(input.getText().toString())) {
                    prefs.edit().putBoolean("isAdmin", true).apply();
                    isAdmin = true;
                    recreate();
                    Log.d("Admin", "Switched to Admin mode");
                    Toast.makeText(this, "Switched to Admin mode", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
        updateDisplayedBooks();
    }
}