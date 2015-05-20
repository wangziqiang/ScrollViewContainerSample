package com.sf.wzq.ScrollViewContainerSample;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by WangZiQiang on 2015/5/15.<br/>
 * 仿淘宝商品详情页的上拉(下拉)自动滑动的效果<br></>
 */
public class ScrollViewContainer extends RelativeLayout {
    private int childCount = 2;//默认只包含2个ScrollView
    private VelocityTracker velocityTracker;
    private Context mContext;
    private String TAG = "ScrollViewContainer";
    private int minFlingVelocity, maxFlingVelocity, mTouchSlop;
    private MyTask mTask;

    public ScrollViewContainer(Context context) {
        super(context);
    }

    public ScrollViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScrollViewContainer);
        childCount = a.getInt(R.styleable.ScrollViewContainer_children_count, 2);//默认只有2个child
        a.recycle();
        init();
    }

    private void init() {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(mContext);
        minFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        maxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        Log.i(TAG, "getScaledMinimumFlingVelocity() = " + minFlingVelocity);
        Log.i(TAG, "getScaledTouchSlop() = " + mTouchSlop);
        Log.i(TAG, "getScaledMaximumFlingVelocity() = " + maxFlingVelocity);
        mTask = new MyTask(mHandler);
    }

    private boolean isMeasure = false;//默认只测量一次
    private ScrollView topSV;
    private ScrollView bottomSV;
    private View centerView;
    private int topSVHeight;
    private int centerViewHeight;
    private int mWidth;//ScrollViewContainer的宽度
    private int mHeight;//ScrollViewContainer的高度
    private int mMoveLength;//手指移动的距离
    //一些状态
    private static final int STATE_TOP_SC = 10;//topScrollView可见，并且没有到底
    private static final int STATE_CAN_UP = 11;//topScrollView可见，并且到底，可以上滑了
    private static final int STATE_AUTO_UP = 12;//自动上滑
    private static final int STATE_CAN_DOWN = 13;//bottomScrollView可见，并且到顶，可以下滑了
    private static final int STATE_AUTO_DOWN = 14;//自动下滑
    private static final int STATE_BOTTOM_SC = 15;//bottomScrollView可见，并且没有到顶

    private int mState = STATE_TOP_SC;//默认状态是：topScrollView可见，并且没有到底

    private static final int TOP_SCROLLVIEW = 0;//当前可见的view是topScrollView
    private static final int BOTTOM_SCROLLVIEW = 1;//当前可见的view是bottomScrollView
    private int mCurrentView = TOP_SCROLLVIEW;//默认当前可见的view

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!isMeasure) {
            isMeasure = true;
            mWidth = getMeasuredWidth();
            mHeight = getMeasuredHeight();
            if (childCount == 2) {
                topSV = (ScrollView) getChildAt(0);
                bottomSV = (ScrollView) getChildAt(1);
            } else if (childCount == 3) {
                topSV = (ScrollView) getChildAt(0);
                centerView = getChildAt(1);
                bottomSV = (ScrollView) getChildAt(2);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (childCount == 2) {
            topSV.layout(0, mMoveLength, mWidth, mMoveLength + topSVHeight);
            bottomSV.layout(0, mMoveLength + topSVHeight, mWidth, mMoveLength + topSVHeight + bottomSV.getMeasuredHeight());
        } else {

        }

    }

    //检查当前ScrollViewContainer的状态
    private void checkState() {
        //检查顶部ScrollView的状态
        if (mCurrentView == TOP_SCROLLVIEW) {
            if ((topSV.getScaleY() + topSV.getMeasuredHeight()) >= topSV.getChildAt(0).getMeasuredHeight())
                mState = STATE_CAN_UP;//此时：topScrollView可见，并且到底，可以上滑
            else
                mState = STATE_TOP_SC;//此时：topScrollView可见，并且没到底
        }
        //检查底部ScrollView的状态
        if (mCurrentView == BOTTOM_SCROLLVIEW) {
            if (bottomSV.getScaleY() <= 0)
                mState = STATE_CAN_DOWN;//此时：bottomScrollView可见，并且到顶，可以下滑
            else
                mState = STATE_BOTTOM_SC;//此时：bottomScrollView可见，并且没到顶
        }

    }

    /**
     * 设置当前ScrollViewContainer的状态
     *
     * @param state
     */
    private void setState(int state) {
        mState = state;
        switch (mState) {
            case STATE_TOP_SC:
            case STATE_CAN_UP:
                mCurrentView = TOP_SCROLLVIEW;
                mMoveLength = 0;
                break;
            case STATE_CAN_DOWN:
            case STATE_BOTTOM_SC:
                mCurrentView = BOTTOM_SCROLLVIEW;
                mMoveLength = -mHeight;
                break;
            default:
                mCurrentView = TOP_SCROLLVIEW;
                mMoveLength = 0;
                break;
        }
    }

    private float mLastY;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 每次手指滑动前检查ScrollViewContainer的状态
                checkState();
                mLastY = ev.getY();
                if (velocityTracker == null)
                    velocityTracker = VelocityTracker.obtain();
                else
                    velocityTracker.clear();
                velocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(ev);

                if (mState == STATE_CAN_UP) { // 如果能上滑
                    mMoveLength += (ev.getY() - mLastY);
                    if (mMoveLength > 0) {//此时用户下滑了，到了 STATE_TOP_SC 状态
                        setState(STATE_TOP_SC);
                    } else if (mMoveLength <= -mHeight) {//此时用户上滑了超过mHeight的距离
                        setState(STATE_CAN_DOWN);
                    }
                    requestLayout();//根据手势改变layout参数
                } else if (mState == STATE_CAN_DOWN) {//如果能下滑
                    mMoveLength += (ev.getY() - mLastY);
                    if (mMoveLength < -mHeight) {//此时用户上滑了，到了 STATE_BOTTOM_SC 的状态
                        setState(STATE_BOTTOM_SC);
                    } else if (mMoveLength >= 0) {//此时用户下滑了超过mHeight的距离
                        setState(STATE_CAN_UP);
                    }
                    requestLayout();//根据手势改变layout参数
                } else {//其他状态
                    if (mMoveLength == 0)
                        setState(STATE_TOP_SC);
                    else if (mMoveLength == -mHeight)
                        setState(STATE_BOTTOM_SC);
                }
                mLastY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                mLastY = ev.getY();
                velocityTracker.addMovement(ev);
                velocityTracker.computeCurrentVelocity(1000);
                float yVelocity = velocityTracker.getYVelocity();
                if (mState == STATE_TOP_SC || mState == STATE_BOTTOM_SC)
                    break;
                //如果速度够快
                if (Math.abs(yVelocity) > minFlingVelocity) {
                    if (yVelocity > 0) {//下滑
                        setState(STATE_AUTO_DOWN);
                    } else {
                        setState(STATE_AUTO_UP);
                    }
                } else {//
                    if (mMoveLength > -mHeight / 2) {
                        setState(STATE_AUTO_DOWN);
                    } else if (mMoveLength <= -mHeight / 2) {
                        setState(STATE_AUTO_UP);
                    }
                }
                // 不断invoke requestLayout();
                velocityTracker.recycle();
                break;
        }
        try {
            super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    class MyTask extends TimerTask {
        private Handler handler;

        public MyTask(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            handler.obtainMessage().sendToTarget();
        }
    }

    class MyTimer {
        private Handler handler;
        private MyTask myTask;
        private Timer timer;

        public MyTimer(Handler handler) {
            this.handler = handler;
            timer = new Timer();
        }

        public void schedule(long period) {
            cancel();
            myTask = new MyTask(handler);
            timer.schedule(myTask, period);
        }

        public void cancel() {
            if (myTask != null) {
                myTask.cancel();
                myTask = null;
            }
        }
    }

    private int speed = 9;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mMoveLength != 0 || mMoveLength != -mHeight) {
                if (mState == STATE_AUTO_UP) {
                    mMoveLength -= speed;
                    if (mMoveLength <= -mHeight) {
                        setState(STATE_CAN_DOWN);
                    }
                } else if (mState == STATE_AUTO_DOWN) {
                    mMoveLength += speed;
                    if (mMoveLength >= 0) {
                        setState(STATE_AUTO_UP);
                    }
                } else {
                    mTask.cancel();
                }
                requestLayout();
            }
        }
    };

}
