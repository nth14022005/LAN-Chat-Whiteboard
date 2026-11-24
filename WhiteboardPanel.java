package com.lanchat.ui;

import com.lanchat.model.BoardEdit;
import com.lanchat.model.BoardSnapshot;
import com.lanchat.model.DrawShape;
import com.lanchat.model.Packet;
import com.lanchat.network.Client;
import com.lanchat.network.Wire;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class WhiteboardPanel extends JPanel {
  public interface StatusCallback {
    void onCursorMoved(int x, int y, int shapeCount, String activeToolName);

    void onBoardActionLogged(String actionName, String detail);

    default void onBoardStatusChanged(String text) {}
  }

  private Client client;
  private StatusCallback statusCallback;

  private List<DrawShape> shapes = Collections.synchronizedList(new ArrayList<>());
  private DrawShape selectedShape = null;
  private DrawShape currentDrawingShape = null;

  private int currentTool = DrawShape.TYPE_PEN;
  private Color currentColor = new Color(30, 41, 59);
  private float currentStroke = 3.0f;
  private boolean isFilled = false;
  private boolean showGrid = true;
  private boolean isBoardLocked = false;
  private boolean isHost = false;

  private List<DrawShape> baseline = new ArrayList<>();
  private boolean pending;
  private long pendingId, revision = -1;
  private boolean gestureActive;
  private List<DrawShape> authoritative = new ArrayList<>();
  private final Timer syncWatchdog;

  private int startX, startY;
  private int lastX, lastY;

  public WhiteboardPanel(Client client, boolean isHost) {
    this.client = client;
    this.isHost = isHost;
    syncWatchdog =
        new Timer(
            6000,
            e -> {
              if (!client.isConnected()) {
                connectionLost();
                return;
              }
              if (pending) {
                pendingId = client.sendBoardCommand(Packet.Command.BOARD_REQUEST, null);
                report("Host phản hồi chậm; đang yêu cầu đồng bộ lại...");
              } else syncWatchdogStop();
            });
    syncWatchdog.setRepeats(true);
    setBackground(new Color(248, 250, 252));
    setPreferredSize(new Dimension(800, 600));

    MouseAdapter mouseHandler =
        new MouseAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            notifyStatus(e.getX(), e.getY());
          }

          @Override
          public void mousePressed(MouseEvent e) {
            if (!canEdit()) {
              report(getSyncStatus());
              return;
            }
            gestureActive = true;
            startX = e.getX();
            startY = e.getY();
            lastX = startX;
            lastY = startY;

            if (currentTool == DrawShape.TYPE_SELECT) {
              selectedShape = null;
              synchronized (shapes) {
                for (int i = shapes.size() - 1; i >= 0; i--) {
                  DrawShape s = shapes.get(i);
                  if (s.contains(startX, startY)) {
                    selectedShape = s;
                    saveUndoState();
                    break;
                  }
                }
              }
              repaint();
              notifyStatus(startX, startY);
              return;
            }

            saveUndoState();

            if (currentTool == DrawShape.TYPE_TEXT) {
              String text =
                  JOptionPane.showInputDialog(
                      WhiteboardPanel.this, "Nhập nội dung văn bản muốn chèn lên bảng:");
              if (text != null && !text.trim().isEmpty()) {
                DrawShape shape =
                    new DrawShape(
                        DrawShape.TYPE_TEXT,
                        startX,
                        startY,
                        text,
                        currentColor.getRGB(),
                        currentStroke);
                shapes.add(shape);

                if (statusCallback != null)
                  statusCallback.onBoardActionLogged("Vẽ chữ", "Chèn chữ: " + text);
                repaint();
                syncShapesToNetwork();
                notifyStatus(startX, startY);
              }
              gestureActive = false;
              return;
            }

            if (currentTool == DrawShape.TYPE_PEN || currentTool == DrawShape.TYPE_ERASER) {
              int col =
                  (currentTool == DrawShape.TYPE_ERASER)
                      ? Color.WHITE.getRGB()
                      : currentColor.getRGB();
              float strk =
                  (currentTool == DrawShape.TYPE_ERASER) ? currentStroke * 4 : currentStroke;
              currentDrawingShape =
                  new DrawShape(currentTool, startX, startY, startX, startY, col, strk, false);
              currentDrawingShape.addPoint(startX, startY);
            } else {
              currentDrawingShape =
                  new DrawShape(
                      currentTool,
                      startX,
                      startY,
                      startX,
                      startY,
                      currentColor.getRGB(),
                      currentStroke,
                      isFilled);
            }
          }

          @Override
          public void mouseDragged(MouseEvent e) {
            if (!gestureActive || !canEdit()) return;
            int curX = e.getX();
            int curY = e.getY();
            notifyStatus(curX, curY);

            if (currentTool == DrawShape.TYPE_SELECT && selectedShape != null) {
              int dx = curX - lastX;
              int dy = curY - lastY;
              selectedShape.move(dx, dy);
              lastX = curX;
              lastY = curY;
              repaint();
              return;
            }

            if (currentDrawingShape != null) {
              if (currentTool == DrawShape.TYPE_PEN || currentTool == DrawShape.TYPE_ERASER) {
                currentDrawingShape.addPoint(curX, curY);
                currentDrawingShape.setCoords(startX, startY, curX, curY);
              } else {
                currentDrawingShape.setCoords(startX, startY, curX, curY);
              }
              repaint();
            }
          }

          @Override
          public void mouseReleased(MouseEvent e) {
            if (!gestureActive) return;
            gestureActive = false;
            if (!canEdit()) {
              cancelDraft();
              return;
            }

            if (currentTool == DrawShape.TYPE_SELECT) {
              if (selectedShape != null) {
                syncShapesToNetwork();
                if (statusCallback != null)
                  statusCallback.onBoardActionLogged(
                      "Di chuyển hình", "Cập nhật vị trí " + selectedShape.getTypeName());
              }
              return;
            }

            if (currentDrawingShape != null) {
              int endX = e.getX();
              int endY = e.getY();
              currentDrawingShape.setCoords(startX, startY, endX, endY);
              shapes.add(currentDrawingShape);

              if (statusCallback != null)
                statusCallback.onBoardActionLogged(
                    "Vẽ hình", "Thêm mới đối tượng " + currentDrawingShape.getTypeName());
              currentDrawingShape = null;
              repaint();
              syncShapesToNetwork();
              notifyStatus(endX, endY);
            }
          }
        };

    addMouseListener(mouseHandler);
    addMouseMotionListener(mouseHandler);
  }

  public void setStatusCallback(StatusCallback callback) {
    this.statusCallback = callback;
  }

  private void notifyStatus(int x, int y) {
    if (statusCallback != null) {
      statusCallback.onCursorMoved(x, y, shapes.size(), getToolDisplayName());
    }
  }

  public String getToolDisplayName() {
    switch (currentTool) {
      case DrawShape.TYPE_SELECT:
        return "Di Chuyển & Chọn";
      case DrawShape.TYPE_PEN:
        return "Bút Vẽ Tự Do";
      case DrawShape.TYPE_LINE:
        return "Đoạn Thẳng";
      case DrawShape.TYPE_ARROW:
        return "Mũi Tên";
      case DrawShape.TYPE_RECT:
        return "Hình Chữ Nhật";
      case DrawShape.TYPE_OVAL:
        return "Hình Tròn / Elip";
      case DrawShape.TYPE_TEXT:
        return "Chèn Chữ";
      case DrawShape.TYPE_ERASER:
        return "Tẩy Xóa";
      default:
        return "Bút Vẽ";
    }
  }

  private boolean canEdit() {
    return client.isConnected() && !pending && (!isBoardLocked || isHost);
  }

  private void saveUndoState() {
    baseline = Wire.copy(new ArrayList<>(shapes));
  }

  public void deleteSelectedShape() {
    if (!canEdit()) return;
    if (selectedShape != null) {
      saveUndoState();
      shapes.remove(selectedShape);
      if (statusCallback != null)
        statusCallback.onBoardActionLogged(
            "Xóa hình chọn", "Đã xóa đối tượng " + selectedShape.getTypeName());
      selectedShape = null;
      repaint();
      syncShapesToNetwork();
      notifyStatus(lastX, lastY);
    } else {
      JOptionPane.showMessageDialog(
          this,
          "Hãy dùng công cụ 'Di Chuyển' click vào một hình trên bảng trước khi bấm Xóa Hình Chọn!");
    }
  }

  public void importImageToBoard(File imgFile) {
    if (!canEdit()) return;
    try {
      if (imgFile.length() > 2 * 1024 * 1024)
        throw new IllegalArgumentException("Ảnh tối đa 2 MB.");
      byte[] bytes = Files.readAllBytes(imgFile.toPath());
      BufferedImage bimg = ImageIO.read(imgFile);
      if (bimg == null || (long) bimg.getWidth() * bimg.getHeight() > 20000000)
        throw new IllegalArgumentException("Chọn ảnh PNG/JPG hợp lệ, tối đa 20 triệu điểm ảnh.");
      int w = 320;
      int h = 240;
      if (bimg != null) {
        w = Math.min(420, bimg.getWidth());
        h = (int) ((double) bimg.getHeight() / bimg.getWidth() * w);
      }
      saveUndoState();
      DrawShape imgShape = new DrawShape(80, 60, w, h, bytes);
      shapes.add(imgShape);
      selectedShape = imgShape;
      setCurrentTool(DrawShape.TYPE_SELECT);
      repaint();
      syncShapesToNetwork();
      if (statusCallback != null)
        statusCallback.onBoardActionLogged("Chèn ảnh", "Tải ảnh lên bảng vẽ: " + imgFile.getName());
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this, "Không thể nạp ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
  }

  public void undo() {
    if (canEdit()) submit(Packet.Command.BOARD_UNDO, null);
    else report(getSyncStatus());
  }

  public void redo() {
    if (canEdit()) submit(Packet.Command.BOARD_REDO, null);
    else report(getSyncStatus());
  }

  private void submit(Packet.Command command, Object payload) {
    pendingId = client.sendBoardCommand(command, payload);
    pending = pendingId > 0;
    if (pending) syncWatchdog.restart();
    else syncWatchdog.stop();
    publishStatus();
  }

  public void requestSync() {
    if (!client.isConnected()) {
      connectionLost();
      return;
    }
    cancelDraft();
    submit(Packet.Command.BOARD_REQUEST, null);
  }

  private void syncWatchdogStop() {
    syncWatchdog.stop();
  }

  private void report(String message) {
    if (statusCallback != null) statusCallback.onBoardActionLogged("Bảng vẽ", message);
    publishStatus();
  }

  private void publishStatus() {
    if (statusCallback != null) statusCallback.onBoardStatusChanged(getSyncStatus());
  }

  public String getSyncStatus() {
    if (!client.isConnected()) return "Mất kết nối Host";
    if (isBoardLocked && !isHost) return "Bảng đang khóa bởi Host";
    if (pending) return "Đang chờ Host xác nhận nét vẽ...";
    return isBoardLocked ? "Bảng khóa — Host được vẽ" : "Bảng đang mở";
  }

  public int getShapeCount() {
    return shapes.size();
  }

  public boolean isSyncPending() {
    return pending;
  }

  public boolean isReadyToEdit() {
    return canEdit();
  }

  public void connectionLost() {
    pending = false;
    pendingId = 0;
    syncWatchdog.stop();
    cancelDraft();
    publishStatus();
  }

  public void disposePanel() {
    syncWatchdog.stop();
  }

  private void cancelDraft() {
    gestureActive = false;
    currentDrawingShape = null;
    selectedShape = null;
    shapes.clear();
    shapes.addAll(Wire.copy(authoritative));
    baseline = Wire.copy(authoritative);
    repaint();
    notifyStatus(lastX, lastY);
  }

  public void applyBoardState(BoardSnapshot state) {
    if (state.revision < revision) return;
    revision = state.revision;
    authoritative = Wire.copy(state.shapes);
    boolean wasLocked = isBoardLocked;
    isBoardLocked = state.locked;
    if (state.requestId > 0 && state.requestId == pendingId) {
      pending = false;
      pendingId = 0;
      syncWatchdog.stop();
    }
    if (isBoardLocked && !isHost) cancelDraft();
    else if (!gestureActive && !pending) cancelDraft();
    if (wasLocked && !isBoardLocked && !pending) cancelDraft();
    if (state.message != null) report(state.message);
    publishStatus();
  }

  public void syncShapesToNetwork() {
    java.util.Map<String, DrawShape> before = new java.util.LinkedHashMap<>(),
        after = new java.util.LinkedHashMap<>();
    for (DrawShape s : baseline) before.put(s.getId(), s);
    for (DrawShape s : shapes) after.put(s.getId(), s);
    List<DrawShape> old = new ArrayList<>(), fresh = new ArrayList<>();
    java.util.Set<String> ids = new java.util.LinkedHashSet<>(before.keySet());
    ids.addAll(after.keySet());
    for (String id : ids) {
      DrawShape a = before.get(id), b = after.get(id);
      if (!java.util.Arrays.equals(Wire.bytes(a), Wire.bytes(b))) {
        if (a != null) old.add(a);
        if (b != null) fresh.add(b);
      }
    }
    if (old.isEmpty() && fresh.isEmpty()) {
      cancelDraft();
      return;
    }
    submit(Packet.Command.BOARD_EDIT, new BoardEdit(old, Wire.copy(fresh)));
  }

  // Legacy listener adapter. Current protocol uses applyBoardState with acknowledgement IDs.
  public void applyShapeSync(List<DrawShape> newShapes) {
    authoritative = Wire.copy(newShapes);
    if (!gestureActive && !pending) cancelDraft();
  }

  public void clearBoard(boolean notifyNetwork) {
    if (notifyNetwork) {
      if (canEdit() && isHost) submit(Packet.Command.CLEAR_WHITEBOARD, null);
      else report(getSyncStatus());
    } else {
      authoritative.clear();
      cancelDraft();
    }
  }

  public void saveToPNG(File file) {
    int w = Math.max(getWidth(), 800);
    int h = Math.max(getHeight(), 600);
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, w, h);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    synchronized (shapes) {
      for (DrawShape s : shapes) {
        s.draw(g);
      }
    }
    g.dispose();

    try {
      ImageIO.write(img, "PNG", file);
      client.sendPacket(
          new Packet(
              Packet.Command.EXPORT_RECORD,
              client.getUsername(),
              new String[] {file.getName(), file.getAbsolutePath(), w + "x" + h}));

      if (statusCallback != null)
        statusCallback.onBoardActionLogged("Lưu PNG", "Xuất bảng vẽ ra file: " + file.getName());
      JOptionPane.showMessageDialog(
          this, "Đã lưu bản vẽ Whiteboard thành công tại:\n" + file.getAbsolutePath());
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this, "Lỗi khi lưu ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    if (showGrid) {
      g2d.setColor(new Color(218, 226, 236));
      int step = 24;
      for (int x = 0; x < getWidth(); x += step) g2d.drawLine(x, 0, x, getHeight());
      for (int y = 0; y < getHeight(); y += step) g2d.drawLine(0, y, getWidth(), y);
    }

    synchronized (shapes) {
      for (DrawShape s : shapes) {
        s.draw(g2d);
      }
    }

    if (currentDrawingShape != null) {
      currentDrawingShape.draw(g2d);
    }

    if (currentTool == DrawShape.TYPE_SELECT && selectedShape != null) {
      selectedShape.drawSelectionBox(g2d);
    }

    g2d.dispose();
  }

  public void toggleGrid() {
    this.showGrid = !this.showGrid;
    repaint();
  }

  public void setBoardLocked(boolean locked) {
    boolean wasLocked = isBoardLocked;
    this.isBoardLocked = locked;
    if (locked && !isHost) cancelDraft();
    if (wasLocked && !locked) requestSync();
    publishStatus();
  }

  public boolean isBoardLocked() {
    return isBoardLocked;
  }

  public void setCurrentTool(int tool) {
    this.currentTool = tool;
    if (tool == DrawShape.TYPE_SELECT) {
      setCursor(new Cursor(Cursor.HAND_CURSOR));
    } else {
      setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
      selectedShape = null;
    }
    notifyStatus(lastX, lastY);
    repaint();
  }

  public Color getCurrentColor() {
    return currentColor;
  }

  public void setCurrentColor(Color color) {
    this.currentColor = color;
  }

  public void setCurrentStroke(float stroke) {
    this.currentStroke = stroke;
  }

  public void setFilled(boolean filled) {
    this.isFilled = filled;
  }
}
