@echo off
echo ========================================
echo   SQLite Database Cleaner for RPGApp
echo ========================================
echo.

echo [1] Deleting SQLite database from device...
adb shell "run-as com.example.rpgapp rm /data/data/com.example.rpgapp/databases/rpg_app.db"
adb shell "run-as com.example.rpgapp rm /data/data/com.example.rpgapp/databases/rpg_app.db-shm"
adb shell "run-as com.example.rpgapp rm /data/data/com.example.rpgapp/databases/rpg_app.db-wal"

echo.
echo [2] Database deleted successfully!
echo.
echo [3] Now restart your app to create a fresh database.
echo.
pause

