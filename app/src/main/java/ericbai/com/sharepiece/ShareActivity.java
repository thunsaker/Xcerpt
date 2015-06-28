package ericbai.com.sharepiece;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;


public class ShareActivity extends Activity {

    private ImageView finalImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        Intent intent = getIntent();
        finalImage = (ImageView) findViewById(R.id.final_image);

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray(CustomizeActivity.IMAGE);
        Bitmap img = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        finalImage.setImageBitmap(img);
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
