package com.lanchat.database;

import java.sql.*;

public class RoomDAO {
  public static int createOrGetRoom(String roomName, int hostId, String password) {

    String checkSql = "SELECT ma_phong FROM PhongChat WHERE ten_phong = ? AND chu_phong_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(checkSql)) {
      ps.setString(1, roomName);
      ps.setInt(2, hostId);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return rs.getInt(1);
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }

    String sql =
        "INSERT INTO PhongChat (ten_phong, chu_de, chu_phong_id, mat_khau_phong, khoa_bang_ve)"
            + " VALUES (?, N'Phòng họp và bảng vẽ trực tuyến LAN', ?, ?, 0)";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, roomName);
      ps.setInt(2, hostId);
      ps.setString(3, password);
      ps.executeUpdate();
      ResultSet rs = ps.getGeneratedKeys();
      if (rs.next()) return rs.getInt(1);
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
    throw new IllegalStateException("Không tạo được phòng.");
  }

  public static void addRoomMember(int roomId, int userId, String role) {

    String checkSql =
        "SELECT COUNT(*) FROM ThanhVienPhong WHERE ma_phong = ? AND ma_nguoi_dung = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(checkSql)) {
      ps.setInt(1, roomId);
      ps.setInt(2, userId);
      ResultSet rs = ps.executeQuery();
      if (rs.next() && rs.getInt(1) > 0) return;
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }

    String sql =
        "INSERT INTO ThanhVienPhong (ma_phong, ma_nguoi_dung, vai_tro_phong, duoc_quyen_ve) VALUES"
            + " (?, ?, ?, 1)";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, roomId);
      ps.setInt(2, userId);
      ps.setString(3, role);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
  }

  public static void updateBoardLock(int roomId, boolean locked) {

    String sql = "UPDATE PhongChat SET khoa_bang_ve = ? WHERE ma_phong = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, locked ? 1 : 0);
      ps.setInt(2, roomId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không lưu được dữ liệu: " + e.getMessage(), e);
    }
  }
}
