package com.tomatedigital.adinjector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
import androidx.appcompat.app.AlertDialog;
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

import java.util.HashSet;
import java.util.Set;


@SuppressWarnings("SameReturnValue")
public abstract class AdsAppCompatActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener, OnSuccessListener<Location> {

    private static final int CODE_REQUEST_GPS = 25471;

    private static final long MINIMUM_BUSY_TIME = 5500;
    private static final long MAX_LOAD_AD_VIDEO_DURATION = 8000;


    private static Location loc;


    private ViewGroup mainLayout;
    private RelativeLayout fadeView;

    private AdView adView;


    private ResizableBannerAdHandler busyHandler;
    private long busyAdStartDisplayAt;


    @SuppressLint("StaticFieldLeak")
    private static RewardAdHandler rewardHandler;

    protected ViewGroup getMainLayout() {
        return mainLayout;
    }


    @NonNull
    private static String[] keywords = new String[0];


    private static Set<String> requestingPermissions = new HashSet<>();

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

        if (this.busyHandler != null)
            this.busyHandler.destroy(this);

        super.onDestroy();
    }


    @Override
    public void onPause() {

        if (this.adView != null)
            this.adView.pause();
        if (this.busyHandler != null)
            this.busyHandler.pause(this);

        if (rewardHandler != null)
            rewardHandler.pause(this);

        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (this.adView != null)
            this.adView.resume();

        if (this.busyHandler != null)
            this.busyHandler.resume(this);

        if (rewardHandler != null)
            rewardHandler.resume(this);
    }


    public boolean unlockPermissions(@NonNull final String permission, final int requestorCode, @Nullable final String explanationDialog) {


        if (Build.VERSION.SDK_INT > 18 && ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            final String pref = "ask_" + permission.substring(Math.max(0, permission.lastIndexOf(".")));
            final boolean alreadyAsked = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(pref, false);
            final boolean tmp = requestingPermissions.add(permission) || requestingPermissions.add(requestorCode + "");

            if (!alreadyAsked) //todo this if can be removed and the statement put inside the next if 25/01
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(pref, true).apply();

            if (Build.VERSION.SDK_INT > 23 && alreadyAsked && tmp && explanationDialog != null && isValid()) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(this).setTitle(R.string.permission_necessary_to_continue).setMessage(explanationDialog).setCancelable(false).setPositiveButton(R.string.go_to_settings, (d, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, requestorCode);
                        requestingPermissions.remove(permission);

                    }).show();
                });
            } else if (tmp && !isFinishing() && isDestroyed()) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestorCode);
                requestingPermissions.remove(permission);
            }
            return false;
        }

        return true;

    }

    public boolean isValid() {
        return !this.isFinishing() && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && this.isDestroyed());
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        preInjectStuff();

        if (loc == null)
            loadGpsLocation();

        createRewardHandler();

        super.onCreate(savedInstanceState);
    }

    private void preInjectStuff() {


        //create a new relative layout and inject at the main container
        ViewGroup rootView = this.findViewById(android.R.id.content);
        this.mainLayout = new RelativeLayout(this);


        rootView.addView(this.mainLayout);
        ViewGroup.LayoutParams layout = this.mainLayout.getLayoutParams();
        layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layout.height = ViewGroup.LayoutParams.MATCH_PARENT;
        this.mainLayout.setLayoutParams(layout);


        //add the fade view
        createBannerAd(this.mainLayout);

        this.fadeView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.busy_layout, this.mainLayout, false);

        this.mainLayout.addView(this.fadeView);
        layout = this.fadeView.getLayoutParams();
        layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layout.height = ViewGroup.LayoutParams.MATCH_PARENT;
        this.fadeView.setLayoutParams(layout);
        this.fadeView.setVisibility(View.GONE);


        injectBusyAd(this.fadeView);
    }

    protected void setExtraContent() {


        //remove the main layout and add it to the new relative layout
        ViewGroup rootView = this.findViewById(android.R.id.content);
        View tmp = rootView.getChildAt(0);
        rootView.removeViewAt(0);

        rootView.addView(this.mainLayout);
        this.mainLayout.addView(tmp, 0);
        this.mainLayout = (ViewGroup) tmp;

        if (this.showBannerAd() && (this.mainLayout instanceof LinearLayoutCompat || this.mainLayout instanceof LinearLayout))
            this.mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);

    }


    private void createRewardHandler() {
        if (rewardHandler == null)
            rewardHandler = new RewardAdHandler(this, this.getRewardAdUnit(), this.getInterstitialAdUnit(), this.getWaitBeforeRetryLoadAd(), keywords);
    }


    private void injectBusyAd(@NonNull final RelativeLayout relativeLayout) {

        //todo   if (this.showBusyAds()) {
        AdView busyAd = new AdView(this);
        busyAd.setAdSize(AdSize.MEDIUM_RECTANGLE);
        busyAd.setAdUnitId(getBusyAdUnit());
        busyAd.setId(R.id.busyAd);

        relativeLayout.addView(busyAd);
        RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) busyAd.getLayoutParams();
        layout.addRule(RelativeLayout.CENTER_IN_PARENT);
        busyHandler = new ResizableBannerAdHandler(busyAd, this.getBusyRefreshInterval(), keywords);

        busyHandler.hideAd();
        //}
    }


    private void createBannerAd(@NonNull ViewGroup container) {
        //if (this.showBannerAd()) {
        this.adView = new AdView(this);
        this.adView.setId(R.id.adview);
        this.adView.setAdUnitId(this.getBannerAdUnit());
        container.addView(this.adView);
        // } else
        if (!this.showBannerAd())
            this.adView.setVisibility(View.GONE);
        //      this.adView = findViewById(R.id.adview);


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

            if (this.showBusyAds())
                busyHandler.hideAd();


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


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @SuppressLint("MissingPermission")
    protected void loadGpsLocation() {
        if (unlockPermissions(Manifest.permission.ACCESS_FINE_LOCATION, CODE_REQUEST_GPS, null) || unlockPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, CODE_REQUEST_GPS, null))
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
