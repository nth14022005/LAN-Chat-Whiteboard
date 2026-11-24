package com.lanchat.model;

import java.io.Serializable;

public class User implements Serializable {
  private static final long serialVersionUID = 1L;
  private int id;
  private String username;
  private String password;
  private String fullName;
  private String email;
  private int roleId;
  private boolean isOnline;

  public User() {}

  public User(int id, String username, String fullName, boolean isOnline) {
    this.id = id;
    this.username = username;
    this.fullName = fullName;
    this.isOnline = isOnline;
    this.roleId = 2;
  }

  public User(String username, String fullName) {
    this.username = username;
    this.fullName = fullName;
    this.isOnline = true;
    this.roleId = 2;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public int getRoleId() {
    return roleId;
  }

  public void setRoleId(int roleId) {
    this.roleId = roleId;
  }

  public boolean isOnline() {
    return isOnline;
  }

  public void setOnline(boolean online) {
    isOnline = online;
  }

  @Override
  public String toString() {
    return fullName + " (" + username + ")";
  }
}
