package com.auction.controller;

import com.auction.model.AuctionItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;

public class AddProductController {

    @FXML private ImageView productImage;
    @FXML private Label lblIcon; // Icon 🖼 ẩn đi khi có ảnh
    @FXML private TextField txtProductId, txtProductName, txtStartPrice, txtEndTime;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dpEnd;

    // 1. XỬ LÝ TẢI ẢNH
    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(txtProductId.getScene().getWindow());
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            productImage.setImage(image);
            lblIcon.setVisible(false); // Ẩn icon mặc định đi
        }
    }

    // 2. XỬ LÝ XÁC NHẬN (THÊM SP VÀO LIST)
    @FXML
    private void handleConfirmAdd() {
        try {
            // Lấy dữ liệu từ TextField
            String id = txtProductId.getText();
            String name = txtProductName.getText();
            String priceRaw = txtStartPrice.getText().replace(".", "").replace(",", "");
            String time = txtEndTime.getText().isEmpty() ? "24:00:00" : txtEndTime.getText();

            if (id.isEmpty() || name.isEmpty() || priceRaw.isEmpty()) {
                System.err.println("Vui lòng nhập đủ thông tin!");
                return;
            }

            double price = Double.parseDouble(priceRaw);

            // Tạo đối tượng mới và thêm vào Service (Kho chung)
            AuctionItem newAuction = new AuctionItem(id, name, price, price, time, "RUNNING", txtDescription.getText(), true);
            com.auction.controller.AuctionService.addAuction(newAuction);
            com.auction.controller.NotificationService.addNotification("Bạn vừa tạo sản phẩm mới: " + name, "ACTION");

            System.out.println("Đã thêm thành công sản phẩm từ bàn phím!");

            // Tự động quay về danh sách sau khi thêm
            goBack();

        } catch (NumberFormatException e) {
            System.err.println("Giá tiền phải là số!");
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/MyAuctionList.fxml"));
            Node view = loader.load();
            // Tìm contentArea để thay thế giao diện giữa mà không mất Sidebar
            StackPane contentArea = (StackPane) txtProductId.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}