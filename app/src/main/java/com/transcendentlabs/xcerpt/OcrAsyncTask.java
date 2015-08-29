package com.transcendentlabs.xcerpt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Created by Eric on 2015-08-29.
 */
public class OcrAsyncTask extends AsyncTask<Void, Void, Void> {

    private Bitmap mBitmap;
    private Callback mCallback;
    private Error mError;
    private Activity mActivity;
    private ProgressDialog dialog;

    private String excerpt;

    public OcrAsyncTask(Activity activity, Bitmap bitmap, Callback callback) {
        mActivity = activity;
        mBitmap = bitmap;
        mCallback = callback;
        dialog = new ProgressDialog(activity);
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage("Processing image...");
        dialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        TessOCR tesseract = new TessOCR();

        excerpt = tesseract.getOCRResult(mBitmap);
        excerpt = excerpt.replaceAll("\n", " ");
        excerpt = excerpt.replaceAll("  ", "\n\n");
        excerpt = excerpt.replaceAll("\\p{Pd}", "-");

        return null;
    }

    @Override
    protected void onPostExecute(Void s) {
        super.onPostExecute(s);
        if (mCallback != null) {
            mCallback.onComplete(excerpt, mError);
        }
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public interface Callback {
        void onComplete(Object o, Error error);
    }
}