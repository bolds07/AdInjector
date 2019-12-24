package com.tomatedigital.adinjector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.tomatedigital.adinjector.handler.ResizableBannerAdHandler;
import com.tomatedigital.adinjector.handler.RewardAdHandler;
import com.tomatedigital.adinjector.listener.RewardAdListener;


@SuppressWarnings("SameReturnValue")
public abstract class AdsAppCompatActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener, OnSuccessListener<Location> {

    private static final int CODE_REQUEST_GPS = 25471;

    private static final long MINIMUM_BUSY_TIME = 5500;
    private static final int MINIMUM_BUSY_TIMES_FOR_REFRESH = 3;
    private static final long MAX_LOAD_AD_VIDEO_DURATION = 8000;


    private static Location loc;


    private ViewGroup mainLayout;
    private RelativeLayout fadeView;

    private AdView adView;

    @SuppressLint("StaticFieldLeak")
    private static ResizableBannerAdHandler busyHandler;
    private static long busyAdStartDisplayAt;


    @SuppressLint("StaticFieldLeak")
    private static RewardAdHandler rewardHandler;

    protected ViewGroup getMainLayout() {
        return mainLayout;
    }


    @NonNull
    private static String[] keywords = new String[0];

    protected static void setKeywords(@NonNull String[] keys) {
        keywords = keys;
    }

    public static Location getLoc() {
        return loc;
    }


    protected void showRewardedVideoOrLoad(@NonNull final RewardAdListener.VideoRewardListener rewardListener, final RewardItem reward) {
        if (rewardHandler.isAdReady())
            rewardHandler.showAd(rewardListener);
        else {
            busy(true);
            final long waitingTime = System.currentTimeMillis();
            rewardHandler.loadAd(this);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                while (!rewardHandler.isAdReady() && !rewardHandler.hasAdFailed()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                while (rewardHandler.hasAdFailed() && System.currentTimeMillis() - waitingTime < MAX_LOAD_AD_VIDEO_DURATION) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }

                runOnUiThread(() -> {
                    busy(false);
                    try {
                        if (rewardHandler.isAdReady())
                            rewardHandler.showAd(rewardListener);
                        else
                            rewardListener.onVideoWatched(reward);
                    } catch (Exception e) {
                        Crashlytics.logException(e);
                    }
                });
            });
        }
    }




    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null)
            busyAdStartDisplayAt = savedInstanceState.getLong("busyAdStartDisplayAt", System.currentTimeMillis());


    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong("busyAdStartDisplayAt", busyAdStartDisplayAt);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {

        if (this.adView != null)
            this.adView.destroy();


        super.onDestroy();
    }


    @Override
    public void onPause() {

        if (this.adView != null)
            this.adView.pause();


        if (rewardHandler != null)
            rewardHandler.pause(this);

        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (this.adView != null)
            this.adView.resume();



        if (rewardHandler != null)
            rewardHandler.resume(this);
    }


    protected boolean unlockPermissions(@NonNull String permission, int requestorCode) {
        if (Build.VERSION.SDK_INT > 18 && ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestorCode);
            return false;
        }

        return true;

    }


    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setExtraContent();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setExtraContent();
    }


    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        this.setExtraContent();

    }

    protected void setExtraContent() {


        this.mainLayout = (ViewGroup) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);

        if (loc == null)
            loadGpsLocation();
        injectStuff();
        createRewardHandler();

        if (this.showBannerAd() && (this.mainLayout instanceof LinearLayoutCompat || this.mainLayout instanceof LinearLayout))
            this.mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);

    }


    private void createRewardHandler() {
        if (rewardHandler == null)
            rewardHandler = new RewardAdHandler(this, this.getRewardAdUnit(), this.getInterstitialAdUnit(), this.getWaitBeforeRetryLoadAd(), keywords);
    }


    private void injectStuff() {


        //remove the main layout and add it to the new relative layout
        ViewGroup rootView = this.findViewById(android.R.id.content);
        RelativeLayout relativeLayout = new RelativeLayout(this);

        View oldMain = rootView.getChildAt(0);
        rootView.removeViewAt(0);

        rootView.addView(relativeLayout);
        ViewGroup.LayoutParams layout = relativeLayout.getLayoutParams();
        layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layout.height = ViewGroup.LayoutParams.MATCH_PARENT;
        relativeLayout.setLayoutParams(layout);

        relativeLayout.addView(oldMain);


        //add the fade view
        createBannerAd(relativeLayout);

        this.fadeView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.busy_layout, relativeLayout, false);

        relativeLayout.addView(this.fadeView);
        layout = this.fadeView.getLayoutParams();
        layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layout.height = ViewGroup.LayoutParams.MATCH_PARENT;
        this.fadeView.setLayoutParams(layout);
        this.fadeView.setVisibility(View.GONE);


        injectBusyAd(this.fadeView);


    }


    private void injectBusyAd(@NonNull final RelativeLayout relativeLayout) {

        if (this.showBusyAds()) {

            if (busyHandler == null) {
                AdView busyAd = new AdView(this);
                busyAd.setAdSize(AdSize.MEDIUM_RECTANGLE);
                busyAd.setAdUnitId(getBusyAdUnit());
                busyAd.setId(R.id.busyAd);

                relativeLayout.addView(busyAd);
                RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) busyAd.getLayoutParams();
                layout.addRule(RelativeLayout.CENTER_IN_PARENT);

                busyAd.loadAd(AdMobRequestUtil.buildAdRequest(loc, keywords).build());

                busyHandler = new ResizableBannerAdHandler(busyAd, this.getBusyRefreshInterval(), keywords);
            } else
                busyHandler.changeContainer(relativeLayout);

            busyHandler.hideAd();
        }
    }

    private void loadBusyAd() {
        if (busyHandler.getShownCount() > AdsAppCompatActivity.MINIMUM_BUSY_TIMES_FOR_REFRESH)
            busyHandler.loadAd(this);


    }


    private void createBannerAd(@NonNull ViewGroup container) {
        if (this.showBannerAd()) {
            this.adView = new AdView(this);
            this.adView.setId(R.id.adview);
            this.adView.setAdUnitId(this.getBannerAdUnit());
            container.addView(this.adView);
        } else
            this.adView = findViewById(R.id.adview);


    }


    @UiThread
    protected void busy(boolean b) {

        if (b || !this.showBusyAds()) {
            showBusyView(b);

        } else {

            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                while (System.currentTimeMillis() - busyAdStartDisplayAt < MINIMUM_BUSY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                runOnUiThread(() -> showBusyView(false));
            });

        }

    }

    private void showBusyView(boolean enable) {
        if (enable) {
            if (this.fadeView != null)
                this.fadeView.setVisibility(View.VISIBLE);

            if (this.showBusyAds()) {
                busyHandler.showAd();
                busyAdStartDisplayAt = System.currentTimeMillis();
            }
            if (this.adView != null && this.showBannerAd()) {
                this.adView.pause();
                this.adView.setVisibility(View.GONE);
            }
        } else {
            if (this.fadeView != null)
                this.fadeView.setVisibility(View.GONE);

            if (this.showBusyAds()) {
                busyHandler.hideAd();
                loadBusyAd();
            }
            if (adView != null) {
                adView.setVisibility(View.VISIBLE);
                adView.resume();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE_REQUEST_GPS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            loadGpsLocation();


        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @SuppressLint("MissingPermission")
    protected void loadGpsLocation() {
        if (unlockPermissions(Manifest.permission.ACCESS_FINE_LOCATION, CODE_REQUEST_GPS) || unlockPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, CODE_REQUEST_GPS))
            LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(this);
    }

    private void injectBannerAd(int height) {
        AdSize adsize;


        float dp = height / ((float) (this.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));


        if (dp >= 350) {
            adsize = AdSize.MEDIUM_RECTANGLE;
        } else if (dp >= 100) {
            adsize = AdSize.LARGE_BANNER;
        } else if (dp >= 50) {
            adsize = AdSize.BANNER;
        } else
            adsize = AdSize.BANNER;


        this.adView.setAdSize(adsize);
        this.adView.loadAd(AdMobRequestUtil.buildAdRequest(loc, keywords).build());

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.adView.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        RelativeLayout.LayoutParams mainLayoutParams = (RelativeLayout.LayoutParams) this.mainLayout.getLayoutParams();
        mainLayoutParams.addRule(RelativeLayout.ABOVE, this.adView.getId());

        /*ViewGroup.LayoutParams layout = this.adView.getLayoutParams();
        if (layout instanceof ViewGroup.MarginLayoutParams) {
            layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layout.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            ((ViewGroup.MarginLayoutParams) layout).leftMargin = -this.mainLayout.getPaddingLeft();
            ((ViewGroup.MarginLayoutParams) layout).rightMargin = -this.mainLayout.getPaddingRight();
            ((ViewGroup.MarginLayoutParams) layout).bottomMargin = -this.mainLayout.getPaddingBottom();
            ((ViewGroup.MarginLayoutParams) layout).topMargin = Math.max(height - adsize.getHeightInPixels(this), 0) + this.mainLayout.getPaddingBottom();
            this.adView.setLayoutParams(layout);
        }
        */

        this.adView.resume();
    }


    @Override
    public void onGlobalLayout() {

        this.mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (this.showBannerAd() && (this.mainLayout instanceof LinearLayoutCompat || this.mainLayout instanceof LinearLayout)) {
            int heightUsed = this.mainLayout.getChildAt(this.mainLayout.getChildCount() - 1).getBottom();
            injectBannerAd(this.mainLayout.getHeight() - heightUsed);
        }

    }


    @Override
    public void onSuccess(Location location) {
        AdsAppCompatActivity.loc = location;
    }

    protected abstract String getInterstitialAdUnit();

    protected abstract String getBusyAdUnit();

    protected abstract String getBannerAdUnit();

    protected abstract String getRewardAdUnit();

    protected abstract long getWaitBeforeRetryLoadAd();

    protected abstract long getBusyRefreshInterval();

    protected abstract boolean showBusyAds();

    protected abstract boolean showBannerAd();

    protected abstract void addExtras(Intent i, Bundle savedInstanceState);


}
