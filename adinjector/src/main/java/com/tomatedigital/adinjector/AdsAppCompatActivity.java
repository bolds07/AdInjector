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
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.tomatedigital.adinjector.handler.IntertitialAdHandler;
import com.tomatedigital.adinjector.handler.ResizableBannerAdHandler;
import com.tomatedigital.adinjector.handler.RewardAdHandler;
import com.tomatedigital.adinjector.handler.ShowableAdHandler;
import com.tomatedigital.adinjector.listener.GenericAdListener;
import com.tomatedigital.adinjector.listener.RewardAdListener;

import java.util.HashSet;
import java.util.Set;


@SuppressWarnings("SameReturnValue")
public abstract class AdsAppCompatActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener, OnSuccessListener<Location> {

    private static final int CODE_REQUEST_GPS = 25471;


    private static Location loc;


    private ViewGroup mainLayout;
    private RelativeLayout fadeView;

    private AdView adView;


    @SuppressLint("StaticFieldLeak")
    private static ResizableBannerAdHandler busyHandler;
    private long busyAdStartDisplayAt;


    private static RewardAdHandler rewardVideoHandler;
    private static IntertitialAdHandler intertitialAdHandler;

    private boolean bannerAdOn;
    private boolean busyAdOn;

    protected ViewGroup getMainLayout() {
        return mainLayout;
    }


    @NonNull
    private static String[] keywords = new String[0];


    private static final Set<String> requestingPermissions = new HashSet<>();


    protected static void setKeywords(@NonNull String[] keys) {
        keywords = keys;
    }

    public static Location getLoc() {
        return loc;
    }

    @Nullable
    private ShowableAdHandler getShowableAdHandler(@Nullable final GenericAdListener.AdType preference) {
        ShowableAdHandler handler = null;

        if (showIntertitialAd() && intertitialAdHandler.isAdReady() && (preference != GenericAdListener.AdType.REWARD_VIDEO || (System.currentTimeMillis() - rewardVideoHandler.getLastRewardTimestamp() < this.minRewardVideoInterval())))
            handler = intertitialAdHandler;
        else if (showRewardedVideoAd())
            handler = rewardVideoHandler;


        return handler;
    }

    public boolean showAdOrMiss(@NonNull final RewardAdListener.RewardListener rewardListener, @Nullable final RewardItem reward, @Nullable GenericAdListener.AdType preference) {

        ShowableAdHandler handler = getShowableAdHandler(preference);

        if (handler == null || !handler.isAdReady()) {
            rewardListener.onRewarded(reward);
            return false;
        } else if (!BuildConfig.DEBUG)
            handler.showAd(rewardListener);
        else {
            Toast.makeText(this, "show ad type: " + handler.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            rewardListener.onRewarded(reward);
        }
        return true;


    }


    /**
     * dont show ad case has no ads
     *
     * @param rewardListener
     * @param reward
     * @param preference
     */
    public void showAdOrLoad(@NonNull final RewardAdListener.RewardListener rewardListener, @Nullable final RewardItem reward, @Nullable GenericAdListener.AdType preference) {

        ShowableAdHandler handler = getShowableAdHandler(preference);

        if (handler == null)
            rewardListener.onRewarded(reward);

        else if (handler.isAdReady()) {
            if (BuildConfig.DEBUG) {
                Toast.makeText(this, "show ad type: " + handler.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                rewardListener.onRewarded(reward);
            } else
                handler.showAd(rewardListener);
        } else {
            busy(true);
            final long startTime = System.currentTimeMillis();
            handler.loadAd();

            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                while (!handler.isAdReady() && System.currentTimeMillis() - startTime < maxLoadVideoAdDuration()) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                runOnUiThread(() -> {
                    busy(false);
                    try {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this, "show ad type: " + handler.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                            rewardListener.onRewarded(reward);
                        } else
                            showAdOrMiss(rewardListener, reward, preference);
                    } catch (Exception e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                });

            });
        }

    }

    @Override
    public void onBackPressed() {

        showAdOrMiss(new OnBackPressededReward(), null, GenericAdListener.AdType.INTERSTITIAL);

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);


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

        /*if (this.busyHandler != null)
            this.busyHandler.destroy(this);
        */
        super.onDestroy();
    }


    @Override
    public void onPause() {

        if (this.adView != null)
            this.adView.pause();
        if (busyHandler != null)
            busyHandler.pause(this);

        if (rewardVideoHandler != null)
            rewardVideoHandler.pause(this);

        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (this.adView != null)
            this.adView.resume();

        if (busyHandler != null)
            busyHandler.resume(this);

        if (rewardVideoHandler != null)
            rewardVideoHandler.resume(this);
    }


    public boolean unlockPermissions(@NonNull final String permission, final int requestorCode, @Nullable final String explanationDialog) {


        if (Build.VERSION.SDK_INT > 18 && ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            final String pref = "ask_" + permission.substring(Math.max(0, permission.lastIndexOf(".")));
            final boolean alreadyAsked = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(pref, false);
            final boolean tmp = requestingPermissions.add(permission) || requestingPermissions.add(requestorCode + "");


            if (Build.VERSION.SDK_INT > 23 && alreadyAsked && tmp && explanationDialog != null && isValid()) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(this).setTitle(R.string.permission_required).setMessage(explanationDialog).setCancelable(false).setPositiveButton(R.string.go_to_settings, (d, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, requestorCode);
                        requestingPermissions.remove(permission);
                    }).show();
                });
            } else if (tmp && !isFinishing() && !isDestroyed()) {
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(pref, true).apply();
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestorCode);
                requestingPermissions.remove(permission);
            }
            return false;
        }


        return true;

    }

    public boolean isValid() {
        return !this.isFinishing();
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

        this.bannerAdOn = this.showBannerAd();
        this.busyAdOn = this.showBusyAds();

        preInjectStuff();

        if (loc == null)
            loadGpsLocation();

        this.createHandlers();

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

        if (this.bannerAdOn && (this.mainLayout instanceof LinearLayoutCompat || this.mainLayout instanceof LinearLayout))
            this.mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);

    }


    private void createHandlers() {
        if (rewardVideoHandler == null && showRewardedVideoAd())
            rewardVideoHandler = new RewardAdHandler(this, this.getRewardAdUnit(), this.getAdUserId(), waitBeforeRetryLoadAd(), keywords);

        if (intertitialAdHandler == null && showIntertitialAd())
            intertitialAdHandler = new IntertitialAdHandler(this, getInterstitialAdUnit(), this.getAdUserId(), waitBeforeRetryLoadAd(), keywords);
    }


    private void injectBusyAd(@NonNull final RelativeLayout relativeLayout) {

        if (busyHandler == null) {
            AdView busyAd = new AdView(this);
            busyAd.setAdSize(AdSize.MEDIUM_RECTANGLE);
            busyAd.setAdUnitId(getBusyAdUnit());
            busyAd.setId(R.id.busyAd);

            relativeLayout.addView(busyAd);
            RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) busyAd.getLayoutParams();
            layout.addRule(RelativeLayout.CENTER_IN_PARENT);


            busyHandler = new ResizableBannerAdHandler(busyAd, this.getAdUserId(), this.minBusyRefreshInterval(), this.minBusyShowTimesBeforeRefresh(), this.waitBeforeRetryLoadAd(), keywords);
        } else
            busyHandler.changeContainer(relativeLayout, this);

        busyHandler.hideAd();

    }


    private void createBannerAd(@NonNull ViewGroup container) {
        if (this.bannerAdOn) {
            this.adView = new AdView(this);
            this.adView.setId(R.id.adview);
            this.adView.setAdUnitId(this.getBannerAdUnit());
            container.addView(this.adView);
        } else
            this.adView = findViewById(R.id.adview);


    }


    @MainThread
    public void busy(boolean b) {

        if (b || !this.busyAdOn) {
            showBusyView(b);

        } else {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                long waitTime = getBusyDefaultDuration() - System.currentTimeMillis() - this.busyAdStartDisplayAt;
                if (waitTime > 0)
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ignored) {
                    }

                runOnUiThread(() -> showBusyView(false));
            });
        }


    }

    private void showBusyView(boolean enable) {
        if (enable) {
            if (this.fadeView != null)
                this.fadeView.setVisibility(View.VISIBLE);

            if (this.busyAdOn)
                busyHandler.showAd(this);

            this.busyAdStartDisplayAt = System.currentTimeMillis();

        } else {
            if (this.fadeView != null)
                this.fadeView.setVisibility(View.GONE);

            if (this.busyAdOn)
                AdsAppCompatActivity.busyHandler.hideAd();


            //todo testando
            AdsAppCompatActivity.busyHandler.loadAd(this);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CODE_REQUEST_GPS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            loadGpsLocation();


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @SuppressLint("MissingPermission")
    protected void loadGpsLocation() {

        if (unlockPermissions(Manifest.permission.ACCESS_FINE_LOCATION, CODE_REQUEST_GPS, null) || unlockPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, CODE_REQUEST_GPS, null)) {
            LocationRequest tmp = LocationRequest.create();
            tmp.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            tmp.setInterval(0L);
            tmp.setFastestInterval(0L);
            tmp.setExpirationDuration(30000L);
            LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationToken() {
                @Override
                public boolean isCancellationRequested() {
                    return false;
                }

                @NonNull
                @Override
                public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                    return this;
                }
            }).addOnSuccessListener(this);

//            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(tmp, new LocationCallback() {
//                @Override
//                public void onLocationResult(@NonNull LocationResult locationResult) {
//                    System.out.println("b");
//                    onSuccess(locationResult.getLastLocation());
//                    LocationServices.getFusedLocationProviderClient(AdsAppCompatActivity.this).removeLocationUpdates(this);
//                }
//            }, getMainLooper());

            LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(this);
        }
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
        if (this.bannerAdOn && (this.mainLayout instanceof LinearLayoutCompat || this.mainLayout instanceof LinearLayout)) {
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

    protected abstract int minBusyShowTimesBeforeRefresh();

    protected abstract long minBusyRefreshInterval();

    protected abstract boolean showBusyAds();

    protected abstract boolean showBannerAd();

    protected abstract long waitBeforeRetryLoadAd();

    protected abstract long maxLoadVideoAdDuration();

    protected abstract long minRewardVideoInterval();

    protected abstract boolean showIntertitialAd();

    protected abstract boolean showRewardedVideoAd();

    protected abstract String getAdUserId();

    protected abstract long getBusyDefaultDuration();


    protected class OnBackPressededReward extends RewardAdListener.AlwaysAcceptRewardListener {

        @Override
        public void onRewarded(RewardItem reward) {
            finish();
        }
    }

}
