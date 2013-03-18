package com.lurencun.imagemap;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.lurencun.imagemap.internal.Bubble;
import com.lurencun.imagemap.internal.Bubble.OnBubbleClickListener;
import com.lurencun.imagemap.internal.Bubble.OnShapeClickListener;
import com.lurencun.imagemap.internal.ImageMap;
import com.lurencun.imagemap.internal.Shape;

public class ImageMapTestActivity extends Activity implements OnBubbleClickListener,OnShapeClickListener{
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final ImageMapLayout imageMapView = (ImageMapLayout)findViewById(R.id.map);
        
       	imageMapView.setImageResource(R.drawable.gridmap);
	    
		Shape shape1 = new Shape("Shape-1-data", "118,124,219,226");
		Shape shape2 = new Shape("Shape-2-data", "474,374,574,476");
		Shape shape3 = new Shape("Shape-3-data", "710,878,808,980");
		int id1 = imageMapView.addShape(shape1);
		int id2 = imageMapView.addShape(shape2);
		int id3 = imageMapView.addShape(shape3);
		
		//设置Bubble显示方式
		
		TextView view1 = new TextView(this);
		view1.setText("Bubble(1)View");
		view1.setPadding(10, 10, 10, 10);
		view1.setBackgroundColor(Color.BLACK);
		imageMapView.addBubble(view1, id1);	 
		
		TextView view2 = new TextView(this);
		view2.setText("Bubble(2)View");
		view2.setPadding(10, 10, 10, 10);
		view2.setBackgroundColor(Color.BLACK);
		imageMapView.addBubble(view2, id2);	  
		
		TextView view3 = new TextView(this);
		view3.setText("Bubble(3)View");
		view3.setPadding(10, 10, 10, 10);
		view3.setBackgroundColor(Color.BLACK);
		imageMapView.addBubble(view3, id3);
		
		imageMapView.setOnBubbleClickeListener(this);
		imageMapView.setOnShapeClickeListener(this);
       
    }

	@Override
	public void onShapeClick(ImageMap imageMap, int shapeId) {
		imageMap.showBubble(true, shapeId);
	}

	@Override
	public void onBubbleClick(Bubble bubble, int shapeId) {
		Toast.makeText(this, "Click Bubble view:shapeid="+shapeId+" data="+bubble.data, Toast.LENGTH_SHORT).show();
	}
}