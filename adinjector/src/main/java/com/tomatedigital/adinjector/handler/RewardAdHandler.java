package com.tomatedigital.adinjector.handler;

import android.app.Activity;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.tomatedigital.adinjector.AdMobRequestUtil;
import com.tomatedigital.adinjector.AdsAppCompatActivity;
import com.tomatedigital.adinjector.listener.RewardAdListener;

public class RewardAdHandler extends ShowableAdHandler {


    private final String rewardAdUnitId;
    private final RewardedVideoAd rewardedVideoAd;

    private int videoAdCount;


    public RewardAdHandler(@NonNull final Activity activity, @NonNull final String reward_ad_unit_id, final long waitForRetry, @NonNull final String[] keywords) {
        super(keywords);


        this.rewardAdUnitId = reward_ad_unit_id;
        this.rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(activity);
        this.listener = new RewardAdListener(activity, this, reward_ad_unit_id, waitForRetry);
        this.rewardedVideoAd.setRewardedVideoAdListener((RewardedVideoAdListener) this.listener);
        this.rewardedVideoAd.setImmersiveMode(true);


        this.loadAd();
        FirebaseCrashlytics.getInstance().log("RewardAdHandler created");

    }

    public void setUserId(@NonNull final String id) {
        this.rewardedVideoAd.setUserId(id);
    }


    @Override
    protected void load() {
        this.rewardedVideoAd.loadAd(this.rewardAdUnitId, AdMobRequestUtil.buildAdRequest(AdsAppCompatActivity.getLoc(), this.keywords).build());
    }

    @MainThread
    public void showAd(@NonNull final RewardAdListener.RewardListener rewardListener) {

        if (this.isAdReady()) {
            FirebaseCrashlytics.getInstance().log("shown video ad: " + this.videoAdCount++);
            ((RewardAdListener) this.listener).setOnVideoRewardListener(rewardListener);
            this.rewardedVideoAd.show();

        } else if (this.listener.shouldLoad())
            super.loadAd();
    }


    @Override
    public void destroy(@NonNull final Activity adsAppCompatActivity) {
        this.rewardedVideoAd.destroy(adsAppCompatActivity);


    }

    @Override
    public void pause(@NonNull final Activity adsAppCompatActivity) {
        this.rewardedVideoAd.pause(adsAppCompatActivity);


    }

    @Override
    public void resume(@NonNull final Activity adsAppCompatActivity) {
        this.rewardedVideoAd.resume(adsAppCompatActivity);
    }


    public long getLastRewardTimestamp() {
        return ((RewardAdListener) this.listener).getLastRewardTimestamp();
    }
}
