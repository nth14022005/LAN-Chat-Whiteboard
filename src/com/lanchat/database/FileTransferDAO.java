package com.lanchat.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FileTransferDAO {
  // Lưu vào bảng TepTruyen (Bảng số 7 trong SSMS)
  public static void saveFileRecord(
      int roomId, int senderId, String fileName, long fileSize, String savedPath) {

    String ext = "";
    int dot = fileName.lastIndexOf('.');
    if (dot >= 0) ext = fileName.substring(dot);

    String sql =
        "INSERT INTO TepTruyen (ma_phong, nguoi_gui_id, ten_file, duoi_file, kich_thuoc_byte,"
            + " duong_dan_luu, so_lan_tai) VALUES (?, ?, ?, ?, ?, ?, 1)";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, roomId);
      ps.setInt(2, senderId);
      ps.setString(3, fileName);
      ps.setString(4, ext);
      ps.setLong(5, fileSize);
      ps.setString(6, savedPath);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
  }
}
