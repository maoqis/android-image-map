package com.lurencun.imagemap.internal;

import android.view.View;

public interface BubbleDisplayer {

	void showBubbleAtPosition(View view,float x, float y);
	
	void hideBubbleView(View view);
}
