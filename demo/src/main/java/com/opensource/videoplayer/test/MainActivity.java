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

package com.opensource.videoplayer.test;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.opensource.videoplayer.DownloadListener;
import com.opensource.videoplayer.Downloader;
import com.opensource.videoplayer.VideoActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Downloader d = new Downloader(MainActivity.this,
                "http://www.gzevergrandefc.com/UploadFile/photos/2013-06/fbb77294-6041-41ac-befa-37e237bd41f2.jpg",
                getExternalCacheDir(), null);

        findViewById(R.id.btn_local).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, VideoActivity.class);
                i.setData(Uri.parse("/storage/emulated/0/DCIM/Camera/VID_20160816_165126.mp4"));
//                i.setData(Uri.parse("/storage/sdcard1/DCIM/Camera/Video/VID_20151121_175527.mp4"));
//                i.setData(Uri.parse("/storage/emulated/0/Android/data/com.opensource.videoplayer.test/cache/FUKESI.mp4"));
                startActivity(i);
//                d.stop();
            }
        });


        findViewById(R.id.btn_network).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, VideoActivity.class);
//                i.setData(Uri.parse("http://172.16.10.84/test.mp4"));
                i.setData(Uri.parse("http://120.24.234.204/static/upload/video/FUKESI.mp4"));
                startActivity(i);
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        try {
//                            d.download(".jpg", new DownloadListener() {
//                                @Override
//                                public void onProgressUpdate(int downloadedSize, int totalSize) {
//                                    Log.e("AAAAAAAAAAAAAAAAAAAAA", downloadedSize + "<>" + totalSize);
//                                }
//                            });
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }).start();
            }
        });


    }
}
