package com.transcendentlabs.xcerpt;

import android.app.Dialog;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import static com.transcendentlabs.xcerpt.Util.setActionBarColour;

/**
 * Created by Eric on 2016-03-13.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private Dialog mDialog;
    private boolean mIsDestroyed = false;

    protected void displayDialog(Dialog dialog) {
        closeDialog();

        if (mIsDestroyed) {
            return;
        }

        mDialog = dialog;
        mDialog.show();
    }

    protected void closeDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    protected void setActionBarElevationOff() {
        ActionBar bar = getSupportActionBar();
        Window window = getWindow();
        setActionBarColour(bar, window, this);
        if(bar != null) {
            bar.setElevation(0);
        }
    }

    @Override
    protected void onDestroy() {
        mIsDestroyed = true;
        closeDialog();
        super.onDestroy();
    }
}
