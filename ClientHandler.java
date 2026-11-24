package com.lanchat.network;

import com.lanchat.database.*;
import com.lanchat.model.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class ClientHandler extends Thread {
  private final Socket socket;
  private final Server server;
  private ObjectOutputStream out;
  private volatile boolean closed;
  SessionInfo session;
  private java.util.List<ClientHandler> recipients = new java.util.ArrayList<>();
  private String transferId;
  private long received, total;
  private Path transferPath;
  private OutputStream fileOut;

  public ClientHandler(Socket s, Server v) {
    socket = s;
    server = v;
    setDaemon(true);
    setName("LAN-client-" + s.getPort());
  }

  String ip() {
    return socket.getInetAddress().getHostAddress();
  }

  boolean isLoopback() {
    return socket.getInetAddress().isLoopbackAddress();
  }

  public void run() {
    try {
      socket.setSoTimeout(15000);
      out = new ObjectOutputStream(socket.getOutputStream());
      out.flush();
      ObjectInputStream in = Wire.input(socket.getInputStream());
      Packet first = (Packet) in.readObject();
      String[] a = (String[]) first.getPayload();
      if (first.getCommand() == Packet.Command.REGISTER) {
        server.store.register(a);
        sendPacket(new Packet(Packet.Command.AUTH_OK, "Server", "Đăng ký thành công."));
        return;
      }
      if (first.getCommand() != Packet.Command.LOGIN || a.length != 2)
        throw new IOException("Yêu cầu đăng nhập không hợp lệ.");
      SessionInfo info = server.login(this, a[0], a[1]);
      sendPacket(new Packet(Packet.Command.AUTH_OK, "Server", info));
      socket.setSoTimeout(0);
      server.welcome(this);
      while (!closed) {
        Packet p = (Packet) in.readObject();
        try {
          server.handle(this, p);
        } catch (Exception e) {
          if (p.getCommand() == Packet.Command.FILE_CHUNK) cancelFile();
          if (p.getRequestId() > 0 || p.getCommand() == Packet.Command.LOCK_WHITEBOARD)
            server.sendBoardState(this, p.getRequestId(), e.getMessage());
          else {
            sendPacket(new Packet(Packet.Command.ERROR, "Server", e.getMessage()));
            server.sendBoardState(this, 0, null);
          }
        }
      }
    } catch (Exception e) {
      if (!closed)
        sendPacket(
            new Packet(
                Packet.Command.ERROR,
                "Server",
                e.getMessage() == null ? "Mất kết nối." : e.getMessage()));
    } finally {
      close();
    }
  }

  public synchronized void sendPacket(Packet p) {
    try {
      if (out != null && !closed) {
        out.writeObject(p);
        out.flush();
        out.reset();
      }
    } catch (IOException e) {
      try {
        socket.close();
      } catch (IOException ignored) {
      }
    }
  }

  void receiveChunk(FileChunk f) throws Exception {
    if (f.id == null
        || f.id.length() > 100
        || f.size < 0
        || f.size > 50L * 1024 * 1024
        || f.bytes == null
        || f.bytes.length > 65536
        || f.offset < 0
        || f.offset + f.bytes.length > f.size
        || (f.size > 0 && f.bytes.length == 0)
        || f.name == null
        || f.name.length() > 200) throw new IOException("File không hợp lệ (tối đa 50 MB).");
    if (f.offset == 0) {
      if (fileOut != null) throw new IOException("Một file đang truyền.");
      recipients.clear();
      for (ClientHandler c : server.clients) if (c != this && c.session != null) recipients.add(c);
      transferId = f.id;
      total = f.size;
      received = 0;
      Path dir = Paths.get("storage", "files", "room_" + server.getRoomId());
      Files.createDirectories(dir);
      transferPath =
          dir.resolve(
              UUID.randomUUID()
                  + "_"
                  + new File(f.name).getName().replaceAll("[^\\p{L}0-9._-]", "_"));
      fileOut = Files.newOutputStream(transferPath);
    }
    if (fileOut == null
        || !Objects.equals(transferId, f.id)
        || f.offset != received
        || f.size != total) throw new IOException("Sai thứ tự gói file.");
    fileOut.write(f.bytes);
    received += f.bytes.length;
    for (ClientHandler c : recipients)
      c.sendPacket(new Packet(Packet.Command.FILE_CHUNK, session.username, f));
    if (received == total) {
      fileOut.close();
      fileOut = null;
      server.store.file(
          server.getRoomId(),
          session.userId,
          f.name,
          total,
          transferPath.toAbsolutePath().toString());
      sendPacket(new Packet(Packet.Command.FILE_ACK, "Server", "Host đã nhận đủ file: " + f.name));
      transferPath = null;
    }
  }

  void cancelFile() {
    try {
      if (fileOut != null) fileOut.close();
      if (transferPath != null) Files.deleteIfExists(transferPath);
    } catch (IOException e) {
    }
    fileOut = null;
    transferPath = null;
    if (session != null && transferId != null)
      for (ClientHandler c : recipients)
        c.sendPacket(new Packet(Packet.Command.FILE_CANCEL, session.username, transferId));
    recipients.clear();
    transferId = null;
  }

  public void close() {
    synchronized (this) {
      if (closed) return;
      closed = true;
    }
    try {
      socket.close();
    } catch (IOException e) {
    }
    cancelFile();
    server.remove(this);
  }
}
