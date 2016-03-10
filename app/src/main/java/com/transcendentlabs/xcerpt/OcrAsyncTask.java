package com.transcendentlabs.xcerpt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

public class OcrAsyncTask extends AsyncTask<Void, Void, Void> {

    private Bitmap mBitmap;
    private Callback mCallback;
    private String mDatapath;

    private String excerpt;

    public OcrAsyncTask(Bitmap bitmap, String datapath, Callback callback) {
        mDatapath = datapath;
        mBitmap = bitmap;
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        TessOCR tesseract = new TessOCR(mDatapath);

        excerpt = tesseract.getOCRResult(mBitmap);
        excerpt = excerpt.replaceAll("\n", " ");
        excerpt = excerpt.replaceAll("  ", "\n");
        excerpt = excerpt.replaceAll("\\p{Pd}", "-");

        return null;
    }

    @Override
    protected void onPostExecute(Void s) {
        super.onPostExecute(s);
        if (mCallback != null && !isCancelled()) {
            mCallback.onComplete(excerpt, null);
        }
    }

    public interface Callback {
        void onComplete(Object o, Error error);
    }
}