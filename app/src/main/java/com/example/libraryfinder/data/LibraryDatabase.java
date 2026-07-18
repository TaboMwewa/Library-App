package com.example.libraryfinder.data;  // Ensure this is the correct package

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {Book.class}, version = 2, exportSchema = false)  // Version 2
public abstract class LibraryDatabase extends RoomDatabase {
    public abstract BookDao bookDao();

    private static volatile LibraryDatabase INSTANCE;

    public static LibraryDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (LibraryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    LibraryDatabase.class, "library_database")
                            .allowMainThreadQueries()  // Kept as in your code
                            .fallbackToDestructiveMigration()  // Added: Allows destructive migration
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}