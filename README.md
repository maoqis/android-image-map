An implementation of an HTML map like element in an Android View:
一个实现类似地图气泡提示，类似HTML <map>标签元素的Android组件。


#(演示)Sample

**See ImageMapTestActivity**
**详见ImageMapTestActivity**

```java
public class ImageMapTestActivity extends Activity implements OnBubbleClickListener,OnShapeClickListener{
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final ImageMapLayout imageMapView = (ImageMapLayout)findViewById(R.id.map);
        
       	imageMapView.setImageResource(R.drawable.gridmap);
	    
		Shape<String> shape1 = new Shape<String>("1111", "118,124,219,226");
		Shape<String> shape2 = new Shape<String>("2222", "474,374,574,476");
		Shape<String> shape3 = new Shape<String>("3333", "710,878,808,980");
		int id1 = imageMapView.addShape(shape1);
		int id2 = imageMapView.addShape(shape2);
		int id3 = imageMapView.addShape(shape3);
		
		//设置Bubble显示方式
		
		
		TextView view1 = new TextView(this);
		view1.setText("这是第(1)个BubbleView");
		view1.setPadding(10, 10, 10, 10);
		view1.setBackgroundColor(Color.BLACK);
		imageMapView.addBubble(view1, id1);	 
		
		TextView view2 = new TextView(this);
		view2.setText("这是第(2)个BubbleView");
		view2.setPadding(10, 10, 10, 10);
		view2.setBackgroundColor(Color.BLACK);
		imageMapView.addBubble(view2, id2);	  
		
		TextView view3 = new TextView(this);
		view3.setText("这是第(3)个BubbleView");
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
	public void onBubbleClick(ImageMap imageMap, int shapeId) {
		Toast.makeText(this, "点击（"+shapeId+")Bubble", Toast.LENGTH_SHORT).show();
	}
}
```

#LICENSE
/*
 * Copyright (C) 2011 Scott Lund
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 /*
 * Copyright (C) 2013 ChenYoca@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

