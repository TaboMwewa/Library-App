# Library Finder

An Android app for browsing a library's book catalog, with a lightweight admin mode for managing the collection and syncing with an external Google Books search.

## Features

**For everyone:**
- Browse the book catalog, with title, author, category, and description
- Filter by category (Fiction, Non-fiction, Sci-Fi)
- Search the local catalog
- Mark books as favorites and view them in a dedicated Favorites screen

**Admin mode** (unlocked via a password prompt in the toolbar menu):
- Search Google Books to pull in new titles
- Add new books to the catalog
- Sync the local catalog with a backend server (pull remote books in, push local books up)

## How data is stored

- **Local storage:** Room (SQLite) database on the device — this is the source of truth for what's shown in the catalog.
- **Remote sync:** A CRUD REST API (built separately, running locally during development) that the app can push to and pull from in admin mode.
- **Book discovery:** The Google Books API, used in admin mode to search for and preview new titles before adding them to the catalog.
- **Favorites and admin state:** Stored locally via `SharedPreferences`.

## Tech stack

- Java, Android SDK
- Room (local database)
- Retrofit + Gson (networking, for both the Google Books API and the local CRUD API)
- Material Components (chips, FABs, toolbar)

## Notes

This project was built as a learning exercise and has evolved over time — some pieces (like the CRUD backend) live in a separate project and aren't included here.
