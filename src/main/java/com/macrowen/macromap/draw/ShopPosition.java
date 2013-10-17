package com.macrowen.macromap.draw;

import com.macrowen.macromap.R;
import com.macrowen.macromap.MacroMap.OnMapEventType;

import android.view.View;
import android.widget.ImageButton;

import android.widget.TextView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

public class ShopPosition extends RelativeLayout {

  public interface OnMapEventListener {
    public void OnMapEvent(String string, OnMapEventType type);
  }

  Shop mShop;
  public boolean mShow = false;
  private OnMapEventListener mOnMapEventListener;

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

  public void setOnMapEventListener(OnMapEventListener onMapEventListener) {
    this.mOnMapEventListener = onMapEventListener;
  }

  public void setShop(Shop shop) {
    mShop = shop;
    setText(mShop.mDisplay);
  }

  public void setText(String display) {
    TextView text = (TextView) findViewById(R.id.shop_display);
    text.setText(display);
  }

  private void init() {
    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.shopposition, this, true);
    ImageButton share = (ImageButton) findViewById(R.id.shop_share);
    share.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        if (mOnMapEventListener != null && mShop != null) {
          mOnMapEventListener.OnMapEvent(mShop.getId(), OnMapEventType.MapClickedLeft);
        }
      }
    });
    ImageButton more = (ImageButton) findViewById(R.id.shop_more);
    more.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        if (mOnMapEventListener != null && mShop != null) {
          mOnMapEventListener.OnMapEvent(mShop.getId(), OnMapEventType.MapClickedRight);
        }
      }
    });
  }
}
