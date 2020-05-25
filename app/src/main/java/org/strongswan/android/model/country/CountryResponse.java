package org.strongswan.android.model.country;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CountryResponse{

	@SerializedName("result")
	private List<ResultItem> result;

	@SerializedName("success")
	private String success;

	public List<ResultItem> getResult(){
		return result;
	}

	public String getSuccess(){
		return success;
	}
}