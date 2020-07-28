package com.tomatedigital.adinjector.listener;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.reward.RewardItem;
import com.tomatedigital.adinjector.handler.IntertitialAdHandler;

public class InterstitialAdListener extends GenericAdListener {

    private static final RewardItem REWARD_ITEM = new RewardItem() {
        @Override
        public String getType() {
            return "interstitial";
        }

        @Override
        public int getAmount() {
            return 1;
        }
    };

    private static final long MIN_WATCH_TIME = 2500;

    private long openTime;
    private RewardAdListener.RewardListener rewardListener;

    public InterstitialAdListener(@NonNull final Activity context, @NonNull final String adUnit, @NonNull final String adUser,@NonNull final IntertitialAdHandler handler, final long retryInterval) {
        super(context, adUnit,adUser, handler, INTERTITIAL, retryInterval);


    }


    public void onAdClosed() {

        if (System.currentTimeMillis() - this.openTime > MIN_WATCH_TIME) {
            this.status = AdStatus.REWARDED;
            this.rewardListener.onRewarded(REWARD_ITEM);
        } else
            this.status = AdStatus.EMPTY;


        this.handler.loadAd();
    }


    public void onAdOpened() {
        this.status = AdStatus.WATCHED;
        this.openTime = System.currentTimeMillis();
    }

    /*
    public void onAdLoaded() {
        super.onAdLoaded();
    }*/

    /*public void onAdClicked() {
        super.onAdClicked();
    }*/

    //only native


    public void setOnVideoRewardListener(@NonNull final RewardAdListener.RewardListener rewardListener) {
        this.rewardListener = rewardListener;
    }
}
