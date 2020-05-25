/*
 * Copyright (C) 2012 Tobias Brunner
 * Copyright (C) 2012 Giuliano Grassi
 * Copyright (C) 2012 Ralf Sager
 * Hochschule fuer Technik Rapperswil
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.  See <http://www.fsf.org/copyleft/gpl.txt>.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 */

package org.strongswan.android.ui;

import org.strongswan.android.data.CertificateProvider;
import org.strongswan.android.data.LogContentProvider;
import org.strongswan.android.logic.TrustedCertificateManager;
import org.strongswan.android.logic.TrustedCertificateManager.TrustedCertificateSource;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.AsyncTaskLoader;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.security.KeyChain;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.strongswan.android.HirechyFragment;
import org.strongswan.android.R;
import org.strongswan.android.VpnFragment;
import org.strongswan.android.api.APIClient;
import org.strongswan.android.api.APIInterface;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.data.VpnType.VpnTypeFeature;
import org.strongswan.android.logic.CharonVpnService;
import org.strongswan.android.logic.TrustedCertificateManager;
import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.logic.VpnStateService.State;
import org.strongswan.android.model.country.CountryResponse;
import org.strongswan.android.security.TrustedCertificateEntry;
import org.strongswan.android.utils.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.skyhacker2.sqliteonweb.SQLiteOnWeb;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity implements VpnFragment.OnVpnProfileSelectedListener,
        HirechyFragment.OnVpnProfileSelectedListener {

    public static final String SELECT_CERTIFICATE = "org.strongswan.android.action.SELECT_CERTIFICATE";
    public static final String CONTACT_EMAIL = "android@strongswan.org";
    public static final String START_PROFILE = "org.strongswan.android.action.START_PROFILE";
    public static final String EXTRA_VPN_PROFILE_ID = "org.strongswan.android.VPN_PROFILE_ID";
    /**
     * Use "bring your own device" (BYOD) features
     */
    public static final boolean USE_BYOD = true;
    private static final int PREPARE_VPN_SERVICE = 0;
    private static final int SELECT_TRUSTED_CERTIFICATE = 1;
    private static final String PROFILE_NAME = "org.strongswan.android.MainActivity.PROFILE_NAME";
    private static final String PROFILE_REQUIRES_PASSWORD = "org.strongswan.android.MainActivity.REQUIRES_PASSWORD";
    private static final String PROFILE_RECONNECT = "org.strongswan.android.MainActivity.RECONNECT";
    private static final String DIALOG_TAG = "Dialog";

    private ImageView mIvProfile, mIvVpn, mIvInfo, mIvHiracy;
    private TextView mTvProfile, mTvVpn, mTvInfo, mTvHiracy;
    private LinearLayout mLLProfile, mLLInfo, mLLHiracy, mLLVpn;
    private FrameLayout mFmVpn, mFmHirachy, mFmInfo, mFmProfile;
    private Bundle mProfileInfo, profileInfo;
    private VpnProfile profile;
    private VpnStateService mService;
    private ActionBar bar;
    private APIInterface apiInterface;
    private X509Certificate certificate;


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((VpnStateService.LocalBinder) service).getService();

            if (START_PROFILE.equals(getIntent().getAction())) {
                startVpnProfile(getIntent());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SQLiteOnWeb.init(this).start();

        setContentView(R.layout.main);
        getCountryList();

        initialize();
        this.bindService(new Intent(this, VpnStateService.class),
                mServiceConnection, Service.BIND_AUTO_CREATE);

        bar = getActionBar();
        bar.setDisplayShowHomeEnabled(false);
        bar.setTitle(Html.fromHtml("<font color='#ffffff'>VPN </font>"));

        /* load CA certificates in a background task */
        new LoadCertificatesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void initialize() {
        mFmVpn = (FrameLayout) findViewById(R.id.mFmVpn);
        mFmHirachy = (FrameLayout) findViewById(R.id.mFmHirachy);
        mFmInfo = (FrameLayout) findViewById(R.id.mFmInfo);
        mFmProfile = (FrameLayout) findViewById(R.id.mFmProfile);
        mIvVpn = (ImageView) findViewById(R.id.mIvVpn);
        mIvInfo = (ImageView) findViewById(R.id.mIvInfo);
        mIvProfile = (ImageView) findViewById(R.id.mIvProfile);
        mIvHiracy = (ImageView) findViewById(R.id.mIvHiracy);
        mTvHiracy = (TextView) findViewById(R.id.mTvHiracy);
        mTvInfo = (TextView) findViewById(R.id.mTvInfo);
        mTvProfile = (TextView) findViewById(R.id.mTvProfile);
        mTvVpn = (TextView) findViewById(R.id.mTvVpn);
        mLLHiracy = (LinearLayout) findViewById(R.id.mLLHiracy);
        mLLInfo = (LinearLayout) findViewById(R.id.mLLInfo);
        mLLProfile = (LinearLayout) findViewById(R.id.mLLProfile);
        mLLVpn = (LinearLayout) findViewById(R.id.mLLVpn);

        mLLVpn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bar.setTitle(Html.fromHtml("<font color='#ffffff'>VPN </font>"));
                mFmVpn.setVisibility(View.VISIBLE);
                mFmHirachy.setVisibility(View.GONE);
                mFmInfo.setVisibility(View.GONE);
                mFmProfile.setVisibility(View.GONE);
                mTvHiracy.setVisibility(View.GONE);
                mTvInfo.setVisibility(View.GONE);
                mTvProfile.setVisibility(View.GONE);
                mTvVpn.setVisibility(View.VISIBLE);
                mIvVpn.setColorFilter(getResources().getColor(R.color.colorPrimary));
                mIvHiracy.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvInfo.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvProfile.setColorFilter(getResources().getColor(R.color.panel_separator));
            }
        });

        mLLHiracy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bar.setTitle(Html.fromHtml("<font color='#ffffff'>Hierarchy</font>"));
                mFmVpn.setVisibility(View.GONE);
                mFmHirachy.setVisibility(View.VISIBLE);
                mFmInfo.setVisibility(View.GONE);
                mFmProfile.setVisibility(View.GONE);
                mTvHiracy.setVisibility(View.VISIBLE);
                mTvInfo.setVisibility(View.GONE);
                mTvProfile.setVisibility(View.GONE);
                mTvVpn.setVisibility(View.GONE);
                mIvVpn.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvHiracy.setColorFilter(getResources().getColor(R.color.colorPrimary));
                mIvInfo.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvProfile.setColorFilter(getResources().getColor(R.color.panel_separator));
            }
        });

        mLLInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bar.setTitle(Html.fromHtml("<font color='#ffffff'>Information</font>"));
                mFmVpn.setVisibility(View.GONE);
                mFmHirachy.setVisibility(View.GONE);
                mFmInfo.setVisibility(View.VISIBLE);
                mFmProfile.setVisibility(View.GONE);
                mTvHiracy.setVisibility(View.GONE);
                mTvInfo.setVisibility(View.VISIBLE);
                mTvProfile.setVisibility(View.GONE);
                mTvVpn.setVisibility(View.GONE);
                mIvVpn.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvHiracy.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvInfo.setColorFilter(getResources().getColor(R.color.colorPrimary));
                mIvProfile.setColorFilter(getResources().getColor(R.color.panel_separator));
            }
        });

        mLLProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bar.setTitle(Html.fromHtml("<font color='#ffffff'>Profile</font>"));
                mFmVpn.setVisibility(View.GONE);
                mFmHirachy.setVisibility(View.GONE);
                mFmInfo.setVisibility(View.GONE);
                mFmProfile.setVisibility(View.VISIBLE);
                mTvHiracy.setVisibility(View.GONE);
                mTvInfo.setVisibility(View.GONE);
                mTvProfile.setVisibility(View.VISIBLE);
                mTvVpn.setVisibility(View.GONE);
                mIvVpn.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvHiracy.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvInfo.setColorFilter(getResources().getColor(R.color.panel_separator));
                mIvProfile.setColorFilter(getResources().getColor(R.color.colorPrimary));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            this.unbindService(mServiceConnection);
        }
    }

    /**
     * Due to launchMode=singleTop this is called if the Activity already exists
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (START_PROFILE.equals(intent.getAction())) {
            startVpnProfile(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_manage_certs:
                Intent certIntent = new Intent(this, TrustedCertificatesActivity.class);
                startActivity(certIntent);
                return true;
            case R.id.menu_show_log:
                Intent logIntent = new Intent(this, LogActivity.class);
                startActivity(logIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Prepare the VpnService. If this succeeds the current VPN profile is
     * started.
     *
     * @param profileInfo a bundle containing the information about the profile to be started
     */
    protected void prepareVpnService(Bundle profileInfo) {
        Intent intent;
        try {
            intent = VpnService.prepare(this);
        } catch (IllegalStateException ex) {
            /* this happens if the always-on VPN feature (Android 4.2+) is activated */
            VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported_during_lockdown);
            return;
        }
        /* store profile info until the user grants us permission */
        mProfileInfo = profileInfo;
        if (intent != null) {
            try {
                startActivityForResult(intent, PREPARE_VPN_SERVICE);
            } catch (ActivityNotFoundException ex) {
                /* it seems some devices, even though they come with Android 4,
                 * don't have the VPN components built into the system image.
                 * com.android.vpndialogs/com.android.vpndialogs.ConfirmDialog
                 * will not be found then */
                VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported);
            }
        } else {    /* user already granted permission to use VpnService */
            onActivityResult(PREPARE_VPN_SERVICE, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PREPARE_VPN_SERVICE:
                if (resultCode == RESULT_OK && mProfileInfo != null) {
                    Intent intent = new Intent(this, CharonVpnService.class);
                    intent.putExtras(mProfileInfo);
                    this.startService(intent);
                }
                break;
            case 5:
                if (mService != null && mService.getState() == State.CONNECTED) {
                    profileInfo.putBoolean(PROFILE_RECONNECT, mService.getProfile().getId() == profile.getId());

                    ConfirmationDialog dialog = new ConfirmationDialog();
                    dialog.setArguments(profileInfo);
                    dialog.show(this.getFragmentManager(), DIALOG_TAG);
                    return;
                }
                startVpnProfile(profileInfo);
                break;
            case SELECT_TRUSTED_CERTIFICATE:
                if (resultCode == RESULT_OK) {
//                    String alias = data.getStringExtra(VpnProfileDataSource.KEY_CERTIFICATE);
//                    X509Certificate certificate = TrustedCertificateManager.getInstance().getCACertificateFromAlias(alias);
//                    TrustedCertificateEntry mCertEntry = certificate == null ? null : new TrustedCertificateEntry(alias, certificate);
//                    String certAlias = mCertEntry.getAlias();
//                    profile.setCertificateAlias(certAlias);
                    //mCertEntry = certificate == null ? null : new TrustedCertificateEntry(alias, certificate);
                    //updateCertificateSelector();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onVpnProfileSelected(VpnProfile profile) {
        this.profile = profile;
        profileInfo = new Bundle();
        profileInfo.putLong(VpnProfileDataSource.KEY_ID, profile.getId());
        profileInfo.putString(VpnProfileDataSource.KEY_USERNAME, profile.getUsername());
        profileInfo.putString(VpnProfileDataSource.KEY_PASSWORD, profile.getPassword());
        profileInfo.putBoolean(PROFILE_REQUIRES_PASSWORD, profile.getVpnType().has(VpnTypeFeature.USER_PASS));
        profileInfo.putString(PROFILE_NAME, profile.getName());


        downloadFile("http://167.179.94.170/macpanel/157.245.cer");

//        Intent intent = new Intent(this, TrustedCertificateImportActivity.class);
//        startActivityForResult(intent, 0);

       /* Intent intent = new Intent(MainActivity.this, TrustedCertificatesActivity.class);
        intent.setAction(TrustedCertificatesActivity.SELECT_CERTIFICATE);
        startActivityForResult(intent, SELECT_TRUSTED_CERTIFICATE);*/
        // removeFragmentByTag(DIALOG_TAG);

        /*String x509cert = "-----BEGIN CERTIFICATE-----\n" +
                *//*"MIICrjCCAhegAwIBAgIJAO9T3E+oW38mMA0GCSqGSIb3DQEBCwUAMHAxCzAJBgNV\n" +
                "BAYTAlVaMREwDwYDVQQHDAhUYXNoa2VudDENMAsGA1UECgwERWZpcjEQMA4GA1UE\n" +
                "CwwHSVQgZGVwdDEQMA4GA1UEAwwHZWZpci51ejEbMBkGCSqGSIb3DQEJARYMaG9z\n" +
                "dEBlZmlyLnV6MB4XDTE2MTExMDA4MjIzMFoXDTE2MTIxMDA4MjIzMFowcDELMAkG\n" +
                "A1UEBhMCVVoxETAPBgNVBAcMCFRhc2hrZW50MQ0wCwYDVQQKDARFZmlyMRAwDgYD\n" +
                "VQQLDAdJVCBkZXB0MRAwDgYDVQQDDAdlZmlyLnV6MRswGQYJKoZIhvcNAQkBFgxo\n" +
                "b3N0QGVmaXIudXowgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAL60mG0Gpl7s\n" +
                "3qMnZcURB1xk5Qen6FN0+AJB5Z/WHA50n1MUkXNY28rkEYupkxpfEqR+/gXgBUAm\n" +
                "FACA3GSdoHMMY1kdeAzxsYbBEbtGKHICF/QFGTqScWmI6uBUwzsLDLv1ELef/zEY\n" +
                "Ru/krXtNh8ZNYyfwVKyZaB9+3M2yOqATAgMBAAGjUDBOMB0GA1UdDgQWBBS1nH3O\n" +
                "ecLDrIZLZ/f1WsNL/xtuEzAfBgNVHSMEGDAWgBS1nH3OecLDrIZLZ/f1WsNL/xtu\n" +
                "EzAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4GBAGzjJnXODvF9UHBKHAUF\n" +
                "kzisr78Og5BrKyAgdnjH196Jg4MO7RNJdQAmuAIk9aBB/jvAiznhhbcD3mYImH+h\n" +
                "F0Scewk5m736ydGhkcUpmxA5ye1hajjs9V7PQD2O4a8rNJSlJjiWRWSqxTfH79Ns\n" +
                "B7x2HND9LU/iz02ugGJ8vwg8\n" +*//*
                profile.getCertificateAlias()+
                "-----END CERTIFICATE-----\n";
        Intent intent = KeyChain.createInstallIntent();
        intent.putExtra(KeyChain.EXTRA_CERTIFICATE, x509cert.getBytes());
        startActivityForResult(intent, 5);*/

    }

    /**
     * Start the given VPN profile asking the user for a password if required.
     *
     * @param profileInfo data about the profile
     */
    private void startVpnProfile(Bundle profileInfo) {
        if (profileInfo.getBoolean(PROFILE_REQUIRES_PASSWORD) &&
                profileInfo.getString(VpnProfileDataSource.KEY_PASSWORD) == null) {
            LoginDialog login = new LoginDialog();
            login.setArguments(profileInfo);
            login.show(getFragmentManager(), DIALOG_TAG);
            return;
        }
        prepareVpnService(profileInfo);
    }

    /**
     * Start the VPN profile referred to by the given intent. Displays an error
     * if the profile doesn't exist.
     *
     * @param intent Intent that caused us to start this
     */
    private void startVpnProfile(Intent intent) {
        long profileId = intent.getLongExtra(EXTRA_VPN_PROFILE_ID, 0);
        if (profileId <= 0) {    /* invalid invocation */
            return;
        }
        VpnProfileDataSource dataSource = new VpnProfileDataSource(this);
        dataSource.open();
        VpnProfile profile = dataSource.getVpnProfile(profileId);
        dataSource.close();

        if (profile != null) {
            onVpnProfileSelected(profile);
        } else {
            Toast.makeText(this, R.string.profile_not_found, Toast.LENGTH_LONG).show();
        }
    }

    /*@Override
    public Loader<List<TrustedCertificateEntry>> onCreateLoader(int i, Bundle bundle) {
        return new CertificateListLoader(MainActivity.this, mSource);
    }

    @Override
    public void onLoadFinished(Loader<List<TrustedCertificateEntry>> loader, List<TrustedCertificateEntry> trustedCertificateEntries) {

    }

    @Override
    public void onLoaderReset(Loader<List<TrustedCertificateEntry>> loader) {

    }
*/

    /**
     * Class that loads the cached CA certificates.
     */
    private class LoadCertificatesTask extends AsyncTask<Void, Void, TrustedCertificateManager> {
        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected TrustedCertificateManager doInBackground(Void... params) {
            return TrustedCertificateManager.getInstance().load();
        }

        @Override
        protected void onPostExecute(TrustedCertificateManager result) {
            Hashtable<String, X509Certificate> certificateHashtable = result.getAllCACertificates();
            certificateHashtable.get("alias");
            setProgressBarIndeterminateVisibility(false);
        }
    }

    /**
     * Dismiss dialog if shown
     */
    public void removeFragmentByTag(String tag) {
        FragmentManager fm = getFragmentManager();
        Fragment login = fm.findFragmentByTag(tag);
        if (login != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(login);
            ft.commit();
        }
    }

    /**
     * Class that displays a confirmation dialog if a VPN profile is already connected
     * and then initiates the selected VPN profile if the user confirms the dialog.
     */
    public static class ConfirmationDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle profileInfo = getArguments();
            int icon = android.R.drawable.ic_dialog_alert;
            int title = R.string.connect_profile_question;
            int message = R.string.replaces_active_connection;
            int button = R.string.connect;

            if (profileInfo.getBoolean(PROFILE_RECONNECT)) {
                icon = android.R.drawable.ic_dialog_info;
                title = R.string.vpn_connected;
                message = R.string.vpn_profile_connected;
                button = R.string.reconnect;
            }

            return new AlertDialog.Builder(getActivity())
                    .setIcon(icon)
                    .setTitle(String.format(getString(title), profileInfo.getString(PROFILE_NAME)))
                    .setMessage(message)
                    .setPositiveButton(button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            MainActivity activity = (MainActivity) getActivity();
                            activity.startVpnProfile(profileInfo);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    }).create();
        }
    }

    /**
     * Class that displays a login dialog and initiates the selected VPN
     * profile if the user confirms the dialog.
     */
    public static class LoginDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle profileInfo = getArguments();
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.login_dialog, null);
            EditText username = (EditText) view.findViewById(R.id.username);
            username.setText(profileInfo.getString(VpnProfileDataSource.KEY_USERNAME));
            final EditText password = (EditText) view.findViewById(R.id.password);

            Builder adb = new AlertDialog.Builder(getActivity());
            adb.setView(view);
            adb.setTitle(getString(R.string.login_title));
            adb.setPositiveButton(R.string.login_confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    MainActivity activity = (MainActivity) getActivity();
                    profileInfo.putString(VpnProfileDataSource.KEY_PASSWORD, password.getText().toString().trim());
                    activity.prepareVpnService(profileInfo);
                }
            });
            adb.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
            return adb.create();
        }
    }

    /**
     * Class representing an error message which is displayed if VpnService is
     * not supported on the current device.
     */
    public static class VpnNotSupportedError extends DialogFragment {
        static final String ERROR_MESSAGE_ID = "org.strongswan.android.VpnNotSupportedError.MessageId";

        public static void showWithMessage(Activity activity, int messageId) {
            Bundle bundle = new Bundle();
            bundle.putInt(ERROR_MESSAGE_ID, messageId);
            VpnNotSupportedError dialog = new VpnNotSupportedError();
            dialog.setArguments(bundle);
            dialog.show(activity.getFragmentManager(), DIALOG_TAG);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle arguments = getArguments();
            final int messageId = arguments.getInt(ERROR_MESSAGE_ID);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.vpn_not_supported_title)
                    .setMessage(messageId)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    }).create();
        }
    }

    private void getCountryList() {
        apiInterface = APIClient.getClient().create(APIInterface.class);
        if (Utils.isNetworkAvailable(this)) {
            Call<CountryResponse> countryResponseCall = apiInterface.getCountryList();
            countryResponseCall.enqueue(new Callback<CountryResponse>() {
                @Override
                public void onResponse(Call<CountryResponse> call, Response<CountryResponse> response) {
                    if (response.body() != null && response.body().getSuccess().equalsIgnoreCase("true")) {
                        Utils.mCountryResponseList.clear();
                        Utils.mCountryResponseList.addAll(response.body().getResult());
                        VpnProfileDataSource mDataSource = new VpnProfileDataSource(MainActivity.this);
                        mDataSource.open();
                        for (org.strongswan.android.model.country.ResultItem resultItem : Utils.mCountryResponseList) {
                            VpnProfile profile = new VpnProfile();
//                            profile.setId(Long.valueOf(resultItem.getCountryId()));
                            profile.setmCountryId(resultItem.getCountryId());
                            profile.setName(resultItem.getName());
                            profile.setGateway(resultItem.getIp());
                            profile.setVpnType(VpnType.IKEV2_EAP);
                            profile.setUsername(resultItem.getUsername());
                            profile.setPassword(resultItem.getPassword());
                            //profile.setCertificateAlias(resultItem.getCertificate());
                            profile.setUserCertificateAlias(null);
                            mDataSource.insertProfile(profile);
                        }
                    }
                }

                @Override
                public void onFailure(Call<CountryResponse> call, Throwable t) {

                }
            });
        }
    }

    public void downloadFile(String certificateUrl) {
        DownloadManager.Request request1 = new DownloadManager.Request(Uri.parse(certificateUrl));
//        request1.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE, DownloadManager.Request.NETWORK_WIFI);
        request1.setDescription("Sample Music File");   //appears the same in Notification bar while downloading
        request1.setTitle("File1.mp3");
        request1.setVisibleInDownloadsUi(false);
        request1.setAllowedOverRoaming(false);
        request1.setVisibleInDownloadsUi(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request1.allowScanningByMediaScanner();
            request1.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        }
        request1.setDestinationInExternalFilesDir(getApplicationContext(), "/File", "157.245.cer");

        DownloadManager manager1 = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager1.enqueue(request1);
        if (DownloadManager.STATUS_SUCCESSFUL == 8) {
            parseCertificate();
        }
    }

    private void parseCertificate() {
        certificate = null;
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            File file = new File(getExternalFilesDir(null) + "/File/" + "/157.245.cer");
            FileInputStream fileInputStream = new FileInputStream(file);
            certificate = (X509Certificate) factory.generateCertificate(fileInputStream);

            if(storeCertificate(certificate)){

                String alias = "local:10e3f820d5206d98cda04c4acde82f124ade7c79";
//            X509Certificate certificate = TrustedCertificateManager.getInstance().getCACertificateFromAlias(alias);
                TrustedCertificateEntry mCertEntry =
                        certificate == null ? null : new TrustedCertificateEntry(certificate.getSigAlgName(), certificate);
                profile.setCertificateAlias(mCertEntry.getAlias());

                if (mService != null && mService.getState() == State.CONNECTED) {
                    profileInfo.putBoolean(PROFILE_RECONNECT, mService.getProfile().getId() == profile.getId());

                    ConfirmationDialog dialog = new ConfirmationDialog();
                    dialog.setArguments(profileInfo);
                    dialog.show(this.getFragmentManager(), DIALOG_TAG);
                    return;
                }
                startVpnProfile(profileInfo);
            }
            /* we don't check whether it's actually a CA certificate or not */
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
//            e.printStackTrace();
            if (mService != null && mService.getState() == State.CONNECTED) {
                profileInfo.putBoolean(PROFILE_RECONNECT, mService.getProfile().getId() == profile.getId());

                ConfirmationDialog dialog = new ConfirmationDialog();
                dialog.setArguments(profileInfo);
                dialog.show(this.getFragmentManager(), DIALOG_TAG);
                return;
            }
            startVpnProfile(profileInfo);
        }
    }

    private boolean storeCertificate(X509Certificate certificate) {
        try {
            KeyStore store = KeyStore.getInstance("LocalCertificateStore");
            store.load(null, null);
            store.setCertificateEntry(null, certificate);
            TrustedCertificateManager.getInstance().reset();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}

