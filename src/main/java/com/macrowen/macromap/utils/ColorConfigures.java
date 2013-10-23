package com.macrowen.macromap.utils;

import com.macrowen.macromap.draw.data.JSONArray;

import org.apache.http.util.EncodingUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Color;

import android.content.Context;

public class ColorConfigures {

  private JSONObject mColorConfigure;

  public ColorConfigures(Context context, String fileName) {
    try {
      InputStream open = context.getResources().getAssets().open(fileName);
      byte[] byts = new byte[open.available()];
      open.read(byts);
      open.close();
      String json = EncodingUtils.getString(byts, "utf-8");
      mColorConfigure = new JSONObject(json).optJSONObject("style");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Integer getColor(String color) {
    Integer c = null;
    if (color == null || color.equals("")) {
      return c;
    }
    String[] split = color.split(",");
    if (split.length != 0) {
      c = Color.rgb(Integer.valueOf(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]));
    } else {
      c = Integer.valueOf("0x" + color, 16);
    }
    return c;
  }

  public JSONObject getColorConfigure() {
    return mColorConfigure;
  }

  public Float getFloat(String cs) {
    Float c = null;
    try {
      c = Float.valueOf(cs);
    } catch (Throwable e) {
      // logd(e);
    }
    return c;
  }

  public String getFrameConfigure(String name) {
    if (mColorConfigure == null) {
      return "";
    }
    return mColorConfigure.optJSONObject("frame").optString(name);
  }

  public Integer getInt(String cs) {
    Integer c = null;
    try {
      c = Integer.valueOf(cs);
    } catch (Throwable e) {
      // logd(e);
    }
    return c;
  }

  public String getShopCategoryConfigure(String name) {
    if (mColorConfigure == null || name == null) {
      return "";
    }
    JSONObject json = mColorConfigure.optJSONObject("shop").optJSONObject("category");
    if (json == null) {
      return "";
    }
    return json.optString(name);
    // for (int i = 1; i < name.length(); i++) {
    // String n = name.substring(0, i);
    // String c = json.optString(n);
    // if (c != null && !c.isEmpty()) {
    // return c;
    // }
    // }
    // return "";
  }

  public String getShopConfigure(String name) {
    if (mColorConfigure == null) {
      return "";
    }
    return mColorConfigure.optJSONObject("shop").optString(name);
  }

}
