package ericbai.com.sharepiece;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ShareActivity extends Activity {

    private ImageView finalImage;
    private TwitterLoginButton loginButton;

    private Bitmap img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        finalImage = (ImageView) findViewById(R.id.final_image);

        TwitterSession twitterSession =
                TwitterCore.getInstance().getSessionManager().getActiveSession();

        final String selectedUrl = getIntent().getStringExtra(CustomizeActivity.URL);

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray(CustomizeActivity.IMAGE);
        img = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        finalImage.setImageBitmap(img);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
        Date now = new Date();
        String strDate = sdf.format(now);
        String fileName = strDate + ".png";
        File file = saveFile(fileName, img);

        if(file == null){
            //TODO throw error
            return;
        }

        final Uri imageUri = Uri.fromFile(file);

        loginButton = (TwitterLoginButton)
                findViewById(R.id.twitter_login_button);
        loginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                ((ViewManager)loginButton.getParent()).removeView(loginButton);
                composeTweet(selectedUrl, imageUri);
            }

            @Override
            public void failure(TwitterException exception) {
                // TODO Do something on failure
            }
        });

        if(twitterSession != null){
            ((ViewManager)loginButton.getParent()).removeView(loginButton);
            composeTweet(selectedUrl, imageUri);
        }
    }

    private File saveFile(String fileName, Bitmap image){
        if(!isExternalStorageWritable()){
            //TODO toast
            return null;
        }

        String fullPath = getAlbumStorageDir("Xcerpt").getAbsolutePath();

        File file = new File(fullPath, fileName);
        try {
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            MediaStore.Images.Media.insertImage(this.getContentResolver(),
                    file.getAbsolutePath(),
                    file.getName(),
                    file.getName()
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("failure", e.getMessage());
        } catch (IOException e) {
            Log.e("failure", e.getMessage());
        }

        return file;
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e("Test", "Directory not created");
        }
        return file;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void composeTweet(String url, Uri imageUri){
        Log.e("Uri path", imageUri.getPath());
        TweetComposer.Builder builder = new TweetComposer.Builder(this)
                .text(url)
                .image(imageUri);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loginButton.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
