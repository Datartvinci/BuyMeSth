package edu.scau.buymesth.discover.detail;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import base.BaseActivity;
import base.util.GlideCircleTransform;
import butterknife.Bind;
import edu.scau.Constant;
import edu.scau.buymesth.R;
import edu.scau.buymesth.data.bean.Moment;
import edu.scau.buymesth.data.bean.MomentsComment;
import edu.scau.buymesth.userinfo.UserInfoActivity;
import edu.scau.buymesth.util.ColorChangeHelper;
import edu.scau.buymesth.util.DateFormatHelper;
import edu.scau.buymesth.util.DividerItemDecoration;
import gallery.PhotoActivity;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import in.srain.cube.views.ptr.header.StoreHouseHeader;
import ui.layout.NineGridLayout;

/**
 * Created by IamRabbit on 2016/8/23.
 */
public class MomentDetailActivity extends BaseActivity implements  MomentDetailContract.View{
    @Bind(R.id.rv_moment_comment)
    RecyclerView mRecyclerView;
    @Bind(R.id.store_house_ptr_frame)
    PtrFrameLayout mPtrFrameLayout;

    private MomentDetailPresenter mPresenter;
    private MomentDetailAdapter mMomentDetailAdapter;
    private View momentView;
    private View notLoadingView;
    private Button mButtonSend;
    private EditText mInputComment;

    public static void navigate(Activity activity, String momentId) {
        Intent intent = new Intent(activity, MomentDetailActivity.class);
        intent.putExtra(Moment.class.getName(), momentId);
        activity.startActivity(intent);
    }

    public static void navigate(Activity activity, Moment moment) {
        Intent intent = new Intent(activity, MomentDetailActivity.class);
        intent.putExtra(Moment.class.getName(), moment);
        activity.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_moment_detail;
    }

    @Override
    public void initView() {
        initToolBar();
        String momentId = getIntent().getStringExtra(Moment.class.getName());
        Moment moment;
        if(momentId==null)
            moment = (Moment) getIntent().getSerializableExtra(Moment.class.getName());
        else{
            moment = new Moment();
            moment.setObjectId(momentId);
        }
        //初始化代理人
        mPresenter=new MomentDetailPresenter(this);
        mPresenter.setVM(this,new MomentDetailModel(moment));
        initStoreHouse();
        initMomentView();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration( new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL_LIST));
        initAdapter();
        mButtonSend = (Button)findViewById(R.id.bt_send_comment);
        mInputComment = ((EditText)findViewById(R.id.intput_comment));
        mButtonSend.findViewById(R.id.bt_send_comment).setOnClickListener(v ->
                mPresenter.postComment(((TextView)findViewById(R.id.intput_comment)).getText().toString()));
        mInputComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = mInputComment.getText().toString();
                if(input.trim().length()>0) mButtonSend.setEnabled(true);
                else mButtonSend.setEnabled(false);
            }
        });
    }

    protected void initToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener((v) -> onBackPressed());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void setLike(Boolean like ,int i){
        mPresenter.mModel.getMoment().setLike(like);
        runOnUiThread(() -> {
            if(like){
                ((ImageView)momentView.findViewById(R.id.iv_likes)).setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_red));
                ((TextView)momentView.findViewById(R.id.tv_likes)).setText(String.valueOf(mPresenter.mModel.getMoment().getLikes()+i));
            }
            else{
                ((ImageView)momentView.findViewById(R.id.iv_likes)).setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite));
                ((TextView)momentView.findViewById(R.id.tv_likes)).setText(String.valueOf(mPresenter.mModel.getMoment().getLikes()+i));
            }
        });
    }

    public void initMomentView(){
        runOnUiThread(() -> {
            if(momentView==null){
                if(mPresenter.mModel.getMoment().getRequest()==null)
                    momentView = getLayoutInflater().inflate(R.layout.item_discover_norequest_detail,(ViewGroup) mRecyclerView.getParent(),false);
                else
                    momentView = getLayoutInflater().inflate(R.layout.item_discover_request_detail,(ViewGroup) mRecyclerView.getParent(),false);
                momentView.findViewById(R.id.ly_likes).setOnClickListener(v -> mPresenter.like());
                ((NineGridLayout)momentView.findViewById(R.id.nine_grid_layout)).setOnItemClickListener(new NineGridLayout.OnItemClickListener() {
                    @Override
                    public void onClick(View view, int position, List<String> urls, int itemType) {
                        PhotoActivity.navigate(MomentDetailActivity.this,
                                (NineGridLayout)momentView.findViewById(R.id.nine_grid_layout),
                                mPresenter.mModel.getMoment().getImages(),
                                position);
                    }
                });
            }
            Moment moment = mPresenter.mModel.getMoment();
            ((TextView)momentView.findViewById(R.id.tv_name)).setText(moment.getUser().getNickname());
            ((TextView)momentView.findViewById(R.id.tv_level)).setText("LV "+moment.getUser().getExp()/10);
            ((TextView)momentView.findViewById(R.id.tv_text)).setText(moment.getContent());
            ((TextView)momentView.findViewById(R.id.tv_likes)).setText(""+moment.getLikes());
            ((TextView)momentView.findViewById(R.id.tv_comments)).setText(""+moment.getComments());
            ((TextView)momentView.findViewById(R.id.tv_date)).setText(DateFormatHelper.dateFormat(moment.getCreatedAt()));
            momentView.findViewById(R.id.tv_level).setBackground(ColorChangeHelper.tintDrawable(mContext.getResources().getDrawable(R.drawable.rect_black),
                    ColorStateList.valueOf(ColorChangeHelper.IntToColorValue((moment.getUser().getExp())))));
            Glide.with(mContext).load(moment.getUser().getAvatar())
                    .crossFade()
                    .placeholder(R.mipmap.def_head)
                    .transform(new GlideCircleTransform(mContext))
                    .into((ImageView) momentView.findViewById(R.id.iv_avatar));
            momentView.findViewById(R.id.iv_avatar).setOnClickListener(v -> UserInfoActivity.navigate(MomentDetailActivity.this, moment.getUser()));
            ((NineGridLayout)momentView.findViewById(R.id.nine_grid_layout)).setUrlList(moment.getImages());
        });
    }

    public void updateLike(Boolean like){
        if(like){
            momentView.post(() -> {
                ((ImageView)momentView.findViewById(R.id.iv_likes))
                        .setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_favorite_red)
                        );
                ((TextView)momentView.findViewById(R.id.tv_likes)).setText(String.valueOf(mPresenter.mModel.getMoment().getLikes()+1));
            });
        }else {
            momentView.post(() -> {
                ((ImageView)momentView.findViewById(R.id.iv_likes))
                        .setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_favorite)
                        );
                ((TextView)momentView.findViewById(R.id.tv_likes)).setText(String.valueOf(mPresenter.mModel.getMoment().getLikes()-1));
            });
        }
    }

    private void initAdapter(){
        mMomentDetailAdapter = new MomentDetailAdapter(MomentDetailActivity.this,mPresenter.mModel.getDatas());
        mMomentDetailAdapter.openLoadAnimation();
        mMomentDetailAdapter.addHeaderView(momentView);
        mRecyclerView.setAdapter(mMomentDetailAdapter);

        mMomentDetailAdapter.setOnLoadMoreListener(() -> mPresenter.LoadMore());
        mMomentDetailAdapter.openLoadMore(Constant.NUMBER_PER_PAGE, true);
    }

    private void initStoreHouse() {
        final StoreHouseHeader header = new StoreHouseHeader(this);
        header.setPadding(0, 80, 0,50);
        header.initWithString("Buy Me Sth");
        header.setTextColor(Color.BLACK);
        mPtrFrameLayout.setDurationToCloseHeader(1500);
        mPtrFrameLayout.setHeaderView(header);
        mPtrFrameLayout.addPtrUIHandler(header);
//        frame.postDelayed(() -> frame.autoRefresh(false), 0);
        mPtrFrameLayout.postDelayed(()-> mPresenter.Refresh(),0);
        mPtrFrameLayout.setPtrHandler(new PtrHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return  !mRecyclerView.canScrollVertically(-1);
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                mPresenter.Refresh();
            }
        });
    }

    @Override
    public boolean canSwipeBack() {
        return true;
    }

    @Override
    public boolean showColorStatusBar() {
        return true;
    }

    @Override
    public int getStatusColorResources() {
        return R.color.colorPrimaryDark;
    }

    @Override
    public void onLoadMoreSuccess(List<MomentsComment>list) {
        if(list.size()>0)
            mMomentDetailAdapter.notifyDataChangedAfterLoadMore(true);
        else{
            mMomentDetailAdapter.notifyDataChangedAfterLoadMore(false);
            if (notLoadingView == null) {
                notLoadingView = this.getLayoutInflater().inflate(R.layout.not_loading, (ViewGroup) mRecyclerView.getParent(), false);
            }
            mMomentDetailAdapter.addFooterView(notLoadingView);
        }
    }

    @Override
    public void onError(Throwable throwable, String msg) {

    }

    @Override
    public void onRefreshComplete(List<MomentsComment> list) {
        mMomentDetailAdapter.notifyDataSetChanged();
        mMomentDetailAdapter.openLoadMore(Constant.NUMBER_PER_PAGE, true);
        mMomentDetailAdapter.removeAllFooterView();
        if(mPtrFrameLayout!=null)
            mPtrFrameLayout.refreshComplete();
    }

    @Override
    public void onInitializeLocalDataComplete() {

    }

    @Override
    public void onAddOneItem(MomentsComment moment) {

    }

    @Override
    public void onItemChanged() {

    }

    @Override
    public void onRefreshInterrupt() {
        if(mPtrFrameLayout!=null)
            mPtrFrameLayout.refreshComplete();
        mMomentDetailAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoadMoreInterrupt() {

    }

    @Override
    public void onPostCommentSuccess(String msg) {
        ((TextView)findViewById(R.id.intput_comment)).setText(null);
        mPresenter.RefreshComments();
    }

}
