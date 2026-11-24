package com.lanchat.main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.lanchat.ui.LoginFrame;
import java.awt.*;
import javax.swing.*;

public class MainApp {
  public static void configureTheme() {
    FlatDarkLaf.setup();
    UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
    UIManager.put("Component.arc", 10);
    UIManager.put("Button.arc", 10);
    UIManager.put("TextComponent.arc", 8);
    UIManager.put("Panel.background", new Color(36, 42, 56));
    UIManager.put("TabbedPane.selectedBackground", new Color(56, 66, 89));
    UIManager.put("TabbedPane.underlineColor", new Color(129, 140, 248));
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          configureTheme();
          new LoginFrame().setVisible(true);
        });
  }
}
