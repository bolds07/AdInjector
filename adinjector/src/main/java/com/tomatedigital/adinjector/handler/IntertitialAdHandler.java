package com.tomatedigital.adinjector.handler;

import android.app.Activity;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.tomatedigital.adinjector.AdMobRequestUtil;
import com.tomatedigital.adinjector.AdsAppCompatActivity;
import com.tomatedigital.adinjector.listener.InterstitialAdListener;
import com.tomatedigital.adinjector.listener.RewardAdListener;

public class IntertitialAdHandler extends ShowableAdHandler {


    private int count;

    @NonNull
    private final InterstitialAd interstitialAd;


    public IntertitialAdHandler(@NonNull final Activity activity, @NonNull final String adunit,@NonNull final String adUser, final long waitBeforeRetryLoadAd, @NonNull final String[] keywords) {
        super(keywords);


        this.interstitialAd = new InterstitialAd(activity);
        this.interstitialAd.setAdUnitId(adunit);
        this.listener = new InterstitialAdListener(activity, adunit,adUser, this, waitBeforeRetryLoadAd);
        this.interstitialAd.setAdListener(this.listener);
        this.interstitialAd.setImmersiveMode(true);

        this.loadAd();
        FirebaseCrashlytics.getInstance().log("IntertitialAdHandler created");

    }


    @MainThread
    public void showAd(@NonNull final RewardAdListener.RewardListener rewardListener) {

        if (this.isAdReady()) {
            FirebaseCrashlytics.getInstance().log("shown interstitial ad: " + this.count++);
            ((InterstitialAdListener) this.listener).setOnVideoRewardListener(rewardListener);
            this.interstitialAd.show();
        }else if(this.listener.shouldLoad())
            super.loadAd();

    }
    @Override
    protected void load() {
        this.interstitialAd.loadAd(AdMobRequestUtil.buildAdRequest(AdsAppCompatActivity.getLoc(), this.keywords).build());
    }


    @Override
    public void destroy(@NonNull Activity adsAppCompatActivity) {

    }

    @Override
    public void pause(@NonNull Activity adsAppCompatActivity) {

    }

    @Override
    public void resume(@NonNull Activity adsAppCompatActivity) {

    }




}
