package com.macrowen.macromap.draw.data;

public class JSONData<T> {

  private T data;

  public JSONData(T data) {
    this.data = data;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

}
