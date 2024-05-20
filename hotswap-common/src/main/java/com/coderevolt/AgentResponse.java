package com.coderevolt;

import java.io.Serializable;

/**
 * @author Administrator
 */
public class AgentResponse<T> implements Serializable {

    private boolean ok;

    private String msg;

    private T data;

    private long ts;

    public AgentResponse(boolean ok, String msg, T data, long ts) {
        this.ok = ok;
        this.msg = msg;
        this.data = data;
        this.ts = ts;
    }

    public static <T> AgentResponse<T> success(String msg, T data) {
        return new AgentResponse(true, msg, data, System.currentTimeMillis());
    }

    public static <T> AgentResponse<T> failed(String msg, T data) {
        return new AgentResponse(false, msg, data, System.currentTimeMillis());
    }


    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "AgentResponse{" +
                "ok=" + ok +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                ", ts=" + ts +
                '}';
    }
}
