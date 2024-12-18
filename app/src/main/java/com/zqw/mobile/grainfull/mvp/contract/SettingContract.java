package com.zqw.mobile.grainfull.mvp.contract;

import com.zqw.mobile.grainfull.mvp.model.entity.AppUpdate;
import com.jess.arms.mvp.IView;
import com.jess.arms.mvp.IModel;

import io.reactivex.Observable;


/**
 * ================================================
 * Description:
 * <p>
 * Created by MVPArmsTemplate on 03/28/2020 13:17
 * ================================================
 */
public interface SettingContract {
    //对于经常使用的关于UI的方法可以定义到IView中,如显示隐藏进度条,和显示文字消息
    interface View extends IView {
        // 升级询问
        void mainAskDialog(AppUpdate info);

        // 已经是最新版本
        void showLatestDialog();
    }

    //Model层定义接口,外部只需关心Model返回的数据,无需关心内部细节,即是否使用缓存
    interface Model extends IModel {
        // 获取APP版本信息
        Observable<AppUpdate> getVersion(String type);
    }
}
