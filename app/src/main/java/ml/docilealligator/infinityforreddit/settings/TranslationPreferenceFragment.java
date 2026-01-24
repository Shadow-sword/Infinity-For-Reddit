package ml.docilealligator.infinityforreddit.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;

import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.customviews.preference.CustomFontPreferenceFragmentCompat;

public class TranslationPreferenceFragment extends CustomFontPreferenceFragmentCompat {
    public TranslationPreferenceFragment() {}

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.translation_preferences, rootKey);
        ((Infinity) mActivity.getApplication()).getAppComponent().inject(this);
    }
}
