package com.macrowen.macromap.draw;

import com.macrowen.macromap.draw.data.JSONData;

import org.json.JSONArray;

import android.graphics.Typeface;
import android.graphics.Paint.Align;

import android.graphics.PointF;

import android.graphics.Paint.Style;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Region.Op;

import android.graphics.Canvas;

public class DrawLayer<T> extends DrawMap<T> {

  protected int mLayerIdx = 0;

  @Override
  public void onDraw(Canvas canvas) {
    switch (mLayerIdx) {
    case 0:
      onDrawBlock(canvas);
      mLayerIdx++;
      break;
    case 1:
      onDrawLine(canvas);
      mLayerIdx++;
      break;
    case 2:
      onDrawText(canvas);
      mLayerIdx = 0;
      break;
    default:
      break;
    }
  }

  @Override
  public void onDrawBlock(Canvas canvas) {
    if (mDrawType == DrawType.NoDraw) {
      return;
    }
    if (mDrawType == DrawType.Draw) {
      Path path = new Path(mPath);
      Matrix matrix = new Matrix();
      matrix.setTranslate(mOffset.x, mOffset.y);
      path.transform(matrix);
      float scale = mScale;
      matrix.setScale(scale, scale, delegateWidth / 2, delegateHeight / 2);
      path.transform(matrix);
      Rect rect = new Rect(-delegateWidth / 3, -delegateHeight / 3, delegateWidth * 4 / 3, delegateHeight * 4 / 3);
      RectF rectf = new RectF();
      path.computeBounds(rectf, false);
      Region region = new Region(rect);
      region.setPath(path, region);
      region.op(rect, region, Op.INTERSECT);
      if (region.isEmpty()) {
        mBlockRegion = null;
        mDrawType = DrawType.NoDraw;
        return;
      }
      mBlockRegion = region;
      path = mBlockRegion.getBoundaryPath();
      path.close();
      mDrawPath = path;
    }
    Paint paint = mPaintBlock;
    // paint.setAntiAlias(true);
    paint.setColor(mFilledColor);
    canvas.drawPath(mDrawPath, paint);
  }

  @Override
  public void onDrawLine(Canvas canvas) {
    if (mDrawType == DrawType.NoDraw) {
      return;
    }
    if (mDrawType == DrawType.Draw) {
      if (mBlockRegion == null) {
        return;
      }
    }
    Paint paint = mPaintLine;
    paint.setAntiAlias(true);
    paint.setStyle(Style.STROKE);
    paint.setStrokeWidth(mBorderSize);// * (float)
    // Math.sqrt(scale /
    // mScale));
    paint.setColor(mBorderColor);
    canvas.drawPath(mDrawPath, paint);
  }

  @Override
  public void onDrawPath(JSONArray jsonArray) {
    mPath = new Path();
    PointF point = new PointF(0, 0);
    int linecount = 0;
    PointF points[] = new PointF[jsonArray.length()];
    // int n = 0;
    int index = 0;
    float length = 0;
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONArray json = jsonArray.optJSONArray(i);
      String t = json.optString(0);
      if (t.equals("M")) {
        point = getPoint(json.optJSONArray(1));
        mStart = point;
        mPath.moveTo(point.x, point.y);
        points[0] = point;
      } else if (t.equals("Z")) {
        mPath.lineTo(mStart.x, mStart.y);
        mPath.close();
      } else if (t.equals("A")) {
        float rx = (float) json.optDouble(1);// * mScale;
        float ry = (float) json.optDouble(2);// * mScale;
        if (rx != ry) {
        }
        float rotation = (float) json.optDouble(3);
        if (rotation != 0) {
          continue;
        }
        PointF center = getPoint(json.optJSONArray(4));
        float startAngle = -(float) json.optDouble(5);
        float sweepAngle = -(float) json.optDouble(6);
        RectF oval = new RectF(center.x - rx, center.y - ry, center.x + rx, center.y + ry);
        linecount++;
        if (linecount > 0) {// && linecount < 6) {
          point =
              new PointF((float) (center.x + rx * Math.cos(startAngle + sweepAngle)), (float) (center.y + ry
                  * Math.sin(startAngle + sweepAngle)));
          points[linecount] = point;
          if (linecount > 1) {
            if (threepointsoneline(points[linecount], points[linecount - 1], points[linecount - 2])) {
              points[linecount - 1] = points[linecount];
              linecount--;
            }
          }
          if (linecount > 0) {
            float len = distance(points[linecount - 1], points[linecount]);
            if (len > length) {
              index = linecount;
              length = len;
            }
          }
        }
        startAngle = (float) (startAngle * 180 / Math.PI);
        sweepAngle = (float) (sweepAngle * 180 / Math.PI);
        // mPath.addOval(oval, Direction.CCW);
        mPath.arcTo(oval, startAngle, sweepAngle / 2);
        mPath.arcTo(oval, startAngle + sweepAngle / 2, sweepAngle / 2);
      } else if (t.equals("L")) {
        point = getPoint(json.optJSONArray(1));
        mPath.lineTo(point.x, point.y);
        linecount++;
        if (linecount > 0) {// && linecount < 6) {
          points[linecount] = point;
          if (linecount > 1) {
            if (threepointsoneline(points[linecount], points[linecount - 1], points[linecount - 2])) {
              points[linecount - 1] = points[linecount];
              linecount--;
              // logd("linecount=" + linecount);
            }
          }
          if (linecount > 0) {
            float len = distance(points[linecount - 1], points[linecount]);
            if (len > length) {
              index = linecount;
              length = len;
            }
          }
        }
      }
    }
    // logd("linecount=" + linecount);
    if (linecount == 6) {
      if (threepointsoneline(points[0], points[1], points[5])) {
        points[0] = points[5];
        linecount--;
      }
    }
    if (linecount == 5) {
      if (threepointsoneline(points[0], points[1], points[4])) {
        points[0] = points[4];
        linecount--;
      }
    }
    if (linecount == 4) {
      mTextPath = new Path();
      // logd("" + points[0] + points[1] + points[2] +
      // points[3] + points[4]);
      float x1, x2, y1, y2;
      if (distance(points[0], points[1]) < distance(points[1], points[2])) {
        x1 = (points[0].x + points[1].x) / 2;
        y1 = (points[0].y + points[1].y) / 2;
        x2 = (points[2].x + points[3].x) / 2;
        y2 = (points[2].y + points[3].y) / 2;
      } else {
        x1 = (points[2].x + points[1].x) / 2;
        y1 = (points[2].y + points[1].y) / 2;
        x2 = (points[0].x + points[3].x) / 2;
        y2 = (points[0].y + points[3].y) / 2;
      }
      if (x1 < x2 || (x1 == x2 && y1 < y2)) {
        mTextPath.moveTo(x1, y1);
        mTextPath.lineTo(x2, y2);
      } else {
        mTextPath.moveTo(x2, y2);
        mTextPath.lineTo(x1, y1);
      }
      mTextWidth = (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    } else if (index > 0 && (!(mDisplay == null || mDisplay.trim().equals("") || mDisplay.equalsIgnoreCase("null")))) {
      // logd("mDisplay=" + mDisplay);
      Path path = new Path();
      float x1, x2, y1, y2;
      x1 = points[index].x;
      y1 = points[index].y;
      x2 = points[index - 1].x;
      y2 = points[index - 1].y;
      path.moveTo(x1 - (y2 - y1) * 10, y1 + (x2 - x1) * 10);
      path.lineTo(x1 + (y2 - y1) * 10, y1 - (x2 - x1) * 10);
      path.lineTo(x2 + (y2 - y1) * 10, y2 - (x2 - x1) * 10);
      path.lineTo(x2 - (y2 - y1) * 10, y2 + (x2 - x1) * 10);
      path.lineTo(x1 - (y2 - y1) * 10, y1 + (x2 - x1) * 10);
      Path mp = new Path(mPath);
      Matrix matrix = new Matrix();
      matrix.setScale(0.1f, 0.1f);
      mp.transform(matrix);
      path.transform(matrix);
      RectF rf = new RectF();
      Rect rect = new Rect();
      path.computeBounds(rf, false);
      // logd("rectf=" + rf + ", x1=" + x1 + ", y1=" + y1 +
      // ", x2=" + x2 + ", y2=" + y2);
      rf.round(rect);
      Region rg = new Region(rect);
      rg.setPath(path, rg);
      path.close();
      mp.computeBounds(rf, false);
      // logd("rectf=" + rf + ", x1=" + x1 + ", y1=" + y1 +
      // ", x2=" + x2 + ", y2=" + y2);
      rf.round(rect);
      Region region = new Region(rect);
      region.setPath(mp, region);
      mp.close();
      rg.op(region, rg, Op.INTERSECT);
      rect = rg.getBounds();
      float dx = 1;
      float dy = 0;
      if (y2 - y1 < 0.01) {
        dy = 1;
      }
      Path p = new Path();
      p.moveTo(rect.centerX() - (x2 - x1) / 10, rect.centerY() - (y2 - y1) / 10);
      p.lineTo(rect.centerX() + (x2 - x1) / 10, rect.centerY() + (y2 - y1) / 10);
      p.lineTo(rect.centerX() + (x2 - x1) / 10 + dx, rect.centerY() + (y2 - y1) / 10 + dy);
      p.lineTo(rect.centerX() - (x2 - x1) / 10 + dx, rect.centerY() - (y2 - y1) / 10 + dy);
      rg = new Region(rect);
      rg.setPath(p, region);
      p.close();
      rg.op(region, rg, Op.INTERSECT);
      rect = rg.getBounds();
      // mTextPath = rg.getBoundaryPath();
      boolean k = (x2 - x1) * (y2 - y1) < 0;
      mTextPath = new Path();
      mTextPath.moveTo(rect.left, k ? rect.bottom : rect.top);
      mTextPath.lineTo(rect.right, k ? rect.top : rect.bottom);
      matrix.setScale(10, 10);
      mTextPath.transform(matrix);
      // rect = rg.getBounds();
      mTextWidth = 10 * (float) Math.sqrt(rect.width() * rect.width() + rect.height() * rect.height());
      mTextPath = null;
    }
    // mRegion = new Region();
    mRect = new RectF();
    mPath.computeBounds(mRect, true);
    if (mTextWidth < 0.01) {
      mTextPath = null;
    }
  }

  @Override
  public void onDrawPosition(Canvas canvas) {
    PointF p = scalePoint(mPosition.x, mPosition.y);
    float x = p.x;
    float y = p.y;
    if (x < -delegateWidth / 3 || x > delegateWidth * 4 / 3 || y < -delegateHeight / 3 || y > delegateHeight * 4 / 3) {
      return;
    }
    Paint paint = mPaintText;
    paint.setColor(0xAA8888FF);
    // paint.setAlpha(0xFF);
    canvas.drawCircle(x, y, 50, paint);
    // paint.setAlpha(0x80);
    paint.setColor(0xEE002266);
    canvas.drawCircle(x, y, 5, paint);
    paint.setStrokeWidth(2);
    paint.setColor(0xDD006688);
    paint.setStyle(Style.STROKE);
    canvas.drawCircle(x, y, 50, paint);
  }

  @Override
  public void onDrawText(Canvas canvas) {
    if (mDrawType == DrawType.NoDraw) {
      return;
    }
    if (mDisplay == null || mDisplay.trim().equals("") || mDisplay.equalsIgnoreCase("null")) {
      mDrawType = DrawType.ReDraw;
      return;
    }
    Paint paint = mPaintText;
    paint.setTypeface(Typeface.DEFAULT);
    if (mDrawType == DrawType.Draw) {
      mDrawType = DrawType.ReDraw;
      Region region = new Region(mBlockRegion);
      Rect rect = region.getBounds();
      if (!region.contains(rect.centerX(), rect.centerY())) {
        Rect r = new Rect(rect);
        Region rg = new Region(region);
        if (rect.width() > rect.height()) {
          r.right -= rect.width() / 2;
        } else {
          r.bottom -= rect.height() / 2;
        }
        rg.op(r, region, Op.INTERSECT);
        r = rg.getBounds();
        rect = r;
        region = rg;
      }
      float size = 100;
      Path path = new Path();
      paint.setTextSize(size);
      float width = paint.measureText(mDisplay);
      if (mTextPath == null) {
        PointF point = new PointF(rect.centerX(), rect.centerY());
        float w = rect.width();
        float h = rect.height();
        boolean dir = true;
        if (rect.width() < rect.height() * 0.9) {
          w = rect.height();
          h = rect.width();
          dir = false;
        }
        size = (float) Math.sqrt(size * w / width / 20) * 20;
        if (size > h * 0.9) {
          size = h * 0.9f;
        }
        Rect r;
        if (rect.width() > rect.height() * 0.9) {
          r = new Rect((int) rect.left, (int) (point.y - 1), (int) rect.right, (int) (point.y + 1));
        } else {
          r = new Rect((int) (point.x - 1), (int) rect.top, (int) (point.x + 1), (int) rect.bottom);
        }
        Region rg = new Region(region);
        rg.op(r, region, Op.INTERSECT);
        r = rg.getBounds();
        // logd("" + rect + r);
        rect = r;
        // region = rg;
        if (rect.width() > rect.height() * 0.9) {
          w /= rect.width();
        } else {
          w /= rect.height();
        }
        size = size / (float) Math.sqrt(w);
        point = new PointF(rect.centerX(), rect.centerY());
        paint.setTextSize(size);
        width = paint.measureText(mDisplay);
        if (rect.width() > rect.height() * 0.8) {
          r = new Rect((int) (point.x - width / 2), (int) (point.y - size * 4), (int) (point.x + width / 2), (int) (point.y + size * 4));
        } else {
          r = new Rect((int) (point.x - size * 4), (int) (point.y - width / 2), (int) (point.x + size * 4), (int) (point.y + width / 2));
        }
        rg = new Region(region);
        rg.op(r, region, Op.INTERSECT);
        r = rg.getBounds();
        // logd("" + rect + r);
        rect = r;
        region = rg;
        w = (rect.width() > rect.height() * 0.8) ? rect.width() : rect.height();
        width = paint.measureText(mDisplay);
        if (width > w) {
          size *= w / width;
        }
        point = new PointF(rect.centerX(), rect.centerY());
        if (dir) {
          path.moveTo(rect.left, point.y);
          path.lineTo(rect.right, point.y);
          h = rect.height();
        } else {
          path.moveTo(point.x, rect.top);
          path.lineTo(point.x, rect.bottom);
          h = rect.width();
        }
        if (size > h * 0.9) {
          size = h * 0.9f;
        }
        if (size < mMiniumSize) {
          mDrawTextSize = 0;
          return;
        }
      } else {
        path = new Path(mTextPath);
        Matrix matrix = new Matrix();
        matrix.setTranslate(mOffset.x, mOffset.y);
        path.transform(matrix);
        float scale = mScale;
        matrix.setScale(scale, scale, delegateWidth / 2, delegateHeight / 2);
        path.transform(matrix);
        size = (float) Math.sqrt(size * mTextWidth * scale / width / 20) * 20;
        if (size < mMiniumSize) {
          // logd("size=" + size);
          mDrawTextSize = 0;
          return;
        }
      }
      mDrawTextSize = size;
      mDrawTextPath = path;
    }
    if (mDrawTextSize < mMiniumSize) {
      return;
    }
    paint.setStrokeWidth(1);
    paint.setTextSize(mDrawTextSize);
    paint.setAntiAlias(true);
    paint.setStyle(Style.FILL);
    paint.setTextAlign(Align.CENTER);
    paint.setColor(mTextColor);
    canvas.drawTextOnPath(mDisplay, mDrawTextPath, 0, mDrawTextSize / 2.4f, paint);
  }

  @Override
  public void setData(JSONData<T> mData) {
    super.setData(mData);
    T data = mData.getData();
    if (data instanceof JSONArray) {
      callPath((JSONArray) data);
    }
  }
}
