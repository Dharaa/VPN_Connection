package org.strongswan.android;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import static org.strongswan.android.api.AndyConstants.TAG;


public class ProfileFragment extends Fragment {


    private WebView mWebView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, null);
        mWebView = (WebView) view.findViewById(R.id.mWebView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //progDailog.show();
//                view.loadUrl(url);
                if (url == null || url.startsWith("http://") || url.startsWith("https://")) return false;

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Log.i(TAG, "shouldOverrideUrlLoading Exception:" + e);
                    return true;
                }
            }
            @Override
            public void onPageFinished(WebView view, final String url) {
                //progDailog.dismiss();
            }
        });

        mWebView.loadUrl("http://www.baidu.com");
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}