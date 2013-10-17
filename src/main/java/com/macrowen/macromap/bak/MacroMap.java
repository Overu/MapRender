package com.macrowen.macromap.bak;

import com.macrowen.macromap.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

public class MacroMap extends ScrollView {

  public interface OnMapEventListener {
    public void OnMapEvent(int id, OnMapEventType type);
  }

  public enum OnMapEventType {
    MapClickedLeft, MapClickedRight, MapSelected, MapUnselected
  }

  public interface OnMapFloorChangedListener {
    public void OnMapFloorChanged(String fromFloorid, String toFloorid);
  }

  public class Poi {
    public int mId;
    public String mName;
    public boolean mSelected;
    public String mType;

    public Poi(int id, String name, String type, boolean selected) {
      mId = id;
      mName = name;
      mType = type;
      mSelected = selected;
    }
  }

  class ConfigureFile {
    JSONObject mConfigures;

    ConfigureFile(File file) {
      try {
        FileInputStream input = new FileInputStream(file);
        byte[] buf = new byte[input.available()];
        input.read(buf);
        input.close();
        String json = EncodingUtils.getString(buf, "UTF-8");
        mConfigures = new JSONObject(json).optJSONObject("style");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    Integer getColor(String cs) {
      Integer c = null;
      try {
        c = Integer.valueOf("0x" + cs, 16);
      } catch (Throwable e) {
        // logd(e);
      }
      return c;
    }

    String getFacilityConfigure(String name) {
      if (mConfigures == null) {
        return "";
      }
      return mConfigures.optJSONObject("facility").optString(name);
    }

    String getFrameConfigure(String name) {
      if (mConfigures == null) {
        return "";
      }
      return mConfigures.optJSONObject("frame").optString(name);
    }

    Integer getInt(String cs) {
      Integer c = null;
      try {
        c = Integer.valueOf(cs);
      } catch (Throwable e) {
        // logd(e);
      }
      return c;
    }

    String getShopCategoryConfigure(String name) {
      if (mConfigures == null || name == null) {
        return "";
      }
      JSONObject json = mConfigures.optJSONObject("shop").optJSONObject("category");
      if (json == null) {
        return "";
      }
      for (int i = 1; i < name.length(); i++) {
        String n = name.substring(0, i);
        String c = json.optString(n);
        if (c != null && !c.isEmpty()) {
          return c;
        }
      }
      return "";
    }

    String getShopConfigure(String name) {
      if (mConfigures == null) {
        return "";
      }
      return mConfigures.optJSONObject("shop").optString(name);
    }

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
        logd("url=" + mUrl + ", file=" + mFile.getAbsolutePath());
        if (mFile.length() < 4) {
          mFile.getParentFile().mkdirs();
          mFile.createNewFile();
          downloadJson(mUrl, mFile);
        }
        mHandler.post(new Runnable() {
          public void run() {
            if (mFloorid != null) {
              setJson(mMallid, mFloorid, mFile);
            } else {
              setJson(mMallid, mFile);
            }
          }
        });
      } catch (Throwable e) {
        logd(e);
      }
    }
  }

  enum DrawType {
    Draw, NoDraw, ReDraw
  }

  class Mall {
    class Floor {
      class Annotation extends Dot {

        String mAngle;

        // String mDisplay;

        Annotation(JSONArray json) {
          super(json);
          mTextColor = mEscalatorTextColor;
          mTextHighlightColor = mEscalatorTextHighlightColor;
        }

        @Override
        void setInfo(JSONArray jsonArray) {
          // logd("jsonArray=" + jsonArray);
          mAngle = jsonArray.optString(0);
          mDisplay = jsonArray.optString(1);
        }
      }

      class Assistant extends Block {

        // String mDisplay;
        String mType;

        Assistant(JSONArray json) {
          super(json);
          mBorderColor = mAssistantBorderColor;
          mBorderHightlightColor = mAssistantBorderHightlightColor;
          mFilledColor = mAssistantFilledColor;
          mFilledHightlightColor = mAssistantFilledHighlightColor;
          mTextColor = mAssistantTextColor;
          mBorderSize = mAssistantBorderSize;
        }

        @Override
        void setInfo(JSONArray jsonArray) {
          // logd("jsonArray=" + jsonArray);
          mDisplay = jsonArray.optString(0);
          mType = jsonArray.optString(1);
        }

      }

      class Block extends Unit {

        int mBorderColor = Color.BLACK;
        int mBorderHightlightColor = Color.BLUE;
        int mBorderSize = 3;
        int mFilledColor = Color.LTGRAY;
        int mFilledHightlightColor = 0x44FFFFFF;

        // Region mRegion;
        Path mPath;
        RectF mRect;
        PointF mTextCenter;
        Path mTextPath;
        float mTextWidth;

        Block(JSONArray json) {
          super(json);
          // logd("jsonArray=" + json);
        }

        float distance(PointF p, PointF q) {
          float x = p.x - q.x;
          float y = p.y - q.y;
          return x * x + y * y;
        }

        @Override
        void drawBlock(Canvas canvas) {
          if (mDrawType == DrawType.NoDraw) {
            return;
          }
          if (mDrawType == DrawType.Draw) {
            Path path = new Path(mPath);
            Matrix matrix = new Matrix();
            matrix.setTranslate(mOffset.x, mOffset.y);
            path.transform(matrix);
            float scale = mScale;
            matrix.setScale(scale, scale, getWidth() / 2, getHeight() / 2);
            path.transform(matrix);
            Rect rect = new Rect(-getWidth() / 3, -getHeight() / 3, getWidth() * 4 / 3, getHeight() * 4 / 3);
            RectF rectf = new RectF();
            path.computeBounds(rectf, false);
            Region region = new Region(rect);
            region.setPath(path, region);
            region.op(rect, region, Op.INTERSECT);
            if (region.isEmpty()) {
              mRegion = null;
              mDrawType = DrawType.NoDraw;
              return;
            }
            mRegion = region;
            path = mRegion.getBoundaryPath();
            path.close();
            mDrawPath = path;
          }
          Paint paint = mPaintBlock;
          // paint.setAntiAlias(true);
          paint.setColor(mHighlight ? mFilledHightlightColor : mFilledColor);
          canvas.drawPath(mDrawPath, paint);
        }

        @Override
        void drawLine(Canvas canvas) {
          if (mDrawType == DrawType.NoDraw) {
            return;
          }
          if (mDrawType == DrawType.Draw) {
            if (mRegion == null) {
              return;
            }
          }
          Paint paint = mPaintLine;
          paint.setAntiAlias(true);
          paint.setStyle(Style.STROKE);
          paint.setStrokeWidth(mBorderSize);// * (float)
          // Math.sqrt(scale /
          // mScale));
          paint.setColor(mHighlight ? mBorderHightlightColor : mBorderColor);
          canvas.drawPath(mDrawPath, paint);
        }

        @Override
        void drawText(Canvas canvas) {
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
            Region region = new Region(mRegion);
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
                r =
                    new Rect((int) (point.x - width / 2), (int) (point.y - size * 4), (int) (point.x + width / 2),
                        (int) (point.y + size * 4));
              } else {
                r =
                    new Rect((int) (point.x - size * 4), (int) (point.y - width / 2), (int) (point.x + size * 4),
                        (int) (point.y + width / 2));
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
                if (mHighlight) {
                  size = mMiniumSize;
                } else {
                  mDrawTextSize = 0;
                  return;
                }
              }
            } else {
              path = new Path(mTextPath);
              Matrix matrix = new Matrix();
              matrix.setTranslate(mOffset.x, mOffset.y);
              path.transform(matrix);
              float scale = mScale;
              matrix.setScale(scale, scale, getWidth() / 2, getHeight() / 2);
              path.transform(matrix);
              size = (float) Math.sqrt(size * mTextWidth * scale / width / 20) * 20;
              if (size < mMiniumSize) {
                // logd("size=" + size);
                if (mHighlight) {
                  size = mMiniumSize;
                } else {
                  mDrawTextSize = 0;
                  return;
                }
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
          paint.setColor(mHighlight ? mTextHighlightColor : mTextColor);
          canvas.drawTextOnPath(mDisplay, mDrawTextPath, 0, mDrawTextSize / 2.4f, paint);
        }

        @Override
        void setInfo(JSONArray jsonArray) {
          mDisplay = jsonArray.optString(0);
          JSONArray json = jsonArray.optJSONArray(1);
          if (json != null) {
            mTextCenter = getPoint(json);
          }
          mType = jsonArray.optString(3);
          mId = jsonArray.optInt(4);
        }

        @Override
        void setPath(JSONArray jsonArray) {
          // logd("mDisplay=" + mDisplay);
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
                logd("rx = " + rx + ", ry = " + ry);
              }
              float rotation = (float) json.optDouble(3);
              if (rotation != 0) {
                logd("rotation=" + rotation);
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

        boolean threepointsoneline(PointF p, PointF q, PointF r) {
          float a = (p.x - q.x) * (q.y - r.y);
          float b = (p.y - q.y) * (q.x - r.x);
          if (a == b) {
            // logd("a=" + a + ", b=" + b + p + q + r);
            return true;
          }
          return Math.abs((b + a) / (b - a)) > 5;
        }
      }

      class Dot extends Unit {

        Dot(JSONArray json) {
          super(json);
        }

        @Override
        void drawBlock(Canvas canvas) {
        }

        @Override
        void drawLine(Canvas canvas) {
        }

        @Override
        void drawText(Canvas canvas) {
          if (mDrawType == DrawType.NoDraw) {
            return;
          }
          String text = mPublicServiceIcons.get(mType);
          // logd("mType=" + mType + ", text=" + text);
          if (text == null) {
            // logd("text=" + text);
            mDrawType = DrawType.NoDraw;
            return;
          }
          if (mDrawType == DrawType.Draw) {
            float size = 400 * mScale;
            size = Math.min(size, 120);
            if (mHighlight) {
              size = Math.max(size, 32);
            }
            if (size < mMiniumSize) {
              // logd("size=" + size);
              mDrawType = DrawType.NoDraw;
              return;
            }
            float x = mStart.x + mOffset.x;
            float y = mStart.y + mOffset.y;
            x = x * mScale + getWidth() / 2 * (1 - mScale);
            y = y * mScale + getHeight() / 2 * (1 - mScale);
            if (x < -getWidth() / 3 || x > getWidth() * 4 / 3 || y < -getHeight() / 3 || y > getHeight() * 4 / 3) {
              mDrawType = DrawType.NoDraw;
              return;
            }
            mDrawTextSize = size;
            mDrawTextPoint = new PointF(x, y);
            mRegion = new Region();
            mRegion.set((int) (mDrawTextPoint.x - size / 2), (int) (mDrawTextPoint.y - size / 2), (int) (mDrawTextPoint.x + size / 2),
                (int) (mDrawTextPoint.y + size / 2));
          }
          Paint paint = mPaintText;
          paint.setColor(mHighlight ? mTextHighlightColor : mTextColor);
          paint.setTypeface(mTypeface);
          paint.setTextSize(mDrawTextSize);
          paint.setTextAlign(Align.CENTER);
          canvas.drawText(text, mDrawTextPoint.x, mDrawTextPoint.y + mDrawTextSize * 0.4f, paint);
          mDrawType = DrawType.ReDraw;
        }

        @Override
        void setInfo(JSONArray jsonArray) {
          // logd("jsonArray=" + jsonArray);
          mDisplay = jsonArray.optString(0);
          mType = jsonArray.optString(1);
        }

        @Override
        void setPath(JSONArray jsonArray) {
          // logd("jsonArray=" + jsonArray);
          mStart = getPoint(jsonArray);
        }
      }

      class Escalator extends Dot {

        // String mDisplay;
        String mDbid;
        String mName;
        String mTomap;
        String mTopoint;
        String mType;

        Escalator(JSONArray json) {
          super(json);
          mTextColor = mEscalatorTextColor;
          mTextHighlightColor = mEscalatorTextHighlightColor;
        }

        @Override
        void setInfo(JSONArray jsonArray) {
          // logd("jsonArray=" + jsonArray);
          mTomap = jsonArray.optString(0);
          mName = jsonArray.optString(1);
          mDisplay = jsonArray.optString(2);
          mDbid = jsonArray.optString(3);
          mType = jsonArray.optString(4);
          mTopoint = jsonArray.optString(5);
        }
      }

      class Frame extends Block {

        Frame(JSONArray json) {
          super(json);
          mBorderColor = mFrameBorderColor;
          mFilledColor = mFrameFilledColor;
          mBorderSize = mFrameBorderSize;
          Integer color = mBorderColors.get(mType);
          String cs = mConfigure.getFrameConfigure("fillColor");
          color = mConfigure.getColor(cs);
          if (color != null) {
            mFilledColor = color;
          }
          cs = mConfigure.getFrameConfigure("borderColor");
          color = mConfigure.getColor(cs);
          if (color != null) {
            mBorderColor = color;
          }
          cs = mConfigure.getFrameConfigure("borderWidth");
          color = mConfigure.getInt(cs);
          if (color != null) {
            mBorderSize = color;
          }
        }

        @Override
        void setInfo(JSONArray jsonArray) {
          // logd("jsonArray=" + jsonArray);
        }

        @Override
        void setPath(JSONArray jsonArray) {
          super.setPath(jsonArray);
          mBorder = mRect;
        }
      }

      class Line extends Unit {

        Line(JSONArray json) {
          super(json);
        }

        @Override
        void setInfo(JSONArray jsonArray) {
        }

        @Override
        void setPath(JSONArray jsonArray) {

        }
      }

      class MPath {
        class MShape {
          PointF mStart;

          MShape(PointF start) {
            mStart = start;
          }

          void setStart(PointF start) {
            mStart = start;
          }
        }

        class MShapeArc extends MShapeLine {

          float mAngleStart;
          float mAngleSweep;
          PointF mCenter;
          float mRadiusX;
          float mRadiusY;

          MShapeArc(PointF start) {
            super(start);
          }

          void arcTo(PointF center, float rx, float ry, float angleStart, float angleSweep) {
            mCenter = center;
            mRadiusX = rx;
            mRadiusY = ry;
            mAngleStart = angleStart;
            mAngleSweep = angleSweep;
          }
        }

        class MShapeDot extends MShape {

          MShapeDot(PointF start) {
            super(start);
          }
        }

        class MShapeLine extends MShape {

          PointF mStop;

          MShapeLine(PointF start) {
            super(start);
          }

          void lineTo(PointF stop) {
            mStop = stop;
          }
        }

        // Path mPath;
        List<MShape> mShapes;

        void lineTo(PointF point) {
          if (mShapes.size() < 1) {
            logd("point=" + point);
          }
          MShapeLine shape;
          MShape prev = mShapes.get(mShapes.size() - 1);
          if (prev instanceof MShapeDot) {
            MShapeDot dot = (MShapeDot) mShapes.remove(mShapes.size() - 1);
            shape = new MShapeLine(dot.mStart);
          } else if (prev instanceof MShapeLine) {
            MShapeLine line = (MShapeLine) prev;
            shape = new MShapeLine(line.mStop);
          } else if (prev instanceof MShapeArc) {
            MShapeLine line = (MShapeLine) prev;
            shape = new MShapeLine(line.mStop);

          } else {
            shape = new MShapeLine(new PointF(0, 0));
          }
          shape.lineTo(point);
        }

        void moveTo(PointF point) {
          MShapeDot shape = new MShapeDot(point);
          mShapes.add(shape);
        }
      }

      class PublicService extends Dot {

        // String mDisplay;
        String mDbid;
        String mName;
        String mType;

        PublicService(JSONArray json) {
          super(json);
          mTextColor = mPublicServiceTextColor;
          mTextHighlightColor = mPublicServiceTextHighlightColor;
          Integer color = mTextColors.get(mType);
          if (color != null) {
            mTextColor = color;
          }
        }
      }

      class Shop extends Block {

        // String mBooth;
        // int mId;
        String mName;

        // String mDisplay;
        // String mType;

        Shop(JSONArray json) {
          super(json);
          mName = mDisplay;
          mBorderColor = mShopBorderColor;
          mBorderHightlightColor = mShopBorderHightlightColor;
          mFilledColor = mShopFilledColor;
          mFilledColor =
              (int) Math.round(Math.random() * 32 + 224) + (int) Math.round(Math.random() * 32 + 224) * 256
                  + (int) Math.round(Math.random() * 32 + 224) * 256 * 256 + (int) Math.round(Math.random() * 32 + 224) * 256 * 256 * 256;
          mFilledHightlightColor = mShopFilledHighlightColor;
          mTextColor = mShopTextColor;
          mBorderSize = mShopBorderSize;
          Integer color = mBorderColors.get(mType);
          if (color != null) {
            mBorderColor = color;
          }
          color = mFilledColors.get(mType);
          if (color != null) {
            mFilledColor = color;
          }
          color = mTextColors.get(mType);
          if (color != null) {
            mTextColor = color;
          }
          String cs = mConfigure.getShopConfigure("fillColor");
          color = mConfigure.getColor(cs);
          if (color != null) {
            mFilledColor = color;
          }
          cs = mConfigure.getShopCategoryConfigure(mType);
          color = mConfigure.getColor(cs);
          if (color != null) {
            mFilledColor = color;
          }
          cs = mConfigure.getShopConfigure("textColor");
          color = mConfigure.getColor(cs);
          if (color != null) {
            mTextColor = color;
          }
          cs = mConfigure.getShopConfigure("borderColor");
          color = mConfigure.getColor(cs);
          if (color != null) {
            mBorderColor = color;
          }
          cs = mConfigure.getShopConfigure("borderWidth");
          color = mConfigure.getInt(cs);
          if (color != null) {
            mBorderSize = color;
          }
        }
      }

      class Unit {

        String mDisplay = "";
        Path mDrawPath;
        Path mDrawTextPath;
        PointF mDrawTextPoint;
        float mDrawTextSize;

        DrawType mDrawType = DrawType.Draw;
        boolean mHighlight = false;
        int mId;

        JSONArray mJson;
        Region mRegion;
        PointF mStart;
        int mTextColor = Color.BLACK;
        int mTextHighlightColor = Color.RED;

        String mType;

        Unit(JSONArray json) {
          mJson = json;
          // logd("json.length()=" + json.length() + ", json=" +
          // json);
          if (json.length() == 2) {
            setInfo(json.optJSONArray(1));
            setPath(json.optJSONArray(0));
          }
        }

        void drawBlock(Canvas canvas) {
        }

        void drawLine(Canvas canvas) {
        }

        void drawText(Canvas canvas) {
        }

        PointF getPoint(JSONArray jsonArray) {
          double x = jsonArray.optDouble(0);// * mScale;
          double y = -jsonArray.optDouble(1);// * mScale;
          PointF point = new PointF((float) x, (float) y);
          // point.offset(mOffset.x, mOffset.y);
          return point;
        }

        boolean isHightlight() {
          return mHighlight;
        }

        void setHighlight(boolean highlight) {
          mHighlight = highlight;
        }

        void setInfo(JSONArray jsonArray) {
        }

        void setPath(JSONArray jsonArray) {

        }
      }

      String mAlias;

      HashMap<PointF, Annotation> mAnnotations = new HashMap<PointF, Annotation>();

      HashMap<PointF, Assistant> mAssistants = new HashMap<PointF, Assistant>();

      RectF mBorder = null; // new RectF(0, 0, getWidth(), getHeight());

      PointF mDeltaOffset = new PointF(0, 0);

      float mDeltaScale = 1;
      DrawType mDrawType = DrawType.Draw;
      HashMap<PointF, Escalator> mEscalators = new HashMap<PointF, Escalator>();
      HashMap<PointF, Frame> mFrames = new HashMap<PointF, Frame>();

      String mId;
      int mIndex;

      JSONObject mJson;
      PointF mLastOffset = new PointF(0, 0);

      float mLastScale = 1;
      int mMapMargin = 100;

      String mName;
      PointF mOffset = new PointF(0, 0);
      Paint mPaintBlock = new Paint();

      Paint mPaintLine = new Paint();
      Paint mPaintText = new Paint();
      PointF mPosition;

      HashMap<PointF, PublicService> mPublicServices = new HashMap<PointF, PublicService>();

      float mScale = 0.01f;

      Shop mShop;

      HashMap<PointF, Shop> mShops = new HashMap<PointF, Shop>();

      Floor(String id, String name, int index) {
        mId = id;
        mName = name;
        mIndex = index;
      }

      @Override
      public String toString() {
        return mMall.mName + " —— " + mName;
      }

      void addOffset(float x, float y) {
        mDeltaOffset = new PointF(x, y);
        setOffset(mOffset.x + x / mScale, mOffset.y + y / mScale);
      }

      void addScale(float scale) {
        mDeltaScale = scale;
        setScale(mScale * scale);
      }

      void changeHighlight(float x, float y) {
        Unit unit = null;
        if (unit == null) {
          for (Entry<PointF, PublicService> entry : mPublicServices.entrySet()) {
            PublicService u = entry.getValue();
            if (u.mRegion != null && u.mRegion.contains((int) x, (int) y)) {
              unit = u;
              mShop = null;
              break;
            }
          }
        }
        if (unit == null) {
          for (Entry<PointF, Shop> entry : mShops.entrySet()) {
            Shop u = entry.getValue();
            if (u.mRegion != null && u.mRegion.contains((int) x, (int) y)) {
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
          for (Entry<PointF, Frame> entry : mFrames.entrySet()) {
            Frame u = entry.getValue();
            if (u.mRegion != null && u.mRegion.contains((int) x, (int) y)) {
              unit = u;
              mShop = null;
              break;
            }
          }
        }
        if (unit != null) {
          // unit.setHighlight(!unit.isHightlight());
          // invalidate();
          if (mShop != null && mShop.mRegion != null
              && !(mShop.mDisplay == null || mShop.mDisplay.trim().equals("") || mShop.mDisplay.equalsIgnoreCase("null"))) {
            PointF p = mShop.mTextCenter;
            if (p == null) {
              p = mShop.mStart;
            }
            p = new PointF(p.x, p.y);
            p.offset(mOffset.x, mOffset.y);
            // mOffset.x, mOffset.y
            x = (p.x - getWidth() / 2) * mScale + getWidth() / 2;
            y = (p.y - getHeight() / 2) * mScale + getHeight() / 2;
            x = Math.max(x, mShopPosition.getWidth() / 2);
            x = Math.min(x, getWidth() - mShopPosition.getWidth() / 2);
            y = Math.max(y, mShopPosition.getHeight());
            y = Math.min(y, getHeight() - mShopPosition.getHeight());
            if (mShop.mRegion.contains((int) x, (int) y)) {
              RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mShopPosition.getLayoutParams();
              params.leftMargin = (int) x - mShopPosition.getWidth() / 2;
              params.topMargin = (int) y - mShopPosition.getHeight();
              params.width = LayoutParams.WRAP_CONTENT;
              params.height = LayoutParams.WRAP_CONTENT;
              mShopPosition.setShop(mShop);
              // mShopPosition.setText(mShop.mDisplay);
              mShopPosition.setLayoutParams(params);
              mShopPosition.mShow = true;
              mShopPosition.setVisibility(VISIBLE);
            } else {
              mShopPosition.setVisibility(INVISIBLE);
              mShopPosition.mShow = false;
            }
          } else {
            mShopPosition.setVisibility(INVISIBLE);
            mShopPosition.mShow = false;
          }
        }
      }

      void draw(Canvas canvas) {
        logd("canvas=" + canvas);
        Paint paint = new Paint();
        paint.setColor(mMapBackgroundColor);
        canvas.drawPaint(paint);
        List<Unit> units = new ArrayList<Unit>();
        List<Unit> hlunits = new ArrayList<Unit>();
        for (Entry<PointF, Frame> entry : mFrames.entrySet()) {
          if (entry.getValue().isHightlight()) {
            hlunits.add(entry.getValue());
          } else {
            units.add(entry.getValue());
          }
        }
        for (Entry<PointF, Shop> entry : mShops.entrySet()) {
          if (entry.getValue().isHightlight()) {
            hlunits.add(entry.getValue());
          } else {
            units.add(entry.getValue());
          }
        }
        for (Entry<PointF, PublicService> entry : mPublicServices.entrySet()) {
          if (entry.getValue().isHightlight()) {
            hlunits.add(entry.getValue());
          } else {
            units.add(entry.getValue());
          }
          // entry.getValue().draw(canvas, fill, highlight);
        }
        for (Entry<PointF, Escalator> entry : mEscalators.entrySet()) {
          if (entry.getValue().isHightlight()) {
            hlunits.add(entry.getValue());
          } else {
            units.add(entry.getValue());
          }
          // entry.getValue().draw(canvas, fill, highlight);
        }
        for (Entry<PointF, Annotation> entry : mAnnotations.entrySet()) {
          if (entry.getValue().isHightlight()) {
            hlunits.add(entry.getValue());
          } else {
            units.add(entry.getValue());
          }
          // entry.getValue().draw(canvas, fill, highlight);
        }
        for (Unit unit : units) {
          if (mDrawType == DrawType.Draw) {
            unit.mDrawType = DrawType.Draw;
          }
          unit.drawBlock(canvas);
          // logd("unit: " + unit.mDisplay);
        }
        for (Unit unit : hlunits) {
          if (mDrawType == DrawType.Draw) {
            unit.mDrawType = DrawType.Draw;
          }
          unit.drawBlock(canvas);
        }
        if (mPosition != null) {
          drawPosition(canvas);
        }
        for (Unit unit : units) {
          unit.drawLine(canvas);
        }
        for (Unit unit : hlunits) {
          unit.drawLine(canvas);
        }
        if (mNavigation != null) {
          drawNavigation(canvas);
        }
        for (Unit unit : units) {
          unit.drawText(canvas);
        }
        for (Unit unit : hlunits) {
          unit.drawText(canvas);
        }
        mDrawType = DrawType.ReDraw;
        if (mShop != null && mShop.mRegion != null
            && !(mShop.mDisplay == null || mShop.mDisplay.trim().equals("") || mShop.mDisplay.equalsIgnoreCase("null"))) {
          PointF p = mShop.mTextCenter;
          if (p == null) {
            p = mShop.mStart;
          }
          p = new PointF(p.x, p.y);
          p.offset(mOffset.x, mOffset.y);
          // mOffset.x, mOffset.y
          float x = (p.x - getWidth() / 2) * mScale + getWidth() / 2;
          float y = (p.y - getHeight() / 2) * mScale + getHeight() / 2;
          x = Math.max(x, mShopPosition.getWidth() / 2);
          x = Math.min(x, getWidth() - mShopPosition.getWidth() / 2);
          y = Math.max(y, mShopPosition.getHeight());
          y = Math.min(y, getHeight() - mShopPosition.getHeight());
          if (mShop.mRegion.contains((int) x, (int) y)) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mShopPosition.getLayoutParams();
            params.leftMargin = (int) x - mShopPosition.getWidth() / 2;
            params.topMargin = (int) y - mShopPosition.getHeight();
            params.width = LayoutParams.WRAP_CONTENT;
            params.height = LayoutParams.WRAP_CONTENT;
            mShopPosition.setShop(mShop);
            // mShopPosition.setText(mShop.mDisplay);
            mShopPosition.setLayoutParams(params);
            // mShopPosition.invalidate();
            // mShopPosition.setVisibility(VISIBLE);
            mShopPosition.mShow = true;
          } else {
            mShopPosition.setVisibility(INVISIBLE);
            mShopPosition.mShow = false;
          }
        } else {
          // mButton.setVisibility(INVISIBLE);
        }
      }

      void drawNavigation(Canvas canvas) {
        int n = 0;
        Paint paint = mPaintText;
        paint.setColor(0xFFFF8800);
        paint.setStrokeWidth(3);
        paint.setStyle(Style.STROKE);
        float lastx = 0;
        float lasty = 0;
        for (int i = 0; i < mNavigation.length(); i++) {
          JSONObject json = mNavigation.optJSONObject(i);
          String floorid = json.optString("floor_id");
          if (!mId.equals(floorid)) {
            continue;
          }
          float x = (float) json.optDouble("x");
          float y = (float) json.optDouble("y");
          x = x + mOffset.x;
          y = y + mOffset.y;
          x = x * mScale + getWidth() / 2 * (1 - mScale);
          y = y * mScale + getHeight() / 2 * (1 - mScale);
          // if (x < -getWidth() / 3 || x > getWidth() * 4 / 3 || y < -getHeight() / 3 || y > getHeight() * 4 / 3)
          // {
          // return;
          // }
          if (n == 0) {
            canvas.drawCircle(x, y, 5, paint);
          } else {
            canvas.drawLine(lastx, lasty, x, y, paint);
          }
          lastx = x;
          lasty = y;
          // paint.setColor(0xAA8888FF);
          // // paint.setAlpha(0xFF);
          // canvas.drawCircle(x, y, 50, paint);
          // // paint.setAlpha(0x80);
          // paint.setColor(0xEE002266);
          // canvas.drawCircle(x, y, 5, paint);
          // paint.setStrokeWidth(2);
          // paint.setColor(0xDD006688);
          // paint.setStyle(Style.STROKE);
          // canvas.drawCircle(x, y, 50, paint);
          n++;
        }
        if (n > 0) {
          paint.setColor(0xFF0000FF);
          canvas.drawCircle(lastx, lasty, 5, paint);
        }
      }

      void drawPosition(Canvas canvas) {
        float x = mPosition.x + mOffset.x;
        float y = mPosition.y + mOffset.y;
        x = x * mScale + getWidth() / 2 * (1 - mScale);
        y = y * mScale + getHeight() / 2 * (1 - mScale);
        if (x < -getWidth() / 3 || x > getWidth() * 4 / 3 || y < -getHeight() / 3 || y > getHeight() * 4 / 3) {
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

      Poi getPoi(float x, float y) {
        Unit unit = null;
        if (unit == null) {
          for (Entry<PointF, Shop> entry : mShops.entrySet()) {
            Shop u = entry.getValue();
            if (u.mRegion != null && u.mRegion.contains((int) x, (int) y)) {
              unit = u;
              // mShop = u;
              Poi poi = new Poi(unit.mId, unit.mDisplay, unit.mType, unit.mHighlight);
              return poi;
            }
          }
        }
        return null;
      }

      int setJson(JSONObject json) {
        // logd("json=" + json);
        // logd("json=" + json.toString());
        mJson = json;
        mId = json.optString("id");
        mName = json.optString("name");
        mAlias = json.optString("alias");
        mIndex = json.optInt("index");
        json = json.optJSONObject("layers");
        JSONArray objs = json.optJSONArray("frame").optJSONArray(1);
        // logd("objs.length()" + objs.length());
        mFrames.clear();
        mShops.clear();
        mPublicServices.clear();
        for (int i = 0; i < objs.length(); i++) {
          JSONArray obj = objs.optJSONArray(i);
          // logd("obj=" + obj);
          Frame frame = new Frame(obj);
          mFrames.put(frame.mStart, frame);
        }
        objs = json.optJSONArray("shop").optJSONArray(1);
        for (int i = 0; i < objs.length(); i++) {
          JSONArray obj = objs.optJSONArray(i);
          Shop shop = new Shop(obj);
          if (mShops.get(shop.mStart) != null) {
            shop.mStart.x += 0.01;
          }
          mShops.put(shop.mStart, shop);
          // logd("shop: " + shop.mDisplay);
        }
        objs = json.optJSONArray("public_service").optJSONArray(1);
        for (int i = 0; i < objs.length(); i++) {
          JSONArray obj = objs.optJSONArray(i);
          PublicService publicservice = new PublicService(obj);
          mPublicServices.put(publicservice.mStart, publicservice);
        }
        return 0;
      }

      void setOffset(float x, float y) {
        if (mBorder == null) {
          return;
        }
        // float margin = 5 / 12f;
        // logd("mRect.width()=" + mRect.width() + ", getWidth()=" +
        // getWidth());
        if (mBorder.width() * mScale <= getWidth()) {
          x = -mBorder.left + (getWidth() - mBorder.width()) / 2;
        } else {
          // x = Math.min(x, -mBorder.left + getWidth() / 2 - getWidth() / mScale * margin);
          // x = Math.max(x, -mBorder.right + getWidth() / 2 + getWidth() / mScale * margin);
          x = Math.min(x, -mBorder.left + getWidth() / 2 - mMapMargin / mScale);
          x = Math.max(x, -mBorder.right + getWidth() / 2 + mMapMargin / mScale);
        }
        if (mBorder.height() * mScale <= getHeight()) {
          y = -mBorder.top + (getHeight() - mBorder.height()) / 2;
        } else {
          // y = Math.min(y, -mBorder.top + getHeight() / 2 - getHeight() / mScale * margin);
          // y = Math.max(y, -mBorder.bottom + getHeight() / 2 + getHeight() / mScale * margin);
          y = Math.min(y, -mBorder.top + getHeight() / 2 - mMapMargin / mScale);
          y = Math.max(y, -mBorder.bottom + getHeight() / 2 + mMapMargin / mScale);
        }
        mOffset = new PointF(x, y);
        // setPath();
        mDrawType = DrawType.Draw;
        invalidate();
      }

      // void setShop(String shopid)
      // {
      // mPosition = new PointF(x, y);
      // if (mBorder != null)
      // {
      // setOffset(x + getWidth() / 2 - mBorder.left, y + getHeight() / 2 - mBorder.top);
      // }
      // invalidate();
      // }

      void setPosition(float x, float y) {
        mPosition = new PointF(x, y);
        setOffset(-x + getWidth() / 2, -y + getHeight() / 2);
        // if (mBorder != null)
        // {
        // setOffset(x + getWidth() / 2 / mScale + mBorder.left, y + getHeight() / 2 / mScale + mBorder.top);
        // }
        invalidate();
      }

      void setScale(float scale) {
        if (mBorder == null) {
          return;
        }
        scale = Math.max(scale, Math.min((getWidth() - mMapMargin) / mBorder.width(), (getHeight() - mMapMargin) / mBorder.height()));
        if (Float.isInfinite(scale) || Float.isNaN(scale)) {
          return;
        }
        mScale = scale;
        setOffset(mOffset.x, mOffset.y);
      }
    }

    Bitmap mBitmap;

    Bitmap mBmp;

    int mDrawTimes = 0;
    Floor mFloor;

    HashMap<String, Floor> mFloors = new HashMap<String, Floor>();

    String mId;

    JSONArray mJson;

    String mName;

    JSONArray mNavigation;

    Mall(String mallid, String mallname) {
      mId = mallid;
      mName = mallname;
    }

    void addOffset(float x, float y) {
      if (mFloor != null) {
        mFloor.addOffset(x, y);
      }
    }

    void addScale(float scale) {
      if (mFloor != null) {
        mFloor.addScale(scale);
      }
    }

    void changeHighlight(float x, float y) {
      if (mFloor != null) {
        mFloor.changeHighlight(x, y);
      }
    }

    void draw(Canvas canvas) {
      if (mFloor != null) {
        // logd("mFloor.mScale=" + mFloor.mScale + ", mFloor.mOffset=" + mFloor.mOffset + ", canvas=" + canvas);
        Paint paint = new Paint();
        // logd("mRedraw=" + mRedraw + ", mDrawTimes=" + mDrawTimes);
        mDrawTimes++;
        if (mRedraw) {
          mRedraw = false;
          mDrawTimes = 0;
          mBmp = Bitmap.createBitmap(getWidth() * 5 / 3, getHeight() * 5 / 3, Config.ARGB_8888);
          Canvas cc = new Canvas(mBmp);
          cc.translate(getWidth() / 3, getHeight() / 3);
          float scale = mFloor.mScale;
          mFloor.mDrawType = DrawType.Draw;
          mFloor.mScale = scale / 2;
          mFloor.draw(cc);
          mFloor.mScale = scale;
          mFloor.mDrawType = DrawType.Draw;
          mBitmap = Bitmap.createBitmap(getWidth() * 5 / 3, getHeight() * 5 / 3, Config.ARGB_8888);
          Canvas c = new Canvas(mBitmap);
          c.translate(getWidth() / 3, getHeight() / 3);
          mFloor.draw(c);
          // c.translate(-getWidth() / 2, -getHeight() / 2);
          canvas.drawBitmap(mBitmap, -getWidth() / 3, -getHeight() / 3, paint);
          mFloor.mLastScale = mFloor.mScale;
          mFloor.mLastOffset = new PointF(mFloor.mOffset.x, mFloor.mOffset.y);
        } else {
          // mShopPosition.setVisibility(INVISIBLE);
          {
            float lastScale = mFloor.mLastScale / 2;
            Rect rect = new Rect(0, 0, getWidth() * 5 / 3, getHeight() * 5 / 3);
            float x =
                (mFloor.mOffset.x - mFloor.mLastOffset.x) * mFloor.mScale - getWidth() / 2 * (mFloor.mScale - lastScale) / lastScale
                    - getWidth() / 3 * mFloor.mScale / lastScale;
            float y =
                (mFloor.mOffset.y - mFloor.mLastOffset.y) * mFloor.mScale - getHeight() / 2 * (mFloor.mScale - lastScale) / lastScale
                    - getHeight() / 3 * mFloor.mScale / lastScale;
            RectF rectf =
                new RectF(x, y, x + getWidth() * 5 / 3 * mFloor.mScale / lastScale, y + getHeight() * 5 / 3 * mFloor.mScale / lastScale);
            // logd("mLastScale="+mLastScale+", ")
            // logd("getWidth() * 5 / 3 * mFloor.mScale / lastScale="
            // + (getWidth() * 5 / 3 * mFloor.mScale / lastScale));
            // logd("rect=" + rect + ", rectf=" + rectf);
            canvas.drawBitmap(mBmp, rect, rectf, paint);
          }
          float w = 5;
          Rect rect = new Rect((int) w, (int) w, getWidth() * 5 / 3 - (int) w, getHeight() * 5 / 3 - (int) w);
          float x =
              (mFloor.mOffset.x - mFloor.mLastOffset.x) * mFloor.mScale - getWidth() / 2 * (mFloor.mScale - mFloor.mLastScale)
                  / mFloor.mLastScale - getWidth() / 3 * mFloor.mScale / mFloor.mLastScale + w * mFloor.mScale;
          float y =
              (mFloor.mOffset.y - mFloor.mLastOffset.y) * mFloor.mScale - getHeight() / 2 * (mFloor.mScale - mFloor.mLastScale)
                  / mFloor.mLastScale - getHeight() / 3 * mFloor.mScale / mFloor.mLastScale + w * mFloor.mScale;
          RectF rectf =
              new RectF(x, y, x + getWidth() * 5 / 3 * mFloor.mScale / mFloor.mLastScale - 2 * w * mFloor.mScale, y + getHeight() * 5 / 3
                  * mFloor.mScale / mFloor.mLastScale - 2 * w * mFloor.mScale);
          logd("rect=" + rect + ", rectf=" + rectf);
          canvas.drawBitmap(mBitmap, rect, rectf, paint);
          if (mShopPosition.mShow) {
            mShopPosition.setVisibility(VISIBLE);
          }
          getResources().getDrawable(R.drawable.logo).draw(canvas);
        }
      }
      getResources().getDrawable(R.drawable.logo).draw(canvas);
    }

    String getFloorid() {
      if (mFloor == null) {
        return null;
      }
      return mFloor.mId;
    }

    String getFloorname() {
      if (mFloor == null) {
        return null;
      }
      return mFloor.mName;
    }

    int setFloor(String id) {
      logd("id=" + id);
      String from = getFloorid();
      Floor floor = mFloors.get(id);
      if (floor == null) {
        return -1;
      } else {
        mFloor = floor;
      }
      if (mFloor.mJson == null) {
        MacroMap.this.setJson(getMallid(), id);
      } else {
        // invalidate();
      }
      if (!id.equals(from)) {
        mFloor.mPosition = null;
        if (mOnMapFloorChangedListener != null) {
          mOnMapFloorChangedListener.OnMapFloorChanged(from, id);
        }
      }
      return 0;
    }

    int setFloor(String id, String name, int index) {
      if (id == null) {
        return -1;
      }
      if (id.equals(getFloorid()) && mFloor != null) {
        mFloor.mName = name;
        mFloor.mIndex = index;
      } else {
        Floor floor = mFloors.get(id);
        if (floor == null) {
          floor = new Floor(id, name, index);
          mFloors.put(id, floor);
        } else {
          floor.mName = name;
          floor.mIndex = index;
        }
        mFloor = floor;
      }
      return 0;
    }

    void setJson(String floorid, JSONObject json) {
      // setFloor(floorid);
      Floor floor = mFloors.get(floorid);
      floor.setJson(json);
    }

    void setNavigation(JSONArray json) {
      mNavigation = json;
    }

    void setOffset(float x, float y) {
      if (mFloor != null) {
        mFloor.setOffset(x, y);
      }
    }

    void setPosition(float x, float y) {
      if (mFloor != null) {
        mFloor.setPosition(x, y);
      }
    }

    void setScale(float scale) {
      if (mFloor != null) {
        mFloor.setScale(scale);
      }
    }
  }

  class ShopPosition extends RelativeLayout {

    Mall.Floor.Shop mShop;

    boolean mShow = false;

    public ShopPosition(Context context) {
      super(context);
      init();
    }

    public ShopPosition(Context context, AttributeSet attrs) {
      super(context, attrs);
      init();
    }

    public ShopPosition(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
      init();
    }

    void init() {
      LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      inflater.inflate(R.layout.shopposition, this, true);
      ImageButton share = (ImageButton) findViewById(R.id.shop_share);
      share.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          if (mOnMapEventListener != null && mShop != null) {
            mOnMapEventListener.OnMapEvent(mShop.mId, OnMapEventType.MapClickedLeft);
          }
        }
      });
      ImageButton more = (ImageButton) findViewById(R.id.shop_more);
      more.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          if (mOnMapEventListener != null && mShop != null) {
            mOnMapEventListener.OnMapEvent(mShop.mId, OnMapEventType.MapClickedRight);
          }
        }
      });
    }

    void setShop(Mall.Floor.Shop shop) {
      mShop = shop;
      setText(mShop.mDisplay);
    }

    void setText(String display) {
      TextView text = (TextView) findViewById(R.id.shop_display);
      text.setText(display);
    }
  }

  static void logd(String log) {
    StackTraceElement ste = new Throwable().getStackTrace()[1];
    Log.d(ste.getClassName(), "at " + ste.getMethodName() + "(" + ste.getFileName() + ":" + ste.getLineNumber() + ")" + "  " + log);
  }

  static void logd(Throwable e) {
    e.printStackTrace();
  }

  private int mAnnotationTextColor = Color.BLACK;

  private int mAnnotationTextHighlightColor = Color.YELLOW;
  private int mAssistantBorderColor = Color.MAGENTA;

  private int mAssistantBorderHightlightColor = Color.GREEN;
  private int mAssistantBorderSize = 3;
  private int mAssistantFilledColor = Color.GREEN;
  private int mAssistantFilledHighlightColor = Color.YELLOW;
  private int mAssistantTextColor = Color.BLACK;
  HashMap<String, Integer> mBorderColors = new HashMap<String, Integer>();

  ConfigureFile mConfigure = new ConfigureFile(new File(Environment.getExternalStorageDirectory(), "/Palmap/configure.json"));
  // private Drawable mEscalatorElevatorIcon;
  // private Drawable mEscalatorEscalatorIcon;
  // private Drawable mEscalatorStairIcon;
  private int mEscalatorTextColor = Color.BLACK;
  private int mEscalatorTextHighlightColor = Color.YELLOW;
  HashMap<String, Integer> mFilledColors = new HashMap<String, Integer>();
  ArrayAdapter<Mall.Floor> mFloorsAdapter;
  private int mFrameBorderColor = Color.BLUE;

  private int mFrameBorderSize = 5;
  private int mFrameFilledColor = Color.LTGRAY;

  Handler mHandler = new Handler();
  boolean mHasMoved = false;
  boolean mIsMove = false;
  boolean mIsScale = false;
  float mLastScale;
  float mLastX;

  float mLastY;
  private Mall mMall;
  private HashMap<String, Mall> mMalls = new HashMap<String, Mall>();
  private int mMapBackgroundColor = Color.WHITE;
  int mMiniumSize = 16;

  OnMapEventListener mOnMapEventListener;

  OnMapFloorChangedListener mOnMapFloorChangedListener;
  int mOrientation = getResources().getConfiguration().orientation;
  private Drawable mPublicServiceAtmIcon;

  HashMap<String, String> mPublicServiceIcons = new HashMap<String, String>();

  private int mPublicServiceTextColor = Color.BLACK;

  private int mPublicServiceTextHighlightColor = Color.YELLOW;

  private Drawable mPublicServiceToiletIcon;

  boolean mRedraw = true;

  RelativeLayout mRelativeLayout = new RelativeLayout(getContext());

  private int mShopBorderColor = Color.MAGENTA;

  private int mShopBorderHightlightColor = Color.GREEN;
  private int mShopBorderSize = 3;

  private int mShopFilledColor = Color.YELLOW;

  private int mShopFilledHighlightColor = Color.YELLOW;

  // Button mButton = new Button(getContext());
  ShopPosition mShopPosition;

  private int mShopTextColor = Color.BLACK;

  Spinner mSpinner;

  HashMap<String, Integer> mTextColors = new HashMap<String, Integer>();

  Typeface mTypeface = Typeface.createFromAsset(getContext().getAssets(), "PalmapPublic.ttf");

  public MacroMap(Context context) {
    super(context);
    init(null, 0);
  }

  public MacroMap(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public MacroMap(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs, defStyle);
  }

  public String getFloorid() {
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

  public String getMallid() {
    if (mMall == null) {
      return null;
    }
    return mMall.mId;
  }

  public String getMallname() {
    if (mMall == null) {
      return null;
    }
    return mMall.mName;
  }

  public Poi getPoi(int x, int y) {
    if (mMall != null && mMall.mFloor != null) {
      return mMall.mFloor.getPoi(x, y);
    }
    return null;
  }

  public Poi[] getPoiList() {
    if (mMall == null || mMall.mFloor == null) {
      return null;
    }
    List<Poi> pois = new ArrayList<Poi>();
    for (Mall.Floor.Shop shop : mMall.mFloor.mShops.values()) {
      Poi poi = new Poi(shop.mId, shop.mName, shop.mType, shop.mHighlight);
      pois.add(poi);
    }
    return (Poi[]) pois.toArray();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    // logd("event=" + event);
    // return false;
    // logd("event.getAction()=" + event.getAction());
    if (event.getPointerCount() == 1) {
      float ex = event.getX();// - mLocationX;
      float ey = event.getY();// - mLocationY;
      mIsScale = false;
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        mIsMove = true;
        mHasMoved = false;
        mLastX = ex;
        mLastY = ey;
      } else if (!mIsMove) {
      } else if (!mHasMoved && event.getAction() == MotionEvent.ACTION_UP) {
        changeHighlight(ex - getLeft(), ey - getTop());
        // invalidate();
        if (mOnMapEventListener != null) {
          Poi poi = getPoi((int) ex - getLeft(), (int) ey - getTop());
          if (poi != null) {
            mOnMapEventListener.OnMapEvent(poi.mId, poi.mSelected ? OnMapEventType.MapSelected : OnMapEventType.MapUnselected);
          }
        }
      } else if ((Math.abs(ex - mLastX) + Math.abs(ey - mLastY) > 10)
          && (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP)) {
        mHasMoved = true;
        mRedraw = event.getAction() == MotionEvent.ACTION_UP;
        // mRedraw = false;
        mShopPosition.setVisibility(INVISIBLE);
        mShopPosition.mShow = false;
        logd("mRedraw=" + mRedraw);
        // logd("ex - mLastX = " + (ex - mLastX)
        // + ", ey - mLastY" + (ey - mLastY));
        addOffset(ex - mLastX, ey - mLastY);
        mLastX = ex;
        mLastY = ey;
      } else if (mHasMoved && event.getAction() == MotionEvent.ACTION_UP) {
        mIsMove = false;
        mRedraw = true;
        // logd("mRedraw=" + mRedraw);
      }
    } else {
      mIsMove = false;
      mHasMoved = false;
      // logd("event.getAction()=" + event.getAction());
      // if (event.getAction() == MotionEvent.ACTION_DOWN) {
      // } else
      if (!mIsScale) {
        mIsScale = true;
        float x = event.getX(1) - event.getX(0);
        float y = event.getY(1) - event.getY(0);
        float r = (float) Math.sqrt(x * x + y * y);
        mLastScale = r;
        mLastX = (event.getX(1) + event.getX(0)) / 2;// - mLocationX;
        mLastY = (event.getY(1) + event.getY(0)) / 2;// - mLocationY;
        // logd("r=" + r);
      } else if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_POINTER_UP) {
        mRedraw = event.getAction() == MotionEvent.ACTION_POINTER_UP;
        mShopPosition.setVisibility(INVISIBLE);
        mShopPosition.mShow = false;
        // mRedraw = false;
        float x = event.getX(1) - event.getX(0);
        float y = event.getY(1) - event.getY(0);
        float r = (float) Math.sqrt(x * x + y * y);
        addScale(r / mLastScale);
        // logd("r/mLastX=" + r / mLastX);
        // logd("r=" + r);
        x = (event.getX(1) + event.getX(0)) / 2;// - mLocationX;
        y = (event.getY(1) + event.getY(0)) / 2;// - mLocationY;
        addOffset(x - ((mLastX - getLeft() - getWidth() / 2) * r / mLastScale + getLeft() + getWidth() / 2), y
            - ((mLastY - getTop() - getHeight() / 2) * r / mLastScale + getTop() + getHeight() / 2));
        mLastX = x;
        mLastY = y;
        mLastScale = r;
        // logd("mRedraw=" + mRedraw);
      } else if (mIsScale
          && (event.getAction() == MotionEvent.ACTION_POINTER_UP || (event.getAction() & 0xFF) == (MotionEvent.ACTION_POINTER_UP))) {
        // mIsScale = false;
        mRedraw = true;
        // logd("mRedraw=" + mRedraw);
      }
    }
    return super.onTouchEvent(event);
  }

  public void redraw() {
    mRedraw = true;
    invalidate();
  }

  public void scrollIntoView(int poid) {

  }

  public void scrollIntoView(int x, int y) {

  }

  public void setBorderColor(int color) {
    mFrameBorderColor = color;
  }

  public void setBorderColor(String type, int color) {
    mBorderColors.put(type, color);
  }

  public void setBorderSize(int size) {
    mFrameBorderSize = size;
  }

  public void setFilledColor(String type, int color) {
    mFilledColors.put(type, color);
  }

  public int setFloor(String id) {
    if (mMall == null || id == null) {
      return -1;
    }
    for (int i = 0; i < mFloorsAdapter.getCount(); i++) {
      String floorid = mFloorsAdapter.getItem(i).mId;
      if (floorid.equals(id)) {
        mSpinner.setSelection(i);
        break;
      }
    }
    return mMall.setFloor(id);
  }

  // int mLocationX = 0;
  // int mLocationY = 0;
  //
  // @Override
  // protected void onLayout(boolean changed, int l, int t, int r, int b)
  // {
  // super.onLayout(changed, l, t, r, b);
  // int[] xy = { 0, 0 };
  // getLocationInWindow(xy);
  // mLocationX = xy[0];
  // mLocationY = xy[1];
  // }

  public int setFloor(String id, String name, int index) {
    if (mMall == null || id == null) {
      return -1;
    }
    return mMall.setFloor(id, name, index);
  }

  public int setMall(String id) {
    if (id == null) {
      return -1;
    }
    if (id.equals(getMallid()) && mMall != null) {
    } else {
      Mall mall = mMalls.get(id);
      if (mall == null) {
        return -1;
      }
      mMall = mall;
      // setJson(id);
    }
    if (mMall.mJson == null) {
      setJson(id);
    }
    return 0;
  }

  public int setMall(String id, String name) {
    if (id == null) {
      return -1;
    }
    if (id.equals(getMallid()) && mMall != null) {
      mMall.mName = name;
    } else {
      Mall mall = mMalls.get(id);
      if (mall == null) {
        mall = new Mall(id, name);
        mMalls.put(id, mall);
      } else {
        mall.mName = name;
      }
      mMall = mall;
      // setJson(id);
    }
    return 0;
  }

  public void setNavigation(JSONArray json) {
    if (mMall != null) {
      mMall.setNavigation(json);
      if (json != null) {
        JSONObject obj = json.optJSONObject(0);
        if (obj != null) {
          String floorid = obj.optString("floor_id");
          setFloor(floorid);
        }
      }
    }
  }

  public void setOffset(float x, float y) {
    addOffset(x, y);
  }

  public void setOnMapEventListener(OnMapEventListener onMapEventListener) {
    mOnMapEventListener = onMapEventListener;
  }

  public void setOnMapFloorChangedListener(OnMapFloorChangedListener onMapFloorChangedListener) {
    mOnMapFloorChangedListener = onMapFloorChangedListener;
  }

  public void setPosition(String floorid, float x, float y) {
    setFloor(floorid);
    if (mMall != null) {
      mMall.setPosition(x, -y);
      mRedraw = true;
      invalidate();
    }
  }

  public void setScale(float scale) {
    addScale(scale);
  }

  public void setTextColor(String type, int color) {
    mTextColors.put(type, color);
  }

  public void zoomin() {
    setScale(2f);
    mRedraw = true;
    invalidate();
  }

  public void zoomout() {
    setScale(0.5f);
    mRedraw = true;
    invalidate();
  }

  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    logd("mOrientation=" + mOrientation + ", newConfig.orientation=" + newConfig.orientation);
    if (mOrientation != newConfig.orientation) {
      mOrientation = newConfig.orientation;
      mRedraw = true;
      addScale(1);
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    logd("canvas=" + canvas);
    if (mMall != null) {
      mMall.draw(canvas);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // logd("widthMeasureSpec=" + widthMeasureSpec + ", heightMeasureSpec="
    // + heightMeasureSpec);
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  void addOffset(float x, float y) {
    if (mMall != null) {
      mMall.addOffset(x, y);
      // invalidate();
    }
  }

  void addScale(float scale) {
    if (mMall != null) {
      mMall.addScale(scale);
      // invalidate();
    }
  }

  void changeHighlight(float x, float y) {
    if (mMall != null) {
      mMall.changeHighlight(x, y);
    }
  }

  int downloadJson(String u, File file) {
    try {
      URL url = new URL(u);
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setRequestMethod("GET");
      urlConnection.setRequestProperty("Accept", "application/json");
      // urlConnection.setRequestMethod("GET");
      // urlConnection.setDoOutput(true);
      urlConnection.connect();
      InputStream inputStream = urlConnection.getInputStream();
      int totalSize = urlConnection.getContentLength();
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
      logd(e);
    }
    return 0;
  }

  int setJson(JSONArray jsonArray) {
    mFloorsAdapter.clear();
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject json = jsonArray.optJSONObject(i);
      String id = json.optString("id");
      String name = json.optString("name");
      int index = json.optInt("index");
      setFloor(id, name, index);
    }
    mFloorsAdapter.addAll(mMall.mFloors.values());
    mFloorsAdapter.sort(new Comparator<Mall.Floor>() {
      public int compare(Mall.Floor lhs, Mall.Floor rhs) {
        return lhs.mIndex - rhs.mIndex;
      }
    });
    setFloor(mFloorsAdapter.getItem(0).mId);
    return 0;
  }

  int setJson(String mallid) {
    // setJson(mallid, "6");
    // String url = "http://10.0.0.10/mall/" + mallid + "/floors";
    String url = "http://apitest.palmap.cn/mall/" + mallid + "/floors";
    new Thread(new DownloadJson(mallid, url)).start();
    return 0;
  }

  int setJson(String mallid, File file) {
    try {
      // File file = new File(jsonfile);
      FileInputStream input = new FileInputStream(file);
      byte[] buf = new byte[input.available()];
      input.read(buf);
      input.close();
      String json = EncodingUtils.getString(buf, "UTF-8");
      JSONArray obj = new JSONArray(json);
      setJson(obj);
      logd("file.length()=" + file.length());
      return 0;
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  int setJson(String mallid, String floorid) {
    String url = "http://apitest.palmap.cn/mall/" + mallid + "/floor/" + floorid;
    new Thread(new DownloadJson(mallid, floorid, url)).start();
    return 0;
  }

  int setJson(String mallid, String floorid, File file) {
    try {
      // File file = new File(jsonfile);
      FileInputStream input = new FileInputStream(file);
      byte[] buf = new byte[input.available()];
      input.read(buf);
      input.close();
      String json = EncodingUtils.getString(buf, "UTF-8");
      JSONObject obj = new JSONObject(json);
      // setMall(mallid);
      Mall mall = mMalls.get(mallid);
      mall.setJson(floorid, obj);
      if (mall == mMall) {
        mRedraw = true;
        addScale(1);
        // addOffset(0, 0);
        // invalidate();
        logd("file.length()=" + file.length());
      }
      return 0;
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  private void init(AttributeSet attrs, int defStyle) {

    for (int i = 0; i < 100; i++) {
      // mPublicServiceIcons.put(9100 + i, "" + (char) i);
      mPublicServiceIcons.put((9100 + i) + "", "" + (char) (i + 20));
      // logd("key=" + (9100 + i) + ", value=" + ((char) i) + "," + ("" +
      // (char) i));
    }
    mPublicServiceIcons.put("27108", "%"); // 洗手间
    mPublicServiceIcons.put("27125", "#"); // 扶梯
    mPublicServiceIcons.put("27124", "$"); // 楼梯
    mPublicServiceIcons.put("27126", "\""); // 电梯
    mPublicServiceIcons.put("27052", "'"); // 出入口
    mPublicServiceIcons.put("27114", "y"); // 自动售货机
    mPublicServiceIcons.put("27010", "("); // ATM
    mPublicServiceIcons.put("27066", "-");
    mPublicServiceIcons.put("27096", "{");
    mPublicServiceIcons.put("27055", "{"); // 问讯处
    mSpinner = new Spinner(getContext());
    mSpinner.setPrompt("Floors:");
    mFloorsAdapter = new ArrayAdapter<Mall.Floor>(getContext(), android.R.layout.simple_spinner_item, 0, new ArrayList<Mall.Floor>());
    // (Mall.Floor[]) mMall.mFloors.values().toArray()
    mSpinner.setAdapter(mFloorsAdapter);
    mRelativeLayout.addView(mSpinner);
    addView(mRelativeLayout);
    mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String floorid = mFloorsAdapter.getItem(position).mId;
        mRedraw = true;
        mShopPosition.mShow = false;
        mShopPosition.setVisibility(INVISIBLE);
        setFloor(floorid);
        invalidate();
      }

      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    LinearLayout linear = new LinearLayout(getContext());
    linear.setOrientation(LinearLayout.VERTICAL);
    ImageView image = new ImageView(getContext());
    image.setImageResource(R.drawable.logo);
    image.setAlpha(0.3f);
    linear.addView(image);
    image = new ImageView(getContext());
    image.setImageResource(R.drawable.logo);
    image.setAlpha(0.3f);
    linear.addView(image);
    // mRelativeLayout.addView(linear);
    mShopPosition = new ShopPosition(getContext(), attrs, defStyle);
    mShopPosition.setVisibility(INVISIBLE);
    android.widget.RelativeLayout.LayoutParams params =
        new android.widget.RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    mRelativeLayout.addView(mShopPosition, params);

    final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MacroMap, defStyle, 0);

    mMapBackgroundColor = a.getColor(R.styleable.MacroMap_mapBackgroundColor, mMapBackgroundColor);
    mFrameBorderColor = a.getColor(R.styleable.MacroMap_frameBorderColor, mFrameBorderColor);
    mFrameFilledColor = a.getColor(R.styleable.MacroMap_frameFilledColor, mFrameFilledColor);
    mFrameBorderSize = a.getInt(R.styleable.MacroMap_frameBorderSize, mFrameBorderSize);

    mAssistantBorderColor = a.getColor(R.styleable.MacroMap_assistantBorderColor, mAssistantBorderColor);
    mAssistantBorderHightlightColor = a.getColor(R.styleable.MacroMap_assistantBorderHighlightColor, mAssistantBorderHightlightColor);
    mAssistantFilledColor = a.getColor(R.styleable.MacroMap_assistantFilledColor, mAssistantFilledColor);
    mAssistantFilledHighlightColor = a.getColor(R.styleable.MacroMap_assistantFilledHighlightColor, mAssistantFilledHighlightColor);
    mAssistantTextColor = a.getColor(R.styleable.MacroMap_assistantTextColor, mAssistantTextColor);
    mAssistantBorderSize = a.getInt(R.styleable.MacroMap_assistantBorderSize, mAssistantBorderSize);

    mShopBorderColor = a.getColor(R.styleable.MacroMap_shopBorderColor, mShopBorderColor);
    mShopBorderHightlightColor = a.getColor(R.styleable.MacroMap_shopBorderHighlightColor, mShopBorderHightlightColor);
    mShopFilledColor = a.getColor(R.styleable.MacroMap_shopFilledColor, mShopFilledColor);
    mShopFilledHighlightColor = a.getColor(R.styleable.MacroMap_shopFilledHighlightColor, mShopFilledHighlightColor);
    mShopTextColor = a.getColor(R.styleable.MacroMap_shopTextColor, mShopTextColor);
    mShopBorderSize = a.getInt(R.styleable.MacroMap_shopBorderSize, mShopBorderSize);

    mPublicServiceTextColor = a.getColor(R.styleable.MacroMap_publicServiceTextColor, mPublicServiceTextColor);
    mPublicServiceTextHighlightColor = a.getColor(R.styleable.MacroMap_publicServiceTextHighlightColor, mPublicServiceTextHighlightColor);
    mPublicServiceAtmIcon = a.getDrawable(R.styleable.MacroMap_publicServiceAtmIcon);
    mPublicServiceToiletIcon = a.getDrawable(R.styleable.MacroMap_publicServiceToiletIcon);

    mEscalatorTextColor = a.getColor(R.styleable.MacroMap_escalatorTextColor, mEscalatorTextColor);
    mEscalatorTextHighlightColor = a.getColor(R.styleable.MacroMap_escalatorTextHighlightColor, mEscalatorTextHighlightColor);
    // mEscalatorEscalatorIcon = a
    // .getDrawable(R.styleable.MacroMap_escalatorEscalatorIcon);
    // mEscalatorElevatorIcon = a
    // .getDrawable(R.styleable.MacroMap_escalatorElevatorIcon);
    // mEscalatorStairIcon = a
    // .getDrawable(R.styleable.MacroMap_escalatorStairIcon);

    mAnnotationTextColor = a.getColor(R.styleable.MacroMap_annotationTextColor, mAnnotationTextColor);
    mAnnotationTextHighlightColor = a.getColor(R.styleable.MacroMap_annotationTextHighlightColor, mAnnotationTextHighlightColor);
    a.recycle();
  }
}
