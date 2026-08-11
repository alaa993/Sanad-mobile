package com.brightpath.sanad.data;

import android.app.Activity;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;

/**
 * ربط أزرار اللغة — نفس أسلوب تبديل الثيم (OnClick مباشر) لأن مستمع ToggleGroup
 * لا يُطلق دائماً داخل NestedScrollView.
 */
public final class LanguageUiHelper {

    private LanguageUiHelper() {}

    public static void bindToggleGroup(@NonNull Fragment fragment,
                                       @Nullable MaterialButtonToggleGroup group,
                                       @IdRes int btnArabic,
                                       @IdRes int btnEnglish,
                                       @IdRes int btnTurkish) {
        if (group == null || !fragment.isAdded()) return;
        Activity activity = fragment.getActivity();
        if (activity == null) return;
        bindGroup(activity, group, btnArabic, btnEnglish, btnTurkish);
    }

    public static void bindAuthToggleGroup(@NonNull Activity activity,
                                           @Nullable MaterialButtonToggleGroup group,
                                           @IdRes int btnArabic,
                                           @IdRes int btnEnglish,
                                           @IdRes int btnTurkish) {
        if (group == null) return;
        bindGroup(activity, group, btnArabic, btnEnglish, btnTurkish);
    }

    private static void bindGroup(@NonNull Activity activity,
                                  @NonNull MaterialButtonToggleGroup group,
                                  @IdRes int btnArabic,
                                  @IdRes int btnEnglish,
                                  @IdRes int btnTurkish) {
        String active = LocaleHelper.resolveSavedTag(activity);
        group.clearOnButtonCheckedListeners();

        bindButton(group, btnArabic, () -> selectLanguage(activity, group, btnArabic, "ar"));
        bindButton(group, btnEnglish, () -> selectLanguage(activity, group, btnEnglish, "en"));
        bindButton(group, btnTurkish, () -> selectLanguage(activity, group, btnTurkish, "tr"));

        group.check(buttonIdForTag(active, btnArabic, btnEnglish, btnTurkish));
    }

    private static void bindButton(@NonNull MaterialButtonToggleGroup group,
                                   @IdRes int buttonId,
                                   @NonNull Runnable action) {
        View button = group.findViewById(buttonId);
        if (button != null) {
            button.setOnClickListener(v -> action.run());
        }
    }

    private static void selectLanguage(@NonNull Activity activity,
                                       @NonNull MaterialButtonToggleGroup group,
                                       @IdRes int buttonId,
                                       @NonNull String tag) {
        try {
            com.brightpath.sanad.ui.tour.CoachMarkManager.dismissActive();
        } catch (Throwable ignored) {}
        try {
            group.check(buttonId);
        } catch (Throwable ignored) {}
        LocaleHelper.applyLocaleAndRecreate(activity, tag);
    }

    @IdRes
    static int buttonIdForTag(@NonNull String tag,
                              @IdRes int btnArabic,
                              @IdRes int btnEnglish,
                              @IdRes int btnTurkish) {
        if ("en".equalsIgnoreCase(tag)) return btnEnglish;
        if ("tr".equalsIgnoreCase(tag)) return btnTurkish;
        return btnArabic;
    }
}
