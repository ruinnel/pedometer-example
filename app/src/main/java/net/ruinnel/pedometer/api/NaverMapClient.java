/*
 * Filename	: AppClient.java
 * Function	:
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.api;

import net.ruinnel.pedometer.api.bean.ReverseGeocode;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NaverMapClient {
  @GET("/v1/map/reversegeocode")
  Call<ReverseGeocode> reverseGeocode(@Query("query") String latLng);
}

