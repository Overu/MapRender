package com.macrowen.macromap.utils;

import com.macrowen.macromap.draw.DrawMap;
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
import java.util.HashMap;
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

  private Map mMall;

  private HashMap<String, Map> mMap = new HashMap<String, Map>();
  private Handler mHandler = new Handler();
  private MapLoadStatusListener mMapLoadStatusListener;
  protected final Object mLock = new Object();
  private Floor mLockValue;
  private Shop nearbyShop;
  private OnMapFloorChangedListener mOnMapFloorChangedListener;

  private MapService() {
  }

  public void addColorConfigures(ColorConfigures colorConfigures) {
    DrawMap.mColorConfigures = colorConfigures;
  }

  public void addOffset(float x, float y) {
    if (mMall != null) {
      mMall.translate(x, y);
      // invalidate();
    }
  }

  public void addScale(float scale) {
    if (mMall != null) {
      mMall.scale(scale);
      // invalidate();
    }
  }

  public void destory() {
    mMall = null;
    mLockValue = null;
    System.gc();
  }

  public void flrushView() {
    mMall.setDelegate(null);
    mMall.setShopPosition(null);
    // getCurFloor().mPosition = null;
    mOnMapFloorChangedListener = null;
  }

  public Floor getCurFloor() {
    if (mMall == null) {
      return null;
    }
    return mMall.getCurFloor();
  }

  public String getFloorId() {
    if (mMall == null) {
      return null;
    }
    return mMall.getFloorid();
  }

  public String getFloorname() {
    if (mMall == null) {
      return null;
    }
    return mMall.getFloorname();
  }

  public Map getMap() {
    return mMall;
  }

  public String getMapId() {
    if (mMall == null) {
      return null;
    }
    return mMall.getId();
  }

  public String getMapName() {
    if (mMall == null) {
      return null;
    }
    return mMall.getName();
  }

  public PointF getMapOffset() {
    return mMall.getMapOffset();
  }

  public float getMapScale() {
    return mMall.getMapScale();
  }

  public Shop getNearbyShop(float x, float y, int scope) {
    x = Math.abs(x);
    y = Math.abs(y);
    List<Shop> shops = this.getShopsByScope(x, y, scope == 0 ? 60 : scope);
    if (shops.size() == 0) {
      nearbyShop = null;
      return null;
    }
    // if (shops.size() == 1) {
    // Shop shop = shops.get(0);
    // if (shop.getName().equals("")) {
    // return null;
    // }
    // if (nearbyShop == null) {
    // nearbyShop = shop;
    // return nearbyShop;
    // } else if (nearbyShop == shop) {
    // return nearbyShop;
    // }
    // } else {
    PointF scalePoint = getCurFloor().scalePoint(x, -y);
    int logueBak = -1;
    int idx = 0;
    for (int i = 0; i < shops.size(); i++) {
      Shop shop = shops.get(i);
      if (shop.getName().equals("")) {
        continue;
      }
      if (shop.mBlockRegion.contains((int) scalePoint.x, (int) scalePoint.y)) {
        idx = i;
        break;
      }
      if (logueBak == -1) {
        logueBak = getLoue(shop, scalePoint);
        Log.w("logueBak", shop.getName() + "--" + logueBak + "");
        continue;
      }
      int logueDx = this.getLoue(shop, scalePoint);
      Log.w("logueBak", shop.getName() + "--" + logueBak + "");
      if (logueBak > logueDx) {
        idx = i;
      }
    }
    nearbyShop = shops.get(idx);
    return nearbyShop;
    // }
    // return null;
  }

  public PointF getPostition() {
    return mMall.mPosition;
  }

  public ShopPosition getShopPosition() {
    return mMall.getShopPosition();
  }

  public List<Shop> getShopsByScope(float x, float y, int scope) {
    Floor floor = getCurFloor();
    if (floor == null) {
      return null;
    }
    PointF scalePoint = floor.scalePoint(x, -y);
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
      // Log.w("mBlockRegion", "false:" + shop.mBlockRegion.toString() + "--" +
      // shop.mDisplay);
      // Log.w("region1111", region.toString());
      if (region.quickReject(shop.mBlockRegion)) {
        // Log.w("centerX", shop.mBlockRegion.getBounds().centerX() + "");
        // Log.w("centerX", shop.mBlockRegion.getBounds().centerY() + "");
        // Log.w("region", region.toString());
        continue;
      }
      // Shop shopbak = new Shop();
      // shopbak.setId(shop.getId());
      // shopbak.setName(shop.getName());
      // shopbak.setType(shop.getType());
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
    Map map = mMap.get(mapId);
    if (map == null) {
      map = new Map();
      map.setId(mapId);
      map.setName(mapName);
      map.setMapName(mapName);
      mMall = map;
      mMap.put(mapId, map);
      loadMapData(mapId);
    } else {
      mMall = map;
    }
    if (map.getCurFloor() != null) {
      mMapLoadStatusListener.onMapLoadStatusEvent(MapLoadStatus.MapDataLoaded, map);
    }
  }

  public void parseMapData(final Canvas canvas) {
    mMall.parseMapData(canvas);
  }

  public void reDraw() {
    if (mMall == null) {
      return;
    }
    mMall.reDraw();
  }

  public void reDraw(boolean reDraw) {
    if (mMall == null) {
      return;
    }
    mMall.reDraw(reDraw);
  }

  public void reStory() {

  }

  public void setDelegateMeasure(int width, int height) {
    mMall.setDelegateMeasure(width, height);
  }

  public int setFloor(String id) {
    String from = mMall.getFloorid();
    if (mMall == null || id == null) {
      return -1;
    }
    // getCurFloor().mPosition = null;
    int idx = mMall.setFloor(id) == -2 ? loadFloorData(mMall.getId(), id) : 1;
    if (!id.equals(from)) {
      if (idx == 1) {
        this.completeData();
      }
      if (mOnMapFloorChangedListener != null) {
        mOnMapFloorChangedListener.OnMapFloorChanged(from, id);
      }
    }
    return idx;
  }

  public void setMapOffset(float x, float y) {
    mMall.setMapOffset(x, y);
  }

  public void setMapScale(float scale) {
    mMall.setMapScale(scale);
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
    int idx = setFloor(floorid);
    mMall.position(x, -y);
    if (mMall != null && idx != 0) {
      mMall.reDraw();
      mMall.delegateRefush();
    }
  }

  public void setPositionLazy(String floorid, float x, float y) {
    setFloor(floorid);
    mMall.position(x, -y);
  }

  public void setPositionTest(String floorid, float x, float y) {
    int idx = setFloor(floorid);
    // Log.w("setPositionTest", x + "-" + y);
    // float dx = (float) ((x + mapOffset.x) / 59.055) * 150;
    // float dy = (float) ((y + mapOffset.y) / 59.055) * 150;
    mMall.position(x, -y);
    if (mMall != null && idx != 0) {
      mMall.reDraw();
      mMall.delegateRefush();
    }
  }

  public void setShopPosition(ShopPosition shopPosition) {
    mMall.setShopPosition(shopPosition);
  }

  public void setViewDelegate(View view) {
    if (mMall == null) {
      return;
    }
    mMall.setDelegate(view);
  }

  public void showShopPosition(float x, float y) {
    mMall.showShopPosition(x, y);
  }

  public void zoomin() {
    addScale(2f);
    mMall.reDraw();
    mMall.delegateRefush();
  }

  public void zoomout() {
    addScale(0.5f);
    mMall.reDraw();
    mMall.delegateRefush();
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
      mMall.setFloor(id, name, index);
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
      final Map mall = mMap.get(mapId);
      mall.setData(new com.macrowen.macromap.draw.data.JSONArray(obj));
      setFloor(obj);
      synchronized (mLock) {
        if (mLockValue == null) {
          mLock.wait();
        }
        mHandler.post(new Runnable() {
          public void run() {
            if (mMapLoadStatusListener != null) {
              mMapLoadStatusListener.onMapLoadStatusEvent(MapLoadStatus.MapDataInit, mall);
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
    if (mMall.getDelegate() != null) {
      mHandler.post(new Runnable() {
        public void run() {
          mMall.reDraw();
          addScale(1);
        }
      });
    }
    return 1;
  }

  private int getLoue(Shop shop, PointF scalePoint) {
    Rect rect = shop.mBlockRegion.getBounds();
    int logue = this.pointToPoint((int) scalePoint.x, (int) scalePoint.y, rect.centerX(), rect.centerY());
    return logue;
  }

  private int pointToPoint(int x1, int y1, int x2, int y2) {
    return Math.abs((int) Math.sqrt((x1 - x2) ^ 2 + (y1 - y2) ^ 2));
  }
}
