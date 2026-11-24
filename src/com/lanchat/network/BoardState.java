package com.lanchat.network;

import com.lanchat.model.*;
import java.util.*;

/** Authoritative board; edits contain only changed objects. Undo is per user. */
public class BoardState {
  private final LinkedHashMap<String, DrawShape> shapes = new LinkedHashMap<>();
  private final Map<Integer, Deque<BoardEdit>> undo = new HashMap<>(), redo = new HashMap<>();

  public synchronized List<DrawShape> snapshot() {
    return Wire.copy(new ArrayList<>(shapes.values()));
  }

  private boolean same(DrawShape a, DrawShape b) {
    return a == null ? b == null : b != null && Arrays.equals(Wire.bytes(a), Wire.bytes(b));
  }

  private Map<String, DrawShape> index(List<DrawShape> a) {
    Map<String, DrawShape> m = new LinkedHashMap<>();
    for (DrawShape s : a) {
      if (s == null || s.getId() == null || m.put(s.getId(), s) != null)
        throw new IllegalArgumentException("Dữ liệu hình không hợp lệ.");
    }
    return m;
  }

  private void apply(BoardEdit e) {
    Map<String, DrawShape> b = index(e.before), a = index(e.after);
    Set<String> ids = new LinkedHashSet<>(b.keySet());
    ids.addAll(a.keySet());
    if (shapes.size() - b.size() + a.size() > 2000)
      throw new IllegalArgumentException("Bảng tối đa 2000 đối tượng.");
    for (String id : ids)
      if (!same(shapes.get(id), b.get(id)))
        throw new IllegalStateException(
            "Hình vừa được người khác thay đổi. Đã tải lại bảng; hãy thử lại.");
    LinkedHashMap<String, DrawShape> candidate = new LinkedHashMap<>(shapes);
    for (String id : ids) {
      if (a.containsKey(id)) candidate.put(id, a.get(id));
      else candidate.remove(id);
    }
    if (Wire.bytes(new ArrayList<>(candidate.values())).length > 16 * 1024 * 1024)
      throw new IllegalArgumentException("Bảng tối đa 16 MB. Lưu PNG rồi tạo bảng mới.");
    for (String id : ids) {
      if (a.containsKey(id)) shapes.put(id, Wire.copy(a.get(id)));
      else shapes.remove(id);
    }
  }

  public synchronized void edit(int user, BoardEdit e) {
    apply(e);
    Deque<BoardEdit> q = undo.computeIfAbsent(user, k -> new ArrayDeque<>());
    q.push(Wire.copy(e));
    while (q.size() > 30) q.removeLast();
    redo.remove(user);
  }

  public synchronized void undo(int u) {
    Deque<BoardEdit> q = undo.get(u);
    if (q == null || q.isEmpty()) return;
    BoardEdit e = q.peek();
    apply(new BoardEdit(e.after, e.before));
    q.pop();
    redo.computeIfAbsent(u, k -> new ArrayDeque<>()).push(e);
  }

  public synchronized void redo(int u) {
    Deque<BoardEdit> q = redo.get(u);
    if (q == null || q.isEmpty()) return;
    BoardEdit e = q.peek();
    apply(e);
    q.pop();
    undo.computeIfAbsent(u, k -> new ArrayDeque<>()).push(e);
  }

  public synchronized void load(List<DrawShape> a) {
    if (a.size() > 2000) throw new IllegalArgumentException("Bảng quá lớn.");
    shapes.clear();
    shapes.putAll(index(Wire.copy(a)));
    undo.clear();
    redo.clear();
  }
}
