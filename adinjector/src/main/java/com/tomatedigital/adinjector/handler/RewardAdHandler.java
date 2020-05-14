package com.tomatedigital.adinjector.handler;

import android.app.Activity;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
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

    private final long waitConsecutiveVideos;
    private long lastVideoAd;

    public RewardAdHandler(@NonNull final Activity activity, @NonNull final String reward_ad_unit_id, @NonNull final String inters_ad_unit_id, final long waitConsecutiveVideos, final long waitForRetry, @NonNull final String[] keywords) {
        super( keywords);

        this.waitConsecutiveVideos = waitConsecutiveVideos;
        this.lastVideoAd = 0L;

        this.rewardAdUnitId = reward_ad_unit_id;
        this.rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(activity);
        this.rewardedVideoAdListener = new RewardAdListener(activity, this, reward_ad_unit_id, GenericAdListener.getREWARD(), waitForRetry);
        this.rewardedVideoAd.setRewardedVideoAdListener(this.rewardedVideoAdListener);
        this.rewardedVideoAd.setImmersiveMode(true);

        this.interstitialAd = new InterstitialAd(activity);
        this.interstitialAd.setAdUnitId(inters_ad_unit_id);
        this.interstitialAdListener = new RewardAdListener(activity, this, inters_ad_unit_id, GenericAdListener.getINTERTITIAL(), waitForRetry) {

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

        this.loadAd(activity);
        FirebaseCrashlytics.getInstance().log("RewardAdHandler created");

    }

    @MainThread
    public void loadAd(@NonNull final Activity act) {

        if (this.rewardedVideoAdListener.shouldLoad()) {
            this.rewardedVideoAd.loadAd(this.rewardAdUnitId, AdMobRequestUtil.buildAdRequest(AdsAppCompatActivity.getLoc(), this.keywords).build());
            this.rewardedVideoAdListener.loading();
            this.videoAdCount = 0;
        }

        if (this.interstitialAdListener.shouldLoad()) {
            this.interstitialAd.loadAd(AdMobRequestUtil.buildAdRequest(AdsAppCompatActivity.getLoc(), this.keywords).build());
            this.interstitialAdListener.loading();
            this.intertitialAdCount = 0;
        }

        this.interstitialAdListener.setContext(act);
        this.rewardedVideoAdListener.setContext(act);
    }

    @Override
    public boolean hasAdFailed() {
        return this.rewardedVideoAdListener.getStatus() == RewardAdListener.AdStatus.FAILED && this.interstitialAdListener.getStatus() == GenericAdListener.AdStatus.FAILED;
    }


    public boolean isAdReady() {
        return this.rewardedVideoAdListener.getStatus() == RewardAdListener.AdStatus.LOADED || this.interstitialAdListener.getStatus() == GenericAdListener.AdStatus.LOADED;

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

    @Override
    public long getLastRefresh() {
        return Math.max(this.interstitialAdListener.getLastLoadTimestamp(), this.rewardedVideoAdListener.getLastLoadTimestamp());
    }

    @MainThread
    public void showAd(@NonNull final RewardAdListener.VideoRewardListener rewardListener, @Nullable GenericAdListener.AdType preference) {

        if (this.rewardedVideoAdListener.getStatus() == GenericAdListener.AdStatus.LOADED && System.currentTimeMillis() - this.lastVideoAd > this.waitConsecutiveVideos && (preference != GenericAdListener.AdType.INTERSTICIAL || this.interstitialAdListener.getStatus() != GenericAdListener.AdStatus.LOADED)) {
            FirebaseCrashlytics.getInstance().log("shown video ad: " + this.videoAdCount++);
            this.rewardedVideoAdListener.setOnVideoRewardListener(rewardListener);
            this.rewardedVideoAd.show();
            this.lastVideoAd = System.currentTimeMillis();
        } else if (this.interstitialAdListener.getStatus() == GenericAdListener.AdStatus.LOADED ) {
            FirebaseCrashlytics.getInstance().log("shown interstitial ad: " + this.intertitialAdCount++);
            this.interstitialAdListener.setOnVideoRewardListener(rewardListener);
            this.interstitialAd.show();
        } else
            FirebaseCrashlytics.getInstance().log("showAd couldn't select reward ad to show");
    }

    @Override
    public int getShownCount() {
        return this.videoAdCount + this.intertitialAdCount;
    }
}
