package com.lanchat.ui;

import com.lanchat.model.BoardSnapshot;
import com.lanchat.model.DrawAction;
import com.lanchat.model.DrawShape;
import com.lanchat.model.FileTransferInfo;
import com.lanchat.model.Packet;
import com.lanchat.network.Client;
import com.lanchat.network.Server;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

public class MainFrame extends JFrame implements Client.Listener, WhiteboardPanel.StatusCallback {
  private Client client;
  private Server localServer;
  private boolean leaving;
  private java.util.Map<String, StringBuilder> privateHistory = new java.util.LinkedHashMap<>();
  private javax.swing.Timer typingTimer;
  private JLabel usersHeader;
  private java.util.Map<Integer, Path> receivedRows = new java.util.HashMap<>();
  private java.util.List<Path> receivedTemps = new java.util.ArrayList<>();
  private DefaultTableModel transferModel =
      new DefaultTableModel(new String[] {"Người gửi", "Tên file", "Dung lượng", "Trạng thái"}, 0) {
        public boolean isCellEditable(int r, int c) {
          return false;
        }
      };

  public void setLocalServer(Server server) {
    this.localServer = server;
  }

  private boolean isHost;
  private String roomName;
  private String serverAddress;

  // Bảng màu Soft Charcoal Dark (Tông tối dịu mắt chuẩn Discord / IntelliJ Darcula)
  public static final Color BG_APP = new Color(24, 28, 40); // #181C28 (Nền tổng thể tối trầm)
  public static final Color BG_PANEL =
      new Color(34, 40, 56); // #222838 (Nền khối Sidebar / Toolbar)
  public static final Color BG_INPUT =
      new Color(27, 32, 46); // #1B202E (Nền khung Chat & danh sách)
  public static final Color BG_BTN = new Color(48, 57, 79); // #30394F (Nền nút công cụ tối dịu)
  public static final Color BG_BTN_ACTIVE =
      new Color(67, 56, 202); // #4338CA (Nút đang chọn Indigo)
  public static final Color BORDER_DARK = new Color(58, 69, 94); // #3A455E (Viền mảnh dịu)

  public static final Color PRIMARY = new Color(79, 110, 247); // #4F6EF7 (Xanh Indigo dịu)
  public static final Color SUCCESS = new Color(52, 211, 153); // #34D399 (Xanh ngọc)
  public static final Color DANGER = new Color(244, 63, 94); // #F43F5E (Đỏ Coral)
  public static final Color TEXT_MAIN = new Color(226, 232, 240); // #E2E8F0 (Chữ trắng xám dịu)
  public static final Color TEXT_MUTED = new Color(148, 163, 184); // #94A3B8 (Chữ xám phụ)

  public static final Font FONT_BOLD_13 = new Font("Segoe UI", Font.BOLD, 13);
  public static final Font FONT_BOLD_11 = new Font("Segoe UI", Font.BOLD, 11);
  public static final Font FONT_PLAIN_13 = new Font("Segoe UI", Font.PLAIN, 13);
  public static final Font FONT_PLAIN_11 = new Font("Segoe UI", Font.PLAIN, 11);

  // CỘT TRÁI: THÀNH VIÊN & 3 TAB
  private DefaultListModel<String> userListModel = new DefaultListModel<>();
  private JList<String> userList = new JList<>(userListModel);
  private JTabbedPane chatTabs = new JTabbedPane();
  private JTextArea groupChatArea = new JTextArea();
  private JTextArea privateChatArea = new JTextArea();
  private String currentPrivateUser = null;

  // Bảng quản lý Kho File và Nhật ký hệ thống (Tab 3)
  private DefaultTableModel fileTableModel =
      new DefaultTableModel(new String[] {"Thời gian", "Loại", "Nội dung / Tên File"}, 0) {
        public boolean isCellEditable(int r, int c) {
          return false;
        }
      };
  private JTable activityTable = new JTable(fileTableModel);

  private JTextField messageField = new JTextField();
  private JButton sendButton = createPrimaryBtn("Gửi");
  private JButton fileButton = createPrimaryBtn("Gửi File");
  private JLabel lblTyping = new JLabel(" ");
  private JProgressBar progressBar = new JProgressBar();

  // CỘT PHẢI: WHITEBOARD PRO
  private WhiteboardPanel whiteboardPanel;
  private JButton btnLockBoard;
  private JPanel colorPreviewBox = new JPanel();

  // THANH TRẠNG THÁI (STATUS BAR)
  private JLabel lblStatusLeft = new JLabel(" Sẵn sàng");
  private JLabel lblStatusCenter =
      new JLabel("Công cụ: Bút Vẽ Tự Do | Tọa độ: (0, 0) | Đối tượng: 0");
  private JLabel lblStatusRight = new JLabel("CSDL: Đang kiểm tra... ");

  public MainFrame(Client client, boolean isHost) {
    this(client, isHost, "Phòng Thảo Luận Đồ Án Java", "127.0.0.1:9999");
  }

  public MainFrame(Client client, boolean isHost, String roomName, String serverAddress) {
    super("Hệ Thống LAN Chat & Whiteboard Pro - [" + client.getUsername() + "] - " + roomName);
    this.client = client;
    this.isHost = isHost;
    this.roomName = roomName;
    this.serverAddress = serverAddress;

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(
        new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            leaveRoom();
          }
        });
    setSize(1300, 780);
    setMinimumSize(new Dimension(1080, 680));
    setLocationRelativeTo(null);
    getContentPane().setBackground(BG_APP);

    initUI();
    logActivity("Hệ Thống", "Khởi tạo phiên làm việc tại " + roomName);
  }

  private void initUI() {
    setLayout(new BorderLayout(8, 8));
    ((JPanel) getContentPane()).setBorder(new EmptyBorder(8, 8, 6, 8));

    // ==========================================
    // 1. CỘT TRÁI (365px CỐ ĐỊNH): ONLINE + 3 TABS + NHẬP LIỆU
    // ==========================================
    JPanel leftColumn = new JPanel(new BorderLayout(0, 8));
    leftColumn.setPreferredSize(new Dimension(365, 0));
    leftColumn.setBackground(BG_PANEL);
    leftColumn.setBorder(new LineBorder(BORDER_DARK, 1, true));

    // Khung thành viên online
    JPanel usersBox = new JPanel(new BorderLayout(0, 5));
    usersBox.setPreferredSize(new Dimension(0, 145));
    usersBox.setBackground(BG_PANEL);
    usersBox.setBorder(new EmptyBorder(8, 10, 4, 10));

    JPanel userHeader = new JPanel(new BorderLayout());
    userHeader.setBackground(BG_PANEL);
    JLabel lblUsers = new JLabel("ONLINE — Nhấp đúp để chat riêng");
    usersHeader = lblUsers;
    lblUsers.setFont(FONT_BOLD_11);
    lblUsers.setForeground(TEXT_MUTED);

    JButton btnExportChat = createDarkSmallBtn("Xuất Biên Bản (.txt)");
    btnExportChat.addActionListener(e -> exportChatHistory());

    userHeader.add(lblUsers, BorderLayout.WEST);
    JPanel userActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
    JButton leave = createDarkSmallBtn("Rời phòng");
    leave.addActionListener(e -> leaveRoom());
    userActions.add(btnExportChat);
    userActions.add(leave);
    usersBox.add(userActions, BorderLayout.SOUTH);
    usersBox.add(userHeader, BorderLayout.NORTH);

    userList.setBackground(BG_INPUT);
    userList.setForeground(TEXT_MAIN);
    userList.setFont(FONT_PLAIN_13);
    userList.setFixedCellHeight(26);
    userList.setCellRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl =
                (JLabel)
                    super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
            lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
            lbl.setIcon(new OnlineDotIcon());
            lbl.setText(
                value.toString()
                    + (value.toString().equals(client.getUsername())
                        ? " (Bạn" + (isHost ? " • Host" : "") + ")"
                        : ""));
            lbl.setBackground(isSelected ? BG_BTN : BG_INPUT);
            lbl.setForeground(isSelected ? SUCCESS : TEXT_MAIN);
            return lbl;
          }
        });

    userList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              String selected = userList.getSelectedValue();
              if (selected != null && !selected.equalsIgnoreCase(client.getUsername())) {
                openPrivateChat(selected);
              }
            }
          }
        });

    JScrollPane userScroll = new JScrollPane(userList);
    userScroll.setBorder(new LineBorder(BORDER_DARK, 1, true));
    userScroll.getViewport().setBackground(BG_INPUT);
    usersBox.add(userScroll, BorderLayout.CENTER);
    leftColumn.add(usersBox, BorderLayout.NORTH);

    // Khung 3 Tabs: Phòng Chung | Chat Riêng (1-1) | Kho File & Nhật Ký
    JPanel chatBox = new JPanel(new BorderLayout(0, 6));
    chatBox.setBackground(BG_PANEL);
    chatBox.setBorder(new EmptyBorder(2, 10, 10, 10));

    chatTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    chatTabs.setBackground(BG_PANEL);
    chatTabs.setForeground(TEXT_MAIN);
    chatTabs.setFont(FONT_BOLD_11);

    setupTextArea(groupChatArea);
    setupTextArea(privateChatArea);

    // Bảng Kho File & Nhật Ký ở Tab 3 (Tông Dark đồng bộ)
    activityTable.setFont(FONT_PLAIN_11);
    activityTable.setRowHeight(24);
    activityTable.setBackground(BG_INPUT);
    activityTable.setForeground(TEXT_MAIN);
    activityTable.setGridColor(BORDER_DARK);
    activityTable.getTableHeader().setFont(FONT_BOLD_11);
    activityTable.getTableHeader().setBackground(BG_BTN);
    activityTable.getTableHeader().setForeground(TEXT_MAIN);
    activityTable.getColumnModel().getColumn(0).setPreferredWidth(65);
    activityTable.getColumnModel().getColumn(1).setPreferredWidth(75);
    activityTable.getColumnModel().getColumn(2).setPreferredWidth(180);

    JScrollPane groupScroll = new JScrollPane(groupChatArea);
    groupScroll.setBorder(new LineBorder(BORDER_DARK, 1));
    JScrollPane privScroll = new JScrollPane(privateChatArea);
    privScroll.setBorder(new LineBorder(BORDER_DARK, 1));
    JScrollPane tableScroll = new JScrollPane(activityTable);
    tableScroll.setBorder(new LineBorder(BORDER_DARK, 1));
    tableScroll.getViewport().setBackground(BG_INPUT);

    chatTabs.addTab("Chat chung", groupScroll);
    chatTabs.addTab("Chat riêng", privScroll);
    chatTabs.addTab("Nhật ký", tableScroll);
    JTable transferTable = new JTable(transferModel);
    transferTable.setRowHeight(26);
    transferTable.setBackground(BG_INPUT);
    transferTable.setForeground(TEXT_MAIN);
    chatTabs.addTab("Tệp nhận", new JScrollPane(transferTable));
    transferTable.setToolTipText("Nhấp đúp để lưu lại tệp nhận");
    transferTable.addMouseListener(
        new MouseAdapter() {
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && transferTable.getSelectedRow() >= 0)
              saveReceived(transferTable.getSelectedRow());
          }
        });
    chatBox.add(chatTabs, BorderLayout.CENTER);

    // Khu vực điều khiển Chat + Emoji + Gửi File
    JPanel chatControl = new JPanel(new BorderLayout(0, 4));
    chatControl.setBackground(BG_PANEL);

    JPanel statusRow = new JPanel(new BorderLayout());
    statusRow.setBackground(BG_PANEL);
    lblTyping.setFont(FONT_PLAIN_11);
    lblTyping.setForeground(SUCCESS);
    progressBar.setStringPainted(true);
    progressBar.setVisible(false);
    progressBar.setBackground(BG_INPUT);
    progressBar.setForeground(PRIMARY);
    progressBar.setPreferredSize(new Dimension(130, 15));
    statusRow.add(lblTyping, BorderLayout.CENTER);
    statusRow.add(progressBar, BorderLayout.EAST);
    chatControl.add(statusRow, BorderLayout.NORTH);

    // Hàng Nhập Liệu: [ Gửi File ] [ Ô Text ] [ Gửi ]
    JPanel inputRow = new JPanel(new BorderLayout(5, 0));
    inputRow.setBackground(BG_PANEL);

    fileButton.setPreferredSize(new Dimension(78, 34));
    fileButton.setBackground(BG_BTN);
    fileButton.setForeground(new Color(165, 180, 252));
    fileButton.setBorder(new LineBorder(BORDER_DARK, 1, true));
    fileButton.addActionListener(e -> chooseAndSendFile());
    inputRow.add(fileButton, BorderLayout.WEST);

    messageField.setBackground(BG_INPUT);
    messageField.setForeground(TEXT_MAIN);
    messageField.setCaretColor(TEXT_MAIN);
    messageField.setFont(FONT_PLAIN_13);
    messageField.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1, true), new EmptyBorder(6, 8, 6, 8)));
    inputRow.add(messageField, BorderLayout.CENTER);

    sendButton.setPreferredSize(new Dimension(58, 34));
    inputRow.add(sendButton, BorderLayout.EAST);
    chatControl.add(inputRow, BorderLayout.CENTER);

    // Hàng biểu tượng cảm xúc nhanh
    JPanel emojiRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
    emojiRow.setBackground(BG_PANEL);
    String[] emojis = {"(y)", ":D", "<3", ":)", "OK", "^^", "Hay quá!", "Đã rõ!"};
    for (String emo : emojis) {
      JButton emoBtn = createDarkSmallBtn(emo);
      emoBtn.addActionListener(
          e ->
              messageField.setText(
                  messageField.getText() + (messageField.getText().isEmpty() ? "" : " ") + emo));
      emojiRow.add(emoBtn);
    }
    chatControl.add(emojiRow, BorderLayout.SOUTH);

    chatBox.add(chatControl, BorderLayout.SOUTH);
    leftColumn.add(chatBox, BorderLayout.CENTER);

    messageField.addActionListener(e -> sendMessage());
    sendButton.addActionListener(e -> sendMessage());

    messageField.addKeyListener(
        new KeyAdapter() {
          private Timer timer = createTypingTimer();

          @Override
          public void keyTyped(KeyEvent e) {
            client.sendTypingStatus(true);
            timer.restart();
          }
        });

    // ==========================================
    // 2. CỘT PHẢI: WHITEBOARD PRO (2 DÒNG TOOLBAR DARK SLATE)
    // ==========================================
    JPanel rightColumn = new JPanel(new BorderLayout(0, 0));
    rightColumn.setBackground(BG_PANEL);
    rightColumn.setBorder(new LineBorder(BORDER_DARK, 1, true));

    whiteboardPanel = new WhiteboardPanel(client, isHost);
    whiteboardPanel.setStatusCallback(this);

    // Khung Toolbar 2 tầng nền tối đồng bộ
    JPanel topToolbarContainer = new JPanel(new GridLayout(2, 1, 0, 2));
    topToolbarContainer.setBackground(BG_PANEL);
    topToolbarContainer.setBorder(new EmptyBorder(4, 6, 4, 6));

    // TẦNG 1: Các công cụ vẽ hình học & Độ dày nét
    JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    row1.setBackground(BG_PANEL);

    JButton moveBtn = createToolBtn("Di Chuyển");
    moveBtn.setBackground(BG_BTN_ACTIVE);
    moveBtn.setForeground(Color.WHITE);
    moveBtn.setFont(FONT_BOLD_11);
    moveBtn.addActionListener(e -> whiteboardPanel.setCurrentTool(DrawShape.TYPE_SELECT));

    JButton penBtn = createToolBtn("Bút Vẽ");
    JButton lineBtn = createToolBtn("Đoạn Thẳng");
    JButton arrowBtn = createToolBtn("Mũi Tên");
    JButton rectBtn = createToolBtn("Hình Hộp");
    JButton ovalBtn = createToolBtn("Hình Tròn");
    JButton textBtn = createToolBtn("Chèn Chữ");
    JButton eraserBtn = createToolBtn("Tẩy Xóa");

    penBtn.addActionListener(e -> whiteboardPanel.setCurrentTool(DrawShape.TYPE_PEN));
    lineBtn.addActionListener(e -> whiteboardPanel.setCurrentTool(DrawShape.TYPE_LINE));
    arrowBtn.addActionListener(e -> whiteboardPanel.setCurrentTool(DrawShape.TYPE_ARROW));
    rectBtn.addActionListener(e -> whiteboardPanel.setCurrentTool(DrawShape.TYPE_RECT));
    ovalBtn.addActionListener(e -> whiteboardPanel.setCurrentTool(DrawShape.TYPE_OVAL));
    textBtn.addActionListener(e -> whiteboardPanel.setCurrentTool(DrawShape.TYPE_TEXT));
    eraserBtn.addActionListener(e -> whiteboardPanel.setCurrentTool(DrawShape.TYPE_ERASER));

    JCheckBox chkFill = new JCheckBox("Tô Đặc");
    chkFill.setFont(FONT_BOLD_11);
    chkFill.setBackground(BG_PANEL);
    chkFill.setForeground(TEXT_MAIN);
    chkFill.setFocusPainted(false);
    chkFill.addActionListener(e -> whiteboardPanel.setFilled(chkFill.isSelected()));

    JLabel lblStroke = new JLabel("  Cỡ nét:");
    lblStroke.setFont(FONT_BOLD_11);
    lblStroke.setForeground(TEXT_MAIN);

    JSlider strokeSlider = new JSlider(1, 18, 3);
    strokeSlider.setPreferredSize(new Dimension(90, 22));
    strokeSlider.setBackground(BG_PANEL);
    strokeSlider.addChangeListener(e -> whiteboardPanel.setCurrentStroke(strokeSlider.getValue()));

    row1.add(moveBtn);
    row1.add(penBtn);
    row1.add(lineBtn);
    row1.add(arrowBtn);
    row1.add(rectBtn);
    row1.add(ovalBtn);
    row1.add(textBtn);
    row1.add(eraserBtn);
    row1.add(chkFill);
    row1.add(lblStroke);
    row1.add(strokeSlider);

    // TẦNG 2: Màu sắc, Chèn ảnh, Xóa hình chọn, Undo/Redo, Lưới, Xuất PNG, Khóa bảng
    JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    row2.setBackground(BG_PANEL);

    colorPreviewBox.setPreferredSize(new Dimension(22, 22));
    colorPreviewBox.setBackground(whiteboardPanel.getCurrentColor());
    colorPreviewBox.setBorder(new LineBorder(Color.WHITE, 1));

    JButton colorBtn = createToolBtn("Đổi Màu");
    JButton importImgBtn = createToolBtn("Chèn Ảnh");
    JButton delSelectedBtn = createToolBtn("Xóa Hình Chọn");
    JButton undoBtn = createToolBtn("Hoàn Tác (Undo)");
    JButton redoBtn = createToolBtn("Làm Lại (Redo)");
    JButton gridBtn = createToolBtn("Bật/Tắt Lưới");
    JButton clearBtn = createToolBtn("Xóa Bảng");
    JButton saveBtn = createToolBtn("Lưu PNG");

    colorBtn.addActionListener(e -> pickColor());
    importImgBtn.addActionListener(e -> importImageToWhiteboard());
    delSelectedBtn.addActionListener(e -> whiteboardPanel.deleteSelectedShape());
    undoBtn.addActionListener(e -> whiteboardPanel.undo());
    redoBtn.addActionListener(e -> whiteboardPanel.redo());
    gridBtn.addActionListener(e -> whiteboardPanel.toggleGrid());
    clearBtn.addActionListener(e -> confirmClear());
    saveBtn.addActionListener(e -> saveWhiteboard());

    row2.add(colorPreviewBox);
    row2.add(colorBtn);
    row2.add(importImgBtn);
    row2.add(delSelectedBtn);
    row2.add(undoBtn);
    row2.add(redoBtn);
    row2.add(gridBtn);
    JButton refreshBoard = createToolBtn("Đồng bộ lại");
    refreshBoard.addActionListener(e -> whiteboardPanel.requestSync());
    row2.add(refreshBoard);
    row2.add(clearBtn);
    row2.add(saveBtn);
    if (isHost) {
      JButton saveSession = createToolBtn("Lưu phiên");
      JButton loadSession = createToolBtn("Mở phiên");
      saveSession.addActionListener(
          e ->
              client.sendPacket(new Packet(Packet.Command.SAVE_BOARD, client.getUsername(), null)));
      loadSession.addActionListener(
          e -> {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Thay bảng hiện tại bằng phiên đã lưu trên Host?",
                    "Mở phiên",
                    JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION)
              client.sendPacket(new Packet(Packet.Command.LOAD_BOARD, client.getUsername(), null));
          });
      row2.add(saveSession);
      row2.add(loadSession);
    }
    clearBtn.setEnabled(isHost);
    bind("control Z", "undo", () -> whiteboardPanel.undo());
    bind("control Y", "redo", () -> whiteboardPanel.redo());
    bind("control S", "save", () -> saveWhiteboard());
    whiteboardPanel.setFocusable(true);
    whiteboardPanel
        .getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke("DELETE"), "delete");
    whiteboardPanel
        .getActionMap()
        .put(
            "delete",
            new AbstractAction() {
              public void actionPerformed(ActionEvent e) {
                whiteboardPanel.deleteSelectedShape();
              }
            });
    whiteboardPanel.addMouseListener(
        new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            whiteboardPanel.requestFocusInWindow();
          }
        });
    JButton[] toolButtons = {
      moveBtn, penBtn, lineBtn, arrowBtn, rectBtn, ovalBtn, textBtn, eraserBtn
    };
    moveBtn.setBackground(BG_BTN);
    penBtn.setBackground(BG_BTN_ACTIVE);
    for (JButton tool : toolButtons)
      tool.addActionListener(
          e -> {
            for (JButton b : toolButtons) b.setBackground(b == tool ? BG_BTN_ACTIVE : BG_BTN);
          });

    if (isHost) {
      btnLockBoard = createToolBtn("Khóa Bảng");
      btnLockBoard.setBackground(new Color(127, 29, 29));
      btnLockBoard.setForeground(new Color(254, 202, 202));
      btnLockBoard.setFont(FONT_BOLD_11);
      btnLockBoard.addActionListener(e -> toggleLock());
      userActions.add(btnLockBoard);
    }

    topToolbarContainer.add(row1);
    topToolbarContainer.add(row2);

    JScrollPane toolScroll =
        new JScrollPane(
            topToolbarContainer,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    toolScroll.setBorder(null);
    toolScroll.setPreferredSize(new Dimension(600, 100));
    rightColumn.add(toolScroll, BorderLayout.NORTH);
    rightColumn.add(whiteboardPanel, BorderLayout.CENTER);

    // ==========================================
    // 3. THANH TRẠNG THÁI (STATUS BAR) DƯỚI ĐÁY
    // ==========================================
    JPanel statusBar = new JPanel(new BorderLayout(10, 0));
    statusBar.setBackground(BG_PANEL);
    statusBar.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1, true), new EmptyBorder(4, 10, 4, 10)));

    lblStatusLeft.setFont(FONT_PLAIN_11);
    lblStatusLeft.setForeground(TEXT_MAIN);
    lblStatusLeft.setText(
        "Phòng: "
            + roomName
            + " | Máy chủ: "
            + serverAddress
            + (isHost ? " (Chủ phòng)" : " (Thành viên)"));

    lblStatusCenter.setFont(FONT_BOLD_11);
    lblStatusCenter.setForeground(new Color(129, 140, 248));
    lblStatusCenter.setHorizontalAlignment(SwingConstants.CENTER);

    boolean dbActive = true;
    lblStatusRight.setFont(FONT_BOLD_11);
    lblStatusRight.setForeground(dbActive ? SUCCESS : TEXT_MUTED);
    lblStatusRight.setText(dbActive ? "● Đã kết nối Host" : "● Chế độ: Bộ nhớ LAN Độc Lập");

    statusBar.add(lblStatusLeft, BorderLayout.WEST);
    statusBar.add(lblStatusCenter, BorderLayout.CENTER);
    statusBar.add(lblStatusRight, BorderLayout.EAST);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftColumn, rightColumn);
    split.setDividerLocation(390);
    split.setResizeWeight(0.3);
    leftColumn.setMinimumSize(new Dimension(320, 0));
    rightColumn.setMinimumSize(new Dimension(450, 0));
    add(split, BorderLayout.CENTER);
    add(statusBar, BorderLayout.SOUTH);
  }

  private void setupTextArea(JTextArea ta) {
    ta.setEditable(false);
    ta.setBackground(BG_INPUT);
    ta.setForeground(TEXT_MAIN);
    ta.setFont(FONT_PLAIN_13);
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setBorder(new EmptyBorder(8, 8, 8, 8));
  }

  private JButton createPrimaryBtn(String text) {
    JButton btn = new JButton(text);
    btn.setOpaque(true);
    btn.setContentAreaFilled(true);
    btn.setBorderPainted(false);
    btn.setFocusPainted(false);
    btn.setFont(FONT_BOLD_11);
    btn.setToolTipText(text);
    btn.setBackground(PRIMARY);
    btn.setForeground(Color.WHITE);
    btn.setBorder(new EmptyBorder(5, 10, 5, 10));
    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    return btn;
  }

  private JButton createToolBtn(String text) {
    JButton btn = new JButton(text);
    btn.setOpaque(true);
    btn.setContentAreaFilled(true);
    btn.setFocusPainted(false);
    btn.setFont(FONT_PLAIN_11);
    btn.setToolTipText(text);
    btn.setBackground(BG_BTN);
    btn.setForeground(TEXT_MAIN);
    btn.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1, true), new EmptyBorder(4, 8, 4, 8)));
    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    return btn;
  }

  private JButton createDarkSmallBtn(String text) {
    JButton btn = new JButton(text);
    btn.setOpaque(true);
    btn.setContentAreaFilled(true);
    btn.setFocusPainted(false);
    btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
    btn.setBackground(BG_BTN);
    btn.setForeground(TEXT_MAIN);
    btn.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_DARK, 1, true), new EmptyBorder(2, 5, 2, 5)));
    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    return btn;
  }

  private String nowTime() {
    return new SimpleDateFormat("HH:mm:ss").format(new Date());
  }

  private void logActivity(String type, String detail) {
    fileTableModel.insertRow(0, new Object[] {nowTime(), type, detail});
  }

  private void pickColor() {
    Color c = JColorChooser.showDialog(this, "Chọn màu nét vẽ", whiteboardPanel.getCurrentColor());
    if (c != null) {
      whiteboardPanel.setCurrentColor(c);
      colorPreviewBox.setBackground(c);
    }
  }

  private void importImageToWhiteboard() {
    JFileChooser chooser = new JFileChooser();
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      whiteboardPanel.importImageToBoard(chooser.getSelectedFile());
    }
  }

  private void confirmClear() {
    int ret =
        JOptionPane.showConfirmDialog(
            this,
            "Bạn có chắc muốn xóa sạch bảng vẽ của toàn phòng?",
            "Xác nhận",
            JOptionPane.YES_NO_OPTION);
    if (ret == JOptionPane.YES_OPTION) {
      whiteboardPanel.clearBoard(true);
      logActivity("Bảng vẽ", "Xóa trắng toàn bộ bảng vẽ");
    }
  }

  private void saveWhiteboard() {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File("Whiteboard_" + System.currentTimeMillis() + ".png"));
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      File target = chooser.getSelectedFile();
      if (!target.getName().toLowerCase().endsWith(".png"))
        target = new File(target.getAbsolutePath() + ".png");
      if (target.exists()
          && JOptionPane.showConfirmDialog(
                  this, "Ảnh đã tồn tại. Ghi đè?", "Lưu PNG", JOptionPane.YES_NO_OPTION)
              != JOptionPane.YES_OPTION) return;
      whiteboardPanel.saveToPNG(target);
    }
  }

  private void exportChatHistory() {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File("BienBanHop_LAN_" + System.currentTimeMillis() + ".txt"));
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      try (java.io.Writer fw =
          Files.newBufferedWriter(
              chooser.getSelectedFile().toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
        fw.write("=== BIÊN BẢN THẢO LUẬN PHÒNG: " + roomName + " ===\n");
        fw.write(groupChatArea.getText());
        JOptionPane.showMessageDialog(this, "Đã xuất lịch sử cuộc họp thành công!");
        logActivity("Xuất file", chooser.getSelectedFile().getName());
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Lỗi xuất biên bản: " + ex.getMessage());
      }
    }
  }

  private void toggleLock() {
    boolean newLock = !whiteboardPanel.isBoardLocked();

    btnLockBoard.setText(newLock ? "Mở Khóa" : "Khóa Bảng");
    btnLockBoard.setBackground(newLock ? new Color(6, 95, 70) : new Color(127, 29, 29));
    client.sendLockBoard(newLock);
    logActivity("Bảo mật", (newLock ? "Khóa" : "Mở khóa") + " quyền vẽ bảng");
  }

  private void openPrivateChat(String targetUser) {
    this.currentPrivateUser = targetUser;
    chatTabs.setTitleAt(1, "Chat: " + targetUser);
    chatTabs.setSelectedIndex(1);
    privateChatArea.setText(
        privateHistory.computeIfAbsent(targetUser, k -> new StringBuilder()).toString());
  }

  private void sendMessage() {
    String text = messageField.getText().trim();
    if (text.isEmpty() || !client.isConnected()) return;
    if (text.length() > 5000) {
      onError("Tin nhắn tối đa 5000 ký tự.");
      return;
    }

    int selectedTab = chatTabs.getSelectedIndex();
    if (selectedTab == 1) {
      if (currentPrivateUser == null) {
        JOptionPane.showMessageDialog(
            this, "Hãy nhấp đúp vào một người trong danh sách để chọn đối tượng chat riêng!");
        return;
      }
      client.sendPrivateChat(currentPrivateUser, text);
    } else {
      client.sendChat(text);
      chatTabs.setSelectedIndex(0);
    }
    messageField.setText("");
  }

  private void chooseAndSendFile() {
    JFileChooser chooser = new JFileChooser();
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      progressBar.setVisible(true);
      progressBar.setIndeterminate(false);
      progressBar.setValue(0);
      progressBar.setString("Đang gửi: " + file.getName());
      client.sendFile(file);
      logActivity("Gửi File", file.getName() + " (" + (file.length() / 1024) + " KB)");
    }
  }

  @Override
  public void onCursorMoved(int x, int y, int shapeCount, String activeToolName) {
    lblStatusCenter.setText(
        "Công cụ: "
            + activeToolName
            + " | Tọa độ: ("
            + x
            + ", "
            + y
            + ") | Số hình vẽ: "
            + shapeCount);
  }

  @Override
  public void onBoardActionLogged(String actionName, String detail) {
    logActivity(actionName, detail);
  }

  @Override
  public void onMessageReceived(String sender, String message) {
    groupChatArea.append("[" + nowTime() + "] " + sender + ": " + message + "\n");
    groupChatArea.setCaretPosition(groupChatArea.getDocument().getLength());
  }

  @Override
  public void onPrivateMessageReceived(String sender, String message) {
    onPrivateMessage(sender, client.getUsername(), message);
  }

  @Override
  public void onPrivateMessage(String sender, String target, String message) {
    String peer = sender.equals(client.getUsername()) ? target : sender;
    StringBuilder history = privateHistory.computeIfAbsent(peer, k -> new StringBuilder());
    history
        .append("[")
        .append(nowTime())
        .append("] ")
        .append(sender)
        .append(": ")
        .append(message)
        .append("\n");
    if (peer.equals(currentPrivateUser)) {
      privateChatArea.setText(history.toString());
      privateChatArea.setCaretPosition(privateChatArea.getDocument().getLength());
    } else {
      logActivity("Tin riêng", "Tin mới từ " + peer);
      chatTabs.setTitleAt(1, "● Tin riêng: " + peer);
    }
  }

  @Override
  public void onTypingIndicatorReceived(String sender, boolean isTyping) {
    lblTyping.setText(isTyping ? "• " + sender + " đang soạn tin..." : " ");
  }

  @Override
  public void onUserListUpdated(List<String> users) {
    userListModel.clear();
    for (String user : users) userListModel.addElement(user);
    usersHeader.setText("ONLINE (" + users.size() + ") — Nhấp đúp chat riêng");
  }

  @Override
  public void onDrawActionReceived(DrawAction action) {}

  @Override
  public void onBoardState(BoardSnapshot state) {
    boolean changed = whiteboardPanel.isBoardLocked() != state.locked;
    whiteboardPanel.applyBoardState(state);
    if (btnLockBoard != null) btnLockBoard.setText(state.locked ? "Mở khóa" : "Khóa bảng");
    if (changed)
      logActivity("Bảng vẽ", state.locked ? "Host đã khóa bảng" : "Host đã mở khóa bảng");
  }

  @Override
  public void onBoardStatusChanged(String text) {
    lblStatusRight.setText("● " + text);
  }

  @Override
  public void onNotice(String message) {
    logActivity("Thông báo", message);
    lblTyping.setText(message);
  }

  @Override
  public void onShapeSyncReceived(List<DrawShape> newShapes) {
    whiteboardPanel.applyShapeSync(newShapes);
  }

  @Override
  public void onClearWhiteboardReceived() {
    whiteboardPanel.clearBoard(false);
    logActivity("Bảng vẽ", "Bảng vẽ đã được làm mới");
  }

  @Override
  public void onLockWhiteboardReceived(boolean isLocked) {
    whiteboardPanel.setBoardLocked(isLocked);
    logActivity("Bảo mật", isLocked ? "Chủ phòng đã khóa bảng" : "Chủ phòng đã mở khóa bảng");
    lblStatusRight.setText(isLocked ? "● Bảng đang khóa" : "● Bảng đang mở");
    if (btnLockBoard != null) btnLockBoard.setText(isLocked ? "Mở khóa" : "Khóa bảng");
  }

  @Override
  public void onFileReceived(FileTransferInfo info) {}

  @Override
  public void onHistory(String history) {
    groupChatArea.setText(
        history.isEmpty()
            ? ""
            : "--- Lịch sử gần nhất ---\n" + history + "--- Phiên hiện tại ---\n");
  }

  @Override
  public void onError(String message) {
    progressBar.setVisible(false);
    logActivity("Lỗi", message);
    JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.WARNING_MESSAGE);
  }

  @Override
  public void onDisconnected() {
    whiteboardPanel.connectionLost();
    messageField.setEnabled(false);
    sendButton.setEnabled(false);
    fileButton.setEnabled(false);
    if (typingTimer != null) typingTimer.stop();
    lblStatusRight.setText("● Mất kết nối");
    JOptionPane.showMessageDialog(this, "Đã mất kết nối Host. Bấm Rời phòng rồi đăng nhập lại.");
  }

  @Override
  public void onProgress(int percent, String message) {
    progressBar.setVisible(percent < 100);
    progressBar.setIndeterminate(false);
    progressBar.setValue(percent);
    progressBar.setString(percent + "%");
    lblTyping.setText(message);
    if (percent == 100) logActivity("Truyền file", message);
  }

  @Override
  public void onFileSaved(String sender, String name, Path temp, long size) {
    receivedTemps.add(temp);
    int row = transferModel.getRowCount();
    receivedRows.put(row, temp);
    transferModel.addRow(
        new Object[] {sender, name, (size / 1024) + " KB", "Đã nhận — nhấp đúp để lưu"});
    if (JOptionPane.showConfirmDialog(
            this,
            "Đã nhận " + name + " từ " + sender + ". Lưu về máy?",
            "Nhận file",
            JOptionPane.YES_NO_OPTION)
        == JOptionPane.YES_OPTION) saveReceived(row);
  }

  private void saveReceived(int row) {
    Path temp = receivedRows.get(row);
    if (temp == null || !Files.exists(temp)) {
      onError("Tệp tạm không còn. Hãy nhờ gửi lại.");
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(
        new File(new File(transferModel.getValueAt(row, 1).toString()).getName()));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    Path target = chooser.getSelectedFile().toPath();
    if (Files.exists(target)
        && JOptionPane.showConfirmDialog(
                this, "File đã tồn tại. Ghi đè?", "Lưu file", JOptionPane.YES_NO_OPTION)
            != JOptionPane.YES_OPTION) return;
    new SwingWorker<Void, Void>() {
      protected Void doInBackground() throws Exception {
        Files.copy(temp, target, StandardCopyOption.REPLACE_EXISTING);
        return null;
      }

      protected void done() {
        try {
          get();
          transferModel.setValueAt(target.toAbsolutePath().toString(), row, 3);
        } catch (Exception e) {
          onError("Lưu file thất bại: " + e.getMessage());
        }
      }
    }.execute();
  }

  private Timer createTypingTimer() {
    typingTimer = new Timer(1500, e -> client.sendTypingStatus(false));
    typingTimer.setRepeats(false);
    return typingTimer;
  }

  private void bind(String key, String id, Runnable action) {
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(key), id);
    getRootPane()
        .getActionMap()
        .put(
            id,
            new AbstractAction() {
              public void actionPerformed(ActionEvent e) {
                if (!id.equals("save")
                    && java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .getFocusOwner()
                        instanceof javax.swing.text.JTextComponent) return;
                action.run();
              }
            });
  }

  private void leaveRoom() {
    if (leaving) return;
    if (JOptionPane.showConfirmDialog(
            this,
            isHost
                ? "Rời phòng sẽ đóng Host và lưu phiên bảng vẽ. Tiếp tục?"
                : "Rời phòng và về màn hình đăng nhập?",
            "Rời phòng",
            JOptionPane.YES_NO_OPTION)
        != JOptionPane.YES_OPTION) return;
    leaving = true;
    whiteboardPanel.disposePanel();
    if (typingTimer != null) typingTimer.stop();
    setEnabled(false);
    new SwingWorker<Void, Void>() {
      protected Void doInBackground() {
        client.disconnect();
        if (localServer != null) localServer.stopServer();
        for (Path p : receivedTemps)
          try {
            Files.deleteIfExists(p);
          } catch (Exception e) {
          }
        return null;
      }

      protected void done() {
        dispose();
        new LoginFrame().setVisible(true);
      }
    }.execute();
  }

  private static class OnlineDotIcon implements Icon {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(new Color(52, 211, 153));
      g2.fillOval(x, y + 2, 7, 7);
      g2.dispose();
    }

    @Override
    public int getIconWidth() {
      return 10;
    }

    @Override
    public int getIconHeight() {
      return 10;
    }
  }
}
