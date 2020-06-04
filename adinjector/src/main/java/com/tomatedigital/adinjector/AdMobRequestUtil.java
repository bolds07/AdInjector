package com.tomatedigital.adinjector;

import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.query.AdInfo;


public class AdMobRequestUtil {

    static {
        MobileAds.setAppMuted(true);
        //MobileAds.setAppVolume(0f);
        MobileAds.setRequestConfiguration(MobileAds.getRequestConfiguration()
                .toBuilder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_MA)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .build());
    }

    @NonNull
    public static AdRequest.Builder buildAdRequest(@Nullable Location loc, @NonNull String... keywords) {

        AdRequest.Builder adRequest = new AdRequest.Builder();


        adRequest.setContentUrl("https://www.sorteiomaster.com.br");


        for (String keyword : keywords)
            adRequest.addKeyword(keyword);
        if (loc != null)
            adRequest.setLocation(loc);



        return adRequest;
    }

}
