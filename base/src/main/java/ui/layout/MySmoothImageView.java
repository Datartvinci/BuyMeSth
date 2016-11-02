package ui.layout;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationSet;
import android.widget.ImageView;

import photoview.PhotoView;

/**
 * Created by ！ on 2016/10/19.
 */

public class MySmoothImageView extends PhotoView implements Animator.AnimatorListener{

    private Paint mPaint;
    private Bitmap mBitmap;
    private final int mBgColor = 0xFF000000;

    private float[] mOriginCenterPosition;//X,Y
    private float[] mTempCenterPosition;//X,Y
    private float[] mEndCenterPosition;//X,Y
    int mOriginWidth; int mOriginHeight; int mOriginLocationX; int mOriginLocationY;
    int mTempWidth; int mTempHeight; int mTempLocationX; int mTempLocationY;
    int mEndWidth; int mEndHeight; int mEndLocationX; int mEndLocationY;
    private int mBgAlpha = 0;
    private boolean mDrawBackground = true;
    private Matrix mSmoothMatrix;
    private long mDuration = 300;

    private int mClipRectLeft = 0;
    private int mClipRectTop = 0;
    private int mClipRectRight = 0;
    private int mClipRectBottom = 0;
    private ValueAnimator translateToCenterAnimator;
    private ValueAnimator translateToOriginAnimator;

    private ValueAnimator scaleOutAnimator;
    private ValueAnimator scaleInAnimator;

    private TransformListener mTransformListener;
    private int mState = 0;
    public static final int STATE_HAVE_TRANSLATE = 1;//01
    public static final int STATE_HAVE_SCALE = 2;//10

    private boolean mInitOrigin = false;
    private boolean mIsScreenPosition;

    public MySmoothImageView(Context context) {
        super(context);
        initThis();
    }

    public MySmoothImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initThis();
    }

    public MySmoothImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initThis();
    }

    private void initThis() {
        mOriginCenterPosition = new float[2];
        mTempCenterPosition = new float[2];
        mEndCenterPosition = new float[2];
        mSmoothMatrix = new Matrix();
        mPaint=new Paint();
        mPaint.setColor(mBgColor);
        mPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * 测量view的位置 将获得的图片屏幕位置转为view内相对位置 要求传入的坐标为屏幕坐标
     */
    private void initOrigin(){
        if(!mIsScreenPosition) return;
        mInitOrigin = true;
        int[] location = new int[2];
        getLocationOnScreen(location);
        mOriginCenterPosition[1] = mOriginCenterPosition[1] - location[1];
        mOriginCenterPosition[0] = mOriginCenterPosition[0] - location[0];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(getDrawable()==null) return;
        if(!mInitOrigin)initOrigin();
        if(mDrawBackground){
            mPaint.setAlpha(mBgAlpha);
            canvas.drawPaint(mPaint);
        }
        if(isAnimRunning()||mState!=(STATE_HAVE_SCALE|STATE_HAVE_TRANSLATE)){
            canvas.save();
            canvas.translate(mTempCenterPosition[0]-mTempWidth/2f,mTempCenterPosition[1]-mTempHeight/2f);
            canvas.clipRect(0,0,mTempWidth,mTempHeight);
            getBitmapMatrix();
            canvas.concat(mSmoothMatrix);
            getDrawable().draw(canvas);
            canvas.restore();
        }else {
            super.onDraw(canvas);
            mState = 3;
            mBgAlpha = 255;
            Bitmap bitmap = ((BitmapDrawable)getDrawable()).getBitmap();
            int h = bitmap.getHeight();
            int w = bitmap.getWidth();
            if(h/(float)w<=getHeight()/(float)getWidth()){//适应宽
                mTempWidth = getWidth();
                mTempHeight = (int)(h* getWidth() / (float) w);
            }else {//适应高
                mTempWidth = (int)(w* getHeight() / (float) h);
                mTempHeight = getHeight();
            }
            this.mTempCenterPosition[0] = getWidth()/2f;
            this.mTempCenterPosition[1] = getHeight()/2f;
        }
    }

    public boolean isAnimRunning() {
        if(scaleInAnimator!=null&&scaleInAnimator.isRunning())
            return true;
        if(scaleOutAnimator!=null&&scaleOutAnimator.isRunning())
            return true;
        if(translateToCenterAnimator!=null&&translateToCenterAnimator.isRunning())
            return true;
        if(translateToOriginAnimator!=null&&translateToOriginAnimator.isRunning())
            return true;
        return false;
    }

    private void getBitmapMatrix(){
        if(getDrawable()==null)return;
        if(!(getDrawable() instanceof BitmapDrawable))return;
        Bitmap bitmap = ((BitmapDrawable)getDrawable()).getBitmap();
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float scale;
        if(h/(float)w<=mTempHeight/(float)mTempWidth){//适应高
            scale = mTempHeight / (float) h;
            Log.d("SmoothIV","scale h "+scale);
            mSmoothMatrix.setScale(scale,scale);
            mSmoothMatrix.postTranslate(-(w*scale-mTempWidth)/2f,0);
        }else {//适应宽
            scale = mTempWidth / (float) w;
            Log.d("SmoothIV","scale w "+scale);
            mSmoothMatrix.setScale(scale,scale);
            mSmoothMatrix.postTranslate(0,-(h*scale-mTempHeight)/2f);
        }
    }

    public void setOriginalInfo(int width, int height, int locationX, int locationY ,boolean isScreenPosition, boolean fill) {
        this.mIsScreenPosition = isScreenPosition;
        this.mOriginWidth = width;
        this.mOriginHeight = height;
        this.mOriginLocationX = locationX;
        this.mOriginLocationY = locationY;
        this.mOriginCenterPosition[0] = (float) (locationX+width/2.0);
        this.mOriginCenterPosition[1] = (float) (locationY+height/2.0);
        if(!fill) {
            mState = 0;
            mBgAlpha = 0;
            mTempWidth = width;
            mTempHeight = height;
            this.mTempCenterPosition[0] = this.mOriginCenterPosition[0];
            this.mTempCenterPosition[1] = this.mOriginCenterPosition[1];
        }else {
            mState = 3;
            mBgAlpha = 255;
        }
    }



    public void startTranslateToCenter(){
        if(translateToOriginAnimator!=null) translateToOriginAnimator.cancel();
        if(translateToCenterAnimator!=null&&translateToCenterAnimator.isRunning())return;
        translateToCenterAnimator = new ValueAnimator();
        translateToCenterAnimator.setDuration(mDuration);
        translateToCenterAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        PropertyValuesHolder positionXHolder = PropertyValuesHolder.ofFloat("centerPositionX", this.mTempCenterPosition[0], getWidth()/2f);
        PropertyValuesHolder positionYHolder = PropertyValuesHolder.ofFloat("centerPositionY", this.mTempCenterPosition[1], getHeight()/2f);
        PropertyValuesHolder alphaHolder = PropertyValuesHolder.ofInt("alpha", mBgAlpha, 255);
        translateToCenterAnimator.setValues(positionXHolder, positionYHolder, alphaHolder);
        translateToCenterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTempCenterPosition[0] = (Float) animation.getAnimatedValue("centerPositionX");
                mTempCenterPosition[1] = (Float) animation.getAnimatedValue("centerPositionY");
                mBgAlpha = (Integer) animation.getAnimatedValue("alpha");
                invalidate();
            }
        });
        translateToCenterAnimator.addListener(this);
        mState = mState|STATE_HAVE_TRANSLATE;
        translateToCenterAnimator.start();
    }

    public void startTranslateToOrigin(){
        if(translateToCenterAnimator!=null) translateToCenterAnimator.cancel();
        if(translateToOriginAnimator!=null&&translateToOriginAnimator.isRunning())return;
        translateToOriginAnimator = new ValueAnimator();
        translateToOriginAnimator.setDuration(mDuration);
        translateToOriginAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        PropertyValuesHolder positionXHolder = PropertyValuesHolder.ofFloat("centerPositionX", this.mTempCenterPosition[0], mOriginCenterPosition[0]);
        PropertyValuesHolder positionYHolder = PropertyValuesHolder.ofFloat("centerPositionY", this.mTempCenterPosition[1], mOriginCenterPosition[1]);
        PropertyValuesHolder alphaHolder = PropertyValuesHolder.ofInt("alpha", mBgAlpha, 0);
        translateToOriginAnimator.setValues(positionXHolder, positionYHolder, alphaHolder);
        translateToOriginAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTempCenterPosition[0] = (Float) animation.getAnimatedValue("centerPositionX");
                mTempCenterPosition[1] = (Float) animation.getAnimatedValue("centerPositionY");
                mBgAlpha = (Integer) animation.getAnimatedValue("alpha");
                invalidate();
            }
        });
        translateToOriginAnimator.addListener(this);
        if((mState&STATE_HAVE_TRANSLATE)!=0)mState = mState - STATE_HAVE_TRANSLATE;
        translateToOriginAnimator.start();
    }

    public void startScaleOut(){
        if(scaleInAnimator!=null)scaleInAnimator.cancel();
        if(scaleOutAnimator!=null&&scaleOutAnimator.isRunning())return;
        scaleOutAnimator = new ValueAnimator();
        scaleOutAnimator.setDuration(mDuration);
        scaleOutAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        if(getDrawable()==null)return;
        if(!(getDrawable() instanceof BitmapDrawable))return;
        Bitmap bitmap = ((BitmapDrawable)getDrawable()).getBitmap();
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float scale;
        PropertyValuesHolder widthHolder;
        PropertyValuesHolder heightHolder;
        if(h/(float)w<=getHeight()/(float)getWidth()){//适应宽
            widthHolder = PropertyValuesHolder.ofInt("width", this.mTempWidth, getWidth());
            heightHolder = PropertyValuesHolder.ofInt("height", this.mTempHeight, (int)(h* getWidth() / (float) w));
        }else {//适应高
            widthHolder = PropertyValuesHolder.ofInt("width", this.mTempWidth, (int)(w* getHeight() / (float) h));
            heightHolder = PropertyValuesHolder.ofInt("height", this.mTempHeight, getHeight());
        }

        scaleOutAnimator.setValues(widthHolder, heightHolder);
        scaleOutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTempWidth = (Integer) animation.getAnimatedValue("width");
                mTempHeight = (Integer) animation.getAnimatedValue("height");
                invalidate();
            }
        });
        scaleOutAnimator.addListener(this);
        mState = mState|STATE_HAVE_SCALE;
        scaleOutAnimator.start();
    }

    public void startScaleIn(){
        if(scaleOutAnimator!=null)scaleOutAnimator.cancel();
        if(scaleOutAnimator!=null&&scaleOutAnimator.isRunning())return;
        scaleInAnimator = new ValueAnimator();
        scaleInAnimator.setDuration(mDuration);
        scaleInAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        PropertyValuesHolder widthHolder;
        PropertyValuesHolder heightHolder;
        widthHolder = PropertyValuesHolder.ofInt("width", this.mTempWidth, this.mOriginWidth);
        heightHolder = PropertyValuesHolder.ofInt("height", this.mTempHeight, this.mOriginHeight);

        scaleInAnimator.setValues(widthHolder, heightHolder);
        scaleInAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTempWidth = (Integer) animation.getAnimatedValue("width");
                mTempHeight = (Integer) animation.getAnimatedValue("height");
                invalidate();
            }
        });
        scaleInAnimator.addListener(this);
        if((mState&STATE_HAVE_SCALE)!=0)mState = mState - STATE_HAVE_SCALE;
        scaleInAnimator.start();
    }

    public int getBgColor() {
        return mBgColor;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long mDuration) {
        this.mDuration = mDuration;
    }

    public boolean isDrawBackground() {
        return mDrawBackground;
    }

    public void setDrawBackground(boolean mDrawBackground) {
        this.mDrawBackground = mDrawBackground;
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if(mTransformListener!=null) mTransformListener.onTransformStart(mState);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if(mTransformListener!=null) mTransformListener.onTransformStop(mState);
    }

    @Override
    public void onAnimationCancel(Animator animation) {
//        if(mTransformListener!=null) mTransformListener.onTransformStop(mState);
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        if(mTransformListener!=null) mTransformListener.onTransformStart(mState);
    }

    public void setOnTransformListener(TransformListener listener) {
        mTransformListener = listener;
    }

    public interface TransformListener {
        void onTransformStart(int state);
        void onTransformStop(int state);
    }
}
