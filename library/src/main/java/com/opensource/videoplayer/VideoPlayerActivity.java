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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

import com.opensource.videoplayer.presenter.VideoPlayerPresenter;
import com.opensource.videoplayer.view.IVideoPlayerView;

/**
 * This activity plays a video from a specified URI.
 */
public class VideoPlayerActivity extends Activity implements IVideoPlayerView {

    private VideoView mVideoView;
    private View mProgressView;

    private VideoPlayerPresenter mVideoPlayer;
    private boolean mFinishOnCompletion;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_video_player);

        mVideoView = (VideoView) findViewById(R.id.video_player_surface_view);
        mProgressView = findViewById(R.id.video_player_progress_indicator);

        mVideoView.setMediaController(new MediaController(this));
        // make the video view handle keys for seeking and pausing
        mVideoView.requestFocus();
        mVideoView.setKeepScreenOn(true);

        Intent intent = getIntent();

        mVideoPlayer = new VideoPlayerPresenter(this, this, intent.getData(), new VideoPlayListener() {
            @Override
            public void onCompletion() {
                if (mFinishOnCompletion) {
                    finish();
                }
            }

            @Override
            public void onError(int what, String msg) {

            }
        });

        if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = intent.getIntExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            //noinspection ResourceType
            if (orientation != getRequestedOrientation()) {
                //noinspection ResourceType
                setRequestedOrientation(orientation);
            }
        }
        mFinishOnCompletion = intent.getBooleanExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        Window window = getWindow();
        WindowManager.LayoutParams winParams = window.getAttributes();
        winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        window.setAttributes(winParams);
    }

    @Override
    public void onPause() {
        mVideoPlayer.onPause();
        if(null != mVideoView) {
            mVideoView.suspend();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        mVideoPlayer.onResume();
        if(null != mVideoView) {
            mVideoView.resume();
        }
        super.onResume();
    }
    
    @Override
    public void onDestroy() {
        mVideoPlayer.onDestroy();
        if(null != mVideoView) {
            mVideoView.stopPlayback();
        }
    	super.onDestroy();
    }

    @Override
    public void playVideo(Uri uri, int position) {
        if(null == mVideoView || null == uri) {
            return;
        }
        mVideoView.setVideoURI(uri);
        mVideoView.seekTo(position);
        mVideoView.start();
    }

    @Override
    public boolean isPlaying() {
        if(null == mVideoView) {
            return false;
        }
        return mVideoView.isPlaying();
    }

    @Override
    public void showLoadingProgress() {
        if(null == mProgressView) {
            return;
        }
        mProgressView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingProgress() {
        if(null == mProgressView) {
            return;
        }
        mProgressView.setVisibility(View.GONE);
    }
}
