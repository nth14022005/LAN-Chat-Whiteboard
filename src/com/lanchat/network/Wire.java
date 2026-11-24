package com.lanchat.network;

import java.io.*;

public final class Wire {
  public static ObjectInputStream input(InputStream in) throws IOException {
    ObjectInputStream s = new ObjectInputStream(in);
    s.setObjectInputFilter(
        info -> {
          if (info.depth() > 40 || info.arrayLength() > 8 * 1024 * 1024)
            return ObjectInputFilter.Status.REJECTED;
          Class<?> c = info.serialClass();
          if (c == null) return ObjectInputFilter.Status.UNDECIDED;
          while (c.isArray()) c = c.getComponentType();
          if (c.isPrimitive()) return ObjectInputFilter.Status.ALLOWED;
          String n = c.getName();
          return n.startsWith("com.lanchat.model.")
                  || n.equals("java.util.ArrayList")
                  || n.equals("java.awt.Point")
                  || n.equals("java.awt.geom.Point2D")
                  || n.equals("java.lang.Object")
                  || n.equals("java.lang.String")
                  || n.equals("java.lang.Boolean")
                  || n.equals("java.lang.Number")
                  || n.equals("java.lang.Integer")
                  || n.equals("java.lang.Enum")
              ? ObjectInputFilter.Status.ALLOWED
              : ObjectInputFilter.Status.REJECTED;
        });
    return s;
  }

  public static byte[] bytes(Object v) {
    try {
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      try (ObjectOutputStream o = new ObjectOutputStream(b)) {
        o.writeObject(v);
      }
      return b.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T copy(T v) {
    try (ObjectInputStream i = input(new ByteArrayInputStream(bytes(v)))) {
      return (T) i.readObject();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
