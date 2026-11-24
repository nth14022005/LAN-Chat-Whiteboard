package com.lanchat.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditLogDAO {
  // Lưu vào bảng NhatKyHeThong (Bảng số 10 trong SSMS)
  public static void logAction(
      int userId, String actionType, String description, String ipAddress) {

    String sql =
        "INSERT INTO NhatKyHeThong (ma_nguoi_dung, loai_hanh_dong, mo_ta, dia_chi_ip) VALUES (?, ?,"
            + " ?, ?)";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ps.setString(2, actionType);
      ps.setString(3, description);
      ps.setString(4, ipAddress);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
  }
}
