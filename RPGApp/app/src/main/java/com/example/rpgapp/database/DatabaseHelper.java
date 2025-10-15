package com.example.rpgapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper
{

    // Database info
    private static final String DATABASE_NAME = "rpg_app.db";
    private static final int DATABASE_VERSION = 2; // Povećana verzija zbog dodavanja active_days_streak kolone

    // User table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_USERNAME = "username";
    public static final String COLUMN_USER_AVATAR_ID = "avatar_id";
    public static final String COLUMN_USER_LEVEL = "level";
    public static final String COLUMN_USER_TITLE = "title";
    public static final String COLUMN_USER_POWER_POINTS = "power_points";
    public static final String COLUMN_USER_EXPERIENCE_POINTS = "experience_points";
    public static final String COLUMN_USER_COINS = "coins";
    public static final String COLUMN_USER_EMAIL_VERIFIED = "email_verified";
    public static final String COLUMN_USER_REGISTRATION_DATE = "registration_date";
    public static final String COLUMN_USER_LAST_LOGIN = "last_login";
    public static final String COLUMN_USER_ACTIVE_DAYS_STREAK = "active_days_streak";

    // Create table statements
    private static final String CREATE_USER_TABLE =
        "CREATE TABLE " + TABLE_USERS + " (" +
        COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
        COLUMN_USER_EMAIL + " TEXT UNIQUE NOT NULL, " +
        COLUMN_USER_USERNAME + " TEXT UNIQUE NOT NULL, " +
        COLUMN_USER_AVATAR_ID + " TEXT NOT NULL, " +
        COLUMN_USER_LEVEL + " INTEGER DEFAULT 0, " +
        COLUMN_USER_TITLE + " TEXT DEFAULT 'Crook', " +
        COLUMN_USER_POWER_POINTS + " INTEGER DEFAULT 0, " +
        COLUMN_USER_EXPERIENCE_POINTS + " INTEGER DEFAULT 0, " +
        COLUMN_USER_COINS + " INTEGER DEFAULT 0, " +
        COLUMN_USER_EMAIL_VERIFIED + " INTEGER DEFAULT 0, " +
        COLUMN_USER_REGISTRATION_DATE + " INTEGER, " +
        COLUMN_USER_ACTIVE_DAYS_STREAK + " INTEGER DEFAULT 0," +
        COLUMN_USER_LAST_LOGIN + " INTEGER" +
        ")";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(CREATE_USER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        if (oldVersion < 2 && newVersion >= 2)
        {
            // Dodaj novu kolonu active_days_streak u postojeću tabelu
            db.execSQL("ALTER TABLE " + TABLE_USERS +
                      " ADD COLUMN " + COLUMN_USER_ACTIVE_DAYS_STREAK + " INTEGER DEFAULT 0");
        }

        // Za sve ostale verzije, obriši i ponovo kreiraj tabele
        if (oldVersion < newVersion && oldVersion != 1)
        {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db)
    {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}
