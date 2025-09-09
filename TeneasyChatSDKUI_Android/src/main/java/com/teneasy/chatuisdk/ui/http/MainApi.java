package com.teneasy.chatuisdk.ui.http;

import com.google.gson.JsonObject;
import com.teneasy.chatuisdk.Entrance;
import com.teneasy.chatuisdk.ui.http.bean.AssignWorker;
import com.teneasy.chatuisdk.ui.http.bean.AutoReply;
import com.teneasy.chatuisdk.ui.http.bean.AutoReplyItem;
import com.teneasy.chatuisdk.ui.http.bean.ChatHistory.ChatHistory;
import com.teneasy.chatuisdk.ui.http.bean.ChatHistory.Request;
import com.teneasy.chatuisdk.ui.http.bean.ChatHistory.hMessage;
import com.teneasy.chatuisdk.ui.http.bean.ErrorReport;
import com.teneasy.chatuisdk.ui.http.bean.ReplyList;
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo;

import org.json.JSONObject;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import retrofit2.http.Body;
import retrofit2.http.Header;
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

        @POST("v1/api/query-entrance")
        //@Headers({"Content-Type: application/json", "Accept: application/json"})
        Observable<ReturnData<Entrance>> queryEntrance(@Body JsonObject param);

        @POST("v1/api/query-entrance-hidden")
        Observable<ReturnData<Entrance>> queryEntranceHidden(@Body JsonObject param);

        @POST("v1/api/assign-worker")
        Observable<ReturnData<AssignWorker>> assignWorker(@Body JsonObject param);

        @POST("v1/api/query-auto-reply")
        Observable<ReturnData<AutoReply>> queryAutoReply(@Body JsonObject param);

        @POST("v1/api/message/sync")
        Observable<ReturnData<ChatHistory>> queryChatHistory(@Body JsonObject param);

        @POST("v1/api/message/reply-message/sync")
        Observable<ReturnData<ReplyList>> queryMessage(@Body JsonObject param);

        @POST("v1/api/chat/mark-read")
        Observable<ReturnData<Object>> markRead(@Body JsonObject param);

        @POST("v1/api/error-report/upload")
        Observable<ReturnData<Object>> repError(@Body ErrorReport param);

        @Multipart
        @POST("/v1/assets/upload/")
//        @Headers({"Content-Type: multipart/form-data", "Accept: multipart/form-data"})
        Observable<ReturnData> uploads(@Part MultipartBody.Part body);
    }
}
