package com.example.wordshelper;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

/**
 * 串联着仓库层，对于Room的操作的方法都在这里进行分发
 */
public class MyViewModel  extends ViewModel {

    private LiveData<List<Word>> liveWordlist;

    private LiveData<Word> liveWord;

    private LiveData<List<Word>> englishList;

    private LiveData<List<Word>> chineseList;

    private MutableLiveData<Boolean> isRecyclerview;

    private WordDepository depository;

       void init(Context context){
           depository = new WordDepository(context);
           chineseList = new MutableLiveData<>();
           englishList = new MutableLiveData<>();
           liveWordlist = depository.queryAll();
           isRecyclerview = new MutableLiveData<>();
           isRecyclerview.setValue(false);
       }

       void insert(Word word){
           depository.insert(word);
       }

       void update(Word word){
           depository.update(word);
       }

       void delete(Word word){
           depository.delete(word);
       }

       void deleteAll(){
           depository.deleteAll();
       }

       LiveData<List<Word>> getList(){
           return liveWordlist;
       }

       LiveData<Boolean> getIsRecyclerview(){return isRecyclerview;}

       void setIsRecyclerview(boolean b){
           isRecyclerview.setValue(b);
       }

    LiveData<List<Word>> queryChinese(String chinese){
        chineseList  = depository.queryChinese(chinese);
        return chineseList;
    }

    LiveData<List<Word>> queryEnglish(String english){
        englishList  = depository.queryEnglish(english);
        return englishList;
    }
}
