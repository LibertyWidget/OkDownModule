package com.okdown.request.m3u8;

import android.util.Log;

import com.okdown.utils.IOUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MUtils {

    public static M3U8 parseIndex(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (conn.getResponseCode() == 200) {
            String realUrl = conn.getURL().toString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String basePath = realUrl.substring(0, realUrl.lastIndexOf("/") + 1);
            M3U8 ret = new M3U8();
            ret.setBasePath(basePath);

            String line;
            float seconds = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    if (line.startsWith("#EXTINF:")) {
                        line = line.substring(8);
                        if (line.endsWith(",")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        seconds = Float.parseFloat(line);
                    }
                    continue;
                }
                if (line.endsWith("m3u8")) {
                    return parseIndex(basePath + line);
                }
                ret.addTs(new M3U8Ts(line, seconds));
                seconds = 0;
            }
            reader.close();
            return ret;
        } else {
            return null;
        }
    }

    public static M3U8 parse(String url) {
        M3U8 ret = new M3U8();
        float seconds = 0;
        String[] split = url.split("\n");
        for (String line : split) {
            if (line.startsWith("#")) {
                if (line.startsWith("#EXTINF:")) {
                    line = line.substring(8);
                    if (line.endsWith(",")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    seconds = Float.parseFloat(line);
                }
                continue;
            }
            ret.addTs(new M3U8Ts(line, seconds));
            seconds = 0;
        }
        return ret;
    }

    public static String merge(M3U8 m3u8, String toFile) throws IOException {
        List<M3U8Ts> mergeList = getLimitM3U8Ts(m3u8);
        File file = new File(toFile);
        FileOutputStream fos = new FileOutputStream(file);

        for (M3U8Ts ts : mergeList) {
            IOUtils.copyLarge(new FileInputStream(new File(file.getParentFile(), ts.getFileName())), fos);
        }
        fos.close();
        return toFile;
    }

    public static void merge(File[] fileList, String toFile) throws IOException {
        File file = new File(toFile);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(file);
        for (File tsFile : fileList) {
            IOUtils.copyLarge(new FileInputStream(tsFile), fos);
        }
        fos.close();
    }

    public static void merge(List<File> fileList, String toFile) throws IOException {
        File file = new File(toFile);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(file);
        for (File tsFile : fileList) {
            IOUtils.copyLarge(new FileInputStream(tsFile), fos);
        }
        fos.close();
    }

    public static void merge(M3U8 m3u8, String toFile, String basePath) throws IOException {
        List<M3U8Ts> mergeList = getLimitM3U8Ts(m3u8);
        File saveFile = new File(toFile);
        FileOutputStream fos = new FileOutputStream(saveFile);
        File file;
        for (M3U8Ts ts : mergeList) {
            file = new File(basePath, ts.getFileName());
            if (file.isFile() && file.exists()) {
                IOUtils.copyLarge(new FileInputStream(file), fos);
            }
        }
        fos.close();
    }

    public static boolean moveFile(String sFile, String tFile) {
        File srcFile = new File(sFile);
        if (!srcFile.exists() || !srcFile.isFile())
            return false;
        File destDir = new File(tFile);
        if (!destDir.exists())
            destDir.mkdirs();

        return srcFile.renameTo(new File(tFile + File.separator + srcFile.getName()));
    }

    public static void clearDir(File dir) {
        if (dir.exists()) {
            if (dir.isFile()) {
                dir.delete();
            } else if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    clearDir(file);
                }
                dir.delete();
            }
        }
    }

    public static List<M3U8Ts> getLimitM3U8Ts(M3U8 m3u8) {
        List<M3U8Ts> downList = new ArrayList<>();

        if (m3u8.getStartDownloadTime() < m3u8.getStartTime() || m3u8.getEndDownloadTime() > m3u8.getEndTime()) {
            downList = m3u8.getTsList();
            return downList;
        }


        if ((m3u8.getStartDownloadTime() == -1 && m3u8.getEndDownloadTime() == -1) || m3u8.getEndDownloadTime() <= m3u8.getStartDownloadTime()) {
            downList = m3u8.getTsList();
        } else if (m3u8.getStartDownloadTime() == -1 && m3u8.getEndDownloadTime() > -1) {
            for (final M3U8Ts ts : m3u8.getTsList()) { //从头下到指定时间
                if (ts.getLongDate() <= m3u8.getEndDownloadTime()) {
                    downList.add(ts);
                }
            }
        } else if (m3u8.getStartDownloadTime() > -1 && m3u8.getEndDownloadTime() == -1) {
            for (final M3U8Ts ts : m3u8.getTsList()) { //从指定时间下到尾部
                if (ts.getLongDate() >= m3u8.getStartDownloadTime()) {
                    downList.add(ts);
                }
            }
        } else {//从指定开始时间下载到指定结束时间
            for (final M3U8Ts ts : m3u8.getTsList()) {
                if (m3u8.getStartDownloadTime() <= ts.getLongDate() && ts.getLongDate() <= m3u8.getEndDownloadTime()) {
                    downList.add(ts);//指定区间的ts
                }
            }
        }
        Log.e("hdltag", "getLimitM3U8Ts(MUtils.java:152):" + downList);
        return downList;
    }

    public static String toList(String m3u8Url, List<M3U8Ts> tsList) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        Iterator<M3U8Ts> iterator = tsList.iterator();
        while (iterator.hasNext()) {
            M3U8Ts m3U8Ts = iterator.next();
            builder.append("{");
            builder.append("\"file\":\"");
            String file = m3U8Ts.getFile();
            if (file.contains("http://") || file.contains("https://")) {
                builder.append(file);
            } else {
                builder.append(m3u8Url).append(file);
            }
            builder.append("\",\"seconds\":\"");
            builder.append(m3U8Ts.getSeconds());
            if (iterator.hasNext()) {
                builder.append("\"},");
            } else {
                builder.append("\"}");
            }
        }
        builder.append("]");
        Log.e("tag", "Json-->" + builder.toString());
        return builder.toString();
    }

    public static M3U8 toJson(String m3u8UrlList) {
        M3U8 m3U8 = new M3U8();
        try {
            JSONArray jsonArray = new JSONArray(m3u8UrlList);
            List<M3U8Ts> tsList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.optJSONObject(i);
                if (null != jsonObject) {
                    M3U8Ts ts = new M3U8Ts(jsonObject.optString("file"), Float.valueOf(jsonObject.optString("seconds")));
                    tsList.add(ts);
                }
            }
            m3U8.setTsList(tsList);
        } catch (Exception ignored) {

        }
        return m3U8;
    }
}
