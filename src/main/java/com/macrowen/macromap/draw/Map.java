package com.macrowen.macromap.draw;

import java.util.HashMap;

import android.graphics.Typeface;

import android.view.View;

import android.graphics.Rect;
import android.graphics.RectF;

import android.graphics.PointF;

import org.json.JSONArray;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;

@SuppressLint({ "DrawAllocation", "WrongCall" })
public class Map extends DrawMap<JSONArray> {

  private HashMap<String, Floor> floors;

  private Floor mCurFloor;

  // private Bitmap mMapBitmap;
  private Bitmap mBmp;

  public Map() {
    floors = new HashMap<String, Floor>();
  }

  public void addFloor(Floor floor) {
    getFloors().put(floor.getId(), floor);
  }

  public void addFloor(String id, String name, int index) {
    Floor floor = new Floor(id, name, index);
    getFloors().put(id, floor);
  }

  public Floor getCurFloor() {
    return mCurFloor;
  }

  public View getDelegate() {
    return DrawMap.delegate;
  }

  public String getFloorid() {
    if (mCurFloor == null) {
      return null;
    }
    return mCurFloor.getId();
  }

  public String getFloorname() {
    if (mCurFloor == null) {
      return null;
    }
    return mCurFloor.getName();
  }

  public HashMap<String, Floor> getFloors() {
    return floors;
  }

  public ShopPosition getShopPosition() {
    return DrawMap.shopPosition;
  }

  @Override
  public void onDraw(final Canvas canvas) {
    final Floor mFloor = this.getCurFloor();
    if (mFloor == null) {
      return;
    }
    Paint paint = new Paint();
    delegateHeight = delegate.getHeight();
    delegateWidth = delegate.getWidth();
    if (mRedraw) {
      mRedraw = false;
      paint.setColor(Color.WHITE);
      this.recycleBitmap(mBmp);
      mBmp = null;
      mBmp = Bitmap.createBitmap(delegateWidth * 5 / 3, delegateHeight * 5 / 3, Config.ARGB_8888);
      Canvas cc = new Canvas(mBmp);
      cc.drawPaint(paint);
      cc.translate(delegateWidth / 3, delegateHeight / 3);
      float scale = mScale;
      mFloor.mDrawType = DrawType.Draw;
      mScale = scale / 2;
      mFloor.onDraw(cc);
      mScale = scale;
      mFloor.mDrawType = DrawType.Draw;
      mainLayer = null;
      mainLayer = Bitmap.createBitmap(delegateWidth * 5 / 3, delegateHeight * 5 / 3, Config.ARGB_8888);
      final Canvas c = new Canvas(mainLayer);
      c.translate(delegateWidth / 3, delegateHeight / 3);
      mFloor.onDraw(c);
      canvas.drawBitmap(mainLayer, -delegateWidth / 3, -delegateHeight / 3, paint);
      mFloor.mLastScale = mScale;
      mFloor.mLastOffset = new PointF(mOffset.x, mOffset.y);
    } else {
      if (mBmp == null || mainLayer == null) {
        return;
      }
      {
        float lastScale = mFloor.mLastScale / 2;
        Rect rect = new Rect(0, 0, delegateWidth * 5 / 3, delegateHeight * 5 / 3);
        float x =
            (mOffset.x - mFloor.mLastOffset.x) * mScale - delegateWidth / 2 * (mScale - lastScale) / lastScale - delegateWidth / 3 * mScale
                / lastScale;
        float y =
            (mOffset.y - mFloor.mLastOffset.y) * mScale - delegateHeight / 2 * (mScale - lastScale) / lastScale - delegateHeight / 3
                * mScale / lastScale;
        RectF rectf = new RectF(x, y, x + delegateWidth * 5 / 3 * mScale / lastScale, y + delegateHeight * 5 / 3 * mScale / lastScale);
        canvas.drawBitmap(mBmp, rect, rectf, paint);
      }
      float w = 5;
      Rect rect = new Rect((int) w, (int) w, delegateWidth * 5 / 3 - (int) w, delegateHeight * 5 / 3 - (int) w);
      float x =
          (mOffset.x - mFloor.mLastOffset.x) * mScale - delegateWidth / 2 * (mScale - mFloor.mLastScale) / mFloor.mLastScale
              - delegateWidth / 3 * mScale / mFloor.mLastScale + w * mScale;
      float y =
          (mOffset.y - mFloor.mLastOffset.y) * mScale - delegateHeight / 2 * (mScale - mFloor.mLastScale) / mFloor.mLastScale
              - delegateHeight / 3 * mScale / mFloor.mLastScale + w * mScale;
      RectF rectf =
          new RectF(x, y, x + delegateWidth * 5 / 3 * mScale / mFloor.mLastScale - 2 * w * mScale, y + delegateHeight * 5 / 3 * mScale
              / mFloor.mLastScale - 2 * w * mScale);
      canvas.drawBitmap(mainLayer, rect, rectf, paint);
      if (shopPosition != null && shopPosition.mShow) {
        shopPosition.setVisibility(View.VISIBLE);
      }
    }
  }

  public void parseMapData(final Canvas canvas) {
    final Floor mFloor = this.getCurFloor();
    if (mFloor == null) {
      return;
    }
    if (mFloor.mParseType == ParseType.NoParse) {
      return;
    }
    scale(1);
    Paint paint = new Paint();
    mainLayer = null;
    mainLayer = Bitmap.createBitmap(delegateWidth * 5 / 3, delegateHeight * 5 / 3, Config.ARGB_8888);
    final Canvas c = new Canvas(mainLayer);
    c.translate(delegateWidth / 3, delegateHeight / 3);
    mFloor.onDraw(c);
    if (canvas != null) {
      canvas.drawBitmap(mainLayer, -delegateWidth / 3, -delegateHeight / 3, paint);
    }
    this.recycleBitmap(mainLayer);
  }

  public void position(float x, float y) {
    if (mCurFloor == null) {
      return;
    }
    mCurFloor.setPosition(x, y);
    mCurFloor.setOffset(-x + delegateWidth / 2, -y + delegateHeight / 2);
  }

  public void reStory() {
    mPublicServiceIcons = null;
    mTypeface = null;
    mMapName = "";
    delegate = null;
    delegateWidth = 0;
    delegateHeight = 0;
  }

  public void scale(float scale) {
    mCurFloor.addScale(scale);
  }

  public int setCurFloor(String id) {
    mCurFloor = getFloors().get(id);
    if (mCurFloor.getData() == null) {
      return -1;
    }
    return 0;
  }

  public void setDelegate(View delegate) {
    DrawMap.delegate = delegate;
  }

  public void setDelegateMeasure(int width, int height) {
    if (delegateHeight != 0 || delegateWidth != 0) {
      return;
    }
    delegateHeight = height;
    delegateWidth = width;
  }

  public int setFloor(String id) {
    Floor floor = floors.get(id);
    if (mCurFloor != null) {
      mCurFloor.mPosition = null;
    }
    if (floor == null) {
      return -1;
    } else {
      mCurFloor = floor;
    }
    if (mCurFloor.getData() == null) {
      return -2;
    } else {
      // if (delegate != null) {
      // delegateRefush();
      // }
    }
    // if (!id.equals(from)) {
    // mFloor.mPosition = null;
    // if (mOnMapFloorChangedListener != null) {
    // mOnMapFloorChangedListener.OnMapFloorChanged(from, id);
    // }
    // }
    return 0;
  }

  public int setFloor(String id, String name, int index) {
    if (id == null) {
      return -1;
    }
    if (id.equals(getFloorid()) && mCurFloor != null) {
      mCurFloor.setName(name);
      mCurFloor.setIndex(index);
    } else {
      Floor floor = floors.get(id);
      if (floor == null) {
        floor = new Floor(id, name, index);
        floors.put(id, floor);
      } else {
        floor.setName(name);
        floor.setIndex(index);
      }
      mCurFloor = floor;
    }
    return 0;
  }

  public void setMapName(String mapName) {
    DrawMap.mMapName = mapName;
  }

  public void setPublicService(HashMap<String, String> publicServiceIcons) {
    DrawMap.mPublicServiceIcons = publicServiceIcons;
  }

  public void setShopPosition(ShopPosition shopPoisiton) {
    DrawMap.shopPosition = shopPoisiton;
  }

  public void setTypaface(Typeface typeFace) {
    DrawMap.mTypeface = typeFace;
  }

  @Override
  public Shop showShopPosition(float x, float y) {
    return mCurFloor.showShopPosition(x, y);
  }

  public void translate(float x, float y) {
    mCurFloor.addOffset(x, y);
  }
}
