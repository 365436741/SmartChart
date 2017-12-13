package com.daivd.chart.matrix;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import com.daivd.chart.core.base.BaseChart;
import com.daivd.chart.listener.Observable;
import com.daivd.chart.listener.ChartGestureObserver;
import com.daivd.chart.listener.OnChartChangeListener;

import java.util.List;

/**
 * Created by huang on 2017/9/29.
 * 图表放大缩小协助类
 */

public class MatrixHelper extends Observable<ChartGestureObserver> implements ITouch, ScaleGestureDetector.OnScaleGestureListener {

    private static final int MAX_ZOOM = 5;
    public static final int MIN_ZOOM = 1;
    private float zoom = MIN_ZOOM; //缩放比例  不得小于1
    private int translateX; //以左上角为准，X轴位移的距离
    private int translateY;//以左上角为准，y轴位移的距离
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;
    private boolean isCanZoom;
    private boolean isScale; //是否正在缩放
    private Rect originalRect; //原始大小
    private Rect zoomRect;
    private float mDownX, mDownY;
    private int pointMode; //屏幕的手指点个数
    private float widthMultiple =1;
    private Scroller scroller;
    private int mMinimumVelocity;
    private boolean isFling;
    private float flingRate = 0.8f; //速率
    private OnChartChangeListener listener;


    public MatrixHelper(Context context) {
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mGestureDetector = new GestureDetector(context, new OnChartGestureListener());
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        scroller = new Scroller(context);
    }

    /**
     * 处理手势
     */
    @Override
    public boolean handlerTouchEvent(MotionEvent event) {
        if (isCanZoom) {
            mScaleGestureDetector.onTouchEvent(event);
        }
        mGestureDetector.onTouchEvent(event);
        return true;
    }



    /**
     * 判断是否需要接收触摸事件
     */
    @Override
    public void onDisallowInterceptEvent(BaseChart chart, MotionEvent event) {
        if (!isCanZoom) {
            return;
        }
        ViewParent parent = chart.getParent();
        if (zoomRect == null || originalRect == null) {
            parent.requestDisallowInterceptTouchEvent(false);
            return;
        }
        switch (event.getAction()&MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                pointMode = 1;
                //ACTION_DOWN的时候，赶紧把事件hold住
                mDownX = event.getX();
                mDownY = event.getY();
                if(originalRect.contains((int)mDownX,(int)mDownY)){ //判断是否落在图表内容区中
                    parent.requestDisallowInterceptTouchEvent(true);
                }else{
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointMode += 1;
                parent.requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (pointMode > 1) {
                    parent.requestDisallowInterceptTouchEvent(true);
                    return;
                }
                float disX = event.getX() - mDownX;
                float disY = event.getY() - mDownY;
                boolean isDisallowIntercept = true;
                if (Math.abs(disX) > Math.abs(disY)) {
                    if ((disX > 0 && toRectLeft()) || (disX < 0 && toRectRight())) { //向右滑动
                        isDisallowIntercept = false;
                    }
                } else {
                   /* if ((disY > 0 && toRectTop()) || (disY < 0 && toRectBottom())) {*/
                        isDisallowIntercept = false;
                   /* }*/
                }
                parent.requestDisallowInterceptTouchEvent(isDisallowIntercept);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                pointMode -= 1;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                pointMode = 0;
                parent.requestDisallowInterceptTouchEvent(false);
        }

    }

    private boolean toRectLeft() {
        return translateX <= -(zoomRect.width() - originalRect.width()) / 2;
    }

    private boolean toRectRight() {
        return translateX >= (zoomRect.width() - originalRect.width()) / 2;
    }

    private boolean toRectBottom() {

        return translateY >= (zoomRect.height() - originalRect.height()) / 2;
    }

    private boolean toRectTop() {
        return translateY <= -(zoomRect.height() - originalRect.height()) / 2;
    }

    @Override
    public void notifyObservers(List<ChartGestureObserver> observers) {

    }

    private float tempScale = MIN_ZOOM; //缩放比例  不得小于1
    private int tempTranslateX; //以左上角为准，X轴位移的距离
    private int tempTranslateY;//以左上角为准，y轴位移的距离
    class OnChartGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            translateX += distanceX;
            translateY += distanceY;
             notifyViewChanged();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if(Math.abs(velocityX) >mMinimumVelocity || Math.abs(velocityY) >mMinimumVelocity) {
                scroller.setFinalX(0);
                scroller.setFinalY(0);
                tempTranslateX = translateX;
                tempTranslateY = translateY;
                scroller.fling(0,0,(int)velocityX,(int)velocityY,-50000,50000
                        ,-50000,50000);
                isFling = true;
                startFilingAnim(false);
            }

            return true;
        }

        //双击
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (isCanZoom) {
                float oldZoom = zoom;
                if (isScale) { //缩小
                    zoom = zoom / 1.5f;
                    if (zoom < 1) {
                        zoom = MIN_ZOOM;
                        isScale = false;
                    }
                } else { //放大
                    zoom = zoom * 1.5f;
                    if (zoom > MAX_ZOOM) {
                        zoom = MAX_ZOOM;
                        isScale = true;
                    }
                }
                float factor = zoom / oldZoom;
                resetTranslate(factor);
                notifyObservers(observables);
            }

            return true;
        }

        //单击
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            notifyViewChanged();
            for (ChartGestureObserver observer : observables) {
                observer.onClick(e.getX(), e.getY());
            }
            return true;
        }
    }
    private Point startPoint = new Point(0, 0);
    private Point endPoint = new Point();
    private TimeInterpolator interpolator = new DecelerateInterpolator();
    private PointEvaluator evaluator= new PointEvaluator();

    /**
     * 开始飞滚
     * @param doubleWay 双向飞滚
     */
    private void startFilingAnim(boolean doubleWay) {

        int scrollX =Math.abs(scroller.getFinalX());
        int scrollY =Math.abs(scroller.getFinalY());
        if(doubleWay){
            endPoint.set((int) (scroller.getFinalX() * flingRate),
                    (int) (scroller.getFinalY() * flingRate));
        }else {
            if (scrollX > scrollY) {
                endPoint.set((int) (scroller.getFinalX() * flingRate), 0);
            } else {
                endPoint.set(0, (int) (scroller.getFinalY() * flingRate));
            }
        }
        final ValueAnimator valueAnimator = ValueAnimator.ofObject(evaluator,startPoint,endPoint);
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(isFling) {
                    Point point = (Point) animation.getAnimatedValue();
                    translateX = tempTranslateX - point.x;
                    translateY = tempTranslateY - point.y;
                    notifyViewChanged();
                }else{
                    animation.cancel();
                }
            }
        });
        int duration = (int)(Math.max(scrollX,scrollY)*flingRate)/2;
        valueAnimator.setDuration(duration>300 ?300:duration);
        valueAnimator.start();
    }

    public void notifyViewChanged(){
        if(listener != null) {
            listener.onTableChanged(translateX, translateY);
        }
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        tempScale = this.zoom;

        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float oldZoom = zoom;
        float scale = detector.getScaleFactor();
        this.zoom = (float) (tempScale * Math.pow(scale, 2));
        float factor = zoom / oldZoom;
        resetTranslate(factor);
        notifyObservers(observables);
        if (this.zoom > MAX_ZOOM) {
            this.zoom = MAX_ZOOM;
            return true;
        } else if (this.zoom < 1) {
            this.zoom = 1;
            return true;
        }
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }


    /**
     * 重新计算偏移量
     * * @param factor
     */
    private void resetTranslate(float factor) {

        translateX = (int) (translateX * factor);
        translateY = (int) (translateY * factor);
    }

    /**
     * 获取图片内容的缩放大小
     *
     * @param providerRect 内容的大小
     * @return 缩放后内容的大小
     */
    public Rect getZoomProviderRect(Rect providerRect) {
        originalRect = providerRect;
        Rect scaleRect = new Rect();
        int oldw = providerRect.width();
        int oldh = providerRect.height();
        int multipleWidth = (int) (oldw *widthMultiple);

        int newWidth = (int) (multipleWidth * zoom);
        int newHeight = (int) (oldh * zoom);
        int offsetX = (int) (oldw*(zoom-1))/2;
        int offsetY =(int) (oldh*(zoom-1))/2;
        int maxTranslateLeft = (int)Math.abs(oldw*(zoom-1) / 2);
        int maxTranslateRight = newWidth - oldw - offsetX;
        int maxTranslateY = Math.abs(newHeight - oldh) / 2;
        if(translateX >maxTranslateRight){
            translateX = maxTranslateRight;
        }
        if(translateX < -maxTranslateLeft){
            translateX = -maxTranslateLeft;
        }
        if (Math.abs(translateY) > maxTranslateY) {
            translateY = translateY > 0 ? maxTranslateY : -maxTranslateY;
        }

        scaleRect.left = providerRect.left - offsetX - translateX;
        scaleRect.right=scaleRect.left+newWidth;
        scaleRect.top = providerRect.top - offsetY - translateY;
        scaleRect.bottom = scaleRect.top+newHeight;
        zoomRect = scaleRect;
        return scaleRect;
    }


    public float getWidthMultiple() {
        return widthMultiple;
    }

    public void setWidthMultiple(float widthMultiple) {
        this.widthMultiple = widthMultiple;
    }

    public boolean isCanZoom() {
        zoom = 1f;
        return isCanZoom;

    }

    public void setCanZoom(boolean canZoom) {
        isCanZoom = canZoom;
    }

    public float getZoom() {
        return zoom;
    }

    public OnChartChangeListener getOnTableChangeListener() {
        return listener;
    }

    public void setOnTableChangeListener(OnChartChangeListener onTableChangeListener) {
        this.listener = onTableChangeListener;
    }

}
