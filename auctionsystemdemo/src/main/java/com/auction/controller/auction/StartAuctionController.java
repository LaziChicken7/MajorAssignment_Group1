package com.auction.controller.auction;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class StartAuctionController {

    @FXML private Label lblSelectedProduct;
    @FXML private TextField txtStartPrice;
    @FXML private DatePicker dpEnd;
    @FXML private TextField txtEndTime;

    private Stage dialogStage;
    private String currentProductId;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setProductData(String productId, String productName) {
        this.currentProductId = productId;
        lblSelectedProduct.setText("[" + productId + "] - " + productName);
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleStartAuctionSubmit() {
        String startPrice = txtStartPrice.getText();
        LocalDate endDate = dpEnd.getValue();
        String endTime = txtEndTime.getText();

        // TODO: Validate logic rỗng, sai định dạng ở đây

        System.out.println("=== API: POST /auctions ===");
        System.out.println("ProductID: " + currentProductId);
        System.out.println("Start Price: " + startPrice);
        System.out.println("End Time: " + endDate + "T" + endTime + ":00");

        // Sau khi gọi API Gson thành công:
        dialogStage.close();

        // TODO: Kích hoạt Refresh lại danh sách "MyAuctionList" bên dưới nền
    }
}