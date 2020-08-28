package com.tomatedigital.adinjector.listener;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.tomatedigital.adinjector.handler.GenericAdHandler;

public abstract class GenericAdListener extends AdListener {

    protected static final AdSize INTERTITIAL = new AdSize(123, 2345);
    protected static final AdSize REWARD = new AdSize(1567, 212);


    private final String adUnit;
    private final AdSize size;
    protected final GenericAdHandler handler;
    final long retryInterval;
    protected Activity activity;
    protected AdStatus status;

    int triesFailed;
    long lastFailTimestamp;


    private long lastLoadTimestamp;
    private int clicks;

    private final String adUser;


    GenericAdListener(@NonNull final Activity context, @NonNull final String adUnit, @NonNull String adUserId, @NonNull final GenericAdHandler handler, @NonNull final AdSize size, final long retryInterval) {
        this.adUnit = adUnit;
        this.activity = context;
        this.size = size;
        this.handler = handler;
        this.retryInterval = retryInterval;
        this.adUser = adUserId;
        this.status = AdStatus.EMPTY;
        this.triesFailed = 0;
        this.lastFailTimestamp = 0;
        this.lastLoadTimestamp = 0;
        this.clicks = 0;

    }


    @Override
    /**
     * Error Code 1 : ERROR_CODE_INVALID_REQUEST
     * Error Code 2 : ERROR_CODE_NETWORK_ERROR
     * Error Code 3 : ERROR_CODE_NO_FILL
     */
    public void onAdFailedToLoad(LoadAdError i) {

        logError(i, this.size);
        this.status = AdStatus.FAILED;
        this.lastFailTimestamp = System.currentTimeMillis();

        this.handler.loadAd();
    }


    @Override
    public void onAdLoaded() {
        FirebaseCrashlytics.getInstance().log("ad: " + this.adUnit + " loaded");
        this.triesFailed = 0;
        this.lastLoadTimestamp = System.currentTimeMillis();
        this.status = AdStatus.LOADED;


    }

    @Override
    public void onAdClicked() {
        FirebaseCrashlytics.getInstance().log("ad: " + this.adUnit + " clicked: " + ++this.clicks);
    }


    public AdStatus getStatus() {
        return this.status;
    }

    public boolean shouldLoad() {
        return this.status != AdStatus.LOADED && this.status != AdStatus.LOADING && (this.status != AdStatus.FAILED || (System.currentTimeMillis() - this.lastFailTimestamp) > this.retryInterval * this.triesFailed);
    }

    public long getLastLoadTimestamp() {
        return this.lastLoadTimestamp;
    }


    @NonNull
    String getAdUnit() {
        return this.adUnit;
    }

    private void logError(final LoadAdError i, @NonNull final AdSize size) {
        Bundle b = new Bundle();
        b.putInt("error_code", i.getCode());
        b.putString("error_message", i.getMessage());

        b.putString("ad_unit", this.adUnit);
        b.putInt("retries", ++this.triesFailed);
        b.putString("ad_user", this.adUser);
        if (this.lastFailTimestamp > 0)
            b.putLong("last_error_interval", System.currentTimeMillis() - this.lastFailTimestamp);


        int sizeCode = 0;

        if (size == AdSize.BANNER)
            sizeCode = 1;
        else if (size == AdSize.FULL_BANNER)
            sizeCode = 2;
        else if (size == AdSize.LARGE_BANNER)
            sizeCode = 3;
        else if (size == AdSize.MEDIUM_RECTANGLE)
            sizeCode = 4;
        else if (size == AdSize.SMART_BANNER)
            sizeCode = 5;
        else if (size == INTERTITIAL)
            sizeCode = 6;
        else if (size == REWARD)
            sizeCode = 7;

        b.putInt("size", sizeCode);
        FirebaseCrashlytics.getInstance().log("ad: " + this.adUnit + " failed to load: " + this.triesFailed + " code: " + i + " lastError: " + (System.currentTimeMillis() - this.lastFailTimestamp) / 1000 + "sec ago");
        FirebaseAnalytics.getInstance(this.activity).logEvent("ad_failed_to_load", b);
    }

    public void loading() {
        this.status = AdStatus.LOADING;
    }

    public enum AdType {
        DEFAULT, BUSY, CARD, REWARD_VIDEO, INTERSTITIAL
    }

    public enum AdStatus {
        EMPTY, LOADING, LOADED, WATCHED, FAILED, REWARDED
    }


}
