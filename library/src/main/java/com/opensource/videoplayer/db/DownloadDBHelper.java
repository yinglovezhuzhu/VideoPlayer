package com.opensource.videoplayer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * 功能：下载日志数据库
 * @author yinglovezhuzhu@gmail.com
 *
 */
public class DownloadDBHelper extends SQLiteOpenHelper {
	
	private static final String DB_NAME = "download.db";
	
	private static final int DB_VERSION = 1;
	
	public static DownloadDBHelper mDBHelper = null;
	
	public DownloadDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	public static DownloadDBHelper getInstance(Context context) {
		if(mDBHelper == null) {
			mDBHelper = new DownloadDBHelper(context);
		}
		return mDBHelper;
	}
	
	public static SQLiteDatabase getReadableDatabase(Context context) {
		return new DownloadDBHelper(context).getReadableDatabase();
	}
	
	public static SQLiteDatabase getWriteableDatabase(Context context) {
		return new DownloadDBHelper(context).getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		//下载进度表
		db.execSQL("CREATE TABLE IF NOT EXISTS download_log(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT, downloaded_size INTEGER, total_size INTEGER, saved_file TEXT, end_downloaded INTEGER)");
		
		//下载历史
		db.execSQL("CREATE TABLE IF NOT EXISTS download_history(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT, total_size INTEGER, finished_time INTEGER, saved_file TEXT)");
		
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		dropAllTables(db);
		onCreate(db);
	}
	
	private void dropAllTables(SQLiteDatabase db) {
		db.execSQL("drop table if exists download_log");
		
		onCreate(db);
	}
}
