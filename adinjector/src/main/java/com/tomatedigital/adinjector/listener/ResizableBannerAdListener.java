package com.tomatedigital.adinjector.listener;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdSize;
import com.tomatedigital.adinjector.handler.ResizableBannerAdHandler;

public class ResizableBannerAdListener extends GenericAdListener {


    private final long refreshInterval;
    private final ResizableBannerAdHandler adHandler;


    public ResizableBannerAdListener(@NonNull final Activity activity, @NonNull final String adunit, @NonNull final AdSize size, @NonNull final ResizableBannerAdHandler handler, final long refreshInterval) {
        super(activity, adunit, size);
        this.refreshInterval = refreshInterval;


        this.adHandler = handler;
    }


    public long getRetry() {
        return triesFailed;
    }


    @Override
    public void onAdFailedToLoad(int i) {
        super.onAdFailedToLoad(i);
        this.adHandler.resize();
    }


    @Override
    public boolean shouldLoad() {
        return !this.activity.isFinishing() && (this.getStatus() == AdStatus.FAILED || (System.currentTimeMillis() - this.adHandler.getLastRefresh() > this.refreshInterval));
    }
}