package com.lanchat.ui;

import com.lanchat.database.*;
import com.lanchat.model.User;
import com.lanchat.network.*;
import java.awt.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LoginFrame extends JFrame {
  private final JTextField account = new JTextField("hungnguyen"),
      room = new JTextField("Phòng Thảo Luận Đồ Án Java"),
      ip = new JTextField("127.0.0.1"),
      port = new JTextField("9999");
  private final JPasswordField password = new JPasswordField();
  private final JRadioButton host = new JRadioButton("Tạo phòng (Host)", true),
      join = new JRadioButton("Tham gia phòng");
  private final JButton enter = new JButton("Vào phòng"),
      register = new JButton("Đăng ký"),
      discover = new JButton("Dò LAN");
  private final JLabel status =
      new JLabel("Host dùng CSDL SQL Server hiện có. Máy khách chỉ cần IP Host.");
  private boolean busy;

  public LoginFrame() {
    super("LAN Chat & Whiteboard — Kết nối");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(680, 570);
    setMinimumSize(new Dimension(650, 540));
    setLocationRelativeTo(null);
    JPanel p = new JPanel(new GridBagLayout());
    p.setBorder(new EmptyBorder(24, 28, 24, 28));
    p.setBackground(MainFrame.BG_PANEL);
    setContentPane(p);
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(7, 6, 7, 6);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1;
    JLabel title = new JLabel("LAN CHAT & WHITEBOARD", SwingConstants.CENTER);
    title.setFont(new Font("Segoe UI", Font.BOLD, 23));
    title.setForeground(new Color(129, 140, 248));
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    p.add(title, c);
    JPanel modes = new JPanel(new FlowLayout(FlowLayout.CENTER));
    ButtonGroup g = new ButtonGroup();
    g.add(host);
    g.add(join);
    modes.add(host);
    modes.add(join);
    c.gridy++;
    p.add(modes, c);
    c.gridwidth = 1;
    addRow(p, c, "Tên đăng nhập / Họ tên", account);
    addRow(p, c, "Mật khẩu", password);
    JCheckBox show = new JCheckBox("Hiện mật khẩu");
    char echo = password.getEchoChar();
    show.addActionListener(e -> password.setEchoChar(show.isSelected() ? (char) 0 : echo));
    c.gridy++;
    c.gridx = 1;
    p.add(show, c);
    addRow(p, c, "Tên phòng (Host)", room);
    JPanel address = new JPanel(new BorderLayout(6, 0));
    address.add(ip);
    address.add(discover, BorderLayout.EAST);
    addRow(p, c, "IP máy Host", address);
    addRow(p, c, "Cổng TCP", port);
    JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
    buttons.add(register);
    buttons.add(enter);
    enter.setBackground(MainFrame.PRIMARY);
    enter.setForeground(Color.WHITE);
    c.gridx = 0;
    c.gridy++;
    c.gridwidth = 2;
    p.add(buttons, c);
    status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    status.setForeground(MainFrame.TEXT_MUTED);
    c.gridy++;
    p.add(status, c);
    host.addActionListener(e -> mode());
    join.addActionListener(e -> mode());
    mode();
    enter.addActionListener(e -> connect());
    register.addActionListener(e -> register());
    discover.addActionListener(e -> discover());
    getRootPane().setDefaultButton(enter);
  }

  private void addRow(JPanel p, GridBagConstraints c, String label, JComponent field) {
    c.gridy++;
    c.gridx = 0;
    c.weightx = 0;
    JLabel l = new JLabel(label);
    l.setForeground(MainFrame.TEXT_MAIN);
    p.add(l, c);
    c.gridx = 1;
    c.weightx = 1;
    p.add(field, c);
  }

  private void mode() {
    ip.setEnabled(!host.isSelected());
    room.setEnabled(host.isSelected());
    discover.setEnabled(!host.isSelected() && !busy);
  }

  private int port() {
    int n = Integer.parseInt(port.getText().trim());
    if (n < 1024 || n > 65535) throw new IllegalArgumentException("Cổng phải từ 1024 đến 65535.");
    return n;
  }

  private void busy(boolean b) {
    busy = b;
    enter.setEnabled(!b);
    register.setEnabled(!b);
    host.setEnabled(!b);
    join.setEnabled(!b);
    mode();
  }

  private void error(Throwable e) {
    Throwable t = e instanceof ExecutionException ? e.getCause() : e;
    status.setText("Không hoàn tất. Kiểm tra thông báo lỗi.");
    JOptionPane.showMessageDialog(this, t.getMessage(), "Thông báo", JOptionPane.ERROR_MESSAGE);
  }

  private void connect() {
    if (busy) return;
    final int n;
    try {
      n = port();
      if (account.getText().trim().isEmpty() || password.getPassword().length == 0)
        throw new IllegalArgumentException("Nhập tài khoản và mật khẩu.");
      if (host.isSelected() && (room.getText().trim().isEmpty() || room.getText().length() > 100))
        throw new IllegalArgumentException("Tên phòng từ 1–100 ký tự.");
    } catch (Exception e) {
      error(e);
      return;
    }
    final boolean hosting = host.isSelected();
    final String user = account.getText().trim(),
        pass = new String(password.getPassword()),
        name = room.getText().trim(),
        address = hosting ? "127.0.0.1" : ip.getText().trim();
    busy(true);
    status.setText("Đang xác thực và kết nối...");
    new SwingWorker<Client, Void>() {
      Server server;

      protected Client doInBackground() throws Exception {
        try {
          if (hosting) {
            User u = UserDAO.authenticate(user, pass, "127.0.0.1");
            server = new Server(n, name, u.getId());
            server.open();
          }
          Client c = new Client(address, n, user, pass, null);
          if (!c.connect()) throw new IllegalStateException(c.getError());
          return c;
        } catch (Exception e) {
          if (server != null) server.stopServer();
          throw e;
        }
      }

      protected void done() {
        busy(false);
        try {
          Client c = get();
          MainFrame f =
              new MainFrame(c, c.getSession().host, c.getSession().roomName, address + ":" + n);
          f.setLocalServer(server);
          c.setListener(f);
          f.setVisible(true);
          c.startListening();
          dispose();
        } catch (Exception e) {
          error(e);
        }
      }
    }.execute();
  }

  private void register() {
    JTextField u = new JTextField(), name = new JTextField(), mail = new JTextField();
    JPasswordField pass = new JPasswordField(), confirm = new JPasswordField();
    Object[] fields = {
      "Tên đăng nhập (3–50 ký tự)",
      u,
      "Họ tên",
      name,
      "Email (không bắt buộc)",
      mail,
      "Mật khẩu (ít nhất 6 ký tự)",
      pass,
      "Nhập lại mật khẩu",
      confirm
    };
    if (JOptionPane.showConfirmDialog(
            this, fields, "Đăng ký tài khoản", JOptionPane.OK_CANCEL_OPTION)
        != JOptionPane.OK_OPTION) return;
    if (!java.util.Arrays.equals(pass.getPassword(), confirm.getPassword())) {
      error(new Exception("Mật khẩu nhập lại không khớp."));
      return;
    }
    final String[] a = {
      u.getText().trim(),
      new String(pass.getPassword()),
      name.getText().trim(),
      mail.getText().trim()
    };
    final boolean local = host.isSelected();
    final String address = ip.getText().trim();
    final int n;
    try {
      n = port();
    } catch (Exception e) {
      error(e);
      return;
    }
    busy(true);
    status.setText("Đang đăng ký...");
    new SwingWorker<Void, Void>() {
      protected Void doInBackground() throws Exception {
        if (local) UserDAO.register(a[0], a[1], a[2], a[3]);
        else Client.register(address, n, a);
        return null;
      }

      protected void done() {
        busy(false);
        try {
          get();
          account.setText(a[0]);
          password.setText("");
          status.setText("Đăng ký thành công. Nhập mật khẩu để vào phòng.");
        } catch (Exception e) {
          error(e);
        }
      }
    }.execute();
  }

  private void discover() {
    discover.setEnabled(false);
    status.setText("Đang dò máy chủ trong 3 giây...");
    LANDiscovery.findServerAsync(
        (h, p) ->
            SwingUtilities.invokeLater(
                () -> {
                  discover.setEnabled(true);
                  if (h == null) {
                    status.setText("Không tìm thấy Host. Nhập IP hoặc bấm dò lại.");
                  } else {
                    ip.setText(h);
                    port.setText(String.valueOf(p));
                    status.setText("Đã tìm thấy Host: " + h);
                  }
                }));
  }
}
