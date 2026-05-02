package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionService {
    private static final String FILE_NAME = "auctions_data.dat";
    private static final ObservableList<AuctionItem> allAuctions = FXCollections.observableArrayList();

    // Khối này chạy ngay khi ứng dụng khởi động để nạp dữ liệu cũ
    static {
        loadFromFile();
        // Nếu file trắng, thêm 1 cái mẫu để test
        if (allAuctions.isEmpty()) {
            allAuctions.add(new AuctionItem("SP1", "Sản phẩm mẫu", 1000, 1200, "00:01:12", "RUNNING", "Mô tả..."));
            saveToFile();
        }
    }

    public static ObservableList<AuctionItem> getAllAuctions() {
        return allAuctions;
    }

    public static void addAuction(AuctionItem item) {
        allAuctions.add(0, item);
        saveToFile(); // Lưu ngay xuống ổ cứng khi thêm mới
    }

    public static void saveToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(new ArrayList<>(allAuctions));
            System.out.println(">>> Đã lưu dữ liệu thành công!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<AuctionItem> list = (List<AuctionItem>) ois.readObject();
            allAuctions.setAll(list);
            System.out.println(">>> Đã khôi phục dữ liệu từ file!");
        } catch (Exception e) {
            System.out.println("Không thể đọc dữ liệu cũ, khởi tạo mới.");
        }
    }
}