package com.lanchat.model;

import java.io.Serializable;

public class FileChunk implements Serializable {
  private static final long serialVersionUID = 1L;
  public final String id, name;
  public final long size, offset;
  public final byte[] bytes;

  public FileChunk(String i, String n, long s, long o, byte[] b) {
    id = i;
    name = n;
    size = s;
    offset = o;
    bytes = b;
  }
}
