package com.tomatedigital.adinjector.listener;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdSize;
import com.tomatedigital.adinjector.handler.ResizableBannerAdHandler;

public class ResizableBannerAdListener extends GenericAdListener {


    private final long refreshInterval;
    private final ResizableBannerAdHandler adHandler;
    private int minShowCount;
    private int showCount;


    public ResizableBannerAdListener(@NonNull final Activity activity, @NonNull final String adunit, @NonNull final AdSize size, @NonNull final ResizableBannerAdHandler handler, final long refreshInterval, final int minShowCount) {
        super(activity, adunit, handler, size, 0L);
        this.refreshInterval = refreshInterval;
        this.minShowCount = minShowCount;

        this.adHandler = handler;
    }


    public void setContext(@NonNull final Activity c) {
        this.activity = c;
    }

    @Override
    public void onAdLoaded() {
        super.onAdLoaded();
        this.showCount = 0;
    }

    @Override
    public void onAdFailedToLoad(int i) {
        super.onAdFailedToLoad(i);
        this.adHandler.resize();
    }

    /*
        //this method never is called for banners
        @Override
        public void onAdClosed() {
            //   super.onAdClosed(); does nothing

            this.status = AdStatus.EMPTY;
           // ((ResizableBannerAdHandler) this.handler).loadAd(this.activity);


        }
    */

    @Override
    public void onAdOpened() {
        this.showCount++;
        super.onAdOpened();
    }

    @Override
    public boolean shouldLoad() {
        return !this.activity.isFinishing() && (this.status == AdStatus.EMPTY || this.status == AdStatus.FAILED || (this.showCount >= this.minShowCount && System.currentTimeMillis() - this.getLastLoadTimestamp() > this.refreshInterval));
    }
}