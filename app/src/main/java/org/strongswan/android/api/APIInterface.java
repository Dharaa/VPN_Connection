package org.strongswan.android.api;



import org.strongswan.android.model.country.CountryResponse;
import org.strongswan.android.model.image.ImageResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface APIInterface {

    /*@Multipart
    @POST(AndyConstants.WebAPI.SIGN_UP)
    Call<LoginResponse> callSignUpAPI(@Part(AndyConstants.Parameter.FIRST_NAME) RequestBody name,
                                      @Part(AndyConstants.Parameter.LAST_NAME) RequestBody lname,
                                      @Part(AndyConstants.Parameter.EMAIL) RequestBody email,
                                      @Part(AndyConstants.Parameter.PASSWORD) RequestBody password,
                                      @Part(AndyConstants.Parameter.PHONE_NUMBER) RequestBody phone_number,
                                      @Part(AndyConstants.Parameter.GENDER) RequestBody gender,
                                      @Part(AndyConstants.Parameter.ADDRESS) RequestBody address,
                                      @Part(AndyConstants.Parameter.DEVICE_TYPE) RequestBody device_type,
                                      @Part(AndyConstants.Parameter.DEVICE_TOKEN) RequestBody device_token,
                                      @Part MultipartBody.Part profile_picture
    );

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.EDIT_PROFILE)
    Call<LoginResponse> callEditProfileAPI(@Field(AndyConstants.Parameter.USER_ID) String userId,
                                           @Field(AndyConstants.Parameter.FIRST_NAME) String name,
                                           @Field(AndyConstants.Parameter.LAST_NAME) String lname,
                                           @Field(AndyConstants.Parameter.EMAIL) String email,
                                           @Field(AndyConstants.Parameter.PHONE_NUMBER) String phone_number,
                                           @Field(AndyConstants.Parameter.PROFILE_PICTURE) String profile_picture
    );

    @Multipart
    @POST(AndyConstants.WebAPI.EDIT_PROFILE)
    Call<LoginResponse> callEditProfileAPI(@Part(AndyConstants.Parameter.USER_ID) RequestBody userId,
                                           @Part(AndyConstants.Parameter.FIRST_NAME) RequestBody name,
                                           @Part(AndyConstants.Parameter.LAST_NAME) RequestBody lname,
                                           @Part(AndyConstants.Parameter.EMAIL) RequestBody email,
                                           @Part(AndyConstants.Parameter.PHONE_NUMBER) RequestBody phone_number,
                                           @Part(AndyConstants.Parameter.ADDRESS) RequestBody address,
                                           @Part MultipartBody.Part profile_picture
    );

    @Multipart
    @POST(AndyConstants.WebAPI.EDIT_PROFILE)
    Call<LoginResponse> callEditProfileWithProfilePictureAPI(@Part(AndyConstants.Parameter.DRIVER_ID) RequestBody driver_id,
                                                             @Part(AndyConstants.Parameter.FIRST_NAME) RequestBody name,
                                                             @Part(AndyConstants.Parameter.LAST_NAME) RequestBody lname,
                                                             @Part(AndyConstants.Parameter.ADDRESS) RequestBody address,
                                                             @Part(AndyConstants.Parameter.PHONE_NUMBER) RequestBody phone_number,
                                                             @Part(AndyConstants.Parameter.CAR_COMPANY) RequestBody car_company,
                                                             @Part(AndyConstants.Parameter.CAR_NAME) RequestBody car_name,
                                                             @Part(AndyConstants.Parameter.ZIP_CODE) RequestBody zip_code,
                                                             @Part(AndyConstants.Parameter.CAR_PLATE_NO) RequestBody car_plate_no,
                                                             @Part(AndyConstants.Parameter.GENDER) RequestBody gender,
                                                             @Part(AndyConstants.Parameter.CITY) RequestBody city,
                                                             @Part(AndyConstants.Parameter.STATE) RequestBody state,
                                                             @Part(AndyConstants.Parameter.COUNTRY) RequestBody country,
                                                             @Part MultipartBody.Part profile_picture
    );

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.SOCIAL_LOGIN)
    Call<LoginResponse> callFbLoginAPI(@Field(AndyConstants.Parameter.FIRST_NAME) String name,
                                       @Field(AndyConstants.Parameter.LAST_NAME) String lname,
                                       @Field(AndyConstants.Parameter.EMAIL) String email,
                                       @Field(AndyConstants.Parameter.PROFILE_PICTURE) String profile_picture,
                                       @Field(AndyConstants.Parameter.FB_ID) String fb_id,
                                       @Field(AndyConstants.Parameter.GOOGLE_ID) String google_id,
                                       @Field(AndyConstants.Parameter.GENDER) String gender,
                                       @Field(AndyConstants.Parameter.LOGIN_TYPE) String login_type,
                                       @Field(AndyConstants.Parameter.DEVICE_TYPE) String device_type,
                                       @Field(AndyConstants.Parameter.DEVICE_TOKEN) String device_token
    );

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.SOCIAL_LOGIN)
    Call<LoginResponse> callGooglePlusLoginAPI(@Field(AndyConstants.Parameter.FIRST_NAME) String name,
                                               @Field(AndyConstants.Parameter.LAST_NAME) String lname,
                                               @Field(AndyConstants.Parameter.EMAIL) String email,
                                               @Field(AndyConstants.Parameter.PROFILE_PICTURE) String profile_picture,
                                               @Field(AndyConstants.Parameter.FB_ID) String fb_id,
                                               @Field(AndyConstants.Parameter.GOOGLE_ID) String google_id,
                                               @Field(AndyConstants.Parameter.LOGIN_TYPE) String login_type,
                                               @Field(AndyConstants.Parameter.DEVICE_TYPE) String device_type,
                                               @Field(AndyConstants.Parameter.DEVICE_TOKEN) String device_token
    );

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.LOGIN)
    Call<LoginResponse> callCustomeLoginAPI(@Field(AndyConstants.Parameter.EMAIL) String email,
                                            @Field(AndyConstants.Parameter.PASSWORD) String password,
                                            @Field(AndyConstants.Parameter.LOGIN_TYPE) String login_type,
                                            @Field(AndyConstants.Parameter.DEVICE_TYPE) String device_type,
                                            @Field(AndyConstants.Parameter.DEVICE_TOKEN) String device_token
    );

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.FORGOT_PASSWORD)
    Call<CommonResponse> callForgotPasswordAPI(@Field(AndyConstants.Parameter.EMAIL) String email

    );

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.CHANGE_PASSWORD)
    Call<CommonResponse> callChangePasswordAPI(@Field(AndyConstants.Parameter.USER_ID) String user_id,
                                               @Field(AndyConstants.Parameter.OLD_PASSWORD) String old_password,
                                               @Field(AndyConstants.Parameter.NEW_PASSWORD) String new_password

    );

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.OTP_VERIFICATION)
    Call<CommonResponse> callOTPVerificationAPI(@Field(AndyConstants.Parameter.USER_ID) String userId,
                                                @Field(AndyConstants.Parameter.OTP) String otp);

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.GENERATE_REQUEST)
    Call<GenerateRequestResponse> callGenerateRequestAPI(@Field(AndyConstants.Parameter.USER_ID) String user_id,
                                                         @Field(AndyConstants.Parameter.PICKUP_ADDRESS) String pickup_address,
                                                         @Field(AndyConstants.Parameter.PICKUP_LATITUDE) String pickup_latitude,
                                                         @Field(AndyConstants.Parameter.PICKUP_LONGITUDE) String pickup_longitude,
                                                         @Field(AndyConstants.Parameter.DROP_ADDRESS) String drop_address,
                                                         @Field(AndyConstants.Parameter.DROP_LATITUDE) String drop_latitude,
                                                         @Field(AndyConstants.Parameter.DROP_LONGITUDE) String drop_longitude,
                                                         @Field(AndyConstants.Parameter.GENDER) String gender,
                                                         @Field(AndyConstants.Parameter.AMOUNT) String amount,
                                                         @Field(AndyConstants.Parameter.DISTANCE) String distance,
                                                         @Field(AndyConstants.Parameter.DURATION) String duration,
                                                         @Field(AndyConstants.Parameter.STRIPE_CUSTOMER_ID) String stripe_customer_id

    );

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.GET_REQUEST_STATUS)
    Call<GetRequestStatusResponse> callGetRequestStatusAPI(@Field(AndyConstants.Parameter.USER_ID) String user_id);

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.GET_REQUEST_STATUS)
    Observable<GetRequestStatusResponse> callGetRequestStatusRecursiveAPI(@Field(AndyConstants.Parameter.USER_ID) String userId);

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.CANCEL_REQUEST)
    Call<CommonResponse> callCancelRequestAPI(@Field(AndyConstants.Parameter.USER_ID) String user_id,
                                              @Field(AndyConstants.Parameter.DRIVER_ID) String driver_id,
                                              @Field(AndyConstants.Parameter.REQUEST_ID) String request_id);

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.GET_REQUEST_HISTORY)
    Call<GetHistoryResponse> callGetHistoryAPI(@Field(AndyConstants.Parameter.USER_ID) String driverId);


    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.RATING)
    Call<CommonResponse> callRatingAPI(@Field(AndyConstants.Parameter.USER_ID) String user_id,
                                       @Field(AndyConstants.Parameter.DRIVER_ID) String driver_id,
                                       @Field(AndyConstants.Parameter.RATING) String rating,
                                       @Field(AndyConstants.Parameter.COMMENT) String comment);

    @FormUrlEncoded
    @POST(AndyConstants.WebAPI.GET_CHAT_HISTORY)
    Call<GetChatHistoryResponse> getChatHistory(@Field(AndyConstants.Parameter.SENDER_ID) String sender_id,
                                                @Field(AndyConstants.Parameter.RECEIVER_ID) String receiver_id,
                                                @Field(AndyConstants.Parameter.SENDER_TYPE) String senderType,
                                                @Field(AndyConstants.Parameter.RECEIVER_TYPE) String receiverType);*/

    @GET(AndyConstants.WebAPI.COUNTRY)
    Call<CountryResponse> getCountryList();

    @GET(AndyConstants.WebAPI.IMAGE)
    Call<ImageResponse> getImageList();
}
