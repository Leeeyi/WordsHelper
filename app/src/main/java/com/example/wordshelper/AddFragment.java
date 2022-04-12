package com.example.wordshelper;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AddFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AddFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddFragment newInstance(String param1, String param2) {
        AddFragment fragment = new AddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    private EditText editC, editE;

    private MaterialButton button;

    private MyViewModel viewModel;

    private  InputMethodManager inm;

    private String apiChinese;

    private String uriMethod;

    private MyHandler myhandler;

    private String chinese;

    private String english;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        apiChinese = null;
        viewModel = new MyViewModel();
        viewModel.init(requireActivity());
        editC = requireActivity().findViewById(R.id.editChinese);
        editE = requireActivity().findViewById(R.id.editEnglish);
        button = requireActivity().findViewById(R.id.btnadd);
        button.setEnabled(false);
        inm =(InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        /**
         * 在输入的时候监听EditText的状态
         */
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String chi = editC.getText().toString().trim();
                String eng = editE.getText().toString().trim();
                if(!chi.isEmpty()&&!eng.isEmpty()){
                    button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        editE.addTextChangedListener(textWatcher);
        editC.addTextChangedListener(textWatcher);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chinese = editC.getText().toString().trim();
                english = editE.getText().toString().trim();
                getApiChinese(english);
            }
        });
    }

    @Override
    public void onStop() { // 在navigate()调用的时候确实执行了这个回调
        super.onStop();
        inm.hideSoftInputFromWindow(getView().getWindowToken(),0);
    }

    class MyHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case 1:
                    Word word = new Word();
                    word.setEnglish(english);
                    word.setChinese(chinese);
                    word.setInv_chinese(true);
                    String json = (String) msg.obj;
                    if(json==null){
                        Toast.makeText(requireActivity(),"无网络，请打开网络",Toast.LENGTH_SHORT).show();
                        word.setApiChinese("暂无网络释义");
                        insertWithoutApi(word);
                    }else{
                        insertWithApi(json,word);
                    }
                    break;
            }
        }
    }

    /**
     * 这个的方法在于去网上的词典获取更多的单词解释，我这里用的是天行数据的API，一天最多一百个，你们可以照着用，不改我的数据
     * 也可以考虑自己弄一个API去搞单词解释，本质就是请求后处理一下返回结果就好了
     * @param english
     */
    void getApiChinese(String english){
        OkHttpClient client = new OkHttpClient.Builder().build();
        uriMethod = "https://api.tianapi.com/txapi/enwords/index?key=APIKEY&word=lexicon";
        uriMethod =  uriMethod.replaceAll("APIKEY","1f45fa4996a09a50a0caff1b5c7cd5bf");
        uriMethod = uriMethod.replaceAll("lexicon",english);
        myhandler = new MyHandler();
        /**
         * 我在一个子线程下去跑OkHTTP的同步请求流程，得到的结果发送到handler下处理
         * 得到的内容处理在Handler的handleMessage下处理
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = Message.obtain();
                message.what = 1;
                Request request = new Request.Builder().url(uriMethod).get().build();
                Response response = null;
                try {
                    response = client.newCall(request).execute(); // 同步请求
                    message.obj = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                myhandler.sendMessage(message);
            }
        }).start();
    }


    /**
     * 老Api不一定能查到东西，所以我区分了两种情况
     * @param json
     * @param word
     */

    void insertWithApi(String json,Word word){
        Gson gson = new Gson();
        Data data = gson.fromJson(json,Data.class);
        List<NewsList> newsList = data.getNewslist();
        String content;
        if(newsList==null){
            content = "暂无网络释义";
        }else{
            content = newsList.get(0).getContent();
        }
        word.setApiChinese(content);
        Log.d("看这里",content);
        insertWithoutApi(word);
    }

    void insertWithoutApi(Word word){
        viewModel.insert(word);
        viewModel.setIsRecyclerview(true);
        NavController controller = Navigation.findNavController(button);
        controller.navigateUp();
    }

}