package com.auction.controller;

import com.auction.model.Notification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.util.ArrayList;

public class NotificationService {
    private static final String FILE_NAME = "notifications.dat";
    private static final ObservableList<Notification> notifications = FXCollections.observableArrayList();

    static { loadFromFile(); }

    public static ObservableList<Notification> getNotifications() {
        return notifications;
    }

    // HÀM QUAN TRỌNG: Gọi hàm này mỗi khi bạn muốn ghi lại hoạt động
    public static void addNotification(String msg, String type) {
        notifications.add(0, new Notification(msg, type)); // Thêm lên đầu
        saveToFile();
    }

    private static void saveToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(new ArrayList<>(notifications));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ArrayList<Notification> list = (ArrayList<Notification>) ois.readObject();
            notifications.setAll(list);
        } catch (Exception e) { System.out.println("Khởi tạo thông báo mới."); }
    }
}