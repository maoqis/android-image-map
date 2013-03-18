package com.lurencun.imagemap.internal;

public class Shape {
	
	public enum Type{
		/**
		 * 矩形
		 */
		RECT,
		/**
		 * 圆形
		 */
		CIRCLE,
		/**
		 * 多边形
		 */
		POLY;
	}
	
	final Object data;
	final float[] coords;
	final Type type;

	public Shape(Object data, String coords) {
		this.data = data;
		String[] sCoords = coords.split(",");
		
		if(sCoords.length == 3){
			this.type = Type.CIRCLE;
		}else if(sCoords.length == 4){
			this.type = Type.RECT;
		}else{
			this.type = Type.POLY;
		}
		
		this.coords = new float[sCoords.length];
		for(int i=0;i<sCoords.length;i++){
			this.coords[i] = Float.parseFloat(sCoords[i]);
		}
		
	}
	

}