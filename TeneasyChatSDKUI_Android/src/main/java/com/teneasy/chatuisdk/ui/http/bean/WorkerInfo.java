package com.teneasy.chatuisdk.ui.http.bean;

import java.io.Serializable;

public class WorkerInfo implements Serializable {
    private String workerName;
    private int id;
    private String workerAvatar;

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getWorkerAvatar() {
        return workerAvatar;
    }

    public void setWorkerAvatar(String workerAvatar) {
        this.workerAvatar = workerAvatar;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
