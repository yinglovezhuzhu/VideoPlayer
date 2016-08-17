package com.opensource.videoplayer;

/**
 * 播放监听
 * Created by yinglovezhuzhu@gmail.com on 2016/8/17.
 */
public interface PlayListener {
    public void onCompletion();

    public void onError(int what, int extra);
}
