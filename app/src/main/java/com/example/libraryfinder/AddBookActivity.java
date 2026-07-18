package com.example.libraryfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.libraryfinder.data.Book;  // Updated to repository (change to data if not renamed)
import com.example.libraryfinder.data.BookDao;  // Updated to repository (change to data if not renamed)
import com.example.libraryfinder.data.LibraryDatabase;  // Updated to repository (change to data if not renamed)

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddBookActivity extends AppCompatActivity {
    private EditText inputTitle, inputAuthor, inputCategory, inputDescription;
    private Button submitButton, backButton;
    private BookDao dao;
    private CrudApiService crudApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book);

        LibraryDatabase database = LibraryDatabase.getDatabase(this);
        dao = database.bookDao();

        // CRUD API Retrofit
        Retrofit crudRetrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")  // Change to PC IP for real device
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        crudApiService = crudRetrofit.create(CrudApiService.class);

        inputTitle = findViewById(R.id.inputBookTitle);
        inputAuthor = findViewById(R.id.inputBookAuthor);
        inputCategory = findViewById(R.id.inputBookCategory);
        inputDescription = findViewById(R.id.inputBookDescription);
        submitButton = findViewById(R.id.buttonSubmitBook);
        backButton = findViewById(R.id.btn_back);

        submitButton.setOnClickListener(v -> {
            String title = inputTitle.getText().toString().trim();
            String author = inputAuthor.getText().toString().trim();
            String category = inputCategory.getText().toString().trim();
            String description = inputDescription.getText().toString().trim();

            if (title.isEmpty()) {
                inputTitle.setError("Title is required");
                return;
            }
            if (author.isEmpty()) {
                inputAuthor.setError("Author is required");
                return;
            }
            List<String> validCategories = Arrays.asList("Fiction", "Non-fiction", "Sci-Fi");
            if (!validCategories.stream().anyMatch(cat -> cat.equalsIgnoreCase(category))) {
                inputCategory.setError("Valid categories: Fiction, Non-fiction, or Sci-Fi");
                return;
            }

            Book newBook = new Book(null, title, author, category, description, false);  // Set ID to null for auto-generation
            dao.insert(newBook);  // Local save
            Toast.makeText(this, "Book added locally!", Toast.LENGTH_SHORT).show();

            // Send to CRUD API with debug
            Call<Book> call = crudApiService.createBook(newBook);
            call.enqueue(new Callback<Book>() {
                @Override
                public void onResponse(Call<Book> call, Response<Book> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddBookActivity.this, "Book also added to server! Check Postman.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(AddBookActivity.this, "Server failed: " + response.code() + " - " + response.message(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Book> call, Throwable t) {
                    Toast.makeText(AddBookActivity.this, "Server connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            inputTitle.setText("");
            inputAuthor.setText("");
            inputCategory.setText("");
            inputDescription.setText("");
            setResult(RESULT_OK);
            finish();
        });

        backButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}
