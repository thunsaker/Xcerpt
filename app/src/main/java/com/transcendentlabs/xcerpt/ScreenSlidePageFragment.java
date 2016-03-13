package com.transcendentlabs.xcerpt;

import android.content.ClipboardManager;
import android.content.Context;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import uz.shift.colorpicker.LineColorPicker;
import uz.shift.colorpicker.OnColorChangedListener;

import static com.transcendentlabs.xcerpt.Util.getTextFromClipboard;

public class ScreenSlidePageFragment extends Fragment {
    public static final String ARG_PAGE = "page";
    private final int MARGIN = 5;
    private static ProgressBar spinner;


    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;
    private ClipboardManager clipboard;
    private int dpAsPixels;

    public static final int COLOUR = 0;
    public static final int SOURCE = 1;

    private final String MAIN_COLOUR = "main_colour";
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
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
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
        final SharedPreferences.Editor editor = settings.edit();

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View colourLayout = inflater.inflate(R.layout.fragment_colour_picker, null);

        LineColorPicker colorPicker = (LineColorPicker) colourLayout.findViewById(R.id.picker);
        final LineColorPicker colorPicker2 = (LineColorPicker) colourLayout.findViewById(R.id.picker2);

        colorPicker.setColors(new int[]{
                getResources().getColor(R.color.md_red_500),
                getResources().getColor(R.color.md_pink_500),
                getResources().getColor(R.color.md_purple_500),
                getResources().getColor(R.color.md_deep_purple_500),
                getResources().getColor(R.color.md_indigo_500),
                getResources().getColor(R.color.md_blue_500),
                getResources().getColor(R.color.md_light_blue_500),
                getResources().getColor(R.color.md_cyan_500),
                getResources().getColor(R.color.md_teal_500),
                getResources().getColor(R.color.md_green_500),
                getResources().getColor(R.color.md_light_green_500),
                getResources().getColor(R.color.md_amber_500),
                getResources().getColor(R.color.md_orange_500),
                getResources().getColor(R.color.md_deep_orange_500),
                getResources().getColor(R.color.md_brown_500),
                getResources().getColor(R.color.md_grey_500),
                getResources().getColor(R.color.md_blue_grey_500),

        });

        int colour = settings.getInt(MAIN_COLOUR, getResources().getColor(R.color.md_teal_500));
        colorPicker.setSelectedColor(colour);
        colorPicker2.setColors(getColors(colour));
        colorPicker2.setSelectedColor(settings.getInt(CustomizeActivity.COLOUR_SETTING, getResources().getColor(R.color.md_teal_500)));

        colorPicker.setOnColorChangedListener(new OnColorChangedListener() {
            @Override
            public void onColorChanged(int c) {
                editor.putInt(MAIN_COLOUR, c);
                editor.commit();
                colorPicker2.setColors(getColors(c));
                colorPicker2.setSelectedColor(c);


            }
        });

        colorPicker2.setOnColorChangedListener(new OnColorChangedListener() {
            @Override
            public void onColorChanged(int i) {
                CustomizeActivity activity = (CustomizeActivity) getActivity();
                activity.setColour(i);
                editor.putInt(CustomizeActivity.COLOUR_SETTING, i);
                editor.commit();
            }
        });

        return colourLayout;
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

        spinner = new ProgressBar(
                getActivity(),
                null,
                android.R.attr.progressBarStyleSmall
        );

        RadioGroup sourceSelect = getSourceSelectRadioGroup(articles);

        layout.addView(sourceSelect);

        boolean loading = sourceSelect.getChildCount() == 0
                || sourceSelect.getChildCount() < ((CustomizeActivity)getActivity()).numResults;
        if(loading && !((CustomizeActivity)getActivity()).no_results_or_error){
            layout.addView(spinner);
        }

        Button customSourceButton = getCustomSourceButton(sourceSelect);
        layout.addView(customSourceButton);

        sv.addView(layout);
        return sv;
    }

    private RadioGroup getSourceSelectRadioGroup(Article[] articles) {
        RadioGroup sourceSelect = new RadioGroup(getActivity());
        RadioGroup.LayoutParams rgParams = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
        );

        final int margin =
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                        .getDisplayMetrics());
        rgParams.gravity = Gravity.CENTER_HORIZONTAL;
        rgParams.setMargins(margin, margin, margin, margin);
        sourceSelect.setLayoutParams(rgParams);

        for(int i = 0; i < ((CustomizeActivity)getActivity()).numResults; i++){
            if(articles[i] == null){
                continue;
            }

            RadioButton rb = getRadioButton(articles[i], i);
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
        return sourceSelect;
    }

    private RadioButton getRadioButton(Article article, int index) {
        RadioButton rb = new RadioButton(getActivity());

        String html;
        if(article.title == null || article.title.isEmpty()) {
            html = article.displayUrl;
        }else{
            html = "<b>" + article.title + "</b> - "
                    + article.displayUrl;
        }
        rb.setId(index);
        rb.setText(Html.fromHtml(html), TextView.BufferType.SPANNABLE);
        return rb;
    }

    public void initCustomSourceButton(Button customSourceButton, final RadioGroup sourceSelect) {
        customSourceButton.setTextColor(Color.WHITE);
        customSourceButton.setBackgroundResource(R.drawable.customize_button);

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
                final String pasteData = getTextFromClipboard(getActivity(), clipboard);
                if(pasteData != null){
                    getSourceHtml(pasteData, sourceSelect);
                }else{
                    Toast.makeText(getActivity(), getString(R.string.empty_clipboard_toast),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Button getCustomSourceButton(final RadioGroup sourceSelect) {
        Button customSourceButton = new Button(getActivity());
        initCustomSourceButton(customSourceButton, sourceSelect);
        return customSourceButton;
    }

    private void getSourceHtml(final String url, final RadioGroup sourceSelect) {
        // check if text is URL
        ParseHtmlAsyncTask titleTask =
            new ParseHtmlAsyncTask(url, new ParseHtmlAsyncTask.Callback(){
                @Override
                public void onComplete(Object o, Error error) {
                    if(error != null){
                        Toast.makeText(getActivity(), "The clipboard text is not a valid URL.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String title = (String) o;

                    String baseUrl = App.getDisplayUrl(url);

                    Article customArticle = new Article(title, baseUrl, url);
                    if(sourceSelect != null) sourceSelect.clearCheck();
                    ((CustomizeActivity)getActivity()).updateSource(customArticle);
                    ((CustomizeActivity)getActivity()).nextItem.setEnabled(true);
                }
            });
        ((CustomizeActivity)getActivity()).tasks.add(titleTask);
        titleTask.execute();
    }

    public int[] getColors(int c) {
        if (c == getResources().getColor(R.color.md_red_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_red_300),
                    getResources().getColor(R.color.md_red_400),
                    getResources().getColor(R.color.md_red_500),
                    getResources().getColor(R.color.md_red_600),
                    getResources().getColor(R.color.md_red_700),
                    getResources().getColor(R.color.md_red_800),
                    getResources().getColor(R.color.md_red_900)
            };
        } else if (c == getResources().getColor(R.color.md_pink_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_pink_300),
                    getResources().getColor(R.color.md_pink_400),
                    getResources().getColor(R.color.md_pink_500),
                    getResources().getColor(R.color.md_pink_600),
                    getResources().getColor(R.color.md_pink_700),
                    getResources().getColor(R.color.md_pink_800),
                    getResources().getColor(R.color.md_pink_900)
            };
        } else if (c == getResources().getColor(R.color.md_purple_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_purple_300),
                    getResources().getColor(R.color.md_purple_400),
                    getResources().getColor(R.color.md_purple_500),
                    getResources().getColor(R.color.md_purple_600),
                    getResources().getColor(R.color.md_purple_700),
                    getResources().getColor(R.color.md_purple_800),
                    getResources().getColor(R.color.md_purple_900)
            };
        } else if (c == getResources().getColor(R.color.md_deep_purple_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_deep_purple_300),
                    getResources().getColor(R.color.md_deep_purple_400),
                    getResources().getColor(R.color.md_deep_purple_500),
                    getResources().getColor(R.color.md_deep_purple_600),
                    getResources().getColor(R.color.md_deep_purple_700),
                    getResources().getColor(R.color.md_deep_purple_800),
                    getResources().getColor(R.color.md_deep_purple_900)
            };
        } else if (c == getResources().getColor(R.color.md_indigo_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_indigo_300),
                    getResources().getColor(R.color.md_indigo_400),
                    getResources().getColor(R.color.md_indigo_500),
                    getResources().getColor(R.color.md_indigo_600),
                    getResources().getColor(R.color.md_indigo_700),
                    getResources().getColor(R.color.md_indigo_800),
                    getResources().getColor(R.color.md_indigo_900)
            };
        } else if (c == getResources().getColor(R.color.md_blue_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_blue_300),
                    getResources().getColor(R.color.md_blue_400),
                    getResources().getColor(R.color.md_blue_500),
                    getResources().getColor(R.color.md_blue_600),
                    getResources().getColor(R.color.md_blue_700),
                    getResources().getColor(R.color.md_blue_800),
                    getResources().getColor(R.color.md_blue_900)
            };
        } else if (c == getResources().getColor(R.color.md_light_blue_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_light_blue_300),
                    getResources().getColor(R.color.md_light_blue_400),
                    getResources().getColor(R.color.md_light_blue_500),
                    getResources().getColor(R.color.md_light_blue_600),
                    getResources().getColor(R.color.md_light_blue_700),
                    getResources().getColor(R.color.md_light_blue_800),
                    getResources().getColor(R.color.md_light_blue_900)
            };
        } else if (c == getResources().getColor(R.color.md_cyan_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_cyan_300),
                    getResources().getColor(R.color.md_cyan_400),
                    getResources().getColor(R.color.md_cyan_500),
                    getResources().getColor(R.color.md_cyan_600),
                    getResources().getColor(R.color.md_cyan_700),
                    getResources().getColor(R.color.md_cyan_800),
                    getResources().getColor(R.color.md_cyan_900)
            };
        } else if (c == getResources().getColor(R.color.md_teal_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_teal_300),
                    getResources().getColor(R.color.md_teal_400),
                    getResources().getColor(R.color.md_teal_500),
                    getResources().getColor(R.color.md_teal_600),
                    getResources().getColor(R.color.md_teal_700),
                    getResources().getColor(R.color.md_teal_800),
                    getResources().getColor(R.color.md_teal_900)
            };
        } else if (c == getResources().getColor(R.color.md_green_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_green_300),
                    getResources().getColor(R.color.md_green_400),
                    getResources().getColor(R.color.md_green_500),
                    getResources().getColor(R.color.md_green_600),
                    getResources().getColor(R.color.md_green_700),
                    getResources().getColor(R.color.md_green_800),
                    getResources().getColor(R.color.md_green_900)
            };
        } else if (c == getResources().getColor(R.color.md_light_green_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_light_green_300),
                    getResources().getColor(R.color.md_light_green_400),
                    getResources().getColor(R.color.md_light_green_500),
                    getResources().getColor(R.color.md_light_green_600),
                    getResources().getColor(R.color.md_light_green_700),
                    getResources().getColor(R.color.md_light_green_800),
                    getResources().getColor(R.color.md_light_green_900)
            };
        } else if (c == getResources().getColor(R.color.md_amber_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_amber_300),
                    getResources().getColor(R.color.md_amber_400),
                    getResources().getColor(R.color.md_amber_500),
                    getResources().getColor(R.color.md_amber_600),
                    getResources().getColor(R.color.md_amber_700),
                    getResources().getColor(R.color.md_amber_800),
                    getResources().getColor(R.color.md_amber_900)
            };
        } else if (c == getResources().getColor(R.color.md_orange_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_orange_300),
                    getResources().getColor(R.color.md_orange_400),
                    getResources().getColor(R.color.md_orange_500),
                    getResources().getColor(R.color.md_orange_600),
                    getResources().getColor(R.color.md_orange_700),
                    getResources().getColor(R.color.md_orange_800),
                    getResources().getColor(R.color.md_orange_900)
            };
        } else if (c == getResources().getColor(R.color.md_deep_orange_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_deep_orange_300),
                    getResources().getColor(R.color.md_deep_orange_400),
                    getResources().getColor(R.color.md_deep_orange_500),
                    getResources().getColor(R.color.md_deep_orange_600),
                    getResources().getColor(R.color.md_deep_orange_700),
                    getResources().getColor(R.color.md_deep_orange_800),
                    getResources().getColor(R.color.md_deep_orange_900)
            };
        } else if (c == getResources().getColor(R.color.md_brown_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_brown_300),
                    getResources().getColor(R.color.md_brown_400),
                    getResources().getColor(R.color.md_brown_500),
                    getResources().getColor(R.color.md_brown_600),
                    getResources().getColor(R.color.md_brown_700),
                    getResources().getColor(R.color.md_brown_800),
                    getResources().getColor(R.color.md_brown_900)
            };
        } else if (c == getResources().getColor(R.color.md_grey_500)) {
            return new int[]{
                    getResources().getColor(R.color.md_grey_300),
                    getResources().getColor(R.color.md_grey_400),
                    getResources().getColor(R.color.md_grey_500),
                    getResources().getColor(R.color.md_grey_600),
                    getResources().getColor(R.color.md_grey_700),
                    getResources().getColor(R.color.md_grey_800),
                    getResources().getColor(R.color.md_grey_900)
            };
        } else {
            return new int[]{
                    getResources().getColor(R.color.md_blue_grey_300),
                    getResources().getColor(R.color.md_blue_grey_400),
                    getResources().getColor(R.color.md_blue_grey_500),
                    getResources().getColor(R.color.md_blue_grey_600),
                    getResources().getColor(R.color.md_blue_grey_700),
                    getResources().getColor(R.color.md_blue_grey_800),
                    getResources().getColor(R.color.md_blue_grey_900)
            };

        }
    }
}