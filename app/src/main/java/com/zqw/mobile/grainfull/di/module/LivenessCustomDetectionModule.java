package com.zqw.mobile.grainfull.di.module;

import com.jess.arms.di.scope.ActivityScope;

import dagger.Binds;
import dagger.Module;

import com.zqw.mobile.grainfull.mvp.contract.LivenessCustomDetectionContract;
import com.zqw.mobile.grainfull.mvp.model.LivenessCustomDetectionModel;

/**
 * ================================================
 * Description:
 * <p>
 * Created by MVPArmsTemplate on 2022/07/15 15:37
 * ================================================
 */
@Module
//构建LivenessCustomDetectionModule时,将View的实现类传进来,这样就可以提供View的实现类给presenter
public abstract class LivenessCustomDetectionModule {

    @Binds
    abstract LivenessCustomDetectionContract.Model bindLivenessCustomDetectionModel(LivenessCustomDetectionModel model);
}