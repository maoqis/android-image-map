package com.lurencun.imagemap.internal;

import android.graphics.Canvas;
import android.view.View;

/**
 * information bubble class
 */
public class Bubble {
	private final ShapeArea shapeArea;
	private float posX;
	private float posY;
	private float top;
	private float left;
	
	public final Object data;
	public final View bubbleView;
	public int height;
	public int width;

	Bubble(View view, ShapeArea shapeArea, float resizeFactorX, float resizeFactorY) {
		this.shapeArea = shapeArea;
		this.bubbleView = view;
		this.data = shapeArea.data;
		float x = shapeArea.getOriginX();
		float y = shapeArea.getOriginY();
		init(x * resizeFactorX, y * resizeFactorY);
	}

	void init(float x, float y) {
		left = posX = x;
		top = posY = y;
		
		if (bubbleView == null) return;
		width = bubbleView.getWidth();
		height = bubbleView.getHeight();
		if (width == 0 && height == 0)return;
		left = posX - (width / 4);
		final int topOffset = 10;
		top = posY - height - topOffset;
	}

	public boolean isInArea(float x, float y) {
		boolean ret = false;
		if ((x > left) && (x < (left + width))) {
			if ((y > top) && (y < (top + height))) {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * 绘制形状
	 * @param canvas
	 */
	public void drawShape(Canvas canvas, boolean enableHighlightArea) {
		if (shapeArea != null && enableHighlightArea) {
			shapeArea.drawShape(canvas);
		}
	}

	/**
	 * 绘制冒泡样式
	 * 
	 * @param canvas
	 */
	public void drawBubble(Canvas canvas, float scrollLeft, float scrollTop) {
		float originX = left + scrollLeft;
		float originY = top + scrollTop;
		if (bubbleView != null) {
			displayer.showBubbleAtPosition(bubbleView, originX, originY);
		}
	}

	public void onTapped() {
		if (listener != null) {
			listener.onBubbleClick(this, shapeArea.getId());
		}
	}
	
	private BubbleDisplayer displayer;
	private OnBubbleClickListener listener;

	public void setDisplayer(BubbleDisplayer displayer) {
		this.displayer = displayer;
	}

	public void setOnBubbleClickListener(OnBubbleClickListener listener) {
		this.listener = listener;
	}
	
	
	public interface OnBubbleClickListener{
		
		void onBubbleClick(Bubble bubble,int shapeId);
	}
	
	public interface OnShapeClickListener{
		
		void onShapeClick(ImageMap imageMap,int shapeId);
	}
	
}