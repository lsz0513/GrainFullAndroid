package com.zqw.mobile.grainfull.mvp.ui.activity;

import static com.jess.arms.utils.Preconditions.checkNotNull;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.baidu.aip.asrwakeup3.core.inputstream.InFileStream;
import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.aip.asrwakeup3.uiasr.params.CommonRecogParams;
import com.baidu.aip.asrwakeup3.uiasr.params.OnlineRecogParams;
import com.baidu.speech.asr.SpeechConstant;
import com.blankj.utilcode.util.KeyboardUtils;
import com.bumptech.glide.Glide;
import com.jess.arms.base.BaseActivity;
import com.jess.arms.di.component.AppComponent;
import com.jess.arms.utils.ArmsUtils;
import com.umeng.analytics.MobclickAgent;
import com.zqw.mobile.grainfull.BuildConfig;
import com.zqw.mobile.grainfull.R;
import com.zqw.mobile.grainfull.app.global.AccountManager;
import com.zqw.mobile.grainfull.app.tts.SynthActivity;
import com.zqw.mobile.grainfull.app.utils.MediaStoreUtils;
import com.zqw.mobile.grainfull.di.component.DaggerFastGPTComponent;
import com.zqw.mobile.grainfull.mvp.contract.FastGPTContract;
import com.zqw.mobile.grainfull.mvp.model.entity.ChatHistoryInfo;
import com.zqw.mobile.grainfull.mvp.presenter.FastGPTPresenter;
import com.zqw.mobile.grainfull.mvp.ui.widget.AudioRecorderButton;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

/**
 * Description:ChatGPT
 * <p>
 * 1、利用第三方“FastGPT”实现： ChatGPT + 知识库。
 * 2、语音识别与语音合成为第三方：百度-智能云。
 * <p>
 * Created on 2023/12/06 16:12
 *
 * @author 赤槿
 * module name is FastGPTActivity
 */
@RuntimePermissions
public class FastGPTActivity extends BaseActivity<FastGPTPresenter> implements FastGPTContract.View, AudioRecorderButton.VoiceEvents {
    /*--------------------------------控件信息--------------------------------*/
    @BindView(R.id.view_fastgpt_scrollView)
    NestedScrollView mScrollView;                                                                   // 外层 - 滑动布局
    @BindView(R.id.lila_fastgpt_chatlayout)
    LinearLayout lilaChatLayout;                                                                    // 消息总布局
    @BindView(R.id.edit_fastgpt_input)
    EditText editInput;                                                                             // 文字-输入框
    @BindView(R.id.view_fastgpt_voice)
    AudioRecorderButton viewVoice;                                                                  // 语音-按住说话
    @BindView(R.id.imvi_fastgpt_switch)
    ImageView imviVoiceOrText;                                                                      // 文字与语音-切换按钮
    @BindView(R.id.imvi_fastgpt_send)
    ImageView imviSend;                                                                             // 发送文字按钮


    // 接收的消息
    private TextView txviReceiveMsg;
    private ImageView imviReceiveMsg;
    /*--------------------------------业务信息--------------------------------*/
    @Inject
    AccountManager mAccountManager;
    // Api的参数类，仅仅用于生成调用START的json字符串，本身与SDK的调用无关
    private CommonRecogParams apiParams;
    // 识别控制器，使用MyRecognizer控制识别的流程
    protected MyRecognizer myRecognizer;
    // 语音合成(播报)
    private SynthActivity synthActivity;
    // 语音路径
    private String mVoicePath;
    // 是否自动播放语音(默认自动播放)
    private boolean isHorn = true;

    @Override
    protected void onDestroy() {
        // 如果之前调用过myRecognizer.loadOfflineEngine()， release()里会自动调用释放离线资源
        // 基于DEMO5.1 卸载离线资源(离线时使用) release()方法中封装了卸载离线资源的过程
        // 基于DEMO的5.2 退出事件管理器
        myRecognizer.release();
        if (synthActivity != null) {
            synthActivity.onDestroy();
            synthActivity = null;
        }
        super.onDestroy();
        this.mAccountManager = null;
        InFileStream.reset();
    }

    @Override
    public void setupActivityComponent(@NonNull AppComponent appComponent) {
        DaggerFastGPTComponent
                .builder()
                .appComponent(appComponent)
                .view(this)
                .build()
                .inject(this);
    }

    @Override
    public int initView(@Nullable Bundle savedInstanceState) {
        return R.layout.activity_fast_gpt;
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {
        setTitle("GPT+知识库");

        // 友盟统计 - 自定义事件
        MobclickAgent.onEvent(getApplicationContext(), "fastgpt_open");

        viewVoice.initAudio(true, this);
        lilaChatLayout.setOnTouchListener((v, event) -> {
            hideInput();
            return false;
        });

        editInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s)) {
                    imviSend.setVisibility(View.VISIBLE);
                    imviVoiceOrText.setVisibility(View.GONE);
                } else {
                    imviSend.setVisibility(View.GONE);
                    imviVoiceOrText.setVisibility(View.VISIBLE);
                }
            }
        });

        // 初始化语音识别
        apiParams = new OnlineRecogParams();
        apiParams.initSamplePath(this);
        // 基于DEMO集成第1.1, 1.2, 1.3 步骤 初始化EventManager类并注册自定义输出事件
        // DEMO集成步骤 1.2 新建一个回调类，识别引擎会回调这个类告知重要状态和识别结果
        IRecogListener listener = new MessageStatusRecogListener(handler);
        // DEMO集成步骤 1.1 1.3 初始化：new一个IRecogListener示例 & new 一个 MyRecognizer 示例,并注册输出事件
        myRecognizer = new MyRecognizer(this, listener);

        // 初始化语音播报
        synthActivity = new SynthActivity();
        synthActivity.initTTS(getApplicationContext(), true);

        mScrollView.post(() -> {
            // 请求网络
            if (mPresenter != null) {
                mPresenter.getChatHistory();
            }
        });
    }

    @OnClick({
            R.id.imvi_fastgpt_switch,                                                               // 文字与语音-切换按钮
            R.id.imvi_fastgpt_send,                                                                 // 发送文字按钮
    })
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imvi_fastgpt_switch:                                                          // 文字与语音-切换按钮
                if (editInput.isShown()) {
                    // 当前显示的是键盘，如果输入框中有文字，则执行发送事情，如果无内容，则执行切换事件
                    imviVoiceOrText.setImageResource(R.mipmap.icon_chat_softkeyboard);
                    editInput.setVisibility(View.GONE);
                    viewVoice.setVisibility(View.VISIBLE);
                    FastGPTActivityPermissionsDispatcher.addVudioWithPermissionCheck(this);
                } else {
                    editInput.setVisibility(View.VISIBLE);
                    viewVoice.setVisibility(View.GONE);
                    imviVoiceOrText.setImageResource(R.mipmap.icon_chat_voice);
                }
                break;
            case R.id.imvi_fastgpt_send:                                                            // 发送文字按钮
                onSend();
                break;
        }
    }

    /**
     * 发送“文字”消息
     */
    private void onSend() {
        String message = editInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            showMessage("请输入内容！");
        } else {
            // 隐藏软键盘
            hideInput();
            onSend(message);
        }
    }

    /**
     * 发送“语音”消息
     */
    private void onSend(String message) {
        // 在界面上显示“我”发出的消息
        addRightMsg(message);
        // 在界面上显示一条提示“对方，正在输入中……”
        addLeftMsg("正在输入中...");
        onSucc();

        if (mPresenter != null) {
            mPresenter.chatCreate(message);
        }
    }

    /**
     * 添加右侧消息(显示我的消息)
     */
    private void addRightMsg(String text) {
        // 控制布局
        editInput.getText().clear();

        // 添加聊天布局
        LinearLayout viewRightMsg = LayoutInflater.from(this).inflate(R.layout.chat_right_textview, null).findViewById(R.id.chat_right_layout);
        TextView textView = viewRightMsg.findViewById(R.id.txvi_chatrightlayout_chat);
        textView.setText(text);
        if (viewRightMsg.getParent() != null) {
            ((ViewGroup) viewRightMsg.getParent()).removeView(viewRightMsg);
        }

        lilaChatLayout.addView(viewRightMsg);
    }

    /**
     * 添加左侧消息(显示对方消息)
     */
    public void addLeftMsg(String text) {
        LinearLayout viewLeftMsg = LayoutInflater.from(this).inflate(R.layout.chat_left_textview, null).findViewById(R.id.chat_left_layout);

        imviReceiveMsg = viewLeftMsg.findViewById(R.id.imvi_chatleftlayout_chat);
        txviReceiveMsg = viewLeftMsg.findViewById(R.id.txvi_chatleftlayout_chat);
        txviReceiveMsg.setText(text);
        if (viewLeftMsg.getParent() != null) {
            ((ViewGroup) viewLeftMsg.getParent()).removeView(viewLeftMsg);
        }

        lilaChatLayout.addView(viewLeftMsg);
    }

    /**
     * 加载开场白
     */
    @Override
    public void onLoadOpeningRemarks(String tips) {
        // 添加一条消息
        addLeftMsg(tips);
        // 播放语音
        onVoiceAnnouncements(tips);
    }

    /**
     * 加载历史记录
     */
    @Override
    public void onLoadHistory(String tips, List<ChatHistoryInfo> list) {
        // 添加一条提示语
        addLeftMsg(tips);
        for (int i = 0; i < list.size(); i++) {
            ChatHistoryInfo item = list.get(i);
            if (i % 2 == 0) {
                addRightMsg(item.getValue());
            } else {
                addLeftMsg(item.getValue());
            }
        }

        // 滑动到底部
        mScrollView.post(() -> {
            mScrollView.fullScroll(View.FOCUS_DOWN);
        });
    }

    /**
     * 加载显示错误信息
     */
    @Override
    public void onLoadError(StringBuffer info) {
        runOnUiThread(() -> {
            imviReceiveMsg.setVisibility(View.GONE);
            txviReceiveMsg.setVisibility(View.VISIBLE);

            // response返回拼接
            txviReceiveMsg.setText(info.toString());
        });
    }

    /**
     * 加载聊天消息
     */
    @Override
    public void onLoadMessage(StringBuffer info) {
        txviReceiveMsg.setText("");

        // 开启线程处理(流式展示)
        new Thread(() -> {
            for (int i = 0; i < info.length(); i++) {
                char mChar = info.charAt(i);
                try {
                    // 停顿0.12秒
                    Thread.sleep(120);
                } catch (Exception ignored) {
                }

                runOnUiThread(() -> {
                    imviReceiveMsg.setVisibility(View.GONE);
                    txviReceiveMsg.setVisibility(View.VISIBLE);

                    // response返回拼接
                    txviReceiveMsg.append(String.valueOf(mChar));
                });
            }
        }).start();
    }

    /**
     * 语音播报
     */
    @Override
    public void onVoiceAnnouncements(String input) {
        // 是否播放语音
        if (isHorn) {
            // 文字总长度不能超过60个文字。大于50文字时进行文字分割。
            int length = 50;
            if (input.length() > length) {
                for (int i = 0; i < input.length(); i += length) {
                    String text = input.substring(i, Math.min(i + length, input.length()));
                    if (synthActivity != null) {
                        synthActivity.speak(text);
                    }
                }
            } else {
                // 文字长度没有大于50，不需要分割
                if (synthActivity != null) {
                    synthActivity.speak(input);
                }
            }
        }
    }

    /**
     * 加载图片消息
     */
    @Override
    public void onLoadImages(String url) {
        imviReceiveMsg.setVisibility(View.VISIBLE);
        txviReceiveMsg.setVisibility(View.GONE);
        Glide.with(imviReceiveMsg).load(url).into(imviReceiveMsg);
    }

    /**
     * 完成一次会话
     */
    @Override
    public void onSucc() {
        runOnUiThread(() -> {
            mScrollView.fullScroll(View.FOCUS_DOWN);
        });
    }

    /**
     * 隐藏软键盘
     */
    private void hideInput() {
        KeyboardUtils.hideSoftInput(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideInput();
        return super.onTouchEvent(event);
    }

    public Activity getActivity() {
        return this;
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    public void showMessage(@NonNull String message) {
        checkNotNull(message);
        ArmsUtils.snackbarText(message);
    }

    @Override
    public void launchActivity(@NonNull Intent intent) {
        checkNotNull(intent);
        ArmsUtils.startActivity(intent);
    }

    @Override
    public void killMyself() {
        finish();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        FastGPTActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    /**
     * 选择拍摄
     */
    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public void addVudio() {
        // 长按事件
        viewVoice.setOnLongClickListener(v -> {
            viewVoice.onStart();
            return false;
        });

        // 结束事件
        viewVoice.setOnAudioFinishRecorderListener((seconds, filePath) -> {
            Timber.i("##### filePath =%s", filePath);
            mVoicePath = filePath;
        });
    }

    /**
     * 开始录音，点击“开始”按钮后调用。
     * 基于DEMO集成2.1, 2.2 设置识别参数并发送开始事件
     */
    private void onSpeechRecognition() {
        // DEMO集成步骤2.1 拼接识别参数： 此处params可以打印出来，直接写到你的代码里去，最终的json一致即可。
        final Map<String, Object> params = fetchParams();
        if (BuildConfig.DEBUG) {
            params.put(SpeechConstant.APP_ID, getString(R.string.baidu_app_id_debug));
            params.put(SpeechConstant.APP_KEY, getString(R.string.baidu_map_api_key_debug));
            params.put(SpeechConstant.SECRET, getString(R.string.baidu_secret_key_debug));
        } else {
            params.put(SpeechConstant.APP_ID, getString(R.string.baidu_app_id));
            params.put(SpeechConstant.APP_KEY, getString(R.string.baidu_map_api_key));
            params.put(SpeechConstant.SECRET, getString(R.string.baidu_secret_key));
        }

        // params 也可以根据文档此处手动修改，参数会以json的格式在界面和logcat日志中打印
        Timber.i("设置的start输入参数：%s", params);
        // 复制此段可以自动检测常规错误
        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
//                        txtLog.append(message + "\n");
                        Timber.i("======" + message + "\n");

                        ; // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }
        }, false)).checkAsr(params);

        // 这里打印出params， 填写至您自己的app中，直接调用下面这行代码即可。
        // DEMO集成步骤2.2 开始识别
        myRecognizer.start(params);
    }

    private Map<String, Object> fetchParams() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        //  上面的获取是为了生成下面的Map， 自己集成时可以忽略
        Map<String, Object> params = apiParams.fetch(sp);
        //  集成时不需要上面的代码，只需要params参数。
        return params;
    }

    private Handler handler = new Handler() {

        /*
         * @param msg
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.obj != null) {
                Timber.i("###" + msg.obj.toString() + "\n");
            }

            switch (msg.what) { // 处理MessageStatusRecogListener中的状态回调
                case IStatus.STATUS_FINISHED:
                    // 删除音频文件
                    if (!TextUtils.isEmpty(mVoicePath))
                        MediaStoreUtils.deleteMedieFile(FastGPTActivity.this, new File(mVoicePath));
                    if (msg.arg2 == 1) {
                        // 识别成功
                        // ###识别结束，结果是“经过了几次对读者问题解决之后，我打算添加这一环节，非常有必要。”；说话结束到识别结束耗时【312ms】
                        Timber.i("####" + msg.obj.toString() + "\n");
                        // 将识别的结果做一次格式化，去掉(识别结束，结果是“)和(”；说话结束到识别结束耗时【312ms】)
                        String result = "";
                        try {
                            result = msg.obj.toString().substring(
                                    9,
                                    msg.obj.toString().lastIndexOf("”"));
                        } catch (Exception ex) {
                            result = msg.obj.toString();
                        }

                        if (result.contains("-3004")) {
                            showMessage("鉴权失败，没有权限调用接口！");
                        } else if (result.contains("-3005")) {
                            showMessage("未检测到声音，请重新语音！");
                        } else {
                            // 弹出对话框，显示结果
                            onSend(result);
                        }
                    }
                    break;
                case IStatus.STATUS_NONE:
                case IStatus.STATUS_READY:
                case IStatus.STATUS_SPEAKING:
                case IStatus.STATUS_RECOGNITION:
                    break;
                default:
                    break;

            }
        }

    };

    /**
     * 开始录音
     */
    @Override
    public void onVoiceStart() {
        if (!viewVoice.isRecording()) {
            onSpeechRecognition();
        }
    }

    /**
     * 停止
     */
    @Override
    public void onVoiceCancel() {
        myRecognizer.stop();
    }

    /**
     * 释放
     */
    @Override
    public void onVoiceRelease() {
        myRecognizer.stop();
    }
}