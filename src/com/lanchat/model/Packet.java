package com.lanchat.model;

import java.io.Serializable;

public class Packet implements Serializable {
  private static final long serialVersionUID = 5L;

  public enum Command {
    BOARD_STATE,
    BOARD_REQUEST,
    NOTICE,
    LOGIN,
    REGISTER,
    AUTH_OK,
    ERROR,
    HISTORY,
    BOARD_EDIT,
    BOARD_UNDO,
    BOARD_REDO,
    FILE_CHUNK,
    FILE_ACK,
    FILE_CANCEL,
    EXPORT_RECORD,
    SAVE_BOARD,
    LOAD_BOARD,
    LOGOUT,
    USER_LIST,
    CHAT_MESSAGE,
    PRIVATE_MESSAGE,
    TYPING_INDICATOR,
    DRAW_ACTION,
    SHAPE_SYNC,
    CLEAR_WHITEBOARD,
    LOCK_WHITEBOARD,
    FILE_TRANSFER_REQUEST,
    FILE_TRANSFER_DATA
  }

  private long requestId;

  public long getRequestId() {
    return requestId;
  }

  public Packet(Command command, String sender, Object payload, long requestId) {
    this(command, sender, payload);
    this.requestId = requestId;
  }

  private Command command;
  private String sender;
  private String targetUser;
  private Object payload;

  public Packet(Command command, String sender, Object payload) {
    this(command, sender, null, payload);
  }

  public Packet(Command command, String sender, String targetUser, Object payload) {
    this.command = command;
    this.sender = sender;
    this.targetUser = targetUser;
    this.payload = payload;
  }

  public Command getCommand() {
    return command;
  }

  public String getSender() {
    return sender;
  }

  public String getTargetUser() {
    return targetUser;
  }

  public Object getPayload() {
    return payload;
  }
}
