package com.tomatedigital.adinjector.handler;

import android.content.Context;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.tomatedigital.adinjector.AdMobRequestUtil;
import com.tomatedigital.adinjector.AdsAppCompatActivity;
import com.tomatedigital.adinjector.listener.GenericAdListener;
import com.tomatedigital.adinjector.listener.RewardAdListener;

public class RewardAdHandler extends AdHandler {


    private final String rewardAdUnitId;
    private final RewardedVideoAd rewardedVideoAd;
    @NonNull
    private final RewardAdListener rewardedVideoAdListener;
    private int videoAdCount;
    private int intertitialAdCount;

    @NonNull
    private final InterstitialAd interstitialAd;
    @NonNull
    private final RewardAdListener interstitialAdListener;


    public RewardAdHandler(@NonNull Context c, String reward_ad_unit_id, String inters_ad_unit_id, long waitForRetry, @NonNull String[] keywords) {
        super(c, keywords);

        this.rewardAdUnitId = reward_ad_unit_id;
        this.rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(c);
        this.rewardedVideoAdListener = new RewardAdListener(c, this, reward_ad_unit_id, GenericAdListener.getREWARD(), waitForRetry);
        this.rewardedVideoAd.setRewardedVideoAdListener(this.rewardedVideoAdListener);
        this.rewardedVideoAd.setImmersiveMode(true);

        this.interstitialAd = new InterstitialAd(c);
        this.interstitialAd.setAdUnitId(inters_ad_unit_id);
        this.interstitialAdListener = new RewardAdListener(c, this, inters_ad_unit_id, GenericAdListener.getINTERTITIAL(), waitForRetry) {

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                this.onRewarded(new RewardItem() {
                    @NonNull
                    @Override
                    public String getType() {
                        return "interstitial reward";
                    }

                    @Override
                    public int getAmount() {
                        return 1;
                    }
                });
                status = AdStatus.REWARDED;
            }
        };
        this.interstitialAd.setAdListener(this.interstitialAdListener);
        this.interstitialAd.setImmersiveMode(true);

        this.loadAd(c);
        Crashlytics.log(Log.DEBUG,"debug","RewardAdHandler created");

    }

    @MainThread
    public void loadAd(Context c) {

        if (this.rewardedVideoAdListener.shouldLoad()) {
            this.rewardedVideoAd.loadAd(this.rewardAdUnitId, AdMobRequestUtil.buildAdRequest(AdsAppCompatActivity.getLoc(), this.keywords).build());
            this.rewardedVideoAdListener.loading();
            this.videoAdCount = 0;
        } else if (this.rewardedVideoAdListener.getStatus() != GenericAdListener.AdStatus.LOADED && this.rewardedVideoAdListener.getStatus() != GenericAdListener.AdStatus.LOADING && this.interstitialAdListener.shouldLoad()) {
            this.interstitialAd.loadAd(AdMobRequestUtil.buildAdRequest(AdsAppCompatActivity.getLoc(), this.keywords).build());
            this.interstitialAdListener.loading();
            this.intertitialAdCount = 0;
        }

        this.interstitialAdListener.setContext((AdsAppCompatActivity) c);
        this.rewardedVideoAdListener.setContext((AdsAppCompatActivity) c);
    }

    @Override
    public boolean hasAdFailed() {
        return this.rewardedVideoAdListener.getStatus() == RewardAdListener.AdStatus.FAILED && this.interstitialAdListener.getStatus() == GenericAdListener.AdStatus.FAILED;
    }


    public boolean isAdReady() {
        return this.rewardedVideoAdListener.getStatus() == RewardAdListener.AdStatus.LOADED || this.interstitialAdListener.getStatus() == GenericAdListener.AdStatus.LOADED;

    }

    @Override
    public void destroy(Context adsAppCompatActivity) {
        this.rewardedVideoAd.destroy(adsAppCompatActivity);


    }

    @Override
    public void pause(Context adsAppCompatActivity) {
        this.rewardedVideoAd.pause(adsAppCompatActivity);



    }

    @Override
    public void resume(Context adsAppCompatActivity) {
        this.rewardedVideoAd.resume(adsAppCompatActivity);
    }

    @Override
    public long getLastRefresh() {
        return Math.max(this.interstitialAdListener.getLastLoadTimestamp(), this.rewardedVideoAdListener.getLastLoadTimestamp());
    }

    @MainThread
    public void showAd(RewardAdListener.VideoRewardListener rewardListener) {

        if (this.rewardedVideoAdListener.getStatus() == GenericAdListener.AdStatus.LOADED) {
            Crashlytics.log(Log.DEBUG,"DEBUG","shown video ad: " + this.videoAdCount++);
            this.rewardedVideoAdListener.setOnVideoRewardListener(rewardListener);
            this.rewardedVideoAd.show();
        } else if (this.interstitialAdListener.getStatus() == GenericAdListener.AdStatus.LOADED) {
            Crashlytics.log(Log.DEBUG,"DEBUG","shown interstitial ad: " + this.intertitialAdCount++);
            this.interstitialAdListener.setOnVideoRewardListener(rewardListener);
            this.interstitialAd.show();
        }
    }

    @Override
    public int getShownCount() {
        return this.videoAdCount + this.intertitialAdCount;
    }
}
