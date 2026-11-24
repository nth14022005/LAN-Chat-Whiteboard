package com.lanchat.model;

import java.io.Serializable;
import java.util.*;

public class BoardEdit implements Serializable {
  private static final long serialVersionUID = 1L;
  public final List<DrawShape> before, after;

  public BoardEdit(List<DrawShape> b, List<DrawShape> a) {
    before = b;
    after = a;
  }
}
