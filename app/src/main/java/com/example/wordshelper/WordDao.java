package com.example.wordshelper;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WordDao {

    @Insert
    public void insert(Word word);

    @Update
    public void update(Word word);

    @Delete
    public void delete(Word word);

    @Query("DELETE FROM Word;")
    public void deleteAll();

    @Query("SELECT * FROM Word ORDER BY id DESC")
    public LiveData<List<Word>> queryAll();


    @Query("SELECT * FROM Word WHERE id = :id ORDER BY id DESC ")
    public LiveData<Word> queryByid(int id);

    @Query("DELETE FROM sqlite_sequence")
    public void clear();

    @Query("SELECT * FROM Word WHERE English LIKE :english ORDER BY id DESC")
    LiveData<List<Word>> queryEnglish(String english);

    @Query("SELECT * FROM Word WHERE Chinese LIKE :chinese ORDER BY id DESC")
    LiveData<List<Word>> queryChinese(String chinese);
}
