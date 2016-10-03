/*
 * Copyright (C) 2016. The Android Open Source Project.
 *
 *          yinglovezhuzhu@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.opensource.videoplayer;

/**
 * 下载日志数据实体类
 * Created by yinglovezhuzhu@gmail.com on 2016/10/3.
 */

public class DownloadLog {
    private long id;
    private String url;
    private int downloadedSize;
    private int totalSize;
    private String savedFile;

    public DownloadLog() {

    }

    public DownloadLog(String url, int downloadedSize,
                       int totalSize, String savedFile) {
        this.url = url;
        this.downloadedSize = downloadedSize;
        this.totalSize = totalSize;
        this.savedFile = savedFile;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(int downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public String getSavedFile() {
        return savedFile;
    }

    public void setSavedFile(String savedFile) {
        this.savedFile = savedFile;
    }

    @Override
    public String toString() {
        return "DownloadLog{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", downloadedSize=" + downloadedSize +
                ", totalSize=" + totalSize +
                ", savedFile='" + savedFile + '\'' +
                '}';
    }
}
