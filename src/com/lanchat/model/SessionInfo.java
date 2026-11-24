package com.lanchat.model;

import java.io.Serializable;

public class SessionInfo implements Serializable {
  private static final long serialVersionUID = 1L;
  public final int userId, roomId;
  public final String username, fullName, roomName;
  public final boolean host;

  public SessionInfo(int u, int r, String n, String f, String room, boolean h) {
    userId = u;
    roomId = r;
    username = n;
    fullName = f;
    roomName = room;
    host = h;
  }
}
