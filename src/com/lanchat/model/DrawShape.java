package com.lanchat.model;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

public class DrawShape implements Serializable {
  private static final long serialVersionUID = 4L;

  public static final int TYPE_SELECT = 0; // Công cụ chọn & di chuyển
  public static final int TYPE_PEN = 1;
  public static final int TYPE_LINE = 2;
  public static final int TYPE_RECT = 3;
  public static final int TYPE_OVAL = 4;
  public static final int TYPE_ERASER = 5;
  public static final int TYPE_ARROW = 6;
  public static final int TYPE_TEXT = 7;
  public static final int TYPE_IMAGE = 8; // Chèn ảnh từ máy tính lên bảng vẽ

  private String id;
  private int type;
  private int x1, y1, x2, y2;
  private int colorRGB;
  private float strokeWidth;
  private boolean filled;
  private String text;
  private byte[] imageBytes;
  private transient BufferedImage cachedImage;
  private List<Point> pathPoints;

  public DrawShape() {
    this.id = UUID.randomUUID().toString();
    this.pathPoints = new ArrayList<>();
  }

  public DrawShape(
      int type, int x1, int y1, int x2, int y2, int colorRGB, float strokeWidth, boolean filled) {
    this();
    this.type = type;
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    this.colorRGB = colorRGB;
    this.strokeWidth = strokeWidth;
    this.filled = filled;
  }

  public DrawShape(int type, int x1, int y1, String text, int colorRGB, float strokeWidth) {
    this();
    this.type = type;
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x1 + (text != null ? text.length() * 11 : 100);
    this.y2 = y1;
    this.text = text;
    this.colorRGB = colorRGB;
    this.strokeWidth = strokeWidth;
  }

  public DrawShape(int x1, int y1, int width, int height, byte[] imageBytes) {
    this();
    this.type = TYPE_IMAGE;
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x1 + width;
    this.y2 = y1 + height;
    this.imageBytes = imageBytes;
    this.strokeWidth = 2.0f;
    this.colorRGB = Color.GRAY.getRGB();
  }

  public void addPoint(int x, int y) {
    pathPoints.add(new Point(x, y));
  }

  public void move(int dx, int dy) {
    x1 += dx;
    y1 += dy;
    x2 += dx;
    y2 += dy;
    for (Point p : pathPoints) {
      p.x += dx;
      p.y += dy;
    }
  }

  public boolean contains(int px, int py) {
    double hitRadius = Math.max(strokeWidth + 6, 10);
    int minX = Math.min(x1, x2) - 8;
    int maxX = Math.max(x1, x2) + 8;
    int minY = Math.min(y1, y2) - 8;
    int maxY = Math.max(y1, y2) + 8;

    switch (type) {
      case TYPE_RECT:
      case TYPE_OVAL:
      case TYPE_IMAGE:
        return px >= minX && px <= maxX && py >= minY && py <= maxY;
      case TYPE_LINE:
      case TYPE_ARROW:
        return Line2D.ptSegDist(x1, y1, x2, y2, px, py) <= hitRadius;
      case TYPE_TEXT:
        return px >= x1 - 6 && px <= x1 + 220 && py >= y1 - 28 && py <= y1 + 10;
      case TYPE_PEN:
        if (pathPoints.size() < 2) {
          return Math.hypot(px - x1, py - y1) <= hitRadius;
        }
        for (int i = 0; i < pathPoints.size() - 1; i++) {
          Point pA = pathPoints.get(i);
          Point pB = pathPoints.get(i + 1);
          if (Line2D.ptSegDist(pA.x, pA.y, pB.x, pB.y, px, py) <= hitRadius) {
            return true;
          }
        }
        return false;
      default:
        return false;
    }
  }

  public void draw(Graphics2D g2d) {
    g2d.setColor(new Color(colorRGB));
    g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    int rx = Math.min(x1, x2);
    int ry = Math.min(y1, y2);
    int rw = Math.abs(x1 - x2);
    int rh = Math.abs(y1 - y2);

    switch (type) {
      case TYPE_PEN:
      case TYPE_ERASER:
        if (type == TYPE_ERASER) g2d.setColor(Color.WHITE);
        if (pathPoints.size() > 1) {
          for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point a = pathPoints.get(i);
            Point b = pathPoints.get(i + 1);
            g2d.drawLine(a.x, a.y, b.x, b.y);
          }
        } else {
          g2d.drawLine(x1, y1, x2, y2);
        }
        break;
      case TYPE_LINE:
        g2d.drawLine(x1, y1, x2, y2);
        break;
      case TYPE_RECT:
        if (filled) g2d.fillRect(rx, ry, rw, rh);
        else g2d.drawRect(rx, ry, rw, rh);
        break;
      case TYPE_OVAL:
        if (filled) g2d.fillOval(rx, ry, rw, rh);
        else g2d.drawOval(rx, ry, rw, rh);
        break;
      case TYPE_ARROW:
        drawArrow(g2d, x1, y1, x2, y2, (int) strokeWidth * 4 + 8);
        break;
      case TYPE_TEXT:
        if (text != null) {
          g2d.setFont(new Font("Segoe UI", Font.BOLD, (int) (strokeWidth * 3) + 14));
          g2d.drawString(text, x1, y1);
        }
        break;
      case TYPE_IMAGE:
        if (imageBytes != null) {
          try {
            if (cachedImage == null) {
              cachedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            }
            if (cachedImage != null) {
              g2d.drawImage(cachedImage, rx, ry, rw, rh, null);
            }
          } catch (Exception ignored) {
          }
        }
        break;
    }
  }

  private void drawArrow(Graphics2D g, int ax1, int ay1, int ax2, int ay2, int arrowSize) {
    double dx = ax2 - ax1, dy = ay2 - ay1;
    double angle = Math.atan2(dy, dx);
    int len = (int) Math.sqrt(dx * dx + dy * dy);
    AffineTransform at = AffineTransform.getTranslateInstance(ax1, ay1);
    at.concatenate(AffineTransform.getRotateInstance(angle));
    AffineTransform old = g.getTransform();
    g.transform(at);

    g.drawLine(0, 0, len, 0);
    g.fillPolygon(
        new int[] {len, len - arrowSize, len - arrowSize, len},
        new int[] {0, -arrowSize / 2, arrowSize / 2, 0},
        4);
    g.setTransform(old);
  }

  public void drawSelectionBox(Graphics2D g) {
    int minX = Math.min(x1, x2) - 6;
    int maxX = Math.max(x1, x2) + 6;
    int minY = Math.min(y1, y2) - 6;
    int maxY = Math.max(y1, y2) + 6;
    if (type == TYPE_PEN && !pathPoints.isEmpty()) {
      minX = pathPoints.get(0).x;
      maxX = pathPoints.get(0).x;
      minY = pathPoints.get(0).y;
      maxY = pathPoints.get(0).y;
      for (Point p : pathPoints) {
        minX = Math.min(minX, p.x);
        maxX = Math.max(maxX, p.x);
        minY = Math.min(minY, p.y);
        maxY = Math.max(maxY, p.y);
      }
      minX -= 6;
      maxX += 6;
      minY -= 6;
      maxY += 6;
    }
    if (type == TYPE_TEXT) {
      minX = x1 - 4;
      maxX = x1 + 200;
      minY = y1 - 24;
      maxY = y1 + 8;
    }

    g.setColor(new Color(37, 99, 235));
    float[] dash = {6.0f, 4.0f};
    g.setStroke(
        new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
    g.drawRect(minX, minY, maxX - minX, maxY - minY);

    g.setColor(new Color(37, 99, 235));
    g.fillRect(minX - 3, minY - 3, 6, 6);
    g.fillRect(maxX - 3, minY - 3, 6, 6);
    g.fillRect(minX - 3, maxY - 3, 6, 6);
    g.fillRect(maxX - 3, maxY - 3, 6, 6);
  }

  public String getTypeName() {
    switch (type) {
      case TYPE_PEN:
        return "PEN";
      case TYPE_LINE:
        return "LINE";
      case TYPE_RECT:
        return "RECT";
      case TYPE_OVAL:
        return "OVAL";
      case TYPE_ERASER:
        return "ERASER";
      case TYPE_ARROW:
        return "ARROW";
      case TYPE_TEXT:
        return "TEXT";
      case TYPE_IMAGE:
        return "IMAGE";
      default:
        return "SHAPE";
    }
  }

  public String getId() {
    return id;
  }

  public int getType() {
    return type;
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

  public void setCoords(int x1, int y1, int x2, int y2) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
  }
}
