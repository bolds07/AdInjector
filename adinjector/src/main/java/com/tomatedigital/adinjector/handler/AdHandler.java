package com.tomatedigital.adinjector.handler;

import android.app.Activity;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

public abstract class AdHandler {


    @NonNull
    final String[] keywords;

    AdHandler(  @NonNull String[] keywords) {
        this.keywords = keywords;
    }


    @MainThread
    public abstract void loadAd(@NonNull final Activity c);

    public abstract boolean hasAdFailed();

    public abstract boolean isAdReady();

    public abstract void destroy(@NonNull final Activity adsAppCompatActivity);

    public abstract void pause(@NonNull final Activity adsAppCompatActivity);

    public abstract void resume(@NonNull final Activity adsAppCompatActivity);

    public abstract long getLastRefresh();


    public abstract int getShownCount();
}
