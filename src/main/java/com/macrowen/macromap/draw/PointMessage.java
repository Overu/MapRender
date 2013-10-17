package com.macrowen.macromap.draw;

public class PointMessage {

  private String mId;
  private String mName;
  private boolean mSelected;
  private String mType;

  public String getId() {
    return mId;
  }

  public String getName() {
    return mName;
  }

  public String getType() {
    return mType;
  }

  public boolean isSelected() {
    return mSelected;
  }

  public void setId(String mId) {
    this.mId = mId;
  }

  public void setName(String mName) {
    this.mName = mName;
  }

  public void setSelected(boolean mSelected) {
    this.mSelected = mSelected;
  }

  public void setType(String mType) {
    this.mType = mType;
  }

}
