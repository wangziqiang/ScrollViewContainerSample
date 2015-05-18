package com.sf.wzq.ScrollViewContainerSample;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

/**
 * Created by WangZiQiang on 2015/5/15.<br/>
 * 仿淘宝商品详情页的上拉(下拉)自动滑动的效果<br></>
 */
public class ScrollViewContainer extends RelativeLayout {
    private int childCount = 2;//默认只包含2个ScrollView

    public ScrollViewContainer(Context context) {
        super(context);
    }

    public ScrollViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScrollViewContainer);
        childCount = a.getInt(R.styleable.ScrollViewContainer_children_count, 2);//默认只有2个child
        a.recycle();
        init();
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
        if(childCount == 2){
            topSV.layout(0,mMoveLength,mWidth,mMoveLength+topSVHeight);
            bottomSV.layout(0,mMoveLength+topSVHeight,mWidth,mMoveLength+topSVHeight+bottomSV.getMeasuredHeight());
        }else{

        }

    }

    private void init() {

    }
}
