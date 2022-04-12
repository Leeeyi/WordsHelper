package com.example.wordshelper;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Word.class}, version = 2, exportSchema = false)
public abstract class WordDataBase extends RoomDatabase {
    private static WordDataBase INSTANCE;
    public synchronized static  WordDataBase getDatabase(Context context){
        if(INSTANCE==null){
            INSTANCE = Room.databaseBuilder(context,WordDataBase.class,"Word_DataBase")
                    .fallbackToDestructiveMigration().build();
        }
        return INSTANCE;
    }
    public abstract WordDao getDao();
}
