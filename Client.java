package com.lanchat.network;

import com.lanchat.model.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;

public class Client {
  public interface Listener {
    default void onBoardState(BoardSnapshot state) {
      onLockWhiteboardReceived(state.locked);
      onShapeSyncReceived(state.shapes);
    }

    default void onNotice(String message) {}

    void onMessageReceived(String sender, String message);

    void onPrivateMessageReceived(String sender, String message);

    default void onPrivateMessage(String sender, String target, String message) {
      onPrivateMessageReceived(sender, message);
    }

    void onTypingIndicatorReceived(String sender, boolean typing);

    void onUserListUpdated(List<String> users);

    void onDrawActionReceived(DrawAction a);

    void onShapeSyncReceived(List<DrawShape> s);

    void onClearWhiteboardReceived();

    void onLockWhiteboardReceived(boolean locked);

    void onFileReceived(FileTransferInfo f);

    default void onError(String message) {}

    default void onDisconnected() {}

    default void onHistory(String history) {}

    default void onFileSaved(String sender, String name, Path temp, long size) {}

    default void onProgress(int percent, String message) {}
  }

  private final String host, password;
  private final int port;
  private String username;
  private Socket socket;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private Listener listener;
  private volatile boolean connected;
  private final java.util.concurrent.atomic.AtomicLong boardRequests =
      new java.util.concurrent.atomic.AtomicLong();
  private volatile boolean sendingFile;
  private volatile boolean leaving;
  private SessionInfo session;
  private String lastError = "";
  private final ExecutorService sender =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "LAN-send");
            t.setDaemon(true);
            return t;
          });
  private final Map<String, Incoming> incoming = new HashMap<>();

  private static class Incoming {
    Path path;
    OutputStream out;
    long count, size;
  }

  public Client(String h, int p, String u, String password, Listener l) {
    host = h;
    port = p;
    username = u;
    this.password = password;
    listener = l;
  }

  public void setListener(Listener l) {
    listener = l;
  }

  public SessionInfo getSession() {
    return session;
  }

  public String getError() {
    return lastError;
  }

  public boolean isConnected() {
    return connected;
  }

  public String getUsername() {
    return username;
  }

  public boolean connect() {
    try {
      socket = new Socket();
      socket.connect(new InetSocketAddress(host, port), 5000);
      socket.setSoTimeout(20000);
      out = new ObjectOutputStream(socket.getOutputStream());
      out.flush();
      in = Wire.input(socket.getInputStream());
      write(new Packet(Packet.Command.LOGIN, username, new String[] {username, password}));
      Packet p = (Packet) in.readObject();
      if (p.getCommand() != Packet.Command.AUTH_OK)
        throw new IOException(String.valueOf(p.getPayload()));
      session = (SessionInfo) p.getPayload();
      username = session.username;
      connected = true;
      socket.setSoTimeout(0);
      return true;
    } catch (Exception e) {
      lastError = e.getMessage();
      try {
        if (socket != null) socket.close();
      } catch (IOException x) {
      }
      return false;
    }
  }

  public void startListening() {
    Thread t =
        new Thread(
            () -> {
              try {
                while (connected) {
                  Packet p = (Packet) in.readObject();
                  if (p.getCommand() == Packet.Command.FILE_CHUNK)
                    receive((FileChunk) p.getPayload(), p.getSender());
                  else if (p.getCommand() == Packet.Command.FILE_CANCEL) {
                    Incoming x = incoming.remove(p.getSender() + ":" + p.getPayload());
                    if (x != null) {
                      x.out.close();
                      Files.deleteIfExists(x.path);
                    }
                  } else SwingUtilities.invokeLater(() -> handle(p));
                }
              } catch (Exception e) {
                if (!leaving)
                  SwingUtilities.invokeLater(
                      () -> {
                        if (listener != null) listener.onDisconnected();
                      });
              } finally {
                connected = false;
                try {
                  socket.close();
                } catch (Exception e) {
                }
                for (Incoming x : incoming.values())
                  try {
                    x.out.close();
                    Files.deleteIfExists(x.path);
                  } catch (IOException e) {
                  }
                incoming.clear();
              }
            },
            "LAN-receive");
    t.setDaemon(true);
    t.start();
  }

  @SuppressWarnings("unchecked")
  private void handle(Packet p) {
    if (listener == null || leaving) return;
    switch (p.getCommand()) {
      case BOARD_STATE:
        listener.onBoardState((BoardSnapshot) p.getPayload());
        break;
      case NOTICE:
        listener.onNotice((String) p.getPayload());
        break;
      case CHAT_MESSAGE:
        listener.onMessageReceived(p.getSender(), (String) p.getPayload());
        break;
      case PRIVATE_MESSAGE:
        listener.onPrivateMessage(p.getSender(), p.getTargetUser(), (String) p.getPayload());
        break;
      case HISTORY:
        listener.onHistory((String) p.getPayload());
        break;
      case ERROR:
        listener.onError((String) p.getPayload());
        break;
      case FILE_ACK:
        listener.onProgress(100, (String) p.getPayload());
        break;
      case USER_LIST:
        listener.onUserListUpdated((List<String>) p.getPayload());
        break;
      case SHAPE_SYNC:
        listener.onShapeSyncReceived((List<DrawShape>) p.getPayload());
        break;
      case LOCK_WHITEBOARD:
        listener.onLockWhiteboardReceived((Boolean) p.getPayload());
        break;
      case TYPING_INDICATOR:
        listener.onTypingIndicatorReceived(p.getSender(), (Boolean) p.getPayload());
        break;
      default:
        break;
    }
  }

  private synchronized void write(Packet p) throws IOException {
    out.writeObject(p);
    out.flush();
    out.reset();
  }

  public void sendPacket(Packet p) {
    if (!connected) return;
    try {
      sender.execute(
          () -> {
            try {
              write(p);
            } catch (IOException e) {
              failConnection(e);
            }
          });
    } catch (RejectedExecutionException e) {
      if (!leaving) failConnection(e);
    }
  }

  private void failConnection(Exception e) {
    lastError = "Không gửi được dữ liệu: " + e.getMessage();
    connected = false;
    try {
      if (socket != null) socket.close();
    } catch (IOException ignored) {
    }
  }

  public long sendBoardCommand(Packet.Command command, Object payload) {
    if (!connected) return -1;
    long id = boardRequests.incrementAndGet();
    sendPacket(new Packet(command, username, payload, id));
    return id;
  }

  public void sendChat(String t) {
    sendPacket(new Packet(Packet.Command.CHAT_MESSAGE, username, t));
  }

  public void sendPrivateChat(String u, String t) {
    sendPacket(new Packet(Packet.Command.PRIVATE_MESSAGE, username, u, t));
  }

  public void sendTypingStatus(boolean t) {
    sendPacket(new Packet(Packet.Command.TYPING_INDICATOR, username, t));
  }

  public void sendLockBoard(boolean t) {
    sendPacket(new Packet(Packet.Command.LOCK_WHITEBOARD, username, t));
  }

  public void sendClearBoard() {
    sendPacket(new Packet(Packet.Command.CLEAR_WHITEBOARD, username, null));
  }

  public void sendFile(File f) {
    if (!connected || sendingFile) {
      listener.onError("Chờ file đang gửi hoàn tất.");
      return;
    }
    if (f.length() > 50L * 1024 * 1024) {
      listener.onError("File tối đa 50 MB.");
      return;
    }
    sendingFile = true;
    sender.execute(
        () -> {
          try (InputStream file = Files.newInputStream(f.toPath())) {
            String id = UUID.randomUUID().toString();
            long size = f.length(), offset = 0;
            byte[] b = new byte[65536];
            do {
              int n = file.read(b);
              if (n < 0) n = 0;
              write(
                  new Packet(
                      Packet.Command.FILE_CHUNK,
                      username,
                      new FileChunk(id, f.getName(), size, offset, Arrays.copyOf(b, n))));
              offset += n;
              final int percent = size == 0 ? 100 : (int) (offset * 100 / size);
              SwingUtilities.invokeLater(
                  () -> listener.onProgress(percent, "Đã gửi " + percent + "% — " + f.getName()));
              if (n == 0 && offset < size) throw new IOException("File bị thay đổi trong lúc gửi.");
            } while (offset < size);
          } catch (Exception e) {
            SwingUtilities.invokeLater(
                () -> listener.onError("Gửi file thất bại: " + e.getMessage()));
          } finally {
            sendingFile = false;
          }
        });
  }

  private void receive(FileChunk f, String from) throws IOException {
    String key = from + ":" + f.id;
    Incoming x = incoming.get(key);
    if (f.offset == 0) {
      if (x != null) throw new IOException("Trùng file");
      x = new Incoming();
      x.size = f.size;
      x.path = Files.createTempFile("lan-receive-", ".part");
      x.out = Files.newOutputStream(x.path);
      incoming.put(key, x);
    }
    if (x == null
        || f.offset != x.count
        || f.size != x.size
        || f.bytes.length > 65536
        || f.size > 50L * 1024 * 1024) throw new IOException("File lỗi");
    x.out.write(f.bytes);
    x.count += f.bytes.length;
    if (x.count == x.size) {
      x.out.close();
      incoming.remove(key);
      final Path path = x.path;
      SwingUtilities.invokeLater(() -> listener.onFileSaved(from, f.name, path, f.size));
    }
  }

  public void disconnect() {
    leaving = true;
    connected = false;
    sender.shutdownNow();
    try {
      if (socket != null) socket.close();
    } catch (IOException e) {
    }
  }

  public static void register(String h, int port, String[] fields) throws Exception {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(h, port), 5000);
      socket.setSoTimeout(20000);
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      out.flush();
      ObjectInputStream in = Wire.input(socket.getInputStream());
      out.writeObject(new Packet(Packet.Command.REGISTER, fields[0], fields));
      out.flush();
      Packet p = (Packet) in.readObject();
      if (p.getCommand() != Packet.Command.AUTH_OK)
        throw new IOException(String.valueOf(p.getPayload()));
    }
  }
}
