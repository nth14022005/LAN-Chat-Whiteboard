package com.lanchat.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class LANDiscovery {
  public interface DiscoveryCallback {
    void onServerFound(String host, int port);
  }

  public static void findServerAsync(DiscoveryCallback callback) {
    new Thread(
            () -> {
              try (DatagramSocket socket = new DatagramSocket(9998)) {
                socket.setSoTimeout(3000); // Đợi 3 giây
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                if (msg.startsWith("LAN_CHAT_SERVER:")) {
                  int port = Integer.parseInt(msg.split(":")[1]);
                  String host = packet.getAddress().getHostAddress();
                  callback.onServerFound(host, port);
                }
              } catch (SocketTimeoutException e) {
                callback.onServerFound(null, 0);
              } catch (Exception e) {
                callback.onServerFound(null, 0);
              }
            })
        .start();
  }
}
