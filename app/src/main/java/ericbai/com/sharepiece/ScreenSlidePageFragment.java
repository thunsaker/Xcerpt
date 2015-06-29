package ericbai.com.sharepiece;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
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

import java.util.List;

public class ScreenSlidePageFragment extends Fragment {
    public static final String ARG_PAGE = "page";

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
      View view = View.inflate(getActivity(), R.layout.pick_colour_view, null);
      View purple = (View) view.findViewById(R.id.purple);
      View blue = (View) view.findViewById(R.id.blue);
      View orange = (View) view.findViewById(R.id.orange);
      View green = (View) view.findViewById(R.id.green);

      purple.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              CustomizeActivity activity = (CustomizeActivity) getActivity();
              activity.getBackgroundView().setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
          }
      });

      blue.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          CustomizeActivity activity = (CustomizeActivity) getActivity();
          activity.getBackgroundView().setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
        }
      });

      orange.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          CustomizeActivity activity = (CustomizeActivity) getActivity();
          activity.getBackgroundView().setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
        }
      });

      green.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          CustomizeActivity activity = (CustomizeActivity) getActivity();
          activity.getBackgroundView().setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        }
      });
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