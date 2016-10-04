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
import android.util.Log;

import com.opensource.videoplayer.db.DownloadDBUtils;
import com.opensource.videoplayer.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * usage Downloader class
 * @author yinglovezhuzhu@gmail.com
 *
 */
public class Downloader {
	
	private static final String TAG = "DOWNLOADER";

    private static final int BUFFER_SIZE = 1024 * 1024 * 16;

	private static final int RESPONSE_OK = 200;
	private Context mContext;
	private boolean mStop = true; // The flag of stopped.
	private int mFileSize = 0; // The size of the file which to download.
    private File mSaveFolder;
	private String mFileName; // saveLog file name;
    private File mSavedFile = null;
	private DownloadLog mDownloadLog; // The data download state
	private String mUrl; // The url of the file which to download.
	
	private boolean mFinished = false;
	
	private boolean mBreakPointSupported = true;

	/**
	 * Constructor<br><br>
	 * @param context Context对象
	 * @param downloadUrl 下载地址
	 * @param saveFolder 保存目录
     * @param fileName 保存文件名称，可以为null，如果为null，将从服务器解析文件名，如果解析失败，则随机生成一个文件名称
	 */
	public Downloader(Context context, String downloadUrl, File saveFolder, String fileName) {
        this.mContext = context;
        this.mUrl = downloadUrl;
        this.mSaveFolder = saveFolder;
        this.mFileName = fileName;

        checkDownloadFolder(saveFolder);
    }

	/**
	 * Constructor<br><br>
	 * @param context Context对象
	 * @param downloadUrl 下载地址
	 * @param saveFolder 保存目录
     * @param fileName 保存文件名称，可以为null，如果为null，将从服务器解析文件名，如果解析失败，则随机生成一个文件名称
     * @param breakPointSupported 是否启用断点
	 */
	public Downloader(Context context, String downloadUrl, File saveFolder,
                      String fileName, boolean breakPointSupported) {
        this.mContext = context;
        this.mUrl = downloadUrl;
        this.mSaveFolder = saveFolder;
        this.mFileName = fileName;
        this.mBreakPointSupported = breakPointSupported;
        checkDownloadFolder(saveFolder);
    }

	/**
	 * Download file，this method has network, don't use it on ui thread.
	 * 
	 * @param listener The listener to listen download state, can be null if not need.
     * @param defaultSuffix 默认后缀，没有设置文件名的时候生效，带点，例如".mp4"
	 * @return The size that downloaded.
	 * @throws Exception The error happened when downloading.
	 */
	public File download(String defaultSuffix, DownloadListener listener) throws Exception {
        if(null != mDownloadLog && mDownloadLog.isLocked()) {
            Log.e(TAG, "File downloading now. url = " + mUrl);
            return mSavedFile;
        }
        mStop = false;
        if(mBreakPointSupported) {
            mDownloadLog = DownloadDBUtils.getLogByUrl(mContext, mUrl);
            if(null != mDownloadLog
                    && mDownloadLog.getDownloadedSize() == mDownloadLog.getTotalSize()) {
                mSavedFile = new File(mDownloadLog.getSavedFile());
                DownloadDBUtils.deleteLog(mContext, mUrl);
                DownloadDBUtils.saveHistory(mContext, mDownloadLog);
                mFinished = true;
                mDownloadLog.unlock();
                mStop = true;
                Log.w(TAG, "File download finished!");
                return mSavedFile;
            }
        }

        if(mFinished) {
            if(null != mDownloadLog) {
                mDownloadLog.unlock();
            }
            mStop = true;
            Log.w(TAG, "File download finished!");
            return mSavedFile;
        }

        HttpURLConnection conn = null;
        RandomAccessFile randomFile = null;

        if(null == mDownloadLog) {
            try {
                conn = getConnection(mUrl);
                if (conn.getResponseCode() == RESPONSE_OK) {
                    mFileSize = conn.getContentLength();
                    // Throw a RuntimeException when got file size failed.
                    if (mFileSize < 0) {
                        throw new RuntimeException("Can't get file size ");
                    }

                    if(StringUtils.isEmpty(mFileName)) {
                        final String filename = getFileName(conn, defaultSuffix);
                        // Create local file object according to local saved folder and local file name.
                        mSavedFile = new File(mSaveFolder, filename);
                    } else {
                        mSavedFile = new File(mSaveFolder, mFileName);
                    }

                    mDownloadLog = new DownloadLog(mUrl, 0, mFileSize, mSavedFile.getPath());
                    if(mBreakPointSupported) {
                        DownloadDBUtils.saveLog(mContext, mDownloadLog);
                    }
                    if (mDownloadLog.getDownloadedSize() == mFileSize) {
                        if(mBreakPointSupported) {
                            DownloadDBUtils.deleteLog(mContext, mUrl);// Delete download log when finished download
                        }
                        DownloadDBUtils.saveHistory(mContext, mDownloadLog);
                        mStop = true;
                        mFinished = true;
                        return mSavedFile;
                    }
                    mDownloadLog.lock();
                } else {
                    if(null != mDownloadLog) {
                        mDownloadLog.unlock();
                    }
                    Log.w(TAG, "Server response error! Response code：" + conn.getResponseCode()
                            + "Response message：" + conn.getResponseMessage());
                    throw new RuntimeException("server response error, response code:" + conn.getResponseCode());
                }
            } catch (Exception e) {
                if(null != mDownloadLog) {
                    mDownloadLog.unlock();
                }
                Log.e(TAG, e.toString());
                throw new RuntimeException("Failed to connect the url:" + mUrl, e);
            } finally {
                if(null != conn) {
                    conn.disconnect();
                }
                conn = null;
            }

            try {
                randomFile = new RandomAccessFile(mSavedFile, "rwd");
                if (mFileSize > 0) {
                    randomFile.setLength(mFileSize); // Set total size of the download file.
                }
            } catch (Exception e) {
                if(null != mDownloadLog) {
                    mDownloadLog.unlock();
                }
                Log.e(TAG, e.toString(), e);// 打印错误
                throw new Exception("Exception occur when downloading file\n", e);// Throw exception when some error happened when downloading.
            } finally {
                if(null != randomFile) {
                    try {
                        randomFile.close(); // Close the RandomAccessFile to make the settings effective
                        randomFile = null;
                    } catch (Exception e) {
                        Log.e(TAG, e.toString(), e);
                    }
                }
            }
        } else {
            mFileSize = mDownloadLog.getTotalSize();
            mSavedFile = new File(mDownloadLog.getSavedFile());
            mSaveFolder = mSavedFile.getParentFile();
            mFileName = mSavedFile.getName();
        }

        if(null != listener) {
            listener.onProgressUpdate(mDownloadLog.getDownloadedSize(), mDownloadLog.getTotalSize());
        }

        InputStream inStream = null;
        try {
            URL url = new URL(mUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6 * 1000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "*/*"); // accept all MIME-TYPE
            conn.setRequestProperty("Accept-Language", "zh-CN");
            conn.setRequestProperty("Referer", mUrl);
            conn.setRequestProperty("Charset", "UTF-8");

            // Get the position of this thread start to download.
            int startPos = mDownloadLog.getDownloadedSize();
            // Get the position of this thread end to download.
            int endPos = mDownloadLog.getTotalSize();

            //Setting the rage of the data, it will return exact realistic size automatically,
            // if the size set to be is lager then realistic size.
            conn.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);

            // Client agent
            conn.setRequestProperty("User-Agent",
                    "Mozilla/4.0 (compatible; MSIE 8.0;"
                            + " Windows NT 5.2; Trident/4.0;"
                            + " .NET CLR 1.1.4322;"
                            + " .NET CLR 2.0.50727;"
                            + " .NET CLR 3.0.04506.30;"
                            + " .NET CLR 3.0.4506.2152;"
                            + " .NET CLR 3.5.30729)");

            // Use long connection.
            conn.setRequestProperty("Connection", "Keep-Alive");
            // Get the input stream of the connection.
            inStream = conn.getInputStream();
            // Set local cache size
            byte[] buffer = new byte[BUFFER_SIZE];
            int offset = 0;
            Log.i(TAG, "Starts to download from position " + startPos);
            randomFile = new RandomAccessFile(mSavedFile, "rwd");
            // Make the pointer point to the position where start to download.
            randomFile.seek(startPos);
            // The data is written to file until user stop download or data is finished download.
            while (!mStop && (offset = inStream.read(buffer)) != -1) {
                randomFile.write(buffer, 0, offset);
                mDownloadLog.setDownloadedSize(mDownloadLog.getDownloadedSize() + offset);
                // Update the range of this thread to database.
                DownloadDBUtils.updateLog(mContext, mDownloadLog);
                if(null != listener) {
                    listener.onProgressUpdate(mDownloadLog.getDownloadedSize(), mDownloadLog.getTotalSize());
                }
            }
            this.mFinished = mDownloadLog.getDownloadedSize() == mDownloadLog.getTotalSize();

            if(null != mDownloadLog) {
                mDownloadLog.unlock();
            }
        } catch (Exception e) {
            if(null != mDownloadLog) {
                mDownloadLog.unlock();
            }
            Log.e(TAG, e.toString());// 打印错误
            throw new RuntimeException("Failed to download file from " + mUrl, e);
        } finally {
            if(null != randomFile) {
                try {
                    randomFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(null != inStream) {
                try {
                    inStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(null != conn) {
                conn.disconnect();
            }

        }
        return mSavedFile;
	}


	/**
	 * Get download state is finished or not.
	 * @return
	 */
	public boolean isFinished() {
		return mFinished;
	}

	/**
	 * Stop the download
	 */
	public synchronized void stop() {
		this.mStop = true;
        if(null != mDownloadLog) {
            mDownloadLog.unlock();
        }
	}

	/**
	 * Get download state is stopped or not.
	 * @return
	 */
	public synchronized boolean isStop() {
		return this.mStop;
	}

	/**
	 * Get total file size
	 * 
	 * @return
	 */
	public int getFileSize() {
		return mFileSize;
	}

	/**
	 * Get HttpConnection object
	 * @param downloadUrl the url to download.
	 * @return HttpConnection object
	 */
	private HttpURLConnection getConnection(String downloadUrl) throws IOException {
		URL url = new URL(downloadUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(5 * 1000);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Accept-Language", "zh-CN");
		conn.setRequestProperty("Referer", downloadUrl);
		conn.setRequestProperty("Charset", "UTF-8");
		// Set agent.
		conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; "
				+ "MSIE 8.0; Windows NT 5.2;"
				+ " Trident/4.0; .NET CLR 1.1.4322;"
				+ ".NET CLR 2.0.50727; " + ".NET CLR 3.0.04506.30;"
				+ " .NET CLR 3.0.4506.2152; " + ".NET CLR 3.5.30729)");
		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.connect();
		Log.i(TAG, getResponseHeader(conn));
		return conn;
	}
	
	/**
	 * Check the download folder, make new folder if it is not exist.
	 * @param folder 目录
	 */
	private void checkDownloadFolder(File folder) {
		if (!folder.exists() && !folder.mkdirs()) {
			throw new IllegalStateException("创建目录失败");
		}
	}
	
	/**
	 * Get file name
	 * @param conn HttpConnection object
     * @param defaultSuffix 默认后缀（后缀格式带点，如".mp4"）
	 * @return 文件名称，如果不能从网络解析，自动生成一个，后缀为指定的默认后缀
	 */
	private String getFileName(HttpURLConnection conn, String defaultSuffix) {
		String filename = mUrl.substring(mUrl.lastIndexOf("/") + 1);

		if (StringUtils.isEmpty(filename)) {// Get file name failed.
			for (int i = 0;; i++) { // Get file name from http header.
				String mine = conn.getHeaderField(i);
				if (mine == null)
					break; // Exit the loop when go through all http header.
				if ("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase(Locale.ENGLISH))) { // Get content-disposition header field returns, which may contain a file name
					Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase(Locale.ENGLISH)); // Using regular expressions query file name
					if (m.find()) {
						return m.group(1); // If there is compliance with the rules of the regular expression string
					}
				}
			}
			filename = UUID.randomUUID() + defaultSuffix;// A 16-byte binary digits generated by a unique identification number
			                                      // (each card has a unique identification number)
			                                      // on the card and the CPU clock as the file name
		}
		return filename;
	}

	/**
	 * Get HTTP response header field
	 * 
	 * @param http HttpURLConnection object
	 * @return HTTp response header field map.
	 */
	private static Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
		Map<String, String> header = new LinkedHashMap<String, String>();
		for (int i = 0;; i++) {
			String fieldValue = http.getHeaderField(i);
			if (fieldValue == null) {
				break;
			}
			header.put(http.getHeaderFieldKey(i), fieldValue);
		}
		return header;
	}

	/**
	 * Get HTTP response header field as a string
	 * @param conn HttpURLConnection object
     * @return HTTP response header field as a string
	 */
	private static String getResponseHeader(HttpURLConnection conn) {
		Map<String, String> header = getHttpResponseHeader(conn);
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : header.entrySet()) {
			String key = entry.getKey() != null ? entry.getKey() + ":" : "";
			sb.append(key + entry.getValue());
		}
		return sb.toString();
	}
}
