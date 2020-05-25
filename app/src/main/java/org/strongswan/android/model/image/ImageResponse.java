package org.strongswan.android.model.image;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ImageResponse{

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