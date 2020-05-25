package org.strongswan.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.strongswan.android.api.APIClient;
import org.strongswan.android.api.APIInterface;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.logic.imc.ImcState;
import org.strongswan.android.logic.imc.RemediationInstruction;
import org.strongswan.android.model.country.CountryResponse;
import org.strongswan.android.model.image.ImageResponse;
import org.strongswan.android.model.image.ResultItem;
import org.strongswan.android.ui.LogActivity;
import org.strongswan.android.ui.RemediationInstructionsActivity;
import org.strongswan.android.ui.RemediationInstructionsFragment;
import org.strongswan.android.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class VpnFragment extends Fragment implements VpnStateService.VpnStateListener {

    private boolean mReadOnly;
    private List<VpnProfile> mVpnProfiles;
    private VpnProfileDataSource mDataSource;
    private LinearLayout layout_info;
    private ImageView mIvPicture;
    private TextView tv_free, tv_status;
    private OnVpnProfileSelectedListener mListener;
    private int stateBaseColor;
    private ProgressDialog mConnectDialog;
    private ProgressDialog mDisconnectDialog;
    private ProgressDialog mProgressDialog;
    private AlertDialog mErrorDialog;
    private long mErrorConnectionID;
    private long mDismissedConnectionID;
    private APIInterface apiInterface;
    private ArrayList<ResultItem> mImageList;
    private Context mContext;

    private VpnStateService mService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((VpnStateService.LocalBinder) service).getService();
            mService.registerListener(VpnFragment.this);
            updateView();
        }
    };

    /**
     * The activity containing this fragment should implement this interface
     */
    public interface OnVpnProfileSelectedListener {
        public void onVpnProfileSelected(VpnProfile profile);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity().getApplicationContext();
        context.bindService(new Intent(context, VpnStateService.class),
                mServiceConnection, Service.BIND_AUTO_CREATE);

        Bundle args = getArguments();
        if (args != null) {
            mReadOnly = args.getBoolean("read_only", mReadOnly);
        }

        if (!mReadOnly) {
            setHasOptionsMenu(true);
        }

        mDataSource = new VpnProfileDataSource(this.getActivity());
        mDataSource.open();

        /* cached list of profiles used as backend for the ListView */
//        mVpnProfiles = mDataSource.getAllVpnProfiles();
        mVpnProfiles = mDataSource.getAllVpnProfiles();
        getImageApi();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_vpn, null);
        layout_info = (LinearLayout) view.findViewById(R.id.layout_info);
        mIvPicture = (ImageView) view.findViewById(R.id.mIvPicture);
        tv_free = (TextView) view.findViewById(R.id.tv_free);
        tv_status = (TextView) view.findViewById(R.id.tv_status);
        stateBaseColor = tv_status.getCurrentTextColor();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        layout_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // callback.theMethod();
                if (mListener != null && mVpnProfiles != null && mVpnProfiles.size() > 0) {
                    mListener.onVpnProfileSelected(mVpnProfiles.get(Utils.selectedPosition));
                }else {
                    getCountryList();
                }
            }
        });
    }

    private void getCountryList() {
        apiInterface = APIClient.getClient().create(APIInterface.class);
        if(Utils.isNetworkAvailable(mContext)){
            Call<CountryResponse> countryResponseCall = apiInterface.getCountryList();
            countryResponseCall.enqueue(new Callback<CountryResponse>() {
                @Override
                public void onResponse(Call<CountryResponse> call, Response<CountryResponse> response) {
                    if(response.body() != null && response.body().getSuccess().equalsIgnoreCase("true")){
                        Utils.mCountryResponseList.clear();
                        Utils.mCountryResponseList.addAll(response.body().getResult());

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

                        mVpnProfiles = mDataSource.getAllVpnProfiles();
                        layout_info.performClick();
                    }
                }

                @Override
                public void onFailure(Call<CountryResponse> call, Throwable t) {

                }
            });
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataSource.close();
        if (mService != null) {
            getActivity().getApplicationContext().unbindService(mServiceConnection);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mService != null) {
            mService.registerListener(this);
            updateView();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mService != null) {
            mService.unregisterListener(this);
        }
    }

    @Override
    public void stateChanged() {
        updateView();
    }

    public void updateView() {
        long connectionID = mService.getConnectionID();
        VpnProfile profile = mService.getProfile();
        VpnStateService.State state = mService.getState();
        VpnStateService.ErrorState error = mService.getErrorState();
        ImcState imcState = mService.getImcState();
        String name = "", gateway = "";

        if (profile != null) {
            name = profile.getName();
            gateway = profile.getGateway();
        }

        if (reportError(connectionID, name, error, imcState)) {
            return;
        }

        enableActionButton(false);
        tv_free.setText(name);

        switch (state) {
            case DISABLED:
                showProfile(false);
                hideProgressDialog();
                tv_status.setText(R.string.state_disabled);
                tv_status.setTextColor(stateBaseColor);
                break;
            case CONNECTING:
                showProfile(true);
                showConnectDialog(name, gateway);
                tv_status.setText(R.string.state_connecting);
                tv_status.setTextColor(stateBaseColor);
                break;
            case CONNECTED:
                showProfile(true);
                hideProgressDialog();
                enableActionButton(true);
                tv_status.setText(R.string.state_connected);
                tv_status.setTextColor(getResources().getColor(R.color.success_text));
                break;
            case DISCONNECTING:
                showProfile(true);
                showDisconnectDialog(name);
                tv_status.setText(R.string.state_disconnecting);
                tv_status.setTextColor(stateBaseColor);
                break;
        }
    }

    private boolean reportError(long connectionID, String name, VpnStateService.ErrorState error, ImcState imcState) {
        if (connectionID > mDismissedConnectionID) {    /* report error if it hasn't been dismissed yet */
            mErrorConnectionID = connectionID;
        } else {    /* ignore all other errors */
            error = VpnStateService.ErrorState.NO_ERROR;
        }
        if (error == VpnStateService.ErrorState.NO_ERROR) {
            hideErrorDialog();
            return false;
        } else if (mErrorDialog != null) {    /* we already show the dialog */
            return true;
        }
        hideProgressDialog();
        tv_free.setText(name);
        showProfile(true);
        enableActionButton(false);
        tv_status.setText(R.string.state_error);
        tv_status.setTextColor(getResources().getColor(R.color.error_text));
        switch (error) {
            case AUTH_FAILED:
                if (imcState == ImcState.BLOCK) {
                    showErrorDialog(R.string.error_assessment_failed);
                } else {
                    showErrorDialog(R.string.error_auth_failed);
                }
                break;
            case PEER_AUTH_FAILED:
                showErrorDialog(R.string.error_peer_auth_failed);
                break;
            case LOOKUP_FAILED:
                showErrorDialog(R.string.error_lookup_failed);
                break;
            case UNREACHABLE:
                showErrorDialog(R.string.error_unreachable);
                break;
            default:
                showErrorDialog(R.string.error_generic);
                break;
        }
        return true;
    }

    private void showProfile(boolean show) {
//        mProfileView.setVisibility(show ? View.VISIBLE : View.GONE);
//        mProfileNameView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void enableActionButton(boolean enable) {

//        mActionButton.setEnabled(enable);
//        mActionButton.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
            mDisconnectDialog = mConnectDialog = null;
        }
    }

    private void hideErrorDialog() {
        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }
    }

    private void clearError() {
        if (mService != null) {
            mService.disconnect();
        }
        mDismissedConnectionID = mErrorConnectionID;
        updateView();
    }

    private void showConnectDialog(String profile, String gateway) {
        if (mConnectDialog != null) {    /* already showing the dialog */
            return;
        }
        hideProgressDialog();
        mConnectDialog = new ProgressDialog(getActivity());
        mConnectDialog.setTitle(String.format(getString(R.string.connecting_title), profile));
        mConnectDialog.setMessage(String.format(getString(R.string.connecting_message), gateway));
        mConnectDialog.setIndeterminate(true);
        mConnectDialog.setCancelable(false);
        mConnectDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mService != null) {
                            mService.disconnect();
                        }
                    }
                });
        mConnectDialog.show();
        mProgressDialog = mConnectDialog;
    }

    private void showDisconnectDialog(String profile) {
        if (mDisconnectDialog != null) {    /* already showing the dialog */
            return;
        }
        hideProgressDialog();
        mDisconnectDialog = new ProgressDialog(getActivity());
        mDisconnectDialog.setMessage(getString(R.string.state_disconnecting));
        mDisconnectDialog.setIndeterminate(true);
        mDisconnectDialog.setCancelable(false);
        mDisconnectDialog.show();
        mProgressDialog = mDisconnectDialog;
    }

    private void showErrorDialog(int textid) {
        final List<RemediationInstruction> instructions = mService.getRemediationInstructions();
        final boolean show_instructions = mService.getImcState() == ImcState.BLOCK && !instructions.isEmpty();
        int text = show_instructions ? R.string.show_remediation_instructions : R.string.show_log;

        mErrorDialog = new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.error_introduction) + " " + getString(textid))
                .setCancelable(false)
                .setNeutralButton(text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearError();
                        dialog.dismiss();
                        Intent intent;
                        if (show_instructions) {
                            intent = new Intent(getActivity(), RemediationInstructionsActivity.class);
                            intent.putParcelableArrayListExtra(RemediationInstructionsFragment.EXTRA_REMEDIATION_INSTRUCTIONS,
                                    new ArrayList<RemediationInstruction>(instructions));
                        } else {
                            intent = new Intent(getActivity(), LogActivity.class);
                        }
                        startActivity(intent);
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        clearError();
                        dialog.dismiss();
                    }
                }).create();
        mErrorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mErrorDialog = null;
            }
        });
        mErrorDialog.show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        if (activity instanceof OnVpnProfileSelectedListener) {
            mListener = (OnVpnProfileSelectedListener) activity;
        }
    }

    private void getImageApi() {
        mImageList = new ArrayList<ResultItem>();
        apiInterface = APIClient.getClient().create(APIInterface.class);
        Call<ImageResponse> imageResponseCall = apiInterface.getImageList();
        imageResponseCall.enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                if (response.body() != null && response.body().getSuccess().equalsIgnoreCase("true")) {
                    mImageList.addAll(response.body().getResult());
                    setImage();
                }
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {

            }
        });
    }

    private void setImage() {
//        Glide.with(mContext).load(mImageList.get(0).getImage()).apply(RequestOptions.circleCropTransform())
//                .into(mIvPicture);
    }
}