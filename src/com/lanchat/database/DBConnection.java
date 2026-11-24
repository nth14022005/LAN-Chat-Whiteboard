package com.lanchat.database;

import java.io.*;
import java.sql.*;
import java.util.Properties;

/** Only the host opens database connections. No schema changes are executed. */
public final class DBConnection {
  public static Connection getConnection() {
    Properties p = new Properties();
    try (InputStream in = new FileInputStream("config/database.properties")) {
      p.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Thiếu config/database.properties", e);
    }
    try {
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      DriverManager.setLoginTimeout(5);
      return DriverManager.getConnection(
          p.getProperty("url"), p.getProperty("user"), p.getProperty("password"));
    } catch (Exception e) {
      throw new IllegalStateException(
          "Không kết nối được SQL Server. Kiểm tra config/database.properties, TCP 1433 và tài"
              + " khoản SQL. "
              + e.getMessage(),
          e);
    }
  }

  public static boolean isAvailable() {
    try (Connection c = getConnection()) {
      return c.isValid(3);
    } catch (Exception e) {
      return false;
    }
  }
}
