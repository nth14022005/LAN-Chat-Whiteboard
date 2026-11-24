package com.lanchat.database;

import com.lanchat.model.DrawShape;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WhiteboardDAO {
  // Lưu vào bảng PhienBangVe (Bảng số 8 trong SSMS)
  public static void saveWhiteboardExport(
      int roomId, int userId, String boardName, String imagePath, String resolution) {

    String sql =
        "INSERT INTO PhienBangVe (ma_phong, nguoi_tao_id, ten_ban_ve, duong_dan_anh, do_phan_giai)"
            + " VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, roomId);
      ps.setInt(2, userId);
      ps.setString(3, boardName);
      ps.setString(4, imagePath);
      ps.setString(5, resolution);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
  }

  // Lưu vào bảng ChiTietNetVe (Bảng số 9 trong SSMS)
  public static void saveStrokeDetail(int roomId, int userId, DrawShape shape) {

    if (shape == null) return;
    String sql =
        "INSERT INTO ChiTietNetVe (ma_phong, nguoi_ve_id, loai_hinh, toa_do_x1, toa_do_y1,"
            + " toa_do_x2, toa_do_y2, ma_mau_rgb, do_day_net, to_mau) VALUES (?, ?, ?, ?, ?, ?, ?,"
            + " ?, ?, ?)";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, roomId);
      ps.setInt(2, userId);
      ps.setString(3, shape.getTypeName());
      ps.setInt(4, shape.getX1());
      ps.setInt(5, shape.getY1());
      ps.setInt(6, shape.getX2());
      ps.setInt(7, shape.getY2());
      ps.setInt(8, shape.getColorRGB());
      ps.setFloat(9, shape.getStrokeWidth());
      ps.setInt(10, shape.isFilled() ? 1 : 0);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
  }
}
