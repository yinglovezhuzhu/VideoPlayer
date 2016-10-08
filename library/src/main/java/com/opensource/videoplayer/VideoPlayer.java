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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.opensource.videoplayer.db.DownloadDBUtils;

import java.io.File;


public class VideoPlayer implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, Handler.Callback {

    /** 消息：下载错误 **/
    private static final int MSG_DOWNLOAD_ERROR = 0x01;
    /** 消息：缓存完毕 **/
    private static final int MSG_CACHE_READY = 0x02;
    /** 消息：获取到播放文件URI **/
    private static final int MSG_GET_URI = 0x03;

    private static final int CACHE_MIN_SIZE = 1024 * 1024;

    private IVideoPlayerView mView;
    private Uri mUri;
    /** 当前播放进度 **/
    private int mCurrentPosition = 0;
    /** 当前处于错误状态下 **/
    private boolean mOnError = false;
    /** 正在缓存数据 **/
    private boolean mCaching = false;
    /** 开始缓存的下载长度 **/
    private long mStartCachingSize = 0;

    private Downloader mDownloader;

    private PlayListener mPlayListener = null;

    private final Handler mHandler = new Handler(this);

    private final Runnable mPlayingChecker = new Runnable() {
        public void run() {
            if (mView.isPlaying()) {
                mView.hideLoadingProgress();
            } else {
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };

    public VideoPlayer(final Context context, IVideoPlayerView view, Uri videoUri) {
        this.mView = view;

        // For streams that we expect to be slow to start up, show a
        // progress spinner until playback starts.
        String scheme = videoUri.getScheme();
        if (null != scheme && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")
                || scheme.equalsIgnoreCase("ftp") || "rtsp".equalsIgnoreCase(scheme))) {
            // 网络视频
            final String url = videoUri.toString();
            File cacheFile;
            DownloadLog history = DownloadDBUtils.getHistoryByUrl(context, url);
            if(null != history && (cacheFile = new File(history.getSavedFile())).exists()) {
                // 网络视频，且已经有下载记录,并且缓存存在，直接播放缓存
                mView.hideLoadingProgress();
                mUri = Uri.fromFile(cacheFile);
                mView.playVideo(mUri, 0);
            } else {
                // 网络视频，没有下载记录（未下载完成或者还没有开始下载）
                mHandler.postDelayed(mPlayingChecker, 250);
                DownloadLog log = DownloadDBUtils.getLogByUrl(context, url);
                if(null != log) {
                    cacheFile = new File(log.getSavedFile());
                    if(cacheFile.exists()) {
                        mUri = Uri.fromFile(cacheFile);
                        mView.playVideo(mUri, 0);
                    } else {
                        // 缓存文件丢失，删除下载日志
                        DownloadDBUtils.deleteLog(context, url);
                    }
                }
                mDownloader = new Downloader(context, videoUri.toString(), new File(context.getExternalCacheDir(), "Video"), null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mDownloader.download(".mp4", new DownloadListener() {
                                @Override
                                public void onProgressUpdate(int downloadedSize, int totalSize) {
                                    Log.e("VideoPlayer", downloadedSize + " / " + totalSize);
                                    if(mOnError) {
                                        if(!mCaching) {
                                            mStartCachingSize = downloadedSize;
                                            mCaching = true;
                                        }
                                        if(downloadedSize - mStartCachingSize > CACHE_MIN_SIZE) {
                                            if(null == mUri) {
                                                mUri = Uri.fromFile(mDownloader.getSavedFile());
                                            }
                                            mCaching = false;
                                            mOnError = false;
                                            mHandler.sendMessage(mHandler.obtainMessage(MSG_CACHE_READY, mUri));
                                        }
                                    } else {
                                        if(null == mUri) {
                                            mUri = Uri.fromFile(mDownloader.getSavedFile());
                                            mHandler.sendMessage(mHandler.obtainMessage(MSG_GET_URI, mUri));
                                        }
                                    }
                                }
                            });
                        } catch (Exception e) {
                            mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOAD_ERROR, e));
                        }
                    }
                }).start();
            }

        } else {
            mUri = videoUri;
            mView.hideLoadingProgress();
            mView.playVideo(mUri, 0);
        }
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mHandler.removeCallbacksAndMessages(null);
        mView.showLoadingProgress();
        mCurrentPosition = mp.getCurrentPosition();
        mOnError = true;
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(null != mPlayListener) {
            mPlayListener.onCompletion();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_DOWNLOAD_ERROR:
                if(null != mPlayListener) {
                    mPlayListener.onError(PlayListener.WHAT_DOWNLOAD_ERROR,
                            "Video download failed: " + (null == msg.obj ? "" : ((Exception) msg.obj).getMessage()));
                }
                return true;
            case MSG_CACHE_READY:
                mView.playVideo(mUri, mCurrentPosition);
                mView.hideLoadingProgress();
                return true;
            case MSG_GET_URI:
                mView.playVideo(mUri, 0);
                return true;
            default:
                break;
        }
        return false;
    }

    /**
     * 设置播放监听
     * @param listener
     */
    public void setPlayListener(PlayListener listener) {
        this.mPlayListener = listener;
    }

    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);

    }

    public void onResume() {
    }

    public void onDestroy() {
        if(null != mDownloader) {
            mDownloader.stop();
        }
    }

    private String formatDuration(final Context context, int durationMs) {
        int duration = durationMs / 1000;
        int h = duration / 3600;
        int m = (duration - h * 3600) / 60;
        int s = duration - (h * 3600 + m * 60);
        String durationValue;
        if (h == 0) {
            durationValue = String.format(context.getString(R.string.details_ms), m, s);
        } else {
            durationValue = String.format(context.getString(R.string.details_hms), h, m, s);
        }
        return durationValue;
    }
}
