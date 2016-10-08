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
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
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



    private static final int HALF_MINUTE = 30 * 1000;
    private static final int TWO_MINUTES = 4 * HALF_MINUTE;

    private final VideoView mVideoView;
    private final View mProgressView;
    private Uri mUri;
    /** 当前播放进度 **/
    private int mCurrentPosition = 0;
    /** 当前处于错误状态下 **/
    private boolean mOnError = false;

    private Downloader mDownloader;

    private PlayListener mPlayListener = null;

    private final Handler mHandler = new Handler(this);

    private final Runnable mPlayingChecker = new Runnable() {
        public void run() {
            if (mVideoView.isPlaying()) {
                mProgressView.setVisibility(View.GONE);
                if(null == mUri && null != mDownloader) {
                    mUri = Uri.fromFile(mDownloader.getSavedFile());
                }
            } else {
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };

    public VideoPlayer(View rootView, final Context context, Uri videoUri) {
        mVideoView = (VideoView) rootView.findViewById(R.id.video_player_surface_view);
        mProgressView = rootView.findViewById(R.id.video_player_progress_indicator);

        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setMediaController(new MediaController(context));

        // make the video view handle keys for seeking and pausing
        mVideoView.requestFocus();

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
                mProgressView.setVisibility(View.GONE);
                mUri = Uri.fromFile(cacheFile);
                mVideoView.setVideoURI(mUri);
                mVideoView.start();
            } else {
                // 网络视频，没有下载记录（未下载完成或者还没有开始下载）
                mHandler.postDelayed(mPlayingChecker, 250);
                DownloadLog log = DownloadDBUtils.getLogByUrl(context, url);
                if(null != log) {
                    cacheFile = new File(log.getSavedFile());
                    if(cacheFile.exists()) {
                        mUri = Uri.fromFile(cacheFile);
                        mVideoView.setVideoURI(mUri);
                        mVideoView.start();
                    } else {
                        // 缓存文件丢失，删除下载日志
                        DownloadDBUtils.deleteLog(context, url);
                    }
                }
                mDownloader = new Downloader(context, videoUri.toString(), context.getExternalCacheDir(), null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mDownloader.download(".mp4", new DownloadListener() {
                                @Override
                                public void onProgressUpdate(int downloadedSize, int totalSize) {
                                    Log.e("VideoPlayer", downloadedSize + " / " + totalSize);
                                    if(mOnError) {
                                        if(!buffer) {
                                            buffSize = downloadedSize;
                                            buffer = true;
                                        }
                                        if(downloadedSize - buffSize > 1024 * 1024) {
                                            if(null == mUri) {
                                                mUri = Uri.fromFile(mDownloader.getSavedFile());
                                            }
                                            if(null != mUri) {
                                                mHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mVideoView.setVideoURI(mUri);
                                                        mVideoView.seekTo(mCurrentPosition);
                                                        mVideoView.start();
                                                        mProgressView.setVisibility(View.GONE);
                                                    }
                                                });
                                            }
                                            buffer = false;
                                            mOnError = false;
                                        }
                                    } else {
                                        if(null == mUri) {
                                            mUri = Uri.fromFile(mDownloader.getSavedFile());
                                            if(null != mUri) {
                                                mHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mVideoView.setVideoURI(mUri);
                                                        mVideoView.start();
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

        } else {
            mProgressView.setVisibility(View.GONE);
            mUri = videoUri;
            mVideoView.setVideoURI(mUri);
            mVideoView.start();
        }
    }


    private boolean buffer = false;
    private long buffSize = 0;

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mHandler.removeCallbacksAndMessages(null);
        mProgressView.setVisibility(View.VISIBLE);
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
        Log.e("WWWWWWWWW", "onPrepared");
    }

    @Override
    public boolean handleMessage(Message msg) {
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
        if(null != mVideoView) {
            mVideoView.suspend();
        }
    }

    public void onResume() {
        if(null != mVideoView) {
            mVideoView.resume();
        }
    }

    public void onDestroy() {
        if(null != mVideoView) {
            mVideoView.stopPlayback();
        }
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
