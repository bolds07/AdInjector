package com.tomatedigital.adinjector.handler;

import android.app.Activity;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.tomatedigital.adinjector.listener.GenericAdListener;

public abstract class GenericAdHandler {


    @NonNull
    final String[] keywords;

    protected GenericAdListener listener;

    GenericAdHandler(@NonNull final String[] keywords) {
        this.keywords = keywords;

    }


    @MainThread

    public boolean loadAd() {


        if (this.listener.shouldLoad()) {
            this.load();
            this.listener.loading();
            return true;
        }

        return false;
    }

    public boolean hasAdFailed() {
        return this.listener.getStatus() == GenericAdListener.AdStatus.FAILED;
    }


    public boolean isAdReady() {
        return this.listener.getStatus() == GenericAdListener.AdStatus.LOADED;
    }

    protected abstract void load();


    public abstract void destroy(@NonNull final Activity adsAppCompatActivity);

    public abstract void pause(@NonNull final Activity adsAppCompatActivity);

    public abstract void resume(@NonNull final Activity adsAppCompatActivity);




}
