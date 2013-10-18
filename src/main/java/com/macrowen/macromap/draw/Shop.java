package com.macrowen.macromap.draw;

import org.json.JSONArray;

import android.graphics.Canvas;

import android.graphics.Color;

public class Shop extends DrawLayer<JSONArray> {

  public Shop() {
    setName(mDisplay);
    mBorderColor = Color.MAGENTA;
    mFilledColor = Color.YELLOW;
    mFilledColor =
        (int) Math.round(Math.random() * 32 + 224) + (int) Math.round(Math.random() * 32 + 224) * 256
            + (int) Math.round(Math.random() * 32 + 224) * 256 * 256 + (int) Math.round(Math.random() * 32 + 224) * 256 * 256 * 256;
    mTextColor = Color.BLACK;
    mBorderSize = 1;
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
  public String toString() {
    return getName().equals("") ? "no name" : getName();
  }

}
