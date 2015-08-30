package com.transcendentlabs.xcerpt;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/** An image view which always retains its aspect ratio. */
final class AspectRatioImageView extends ImageView {
    float mRatio;

    public AspectRatioImageView(Context context, float ratio) {
        super(context);
        mRatio = ratio;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), (int) (mRatio * getMeasuredWidth()));
    }
}