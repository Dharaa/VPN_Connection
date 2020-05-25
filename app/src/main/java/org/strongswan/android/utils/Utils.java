/*
 * Copyright (C) 2014 Tobias Brunner
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

package org.strongswan.android.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.strongswan.android.model.country.ResultItem;

import java.util.ArrayList;

public class Utils {
    static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();
    public static ArrayList<ResultItem> mCountryResponseList = new ArrayList<ResultItem>();
    public static int selectedPosition = 0;

    /**
     * Converts the given byte array to a hexadecimal string encoding.
     *
     * @param bytes byte array to convert
     * @return hex string
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i];
            hex[i * 2] = HEXDIGITS[(value & 0xf0) >> 4];
            hex[i * 2 + 1] = HEXDIGITS[value & 0x0f];
        }
        return new String(hex);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
