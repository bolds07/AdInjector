package com.tomatedigital.adinjector.listener;

import android.content.Context;

import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.tomatedigital.adinjector.handler.AdHandler;

public class RewardAdListener extends GenericAdListener implements RewardedVideoAdListener {


    private final long retryInterval;

    private VideoRewardListener rewardListener;
    @Nullable
    private RewardItem lastReward;

    private long lastRewardTimestamp;
    private final AdHandler handler;


    public RewardAdListener(Context c, AdHandler handler, String adunit, AdSize size, long retryInterval) {
        super(c, adunit, size);
        this.retryInterval = retryInterval;
        this.handler = handler;

    }

    public long getLastRewardTimestamp() {
        return this.lastRewardTimestamp;
    }

    public void setOnVideoRewardListener(VideoRewardListener listener) {
        this.rewardListener = listener;
    }


    @Override
    public void onRewardedVideoAdLoaded() {
        this.onAdLoaded();

    }

    @Override
    public void onRewardedVideoAdOpened() {
        this.status = AdStatus.WATCHED;
        this.lastReward = null;

    }

    @Override
    public void onAdOpened() {
        this.onRewardedVideoAdOpened();
    }


    @Override
    public void onRewardedVideoStarted() {
        Crashlytics.log("ad: " +this.getAdUnit() + " started fullscreen");
    }

    @Override
    public void onAdClosed() {
        this.onRewardedVideoAdClosed();
    }

    @Override
    public void onRewardedVideoAdClosed() {
        Crashlytics.log("ad: " + this.getAdUnit() + " closed");
        if (this.rewardListener != null)
            this.rewardListener.onVideoWatched(this.lastReward);


            this.handler.loadAd(this.context);


    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        this.status = AdStatus.REWARDED;
        this.lastRewardTimestamp = System.currentTimeMillis();


        this.lastReward = rewardItem;
    }


    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {
        this.onAdFailedToLoad(i);
        this.handler.loadAd(this.context);
    }


    @Override
    public void onRewardedVideoCompleted() {

    }

    public long getRetry() {
        return triesFailed;
    }

    public boolean shouldLoad() {
        return this.status != AdStatus.LOADED && this.status != AdStatus.LOADING && (this.status != AdStatus.FAILED || (System.currentTimeMillis() - this.lastFailTimestamp) > this.retryInterval * this.triesFailed);
    }


    public interface VideoRewardListener {
        void onVideoWatched(RewardItem reward);

    }


}

