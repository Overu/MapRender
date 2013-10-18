package com.macrowen.macromap.draw;

import com.macrowen.macromap.draw.data.JSONData;

import org.json.JSONArray;

import java.util.HashMap;

import android.util.Log;

import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.View;

public class DrawMap<T> {

  protected static HashMap<String, String> mPublicServiceIcons;
  protected static Typeface mTypeface;
  protected static String mMapName;
  protected static View delegate;
  protected static ShopPosition shopPosition;
  protected static int delegateWidth;
  protected static int delegateHeight;
  protected static Bitmap mainLayer;
  protected static Bitmap floorLayer;
  protected static Bitmap shopLayer;
  protected static Bitmap textLayer;
  protected static Paint mPaintBlock = new Paint();
  protected static Paint mPaintLine = new Paint();
  protected static Paint mPaintText = new Paint();
  public static PointF mOffset = new PointF(0, 0);
  public static float mScale = 0.01f;

  protected String mId;
  protected String mName;
  protected String mType;
  public String mDisplay = "";
  private JSONData<T> mData;

  public DrawType mDrawType = DrawType.Draw;

  protected boolean mRedraw = false;

  public Path mPath;
  public Path mTextPath;
  public Path mDrawPath;
  public Path mDrawTextPath;
  public Region mBlockRegion;
  public PointF mStart;
  public RectF mRect;
  public RectF mBorder = null;

  public PointF mDrawTextPoint;
  public PointF mPosition;

  public float mTextWidth;
  public PointF mTextCenter;
  public int mTextColor = Color.BLACK;
  public float mDrawTextSize;
  int mMiniumSize = 16;

  public float mLastScale = 1;
  public PointF mLastOffset = new PointF(0, 0);

  public int mMapMargin = 100;
  public int mBorderSize = 3;
  public int mFilledColor = Color.LTGRAY;
  public int mBorderColor = Color.BLACK;

  public void addOffset(float x, float y) {
    setOffset(mOffset.x + x / mScale, mOffset.y + y / mScale);
  }

  public void addScale(float scale) {
    setScale(scale * mScale);
  }

  public void callPath(JSONArray jsonArray) {
    if (jsonArray.length() == 2) {
      onInfo(jsonArray.optJSONArray(1));
      onDrawPath(jsonArray.optJSONArray(0));
    }
  }

  public void delegateRefush() {
    if (delegate == null) {
      return;
    }
    delegate.invalidate();
  }

  public float distance(PointF p, PointF q) {
    float x = p.x - q.x;
    float y = p.y - q.y;
    return x * x + y * y;
  }

  public JSONData<T> getData() {
    return mData;
  }

  public String getId() {
    return mId;
  }

  public String getName() {
    return mName;
  }

  public PointF getPoint(JSONArray jsonArray) {
    double x = jsonArray.optDouble(0);// * mScale;
    double y = -jsonArray.optDouble(1);// * mScale;
    PointF point = new PointF((float) x, (float) y);
    // point.offset(mOffset.x, mOffset.y);
    return point;
  }

  public String getType() {
    return mType;
  }

  public void onDraw(Canvas canvas) {
  }

  public void onDrawBlock(Canvas canvas) {
  }

  public void onDrawLine(Canvas canvas) {
  }

  public void onDrawPath(JSONArray jsonArray) {
  }

  public void onDrawPosition(Canvas canvas) {
  }

  public void onDrawText(Canvas canvas) {
  }

  public void onInfo(JSONArray jsonArray) {
  }

  public void recycleBitmap(Bitmap bitmap) {
    if (bitmap != null) {
      if (!bitmap.isRecycled()) {
        bitmap.recycle();
        bitmap = null;
        System.gc();
      }
    }
  }

  public void reDraw() {
    this.mRedraw = true;
  }

  public void reDraw(boolean reDraw) {
    this.mRedraw = reDraw;
  }

  public PointF scalePoint(float xed, float yed) {
    float x = xed + mOffset.x;
    float y = yed + mOffset.y;
    x = x * mScale + delegateWidth / 2 * (1 - mScale);
    y = y * mScale + delegateHeight / 2 * (1 - mScale);
    return new PointF(x, y);
  }

  public float scaleScope(int scope) {
    Log.w("mScale", mScale + "");
    return mScale * scope;
  }

  public void setData(JSONData<T> mData) {
    this.mData = mData;
  }

  public void setId(String mId) {
    this.mId = mId;
  }

  public void setName(String mName) {
    this.mName = mName;
  }

  public void setPosition(float x, float y) {
  }

  public void setType(String type) {
    this.mType = type;
  }

  public Shop showShopPosition(float x, float y) {
    return null;
  }

  // protected void support(DrawMap drawMap) {
  // drawMap.mOffset = this.mOffset;
  // drawMap.mScale = this.mScale;
  // }

  public boolean threepointsoneline(PointF p, PointF q, PointF r) {
    float a = (p.x - q.x) * (q.y - r.y);
    float b = (p.y - q.y) * (q.x - r.x);
    if (a == b) {
      // logd("a=" + a + ", b=" + b + p + q + r);
      return true;
    }
    return Math.abs((b + a) / (b - a)) > 5;
  }

  protected Canvas genLayer(Bitmap bitmap) {
    bitmap = Bitmap.createBitmap(delegateWidth * 5 / 3, delegateHeight * 5 / 3, Config.ARGB_8888);
    Canvas c = new Canvas(bitmap);
    c.translate(delegateWidth / 3, delegateHeight / 3);
    return c;
  }

  protected void setOffset(float x, float y) {
    if (mBorder == null) {
      return;
    }
    // float margin = 5 / 12f;
    // logd("mRect.width()=" + mRect.width() + ", getWidth()=" +
    // getWidth());
    if (mBorder.width() * mScale <= delegateWidth) {
      x = -mBorder.left + (delegateWidth - mBorder.width()) / 2;
    } else {
      // x = Math.min(x, -mBorder.left + getWidth() / 2 - getWidth() /
      // mScale * margin);
      // x = Math.max(x, -mBorder.right + getWidth() / 2 + getWidth() /
      // mScale * margin);
      x = Math.min(x, -mBorder.left + delegateWidth / 2 - mMapMargin / mScale);
      x = Math.max(x, -mBorder.right + delegateWidth / 2 + mMapMargin / mScale);
    }
    if (mBorder.height() * mScale <= delegateHeight) {
      y = -mBorder.top + (delegateHeight - mBorder.height()) / 2;
    } else {
      // y = Math.min(y, -mBorder.top + getHeight() / 2 - getHeight() /
      // mScale * margin);
      // y = Math.max(y, -mBorder.bottom + getHeight() / 2 + getHeight() /
      // mScale * margin);
      y = Math.min(y, -mBorder.top + delegateHeight / 2 - mMapMargin / mScale);
      y = Math.max(y, -mBorder.bottom + delegateHeight / 2 + mMapMargin / mScale);
    }
    mOffset = new PointF(x, y);
    mDrawType = DrawType.Draw;
    this.delegateRefush();
    // setPath();
  }

  protected void setScale(float scale) {
    if (mBorder == null) {
      return;
    }
    scale = Math.max(scale, Math.min((delegateWidth - mMapMargin) / mBorder.width(), (delegateHeight - mMapMargin) / mBorder.height()));
    if (Float.isInfinite(scale) || Float.isNaN(scale)) {
      return;
    }
    mScale = scale;
    setOffset(mOffset.x, mOffset.y);
  }
}
