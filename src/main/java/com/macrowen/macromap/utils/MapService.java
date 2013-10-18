package com.macrowen.macromap.utils;

import com.macrowen.macromap.draw.Floor;
import com.macrowen.macromap.draw.Map;
import com.macrowen.macromap.draw.Shop;
import com.macrowen.macromap.draw.ShopPosition;
import com.macrowen.macromap.draw.ShopPosition.OnMapEventListener;

import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import android.graphics.Region;

import android.graphics.Rect;

import android.graphics.PointF;

import android.graphics.Canvas;

import android.view.View;

import android.os.Handler;
import android.os.Environment;
import android.util.Base64;

public class MapService {

  public enum MapLoadStatus {
    MapDataLoaded, MapDataInit
  }

  public interface MapLoadStatusListener {
    public void onMapLoadStatusEvent(MapLoadStatus mapLoadStatus, Map map);
  }

  public interface OnMapFloorChangedListener {
    public void OnMapFloorChanged(String fromFloorid, String toFloorid);
  }

  class DownloadJson implements Runnable {

    File mFile;
    String mFloorid;
    String mMallid;
    String mUrl;

    DownloadJson(String mallid, String url) {
      mMallid = mallid;
      mFloorid = null;
      mUrl = url;
    }

    DownloadJson(String mallid, String floorid, String url) {
      mMallid = mallid;
      mFloorid = floorid;
      mUrl = url;
    }

    public void run() {
      try {
        mFile = Environment.getExternalStorageDirectory();
        mFile = new File(mFile, "/Palmap/MacroMap/" + Base64.encodeToString(mUrl.getBytes(), Base64.NO_WRAP));
        if (mFile.length() < 4) {
          mFile.getParentFile().mkdirs();
          mFile.createNewFile();
          downloadJson(mUrl, mFile);
        }
        if (mFloorid != null) {
          setFloorData(mMallid, mFloorid, mFile);
        } else {
          setMapJson(mMallid, mFile);
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }

  public static MapService INSTANCE = null;

  public static MapService getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new MapService();
    }
    return INSTANCE;
  }

  private Map mMap;
  private Handler mHandler = new Handler();
  private MapLoadStatusListener mMapLoadStatusListener;
  protected final Object mLock = new Object();
  private Floor mLockValue;

  private OnMapFloorChangedListener mOnMapFloorChangedListener;

  private MapService() {
  }

  public void addOffset(float x, float y) {
    if (mMap != null) {
      mMap.translate(x, y);
      // invalidate();
    }
  }

  public void addScale(float scale) {
    if (mMap != null) {
      mMap.scale(scale);
      // invalidate();
    }
  }

  public void destory() {
    mMap = null;
    mLockValue = null;
    System.gc();
  }

  public void flrushView() {
    mMap.setDelegate(null);
    mMap.setShopPosition(null);
    mOnMapFloorChangedListener = null;
  }

  public Floor getCurFloor() {
    if (mMap == null) {
      return null;
    }
    return mMap.getCurFloor();
  }

  public String getFloorId() {
    if (mMap == null) {
      return null;
    }
    return mMap.getFloorid();
  }

  public String getFloorname() {
    if (mMap == null) {
      return null;
    }
    return mMap.getFloorname();
  }

  public Map getMap() {
    return mMap;
  }

  public String getMapId() {
    if (mMap == null) {
      return null;
    }
    return mMap.getId();
  }

  public String getMapName() {
    if (mMap == null) {
      return null;
    }
    return mMap.getName();
  }

  public ShopPosition getShopPosition() {
    return mMap.getShopPosition();
  }

  public List<Shop> getShopsByScope(float x, float y, int scope) {
    Floor floor = getCurFloor();
    if (floor == null) {
      return null;
    }
    PointF scalePoint = mMap.scalePoint(x, -y);
    int intX = (int) scalePoint.x;
    int intY = (int) scalePoint.y;
    // Log.w("intX & intY", intX + " & " + intY);
    // int scaleScope = (int) Math.floor(mMap.scaleScope(scope));
    Rect rect = new Rect(intX - scope, intY - scope, intX + scope, intY + scope);
    // Log.w("scaleScope", scaleScope + "");
    Region region = new Region(rect);
    // Log.w("region rect", region.toString());
    List<Shop> shops = new ArrayList<Shop>();
    for (Shop shop : floor.getShops().values()) {
      if (shop.mBlockRegion == null) {
        // Log.w("mBlockRegion", "null");
        continue;
      }
      // Log.w("mBlockRegion", "false:" + shop.mBlockRegion.toString() + "--" + shop.mDisplay);
      // Log.w("region1111", region.toString());
      if (region.quickReject(shop.mBlockRegion)) {
        // Log.w("region", region.toString());
        continue;
      }
      shops.add(shop);
    }
    Log.w("shops count1111", floor.getShops().values().size() + "");
    Log.w("shops count", shops.size() + "");
    //
    // for (Shop shop : shops) {
    // Log.w("shop:", shop.getName());
    // }
    return shops;
  }

  public void initMapData(String mapId, String mapName) {
    if (mMap == null) {
      Map map = new Map();
      map.setId(mapId);
      map.setName(mapName);
      map.setMapName(mapName);
      mMap = map;
      loadMapData(mapId);
    }
    if (mMap.getCurFloor() != null) {
      mMapLoadStatusListener.onMapLoadStatusEvent(MapLoadStatus.MapDataLoaded, mMap);
    }
  }

  public void parseMapData(final Canvas canvas) {
    mMap.parseMapData(canvas);
  }

  public void reDraw() {
    if (mMap == null) {
      return;
    }
    mMap.reDraw();
  }

  public void reDraw(boolean reDraw) {
    if (mMap == null) {
      return;
    }
    mMap.reDraw(reDraw);
  }

  public void reStory() {

  }

  public void setDelegateMeasure(int width, int height) {
    mMap.setDelegateMeasure(width, height);
  }

  public int setFloor(String id) {
    String from = mMap.getFloorid();
    if (mMap == null || id == null) {
      return -1;
    }
    int idx = mMap.setFloor(id) == -2 ? loadFloorData(mMap.getId(), id) : this.completeData();
    if (!id.equals(from)) {
      getCurFloor().mPosition = null;
      if (mOnMapFloorChangedListener != null) {
        mOnMapFloorChangedListener.OnMapFloorChanged(from, id);
      }
    }
    return idx;
  }

  public void setOnMapEventListener(OnMapEventListener onMapEventListener) {
    if (getShopPosition() == null) {
      return;
    }
    getShopPosition().setOnMapEventListener(onMapEventListener);
  }

  public void setOnMapFloorChangedListener(OnMapFloorChangedListener onMapFloorChangedListener) {
    mOnMapFloorChangedListener = onMapFloorChangedListener;
  }

  public void setOnMapLoadStatusListener(MapLoadStatusListener mMapLoadStatusListener) {
    this.mMapLoadStatusListener = mMapLoadStatusListener;
  }

  public void setPosition(String floorid, float x, float y) {
    setFloor(floorid);
    if (mMap != null) {
      mMap.position(x, -y);
      mMap.reDraw();
      mMap.delegateRefush();
    }
  }

  public void setShopPosition(ShopPosition shopPosition) {
    mMap.setShopPosition(shopPosition);
  }

  public void setViewDelegate(View view) {
    if (mMap == null) {
      return;
    }
    mMap.setDelegate(view);
  }

  public void showShopPosition(float x, float y) {
    mMap.showShopPosition(x, y);
  }

  public void zoomin() {
    addScale(2f);
    mMap.reDraw();
    mMap.delegateRefush();
  }

  public void zoomout() {
    addScale(0.5f);
    mMap.reDraw();
    mMap.delegateRefush();
  }

  protected int downloadJson(String u, File file) {
    try {
      URL url = new URL(u);
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setRequestMethod("GET");
      urlConnection.setRequestProperty("Accept", "application/json");
      urlConnection.connect();
      InputStream inputStream = urlConnection.getInputStream();
      // int totalSize = urlConnection.getContentLength();
      int downloadedSize = 0;
      byte[] buffer = new byte[1024];
      int bufferLength = 0;
      FileOutputStream fileOutput = new FileOutputStream(file);
      while ((bufferLength = inputStream.read(buffer)) > 0) {
        fileOutput.write(buffer, 0, bufferLength);
        downloadedSize += bufferLength;
      }
      fileOutput.close();
      inputStream.close();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return 0;
  }

  protected int loadFloorData(String mallid, String floorid) {
    String url = "http://apitest.palmap.cn/mall/" + mallid + "/floor/" + floorid;
    new Thread(new DownloadJson(mallid, floorid, url)).start();
    return 0;
  }

  protected int loadMapData(String mallid) {
    String url = "http://apitest.palmap.cn/mall/" + mallid + "/floors";
    new Thread(new DownloadJson(mallid, url)).start();
    return 0;
  }

  protected int setFloor(JSONArray jsonArray) {
    String floorid = "";
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject json = jsonArray.optJSONObject(i);
      String id = json.optString("id");
      String name = json.optString("name");
      int index = json.optInt("index");
      if (index == 0) {
        floorid = id;
      }
      mMap.setFloor(id, name, index);
    }
    setFloor(floorid);
    return 0;
  }

  protected int setFloorData(String mapId, String floorid, File file) {
    try {
      FileInputStream input = new FileInputStream(file);
      byte[] buf = new byte[input.available()];
      input.read(buf);
      input.close();
      String json = EncodingUtils.getString(buf, "UTF-8");
      JSONObject obj = new JSONObject(json);
      getCurFloor().setData(new com.macrowen.macromap.draw.data.JSONObject(obj));
      synchronized (MapService.this.mLock) {
        mLockValue = getCurFloor();
        mLock.notifyAll();
      }
      this.completeData();
      return 0;
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  protected int setMapJson(String mapId, File file) {
    try {
      FileInputStream input = new FileInputStream(file);
      byte[] buf = new byte[input.available()];
      input.read(buf);
      input.close();
      String json = EncodingUtils.getString(buf, "UTF-8");
      JSONArray obj = new JSONArray(json);
      mMap.setData(new com.macrowen.macromap.draw.data.JSONArray(obj));
      setFloor(obj);
      synchronized (mLock) {
        if (mLockValue == null) {
          mLock.wait();
        }
        mHandler.post(new Runnable() {
          public void run() {
            if (mMapLoadStatusListener != null) {
              mMapLoadStatusListener.onMapLoadStatusEvent(MapLoadStatus.MapDataInit, mMap);
            }
          }
        });
      }
      return 0;
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  private int completeData() {
    if (mMap.getDelegate() != null) {
      mHandler.post(new Runnable() {
        public void run() {
          mMap.reDraw();
          addScale(1);
        }
      });
    }
    return 1;
  }
}
