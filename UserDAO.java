package com.lanchat.database;

import com.lanchat.model.User;
import java.sql.*;

public class UserDAO {
  public static User authenticate(String login, String password, String ip) throws SQLException {
    try (Connection c = DBConnection.getConnection();
        PreparedStatement q =
            c.prepareStatement(
                "SELECT ma_nguoi_dung,ten_dang_nhap,ho_ten,mat_khau FROM NguoiDung WHERE"
                    + " ten_dang_nhap=? OR ho_ten=?")) {
      q.setString(1, login);
      q.setString(2, login);
      try (ResultSet r = q.executeQuery()) {
        User u = null;
        String old = null;
        while (r.next()) {
          if (PasswordUtil.verify(password, r.getString(4))) {
            if (u != null) throw new SQLException("Họ tên bị trùng. Hãy dùng tên đăng nhập.");
            u = new User(r.getInt(1), r.getString(2), r.getString(3), true);
            old = r.getString(4);
          }
        }
        if (u == null) throw new SQLException("Sai tài khoản hoặc mật khẩu.");
        if (!old.startsWith("p2$")) {
          try (PreparedStatement up =
              c.prepareStatement("UPDATE NguoiDung SET mat_khau=? WHERE ma_nguoi_dung=?")) {
            up.setString(1, PasswordUtil.hash(password));
            up.setInt(2, u.getId());
            up.executeUpdate();
          }
        }
        updateStatusById(u.getId(), true, ip);
        return u;
      }
    }
  }

  public static void register(String username, String password, String name, String email)
      throws SQLException {
    if (!username.matches("[A-Za-z0-9_.-]{3,50}")
        || password.length() < 6
        || password.length() > 128
        || name.trim().isEmpty()
        || name.length() > 100
        || email.length() > 100
        || (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")))
      throw new SQLException(
          "Tài khoản: 3–50 ký tự chữ/số/._-; mật khẩu: 6–128 ký tự; kiểm tra họ tên và email.");
    try (Connection c = DBConnection.getConnection();
        PreparedStatement q =
            c.prepareStatement(
                "INSERT INTO NguoiDung(ten_dang_nhap,mat_khau,ho_ten,email,ma_vai_tro,trang_thai)"
                    + " VALUES(?,?,?,?,2,N'OFFLINE')")) {
      q.setString(1, username);
      q.setString(2, PasswordUtil.hash(password));
      q.setString(3, name);
      q.setString(4, email);
      q.executeUpdate();
    } catch (SQLException e) {
      if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627)
        throw new SQLException("Tên đăng nhập đã tồn tại.");
      throw e;
    }
  }

  public static void updateStatusById(int id, boolean online, String ip) {
    try (Connection c = DBConnection.getConnection();
        PreparedStatement q =
            c.prepareStatement(
                "UPDATE NguoiDung SET trang_thai=?,ip_hien_tai=?,lan_dang_nhap_cuoi=GETDATE() WHERE"
                    + " ma_nguoi_dung=?")) {
      q.setString(1, online ? "ONLINE" : "OFFLINE");
      q.setString(2, ip);
      q.setInt(3, id);
      q.executeUpdate();
    } catch (Exception e) {
      System.err.println("[SQL] " + e.getMessage());
    }
  }
}
