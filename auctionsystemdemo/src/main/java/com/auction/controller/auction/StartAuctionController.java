package com.auction.controller.auction;


import lombok.extern.slf4j.Slf4j;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

@Slf4j
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
        log.info("\u25B6 Controller Action - Execute: handleCancel()");
        dialogStage.close();
    }

    @FXML
    private void handleStartAuctionSubmit() {
        log.info("\u25B6 Controller Action - Execute: handleStartAuctionSubmit()");
        String startPrice = txtStartPrice.getText();
        LocalDate endDate = dpEnd.getValue();
        String endTime = txtEndTime.getText();

        // TODO: Validate logic rỗng, sai định dạng ở đây

        log.info("=== API: POST /auctions ===");
        log.info("ProductID: " + currentProductId);
        log.info("Start Price: " + startPrice);
        log.info("End Time: " + endDate + "T" + endTime + ":00");

        // Sau khi gọi API Gson thành công:
        dialogStage.close();

        // TODO: Kích hoạt Refresh lại danh sách "MyAuctionList" bên dưới nền
    }
}