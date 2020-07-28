package com.tomatedigital.adinjector.handler;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.tomatedigital.adinjector.AdMobRequestUtil;
import com.tomatedigital.adinjector.AdsAppCompatActivity;
import com.tomatedigital.adinjector.listener.ResizableBannerAdListener;

public class ResizableBannerAdHandler extends GenericAdHandler {


    private Activity activity;

    private AdView adView;


    //actually it just get smaller, i will need this if i want to enlarge in future
    @SuppressWarnings("FieldCanBeLocal")
    private final AdSize maxSize;


    public ResizableBannerAdHandler(@NonNull AdView initialAd, @NonNull final String adUser, long busyRefreshInterval, int minBusyShowTimes, long retryInterval, @NonNull String[] keywords) {
        super(keywords);
        this.maxSize = initialAd.getAdSize();
        this.adView = initialAd;

        this.activity = (Activity) initialAd.getContext();
        this.listener = new ResizableBannerAdListener(this.activity, initialAd.getAdUnitId(),adUser, this.maxSize, this, busyRefreshInterval, minBusyShowTimes,retryInterval);
        this.adView.setAdListener(this.listener);

        this.loadAd(this.activity);
    }

    public long getLastLoadTimestamp() {
        return this.listener.getLastLoadTimestamp();
    }


    public boolean loadAd(@NonNull final Activity c) {
        this.activity = c;
        ((ResizableBannerAdListener) this.listener).setContext(c);
        return super.loadAd();
    }

    @MainThread
    public void showAd(@NonNull final Activity c) {
        this.loadAd(c);
        this.adView.setVisibility(View.VISIBLE);

        this.adView.resume();

    }

    @MainThread
    public void hideAd() {
        this.adView.setVisibility(View.INVISIBLE);
        this.adView.pause();
    }


    @Override
    protected void load() {
        this.adView.loadAd(AdMobRequestUtil.buildAdRequest(AdsAppCompatActivity.getLoc(), this.keywords).build());
    }

    @Override
    public void destroy(@NonNull final Activity adsAppCompatActivity) {
        this.adView.destroy();
    }

    @Override
    public void pause(@NonNull final Activity adsAppCompatActivity) {
        this.adView.pause();
    }

    @Override
    public void resume(@NonNull final Activity adsAppCompatActivity) {
        this.activity = adsAppCompatActivity;
        this.adView.resume();
    }


    public void resize() {


        AdSize size = null;

        if (this.adView.getAdSize().equals(AdSize.MEDIUM_RECTANGLE))
            size = AdSize.LARGE_BANNER;
        else if (this.adView.getAdSize().equals(AdSize.LARGE_BANNER))
            size = AdSize.BANNER;


        if (size != null) {
            AdView newAd = new AdView(this.activity);
            newAd.setId(this.adView.getId());
            newAd.setAdUnitId(this.adView.getAdUnitId());
            newAd.setAdSize(size);
            newAd.setAdListener(this.adView.getAdListener());
            newAd.setVisibility(this.adView.getVisibility());

            ViewGroup.LayoutParams layout = this.adView.getLayoutParams();
            ViewGroup container = (ViewGroup) this.adView.getParent();
            container.removeView(this.adView);
            this.adView.destroy();

            this.adView = newAd;
            container.addView(this.adView);
            this.adView.setLayoutParams(layout);

            this.loadAd(this.activity);
        }
    }


    public void changeContainer(@NonNull final ViewGroup container,@NonNull final Activity activity) {
        try {
            ViewGroup.LayoutParams layoutParams = this.adView.getLayoutParams();
            ((ViewGroup) this.adView.getParent()).removeView(this.adView);
            container.addView(this.adView);
            this.adView.setLayoutParams(layoutParams);
            this.loadAd(activity);
        } catch (Exception e) {
            //monitore
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }


}
