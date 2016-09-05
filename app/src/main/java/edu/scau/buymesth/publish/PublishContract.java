package edu.scau.buymesth.publish;

import java.util.List;

import edu.scau.buymesth.data.bean.Request;
import rx.Subscriber;

/**
 * Created by Jammy on 2016/8/16.
 */
public interface PublishContract {
    interface  View{
        void onSubmitFinish();
        void onSubmitFail();
        void showLoadingDialog();
        void closeLoadingDialog();
    }

    interface Model{
        void submit(Request request, Subscriber<String> observable, List<String> list);
    }


}
