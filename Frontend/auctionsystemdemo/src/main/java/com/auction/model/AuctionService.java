package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionService {
    private static final String FILE_NAME = "auctions_data.dat";
    private static final ObservableList<AuctionItem> allAuctions = FXCollections.observableArrayList();
    // Danh sách dành riêng cho trang "Sản phẩm của tôi"
    private static final ObservableList<AuctionItem> myAuctions = FXCollections.observableArrayList();

    static {
        loadFromFile();
        // Sản phẩm mẫu (isMine = false) - Chỉ xuất hiện ở trang Đấu giá chung
        if (allAuctions.isEmpty()) {
            allAuctions.add(new AuctionItem(" mẫu 1", "Sản phẩm mẫu 1", 1000, 1200, "00:01:12", "RUNNING", "Mô tả sản phẩm mẫu 1", false));
            allAuctions.add(new AuctionItem(" mẫu 2", "Sản phẩm mẫu 2", 5000, 5000, "00:00:00", "SUCCESS", "Mô tả sản phẩm mẫu 2", false));
            saveToFile();
        }
        updateMyList(); // Lọc ra những cái của bạn từ file đã lưu
    }

    public static ObservableList<AuctionItem> getAllAuctions() { return allAuctions; }
    public static ObservableList<AuctionItem> getMyAuctions() { return myAuctions; }

    public static void addAuction(AuctionItem item) {
        allAuctions.add(0, item);
        if (item.isMine()) {
            myAuctions.add(0, item);
        }
        saveToFile();
    }

    // Hàm cập nhật danh sách "Của tôi" dựa trên danh sách tổng
    private static void updateMyList() {
        List<AuctionItem> mine = allAuctions.stream()
                .filter(AuctionItem::isMine)
                .collect(Collectors.toList());
        myAuctions.setAll(mine);
    }

    public static void saveToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(new ArrayList<>(allAuctions));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<AuctionItem> list = (List<AuctionItem>) ois.readObject();
            allAuctions.setAll(list);
        } catch (Exception e) { e.printStackTrace(); }
    }
}