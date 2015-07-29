package ericbai.com.sharepiece;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.fabric.sdk.android.Fabric;
import twitter4j.StatusUpdate;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;


public class ShareActivity extends AppCompatActivity {

    private static final String TWITTER_KEY = BuildConfig.TWITTER_KEY;
    private static final String TWITTER_SECRET = BuildConfig.TWITTER_SECRET_KEY;
    private static final int CHAR_LIMIT = 94;
    private final int LINK_PREVIEW_LENGTH = 32;

    private TwitterSession twitterSession;

    private ProgressDialog pDialog;

    private ImageView finalImage;
    private TwitterLoginButton loginButton;
    private TextView userName;
    private Button tweetButton;
    private TextView characterCount;
    private EditText tweet;
    private LinearLayout tweetLayout;
    private TextView linkPreview;
    private LinearLayout tweetBar;
    private TextView logOut;

    private Bitmap img;
    private String fileName;
    private String tweetText;
    // private File imageFile;
    private boolean imageSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String PREFIX = "Post as @";

        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        ActionBar bar = getSupportActionBar();
        if(bar != null){
            bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.material_blue_grey_800)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(getResources().getColor(R.color.material_blue_grey_900));
            }
        }

        finalImage = (ImageView) findViewById(R.id.final_image);
        tweetButton = (Button) findViewById(R.id.tweet_button);
        characterCount = (TextView) findViewById(R.id.character_count);
        tweet = (EditText) findViewById(R.id.tweet);
        tweetLayout = (LinearLayout) findViewById(R.id.tweet_layout);
        userName = (TextView) findViewById(R.id.user_name);
        linkPreview = (TextView) findViewById(R.id.link_preview);
        tweetBar = (LinearLayout) findViewById(R.id.tweet_bar);
        logOut = (TextView) findViewById(R.id.logout);

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Twitter.logOut();
                loginButton.setVisibility(View.VISIBLE);
                tweetLayout.setVisibility(View.GONE);
                tweetBar.setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        final String selectedUrl = getIntent().getStringExtra(CustomizeActivity.URL);
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
        linkPreview.setText(urlPreview);
        linkPreview.setTextColor(getResources().getColor(R.color.tw__blue_default));

        tweetLayout.setVisibility(View.GONE);
        tweetBar.setVisibility(View.GONE);
        characterCount.setText(Integer.toString(CHAR_LIMIT));
        characterCount.setTextColor(Color.BLACK);

        tweetText = selectedUrl;
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

        twitterSession =
                TwitterCore.getInstance().getSessionManager().getActiveSession();

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray(CustomizeActivity.IMAGE);
        img = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        finalImage.setImageBitmap(img);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
        Date now = new Date();
        String strDate = sdf.format(now);
        fileName = strDate + ".png";
        imageSaved = saveFile(fileName, img);

        if(!imageSaved){
            //TODO throw error
            return;
        }

        // final Uri imageUri = Uri.fromFile(imageFile);

        loginButton = (TwitterLoginButton)
                findViewById(R.id.twitter_login_button);
        loginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                twitterSession = result.data;
                loginButton.setVisibility(View.GONE);
                tweetBar.setVisibility(View.VISIBLE);
                tweetLayout.setVisibility(View.VISIBLE);
                userName.setText(PREFIX + twitterSession.getUserName());

            }

            @Override
            public void failure(TwitterException exception) {
                // TODO Do something on failure
            }
        });

        if(twitterSession != null){
            loginButton.setVisibility(View.GONE);
            tweetLayout.setVisibility(View.VISIBLE);
            tweetBar.setVisibility(View.VISIBLE);
            userName.setText(PREFIX + twitterSession.getUserName());
        }
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
        }
        return super.onOptionsItemSelected(item);
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
            pDialog.setMessage("Posting to twitter...");
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
            tweetButton.setAlpha(.7f);
            tweetButton.setEnabled(false);
            tweetButton.setText("Posted");
            tweet.setEnabled(false);
        }

    }
}
