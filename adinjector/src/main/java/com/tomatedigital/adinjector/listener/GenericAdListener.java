package com.tomatedigital.adinjector.listener;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tomatedigital.adinjector.AdsAppCompatActivity;

public abstract class GenericAdListener extends AdListener {

    private static final AdSize INTERTITIAL = new AdSize(123, 2345);
    private static final AdSize REWARD = new AdSize(1567, 212);


    private final String adUnit;
    private final AdSize size;
    Context context;
    protected AdStatus status;

    int triesFailed;
    long lastFailTimestamp;


    private long lastLoadTimestamp;
    private int clicks;

    protected RewardItem lastReward;



    GenericAdListener(Context context, String adUnit, AdSize size) {
        this.adUnit = adUnit;
        this.context = context;
        this.size = size;

        this.status = AdStatus.EMPTY;
        this.triesFailed = 0;
        this.lastFailTimestamp = 0;
        this.lastLoadTimestamp = 0;
        this.clicks = 0;

    }

    @NonNull
    public static AdSize getREWARD() {
        return REWARD;
    }

    @NonNull
    public static AdSize getINTERTITIAL() {
        return INTERTITIAL;
    }

    @Override
    public void onAdFailedToLoad(int i) {
        logError(i, this.size);
        this.status = AdStatus.FAILED;
        this.lastFailTimestamp = System.currentTimeMillis();


    }

    @Override
    public void onAdLoaded() {
        Crashlytics.log(Log.DEBUG,"DEBUG","ad: " + this.adUnit + " loaded");
        this.triesFailed = 0;
        this.lastLoadTimestamp = System.currentTimeMillis();
        this.status = AdStatus.LOADED;


    }

    @Override
    public void onAdClicked() {
        Crashlytics.log(Log.DEBUG,"DEBUG","ad: " + this.adUnit + " clicked: " + this.clicks++);
        this.status = AdStatus.CLICKED;

    }


    public void setContext(AdsAppCompatActivity c) {
        this.context = c;
    }

    public AdStatus getStatus() {
        return this.status;
    }

    public abstract boolean shouldLoad();

    public long getLastLoadTimestamp() {
        return lastLoadTimestamp;
    }


    String getAdUnit() {
        return adUnit;
    }

    private void logError(int i, AdSize size) {
        Bundle b = new Bundle();
        b.putInt("error_code", i);

        b.putString("ad_unit", this.adUnit);
        b.putInt("retries", ++this.triesFailed);
        if (this.lastFailTimestamp > 0)
            b.putLong("failInterval", System.currentTimeMillis() - this.lastFailTimestamp);

        b.putLong("successInterval", System.currentTimeMillis() - this.lastLoadTimestamp);
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
        Crashlytics.log(Log.VERBOSE,"DEBUG","ad: " + this.adUnit + " failed to load: " + this.triesFailed + " code: " + i + " lastError: " + (System.currentTimeMillis()-this.lastFailTimestamp)/1000 + "sec ago");
        FirebaseAnalytics.getInstance(this.context).logEvent("ad_failed_to_load", b);
    }

    public void loading() {
        this.status = AdStatus.LOADING;
    }

    public enum AdType {
        DEFAULT, BUSY, CARD, REWARD, INTERSTICIAL
    }

    public enum AdStatus {
        EMPTY, LOADING, LOADED, WATCHED, CLICKED, FAILED, REWARDED
    }


}
