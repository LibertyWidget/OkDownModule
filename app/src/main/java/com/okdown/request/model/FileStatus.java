package com.okdown.request.model;

public enum FileStatus {
    NONE,        //无状态
    WAITING,      //等待
    LOADING,    //下载中
    PAUSE,      //暂停
    ERROR,       //错误
    FINISH,    //完成
}
