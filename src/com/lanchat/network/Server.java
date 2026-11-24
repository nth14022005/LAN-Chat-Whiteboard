package com.lanchat.network;

import com.lanchat.database.*;
import com.lanchat.model.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server extends Thread {
  private final int port, ownerId;
  private final String roomName;
  private int roomId;
  private ServerSocket socket;
  private volatile boolean running;
  private volatile boolean initialized;
  final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
  final DataStore store;
  final BoardState board = new BoardState();
  private boolean locked;
  private long boardRevision;
  private final java.util.concurrent.ExecutorService boardWrites =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "LAN-board-database");
            t.setDaemon(true);
            return t;
          });

  private void persistBoardTask(ClientHandler author, Runnable task) {
    boardWrites.execute(
        () -> {
          try {
            task.run();
          } catch (Exception e) {
            author.sendPacket(
                new Packet(
                    Packet.Command.NOTICE,
                    "Server",
                    "Đồng bộ vẫn hoạt động; chưa ghi được nhật ký SQL: " + e.getMessage()));
          }
        });
  }

  synchronized void sendBoardState(ClientHandler target, long requestId, String message) {
    target.sendPacket(
        new Packet(
            Packet.Command.BOARD_STATE,
            "Server",
            new BoardSnapshot(board.snapshot(), locked, boardRevision, requestId, message)));
  }

  private void broadcastBoard(ClientHandler author, long requestId) {
    java.util.List<DrawShape> snapshot = board.snapshot();
    for (ClientHandler c : clients)
      if (c.session != null)
        c.sendPacket(
            new Packet(
                Packet.Command.BOARD_STATE,
                "Server",
                new BoardSnapshot(
                    snapshot, locked, boardRevision, c == author ? requestId : 0, null)));
  }

  public Server(int port, String name, int owner) {
    this(port, name, owner, new SqlDataStore());
  }

  public Server(int port, String name, int owner, DataStore store) {
    this.store = store;
    this.port = port;
    roomName = name;
    ownerId = owner;
    setName("LAN-server");
    setDaemon(true);
  }

  public void open() throws Exception {
    socket = new ServerSocket();
    socket.setReuseAddress(true);
    socket.bind(new InetSocketAddress(port));
    try {
      roomId = store.room(roomName, ownerId);
      store.lock(roomId, false);
      loadBoard();
      running = true;
      initialized = true;
      start();
    } catch (Exception e) {
      socket.close();
      throw e;
    }
  }

  public void run() {
    Thread beacon =
        new Thread(
            () -> {
              try (DatagramSocket d = new DatagramSocket()) {
                d.setBroadcast(true);
                byte[] b =
                    ("LAN_CHAT_SERVER:" + port).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                while (running) {
                  d.send(
                      new DatagramPacket(
                          b, b.length, InetAddress.getByName("255.255.255.255"), 9998));
                  Thread.sleep(1000);
                }
              } catch (Exception e) {
                if (running) System.err.println(e.getMessage());
              }
            },
            "LAN-discovery");
    beacon.setDaemon(true);
    beacon.start();
    while (running)
      try {
        Socket s = socket.accept();
        if (clients.size() >= 50) {
          s.close();
          continue;
        }
        ClientHandler h = new ClientHandler(s, this);
        clients.add(h);
        h.start();
      } catch (IOException e) {
        if (running) System.err.println(e.getMessage());
      }
  }

  synchronized SessionInfo login(ClientHandler h, String name, String password) throws Exception {
    User u = store.authenticate(name, password, h.ip());
    for (ClientHandler c : clients)
      if (c != h && c.session != null && c.session.userId == u.getId())
        throw new IllegalStateException("Tài khoản này đang ở trong phòng.");
    boolean host = u.getId() == ownerId && h.isLoopback();
    SessionInfo info =
        new SessionInfo(u.getId(), roomId, u.getUsername(), u.getFullName(), roomName, host);
    store.member(roomId, u.getId(), host);
    h.session = info;
    store.audit(u.getId(), "DANG_NHAP", "Tham gia phòng " + roomName, h.ip());
    return info;
  }

  synchronized void welcome(ClientHandler h) throws Exception {
    h.sendPacket(new Packet(Packet.Command.HISTORY, "Hệ thống", store.history(roomId)));
    sendBoardState(h, 0, null);
    broadcastUserList();
    broadcast(
        new Packet(
            Packet.Command.CHAT_MESSAGE, "Hệ thống", h.session.username + " đã tham gia phòng."),
        null);
  }

  synchronized void handle(ClientHandler h, Packet p) throws Exception {
    SessionInfo u = h.session;
    if (u == null) throw new IllegalStateException("Chưa đăng nhập.");
    Packet safe = new Packet(p.getCommand(), u.username, p.getTargetUser(), p.getPayload());
    switch (p.getCommand()) {
      case BOARD_REQUEST:
        sendBoardState(h, p.getRequestId(), null);
        break;
      case CHAT_MESSAGE:
        {
          String text = text(p);
          store.group(roomId, u.userId, text);
          broadcast(new Packet(p.getCommand(), u.username, text), null);
          break;
        }
      case PRIVATE_MESSAGE:
        {
          String text = text(p);
          ClientHandler target = null;
          for (ClientHandler c : clients)
            if (c.session != null && c.session.username.equals(p.getTargetUser())) target = c;
          if (target == null) throw new IllegalStateException("Người nhận đã rời phòng.");
          store.direct(u.userId, target.session.userId, text);
          target.sendPacket(safe);
          if (target != h) h.sendPacket(safe);
          break;
        }
      case TYPING_INDICATOR:
        if (p.getPayload() instanceof Boolean) broadcast(safe, h);
        break;
      case LOCK_WHITEBOARD:
        if (!u.host) throw new IllegalStateException("Chỉ chủ phòng được khóa bảng.");
        boolean nextLocked = (Boolean) p.getPayload();
        locked = nextLocked;
        boardRevision++;
        broadcastBoard(h, p.getRequestId());
        persistBoardTask(
            h,
            () -> {
              try {
                store.lock(roomId, nextLocked);
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            });
        break;
      case BOARD_EDIT:
      case BOARD_UNDO:
      case BOARD_REDO:
      case CLEAR_WHITEBOARD:
      case LOAD_BOARD:
        if (locked && !u.host) throw new IllegalStateException("Chủ phòng đang khóa bảng.");
        if (p.getCommand() == Packet.Command.BOARD_EDIT) {
          board.edit(u.userId, (BoardEdit) p.getPayload());
          java.util.List<DrawShape> logged = Wire.copy(((BoardEdit) p.getPayload()).after);
          persistBoardTask(
              h,
              () -> {
                try {
                  for (DrawShape shape : logged) store.stroke(roomId, u.userId, shape);
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
              });
        } else if (p.getCommand() == Packet.Command.BOARD_UNDO) board.undo(u.userId);
        else if (p.getCommand() == Packet.Command.BOARD_REDO) board.redo(u.userId);
        else if (p.getCommand() == Packet.Command.CLEAR_WHITEBOARD) {
          if (!u.host) throw new IllegalStateException("Chỉ chủ phòng được xóa toàn bảng.");
          board.edit(u.userId, new BoardEdit(board.snapshot(), new ArrayList<>()));
        } else {
          if (!u.host) throw new IllegalStateException("Chỉ chủ phòng được mở bản đã lưu.");
          loadBoard();
        }
        boardRevision++;
        broadcastBoard(h, p.getRequestId());
        break;
      case SAVE_BOARD:
        if (!u.host) throw new IllegalStateException("Chỉ chủ phòng được lưu phiên bảng.");
        saveBoard();
        h.sendPacket(
            new Packet(Packet.Command.FILE_ACK, "Server", "Đã lưu phiên bảng vẽ trên máy Host."));
        break;
      case EXPORT_RECORD:
        String[] export = (String[]) p.getPayload();
        if (export.length != 3
            || export[0].length() > 150
            || export[1].length() > 500
            || export[2].length() > 50)
          throw new IllegalArgumentException("Thông tin ảnh không hợp lệ.");
        store.export(roomId, u.userId, export[0], export[1], export[2]);
        break;
      case FILE_CHUNK:
        h.receiveChunk((FileChunk) p.getPayload());
        break;
      case LOGOUT:
        h.close();
        break;
      default:
        throw new IllegalStateException("Lệnh không hợp lệ.");
    }
  }

  private String text(Packet p) {
    String t = (String) p.getPayload();
    if (t == null || t.trim().isEmpty() || t.length() > 5000)
      throw new IllegalArgumentException("Tin nhắn phải có 1–5000 ký tự.");
    return t;
  }

  public int getRoomId() {
    return roomId;
  }

  public void broadcast(Packet p, ClientHandler exclude) {
    for (ClientHandler c : clients) if (c != exclude && c.session != null) c.sendPacket(p);
  }

  public void broadcastUserList() {
    ArrayList<String> a = new ArrayList<>();
    for (ClientHandler c : clients) if (c.session != null) a.add(c.session.username);
    broadcast(new Packet(Packet.Command.USER_LIST, "Server", a), null);
  }

  synchronized void remove(ClientHandler h) {
    clients.remove(h);
    if (h.session != null) {
      store.offline(h.session.userId, h.ip());
      broadcastUserList();
      broadcast(
          new Packet(
              Packet.Command.CHAT_MESSAGE, "Hệ thống", h.session.username + " đã rời phòng."),
          null);
    }
  }

  Path boardFile() {
    return Paths.get("storage", "boards", "room_" + roomId + ".board");
  }

  synchronized void saveBoard() throws IOException {
    Path f = boardFile();
    Files.createDirectories(f.getParent());
    Path temp = f.resolveSibling(f.getFileName() + ".tmp");
    try (ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(temp))) {
      o.writeObject(board.snapshot());
    }
    Files.move(temp, f, StandardCopyOption.REPLACE_EXISTING);
  }

  @SuppressWarnings("unchecked")
  synchronized void loadBoard() throws Exception {
    Path f = boardFile();
    if (Files.exists(f)) {
      try (ObjectInputStream in = Wire.input(Files.newInputStream(f))) {
        board.load((List<DrawShape>) in.readObject());
      }
    }
  }

  public void stopServer() {
    running = false;
    try {
      if (initialized) saveBoard();
    } catch (Exception e) {
      System.err.println("Lưu bảng: " + e.getMessage());
    }
    try {
      if (socket != null) socket.close();
    } catch (IOException e) {
    }
    boardWrites.shutdown();
    try {
      if (!boardWrites.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS))
        boardWrites.shutdownNow();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    for (ClientHandler c : clients) c.close();
    if (Thread.currentThread() != this)
      try {
        join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
  }
}
