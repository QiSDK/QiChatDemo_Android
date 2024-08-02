package com.teneasy.chatuisdk.ui.http;

import java.io.Serializable;

/**
 * API返回数据实体，统一化返回结构，便于解析处理
 * @param <T>
 */
public class ReturnData<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
