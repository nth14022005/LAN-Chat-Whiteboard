package com.lanchat.network;

import com.lanchat.model.*;

/** Persistence boundary. The production implementation uses the existing SQL schema. */
public interface DataStore {
  User authenticate(String login, String password, String ip) throws Exception;

  void register(String[] fields) throws Exception;

  int room(String name, int owner) throws Exception;

  void member(int room, int user, boolean host) throws Exception;

  void lock(int room, boolean locked) throws Exception;

  String history(int room) throws Exception;

  void group(int room, int user, String text) throws Exception;

  void direct(int sender, int receiver, String text) throws Exception;

  void file(int room, int sender, String name, long size, String path) throws Exception;

  void export(int room, int user, String name, String path, String resolution) throws Exception;

  void offline(int user, String ip);

  void stroke(int room, int user, DrawShape shape) throws Exception;

  void audit(int user, String action, String description, String ip) throws Exception;
}
