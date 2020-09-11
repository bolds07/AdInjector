package com.tomatedigital.adinjector;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;


public class AdMobRequestUtil {

    static {
        //MobileAds.setAppMuted(true); // Note: Video ads that are ineligible to be shown with muted audio are not returned for ad requests made when the app volume is reported as muted or set to a value of 0. This may restrict a subset of the broader video ads pool from serving.
        MobileAds.setAppVolume(0.4f);
        MobileAds.setRequestConfiguration(MobileAds.getRequestConfiguration()
                .toBuilder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_MA)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .build());
    }

    @NonNull
    public static AdRequest.Builder buildAdRequest(@Nullable Location loc, @NonNull String... keywords) {

        AdRequest.Builder adRequest = new AdRequest.Builder();


        //adRequest.setContentUrl("https://www.sorteiomaster.com.br");
        //xadRequest.set

        for (String keyword : keywords)
            adRequest.addKeyword(keyword);
        if (loc != null)
            adRequest.setLocation(loc);


        return adRequest;
    }

}
