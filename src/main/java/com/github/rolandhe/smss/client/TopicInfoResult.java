package com.github.rolandhe.smss.client;

import lombok.Getter;

@Getter
public class TopicInfoResult {
    private final boolean success;
    private String errMessage;
    private String jsonBody;

    public TopicInfoResult(boolean success, String errMessage) {
        this.success = success;
        this.errMessage = errMessage;
    }
    public TopicInfoResult(String jsonBody) {
        this.success = true;
        this.jsonBody = jsonBody;
    }


    @Override
    public String toString(){
        if(success){
            return String.format("suc=true, json=%s",jsonBody);
        }
        return String.format("suc=false, err=%s",errMessage);
    }
}
