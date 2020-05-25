package org.strongswan.android.model.country;

import com.google.gson.annotations.SerializedName;

public class ResultItem{

	@SerializedName("password")
	private String password;

	@SerializedName("ip")
	private String ip;

	@SerializedName("name")
	private String name;

	@SerializedName("certificate")
	private String certificate;

	@SerializedName("countryId")
	private String countryId;

	@SerializedName("username")
	private String username;

	public String getPassword(){
		return password;
	}

	public String getIp(){
		return ip;
	}

	public String getName(){
		return name;
	}

	public String getCertificate(){
		return "user:1a09f213.0";
//		return certificate;
	}

	public String getCountryId(){
		return countryId;
	}

	public String getUsername(){
		return username;
	}
}