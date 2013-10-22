package com.macrowen.macromap.draw;

import com.macrowen.macromap.draw.data.JSONData;

import java.util.HashMap;
import java.util.Map.Entry;

import android.view.View;

import android.widget.RelativeLayout;
import android.widget.FrameLayout.LayoutParams;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;

@SuppressLint("DrawAllocation")
public class Floor extends DrawLayer<JSONObject> {

  HashMap<PointF, Shop> mShops = new HashMap<PointF, Shop>();
  HashMap<PointF, PublicService> mPublicServices = new HashMap<PointF, PublicService>();

  private int mIndex;
  private String mAlias;
  private Canvas shopCanvas;

  private Canvas textCanvas;
  public ParseType mParseType = ParseType.Parse;

  public Shop mShop;

  public Floor(String id, String name, int index) {
    setId(id);
    setName(name);
    this.setIndex(index);

    mBorderSize = 3;
    mBorderColor = Color.BLUE;
  }

  @SuppressLint("WrongCall")
  public void drawLayer(DrawLayer draw) {
    // this.support(draw);
    draw.onDraw(shopCanvas);
    draw.onDraw(textCanvas);
    draw.onDraw(textCanvas);
  }

  public String getAlias() {
    return mAlias;
  }

  public int getIndex() {
    return mIndex;
  }

  public HashMap<PointF, Shop> getShops() {
    return mShops;
  }

  @Override
  public void onDraw(Canvas canvas) {
    if (mPath == null) {
      return;
    }
    Paint paint = new Paint();
    paint.setColor(Color.WHITE);
    canvas.drawPaint(paint);
    this.drawFloor(canvas);

    for (Entry<PointF, Shop> entry : mShops.entrySet()) {
      Shop value = entry.getValue();
      if (mDrawType == DrawType.Draw) {
        value.mDrawType = DrawType.Draw;
      }
      value.onDrawBlock(canvas);
    }

    for (Entry<PointF, Shop> entry : mShops.entrySet()) {
      Shop value = entry.getValue();
      value.onDrawLine(canvas);
    }

    for (Entry<PointF, Shop> entry : mShops.entrySet()) {
      Shop value = entry.getValue();
      value.onDrawText(canvas);
    }

    // if (mPosition != null) {
    // onDrawPosition(canvas);
    // }
    mParseType = ParseType.NoParse;
    if (shopPosition == null) {
      return;
    }
    mDrawType = DrawType.ReDraw;
    if (mShop != null
        && mShop.mBlockRegion != null
        && !(mShop.mDisplay == null || mShop.mDisplay.trim().equals("") || mShop.mDisplay
            .equalsIgnoreCase("null"))) {
      PointF p = mShop.mTextCenter;
      if (p == null) {
        p = new PointF(mShop.mRect.centerX(), mShop.mRect.centerY());
      }
      p = new PointF(p.x, p.y);
      p.offset(mOffset.x, mOffset.y);
      // mOffset.x, mOffset.y
      float x = (p.x - delegateWidth / 2) * mScale + delegateWidth / 2;
      float y = (p.y - delegateHeight / 2) * mScale + delegateHeight / 2;
      x = Math.max(x, shopPosition.getWidth() / 2);
      x = Math.min(x, delegateWidth - shopPosition.getWidth() / 2);
      y = Math.max(y, shopPosition.getHeight());
      y = Math.min(y, delegateHeight - shopPosition.getHeight());
      if (mShop.mBlockRegion.contains((int) x, (int) y)) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) shopPosition
            .getLayoutParams();
        params.leftMargin = (int) x - shopPosition.getWidth() / 2;
        params.topMargin = (int) y - shopPosition.getHeight();
        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;
        shopPosition.setShop(mShop);
        // shopPosition.setText(mShop.mDisplay);
        shopPosition.setLayoutParams(params);
        // mShopPosition.invalidate();
        // mShopPosition.setVisibility(VISIBLE);
        shopPosition.mShow = true;
      } else {
        shopPosition.setVisibility(View.INVISIBLE);
        shopPosition.mShow = false;
      }
    } else {
      // mButton.setVisibility(INVISIBLE);
    }

  }

  public void onDrawbak(Canvas canvas) {
    if (mPath == null) {
      return;
    }
    Paint paint = new Paint();
    paint.setColor(Color.WHITE);
    recycleBitmap(floorLayer);
    floorLayer = Bitmap.createBitmap(delegateWidth * 5 / 3,
        delegateHeight * 5 / 3, Config.ARGB_8888);
    Canvas c = new Canvas(floorLayer);
    c.translate(delegateWidth / 3, delegateHeight / 3);
    canvas.drawPaint(paint);
    this.drawFloor(c);
    canvas.drawBitmap(floorLayer, -delegateWidth / 3, -delegateHeight / 3,
        paint);

    this.recycleBitmap(shopLayer);
    shopLayer = Bitmap.createBitmap(delegateWidth * 5 / 3,
        delegateHeight * 5 / 3, Config.ARGB_8888);
    shopCanvas = new Canvas(shopLayer);
    shopCanvas.translate(delegateWidth / 3, delegateHeight / 3);

    this.recycleBitmap(textLayer);
    textLayer = Bitmap.createBitmap(delegateWidth * 5 / 3,
        delegateHeight * 5 / 3, Config.ARGB_8888);
    textCanvas = new Canvas(textLayer);
    textCanvas.translate(delegateWidth / 3, delegateHeight / 3);

    for (Entry<PointF, Shop> entry : mShops.entrySet()) {
      Shop value = entry.getValue();
      if (mDrawType == DrawType.Draw) {
        value.mDrawType = DrawType.Draw;
      }
      drawLayer(value);
    }

    for (Entry<PointF, PublicService> entry : mPublicServices.entrySet()) {
      PublicService value = entry.getValue();
      if (mDrawType == DrawType.Draw) {
        value.mDrawType = DrawType.Draw;
      }
      drawLayer(value);
    }
    canvas.drawBitmap(shopLayer, -delegateWidth / 3, -delegateHeight / 3, null);
    canvas.drawBitmap(textLayer, -delegateWidth / 3, -delegateHeight / 3, null);
  }

  @Override
  public void onInfo(JSONArray jsonArray) {
  }

  public void onDrawPositionLayer(Canvas canvas, boolean isReflush) {
    if (isReflush) {
      positionLayer = null;
      positionLayer = Bitmap.createBitmap(delegateWidth * 5 / 3,
          delegateHeight * 5 / 3, Config.ARGB_8888);
      positionCanvas = new Canvas(positionLayer);
      positionCanvas.translate(delegateWidth / 3, delegateHeight / 3);
    }
    positionLayer.eraseColor(Color.TRANSPARENT);
    onDrawPosition(positionCanvas);
//    if (isReflush) {
      canvas.drawBitmap(positionLayer, -delegateWidth / 3, -delegateHeight / 3,
          null);
//    }
  }

  public void onDrawPositionLayerTest() {
    mPosition.x += 1;
    mPosition.y += 1;
    Paint paint  = new Paint();
    paint.setColor(0xAA8888FF);
    PointF p = scalePoint(mPosition.x, mPosition.y);
    float x = p.x;
    float y = p.y;
//    positionCanvas = new Canvas(positionLayer);
    positionCanvas.drawCircle(x, y, 100, paint);
    onDrawPositionLayer(mainCanvas, false);
  }

  public void setAlias(String alias) {
    this.mAlias = alias;
  }

  @Override
  public void setData(JSONData<JSONObject> mData) {
    super.setData(mData);
    JSONObject json = mData.getData();
    setId(json.optString("id"));
    setName(json.optString("name"));
    setAlias(json.optString("alias"));
    setIndex(json.optInt("index"));

    json = json.optJSONObject("layers");

    JSONArray objs = json.optJSONArray("frame").optJSONArray(1);
    for (int i = 0; i < objs.length(); i++) {
      JSONArray obj = objs.optJSONArray(i);
      callPath(obj);
      mBorder = mRect;
    }

    objs = json.optJSONArray("shop").optJSONArray(1);
    for (int i = 0; i < objs.length(); i++) {
      JSONArray obj = objs.optJSONArray(i);
      Shop shop = new Shop();
      shop.setData(new com.macrowen.macromap.draw.data.JSONArray(obj));
      if (mShops.get(shop.mStart) != null) {
        shop.mStart.x += 0.01;
      }
      mShops.put(shop.mStart, shop);
    }

    objs = json.optJSONArray("public_service").optJSONArray(1);
    for (int i = 0; i < objs.length(); i++) {
      JSONArray obj = objs.optJSONArray(i);
      PublicService publicservice = new PublicService();
      publicservice.setData(new com.macrowen.macromap.draw.data.JSONArray(obj));
      mPublicServices.put(publicservice.mStart, publicservice);
    }
  }

  public void setIndex(int index) {
    this.mIndex = index;
  }

  @Override
  public void setPosition(float x, float y) {
    mPosition = new PointF(x, y);
  }

  @Override
  public Shop showShopPosition(float x, float y) {
    DrawMap<?> unit = null;
    if (unit == null) {
      for (Entry<PointF, PublicService> entry : mPublicServices.entrySet()) {
        PublicService u = entry.getValue();
        if (u.mBlockRegion != null && u.mBlockRegion.contains((int) x, (int) y)) {
          unit = u;
          mShop = null;
          break;
        }
      }
    }
    if (unit == null) {
      for (Entry<PointF, Shop> entry : mShops.entrySet()) {
        Shop u = entry.getValue();
        if (u.mBlockRegion != null && u.mBlockRegion.contains((int) x, (int) y)) {
          unit = u;
          if (mShop == u) {
            mShop = null;
          } else {
            mShop = u;
          }
          break;
        }
      }
    }

    if (unit == null) {
      if (this.mBlockRegion != null
          && this.mBlockRegion.contains((int) x, (int) y)) {
        unit = this;
        mShop = null;
      }
    }
    if (unit != null) {
      // unit.setHighlight(!unit.isHightlight());
      // invalidate();
      if (mShop != null
          && mShop.mBlockRegion != null
          && !(mShop.mDisplay == null || mShop.mDisplay.trim().equals("") || mShop.mDisplay
              .equalsIgnoreCase("null"))) {
        PointF p = mShop.mTextCenter;
        if (p == null) {
          p = new PointF(mShop.mRect.centerX(), mShop.mRect.centerY());
        }
        p = new PointF(p.x, p.y);
        p.offset(mOffset.x, mOffset.y);
        // mOffset.x, mOffset.y
        x = (p.x - delegateWidth / 2) * mScale + delegateWidth / 2;
        y = (p.y - delegateHeight / 2) * mScale + delegateHeight / 2;
        x = Math.max(x, shopPosition.getWidth() / 2);
        x = Math.min(x, delegateWidth - shopPosition.getWidth() / 2);
        y = Math.max(y, shopPosition.getHeight());
        y = Math.min(y, delegateHeight - shopPosition.getHeight());
        if (mShop.mBlockRegion.contains((int) x, (int) y)) {
          RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) shopPosition
              .getLayoutParams();
          params.leftMargin = (int) x - shopPosition.getWidth() / 2;
          params.topMargin = (int) y - shopPosition.getHeight();
          params.width = LayoutParams.WRAP_CONTENT;
          params.height = LayoutParams.WRAP_CONTENT;
          shopPosition.setShop(mShop);
          // shopPosition.setText(mShop.mDisplay);
          shopPosition.setLayoutParams(params);
          shopPosition.mShow = true;
          shopPosition.setVisibility(View.VISIBLE);
        } else {
          shopPosition.setVisibility(View.INVISIBLE);
          shopPosition.mShow = false;
        }
      } else {
        shopPosition.setVisibility(View.INVISIBLE);
        shopPosition.mShow = false;
      }
    }
    return mShop;
  }

  @Override
  public String toString() {
    return DrawLayer.mMapName + " 鈥斺� " + getName();
  }

  private void drawFloor(Canvas canvas) {
    this.onDrawBlock(canvas);
    this.onDrawLine(canvas);
  }

}
