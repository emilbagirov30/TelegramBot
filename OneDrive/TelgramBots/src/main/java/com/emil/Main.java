package com.emil;

import com.emil.bot.SlavaBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new SlavaBot());

        JFrame frame = new JFrame("SlavaBot");
        frame.setLayout(new BorderLayout());
        JLabel label1 = new JLabel("Бот запущен", JLabel.CENTER);
        frame.add(label1, BorderLayout.NORTH);
        JLabel label2 = new JLabel("Разработчик: Емиль Багиров", JLabel.CENTER);
        frame.add(label2, BorderLayout.SOUTH);
        JPanel centerPanel = new JPanel(new BorderLayout());
        frame.add(centerPanel, BorderLayout.CENTER);
        JButton reloadButton = new JButton("Перезагрузить");
        centerPanel.add(reloadButton, BorderLayout.SOUTH);
        frame.setSize(350, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        reloadButton.addActionListener(e -> {
            try {
                String javaBin = System.getProperty("java.home") + "/bin/java";
                String classPath = System.getProperty("java.class.path");
                String className = Main.class.getName();

                ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classPath, className);
                builder.start();
                System.exit(0);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        ClassLoader classLoader = Main.class.getClassLoader();
        ImageIcon icon = new ImageIcon(classLoader.getResource("bot_icon.png"));
        frame.setIconImage(icon.getImage());
    }
}
