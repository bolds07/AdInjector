package com.tomatedigital.adinjector.handler;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.tomatedigital.adinjector.listener.GenericAdListener;
import com.tomatedigital.adinjector.listener.RewardAdListener;

public abstract class ShowableAdHandler extends GenericAdHandler {


    ShowableAdHandler(@NonNull String[] keywords ) {
        super(keywords);
    }

    @MainThread
    public abstract void showAd(@NonNull final RewardAdListener.RewardListener rewardListener);
}
