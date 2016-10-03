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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;


public class VideoPlayer implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final int HALF_MINUTE = 30 * 1000;
    private static final int TWO_MINUTES = 4 * HALF_MINUTE;

    private final VideoView mVideoView;
    private final View mProgressView;
    private final Uri mUri;
    private final ContentResolver mContentResolver;

    private PlayListener mPlayListener = null;

    private final Handler mHandler = new Handler();

    private final Runnable mPlayingChecker = new Runnable() {
        public void run() {
            if (mVideoView.isPlaying()) {
                mProgressView.setVisibility(View.GONE);
            } else {
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };

    public VideoPlayer(View rootView, Context context, Uri videoUri) {
        mContentResolver = context.getContentResolver();
        mVideoView = (VideoView) rootView.findViewById(R.id.video_player_surface_view);
        mProgressView = rootView.findViewById(R.id.video_player_progress_indicator);

        mUri = videoUri;

        // For streams that we expect to be slow to start up, show a
        // progress spinner until playback starts.
        String scheme = mUri.getScheme();
        if (null != scheme && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")
                || scheme.equalsIgnoreCase("ftp") || "rtsp".equalsIgnoreCase(scheme))) {
            mHandler.postDelayed(mPlayingChecker, 250);
        } else {
            mProgressView.setVisibility(View.GONE);
        }

        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setVideoURI(mUri);
        mVideoView.setMediaController(new MediaController(context));

        // make the video view handle keys for seeking and pausing
        mVideoView.requestFocus();

//        final Integer bookmark = getBookmark();
//        if (bookmark != null) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(context);
//            builder.setTitle(R.string.resume_playing_title);
//            builder.setMessage(String
//                            .format(context.getString(R.string.resume_playing_message),
//                                    formatDuration(context, bookmark)));
//            builder.setOnCancelListener(new OnCancelListener() {
//                public void onCancel(DialogInterface dialog) {
//                    if(null != mPlayListener) {
//                        mPlayListener.onCompletion();
//                    }
//                }
//            });
//            builder.setPositiveButton(R.string.resume_playing_resume, new OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    mVideoView.seekTo(bookmark);
//                    mVideoView.start();
//                }
//            });
//            builder.setNegativeButton(R.string.resume_playing_restart, new OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    mVideoView.start();
//                }
//            });
//            builder.show();
//        } else {
//            mVideoView.start();
//        }
        mVideoView.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mHandler.removeCallbacksAndMessages(null);
        mProgressView.setVisibility(View.GONE);
        if(null != mPlayingChecker) {
            mPlayListener.onError(what, extra);
        }
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(null != mPlayListener) {
            mPlayListener.onCompletion();
        }
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
//            setBookmark(mVideoView.getCurrentPosition(), mVideoView.getDuration());
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
    }

//    private static boolean uriSupportsBookmarks(Uri uri) {
//        String scheme = uri.getScheme();
//        String authority = uri.getAuthority();
//        return ("content".equalsIgnoreCase(scheme) && MediaStore.AUTHORITY.equalsIgnoreCase(authority));
//    }

//    private Integer getBookmark() {
//        if (!uriSupportsBookmarks(mUri)) {
//            return null;
//        }
//
//        String[] projection = new String[] { Video.VideoColumns.DURATION, Video.VideoColumns.BOOKMARK };
//
//        try {
//            Cursor cursor = mContentResolver.query(mUri, projection, null, null, null);
//            if (cursor != null) {
//                try {
//                    if (cursor.moveToFirst()) {
//                        int duration = getCursorInteger(cursor, 0);
//                        int bookmark = getCursorInteger(cursor, 1);
//                        if ((bookmark < HALF_MINUTE) || (duration < TWO_MINUTES)
//                                || (bookmark > (duration - HALF_MINUTE))) {
//                            return null;
//                        }
//                        return Integer.valueOf(bookmark);
//                    }
//                } finally {
//                    cursor.close();
//                }
//            }
//        } catch (SQLiteException e) {
//            // ignore
//        }
//
//        return null;
//    }

    private static int getCursorInteger(Cursor cursor, int index) {
        try {
            return cursor.getInt(index);
        } catch (SQLiteException e) {
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }

    }

//    private void setBookmark(int bookmark, int duration) {
//        if (!uriSupportsBookmarks(mUri)) {
//            return;
//        }
//
//        ContentValues values = new ContentValues();
//        values.put(Video.VideoColumns.BOOKMARK, Integer.toString(bookmark));
//        values.put(Video.VideoColumns.DURATION, Integer.toString(duration));
//        try {
//            mContentResolver.update(mUri, values, null, null);
//        } catch (SecurityException ex) {
//            // Ignore, can happen if we try to set the bookmark on a read-only
//            // resource such as a video attached to GMail.
//        } catch (SQLiteException e) {
//            // ignore. can happen if the content doesn't support a bookmark
//            // column.
//        } catch (UnsupportedOperationException e) {
//            // ignore. can happen if the external volume is already detached.
//        }
//    }

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
