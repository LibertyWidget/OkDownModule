package com.okdown.request.m3u8;

public class M3U8Ts implements Comparable<M3U8Ts> {
    private String file;
    private float seconds;

    public M3U8Ts(String file, float seconds) {
        this.file = file;
        this.seconds = seconds;
    }

    public String getFile() {
        return file;
    }

    public String getFileName() {
        String fileName = file.substring(file.lastIndexOf("/") + 1);
        if (fileName.contains("?")) {
            return fileName.substring(0, fileName.indexOf("?"));
        }
        return fileName;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public float getSeconds() {
        return seconds;
    }

    public long getLongDate() {
        try {
            return Long.parseLong(file.substring(0, file.lastIndexOf(".")));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int compareTo(M3U8Ts o) {
        return file.compareTo(o.file);
    }
}
