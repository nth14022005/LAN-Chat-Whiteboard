package com.lanchat.model;

import java.io.Serializable;

public class DrawAction implements Serializable {
  private static final long serialVersionUID = 2L;

  public static final int TYPE_PEN = 1;
  public static final int TYPE_LINE = 2;
  public static final int TYPE_RECT = 3;
  public static final int TYPE_OVAL = 4;
  public static final int TYPE_ERASER = 5;
  public static final int TYPE_ARROW = 6;
  public static final int TYPE_TEXT = 7;
  public static final int TYPE_CLEAR = 8;
  public static final int TYPE_UNDO = 9;

  private int type;
  private int x1, y1, x2, y2;
  private int colorRGB;
  private float strokeWidth;
  private boolean filled;
  private String textContent;

  public DrawAction() {}

  public DrawAction(
      int type, int x1, int y1, int x2, int y2, int colorRGB, float strokeWidth, boolean filled) {
    this.type = type;
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    this.colorRGB = colorRGB;
    this.strokeWidth = strokeWidth;
    this.filled = filled;
  }

  public DrawAction(int type, int x1, int y1, String textContent, int colorRGB, float strokeWidth) {
    this.type = type;
    this.x1 = x1;
    this.y1 = y1;
    this.textContent = textContent;
    this.colorRGB = colorRGB;
    this.strokeWidth = strokeWidth;
  }

  public static DrawAction clearAction() {
    DrawAction action = new DrawAction();
    action.type = TYPE_CLEAR;
    return action;
  }

  public static DrawAction undoAction() {
    DrawAction action = new DrawAction();
    action.type = TYPE_UNDO;
    return action;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public int getX1() {
    return x1;
  }

  public int getY1() {
    return y1;
  }

  public int getX2() {
    return x2;
  }

  public int getY2() {
    return y2;
  }

  public int getColorRGB() {
    return colorRGB;
  }

  public float getStrokeWidth() {
    return strokeWidth;
  }

  public boolean isFilled() {
    return filled;
  }

  public String getTextContent() {
    return textContent;
  }

  public void setTextContent(String textContent) {
    this.textContent = textContent;
  }
}
