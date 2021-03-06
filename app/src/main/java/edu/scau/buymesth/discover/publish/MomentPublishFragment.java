package edu.scau.buymesth.discover.publish;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import base.BaseActivity;
import base.util.SpaceItemDecoration;
import base.util.ToastUtil;
import butterknife.Bind;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.QueryListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UploadBatchListener;
import edu.scau.Constant;
import edu.scau.buymesth.R;
import edu.scau.buymesth.adapter.MyPictureAdapter;
import edu.scau.buymesth.data.bean.Moment;
import edu.scau.buymesth.data.bean.Request;
import edu.scau.buymesth.data.bean.User;
import edu.scau.buymesth.fragment.EmptyActivity;
import edu.scau.buymesth.location.LocationFragment;
import edu.scau.buymesth.util.CompressHelper;
import gallery.PhotoActivity;
import me.iwf.photopicker.PhotoPicker;
import rx.Observable;
import util.FileUtils;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Created by ！ on 2016/10/9.
 */

public class MomentPublishFragment extends Fragment implements EmptyActivity.ButtonOnToolbar,MyPictureAdapter.CompressList{
    RecyclerView recyclerView;
    EditText momentContent;
    TextView tvRequest;
    TextView tvSize;
    Switch swCompress;
    TextView tvLocation;
    ArrayList<MyPictureAdapter.ImageItem> mUrlList;
    List<String> mImagesUrlOnBmob;
    MyPictureAdapter adapter;
    Request mRequest;
    String mLocation;
    boolean mCompressing = false;
    boolean mCompressed = false;
    boolean mCompress = false;
    long mImageSize = 0;
    private ExecutorService threadPoolExecutor=null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moment_publish,container,false);
        mUrlList = new ArrayList<>();
//        mTempUrlList = new ArrayList<>();

        recyclerView = (RecyclerView) view.findViewById(R.id.rv_images);
        momentContent = (EditText) view.findViewById(R.id.et_moment_content);
        tvRequest = (TextView) view.findViewById(R.id.tv_request);
        tvSize = (TextView) view.findViewById(R.id.tv_size);
        swCompress = (Switch) view.findViewById(R.id.sw_compress);
        tvLocation = (TextView) view.findViewById(R.id.tv_location);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.dp_6)));
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        adapter = new MyPictureAdapter(mUrlList,this);
        recyclerView.setAdapter(adapter);
        adapter.setOnRecyclerViewItemClickListener((view0, position) -> {
            ////这里设置点击事件
            ArrayList<String> selectImages = new ArrayList<>();
            if (adapter.getItemId(position) == 1) {
                for(MyPictureAdapter.ImageItem ii : mUrlList) selectImages.add(ii.sourceImage);
                PhotoPicker.builder()
                        .setPhotoCount(9)
                        .setShowCamera(true)
                        .setShowGif(false)
                        .setPreviewEnabled(true)
                        .setSelected(selectImages)
                        .start(getActivity(), PhotoPicker.REQUEST_CODE);
            } else {
                for(MyPictureAdapter.ImageItem ii : mUrlList) selectImages.add(mCompress?ii.compressedImage:ii.sourceImage);
                PhotoActivity.navigate(getActivity(),recyclerView, selectImages,position);
            }
        });

        swCompress.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mCompress = isChecked;
            if(isChecked&&!mCompressed&&!mCompressing){
                compress();
            }else if(mCompressed&&!mCompressing){
                mImageSize = 0;
                adapter.setList(mUrlList,mCompress?1:0);
                Observable.from(mUrlList)
                        .map(imageItem -> new File(mCompress?imageItem.compressedImage:imageItem.sourceImage))
                        .subscribe(file -> mImageSize += file.length(),
                                o->{},
                                () -> tvSize.setText("图片大小："+ FileUtils.convert(mImageSize)));
            }
        });

        tvRequest.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(),SelectActivity.class);
            intent.putExtra("selectRequest",true);
            startActivityForResult(intent,0);
        });
        tvLocation.setOnClickListener(v ->
                EmptyActivity.navigateForResult(getActivity()
                        , LocationFragment.class.getName()
                        ,null
                        , Constant.LOCATION_SELECT_REQUEST_CODE,"定位"));
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
            if (data != null) {
                ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
                mUrlList.clear();
                for(String url : photos){
                    mUrlList.add(new MyPictureAdapter.ImageItem(url,null));
                }
                adapter.setList(mUrlList,mCompress?1:0);
                mImageSize = 0;
                mCompressed = false;
                Observable.from(mUrlList)
                        .map(imageItem -> new File(mCompress?imageItem.compressedImage:imageItem.sourceImage))
                        .subscribe(file -> mImageSize += file.length(),
                                o -> {},
                                () -> tvSize.setText("图片大小："+ FileUtils.convert(mImageSize)));
                if(mCompress) compress();
            }
        } if(resultCode == Activity.RESULT_OK && requestCode == 0){
            String id = data.getStringExtra("requestId");
            if(id==null)return;
            BmobQuery<Request> bmobQuery = new BmobQuery<>();
            bmobQuery.getObject(id, new QueryListener<Request>() {
                @Override
                public void done(Request request, BmobException e) {
                    mRequest = request;
                    getActivity().runOnUiThread(() -> tvRequest.setText(request.getTitle()));
                }
            });
        } if(requestCode == Constant.LOCATION_SELECT_REQUEST_CODE && resultCode == Constant.LOCATION_SELECT_RESULT_CODE){
            Bundle locationInfo = data.getBundleExtra("data");
            mLocation = locationInfo.getString("Country");
            mLocation += locationInfo.getString("Province");
            mLocation += locationInfo.getString("City");
            mLocation += locationInfo.getString("Address");
            tvLocation.post(() -> tvLocation.setText(mLocation));
        }
    }

    private void compress() {
        if(threadPoolExecutor==null)
        {
            Runtime.getRuntime().gc();
            short mem= (short) (Runtime.getRuntime().freeMemory()>>20);
            threadPoolExecutor = newFixedThreadPool(mem>6?mem/5:1);

        }
        swCompress.setEnabled(false);
        tvSize.setText("压缩中");
        ((BaseActivity)getActivity()).showLoadingDialog(1);
        ((BaseActivity)getActivity()).mDialog.setMax(mUrlList.size());
        new Thread(() -> {
            mCompressing = true;
            CompressHelper compressHelper = new CompressHelper(getContext());
            CountDownLatch countDownLatch = new CountDownLatch(mUrlList.size());
            for (int i = 0; i < mUrlList.size(); i++) {
                final int finalI = i;
                threadPoolExecutor.execute(() -> {
                    compressHelper.setFilename("cc_" + finalI);
                    mUrlList.get(finalI).compressedImage = compressHelper.thirdCompress(new File(mUrlList.get(finalI).sourceImage));
                    countDownLatch.countDown();
                    getActivity().runOnUiThread(() -> ((BaseActivity)getActivity()).mDialog.setProgress((int)(mUrlList.size()-countDownLatch.getCount())));
                });
            }

            try {
                countDownLatch.await();
                getActivity().runOnUiThread(() -> {
                    ((BaseActivity)getActivity()).closeLoadingDialog();
                    ToastUtil.show("压缩完成");
                    mImageSize = 0;
                    Observable.from(mUrlList)
                            .map(imageItem -> new File(mCompress ? imageItem.compressedImage : imageItem.sourceImage))
                            .subscribe(file -> mImageSize += file.length(),
                                    o -> {
                                    },
                                    () -> tvSize.setText("图片大小：" + FileUtils.convert(mImageSize)));
                    adapter.setList(mUrlList, mCompress ? 1 : 0);
                    swCompress.setEnabled(true);
                });
                mCompressing = false;
                mCompressed = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(threadPoolExecutor!=null)
            threadPoolExecutor.shutdownNow();
    }

    @Override
    public void onToolbarButtonClick(View v) {
        //upload images
        if(mUrlList.size()>0) {
            if(mCompressing){
                ToastUtil.show("压缩中");
                return;
            }
            v.setEnabled(false);
            ArrayList<String> selectImages = new ArrayList<>();
            for(MyPictureAdapter.ImageItem ii : mUrlList) selectImages.add(mCompress?ii.compressedImage:ii.sourceImage);
            String[] sendList = new String[selectImages.size()];
            selectImages.toArray(sendList);

            BmobFile.uploadBatch(sendList, new UploadBatchListener() {
                @Override
                public void onSuccess(List<BmobFile> files, List<String> urls) {
                    if(urls.size()>= mUrlList.size()){
                        Moment moment = new Moment();
                        moment.setUser(BmobUser.getCurrentUser(User.class));
                        moment.setContent(momentContent.getText().toString());
                        mImagesUrlOnBmob = new ArrayList<>();
                        mImagesUrlOnBmob.clear();
                        mImagesUrlOnBmob.addAll(urls);
                        moment.setImages(mImagesUrlOnBmob);
                        if(mRequest!=null)moment.setRequest(mRequest);
                        if(mLocation!=null)moment.setLocation(mLocation);
                        moment.save(new SaveListener<String>() {
                            @Override
                            public void done(String s, BmobException e) {
                                if(e==null){
                                    //succeed
                                    getActivity().finish();
                                }else {
                                    ToastUtil.show(e.toString());
                                }
                            }
                        });
                    }
                }

                @Override
                public void onProgress(int curIndex, int curPercent, int total, int totalPercent) {
                    //1、curIndex--表示当前第几个文件正在上传
                    //2、curPercent--表示当前上传文件的进度值（百分比）
                    //3、total--表示总的上传文件数
                    //4、totalPercent--表示总的上传进度（百分比）
                    View view = recyclerView.getChildAt(curIndex-1);
                    ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_upload);
                    if(progressBar!=null){
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setMax(100);
                        progressBar.setProgress(curPercent);
                    }
                }

                @Override
                public void onError(int i, String s) {
                    ToastUtil.show(s);
                }
            });
        }
        else {
            Moment moment = new Moment();
            moment.setUser(BmobUser.getCurrentUser(User.class));
            moment.setContent(momentContent.getText().toString());
            if(mRequest!=null)moment.setRequest(mRequest);
            if(mLocation!=null)moment.setLocation(mLocation);
            moment.save(new SaveListener<String>() {
                @Override
                public void done(String s, BmobException e) {
                    if(e==null){
                        //succeed
                        getActivity().finish();
                    }else {
                        ToastUtil.show(e.toString());
                    }
                }
            });
        }
    }

    @Override
    public String toolbarsButtonText() {
        return "发送";
    }

    @Override
    public void onDataChange(MyPictureAdapter.ImageItem item) {
        mImageSize = 0;
        Observable.from(mUrlList)
                .map(imageItem -> new File(mCompress?imageItem.compressedImage:imageItem.sourceImage))
                .subscribe(file -> mImageSize += file.length(),
                        o->{},
                        () -> tvSize.setText("图片大小："+ FileUtils.convert(mImageSize)));
    }
}
