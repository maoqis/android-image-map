package com.lurencun.imagemap.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public abstract class ShapeArea {
	private int id;
	private Bitmap decoration = null;

	protected Bubble bubble;
	protected Object data;

	public ShapeArea(int id) {
		this.id = id;
	}

	public void setBubble(Bubble bubble) {
		this.bubble = bubble;
	}

	public int getId() {
		return id;
	}

	// a method for setting a simple decorator for the area
	public void setDecoratorBitmap(Bitmap b) {
		decoration = b;
	}

	// an onDraw is set up to provide an extensible way to
	// decorate an area. When drawing remember to take the
	// scaling and translation into account
	public void doDrawing(Canvas canvas, float resizeFactorX,
			float resizeFactorY, float scrollLeft, float scrollTop) {
		// 绘制区域图标
		if (decoration != null) {
			float x = (getOriginX() * resizeFactorX) + scrollLeft - 17;
			float y = (getOriginY() * resizeFactorY) + scrollTop - 17;
			canvas.drawBitmap(decoration, x, y, null);
		}
	}

	abstract void drawShape(Canvas canvas);

	abstract boolean isInArea(float x, float y);

	abstract float getOriginX();

	abstract float getOriginY();
}