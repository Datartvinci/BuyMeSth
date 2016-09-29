package edu.scau.buymesth.user;

import cn.bmob.v3.BmobQuery;
import edu.scau.buymesth.data.bean.User;
import rx.Observable;

/**
 * Created by Jammy on 2016/8/31.
 */
public class UserModel {
    private User user;

    public Observable<User> getUser(String id) {
        BmobQuery<User> query=new BmobQuery<>();
        return query.getObjectObservable(User.class,id);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
