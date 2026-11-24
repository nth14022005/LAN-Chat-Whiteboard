package com.lanchat.model;

import java.io.Serializable;

public class FileTransferInfo implements Serializable {
  private static final long serialVersionUID = 1L;
  private String fileName;
  private long fileSize;
  private String sender;
  private byte[] data;

  public FileTransferInfo(String fileName, long fileSize, String sender, byte[] data) {
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.sender = sender;
    this.data = data;
  }

  public String getFileName() {
    return fileName;
  }

  public long getFileSize() {
    return fileSize;
  }

  public String getSender() {
    return sender;
  }

  public byte[] getData() {
    return data;
  }
}
