package com.codekaro.kharchatracker;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class MainActivity extends Activity {
    private WebView webView;
    private RewardedInterstitialAd rewardedInterstitialAd;

    // LIVE Ad Unit ID (Rewarded Interstitial)
    private static final String AD_UNIT_ID = "ca-app-pub-4226067070748005/1478973868";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen — status bar hide
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Immersive mode
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // AdMob init
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus status) {}
        });
        loadRewardedInterstitialAd();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);    // localStorage support
        s.setAllowFileAccess(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        // JS <-> Native bridge for ads. In HTML/JS call: AndroidAd.showAd();
        webView.addJavascriptInterface(new AdBridge(), "AndroidAd");

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void loadRewardedInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(this, AD_UNIT_ID, adRequest,
            new RewardedInterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedInterstitialAd ad) {
                    rewardedInterstitialAd = ad;
                    rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            rewardedInterstitialAd = null;
                            loadRewardedInterstitialAd(); // preload next ad
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                            rewardedInterstitialAd = null;
                            loadRewardedInterstitialAd();
                        }
                    });
                }
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    rewardedInterstitialAd = null;
                }
            });
    }

    // Called from JavaScript (index.html) via AndroidAd.showAd()
    private class AdBridge {
        @JavascriptInterface
        public void showAd() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (rewardedInterstitialAd != null) {
                        rewardedInterstitialAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                            @Override
                            public void onUserEarnedReward(RewardItem rewardItem) {
                                // Reward earned — app me abhi koi reward logic nahi hai, bas ad dikhani hai
                            }
                        });
                    }
                    // agar ad load nahi hui thi to chup-chaap skip, app kabhi block nahi hogi
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
