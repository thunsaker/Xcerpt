package com.transcendentlabs.xcerpt.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.transcendentlabs.xcerpt.TessOCR;

/**
 * Performs OCR on a bitmap
 */
public class OcrAsyncTask extends AsyncTask<Void, Void, Void> {

    private Bitmap mBitmap;
    private Callback mCallback;
    private String mDatapath;
    private TessOCR tesseract;

    private String excerpt;

    public OcrAsyncTask(Bitmap bitmap, String datapath, Callback callback) {
        mDatapath = datapath;
        mBitmap = bitmap;
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        tesseract = new TessOCR(mDatapath);

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
        if(tesseract != null) {
            tesseract.onDestroy();
        }
    }

    public interface Callback {
        void onComplete(Object o, Error error);
    }
}