package com.tomatedigital.adinjector;

import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;


public class AdMobRequestUtil {

    @NonNull
    public static AdRequest.Builder buildAdRequest(@Nullable Location loc, @NonNull String... keywords) {
        Bundle extras = new Bundle();
        extras.putString("max_ad_content_rating", "MA");
        AdRequest.Builder adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras);


        for (String keyword : keywords)
            adRequest.addKeyword(keyword);
        if (loc != null)
            adRequest.setLocation(loc);

        return adRequest;
    }

}
