package com.example.rpgapp.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.example.rpgapp.model.User;
import java.util.Date;

public class UserDao
{

    private DatabaseHelper dbHelper;

    public UserDao(DatabaseHelper dbHelper)
    {
        this.dbHelper = dbHelper;
    }

    public long insertUser(User user)
    {
        Log.d("UserDao", "insertUser called for user: " + user.getEmail());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d("UserDao", "SQLite database opened for writing");
        long result = -1;

        try
        {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_ID, user.getId());
            values.put(DatabaseHelper.COLUMN_USER_EMAIL, user.getEmail());
            values.put(DatabaseHelper.COLUMN_USER_USERNAME, user.getUsername());
            values.put(DatabaseHelper.COLUMN_USER_AVATAR_ID, user.getAvatarId());
            values.put(DatabaseHelper.COLUMN_USER_LEVEL, user.getLevel());
            values.put(DatabaseHelper.COLUMN_USER_TITLE, user.getTitle());
            values.put(DatabaseHelper.COLUMN_USER_POWER_POINTS, user.getPowerPoints());
            values.put(DatabaseHelper.COLUMN_USER_EXPERIENCE_POINTS, user.getExperiencePoints());
            values.put(DatabaseHelper.COLUMN_USER_COINS, user.getCoins());
            values.put(DatabaseHelper.COLUMN_USER_EMAIL_VERIFIED, user.isEmailVerified() ? 1 : 0);
            values.put(DatabaseHelper.COLUMN_USER_REGISTRATION_DATE, user.getRegistrationDate() != null ? user.getRegistrationDate().getTime() : System.currentTimeMillis());
            values.put(DatabaseHelper.COLUMN_USER_LAST_LOGIN, user.getLastLogin() != null ? user.getLastLogin().getTime() : null);

            Log.d("UserDao", "ContentValues prepared, executing INSERT into " + DatabaseHelper.TABLE_USERS);
            result = db.insert(DatabaseHelper.TABLE_USERS, null, values);
            Log.d("UserDao", "INSERT result: " + result + " (rowId if success, -1 if failed)");
        } catch (SQLiteException e)
        {
            Log.e("UserDao", "SQLiteException during insertUser: " + e.getMessage(), e);
            e.printStackTrace();
        } finally
        {
            db.close();
            Log.d("UserDao", "SQLite database closed");
        }

        return result;
    }

    public User getUserById(String userId)
    {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;

        try
        {
            String selection = DatabaseHelper.COLUMN_USER_ID + " = ?";
            String[] selectionArgs = {userId};

            Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst())
            {
                user = createUserFromCursor(cursor);
                cursor.close();
            }
        } catch (SQLiteException e)
        {
            e.printStackTrace();
        } finally
        {
            db.close();
        }

        return user;
    }

    public User getUserByEmail(String email)
    {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;

        try
        {
            String selection = DatabaseHelper.COLUMN_USER_EMAIL + " = ?";
            String[] selectionArgs = {email};

            Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst())
            {
                user = createUserFromCursor(cursor);
                cursor.close();
            }
        } catch (SQLiteException e)
        {
            e.printStackTrace();
        } finally
        {
            db.close();
        }

        return user;
    }
    public User getUserByUsername(String username)
    {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;

        try
        {
            String selection = DatabaseHelper.COLUMN_USER_USERNAME + " = ?";
            String[] selectionArgs = {username};
            Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst())
            {
                user = createUserFromCursor(cursor);
                cursor.close();
            }
        } catch (SQLiteException e)
        {
            e.printStackTrace();
        } finally {
            db.close();
        }
        return user;
    }

    public boolean isUsernameExists(String username)
    {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean exists = false;

        try
        {
            String selection = DatabaseHelper.COLUMN_USER_USERNAME + " = ?";
            String[] selectionArgs = {username};

            Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, new String[]{DatabaseHelper.COLUMN_USER_ID}, selection, selectionArgs, null, null, null);

            exists = cursor != null && cursor.getCount() > 0;

            if (cursor != null)
            {
                cursor.close();
            }
        } catch (SQLiteException e)
        {
            e.printStackTrace();
        } finally
        {
            db.close();
        }

        return exists;
    }

    public boolean isEmailExists(String email)
    {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean exists = false;

        try
        {
            String selection = DatabaseHelper.COLUMN_USER_EMAIL + " = ?";
            String[] selectionArgs = {email};

            Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, new String[]{DatabaseHelper.COLUMN_USER_ID}, selection, selectionArgs, null, null, null);

            exists = cursor != null && cursor.getCount() > 0;

            if (cursor != null)
            {
                cursor.close();
            }
        } catch (SQLiteException e)
        {
            e.printStackTrace();
        } finally
        {
            db.close();
        }

        return exists;
    }

    public int updateUser(User user)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = 0;

        try
        {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_EMAIL, user.getEmail());
            values.put(DatabaseHelper.COLUMN_USER_USERNAME, user.getUsername());
            values.put(DatabaseHelper.COLUMN_USER_AVATAR_ID, user.getAvatarId());
            values.put(DatabaseHelper.COLUMN_USER_LEVEL, user.getLevel());
            values.put(DatabaseHelper.COLUMN_USER_TITLE, user.getTitle());
            values.put(DatabaseHelper.COLUMN_USER_POWER_POINTS, user.getPowerPoints());
            values.put(DatabaseHelper.COLUMN_USER_EXPERIENCE_POINTS, user.getExperiencePoints());
            values.put(DatabaseHelper.COLUMN_USER_COINS, user.getCoins());
            values.put(DatabaseHelper.COLUMN_USER_EMAIL_VERIFIED, user.isEmailVerified() ? 1 : 0);
            values.put(DatabaseHelper.COLUMN_USER_LAST_LOGIN, user.getLastLogin() != null ? user.getLastLogin().getTime() : null);

            String whereClause = DatabaseHelper.COLUMN_USER_ID + " = ?";
            String[] whereArgs = {user.getId()};

            rowsAffected = db.update(DatabaseHelper.TABLE_USERS, values, whereClause, whereArgs);
        } catch (SQLiteException e)
        {
            e.printStackTrace();
        } finally
        {
            db.close();
        }

        return rowsAffected;
    }

    public void updateLastLogin(String userId)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try
        {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_LAST_LOGIN, System.currentTimeMillis());

            String whereClause = DatabaseHelper.COLUMN_USER_ID + " = ?";
            String[] whereArgs = {userId};

            db.update(DatabaseHelper.TABLE_USERS, values, whereClause, whereArgs);
        } catch (SQLiteException e)
        {
            e.printStackTrace();
        } finally
        {
            db.close();
        }
    }

    public void updateEmailVerification(String userId, boolean isVerified)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try
        {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_EMAIL_VERIFIED, isVerified ? 1 : 0);

            String whereClause = DatabaseHelper.COLUMN_USER_ID + " = ?";
            String[] whereArgs = {userId};

            db.update(DatabaseHelper.TABLE_USERS, values, whereClause, whereArgs);
        } catch (SQLiteException e)
        {
            e.printStackTrace();
        } finally
        {
            // DON'T CLOSE DB - keepAliveDb stays open
            Log.d("UserDao", "keepAliveDb left open for Database Inspector");
        }
    }

    private User createUserFromCursor(Cursor cursor)
    {
        User user = new User();

        user.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_USERNAME)));
        user.setAvatarId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_AVATAR_ID)));
        user.setLevel(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_LEVEL)));
        user.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_TITLE)));
        user.setPowerPoints(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_POWER_POINTS)));
        user.setExperiencePoints(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EXPERIENCE_POINTS)));
        user.setCoins(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_COINS)));
        user.setEmailVerified(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL_VERIFIED)) == 1);

        long regDate = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_REGISTRATION_DATE));
        user.setRegistrationDate(new Date(regDate));

        int lastLoginColumnIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_LAST_LOGIN);
        if (!cursor.isNull(lastLoginColumnIndex))
        {
            long lastLogin = cursor.getLong(lastLoginColumnIndex);
            user.setLastLogin(new Date(lastLogin));
        }

        return user;
    }

}
