package com.lurencun.imagemap.internal;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.Scroller;

import com.lurencun.imagemap.internal.Bubble.OnBubbleClickListener;
import com.lurencun.imagemap.internal.Bubble.OnShapeClickListener;

@SuppressLint("UseSparseArrays")
public class ImageMap extends ImageView {
	
	// mFitImageToScreen
	// if true - initial image resized to fit the screen, aspect ratio may be broken
	// if false- initial image resized so that no empty screen is visible, aspect ratio maintained
	//           image size will likely be larger than screen
	private boolean mFitImageToScreen=false;
	
	/**
	 * 是否开启高亮选中区域
	 */
	private boolean mEnableHighlightArea=true;
	
	// For certain images, it is best to always resize using the original
	// image bits.  This requires keeping the original image in memory along with the
	// current sized version and thus takes extra memory.
	// If you always want to resize using the original, set mScaleFromOriginal to true
	// If you want to use less memory, and the image scaling up and down repeatedly
	// does not blur or loose quality, set mScaleFromOriginal to false
	private boolean mScaleFromOriginal=true;
	
	// mMaxSize controls the maximum zoom size as a multiplier of the initial size.
	// Allowing size to go too large may result in memory problems.
	//  set this to 1.0f to disable resizing
	private float mMaxSize = 1.5f;
	
	/**
	 * 高亮区域的透明度。[0..255]
	 */
	private int mHighlightAlpha = 200;
	
	/**
	 * 高亮区域的透明
	 */
	private int mHighlightColor = Color.RED;
	
	/*
	 * Touch event handling variables
	 */
	private VelocityTracker mVelocityTracker;

	private int mTouchSlop;
	private int mMinimumVelocity;
	private int mMaximumVelocity;

	private Scroller mScroller;

	private boolean mIsBeingDragged = false;	
	
	HashMap<Integer,TouchPoint> mTouchPoints = new HashMap<Integer,TouchPoint>();
	TouchPoint mMainTouch=null;
	TouchPoint mPinchTouch=null;
	
	/*
	 * Pinch zoom
	 */
	float mInitialDistance;
	boolean mZoomEstablished=false;
	int mLastDistanceChange=0;
	boolean mZoomPending=false;
	
	/*
	 * Bitmap handling
	 */
	Bitmap mImage;
	Bitmap mOriginal;
	

	// Info about the bitmap (sizes, scroll bounds)
	// initial size
	int mImageHeight;  
	int mImageWidth;
	float mAspect;
	// scaled size
	int mExpandWidth;
	int mExpandHeight;
	// the right and bottom edges (for scroll restriction)
	int mRightBound;
	int mBottomBound;
	// the current zoom scaling (X and Y kept separate)
	private float mResizeFactorX;
	private float mResizeFactorY;
	// minimum height/width for the image
	int mMinWidth=-1;
	int mMinHeight=-1;
	// maximum height/width for the image
	int mMaxWidth=-1;
	int mMaxHeight=-1;

	// the position of the top left corner relative to the view	
	int mScrollTop;
	int mScrollLeft;

	// view height and width
	int mViewHeight=-1;
	int mViewWidth=-1;

	/*
	 * containers for the image map areas
	 */
	ArrayList<ShapeArea> mAreaList=new ArrayList<ShapeArea>();
	HashMap<Integer,ShapeArea> mIdToArea= new HashMap<Integer,ShapeArea>();
	
	OnBubbleClickListener mBubbleListener;
	OnShapeClickListener mShapeListener;
	
	BubbleDisplayer mBubbleDisplayer;
	
	// list of open info bubbles
	HashMap<Integer,Bubble> mBubbleMap = new HashMap<Integer,Bubble>();

	int areaIdPool = 1;
	
	/*
	 * Constructors
	 */
	public ImageMap(Context context) {
		super(context);
		init();
	}

	public ImageMap(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ImageMap(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public int addShape(Shape shape){
		int areaId = areaIdPool;
		areaIdPool++;
		ShapeArea area = null;
		switch(shape.type){
		case RECT:
			area = new RectArea(areaId, 
					shape.coords[0],
					shape.coords[1],
					shape.coords[2],
					shape.coords[3]
					);
			break;
		case CIRCLE:
			area = new CircleArea(areaId, 
					shape.coords[0],
					shape.coords[1],
					shape.coords[2]
					);
			break;
		default:
			area = new PolyArea(areaId,shape.coords);	
			break;
		}
		area.data = shape.data;
		mAreaList.add(area);
		mIdToArea.put(areaId, area);
		return areaId;
	}
	
	public void addBubble(View view, int areaId ) {
		if (mBubbleMap.get(areaId) == null) {
			ShapeArea area = mIdToArea.get(areaId);
			Bubble bubble = new Bubble(view,area,mResizeFactorX,mResizeFactorY);
			bubble.setDisplayer(mBubbleDisplayer);
			bubble.setOnBubbleClickListener(mBubbleListener);
			mBubbleMap.put(areaId,bubble);
			if(area != null){
				area.bubble = bubble;
			}
		}
	}
	
	/**
	 * 显示指定区域ID的Bubble，并指定是否清除其它Bubble。
	 * @param cleanOtherBubbles 是否清除其它已经显示的Bubble
	 * @param shapeId
	 */
	public void showBubble(boolean cleanOtherBubbles, int shapeId) {
		if(cleanOtherBubbles){
			cleanBubbles();
		}
		ShapeArea area = mIdToArea.get(shapeId);
		if(area != null){
			addBubble(area.bubble.bubbleView, shapeId);
		}
		postInvalidate();
	}
	
	void cleanBubbles(){
		for (Bubble b : mBubbleMap.values()) {
			mBubbleDisplayer.hideBubbleView(b.bubbleView);
		}
		mBubbleMap.clear();
	}
	
	/**
	 * 将屏幕中心移动到指定ID的区域中
	 * @param areaId
	 */
	public void moveAreaToCenter( int areaId ) {
		ShapeArea a = mIdToArea.get(areaId);
		if (a != null) {
			float x = a.getOriginX()*mResizeFactorX;
			float y = a.getOriginY()*mResizeFactorY;
			int left = (int)((mViewWidth/2)-x);
			int top  = (int)((mViewHeight/2)-y);
			moveTo(left,top);
		}
	}
	
	/**
	 * initialize the view
	 */
	private void init() {
		// create a scroller for flinging
		mScroller = new Scroller(getContext());

		// get some default values from the system for touch/drag/fling
		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	/*
	 * These methods will be called when images or drawables are set
	 * in the XML for the view.  We handle either bitmaps or drawables
	 * @see android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)
	 */
	@Override
	public void setImageBitmap(Bitmap bm) {
		if (mImage==mOriginal) {
			mOriginal=null;
		} else {
			mOriginal.recycle();
			mOriginal=null;
		}
		if (mImage != null) {
			mImage.recycle();
			mImage=null;
		}
		mImage = bm;
		mOriginal = bm;
		mImageHeight = mImage.getHeight();
		mImageWidth = mImage.getWidth();
		mAspect = (float)mImageWidth / mImageHeight;
		setInitialImageBounds();
	}

	@Override
	public void setImageResource(int resId) {
		Bitmap b = BitmapFactory.decodeResource(getResources(), resId);
		setImageBitmap(b);
	}

	@Override
	public void setImageDrawable(Drawable drawable) {		
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bd = (BitmapDrawable) drawable; 			
			setImageBitmap(bd.getBitmap());
		}
	}
	
	

	/*
	 * Called by the scroller when flinging
	 * @see android.view.View#computeScroll()
	 */
	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			int oldX = mScrollLeft;
			int oldY = mScrollTop;
			
			int x = mScroller.getCurrX();
			int y = mScroller.getCurrY();

			if (oldX != x) {
				moveX(x-oldX);
			}
			if (oldY != y) {
				moveY(y-oldY);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);

		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);

		setMeasuredDimension(chosenWidth, chosenHeight);
	}

	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return getPreferredSize();
		}
	}

	/**
	 * set the initial bounds of the image
	 */
	void setInitialImageBounds() {
		if (mFitImageToScreen) {
			setInitialImageBoundsFitImage();
		} else {
			setInitialImageBoundsFillScreen();
		}
	}
	
	/**
	 * setInitialImageBoundsFitImage sets the initial image size to match the
	 * screen size.  aspect ratio may be broken
	 */	
	void setInitialImageBoundsFitImage() {
		if (mImage != null) {
			if (mViewWidth > 0) {			
				mMinHeight = mViewHeight;
				mMinWidth = mViewWidth; 
				mMaxWidth = (int)(mMinWidth * mMaxSize);
				mMaxHeight = (int)(mMinHeight * mMaxSize);				
					
				mScrollTop = 0;
				mScrollLeft = 0;
				scaleBitmap(mMinWidth, mMinHeight);
		    }
		}
	}
	
	/**
	 * 设置是否显示图片时强制压缩图片尺寸，以适配屏幕大小。默认为false,图片滑动查看。
	 * @param fitImageToScreen
	 */
	public void setFitImageToScreen(boolean fitImageToScreen) {
		this.mFitImageToScreen = fitImageToScreen;
	}
	
	/**
	 * 设置高亮区域的透明度。取值：[0...255]
	 * @param alpha
	 */
	public void setHighlightAlapha(int alpha){
		mHighlightAlpha = alpha;
	}
	
	/**
	 * 设置高亮区域颜色
	 * @param color
	 */
	public void setHighlightColor(int color){
		mHighlightColor = color;
	}

	/**
	 * 设置缩放图片时，是否从源图片压缩。
	 * @param scaleFromOriginal
	 */
	public void setScaleFromOriginal(boolean scaleFromOriginal) {
		this.mScaleFromOriginal = scaleFromOriginal;
	}
	
	public void setOnBubbleClickeListener( OnBubbleClickListener listener ) {
		mBubbleListener = listener;
	}
	
	public void setOnShapeClickeListener( OnShapeClickListener listener ) {
		mShapeListener = listener;
	}
	
	public void setBubbleDisplayer(BubbleDisplayer displayer){
		mBubbleDisplayer = displayer;
	}
	
	protected Paint getHighlightPaint(){
		Paint p = new Paint();  
        p.setColor(mHighlightColor); 
        p.setAntiAlias(true);
        p.setAlpha(mHighlightAlpha);
        p.setStyle(Paint.Style.FILL);  
        return p;
	}

	/**
	 * setInitialImageBoundsFillScreen sets the initial image size to so that there
	 * is no uncovered area of the device
	 */	
	void setInitialImageBoundsFillScreen() {
		if (mImage != null && mViewWidth > 0) {
			boolean resize = false;

			int newWidth = mImageWidth;
			int newHeight = mImageHeight;

			// The setting of these max sizes is very arbitrary
			// Need to find a better way to determine max size
			// to avoid attempts too big a bitmap and throw OOM
			if (mMinWidth == -1) {
				// set minimums so that the largest
				// direction we always filled (no empty view space)
				// this maintains initial aspect ratio
				if (mViewWidth > mViewHeight) {
					mMinWidth = mViewWidth;
					mMinHeight = (int) (mMinWidth / mAspect);
				} else {
					mMinHeight = mViewHeight;
					mMinWidth = (int) (mAspect * mViewHeight);
				}
				mMaxWidth = (int) (mMinWidth * 1.5f);
				mMaxHeight = (int) (mMinHeight * 1.5f);
			}

			if (newWidth < mMinWidth) {
				newWidth = mMinWidth;
				newHeight = (int) (((float) mMinWidth / mImageWidth) * mImageHeight);
				resize = true;
			}
			if (newHeight < mMinHeight) {
				newHeight = mMinHeight;
				newWidth = (int) (((float) mMinHeight / mImageHeight) * mImageWidth);
				resize = true;
			}

			mScrollTop = 0;
			mScrollLeft = 0;

			// scale the bitmap
			if (resize) {
				scaleBitmap(newWidth, newHeight);
			} else {
				mExpandWidth = newWidth;
				mExpandHeight = newHeight;
				mResizeFactorX = ((float) newWidth / mImageWidth);
				mResizeFactorY = ((float) newHeight / mImageHeight);
				mRightBound = 0 - (mExpandWidth - mViewWidth);
				mBottomBound = 0 - (mExpandHeight - mViewHeight);
			}
		}
	}
	
	
	/**
	 * Set the image to new width and height
	 * create a new scaled bitmap and dispose of the previous one
	 * recalculate scaling factor and right and bottom bounds
	 * @param newWidth
	 * @param newHeight
	 */
	void scaleBitmap(int newWidth, int newHeight) {
		// Technically since we always keep aspect ratio intact
		// we should only need to check one dimension.
		// Need to investigate and fix
		if ((newWidth > mMaxWidth) || (newHeight > mMaxHeight)) {
			newWidth = mMaxWidth;
			newHeight = mMaxHeight;
		}
		if ((newWidth < mMinWidth) || (newHeight < mMinHeight)) {
			newWidth = mMinWidth;
			newHeight = mMinHeight;			
		}

		if ((newWidth != mExpandWidth) || (newHeight!=mExpandHeight)) {	
			// NOTE: depending on the image being used, it may be 
			//       better to keep the original image available and
			//       use those bits for resize.  Repeated grow/shrink
			//       can render some images visually non-appealing
			//       see comments at top of file for mScaleFromOriginal
			// try to create a new bitmap
			// If you get a recycled bitmap exception here, check to make sure
			// you are not setting the bitmap both from XML and in code
			Bitmap newbits = Bitmap.createScaledBitmap(mScaleFromOriginal ? mOriginal:mImage, newWidth,
					newHeight, true);
			// if successful, fix up all the tracking variables
			if (newbits != null) {
				if (mImage!=mOriginal) {
					mImage.recycle();
				}
				mImage = newbits;
				mExpandWidth=newWidth;
				mExpandHeight=newHeight;
				mResizeFactorX = ((float) newWidth / mImageWidth);
				mResizeFactorY = ((float) newHeight / mImageHeight);
				
				mRightBound = mExpandWidth>mViewWidth ? 0 - (mExpandWidth - mViewWidth) : 0;
				mBottomBound = mExpandHeight>mViewHeight ? 0 - (mExpandHeight - mViewHeight) : 0;
			}							
		}
	}
	
	void resizeBitmap( int amount ) {
		int adjustWidth = amount;
		int adjustHeight = (int)(adjustWidth / mAspect);
		scaleBitmap( mExpandWidth+adjustWidth, mExpandHeight+adjustHeight);
	}

	/**
	 * watch for screen size changes and reset the background image
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		// save device height width, we use it a lot of places
		mViewHeight = h;
		mViewWidth = w;

		// fix up the image
		setInitialImageBounds();
	}

	private int getPreferredSize() {
		return 300;
	}

	/**
	 * the onDraw routine when we are using a background image
	 * 
	 * @param canvas
	 */
	protected void drawMap(Canvas canvas) {
		canvas.save();
		if (mImage != null) {
			if (!mImage.isRecycled()) {
				canvas.drawBitmap(mImage, mScrollLeft, mScrollTop, null);
			}
		}
		canvas.restore();
	}
	
	protected void drawBubbles(Canvas canvas) {
		for (Bubble b : mBubbleMap.values()) {
			b.drawBubble(canvas,mScrollLeft,mScrollTop);
			b.drawShape(canvas,mEnableHighlightArea);
		}
	}
	
	protected void drawLocations(Canvas canvas) {
		for (ShapeArea area : mAreaList) {
			area.doDrawing(canvas,mResizeFactorX,mResizeFactorY,mScrollLeft,mScrollTop);
		}	
	}

	/**
	 * Paint the view
	 *   image first, location decorations next, bubbles on top
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		drawMap(canvas);
		drawLocations(canvas);
		drawBubbles(canvas);
	}
	
	/*
	 * Touch handler
	 *   This handler manages an arbitrary number of points
	 *   and detects taps, moves, flings, and zooms 
	 */	
	public boolean onTouchEvent(MotionEvent ev) {
		int id;
		
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);
		
		final int action = ev.getAction();
		
		int pointerCount = ev.getPointerCount(); 
        int index = 0;
        
        if (pointerCount > 1) {
        	// If you are using new API (level 8+) use these constants
        	// instead as they are much better names
        	//index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK);
        	//index = index >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        	        	
            // for api 7 and earlier we are stuck with these
        	// constants which are poorly named
        	// ID refers to INDEX, not the actual ID of the pointer
        	index = (action & MotionEvent.ACTION_POINTER_ID_MASK);
        	index = index >> MotionEvent.ACTION_POINTER_ID_SHIFT;
        }

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			// Clear all touch points
			// In the case where some view up chain is messing with our
			// touch events, it is possible to miss UP and POINTER_UP 
			// events.  Whenever ACTION_DOWN happens, it is intended
			// to always be the first touch, so we will drop tracking
			// for any points that may have been orphaned
			for ( TouchPoint t: mTouchPoints.values() ) {
				onLostTouch(t.getTrackingPointer());
			}
			// fall through planned
		case MotionEvent.ACTION_POINTER_DOWN:
			id = ev.getPointerId(index);
			onTouchDown(id,ev.getX(index),ev.getY(index));
			break;

		case MotionEvent.ACTION_MOVE:
			for (int p=0;p<pointerCount;p++) {
				id = ev.getPointerId(p);
				TouchPoint t = mTouchPoints.get(id);
				if (t!=null) {
					onTouchMove(t,ev.getX(p),ev.getY(p));		
				}
				// after all moves, check to see if we need
				// to process a zoom
				processZoom();
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			id = ev.getPointerId(index);			
			onTouchUp(id);
			break;
		case MotionEvent.ACTION_CANCEL:
			// Clear all touch points on ACTION_CANCEL
			// according to the google devs, CANCEL means cancel 
			// tracking every touch.  
			// cf: http://groups.google.com/group/android-developers/browse_thread/thread/8b14591ead5608a0/ad711bf24520e5c4?pli=1
			for ( TouchPoint t: mTouchPoints.values() ) {
				onLostTouch(t.getTrackingPointer());
			}
			// let go of the velocity tracker per API Docs
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			break;
		}
		return true;
	}
	
	
	void onTouchDown(int id, float x, float y) {
        // create a new touch point to track this ID
		TouchPoint t=null;
		synchronized (mTouchPoints) {
			// This test is a bit paranoid and research should
			// be done sot that it can be removed.  We should
			// not find a touch point for the id
			t = mTouchPoints.get(id);
			if (t == null) {
				t = new TouchPoint(id);
				mTouchPoints.put(id,t);
			}
			
			// for pinch zoom, we need to pick two touch points
			// they will be called Main and Pinch 
			if (mMainTouch == null) {
				mMainTouch = t;
			} else {
				if (mPinchTouch == null) {
					mPinchTouch=t;
					// second point established, set up to 
					// handle pinch zoom
					startZoom();
				}
			}
		}
		t.setPosition(x,y);		
	}
	
	/*
	 * Track pointer moves
	 */
	void onTouchMove(TouchPoint t, float x, float y) {
		// mMainTouch will drag the view, be part of a
		// pinch zoom, or trigger a tap
		if (t == mMainTouch) {
			if (mPinchTouch == null) {
				// only on point down, this is a move
				final int deltaX = (int) (t.getX() - x);
				final int xDiff = (int) Math.abs(t.getX() - x);

				final int deltaY = (int) (t.getY() - y);
				final int yDiff = (int) Math.abs(t.getY() - y);

				if (!mIsBeingDragged) {
					if ((xDiff > mTouchSlop) || (yDiff > mTouchSlop)) {
						// start dragging about once the user has
						// moved the point far enough
						mIsBeingDragged = true;
					}
				} else {
					// being dragged, move the image
					if (xDiff > 0) {
						moveX(-deltaX);
					}
					if (yDiff > 0) {
						moveY(-deltaY);
					}
					t.setPosition(x, y);					
				}
			} else {
				// two fingers down means zoom				
				t.setPosition(x, y);
				onZoom();
			}
		} else {
			if (t == mPinchTouch) {
				// two fingers down means zoom
				t.setPosition(x, y);
				onZoom();
			}
		}
	}
	
	/*
	 * touch point released
	 */
	void onTouchUp(int id) {
		synchronized (mTouchPoints) {
			TouchPoint t = mTouchPoints.get(id);
			if (t != null) {
				if (t == mMainTouch) {
					if (mPinchTouch==null) {						
						// This is either a fling or tap
						if (mIsBeingDragged) {
							// view was being dragged means this is a fling
							final VelocityTracker velocityTracker = mVelocityTracker;
							velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
							
							int xVelocity = (int) velocityTracker.getXVelocity();
							int yVelocity = (int) velocityTracker.getYVelocity();

							int xfling = Math.abs(xVelocity) > mMinimumVelocity ? xVelocity : 0;
							int yfling = Math.abs(yVelocity) > mMinimumVelocity ? yVelocity : 0;

							if ((xfling != 0) || (yfling != 0)) {
								fling(-xfling, -yfling);
							}

							mIsBeingDragged = false;

							// let go of the velocity tracker
							if (mVelocityTracker != null) {
								mVelocityTracker.recycle();
								mVelocityTracker = null;
							}
						} else {
							// no movement - this was a tap
							onScreenTapped((int)mMainTouch.getX(), (int)mMainTouch.getY());
						}
					} 					
					mMainTouch=null;
					mZoomEstablished=false;
				}
				if (t == mPinchTouch) {
					// lost the 2nd pointer
					mPinchTouch=null;
					mZoomEstablished=false;
				}
				mTouchPoints.remove(id);
				// shuffle remaining pointers so that we are still
				// tracking.  This is necessary for proper action
				// on devices that support > 2 touches 
				regroupTouches();
			} else {
				// lost this ID somehow
				// This happens sometimes due to the way some
				// devices manage touch
			}
		}
	}
	
	/*
	 * Touch handling varies from device to device, we may think we
	 * are tracking an id which goes missing
	 */
	void onLostTouch(int id) {
		synchronized (mTouchPoints) {
			TouchPoint t = mTouchPoints.get(id);
			if (t != null) {
				if (t == mMainTouch) {
					mMainTouch=null;
				}
				if (t == mPinchTouch) {
					mPinchTouch=null;
				}
				mTouchPoints.remove(id);
				regroupTouches();
			}
		}
	}
	
	/*
	 * find a touch pointer that is not being used as main or pinch
	 */
	TouchPoint getUnboundPoint() {
		TouchPoint ret=null;		
		for (Integer i : mTouchPoints.keySet()) {
			TouchPoint p = mTouchPoints.get(i);
			if ((p!=mMainTouch)&&(p!=mPinchTouch)) {
				ret = p;
				break;
			}
		}
		return ret;
	}
	
	/*
	 * go through remaining pointers and try to have
	 * MainTouch and then PinchTouch if possible
	 */
	void regroupTouches() {
		int size = mTouchPoints.size();
		if (size>0) {
			if (mMainTouch == null) {
				if (mPinchTouch != null) {
					mMainTouch=mPinchTouch;
					mPinchTouch=null;
				} else {
					mMainTouch=getUnboundPoint();
				}
			}
			if (size>1) {
				if (mPinchTouch == null) {
					mPinchTouch=getUnboundPoint();
					startZoom();
				}
			}
		}
	}
	
	/*
	 * Called when the second pointer is down indicating that we
	 * want to do a pinch-zoom action
	 */
	void startZoom() {
		// this boolean tells the system that it needs to 
		// initialize itself before trying to zoom
		// This is cleaner than duplicating code
		// see processZoom
		mZoomEstablished=false;
	}	
	
	/*
	 * one of the pointers for our pinch-zoom action has moved
	 * Remember this until after all touch move actions are processed.
	 */
	void onZoom() {
		mZoomPending=true;
	}

	/*
	 * All touch move actions are done, do we need to zoom?
	 */
	void processZoom() {
		if (mZoomPending) {
	 		// check pinch distance, set new scale factor
			float dx=mMainTouch.getX()-mPinchTouch.getX();
			float dy=mMainTouch.getY()-mPinchTouch.getY();
			float newDistance=(float)Math.sqrt((dx*dx)+(dy*dy));
			if (mZoomEstablished) {		
				// baseline was set, check to see if there is enough
				// movement to resize
				int distanceChange=(int)(newDistance-mInitialDistance);
				int delta=distanceChange-mLastDistanceChange;
				if (Math.abs(delta)>mTouchSlop) {
					mLastDistanceChange=distanceChange;
					resizeBitmap(delta);
					invalidate();
				}
			} else {
				// first run through after touches established
				// just set baseline
				mLastDistanceChange=0;				
				mInitialDistance=newDistance;
				mZoomEstablished=true;
			}
			mZoomPending=false;
		}
	}

	/*
	 * Screen tapped x, y is screen coord from upper left and does not account
	 * for scroll
	 */
	void onScreenTapped(int x, int y) {
		boolean isLostFocus = true;
		boolean isSelectedBubbles = false;
		// adjust for scroll
		int testx = x-mScrollLeft;
		int testy = y-mScrollTop;
		
		// adjust for x y resize
		testx = (int)((float)testx/mResizeFactorX);
		testy = (int)((float)testy/mResizeFactorY);

		// check if bubble tapped first
		// in case a bubble covers an area we want it to 
		// have precedent
		for (Bubble b : mBubbleMap.values()) {
			if (b.isInArea((float)x-mScrollLeft,(float)y-mScrollTop)) {
				b.onTapped();
				isSelectedBubbles=true;
				isLostFocus=false;
				// only fire tapped for one bubble
				break;
			}
		}
		
		if (!isSelectedBubbles) {
			// then check for area taps
			for (ShapeArea a : mAreaList) {
				if (a.isInArea((float)testx,(float)testy)) {
					if(mShapeListener != null){
						mShapeListener.onShapeClick(this, a.getId());
					}
					isLostFocus=false;
					// only fire clicked for one area
					break;
				}
			}
		}

		if (isLostFocus) {
			cleanBubbles();
			invalidate();
		}
	}
	
	// process a fling by kicking off the scroller
	public void fling(int velocityX, int velocityY) {
		int startX = mScrollLeft;
		int startY = mScrollTop;
		mScroller.fling(startX, startY, -velocityX, -velocityY, mRightBound, 0,mBottomBound, 0);
		invalidate();
	}
	
	/*
	 * move the view to this x, y 
	 */
	public void moveTo(int x, int y) {
		mScrollLeft = x;
		if (mScrollLeft > 0) {
			mScrollLeft = 0;
		}
		if (mScrollLeft < mRightBound) {
			mScrollLeft = mRightBound;
		}
		mScrollTop=y;
		if (mScrollTop > 0) {
			mScrollTop = 0;
		}
		if (mScrollTop < mBottomBound) {
			mScrollTop = mBottomBound;
		}
		invalidate();
	}

	/*
	 * move the view by this delta in X direction
	 */
	public void moveX(int deltaX) {
		mScrollLeft = mScrollLeft + deltaX;
		if (mScrollLeft > 0) {
			mScrollLeft = 0;
		}
		if (mScrollLeft < mRightBound) {
			mScrollLeft = mRightBound;
		}
		invalidate();
	}

	/*
	 * move the view by this delta in Y direction
	 */
	public void moveY(int deltaY) {
		mScrollTop = mScrollTop + deltaY;
		if (mScrollTop > 0) {
			mScrollTop = 0;
		}
		if (mScrollTop < mBottomBound) {
			mScrollTop = mBottomBound;
		}
		invalidate();
	}
	
	/*
	 * A class to track touches
	 */
	class TouchPoint {
		int _id;
		float _x;
		float _y;
		
		TouchPoint(int id) {
			_id=id;
			_x=0f;
			_y=0f;			
		}
		int getTrackingPointer() {
			return _id;
		}
		void setPosition(float x, float y) {
			if ((_x != x) || (_y != y)) {
				_x=x;
				_y=y;
			}
		}
		float getX() {
			return _x;
		}
		float getY() {
			return _y;
		}
	}
	
    /**
     * Rectangle Area
     */
	class RectArea extends ShapeArea {
		float _left;
		float _top;
		float _right;
		float _bottom;
		
		RectArea(int id, float left, float top, float right, float bottom) {
			super(id);
			_left = left;
			_top = top;
			_right = right;
			_bottom = bottom;
		}
		
		public boolean isInArea(float x, float y) {
			boolean ret = false;
			if ((x > _left) && (x < _right)) {
				if ((y > _top) && (y < _bottom)) {
					ret = true;
				}
			}
			return ret;
		}
		
		public float getOriginX() {
			return _left;
		}
		
		public float getOriginY() {
			return _top;
		}

		@Override
		void drawShape(Canvas canvas) {
	        canvas.drawRect(getLeft(), getTop(), getRight(), getBottom(), getHighlightPaint());  
		}
		
		float getLeft(){
			return _left*mResizeFactorX + mScrollLeft;
		}
		
		float getTop(){
			return _top*mResizeFactorY  + mScrollTop;
		}
		
		float getRight(){
			return _right*mResizeFactorX  + mScrollLeft;
		}
		
		float getBottom(){
			return _bottom*mResizeFactorY  + mScrollTop;
		}
		
	}
	
	/**
	 * Polygon area
	 */
	class PolyArea extends ShapeArea {
		ArrayList<Integer> xpoints = new ArrayList<Integer>();
		ArrayList<Integer> ypoints = new ArrayList<Integer>();
		
		// centroid point for this poly
		float _x;
		float _y;
		
		// number of points (don't rely on array size)		
		int _points;
		
		// bounding box
		int top=-1;
		int bottom=-1;
		int left=-1;
		int right=-1;
	
		public PolyArea(int id, float[] coords) {
			super(id);
						
			int i=0;
			while ((i+1)<coords.length) {
				int x = (int)coords[i];
				int y = (int)coords[i+1];
				xpoints.add(x);
				ypoints.add(y);
				top=(top==-1)?y:Math.min(top,y);
				bottom=(bottom==-1)?y:Math.max(bottom,y);
				left=(left==-1)?x:Math.min(left,x);
				right=(right==-1)?x:Math.max(right,x);
				i+=2;
			}
			_points=xpoints.size();
			
			// add point zero to the end to make
			// computing area and centroid easier
			xpoints.add(xpoints.get(0));
			ypoints.add(ypoints.get(0));
			
			computeCentroid();
			
		}
		
		/**
		 * area() and computeCentroid() are adapted from the implementation
		 * of polygon.java  published from a princeton case study 
		 * The study is here: http://introcs.cs.princeton.edu/java/35purple/
		 * The polygon.java source is here: http://introcs.cs.princeton.edu/java/35purple/Polygon.java.html
		 */

	    // return area of polygon
	    public double area() {
	        double sum = 0.0;
	        for (int i = 0; i < _points; i++) {
	            sum = sum + (xpoints.get(i) * ypoints.get(i+1)) - (ypoints.get(i) * xpoints.get(i+1));
	        }
	        sum = 0.5 * sum;
	        return Math.abs(sum);
	    }

	    // compute the centroid of the polygon
	    public void computeCentroid() {
	        double cx = 0.0, cy = 0.0;
	        for (int i = 0; i < _points; i++) {
	            cx = cx + (xpoints.get(i) + xpoints.get(i+1)) * (ypoints.get(i) * xpoints.get(i+1) - xpoints.get(i) * ypoints.get(i+1));
	            cy = cy + (ypoints.get(i) + ypoints.get(i+1)) * (ypoints.get(i) * xpoints.get(i+1) - xpoints.get(i) * ypoints.get(i+1));
	        }
	        cx /= (6 * area());
	        cy /= (6 * area());
	        _x=Math.abs((int)cx);
	        _y=Math.abs((int)cy);
	    }


		@Override
		public float getOriginX() {			
			return _x;
		}

		@Override
		public float getOriginY() {
			return _y;
		}
		
		/**
		 * This is a java port of the 
		 * W. Randolph Franklin algorithm explained here
		 * http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
		 */
		@Override
		public boolean isInArea(float testx, float testy)
		{
		  int i, j;
		  boolean c = false;
		  for (i = 0, j = _points-1; i < _points; j = i++) {
		    if ( ((ypoints.get(i)>testy) != (ypoints.get(j)>testy)) &&
			 (testx < (xpoints.get(j)-xpoints.get(i)) * (testy-ypoints.get(i)) / (ypoints.get(j)-ypoints.get(i)) + xpoints.get(i)) )
		       c = !c;
		  }
		  return c;
		}

		@Override
		void drawShape(Canvas canvas) {
			Path path = new Path();  
	        float sx = getX(0);
	        float sy = getY(0);
	        path.moveTo(sx, sy);
			for (int i = 0; i < _points; i++) {
				float cx = getX(i);
				float cy = getY(i);
	        	path.lineTo(cx , cy);
	        }
			path.close();
	        canvas.drawPath(path, getHighlightPaint());
		}
		
		float getX(int index){
			float x = xpoints.get(index);
			return x*mResizeFactorX + mScrollLeft;
		}
		
		float getY(int index){
			float y = ypoints.get(index); 
			return y*mResizeFactorY +mScrollTop;
		}
		
	}
	
	/**
	 * Circle Area
	 */
	class CircleArea extends ShapeArea {		
		float centerX;
		float centerY;
		float radius;		
						
		CircleArea(int id, float x, float y, float radius) {
			super(id);
			centerX = x;
			centerY = y;
			this.radius = radius;
		}		
		
		public boolean isInArea(float x, float y) {
			boolean ret = false;
			
			float dx = centerX-x;
			float dy = centerY-y;
			
			// if tap is less than radius distance from the center
			float d = (float)Math.sqrt((dx*dx)+(dy*dy));
			if (d<radius) {
				ret = true;
			}

			return ret;
		}
		
		public float getOriginX() {
			return centerX;
		}
		
		public float getOriginY() {
			return centerY;
		}

		@Override
		void drawShape(Canvas canvas) {
			canvas.drawCircle(getX(), getY(), getRadius(), getHighlightPaint());
		}	
		
		float getX(){
			return centerX*mResizeFactorX + mScrollLeft;
		}
		
		float getY(){
			return centerY*mResizeFactorY + mScrollTop;
		}
		
		float getRadius(){
			return radius*mResizeFactorX;
		}
	}
	
}