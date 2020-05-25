package org.strongswan.android.model.image;

import com.google.gson.annotations.SerializedName;

public class ResultItem{

	@SerializedName("image")
	private String image;

	@SerializedName("link")
	private String link;

	@SerializedName("id")
	private String id;

	public String getImage(){
		return image;
	}

	public String getLink(){
		return link;
	}

	public String getId(){
		return id;
	}
}