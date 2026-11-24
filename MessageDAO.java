package com.lanchat.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MessageDAO {
  // Lưu vào bảng TinNhanPhong (Bảng số 5 trong SSMS)
  public static void saveGroupMessage(int roomId, int senderId, String content, String type) {

    String sql =
        "INSERT INTO TinNhanPhong (ma_phong, nguoi_gui_id, noi_dung, loai_tin_nhan) VALUES (?, ?,"
            + " ?, ?)";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, roomId);
      ps.setInt(2, senderId);
      ps.setString(3, content);
      ps.setString(4, type);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
  }

  // Lưu vào bảng TinNhanRieng (Bảng số 6 trong SSMS)
  public static void savePrivateMessage(int senderId, int receiverId, String content) {

    String sql =
        "INSERT INTO TinNhanRieng (nguoi_gui_id, nguoi_nhan_id, noi_dung, da_doc) VALUES (?, ?, ?,"
            + " 1)";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, senderId);
      ps.setInt(2, receiverId);
      ps.setString(3, content);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
  }

  public static String history(int roomId) {
    StringBuilder b = new StringBuilder();
    String sql =
        "SELECT * FROM (SELECT TOP 100 t.ma_tin_nhan,t.thoi_gian_gui,n.ten_dang_nhap,t.noi_dung"
            + " FROM TinNhanPhong t JOIN NguoiDung n ON n.ma_nguoi_dung=t.nguoi_gui_id WHERE"
            + " t.ma_phong=? ORDER BY t.ma_tin_nhan DESC) h ORDER BY ma_tin_nhan";
    try (Connection c = DBConnection.getConnection();
        PreparedStatement q = c.prepareStatement(sql)) {
      q.setInt(1, roomId);
      try (ResultSet r = q.executeQuery()) {
        while (r.next())
          b.append("[")
              .append(r.getTimestamp(2))
              .append("] ")
              .append(r.getString(3))
              .append(": ")
              .append(r.getString(4))
              .append("\n");
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return b.toString();
  }
}
