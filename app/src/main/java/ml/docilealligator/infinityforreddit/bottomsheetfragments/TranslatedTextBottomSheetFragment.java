package ml.docilealligator.infinityforreddit.bottomsheetfragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.activities.BaseActivity;
import ml.docilealligator.infinityforreddit.customviews.LandscapeExpandedRoundedBottomSheetDialogFragment;
import ml.docilealligator.infinityforreddit.utils.Utils;

public class TranslatedTextBottomSheetFragment extends LandscapeExpandedRoundedBottomSheetDialogFragment {

    public static final String EXTRA_ORIGINAL_TEXT = "EOT";
    public static final String EXTRA_TRANSLATED_TEXT = "ETT";

    private BaseActivity activity;

    public static void show(FragmentManager fragmentManager, String originalText, String translatedText) {
        TranslatedTextBottomSheetFragment fragment = new TranslatedTextBottomSheetFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ORIGINAL_TEXT, originalText);
        bundle.putString(EXTRA_TRANSLATED_TEXT, translatedText);
        fragment.setArguments(bundle);
        fragment.show(fragmentManager, fragment.getTag());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_translated_text_bottom_sheet, container, false);

        TextView originalTextView = rootView.findViewById(R.id.original_text_view);
        TextView translatedTextView = rootView.findViewById(R.id.translated_text_view);

        Bundle bundle = getArguments();
        if (bundle != null) {
            String originalText = bundle.getString(EXTRA_ORIGINAL_TEXT, "");
            String translatedText = bundle.getString(EXTRA_TRANSLATED_TEXT, "");

            originalTextView.setText(originalText);
            translatedTextView.setText(translatedText);
        }

        if (activity.typeface != null) {
            Utils.setFontToAllTextViews(rootView, activity.typeface);
        }

        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (BaseActivity) context;
    }
}
