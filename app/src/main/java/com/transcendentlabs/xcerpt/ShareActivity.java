package com.transcendentlabs.xcerpt;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
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

import com.twitter.Validator;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import twitter4j.StatusUpdate;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import static com.transcendentlabs.xcerpt.Util.setActionBarColour;


public class ShareActivity extends BaseActivity {

    private TwitterSession twitterSession;

    private ProgressDialog pDialog;

    private TwitterLoginButton loginButton;
    private TextView userName;
    private Button tweetButton;
    private TextView characterCount;
    private EditText tweet;
    private LinearLayout tweetLayout;

    private String fileName;
    private String tweetText;
    private Bitmap img;
    private String selectedUrl;
    private Uri shareImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        twitterSession =
                TwitterCore.getInstance().getSessionManager().getActiveSession();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        ActionBar bar = getSupportActionBar();
        Window window = getWindow();
        setActionBarColour(bar, window, this);

        tweetButton = (Button) findViewById(R.id.tweet_button);
        userName = (TextView) findViewById(R.id.user_name);

        tweetLayout = (LinearLayout) findViewById(R.id.tweet_layout);

        initLoginButton();

        showLoggedOutState();

        initLogoutButton();

        selectedUrl = getIntent().getStringExtra(CustomizeActivity.URL);
        initLinkPreviewView(selectedUrl);

        tweetText = selectedUrl;

        final Validator twitterValidator = new Validator();
        int charLimit = Validator.MAX_TWEET_LENGTH - (2 * (twitterValidator.getShortUrlLengthHttps() + 1));
        initCharacterCountView(charLimit);

        initTweetEditor(twitterValidator);

        initPreviewImage();

        boolean imageSaved = saveImage();

        if(!imageSaved){
            //TODO throw error
            return;
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPref.getBoolean(SettingsActivity.KEY_USE_TWITTER_COMPOSER, false)){
            showJustImage();
            openTwitterTweetComposer();
        }
        else if(twitterSession != null){
            showLoggedInState(twitterSession);
        }

        shareImageUri = null;
    }

    private boolean saveImage() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MMdd_HHmm_ssSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        fileName = strDate + ".png";
        return saveFile(fileName, img);
    }

    private void initPreviewImage() {
        img = getImagePreview();
        ImageView finalImage = (ImageView) findViewById(R.id.final_image);
        finalImage.setImageBitmap(img);
    }

    private void initTweetEditor(final Validator twitterValidator) {
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
                int tweetLength = twitterValidator.getTweetLength(tweet.getText().toString()) + (2 * (twitterValidator.getShortUrlLengthHttps() + 1));
                int charsRemaining = Validator.MAX_TWEET_LENGTH - tweetLength;
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
    }

    private void initLogoutButton() {
        TextView logOut = (TextView) findViewById(R.id.logout);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Twitter.logOut();
                showLoggedOutState(view);
            }
        });
    }

    private void openTwitterTweetComposer() {
        if(shareImageUri == null) {
            try {
                cacheImage(false);
                TweetComposer.Builder builder = new TweetComposer.Builder(this)
                        .text(selectedUrl)
                        .image(shareImageUri);
                builder.show();
            } catch (IOException ex) {
                Toast.makeText(ShareActivity.this,
                        "Error: File could not be saved.",
                        Toast.LENGTH_LONG
                ).show();
                ex.printStackTrace();
            }
        } else {
            TweetComposer.Builder builder = new TweetComposer.Builder(this)
                    .text(selectedUrl)
                    .image(shareImageUri);
            builder.show();
        }
    }

    private void showJustImage() {
        loginButton.setVisibility(View.GONE);
        tweetLayout.setVisibility(View.GONE);
        tweetButton.setVisibility(View.VISIBLE);
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

    private void initCharacterCountView(int characterLimit) {
        characterCount = (TextView) findViewById(R.id.character_count);
        characterCount.setText(Integer.toString(characterLimit));
        characterCount.setTextColor(Color.BLACK);
    }

    private void showLoggedInState(TwitterSession twitterSession) {
        final String PREFIX = "Post as @";
        loginButton.setVisibility(View.GONE);
        tweetLayout.setVisibility(View.VISIBLE);
        tweetButton.setVisibility(View.VISIBLE);
        userName.setText(PREFIX + twitterSession.getUserName());
    }

    private void showLoggedOutState() {
        showLoggedOutState(null);
    }

    private void showLoggedOutState(View view) {
        loginButton.setVisibility(View.VISIBLE);
        tweetLayout.setVisibility(View.GONE);
        tweetButton.setVisibility(View.GONE);
        if(view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
        getMenuInflater().inflate(R.menu.menu_share, menu);
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
        } else if (id == R.id.action_info) {
            AlertDialog infoDialog = DialogFactory.buildInfoDialog(this);
            displayDialog(infoDialog);
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
                Toast.makeText(ShareActivity.this, getString(R.string.image_saved), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(ShareActivity.this,
                        getString(R.string.error_file_not_saved),
                        Toast.LENGTH_LONG
                ).show();
                e.printStackTrace();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, fileName);
            values.put(MediaStore.Images.Media.DESCRIPTION, selectedUrl);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
            values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
            values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
            values.put("_data", file.getAbsolutePath());

            ContentResolver cr = getContentResolver();
            if(cr == null) {
                Toast.makeText(ShareActivity.this,
                        getString(R.string.error_file_not_saved),
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        }else{
            Toast.makeText(ShareActivity.this,
                    "Error: External storage is not writable.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    public void postTweet(View view) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPref.getBoolean(SettingsActivity.KEY_USE_TWITTER_COMPOSER, false)){
            openTwitterTweetComposer();
            return;
        }
        File imageFile = getFileStreamPath(fileName);
        UpdateTwitterStatusTask postTweet = new UpdateTwitterStatusTask(tweetText, imageFile);
        postTweet.execute();
    }

    public void shareImage(View view) {

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");
        share.putExtra(Intent.EXTRA_TEXT, selectedUrl);
        if(shareImageUri == null){
            try{
                cacheImage(true);
                share.putExtra(Intent.EXTRA_STREAM, shareImageUri);
                startActivity(Intent.createChooser(share, "Share Image"));
            } catch (IOException e) {
                Toast.makeText(ShareActivity.this,
                        "Error: File could not be saved.",
                        Toast.LENGTH_LONG
                ).show();
                e.printStackTrace();
            }
        }else{
            share.putExtra(Intent.EXTRA_STREAM, shareImageUri);
            startActivity(Intent.createChooser(share, "Share Image"));
        }
    }

    private void cacheImage(boolean copyUrl) throws IOException {
        if(isExternalStorageWritable()){
            String root =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            File dir = new File(root + File.separator + "Xcerpt" + File.separator + "tmp");
            dir.mkdirs();
            File file = new File(dir, fileName);
            if (file.exists()) {
                file.delete();
            }
            if(copyUrl) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Xcerpt URL", selectedUrl);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ShareActivity.this,
                        "Source URL copied to clipboard.",
                        Toast.LENGTH_SHORT
                ).show();
            }
            FileOutputStream out = new FileOutputStream(file);
            img.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            shareImageUri = Uri.fromFile(file);
        } else{
            Toast.makeText(ShareActivity.this,
                    "Error: Cannot write to external storage.",
                    Toast.LENGTH_LONG
            ).show();
        }
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
            displayDialog(pDialog);
        }

        protected Void doInBackground(String... args) {

            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(App.TWITTER_KEY);
                builder.setOAuthConsumerSecret(App.TWITTER_SECRET);

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
            closeDialog();
            Toast.makeText(ShareActivity.this, getString(R.string.posted_to_twitter), Toast.LENGTH_SHORT).show();
            tweet.setEnabled(false);
            tweetButton.setText(getString(R.string.view_on_twitter));
            tweetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    App.getInstance().openTwitterProfile(twitterSession.getUserName());
                }
            });
        }

    }
}
