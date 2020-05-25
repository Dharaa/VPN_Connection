package org.strongswan.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.strongswan.android.adapter.CountryAdapter;
import org.strongswan.android.api.APIClient;
import org.strongswan.android.api.APIInterface;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.model.country.CountryResponse;
import org.strongswan.android.model.country.ResultItem;
import org.strongswan.android.ui.VpnProfileDetailActivity;
import org.strongswan.android.ui.VpnProfileListFragment;
import org.strongswan.android.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HirechyFragment extends Fragment {


    private ListView mRvCountryList;
    private Context mContext;
    private APIInterface apiInterface;
    private ArrayList<ResultItem> mCountryResponseList;
    private CountryAdapter countryAdapter;
    private OnVpnProfileSelectedListener mListener;
    public interface OnVpnProfileSelectedListener {
        public void onVpnProfileSelected(VpnProfile profile);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        if (activity instanceof OnVpnProfileSelectedListener) {
            mListener = (OnVpnProfileSelectedListener) activity;
        }
    }

    private final AdapterView.OnItemClickListener mVpnProfileClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> a, View v, int position, long id) {
            if (mListener != null) {
                Utils.selectedPosition = position;
                ResultItem resultItem = (ResultItem) a.getItemAtPosition(position);
                VpnProfile vpnProfile = new VpnProfile();
                vpnProfile.setmCountryId(resultItem.getCountryId());
                vpnProfile.setName(resultItem.getName());
                vpnProfile.setGateway(resultItem.getIp());
                vpnProfile.setVpnType(VpnType.IKEV2_EAP);
                vpnProfile.setUsername(resultItem.getUsername());
                vpnProfile.setPassword(resultItem.getPassword());
                //vpnProfile.setCertificateAlias(resultItem.getCertificate());
                vpnProfile.setUserCertificateAlias(null);
                mListener.onVpnProfileSelected((VpnProfile) vpnProfile);
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hirechy, container, false);
        mRvCountryList = (ListView) root.findViewById(R.id.mRvCountryList);
        mRvCountryList.setOnItemClickListener(mVpnProfileClicked);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCountryResponseList = new ArrayList<ResultItem>();
        countryAdapter = new CountryAdapter(mContext, mCountryResponseList);
        mRvCountryList.setAdapter(countryAdapter);
        getCountryList();
    }

    private void getCountryList() {
        apiInterface = APIClient.getClient().create(APIInterface.class);
        if(Utils.isNetworkAvailable(mContext)){
            Call<CountryResponse> countryResponseCall = apiInterface.getCountryList();
            countryResponseCall.enqueue(new Callback<CountryResponse>() {
                @Override
                public void onResponse(Call<CountryResponse> call, Response<CountryResponse> response) {
                    if(response.body() != null && response.body().getSuccess().equalsIgnoreCase("true")){
                        mCountryResponseList.clear();
                        mCountryResponseList.addAll(response.body().getResult());
                        countryAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<CountryResponse> call, Throwable t) {

                }
            });
        }
    }

}