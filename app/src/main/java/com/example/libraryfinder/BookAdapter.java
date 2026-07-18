package com.example.libraryfinder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.libraryfinder.data.Book;

import java.util.ArrayList;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> books;  // Fixed: was 'book'
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    public BookAdapter(OnBookClickListener listener) {
        this.listener = listener;
        this.books = new ArrayList<>();
    }

    public void updateBooks(List<Book> newBooks) {  // Fixed: was 'updateBook'
        this.books = (newBooks != null) ? newBooks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);  // Fixed: was 'book.get(position)'
        if (holder.titleText != null) holder.titleText.setText(book.getTitle());
        if (holder.authorText != null) holder.authorText.setText(book.getAuthor() + " (" + book.getCategory() + ")");
        holder.itemView.setOnClickListener(v -> listener.onBookClick(book));
    }

    @Override
    public int getItemCount() {
        return books.size();  // Fixed: was 'book.size()'
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, authorText;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(android.R.id.text1);
            authorText = itemView.findViewById(android.R.id.text2);
            if (titleText != null) {
                titleText.setTextSize(18);
                titleText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
            }
            if (authorText != null) {
                authorText.setTextSize(14);
                authorText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
            }
        }
    }
}