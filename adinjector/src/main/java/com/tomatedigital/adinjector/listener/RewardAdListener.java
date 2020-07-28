package com.tomatedigital.adinjector.listener;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.tomatedigital.adinjector.BuildConfig;
import com.tomatedigital.adinjector.handler.RewardAdHandler;

public class RewardAdListener extends GenericAdListener implements RewardedVideoAdListener {


    private RewardListener rewardListener;
    @Nullable
    private RewardItem lastReward;

    private long lastRewardTimestamp;



    public RewardAdListener(@NonNull final Activity activity, @NonNull final RewardAdHandler handler,@NonNull final String adUser, @NonNull final String adunit, final long retryInterval) {
        super(activity, adunit,adUser, handler, REWARD, retryInterval);
    }

    public long getLastRewardTimestamp() {
        return this.lastRewardTimestamp;
    }

    public void setOnVideoRewardListener(RewardListener listener) {
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
    public void onRewardedVideoStarted() {
        //super.onAdOpened(); does nothing
        FirebaseCrashlytics.getInstance().log("ad: " + this.getAdUnit() + " started fullscreen");
    }


    @Override
    public void onRewardedVideoAdClosed() {
        //super.onAdClosed(); does nothing
        FirebaseCrashlytics.getInstance().log("ad: " + this.getAdUnit() + " closed");
        if (this.rewardListener != null) {
            if (this.status == AdStatus.REWARDED || BuildConfig.DEBUG)
                this.rewardListener.onRewarded(this.lastReward);
            else
                this.rewardListener.onCanceledBeforeReward();
        }



        this.handler.loadAd();


    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        this.status = AdStatus.REWARDED;
        this.lastRewardTimestamp = System.currentTimeMillis();


        this.lastReward = rewardItem;
    }


    @Override
    public void onRewardedVideoAdLeftApplication() {
        //super.onAdLeftApplication(); does nothing;
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {
        this.onAdFailedToLoad(i);
    }


    @Override
    public void onRewardedVideoCompleted() {

    }


    public interface RewardListener {
        void onRewarded(RewardItem reward);

        void onCanceledBeforeReward();
    }

    public static abstract class AlwaysAcceptRewardListener implements RewardListener {

        @Override
        public void onCanceledBeforeReward() {
            onRewarded(null);
        }
    }


}

