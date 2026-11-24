package com.lanchat.database;

import com.lanchat.model.*;
import com.lanchat.network.DataStore;

public class SqlDataStore implements DataStore {
  public User authenticate(String u, String p, String ip) throws Exception {
    return UserDAO.authenticate(u, p, ip);
  }

  public void register(String[] f) throws Exception {
    UserDAO.register(f[0], f[1], f[2], f[3]);
  }

  public int room(String n, int u) {
    return RoomDAO.createOrGetRoom(n, u, null);
  }

  public void member(int r, int u, boolean h) {
    RoomDAO.addRoomMember(r, u, h ? "HOST" : "MEMBER");
  }

  public void lock(int r, boolean l) {
    RoomDAO.updateBoardLock(r, l);
  }

  public String history(int r) {
    return MessageDAO.history(r);
  }

  public void group(int r, int u, String t) {
    MessageDAO.saveGroupMessage(r, u, t, "TEXT");
  }

  public void direct(int u, int v, String t) {
    MessageDAO.savePrivateMessage(u, v, t);
  }

  public void file(int r, int u, String n, long z, String p) {
    FileTransferDAO.saveFileRecord(r, u, n, z, p);
  }

  public void export(int room, int user, String name, String path, String resolution) {
    WhiteboardDAO.saveWhiteboardExport(room, user, name, path, resolution);
  }

  public void offline(int u, String ip) {
    UserDAO.updateStatusById(u, false, ip);
  }

  public void stroke(int r, int u, DrawShape s) {
    WhiteboardDAO.saveStrokeDetail(r, u, s);
  }

  public void audit(int u, String a, String d, String ip) {
    AuditLogDAO.logAction(u, a, d, ip);
  }
}
