package com.okdown.request.m3u8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class M3U8 {
    private String basePath;
    private List<M3U8Ts> tsList = new ArrayList<>();
    private long startTime;//开始时间
    private long endTime;//结束时间
    private long startDownloadTime;//开始下载时间
    private long endDownloadTime;//结束下载时间

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public List<M3U8Ts> getTsList() {
        return tsList;
    }

    public void setTsList(List<M3U8Ts> tsList) {
        this.tsList = tsList;
    }

    public void addTs(M3U8Ts ts) {
        this.tsList.add(ts);
    }

    public long getStartDownloadTime() {
        return startDownloadTime;
    }

    public void setStartDownloadTime(long startDownloadTime) {
        this.startDownloadTime = startDownloadTime;
    }

    public long getEndDownloadTime() {
        return endDownloadTime;
    }

    public void setEndDownloadTime(long endDownloadTime) {
        this.endDownloadTime = endDownloadTime;
    }

    public long getStartTime() {
        if (tsList.size() > 0) {
            Collections.sort(tsList);
            startTime = tsList.get(0).getLongDate();
            return startTime;
        }
        return 0;
    }

    public long getEndTime() {
        if (tsList.size() > 0) {
            M3U8Ts m3U8Ts = tsList.get(tsList.size() - 1);
            endTime = m3U8Ts.getLongDate() + (long) (m3U8Ts.getSeconds() * 1000);
            return endTime;
        }
        return 0;
    }
}
