package edu.scau.buymesth.discover.publish;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

import base.BaseActivity;
import cn.bmob.v3.BmobUser;
import edu.scau.buymesth.R;
import edu.scau.buymesth.request.HomeFragment;
import edu.scau.buymesth.request.HomePresenter;

/**
 * Created by ！ on 2016/9/3.
 */
public class SelectActivity extends BaseActivity{
    @Override
    protected int getLayoutId() {
        return R.layout.activity_select;
    }

    @Override
    protected int getToolBarId() {
        return R.id.toolbar;
    }

    @Override
    public void initView() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        HomeFragment homeFragment = new HomeFragment();
        homeFragment.setFilter(HomePresenter.FILTER_AUTHOR_IDS);
        List<String> ids = new ArrayList<>();
        ids.add(BmobUser.getCurrentUser().getObjectId());
        homeFragment.setFilterKey(ids);
        transaction.replace(R.id.content_fragment, homeFragment);
        transaction.commit();
    }

    @Override
    public boolean canSwipeBack() {
        return false;
    }

    @Override
    public int getStatusColorResources() {
        return R.color.colorPrimaryDark;
    }
}
