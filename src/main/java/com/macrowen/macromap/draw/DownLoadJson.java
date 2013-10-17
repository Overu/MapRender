package com.macrowen.macromap.draw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;

public class DownLoadJson extends AsyncTask<String, Void, File> {

  public final static String BASEURL = "http://apitest.palmap.cn/mall/";

  public static int what;

  public static String getURLbyFloor(String mapId, String floodId) {
    what = 1;
    return BASEURL + mapId + "/floor/" + floodId;
  }

  public static String getURLbyMall(String mapId) {
    what = 0;
    return BASEURL + mapId + "/floors";
  }

  private Handler mHandler;

  public DownLoadJson(Handler handler) {
    this.mHandler = handler;
  }

  @Override
  protected File doInBackground(String... params) {
    try {
      URL url = new URL(params[0]);
      HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
      openConnection.setRequestMethod("GET");
      openConnection.setRequestProperty("Accept", "application/json");
      openConnection.connect();
      InputStream inputStream = openConnection.getInputStream();
      File file = Environment.getExternalStorageDirectory();
      file = new File(file, "/Palmap/MacroMap/" + Base64.encodeToString(params[0].getBytes(), Base64.NO_WRAP));
      byte[] buffer = new byte[1024];
      FileOutputStream out = new FileOutputStream(file);
      while (inputStream.read(buffer) > 0) {
        out.write(buffer);
      }
      inputStream.close();
      out.close();
      return file;
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  protected void onPostExecute(File result) {
    Message message = mHandler.obtainMessage();
    message.obj = result;
    message.what = what;
    mHandler.sendMessage(message);
  }

}
