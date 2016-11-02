package gallery;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.List;

import edu.scau.base.R;
import photoview.PhotoViewAttacher;
import ui.layout.MySmoothImageView;

/**
 * Created by ！ on 2016/8/31.
 */
public class PhotoFragment extends Fragment {

    private View mRootView;
    private MyPagerAdapter mPagerAdapter;
    private List<View> mViewList;
    private boolean mInited = false;
    private SimpleViewTarget[] mSimpleViewTargets;
    private HackyViewPager mViewPager;
    private boolean transforming = false;
    private OnActionListener onActionListener;
    private PhotoViewAttacher.OnPhotoTapListener onPhotoTapListener;
    private boolean imageLoaded = false;

    private  int posi;
    private Bitmap mSmallBitmap;

    public PhotoFragment() {
        super();
        onPhotoTapListener = new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                transformOut();
            }

            @Override
            public void onOutsidePhotoTap() {
                transformOut();
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Object[] args = (Object[]) getArguments().getSerializable("viewTargetList");

        byte [] bis = getArguments().getByteArray("bitmap");
        mSmallBitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);

        SimpleViewTarget[] viewTargetList = new SimpleViewTarget[args.length];
        for(int i = 0;i<args.length;i++){
            viewTargetList[i] = (SimpleViewTarget) args[i];
        }
        mSimpleViewTargets = viewTargetList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mRootView = inflater.inflate(R.layout.fragment_photo, container,false);
        return mRootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle data = getArguments();

        mViewList = new ArrayList<>();
        Object[] urls = (Object[]) data.getSerializable("urls");
        posi = data.getInt("urlposition",0);

        mPagerAdapter = new MyPagerAdapter(urls,posi);
        mViewPager = (HackyViewPager) mRootView.findViewById(R.id.view_pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(posi);

        IndicatorView indicatorView = (IndicatorView) mRootView.findViewById(R.id.id_indicator);
        indicatorView.setViewPager(mViewPager);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void transformOut(){
        int position = mViewPager.getCurrentItem();
        MySmoothImageView smoothImageView = (MySmoothImageView) mPagerAdapter.getPrimaryItem();
        int mLocationX = mSimpleViewTargets[position].getLocationX();
        int mLocationY = mSimpleViewTargets[position].getLocationY();
        int mWidth = mSimpleViewTargets[position].getWidth();
        int mHeight = mSimpleViewTargets[position].getHeight();
        smoothImageView.setOriginalInfo(mWidth, mHeight, mLocationX, mLocationY,true,true);
        smoothImageView.startTranslateToOrigin();
        smoothImageView.startScaleIn();
    }

    public boolean isTransforming() {
        return transforming;
    }

    public OnActionListener getOnActionListener() {
        return onActionListener;
    }

    public void setOnActionListener(OnActionListener onTransformListener) {
        this.onActionListener = onTransformListener;
    }

    public PhotoViewAttacher.OnPhotoTapListener getOnPhotoTapListener() {
        return onPhotoTapListener;
    }

    public void setOnPhotoTapListener(PhotoViewAttacher.OnPhotoTapListener onPhotoTapListener) {
        this.onPhotoTapListener = onPhotoTapListener;
    }

    public interface OnActionListener {
        int TRANSFORM_OUT = 1;
        int TRANSFORM_IN = 0;
        void onTransformStart(int type);
        void onTransformCompete(int type);
        void onStartLoading();
        void onFinishLoading();
    }

    public class MyPagerAdapter extends PagerAdapter {

        private List<View> viewList;
        private Object[] murls;
        private int mPosition;

        private View mCurrentView;

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            mCurrentView = (View)object;
        }

        public View getPrimaryItem() {
            return mCurrentView;
        }

        public MyPagerAdapter(Object[] urls,int position) {
            viewList = new ArrayList<>();

            murls = urls;
            this.mPosition = position;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            MySmoothImageView imageView = new MySmoothImageView(getActivity());
            imageView.setId(0);

            int mLocationX = mSimpleViewTargets[position].getLocationX();
            int mLocationY = mSimpleViewTargets[position].getLocationY();
            int mWidth = mSimpleViewTargets[position].getWidth();
            int mHeight = mSimpleViewTargets[position].getHeight();

            imageView.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            container.addView(imageView,0);
            imageView.setOriginalInfo(mWidth, mHeight, mLocationX, mLocationY,true,!(position == mPosition&&!mInited));
            String[] fileList = Glide.getPhotoCacheDir(getActivity()).list();
            for(String filename:fileList){
                System.out.println("FFFFF "+filename);
            }
            onActionListener.onStartLoading();
            Glide.with(getActivity()).load((String) murls[position])
                    .asBitmap()
                    .into(new SimpleTarget<Bitmap>(){
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            onActionListener.onFinishLoading();
                            if(position==posi)imageLoaded = true;
                            imageView.setImageBitmap(resource);
                            if(position == mPosition&&!mInited) {
                                imageView.startTranslateToCenter();
                                imageView.startScaleOut();
                                mInited = true;
                            }
                        }
                    });
            if(position == mPosition&&!mInited && mSmallBitmap!=null){
                imageView.setImageBitmap(mSmallBitmap);
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        imageView.startTranslateToCenter();
                    }
                });
            }
            imageView.setOnTransformListener(new MySmoothImageView.TransformListener() {
                @Override
                public void onTransformStart(int state) {
                }

                @Override
                public void onTransformStop(int state) {
                    if(state==0) onActionListener.onTransformCompete(OnActionListener.TRANSFORM_OUT);
                }
            });
            imageView.setOnPhotoTapListener(onPhotoTapListener);
            return imageView;
        }

        @Override
        public int getCount() {
            return murls.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }
    }
}
