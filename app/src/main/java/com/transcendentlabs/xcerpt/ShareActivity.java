package com.transcendentlabs.xcerpt;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.content.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;
import twitter4j.StatusUpdate;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import static com.transcendentlabs.xcerpt.Util.setActionBarColour;


public class ShareActivity extends AppCompatActivity {

    private static final String TWITTER_KEY = BuildConfig.TWITTER_KEY;
    private static final String TWITTER_SECRET = BuildConfig.TWITTER_SECRET_KEY;

    // CHAR_LIMIT should be determined by a get request of Twitter's link lengths
    private static final int CHAR_LIMIT = 91;

    private TwitterSession twitterSession;

    private ProgressDialog pDialog;

    private TwitterLoginButton loginButton;
    private TextView userName;
    private Button tweetButton;
    private Button saveButton;
    private TextView characterCount;
    private EditText tweet;
    private LinearLayout tweetLayout;
    private LinearLayout tweetBar;

    private String fileName;
    private String tweetText;
    private Bitmap img;
    private String selectedUrl;
    private Uri shareImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        twitterSession =
                TwitterCore.getInstance().getSessionManager().getActiveSession();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        ActionBar bar = getSupportActionBar();
        Window window = getWindow();
        setActionBarColour(bar, window, this);

        tweetButton = (Button) findViewById(R.id.tweet_button);
        saveButton = (Button) findViewById(R.id.save_button);
        userName = (TextView) findViewById(R.id.user_name);

        tweetLayout = (LinearLayout) findViewById(R.id.tweet_layout);
        tweetBar = (LinearLayout) findViewById(R.id.tweet_bar);
        tweetLayout.setVisibility(View.GONE);
        tweetBar.setVisibility(View.GONE);

        TextView logOut = (TextView) findViewById(R.id.logout);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Twitter.logOut();
                loginButton.setVisibility(View.VISIBLE);
                tweetLayout.setVisibility(View.GONE);
                tweetBar.setVisibility(View.GONE);
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        selectedUrl = getIntent().getStringExtra(CustomizeActivity.URL);

        initLinkPreviewView(selectedUrl);
        initCharacterCountView();

        tweetText = selectedUrl;

        // initialize tweet edittext to listen to character count
        tweet = (EditText) findViewById(R.id.tweet);
        tweet.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                tweetText = tweet.getText() + " " + selectedUrl;
                int charsRemaining =CHAR_LIMIT - tweet.getText().length();
                characterCount.setText(Integer.toString(charsRemaining));
                if(charsRemaining >= 0){
                    tweetButton.setEnabled(true);
                    characterCount.setTextColor(Color.BLACK);
                }else{
                    tweetButton.setEnabled(false);
                    characterCount.setTextColor(Color.RED);
                }
            }
        });

        img = getImagePreview();
        ImageView finalImage = (ImageView) findViewById(R.id.final_image);
        finalImage.setImageBitmap(img);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MMdd_HHmm_ssSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        fileName = strDate + ".png";
        boolean imageSaved = saveFile(fileName, img);

        if(!imageSaved){
            //TODO throw error
            return;
        }

        // final Uri imageUri = Uri.fromFile(imageFile);

        initLoginButton();

        if(twitterSession != null){
            showLoggedInState(twitterSession);
        }

        shareImageUri = null;
    }

    private void initLinkPreviewView(String selectedUrl) {
        final int LINK_PREVIEW_LENGTH = 32;

        String baseUrl = selectedUrl;
        if(baseUrl.startsWith("https://www.")){
            baseUrl = baseUrl.substring("https://www.".length());
        }else if(baseUrl.startsWith("http://www.")){
            baseUrl = baseUrl.substring("http://www.".length());
        }else if(baseUrl.startsWith("https://")){
            baseUrl = baseUrl.substring("https://".length());
        }else if(baseUrl.startsWith("http://")){
            baseUrl = baseUrl.substring("http://".length());
        }
        String urlPreview;
        if(baseUrl.length() < LINK_PREVIEW_LENGTH){
            urlPreview = baseUrl;
        }else {
            urlPreview = baseUrl.substring(0, LINK_PREVIEW_LENGTH) + "...";
        }
        TextView linkPreview = (TextView) findViewById(R.id.link_preview);
        linkPreview.setText(urlPreview);
        linkPreview.setTextColor(getResources().getColor(R.color.tw__blue_default));
    }

    private void initCharacterCountView() {
        characterCount = (TextView) findViewById(R.id.character_count);
        characterCount.setText(Integer.toString(CHAR_LIMIT));
        characterCount.setTextColor(Color.BLACK);
    }

    private void showLoggedInState(TwitterSession twitterSession) {
        final String PREFIX = "Post as @";
        loginButton.setVisibility(View.GONE);
        tweetLayout.setVisibility(View.VISIBLE);
        tweetBar.setVisibility(View.VISIBLE);
        userName.setText(PREFIX + twitterSession.getUserName());
    }

    private void initLoginButton() {
        loginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                twitterSession = result.data;
                showLoggedInState(twitterSession);

            }

            @Override
            public void failure(TwitterException exception) {
                // TODO Do something on failure
            }
        });
    }

    private Bitmap getImagePreview() {
        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray(CustomizeActivity.IMAGE);
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    @Override
    protected void onDestroy() {
        File imageFile = getFileStreamPath(fileName);
        imageFile.delete();
        super.onDestroy();
    }

    private boolean saveFile(String fileName, Bitmap image){
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
        if(id == R.id.home){
            onBackPressed();
            return true;
        } else if (id == R.id.action_share) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/jpeg");

            if(shareImageUri == null){
                if(isExternalStorageWritable()){
                    String root =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                    File dir = new File(root + File.separator + "Xcerpt" + File.separator + "tmp");
                    dir.mkdirs();
                    File file = new File(dir, fileName);
                    Log.i("ShareActivity", "" + file);
                    if (file.exists())
                        file.delete();
                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        img.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        out.flush();
                        out.close();
                        shareImageUri = Uri.fromFile(file);
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Xcerpt URL", selectedUrl);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(ShareActivity.this,
                                "Source URL copied to clipboard.",
                                Toast.LENGTH_LONG
                        ).show();
                        share.putExtra(Intent.EXTRA_STREAM, shareImageUri);
                        startActivity(Intent.createChooser(share, "Share Image"));
                    } catch (Exception e) {
                        Toast.makeText(ShareActivity.this,
                                "Error: File could not be saved.",
                                Toast.LENGTH_LONG
                        ).show();
                        e.printStackTrace();
                    }
                } else{
                    Toast.makeText(ShareActivity.this,
                            "Error: External storage is not writable.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }else{
                share.putExtra(Intent.EXTRA_STREAM, shareImageUri);
                startActivity(Intent.createChooser(share, "Share Image"));
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void saveImage(View view) {
        if(isExternalStorageWritable()){
            String root =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            File dir = new File(root + File.separator + "Xcerpt");
            dir.mkdirs();
            File file = new File(dir, fileName);
            Log.i("ShareActivity", "" + file);
            if (file.exists())
                file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                img.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                saveButton.setEnabled(false);
                saveButton.setText("Image Saved");
            } catch (Exception e) {
                Toast.makeText(ShareActivity.this,
                        "Error: File could not be saved.",
                        Toast.LENGTH_LONG
                ).show();
                e.printStackTrace();
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, fileName);
            values.put(MediaStore.Images.Media.DESCRIPTION, selectedUrl);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
            values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
            values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
            values.put("_data", file.getAbsolutePath());

            ContentResolver cr = getContentResolver();
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        }else{
            Toast.makeText(ShareActivity.this,
                    "Error: External storage is not writable.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    public void postTweet(View view) {
        File imageFile = getFileStreamPath(fileName);
        UpdateTwitterStatusTask postTweet = new UpdateTwitterStatusTask(tweetText, imageFile);
        postTweet.execute();
    }

    class UpdateTwitterStatusTask extends AsyncTask<String, String, Void> {

        String status;
        File image;

        public UpdateTwitterStatusTask(String status, File image) {
            this.status = status;
            this.image = image;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pDialog = new ProgressDialog(ShareActivity.this);
            pDialog.setMessage("Posting to Twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected Void doInBackground(String... args) {

            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_SECRET);

                // Access Token
                String access_token = twitterSession.getAuthToken().token;
                // Access Token Secret
                String access_token_secret = twitterSession.getAuthToken().secret;

                AccessToken accessToken = new AccessToken(access_token, access_token_secret);
                twitter4j.Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                // Update status
                StatusUpdate statusUpdate = new StatusUpdate(status);
                statusUpdate.setMedia(image);

                twitter4j.Status response = twitter.updateStatus(statusUpdate);

                Log.d("Status", response.getText());

            } catch (twitter4j.TwitterException e) {
                Log.d("Failed to post!", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

			/* Dismiss the progress dialog after sharing */
            pDialog.dismiss();
            Toast.makeText(ShareActivity.this, "Posted to Twitter!", Toast.LENGTH_SHORT).show();
            tweet.setEnabled(false);
            tweetButton.setText(getString(R.string.view_on_twitter));
            tweetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent;
                    try {
                        // get the Twitter app if possible
                        getPackageManager().getPackageInfo("com.twitter.android", 0);
                        intent = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("twitter://user?user_id=" + twitterSession.getUserId())
                        );
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    } catch (Exception e) {
                        // no Twitter app, revert to browser
                        intent = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://twitter.com/" + twitterSession.getUserName())
                        );
                    }
                    startActivity(intent);
                }
            });
        }

    }
}
