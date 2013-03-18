ackage com.lurencun.imagemap;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.lurencun.imagemap.internal.BubbleDisplayer;
import com.lurencun.imagemap.internal.ImageMap;
import com.lurencun.imagemap.internal.Shape;
import com.lurencun.imagemap.internal.Bubble.OnBubbleClickListener;
import com.lurencun.imagemap.internal.Bubble.OnShapeClickListener;

public class ImageMapLayout extends FrameLayout implements BubbleDisplayer{
	
	private ImageMap imageMapView;

	public ImageMapLayout(Context context) {
		super(context);
		imageMapView = new ImageMap(getContext());
		initImageMapView();
	}

	public ImageMapLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		imageMapView = new ImageMap(getContext(),attrs);
		initImageMapView();
	}

	public ImageMapLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		imageMapView = new ImageMap(getContext(),attrs,defStyle);
		initImageMapView();
	}
	
	private void initImageMapView(){
		addView(imageMapView);
		imageMapView.setBubbleDisplayer(this);
	}
	
	/**
	 * 为指定ID的形状区域添加气泡View
	 * @param bubble
	 * @param shapeid
	 */
	public void addBubble(View bubble,int shapeid){
		imageMapView.addBubble(bubble, shapeid);
	}
	
	/**
	 * 添加一个形状区域 
	 * @param shape
	 * @return 返回区域ID
	 */
	public int addShape(Shape shape){
		return imageMapView.addShape(shape);
	}
	
	/**
	 * 添加指定资源ID的图片到图层中
	 * @param resid
	 */
	public void setImageResource(int resid){
		imageMapView.setImageResource(resid);
	}
	
	/**
	 * 添加图片到图层中
	 * @param bitmap
	 */
	public void setBitmapResource(Bitmap bitmap){
		imageMapView.setImageBitmap(bitmap);
	}
	
	public void setOnBubbleClickeListener( OnBubbleClickListener listener ) {
		imageMapView.setOnBubbleClickeListener(listener);
	}
	
	public void setOnShapeClickeListener( OnShapeClickListener listener ) {
		imageMapView.setOnShapeClickeListener(listener);
	}

	@Override
	final public void showBubbleAtPosition(View view, float x, float y) {
		LayoutParams params = null;
		final boolean isChildView = isChildView(view);
		if(!isChildView){
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}else{
			params = (LayoutParams) view.getLayoutParams();
			if(params.width == LayoutParams.WRAP_CONTENT){
				params.width = view.getWidth();
				params.height = view.getHeight();
			}
		}
		configParams(params, x, y);
		configParamsAtAPI_11(view, x, y);
		view.setLayoutParams(params);
		if(!isChildView){
			addView(view);
		}
		view.setVisibility(View.VISIBLE);
		view.bringToFront();
	}
	
	private void configParams(LayoutParams params,float x,float y){
		params.leftMargin = (int)x;
		params.topMargin = (int)y;
	}
	
	@TargetApi(11)
	private void configParamsAtAPI_11(View view,float x, float y){
		view.setX(x);
		view.setY(y);
	}
	
	public boolean isChildView(View view){
		final int viewCount = getChildCount();
		boolean isChildView = false;
		for(int i=0;i<viewCount;i++){
			View childView = getChildAt(i);
			if(childView == view){
				isChildView = true;
				break;
			}
		}
		return isChildView;
	}

	@Override
	public void hideBubbleView(View view) {
		view.setVisibility(View.GONE);
	}
	
}
