package com.example.wordshelper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WordsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WordsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";

    private static final String ARG_PARAM2 = "param2";


    private String mParam1;

    private String mParam2;

    private MyViewModel viewModel;

    private RecyclerView recyclerView;

    private LinearLayoutManager manager;

    private MyAdpater adpater_r, adpater_c;

    private List<Word> allWords;

    private LiveData<List<Word>> filterList;

    int oldSize;

    /**
     * 接下来的参数都是讯飞语音用到的参数，特别是2-4个，代码看不懂就回头看
     */
    ConnectivityManager connectivityManager;

    SpeechSynthesizer synthesizer;

    InitListener initListener;

    SynthesizerListener listener;

    NetworkInfo networkInfo;

    public WordsFragment() {
        // Required empty public constructor
    }



    public static WordsFragment newInstance(String param1, String param2) {
        WordsFragment fragment = new WordsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     *  这个是菜单的创建页面
     * @param menu menu 资源文件
     * @param inflater 系统传参
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu,menu);
        InputMethodManager inm =(InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        inm.showSoftInput(searchView,0);

        /**
         *  下面这个部分就是搜索栏模糊匹配的逻辑，重点是第二个重写方法
         *  filterList是一个LiveData
         *  这里设置了两种adapter不停切换是因为可以对应不同的样式
         *  其实有更好的方法解决这个，一个adapter就能做到，或者你们可以不做样式的切换
         */
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                newText = newText.trim();
                filterList.removeObservers(requireActivity());
                if(newText.matches("[\u4e00-\u9fa5]+")){
                    filterList = viewModel.queryChinese(newText);
                }else if(newText.matches("[a-zA-Z]+")){
                    filterList = viewModel.queryEnglish(newText);
                }else{
                    filterList = viewModel.getList();
                }

                filterList.observe(getViewLifecycleOwner(), new Observer<List<Word>>(){
                    int tmp = -1;
                    @Override
                    public void onChanged(List<Word> wordList) {
                        if(tmp==wordList.size()){
                            return;
                        }

                        if(viewModel.getIsRecyclerview().getValue()){
                            MyAdpater adpater = adpater_r;
                            adpater.setList(wordList);
                            changeView_R(adpater,wordList);
                        }else{
                            MyAdpater adpater = adpater_c;
                            adpater.setList(wordList);
                            changeView_C(adpater,wordList);
                        }
                        tmp = wordList.size();
                    }
                });
                inm.hideSoftInputFromWindow(getView().getWindowToken(),0);
                return true;
            }
        });
    }

    /**
     * 菜单其他内容的选中逻辑
     * @param item
     * @return
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.changeItem:
                viewModel.setIsRecyclerview(!viewModel.getIsRecyclerview().getValue());
                break;
            case R.id.deleteItem:
                AlertDialog.Builder builder =  new AlertDialog.Builder(requireActivity());
                builder.setTitle("清空数据");
                builder.setPositiveButton("确定", (dialog, which) -> viewModel.deleteAll());
                builder.setNegativeButton("取消", (dialog, which) -> {

                });
                builder.create();
                builder.show();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        oldSize = -1;
        recyclerView = getActivity().findViewById(R.id.fraRecycler);
        manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(manager);
        filterList = viewModel.getList();
        /**
         * 下面这部分应该是冗余逻辑，去掉可能有关系也可能没关系，去年水平不到家，你们可以自己调试
         */
        filterList.observe(getViewLifecycleOwner(), words -> {
            allWords = words;
            if(viewModel.getIsRecyclerview().getValue()){
                MyAdpater adpater = adpater_r;
                List<Word> wordList = viewModel.getList().getValue();
                if(wordList.size()==oldSize){
                    return;
                }
                adpater.setList(wordList);
                changeView_R(adpater,wordList);
                oldSize = wordList.size();
            }else{
                MyAdpater adpater = adpater_c;
                List<Word> wordList = viewModel.getList().getValue();
                if(wordList.size()==oldSize){
                    return;
                }
                adpater.setList(wordList);
                changeView_C(adpater,wordList);
                oldSize = wordList.size();
            }
        });

        viewModel.getIsRecyclerview().observe(requireActivity(), aBoolean -> {
            MyAdpater adpater;
            List<Word> wordList = viewModel.getList().getValue();
            if(viewModel.getIsRecyclerview().getValue()){
                adpater = adpater_r;
                adpater.setList(wordList);
                changeView_R(adpater, wordList);
            }else{
                adpater = adpater_c;
                adpater.setList(wordList);
                changeView_C(adpater,wordList);
            }

        });

        /**
         * 滑动list删除的逻辑，这里我是在数据库去删除，然后让LiveData去改变，然后改变显示内容，其实可以优化逻辑
         */
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.START|ItemTouchHelper.END) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Word word = allWords.get(viewHolder.getAdapterPosition());
                viewModel.delete(word); // 直接关联数据库删除
                Snackbar.make(requireActivity().findViewById(R.id.wordsFragment),"删除了该词汇",
                        Snackbar.LENGTH_SHORT).setAction("撤销", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                         viewModel.insert(word);
                    }
                }).show();
            }
        }).attachToRecyclerView(recyclerView);

        /**
         * 点击添加按钮的逻辑,用Navigation跳转页面
         */
        FloatingActionButton button = requireActivity().findViewById(R.id.to_add);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController controller = Navigation.findNavController(button);
                controller.navigate(R.id.action_wordsFragment_to_addFragment);
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        /**
         *  304行这个SpeechUtility就是你们安装讯飞语音合成SDK后启动的步骤
         *  但其实不应该在这里进行启动，建议在Activity创建时启动
         *  你们去注册好后填入你们自己的APPID,文档有说明
         */
        SpeechUtility.createUtility(requireActivity(), "Your APPID");

        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        viewModel = new ViewModelProvider(requireActivity(),new ViewModelProvider.NewInstanceFactory()).get(MyViewModel.class);
        viewModel.init(getActivity());
        adpater_r = new MyAdpater(R.layout.word_item_update, viewModel.getList().getValue(),viewModel);
        adpater_c = new MyAdpater(R.layout.card_word_item, new ArrayList<Word>(),viewModel);
        initSeech();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_words, container, false);
    }

    void changeView_R(MyAdpater adpater, List<Word> wordList){
        adpater.addChildClickViewIds(R.id.Layout1);
        adpater.setOnItemChildClickListener(new OnItemChildClickListener() {
            @Override
            public void onItemChildClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
                if (view.getId() == R.id.Layout1) {
                   showBottomSheetDialog(wordList,position);
                }
            }
        });
        recyclerView.setAdapter(adpater);
    }


    void changeView_C(MyAdpater adpater, List<Word> wordList){
        adpater.addChildClickViewIds(R.id.Layout2);
        adpater.setOnItemChildClickListener((adapter, view, position) -> {
            if(view.getId()==R.id.Layout2){
                showBottomSheetDialog(wordList,position);
            }
        });
        recyclerView.setAdapter(adpater);
    }

    /**
     * 单词的详情页面都放在这个BottomSheetDialog里面了
     *
     * @param wordList
     * @param position
     */
    @SuppressLint("SetTextI18n")
    void showBottomSheetDialog(List<Word>wordList, int position){
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog,null);
        TextView textWord = view.findViewById(R.id.wordText);
        TextView textUc = view.findViewById(R.id.userChinese);
        TextView textAc = view.findViewById(R.id.apiChinese);
        textUc.setText(wordList.get(position).getChinese());
        textAc.setText(wordList.get(position).getApiChinese());
        int len = wordList.get(position).getEnglish().length();
        if(len>12){
            textWord.setTextSize(32f);
        }
        textWord.setText(wordList.get(position).getEnglish());
        Button speak = view.findViewById(R.id.button);

        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String english = textWord.getText().toString().trim();
                speakEnglish(english);
            }
        });
        textWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String english = textWord.getText().toString().trim();
                speakEnglish(english);
            }
        });
        DisplayMetrics metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int height = metrics.heightPixels;
        BottomSheetDialog dialog = new BottomSheetDialog(requireActivity(),R.style.MybottomSheetDialog);
        dialog.setCanceledOnTouchOutside(true);
        view.setMinimumHeight((int)(0.4*height));
        dialog.setContentView(view);
        BottomSheetBehavior behavior = BottomSheetBehavior.from((View) view.getParent());
        dialog.show();
    }

    void initSeech(){
        initListener = new InitListener() {
            @Override
            public void onInit(int i) {
                Log.d("语音服务初始化码","code: "+i);
                if (i!=ErrorCode.SUCCESS){
                    Log.d("初始化结果","初始化失败");
                }else{
                    Log.d("初始化结果","初始化成功");
                }
            }
        };
        synthesizer = SpeechSynthesizer.createSynthesizer(requireActivity(),initListener);
        listener = new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {

            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {

            }

            @Override
            public void onSpeakPaused() {

            }

            @Override
            public void onSpeakResumed() {

            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {

            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        };
    }

    void setParam(){
        String voice = "xiaoyan";
        String mEngine = SpeechConstant.TYPE_CLOUD;
        synthesizer.setParameter(SpeechConstant.PARAMS,null);
        // 左边为key, 右边为value
        synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //支持实时音频返回，仅在synthesizeToUri条件下支持
        synthesizer.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
        // 设置在线合成发音人
        synthesizer.setParameter(SpeechConstant.VOICE_NAME, voice);
    }

    void speakEnglish(String english){
        setParam();
        connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo==null){
            Toast.makeText(requireActivity(),"无网络，请打开网络获取语音服务",Toast.LENGTH_LONG).show();
        }else{
            if(networkInfo.getType()==ConnectivityManager.TYPE_WIFI){
                Log.d("网络服务类型","wifi");
            }else if(networkInfo.getType()==ConnectivityManager.TYPE_MOBILE){
                Log.d("网络服务类型","移动数据");
            }
        }
        synthesizer.startSpeaking(english,listener); // 合成语音最关键的方法，在初始化后才能调用
    }

    /**
     * 记得在生命周期结束时释放
     */
    @Override
    public void onStop() {
        synthesizer.stopSpeaking();
        super.onStop();
    }
}