package ericbai.com.sharepiece;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import at.markushi.ui.CircleButton;

public class ScreenSlidePageFragment extends Fragment {
    public static final String ARG_PAGE = "page";

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
            Color.parseColor("#000000") // black


    ));

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;
    private static RadioGroup sourceSelect;

    public static final int HIGHLIGHT = 0;
    public static final int COLOUR = 1;
    public static final int SOURCE = 2;

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
        mPageNumber = getArguments().getInt(ARG_PAGE);
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
            case HIGHLIGHT: child = getHighlightCard();
                break;
            case COLOUR: child = getColourCard();
                break;
            case SOURCE: child = getSourceCard();
                break;
        }

        fl.addView(child);

        return fl;
    }

    public View getHighlightCard(){
        return setInstructions(getString(R.string.instructions_highlight));
    }

    public View getColourCard(){
        final SharedPreferences settings = getActivity().getPreferences(0);

        LinearLayout view = new LinearLayout(getActivity());
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER_HORIZONTAL);

        view.addView(setInstructions(getString(R.string.instructions_colour)));

        LinearLayout colourSelectrow1 = new LinearLayout(getActivity());
        colourSelectrow1.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout colourSelectrow2 = new LinearLayout(getActivity());
        colourSelectrow2.setGravity(Gravity.CENTER_HORIZONTAL);
        final SharedPreferences.Editor editor = settings.edit();

        int buttonsAdded = 0;
        for(final int colour : colourChoices){
            CircleButton b = new CircleButton(getActivity());
            b.setMinimumWidth(150);
            b.setMinimumHeight(150);
            b.setColor(colour);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("colour select", "" + colour);
                    CustomizeActivity activity = (CustomizeActivity) getActivity();
                    activity.getBackgroundView().setBackgroundColor(colour);
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

      return view;
    }

    public View getSourceCard(){
        LinearLayout layout = new LinearLayout(getActivity());
        CustomizeActivity.Article articles[] = ((CustomizeActivity)getActivity()).articles;

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView text = setInstructions(getString(R.string.instructions_source));

        layout.addView(text);

        if(articles[0] == null){
            TextView loadingText = new TextView(getActivity());
            loadingText.setText(getString(R.string.loading));
            layout.addView(loadingText);
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

        for(int i = 0; i < articles.length; i++){
            if(articles[i] == null) continue;
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
                ((CustomizeActivity)getActivity()).updateSource(checkedId);
            }
        });;

        layout.addView(sourceSelect);

        return layout;
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
}