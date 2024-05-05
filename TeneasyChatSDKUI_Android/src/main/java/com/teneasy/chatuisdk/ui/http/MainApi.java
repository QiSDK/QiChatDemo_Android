package com.teneasy.chatuisdk.ui.http;

import com.google.gson.JsonObject;
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * API接口映射
 */
public class MainApi {

    /**
     * 身份验证
     */
    public interface IMainTask {
        @POST("/v1/api/query-worker")
        @Headers({"Content-Type: application/json", "Accept: application/json"})
        Observable<ReturnData<WorkerInfo>> workerInfo(@Body JsonObject param);

        @Multipart
        @POST("/v1/assets/upload/")
//        @Headers({"Content-Type: multipart/form-data", "Accept: multipart/form-data"})
        Observable<ReturnData> uploads(@Part MultipartBody.Part body);
    }
}
