package com.brightpath.sanad.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREF = "onboarding_prefs";
    private static final String KEY_DONE = "onboarding_done";

    private ViewPager2 pager;
    private MaterialButton btnNext;
    private TextView btnSkip;

    private final List<OnboardingPage> pages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new com.brightpath.sanad.data.ThemeStore(this).applySavedTheme(this);
        setContentView(R.layout.activity_onboarding);

        pager = findViewById(R.id.onboardingPager);
        btnNext = findViewById(R.id.btnOnboardingNext);
        btnSkip = findViewById(R.id.btnOnboardingSkip);

        int logoRes = new com.brightpath.sanad.data.ThemeStore(this).getLogoRes(false);
        pages.add(new OnboardingPage(logoRes, R.string.onboarding_title_1, R.string.onboarding_desc_1));
        pages.add(new OnboardingPage(logoRes, R.string.onboarding_title_2, R.string.onboarding_desc_2));
        pages.add(new OnboardingPage(logoRes, R.string.onboarding_title_3, R.string.onboarding_desc_3));

        pager.setAdapter(new OnboardingPagerAdapter(pages));

        btnSkip.setOnClickListener(v -> finishOnboarding());
        btnNext.setOnClickListener(v -> {
            int current = pager.getCurrentItem();
            if (current < pages.size() - 1) {
                pager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                boolean last = position == pages.size() - 1;
                btnNext.setText(last ? R.string.onboarding_start : R.string.onboarding_next);
                btnSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DONE, true).apply();
        startActivity(new Intent(this, SplashActivity.class));
        finishAffinity();
    }

    private static class OnboardingPage {
        final int imageRes;
        final int titleRes;
        final int descRes;

        OnboardingPage(int imageRes, int titleRes, int descRes) {
            this.imageRes = imageRes;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    private static class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.Holder> {
        private final List<OnboardingPage> items;

        OnboardingPagerAdapter(List<OnboardingPage> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_page, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            OnboardingPage item = items.get(position);
            holder.image.setImageResource(item.imageRes);
            holder.title.setText(item.titleRes);
            holder.desc.setText(item.descRes);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final android.widget.ImageView image;
            final android.widget.TextView title;
            final android.widget.TextView desc;

            Holder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.onboardingImage);
                title = itemView.findViewById(R.id.onboardingTitle);
                desc = itemView.findViewById(R.id.onboardingDesc);
            }
        }
    }
}
