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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;

/**
 * 视频播放器Model
 * Created by yinglovezhuzhu@gmail.com on 2016/10/8.
 */

public class VideoPlayerMdoel implements IVideoPlayerModel {

    private Downloader mDownloader;
    /** 当前处于错误状态下 **/
    private boolean mOnError = false;
    /** 正在缓存数据 **/
    private boolean mCaching = false;
    /** 开始缓存的下载长度 **/
    private long mStartCachingSize = 0;

    @Override
    public void download(Context context, String downloadUrl) {
        mDownloader = new Downloader(context, downloadUrl, new File(context.getExternalCacheDir(), "Video"), null);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    mDownloader.download(".mp4", new DownloadListener() {
//                        @Override
//                        public void onProgressUpdate(int downloadedSize, int totalSize) {
//                            Log.e("VideoPlayer", downloadedSize + " / " + totalSize);
//                            if(mOnError) {
//                                if(!mCaching) {
//                                    mStartCachingSize = downloadedSize;
//                                    mCaching = true;
//                                }
//                                if(downloadedSize - mStartCachingSize > CACHE_MIN_SIZE) {
//                                    if(null == mUri) {
//                                        mUri = Uri.fromFile(mDownloader.getSavedFile());
//                                    }
//                                    mCaching = false;
//                                    mOnError = false;
//                                    mHandler.sendMessage(mHandler.obtainMessage(MSG_CACHE_READY, mUri));
//                                }
//                            } else {
//                                if(null == mUri) {
//                                    mUri = Uri.fromFile(mDownloader.getSavedFile());
//                                    mHandler.sendMessage(mHandler.obtainMessage(MSG_GET_URI, mUri));
//                                }
//                            }
//                        }
//                    });
//                } catch (Exception e) {
//                    mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOAD_ERROR, e));
//                }
//            }
//        }).start();
    }
}
