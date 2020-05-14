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

public class ResizableBannerAdHandler extends AdHandler {


    private Activity activity;
    private int adShownCount;
    private AdView adView;
    @NonNull
    private final ResizableBannerAdListener listener;


    //actually it just get smaller, i will need this if i want to enlarge in future
    private final AdSize maxSize;
    private long retriesInterval;


    public ResizableBannerAdHandler(@NonNull AdView initialAd, long busyRefreshInterval, @NonNull String[] keywords) {
        super(keywords);
        this.maxSize = initialAd.getAdSize();
        this.adView = initialAd;

        this.activity = (Activity) initialAd.getContext();
        this.listener = new ResizableBannerAdListener(this.activity, initialAd.getAdUnitId(), this.maxSize, this, busyRefreshInterval);
        this.adView.setAdListener(this.listener);
        this.loadAd((Activity) initialAd.getContext());

        this.adShownCount = 0;
    }

    public AdSize getAdSize() {
        return this.maxSize;
    }

    @Override
    public void loadAd(@NonNull final Activity c) {
        this.activity = c;
        this.listener.setContext(this.activity);
        if (this.listener.shouldLoad()) {
            this.adView.loadAd(AdMobRequestUtil.buildAdRequest(AdsAppCompatActivity.getLoc(), this.keywords).build());
            this.listener.loading();
            this.adShownCount = 0;
        }

    }

    @MainThread
    public void showAd() {
        this.adView.setVisibility(View.VISIBLE);
        this.adView.resume();
        this.adShownCount++;
    }

    @MainThread
    public void hideAd() {
        this.adView.setVisibility(View.INVISIBLE);
        this.adView.pause();
    }

    @Override
    public boolean hasAdFailed() {
        return false;
    }

    @Override
    public boolean isAdReady() {
        return true;
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

    @Override
    public long getLastRefresh() {
        return this.listener.getLastLoadTimestamp();
    }

    @Override
    public int getShownCount() {
        return this.adShownCount;
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


    public void changeContainer(@NonNull ViewGroup container) {
        try {
            ViewGroup.LayoutParams layoutParams = this.adView.getLayoutParams();
            ((ViewGroup) this.adView.getParent()).removeView(this.adView);
            container.addView(this.adView);
            this.adView.setLayoutParams(layoutParams);
        } catch (Exception e) {
            //monitore
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}
