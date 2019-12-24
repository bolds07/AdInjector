package com.tomatedigital.adinjector.handler;

import android.content.Context;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

public abstract class AdHandler {

    Context context;

    @NonNull
    final String[] keywords;

    AdHandler(@NonNull Context c, @NonNull String[] keywords) {
        this.keywords = keywords;
        this.context = c;
    }


    @MainThread
    public abstract void loadAd(Context c);

    public abstract boolean hasAdFailed();

    public abstract boolean isAdReady();

    public abstract void destroy(Context adsAppCompatActivity);

    public abstract void pause(Context adsAppCompatActivity);

    public abstract void resume(Context adsAppCompatActivity);

    public abstract long getLastRefresh();



    public abstract int getShownCount();
}
