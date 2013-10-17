package com.macrowen.macromap.draw;

import org.json.JSONArray;

import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.Paint.Align;

import android.graphics.Canvas;

public class Dot extends DrawLayer<JSONArray> {

  @Override
  public void onDrawBlock(Canvas canvas) {
  }

  @Override
  public void onDrawLine(Canvas canvas) {
  }

  @Override
  public void onDrawPath(JSONArray jsonArray) {
    mStart = getPoint(jsonArray);
  }

  @Override
  public void onDrawText(Canvas canvas) {
    if (mDrawType == DrawType.NoDraw) {
      return;
    }
    String text = null;
    if (mPublicServiceIcons != null) {
      text = mPublicServiceIcons.get(mType);
    }
    // logd("mType=" + mType + ", text=" + text);
    if (text == null) {
      // logd("text=" + text);
      mDrawType = DrawType.NoDraw;
      return;
    }
    if (mDrawType == DrawType.Draw) {
      float size = 400 * mScale;
      size = Math.min(size, 120);
      // if (mHighlight) {
      // size = Math.max(size, 32);
      // }
      if (size < mMiniumSize) {
        // logd("size=" + size);
        mDrawType = DrawType.NoDraw;
        return;
      }
      float x = mStart.x + mOffset.x;
      float y = mStart.y + mOffset.y;
      x = x * mScale + delegateWidth / 2 * (1 - mScale);
      y = y * mScale + delegateHeight / 2 * (1 - mScale);
      if (x < -delegateWidth / 3 || x > delegateWidth * 4 / 3 || y < -delegateHeight / 3 || y > delegateHeight * 4 / 3) {
        mDrawType = DrawType.NoDraw;
        return;
      }
      mDrawTextSize = size;
      mDrawTextPoint = new PointF(x, y);
      mBlockRegion = new Region();
      mBlockRegion.set((int) (mDrawTextPoint.x - size / 2), (int) (mDrawTextPoint.y - size / 2), (int) (mDrawTextPoint.x + size / 2),
          (int) (mDrawTextPoint.y + size / 2));
    }
    Paint paint = mPaintText;
    paint.setColor(mTextColor);
    if (mTypeface != null) {
      paint.setTypeface(mTypeface);
    }
    paint.setTextSize(mDrawTextSize);
    paint.setTextAlign(Align.CENTER);
    canvas.drawText(text, mDrawTextPoint.x, mDrawTextPoint.y + mDrawTextSize * 0.4f, paint);
    mDrawType = DrawType.ReDraw;
  }

  @Override
  public void onInfo(JSONArray jsonArray) {
    mDisplay = jsonArray.optString(0);
    mType = jsonArray.optString(1);
  }
}
