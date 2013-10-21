package com.macrowen.macromap;

import com.macrowen.macromap.draw.Floor;
import com.macrowen.macromap.draw.Map;
import com.macrowen.macromap.draw.ShopPosition;
import com.macrowen.macromap.utils.MapService;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

@SuppressLint("WrongCall")
public class MacroMap extends ScrollView {

  public enum OnMapEventType {
    MapClickedLeft, MapClickedRight, MapSelected, MapUnselected
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
  ArrayAdapter<Floor> mFloorsAdapter;
  private int mFrameBorderColor = Color.BLUE;

  private int mFrameBorderSize = 5;
  private int mFrameFilledColor = Color.LTGRAY;

  Handler mHandler = new Handler();
  boolean mHasMoved = false;
  boolean mIsMove = false;
  boolean mIsScale = false;
  float mLastScale;
  float mLastX;

  MapService mMapService = MapService.getInstance();

  float mLastY;
  // private Map mMall;
  // private HashMap<String, Map> mMalls = new HashMap<String, Map>();
  private int mMapBackgroundColor = Color.WHITE;
  int mMiniumSize = 16;

  int mOrientation = getResources().getConfiguration().orientation;
  private Drawable mPublicServiceAtmIcon;

  HashMap<String, String> mPublicServiceIcons = new HashMap<String, String>();

  private int mPublicServiceTextColor = Color.BLACK;

  private int mPublicServiceTextHighlightColor = Color.YELLOW;

  private Drawable mPublicServiceToiletIcon;

  // boolean mRedraw = true;

  RelativeLayout mRelativeLayout = new RelativeLayout(getContext());

  private int mShopBorderColor = Color.MAGENTA;

  private int mShopBorderHightlightColor = Color.GREEN;
  private int mShopBorderSize = 3;

  private int mShopFilledColor = Color.YELLOW;

  private int mShopFilledHighlightColor = Color.YELLOW;

  // Button mButton = new Button(getContext());
  // ShopPosition mShopPosition;

  private int mShopTextColor = Color.BLACK;

  Spinner mSpinner;

  HashMap<String, Integer> mTextColors = new HashMap<String, Integer>();

  Typeface mTypeface = Typeface.createFromAsset(getContext().getAssets(), "PalmapPublic.ttf");

  private boolean isInit = true;

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

  // public Poi getPoi(int x, int y) {
  // if (mMall != null && mMall.getCurFloor() != null) {
  // // return mMall.mFloor.getPoi(x, y);
  // }
  // return null;
  // }

  // public Poi[] getPoiList() {
  // if (mMall == null || mMall.getCurFloor() == null) {
  // return null;
  // }
  // List<Poi> pois = new ArrayList<Poi>();
  // // for (Mall.Floor.Shop shop : mMall.mFloor.mShops.values()) {
  // // Poi poi = new Poi(shop.mId, shop.mName, shop.mType, shop.mHighlight);
  // // pois.add(poi);
  // // }
  // return (Poi[]) pois.toArray();
  // }

  public void hidePosition() {
    mMapService.getShopPosition().setVisibility(INVISIBLE);
    mMapService.getShopPosition().mShow = false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
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
        mMapService.showShopPosition(ex - getLeft(), ey - getTop());
        // invalidate();
        // if (mOnMapEventListener != null) {
        // Poi poi = getPoi((int) ex - getLeft(), (int) ey - getTop());
        // if (poi != null) {
        // mOnMapEventListener.OnMapEvent(poi.mId, poi.mSelected ? OnMapEventType.MapSelected : OnMapEventType.MapUnselected);
        // }
        // }
      } else if ((Math.abs(ex - mLastX) + Math.abs(ey - mLastY) > 10)
          && (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP)) {
        mHasMoved = true;
        // mRedraw = event.getAction() == MotionEvent.ACTION_UP;
        // mRedraw = false;
        mMapService.getShopPosition().setVisibility(INVISIBLE);
        mMapService.getShopPosition().mShow = false;
        // logd("ex - mLastX = " + (ex - mLastX)
        // + ", ey - mLastY" + (ey - mLastY));
        mMapService.addOffset(ex - mLastX, ey - mLastY);
        mLastX = ex;
        mLastY = ey;
      } else if (mHasMoved && event.getAction() == MotionEvent.ACTION_UP) {
        mIsMove = false;
        mMapService.reDraw();
        // logd("mRedraw=" + mRedraw);
      }
    } else {
      mIsMove = false;
      mHasMoved = false;
      if (!mIsScale) {
        mIsScale = true;
        float x = event.getX(1) - event.getX(0);
        float y = event.getY(1) - event.getY(0);
        float r = (float) Math.sqrt(x * x + y * y);
        mLastScale = r;
        mLastX = (event.getX(1) + event.getX(0)) / 2;// - mLocationX;
        mLastY = (event.getY(1) + event.getY(0)) / 2;// - mLocationY;
      } else if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_POINTER_UP) {
        mMapService.reDraw(event.getAction() == MotionEvent.ACTION_POINTER_UP);
        hidePosition();
        // mRedraw = false;
        float x = event.getX(1) - event.getX(0);
        float y = event.getY(1) - event.getY(0);
        float r = (float) Math.sqrt(x * x + y * y);
        mMapService.addScale(r / mLastScale);
        x = (event.getX(1) + event.getX(0)) / 2;// - mLocationX;
        y = (event.getY(1) + event.getY(0)) / 2;// - mLocationY;
        mMapService.addOffset(x - ((mLastX - getLeft() - getWidth() / 2) * r / mLastScale + getLeft() + getWidth() / 2), y
            - ((mLastY - getTop() - getHeight() / 2) * r / mLastScale + getTop() + getHeight() / 2));
        mLastX = x;
        mLastY = y;
        mLastScale = r;
      } else if (mIsScale
          && (event.getAction() == MotionEvent.ACTION_POINTER_UP || (event.getAction() & 0xFF) == (MotionEvent.ACTION_POINTER_UP))) {
        mMapService.reDraw();
      }
    }
    return super.onTouchEvent(event);
    // return true;
  }

  // public void redraw() {
  // mMall.reDraw();
  // invalidate();
  // }

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

  public void setFloor(String id) {
    if (mMapService.getMap() == null || id == null) {
      return;
    }
    for (int i = 0; i < mFloorsAdapter.getCount(); i++) {
      String floorid = mFloorsAdapter.getItem(i).getId();
      if (floorid.equals(id)) {
        mSpinner.setSelection(i);
        break;
      }
    }
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

  public void setMap(final Map map) {
    ShopPosition shopPosition = new ShopPosition(getContext());
    shopPosition.setVisibility(INVISIBLE);
    android.widget.RelativeLayout.LayoutParams params =
        new android.widget.RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    mRelativeLayout.addView(shopPosition, params);

    mMapService.setShopPosition(shopPosition);
    if (mFloorsAdapter.getCount() == 0) {
      mFloorsAdapter.addAll(map.getFloors().values());
      mFloorsAdapter.sort(new Comparator<Floor>() {
        public int compare(Floor lhs, Floor rhs) {
          return lhs.getIndex() - rhs.getIndex();
        }
      });
    }
    setFloor(map.getFloorid());
    new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(500);
          mHandler.post(new Runnable() {
            public void run() {
              map.reDraw();
              mMapService.addScale(1);
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  public void setNavigation(JSONArray json) {
    if (mMapService.getMap() != null) {
      // mMall.setNavigation(json);
      if (json != null) {
        JSONObject obj = json.optJSONObject(0);
        if (obj != null) {
          String floorid = obj.optString("floor_id");
          // setFloor(floorid);
        }
      }
    }
  }

  public void setTextColor(String type, int color) {
    mTextColors.put(type, color);
  }

  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    logd("mOrientation=" + mOrientation + ", newConfig.orientation=" + newConfig.orientation);
    if (mOrientation != newConfig.orientation) {
      mOrientation = newConfig.orientation;

      mMapService.reDraw();
      mMapService.addScale(1);
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    logd("canvas=" + canvas);
    Map map = mMapService.getMap();
    if (map != null) {
      map.onDraw(canvas);
    }
  }

  private void init(AttributeSet attrs, int defStyle) {

    for (int i = 0; i < 100; i++) {
      // mPublicServiceIcons.put(9100 + i, "" + (char) i);
      mPublicServiceIcons.put((9100 + i) + "", "" + (char) (i + 20));
      // logd("key=" + (9100 + i) + ", value=" + ((char) i) + "," + ("" +
      // (char) i));
    }
    mPublicServiceIcons.put("27108", "%"); // 娲楁墜闂�
    mPublicServiceIcons.put("27125", "#"); // 鎵舵
    mPublicServiceIcons.put("27124", "$"); // 妤兼
    mPublicServiceIcons.put("27126", "\""); // 鐢垫
    mPublicServiceIcons.put("27052", "'"); // 鍑哄叆鍙�
    mPublicServiceIcons.put("27114", "y"); // 鑷姩鍞揣鏈�
    mPublicServiceIcons.put("27010", "("); // ATM
    mPublicServiceIcons.put("27066", "-");
    mPublicServiceIcons.put("27096", "{");
    mPublicServiceIcons.put("27055", "{"); // 闂澶�
    mSpinner = new Spinner(getContext());
    mSpinner.setPrompt("Floors:");
    mFloorsAdapter = new ArrayAdapter<Floor>(getContext(), android.R.layout.simple_spinner_item, 0, new ArrayList<Floor>());
    // (Mall.Floor[]) mMall.mFloors.values().toArray()
    mSpinner.setAdapter(mFloorsAdapter);
    mRelativeLayout.addView(mSpinner);
    addView(mRelativeLayout);
    mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (isInit) {
          isInit = false;
          return;
        }
        String floorid = mFloorsAdapter.getItem(position).getId();
        mMapService.getShopPosition().mShow = false;
        mMapService.getShopPosition().setVisibility(INVISIBLE);
        setFloor(floorid);
        mMapService.setFloor(floorid);
        // invalidate();
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
