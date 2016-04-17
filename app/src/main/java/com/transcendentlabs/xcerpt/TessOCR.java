package com.transcendentlabs.xcerpt;

import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by Eric on 2015-08-29.
 */
public class TessOCR {
    private TessBaseAPI mTess;

    public TessOCR(String datapath) {
        mTess = new TessBaseAPI();
        String language = "eng";
        mTess.init(datapath, language);
    }

    public String getOCRResult(Bitmap bitmap) {

        mTess.setImage(bitmap);

        return mTess.getUTF8Text();
    }

    public void onDestroy() {
        if (mTess != null)
            mTess.end();
    }

}