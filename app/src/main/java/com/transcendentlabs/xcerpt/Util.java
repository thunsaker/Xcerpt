package com.transcendentlabs.xcerpt;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

public class Util {

    // Colour Util

    public static final String DEFAULT_COLOUR = "#009688"; // teal
    private static final String TAG = "Util";

    public static void setActionBarColour(ActionBar bar, Window window, Activity activity){
        setActionBarColour(bar, window, activity, DEFAULT_COLOUR);
    }

    public static void setActionBarColour(ActionBar bar, Window window, Activity activity, String colour){
        if(bar != null) {
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(colour)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                Bitmap icon = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher_white);

                ActivityManager.TaskDescription taskDescription =
                        new ActivityManager.TaskDescription(
                                activity.getString(R.string.app_name),
                                icon,
                                Color.parseColor(colour));
                activity.setTaskDescription(taskDescription);

                float[] hsv = new float[3];
                int darkerColour = Color.parseColor(colour);
                Color.colorToHSV(darkerColour, hsv);
                hsv[2] *= 0.8f; // value component
                darkerColour = Color.HSVToColor(hsv);
                window.setStatusBarColor(darkerColour);
            }
        }
    }

    // Network Util

    public static final String EXCERPT = "com.transcendentlabs.xcerpt.excerpt";

    public static boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo() != null;
    }

    public static void initOcrIfNecessary(Activity activity){

        boolean doNewInit = false;
        File storageDirectory = getStorageDirectory(activity);
        if(storageDirectory != null){
            File data = new File(storageDirectory.toString()
                    + File.separator + "tessdata"
                    + File.separator + "eng.traineddata");
            doNewInit = !data.exists() || data.isDirectory();
        }
        if (doNewInit) {
            new OcrInitAsyncTask(activity).execute(storageDirectory.toString());
        }
    }


    public static File getStorageDirectory(Activity activity) {
        //Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));

        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            Log.e(TAG, "Is the SD card visible?", e);
            showErrorMessage(activity,
                    "Error",
                    "Required external storage (such as an SD card) is unavailable.");
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            // We can read and write the media
            //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
            // For Android 2.2 and above

            try {
                return activity.getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                // We get an error here if the SD card is visible, but full
                Log.e(TAG, "External storage is unavailable");
                showErrorMessage(activity,
                        "Error",
                        "Required external storage (such as an SD card) is full or unavailable.");
            }

            //        } else {
            //          // For Android 2.1 and below, explicitly give the path as, for example,
            //          // "/mnt/sdcard/Android/data/edu.sfsu.cs.orange.ocr/files/"
            //          return new File(Environment.getExternalStorageDirectory().toString() + File.separator +
            //                  "Android" + File.separator + "data" + File.separator + getPackageName() +
            //                  File.separator + "files" + File.separator);
            //        }

        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            Log.e(TAG, "External storage is read-only");
            showErrorMessage(activity,
                    "Error",
                    "Required external storage (such as an SD card) is unavailable for data storage.");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            Log.e(TAG, "External storage is unavailable");
            showErrorMessage(activity,
                    "Error",
                    "Required external storage (such as an SD card) is unavailable or corrupted.");
        }
        return null;
    }

    public static void showErrorMessage(Activity activity, String title, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setOnCancelListener(new FinishListener(activity))
                .setPositiveButton( "Done", new FinishListener(activity))
                .show();
    }

    public static String getTextFromClipboard(Activity activity, ClipboardManager clipboard){
        if(clipboard.getPrimaryClip() == null){
            Toast.makeText(activity,
                    activity.getString(R.string.empty_clipboard_toast),
                    Toast.LENGTH_SHORT)
                    .show();
            return null;
        }
        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        if(item == null){
            Toast.makeText(activity,
                    activity.getString(R.string.empty_clipboard_toast),
                    Toast.LENGTH_SHORT)
                    .show();
            return null;
        }
        CharSequence text = item.getText();
        if(text == null){
            Toast.makeText(activity,
                    activity.getString(R.string.clipboard_no_text),
                    Toast.LENGTH_SHORT)
                    .show();
            return null;
        }

        return text.toString();
    }

    private static final class FinishListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, Runnable {

        private final Activity activityToFinish;

        FinishListener(Activity activityToFinish) {
            this.activityToFinish = activityToFinish;
        }

        public void onCancel(DialogInterface dialogInterface) {
            run();
        }

        public void onClick(DialogInterface dialogInterface, int i) {
            run();
        }

        public void run() {
            activityToFinish.finish();
        }

    }
}
