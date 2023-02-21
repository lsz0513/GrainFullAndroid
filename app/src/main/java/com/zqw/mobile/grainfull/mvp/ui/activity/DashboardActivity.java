package com.zqw.mobile.grainfull.mvp.ui.activity;

import static com.jess.arms.utils.Preconditions.checkNotNull;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jess.arms.base.BaseActivity;
import com.jess.arms.di.component.AppComponent;
import com.jess.arms.utils.ArmsUtils;
import com.zqw.mobile.grainfull.R;
import com.zqw.mobile.grainfull.di.component.DaggerDashboardComponent;
import com.zqw.mobile.grainfull.mvp.contract.DashboardContract;
import com.zqw.mobile.grainfull.mvp.presenter.DashboardPresenter;
import com.zqw.mobile.grainfull.mvp.ui.widget.DashboardView;

import java.util.Random;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Description:仪表盘
 * <p>
 * Created on 2023/02/20 17:44
 *
 * @author 赤槿
 * module name is DashboardActivity
 */
public class DashboardActivity extends BaseActivity<DashboardPresenter> implements DashboardContract.View {
    /*------------------------------------------------控件信息------------------------------------------------*/
    @BindView(R.id.activity_dashboard)
    LinearLayout contentLayout;                                                                     // 主布局

    @BindView(R.id.view_dashboardactivity_dashboard)
    DashboardView mDashboardView;

    /*------------------------------------------------业务区域------------------------------------------------*/
    // 生成的随机数
    private static int mRndom = 0;
    // 是否开始
    private boolean isStart = false;

    @Override
    protected void onDestroy() {
        // 停止线程
        isStart = false;
        super.onDestroy();
    }

    @Override
    public void setupActivityComponent(@NonNull AppComponent appComponent) {
        DaggerDashboardComponent
                .builder()
                .appComponent(appComponent)
                .view(this)
                .build()
                .inject(this);
    }

    @Override
    public int initView(@Nullable Bundle savedInstanceState) {
        return R.layout.activity_dashboard;
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {
        setTitle("仪表盘");

    }

    @OnClick({
            R.id.btn_dashboardactivity_start,                                                       // 开始测试
    })
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_dashboardactivity_start:                                                  // 开始测试
                // 没有开始的情况下才能点击
                if(!isStart){
                    isStart = true;
                    new Thread() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 10; i++) {
                                if(!isStart){
                                    break;
                                }
                                mRndom = getRandom(mDashboardView.getDashboardMax());
                                runOnUiThread(() -> {
                                    // 外部更新
                                    mDashboardView.udDataSpeed(mRndom);
                                });

                                Timber.i("####Rndom=%s", mRndom);

                                try {
                                    Thread.sleep(500);
                                } catch (Exception ignored) {
                                }
                            }

                            // 运行结束
                            isStart = false;
                        }
                    }.start();
                }
                break;
        }
    }

    /**
     * 生成随机数
     */
    public int getRandom(int num) {
        Random random = new Random();
        return random.nextInt(num);
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
}