package edu.scau.buymesth.conversation.chat;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import adpater.BaseQuickAdapter;
import adpater.BaseViewHolder;
import base.util.GlideCircleTransform;
import base.util.ToastUtil;
import cn.bmob.newim.bean.BmobIMConversation;
import cn.bmob.newim.bean.BmobIMMessage;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.QueryListener;
import edu.scau.Constant;
import edu.scau.buymesth.R;
import edu.scau.buymesth.data.bean.Conversation;
import edu.scau.buymesth.data.bean.Request;
import edu.scau.buymesth.request.requestdetail.RequestDetailActivity;
import edu.scau.buymesth.userinfo.UserInfoActivity;
import edu.scau.buymesth.util.DateFormatHelper;
import gallery.PhotoActivity;
import util.DensityUtil;
import util.FileUtils;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.MODE_PRIVATE;

/**
 * Created by ！ on 2016/9/21.
 */

public class ChatAdapter extends BaseQuickAdapter<BmobIMMessage> {

    private SparseArray<Integer> layouts;
    private String myAvatar;

    @Override
    protected int getDefItemViewType(int position) {
        //发出为0 接收为1
        int recvOsend =  mData.get(position).getFromId().equals(BmobUser.getCurrentUser().getObjectId())?0:1;
        BmobIMMessage msg = mData.get(position);
        try {
            int type = new JSONObject(msg.getContent()).getInt("type");
            return type * 2 + recvOsend;
        } catch (JSONException e) { }
        return recvOsend;
    }


    @Override
    protected BaseViewHolder onCreateDefViewHolder(ViewGroup parent, int viewType) {
        return createBaseViewHolder(parent, getLayoutId(viewType));
    }

    private int getLayoutId(int viewType) {
        return layouts.get(viewType);
    }

    protected void addItemType(int type, int layoutResId) {
        if (layouts == null) {
            layouts = new SparseArray<>();
        }
        layouts.put(type, layoutResId);
    }

    Activity mActivity;
    BmobIMConversation mConversation;

    public ChatAdapter(Activity activity , List<BmobIMMessage> msgList , BmobIMConversation conversation) {
        super(msgList);
        this.mActivity = activity;
        this.mConversation = conversation;
        addItemType(0, R.layout.item_chat_send);
        addItemType(1, R.layout.item_chat_recv);
        addItemType(2, R.layout.item_chat_send_image);
        addItemType(3, R.layout.item_chat_recv_image);
        addItemType(4, R.layout.item_chat_send_request);
        addItemType(5, R.layout.item_chat_recv_request);
        addItemType(6, R.layout.item_chat_send_file);
        addItemType(7, R.layout.item_chat_recv_file);
        SharedPreferences settings = activity.getSharedPreferences(Constant.SHARE_PREFERENCE_USER_INFO, MODE_PRIVATE);
        myAvatar = settings.getString(Constant.KEY_AVATAR,"");
    }

    @Override
    protected void convert(BaseViewHolder helper, BmobIMMessage item) {
        JSONObject jsonMsg = null;
        String content = "unknown message";
        String extra = "";
        try {
            jsonMsg = new JSONObject(item.getContent());
            content = jsonMsg.getString("content");
        } catch (JSONException e) { }
        //
        if(jsonMsg == null||helper.getItemViewType()<2){
            helper.setText(R.id.tv_msg_content,content);
        } else if(helper.getItemViewType()<4){
            try {
                int width = 0,height = 0;
                width = DensityUtil.px2dip(mContext,jsonMsg.getInt("width"));
                height = DensityUtil.px2dip(mContext,jsonMsg.getInt("height"));
                ViewGroup.LayoutParams lp = helper.getView(R.id.msg_image).getLayoutParams();
                if(width>200 && width>height) {
                    height = (int) (height * 200f / width);
                    width = 200;
                }else if(height>220 && width<=height) {
                    width = (int) (width * 220f /height);
                    height = 220;
                }
                lp.height = height;
                lp.width = width;
                helper.getView(R.id.msg_image).setLayoutParams(lp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Glide.with(mContext).load(content).into((ImageView) helper.getView(R.id.msg_image));
        } else if(helper.getItemViewType()<6){
            BmobQuery<Request> query = new BmobQuery<>();
            query.getObject(content, new QueryListener<Request>() {
                @Override
                public void done(Request request, BmobException e) {
                    if(e!=null) return;
                    helper.setText(R.id.tv_request_title,request.getTitle());
                    helper.setText(R.id.tv_request_content,request.getContent());
//                    helper.setText(R.id.tv_request_price,"¥"+ request.getMaxPrice()+ (request.getMinPrice()==null?"":"~"+request.getMinPrice()));
                    if(request.getUrls()!=null&&request.getUrls().size()>0){
                        Glide.with(mContext).load(request.getUrls().get(0)).centerCrop().into((ImageView) helper.getView(R.id.iv_request_cover));
                    }
                }
            });
        } else if(helper.getItemViewType()<8){
            try {
                helper.setText(R.id.tv_file_size, FileUtils.convert(jsonMsg.getLong("filesize")));
                helper.setText(R.id.tv_file_name, jsonMsg.getString("filename"));
                extra = jsonMsg.getString("filename");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(item.getSendStatus()==1){
            helper.getView(R.id.iv_warn).setVisibility(View.INVISIBLE);
            helper.getView(R.id.progress_send).setVisibility(View.VISIBLE);
        }else if(item.getSendStatus()==3){
            //fail
            helper.getView(R.id.iv_warn).setVisibility(View.VISIBLE);
            helper.getView(R.id.progress_send).setVisibility(View.INVISIBLE);
        }else {
            helper.getView(R.id.iv_warn).setVisibility(View.INVISIBLE);
            helper.getView(R.id.progress_send).setVisibility(View.INVISIBLE);
        }
        Glide.with(mActivity).load(helper.getItemViewType()%2==1?mConversation.getConversationIcon():myAvatar).crossFade()
                .placeholder(R.mipmap.def_head)
                .transform(new GlideCircleTransform(mContext))
                .into((ImageView) helper.getView(R.id.iv_avatar));
        int position = helper.getAdapterPosition();
        if(position==0||position>0&&item.getCreateTime()-getItem(position - 1).getCreateTime()>200000){
            helper.getView(R.id.tv_date).setVisibility(View.VISIBLE);
            helper.setText(R.id.tv_date, DateFormatHelper.dateFormat(item.getCreateTime()));
        }else helper.getView(R.id.tv_date).setVisibility(View.GONE);

        String finalContent = content;
        String finalExtra = extra;
        if(helper.getItemViewType()>=2&&helper.getItemViewType()<4){
            helper.getView(R.id.msg_image).setOnClickListener(v -> PhotoActivity.navigate(mActivity ,v , finalContent, 0));
        }
        helper.getView(R.id.ly_msg_rect).setOnClickListener(v -> {
            if(helper.getItemViewType()<2){
            } else if(helper.getItemViewType()<4){
            } else if(helper.getItemViewType()<6){
                Request request = new Request();
                request.setObjectId(finalContent);
                RequestDetailActivity.navigate(mActivity,request,true);
            } else if(helper.getItemViewType()<8){
                new AlertDialog.Builder(mContext).setTitle("下载文件"+finalExtra)
                        .setPositiveButton("确定", (dialog, which) -> new BmobFile(item.getId()+ finalExtra,"",finalContent).download(
                                new DownloadFileListener() {
                                    @Override
                                    public void done(String s, BmobException e) {
                                        if(e==null) ToastUtil.show("文件已保存到"+s);
                                        else ToastUtil.show("文件下载失败");
                                    }
                                    @Override
                                    public void onProgress(Integer integer, long l) {
                                        ToastUtil.show("下载中"+integer+"%");
                                    }
                        }))
                        .setNegativeButton("取消", null).show();
            }
        });
        helper.getView(R.id.ly_msg_rect).setOnLongClickListener(v -> {
            ClipboardManager myClipboard = (ClipboardManager) mContext.getSystemService(CLIPBOARD_SERVICE);
            ClipData myClip;
            myClip = ClipData.newPlainText("text", item.getContent());
            myClipboard.setPrimaryClip(myClip);
            ToastUtil.show("已复制到剪贴板");
            return false;
        });
        helper.getView(R.id.iv_avatar).setOnClickListener(v1 -> {
            if(helper.getItemViewType()%2==0)
                UserInfoActivity.navigate(mActivity, BmobUser.getCurrentUser().getObjectId());
            else
                UserInfoActivity.navigate(mActivity, item.getConversationId());
        });
    }
}
