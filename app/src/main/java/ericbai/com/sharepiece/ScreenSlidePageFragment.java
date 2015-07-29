package ericbai.com.sharepiece;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import at.markushi.ui.CircleButton;

public class ScreenSlidePageFragment extends Fragment {
    public static final String ARG_PAGE = "page";
    private final int MARGIN = 5;
    private final String BUTTON_SIZE = "ButtonSize";
    private final int SIZE_IN_DP = 50;

    List<Integer> colourChoices = Collections.unmodifiableList(Arrays.asList(
            Color.parseColor("#9C27B0"), // purple
            Color.parseColor("#F44336"), // red
            Color.parseColor("#E91E63"), // pink
            Color.parseColor("#3F51B5"), // indigo
            Color.parseColor("#2196F3"), // blue
            Color.parseColor("#009688"), // teal
            Color.parseColor("#43A047"), // green
            Color.parseColor("#EF6C00"), // orange
            Color.parseColor("#607D8B"), // blue grey
            Color.parseColor("#FFB300") // amber


    ));

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;
    private static RadioGroup sourceSelect;
    private ClipboardManager clipboard;
    private int dpAsPixels;

    public static final int COLOUR = 0;
    public static final int SOURCE = 1;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */
    public static ScreenSlidePageFragment create(int pageNumber) {
        ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ScreenSlidePageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        mPageNumber = getArguments().getInt(ARG_PAGE);

        float scale = getResources().getDisplayMetrics().density;
        dpAsPixels = (int) (MARGIN*scale + 0.5f);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        FrameLayout fl = new FrameLayout(getActivity());

        final int margin =
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                .getDisplayMetrics());
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.setMargins(margin, margin, margin, margin);

        fl.setLayoutParams(params);

        View child = null;
        switch(mPageNumber){
            case COLOUR: child = getColourCard();
                break;
            case SOURCE: child = getSourceCard();
                break;
        }

        fl.addView(child);

        return fl;
    }
    public View getColourCard(){
        final SharedPreferences settings = getActivity().getPreferences(0);

        ScrollView sv = new ScrollView(getActivity());
        LinearLayout view = new LinearLayout(getActivity());
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        view.setLayoutParams(params);
        view.setPadding(0, dpAsPixels, 0, 0);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER_HORIZONTAL);

        // view.addView(setInstructions(getString(R.string.instructions_colour)));

        LinearLayout colourSelectrow1 = new LinearLayout(getActivity());
        colourSelectrow1.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout colourSelectrow2 = new LinearLayout(getActivity());
        colourSelectrow2.setGravity(Gravity.CENTER_HORIZONTAL);
        final SharedPreferences.Editor editor = settings.edit();

        int buttonSize = settings.getInt(BUTTON_SIZE, -1);
        if(buttonSize == -1){
            buttonSize = Math.round(dpToPx(SIZE_IN_DP));
            editor.putInt(BUTTON_SIZE, buttonSize);
            editor.commit();
        }
        Log.e("button size", "" + buttonSize);

        int buttonsAdded = 0;
        for(final int colour : colourChoices){
            CircleButton b = new CircleButton(getActivity());

            b.setMinimumWidth(buttonSize);
            b.setMinimumHeight(buttonSize);
            b.setColor(colour);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("colour select", "" + colour);
                    CustomizeActivity activity = (CustomizeActivity) getActivity();
                    activity.setColour(colour);
                    editor.putInt(CustomizeActivity.COLOUR_SETTING, colour);
                    editor.commit();
                }
            });
            buttonsAdded++;
            if(buttonsAdded > 5){
                colourSelectrow2.addView(b);
            }else{
                colourSelectrow1.addView(b);
            }
        }
        view.addView(colourSelectrow1);
        view.addView(colourSelectrow2);
        sv.addView(view);
      return sv;
    }

    public View getSourceCard(){
        ScrollView sv = new ScrollView(getActivity());
        LinearLayout layout = new LinearLayout(getActivity());
        Article articles[] = ((CustomizeActivity)getActivity()).articles;

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        layout.setPadding(0, 0, dpAsPixels, 0);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);

        Button customSourceButton = new Button(getActivity());

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        customSourceButton.setLayoutParams(buttonParams);
        customSourceButton.setTextSize(12);

        customSourceButton.setText("Use URL from clipboard");
        customSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get text from clipboard
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                final String pasteData = item.getText().toString();
                if(pasteData != null){
                    // check if text is URL
                    try { URL url = new URL(pasteData);
                        ParseHtmlAsyncTask titleTask =
                                new ParseHtmlAsyncTask(pasteData, new ParseHtmlAsyncTask.Callback(){
                                    @Override
                                    public void onComplete(Object o, Error error) {
                                        if(error != null){
                                            Log.e("ParseHtmlAsyncTask", error.getMessage());
                                            return;
                                        }
                                        String title = (String) o;

                                        String baseUrl = pasteData;
                                        if(baseUrl.startsWith("https://www.")){
                                            baseUrl = baseUrl.substring("https://www.".length());
                                        }else if(baseUrl.startsWith("http://www.")){
                                            baseUrl = baseUrl.substring("http://www.".length());
                                        }else if(baseUrl.startsWith("https://")){
                                            baseUrl = baseUrl.substring("https://".length());
                                        }else if(baseUrl.startsWith("http://")){
                                            baseUrl = baseUrl.substring("http://".length());
                                        }
                                        int backslashAt = baseUrl.indexOf('/');
                                        if(backslashAt > 0){
                                            baseUrl = baseUrl.substring(0, backslashAt);
                                        }

                                        Article customArticle = new Article(title, baseUrl, pasteData);
                                        if(sourceSelect != null) sourceSelect.clearCheck();
                                        ((CustomizeActivity)getActivity()).updateSource(customArticle);
                                        ((CustomizeActivity)getActivity()).nextItem.setEnabled(true);
                                        if(((CustomizeActivity)getActivity()).actionModeOpen){
                                            ((CustomizeActivity)getActivity()).actionModeNextItem.setEnabled(true);
                                        }
                                    }
                                });
                        titleTask.execute();
                    }
                    catch (MalformedURLException e) {
                        // show toast
                        Toast.makeText(getActivity(), "The pasted text is not a valid URL.",
                                Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(getActivity(), "The clipboard is empty.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView loadingText = new TextView(getActivity());
        loadingText.setText(getString(R.string.loading));
        if(articles[0] == null){
            if(((CustomizeActivity)getActivity()).no_results){
                // do something?
            }else{
                layout.addView(loadingText);
            }
            layout.addView(customSourceButton);
            return layout;
        }

        if(sourceSelect != null){
            sourceSelect.removeAllViews();
        }

        sourceSelect = new RadioGroup(getActivity());
        RadioGroup.LayoutParams rgParams =
                new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
        final int margin =
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                        .getDisplayMetrics());
        rgParams.gravity = Gravity.CENTER_HORIZONTAL;
        rgParams.setMargins(margin, margin, margin, margin);
        sourceSelect.setLayoutParams(rgParams);

        boolean showLoadingText = false;
        for(int i = 0; i < articles.length; i++){
            if(articles[i] == null){
                showLoadingText = true;
                continue;
            }
            //TODO
            RadioButton rb = new RadioButton(getActivity());
            String html = "<b>" + articles[i].title + "</b> - "
                    + articles[i].displayUrl;
            rb.setId(i);
            rb.setText(Html.fromHtml(html),TextView.BufferType.SPANNABLE);
            sourceSelect.addView(rb);
            if(i == ((CustomizeActivity)getActivity()).selected_index){
                sourceSelect.check(rb.getId());
            }
        }

        sourceSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId >= 0){
                    ((CustomizeActivity)getActivity()).updateSource(checkedId);
                }
            }
        });

        layout.addView(sourceSelect);
        if(showLoadingText){
            layout.addView(loadingText);
        }
        layout.addView(customSourceButton);
        sv.addView(layout);
        return sv;
    }

    public TextView setInstructions(String instructions){
        TextView text = new TextView(getActivity());
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        text.setText(instructions);
        return text;
    }


    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return mPageNumber;
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
}