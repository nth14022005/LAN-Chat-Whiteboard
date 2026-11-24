package com.lanchat.model;

import java.io.Serializable;
import java.util.List;

/** Board, permission and acknowledgement travel together in server order. */
public final class BoardSnapshot implements Serializable {
  private static final long serialVersionUID = 1L;
  public final List<DrawShape> shapes;
  public final boolean locked;
  public final long revision, requestId;
  public final String message;

  public BoardSnapshot(
      List<DrawShape> shapes, boolean locked, long revision, long requestId, String message) {
    this.shapes = shapes;
    this.locked = locked;
    this.revision = revision;
    this.requestId = requestId;
    this.message = message;
  }
}
