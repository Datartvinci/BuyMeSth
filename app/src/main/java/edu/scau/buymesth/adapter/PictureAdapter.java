package edu.scau.buymesth.adapter;


import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.List;

import adpater.BaseQuickAdapter;
import adpater.BaseViewHolder;
import cn.finalteam.galleryfinal.model.PhotoInfo;
import edu.scau.buymesth.R;

/**
 * Created by Jammy on 2016/8/17.
 */
public class PictureAdapter extends BaseQuickAdapter<PhotoInfo> {

    //    List<PhotoInfo> list;
    public void setList(List<PhotoInfo> list) {
//        this.list = list;
        if (list.size() != 9)
            list.add(new PhotoInfo());
        setNewData(list);
    }


    /////布局和数据
    public PictureAdapter(List<PhotoInfo> data) {
        super(R.layout.picture_item, data);
        setList(data);
    }

    @Override
    protected void convert(BaseViewHolder helper, PhotoInfo item) {
        if (helper.getLayoutPosition()==getItemCount()-1&&getItemCount()!=9) {
            Glide.with(mContext).load(R.mipmap.ic_add).centerCrop().into((ImageView) helper.getView(R.id.iv));
        } else {
            String url = item.getPhotoPath();
            Glide.with(mContext).load(url).centerCrop().into((ImageView) helper.getView(R.id.iv));
        }
    }

    @Override
    public long getItemId(int position) {
        if(getData().get(position).getPhotoPath()!=null)
            return 0;
        return 1;
    }
}