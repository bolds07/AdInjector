package com.tomatedigital.adinjector.listener;

import android.content.Context;

import com.google.android.gms.ads.AdSize;
import com.tomatedigital.adinjector.handler.ResizableBannerAdHandler;

public class ResizableBannerAdListener extends GenericAdListener {


    private final long refreshInterval;
    private final ResizableBannerAdHandler adHandler;


    public ResizableBannerAdListener(Context context, String adunit, AdSize size, ResizableBannerAdHandler handler, long refreshInterval) {
        super(context, adunit, size);
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
        return (System.currentTimeMillis() - this.adHandler.getLastRefresh() > this.refreshInterval);
    }
}