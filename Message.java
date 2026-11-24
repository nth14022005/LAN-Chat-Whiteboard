package com.lanchat.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Type {
    TEXT,
    SYSTEM,
    NOTIFICATION
  }

  private String sender;
  private String content;
  private Date timestamp;
  private Type type;

  public Message(String sender, String content) {
    this(sender, content, Type.TEXT);
  }

  public Message(String sender, String content, Type type) {
    this.sender = sender;
    this.content = content;
    this.type = type;
    this.timestamp = new Date();
  }

  public String getSender() {
    return sender;
  }

  public String getContent() {
    return content;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public Type getType() {
    return type;
  }

  public String getFormattedTime() {
    return new SimpleDateFormat("HH:mm:ss").format(timestamp);
  }
}
