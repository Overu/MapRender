package com.macrowen.macromap.draw;

import com.macrowen.macromap.draw.data.JSONData;

import org.json.JSONArray;

import android.util.Log;

import android.graphics.Color;

public class Shop extends DrawLayer<JSONArray> {

  public Shop() {
    setName(mDisplay);
  }

  @Override
  public void onInfo(JSONArray jsonArray) {
    mDisplay = jsonArray.optString(0);
    setName(mDisplay == null || mDisplay.equals("") || mDisplay.equals("null") ? "" : mDisplay);
    JSONArray json = jsonArray.optJSONArray(1);
    if (json != null) {
      mTextCenter = getPoint(json);
    }
    setType(jsonArray.optString(3));
    setId(String.valueOf(jsonArray.optInt(4)));
  }

  @Override
  public void setData(JSONData<JSONArray> mData) {
    super.setData(mData);

    mBorderColor = Color.MAGENTA;
    mFilledColor = Color.YELLOW;
    mFilledColor =
        (int) Math.round(Math.random() * 32 + 224) + (int) Math.round(Math.random() * 32 + 224) * 256
            + (int) Math.round(Math.random() * 32 + 224) * 256 * 256 + (int) Math.round(Math.random() * 32 + 224) * 256 * 256 * 256;
    mTextColor = Color.BLACK;
    mBorderSize = 1;

    String cs = mColorConfigures.getShopConfigure("fillColor");
    Integer color = mColorConfigures.getColor(cs);
    if (color != null) {
      mFilledColor = color;
    }
    String type = getType();
    Log.w("type", type);
    cs = mColorConfigures.getShopCategoryConfigure(type);
    color = mColorConfigures.getColor(cs);
    if (color != null) {
      mFilledColor = color;
    }
    cs = mColorConfigures.getShopConfigure("textColor");
    color = mColorConfigures.getColor(cs);
    if (color != null) {
      mTextColor = color;
    }
    cs = mColorConfigures.getShopConfigure("borderColor");
    color = mColorConfigures.getColor(cs);
    if (color != null) {
      mBorderColor = color;
    }
    cs = mColorConfigures.getShopConfigure("borderWidth");
    Float colorFloat = mColorConfigures.getFloat(cs);
    if (colorFloat != null) {
      mBorderSize = colorFloat;
    }
  }

  @Override
  public String toString() {
    return getName().equals("") ? "no name" : getName();
  }

}
