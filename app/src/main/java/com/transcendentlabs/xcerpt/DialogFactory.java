package com.transcendentlabs.xcerpt;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/**
 * Created by Eric on 2016-03-13.
 */
public class DialogFactory {
    public static AlertDialog buildInfoDialog(Activity activity) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle("Want to see how others are using Xcerpt?");
        builder.setMessage("Open @XcerptApp to see tweets made by others");
        builder.setPositiveButton("Sure", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                App.getInstance().openTwitterProfile("XcerptApp");
            }
        });
        builder.setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        return builder.create();
    }
}
