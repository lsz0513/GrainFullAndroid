package com.zqw.mobile.grainfull.mvp.presenter;

import android.app.Application;

import com.jess.arms.integration.AppManager;
import com.jess.arms.di.scope.ActivityScope;
import com.jess.arms.mvp.BasePresenter;
import com.jess.arms.http.imageloader.ImageLoader;

import me.jessyan.rxerrorhandler.core.RxErrorHandler;

import javax.inject.Inject;

import com.zqw.mobile.grainfull.mvp.contract.BottomSheetDialogContract;

/**
 * ================================================
 * Description:
 * <p>
 * Created by MVPArmsTemplate on 2024/03/29 10:16
 * ================================================
 */
@ActivityScope
public class BottomSheetDialogPresenter extends BasePresenter<BottomSheetDialogContract.Model, BottomSheetDialogContract.View> {
    @Inject
    RxErrorHandler mErrorHandler;
    @Inject
    Application mApplication;
    @Inject
    ImageLoader mImageLoader;
    @Inject
    AppManager mAppManager;

    @Inject
    public BottomSheetDialogPresenter(BottomSheetDialogContract.Model model, BottomSheetDialogContract.View rootView) {
        super(model, rootView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mErrorHandler = null;
        this.mAppManager = null;
        this.mImageLoader = null;
        this.mApplication = null;
    }
}