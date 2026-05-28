package com.auction.controller.wallet;


import lombok.extern.slf4j.Slf4j;
import com.auction.model.ApiResponse;
import com.auction.model.TransactionHistoryResponse;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class TransactionController {

    @FXML private VBox successContainer;
    @FXML private VBox failedContainer;

    @FXML
    public void initialize() {
        log.info("\u25B6 Controller Action - Execute: initialize()");
        loadTransactionData();
    }

    private void loadTransactionData() {
        log.info("\u25B6 Controller Action - Execute: loadTransactionData()");
        if (SessionManager.userName == null) return;

        ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(response -> {
            Platform.runLater(() -> {
                try {
                    if (response.statusCode() == 200) {
                        ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                        if (apiResponse.code == 1000) {
                            WalletDataResponse walletData = ApiService.gson.fromJson(apiResponse.result, WalletDataResponse.class);

                            // Truyền thêm cờ true/false để ép UI render đúng màu theo danh sách
                            if (successContainer != null) {
                                renderList(walletData.successTransaction, successContainer, true);
                            }
                            if (failedContainer != null) {
                                renderList(walletData.failedTransaction, failedContainer, false);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.info("Lỗi parse JSON trong Transaction: " + e.getMessage());
                }
            });
        });
    }

    // Đã thêm cờ boolean isSuccessType
    private void renderList(List<TransactionHistoryResponse> list, VBox container, boolean isSuccessType) {
        container.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label emptyLbl = new Label("Không có giao dịch nào.");
            emptyLbl.getStyleClass().add("muted-text");
            emptyLbl.setStyle("-fx-font-size: 16px; -fx-font-style: italic;");
            container.getChildren().add(emptyLbl);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            TransactionHistoryResponse tx = list.get(i);
            String status = tx.status != null ? tx.status : "FAILED";
            String itemName = tx.itemName != null ? tx.itemName : "Sản phẩm ẩn";

            String colorHex;
            String statusText;

            // Xử lý ép màu dựa trên danh sách chứa nó (Thành công hay Thất bại)
            if (isSuccessType) {
                colorHex = "#00C853";
                statusText = "Giao dịch thành công";
            } else {
                if ("CANCELLED".equals(status)) {
                    colorHex = "#e74c3c"; // Màu đỏ cam
                    statusText = "Đã từ chối / Hủy giao dịch";
                } else {
                    colorHex = "#FF0000"; // Màu đỏ tươi
                    statusText = "Giao dịch không thành công";
                }
            }

            VBox card = new VBox(15);
            card.getStyleClass().add("custom-row");
            card.setStyle("-fx-padding: 20;");

            HBox headerBox = new HBox(15);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            String shortId = tx.itemId != null && tx.itemId.length() >= 4 ? tx.itemId.substring(0, 4).toUpperCase() : "N/A";
            Label lblId = new Label("SP" + shortId);
            lblId.getStyleClass().add("row-title-bold");
            lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

            Label lblName = new Label(itemName);
            lblName.getStyleClass().add("row-text-normal");
            lblName.setStyle("-fx-font-size: 18px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblAmount = new Label(formatMoney(tx.amount));
            lblAmount.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 15px;");

            headerBox.getChildren().addAll(lblId, lblName, spacer, lblAmount);

            HBox detailBox = new HBox(20);
            detailBox.setAlignment(Pos.CENTER_LEFT);

            // XỬ LÝ ẢNH
            javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
            imgView.setFitWidth(100);
            imgView.setFitHeight(90);

            String imgPath = "https://via.placeholder.com/100x90?text=No+Image";
            try {
                if (tx.imageUrl != null && !tx.imageUrl.isEmpty()) {
                    imgPath = ApiService.BASE_URL + tx.imageUrl;
                }
            } catch (Exception e) {}

            imgView.setImage(new javafx.scene.image.Image(imgPath, true));

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(100, 90);
            clip.setArcWidth(15);
            clip.setArcHeight(15);
            imgView.setClip(clip);

            VBox imageBox = new VBox(imgView);
            imageBox.setPrefSize(100, 90);
            imageBox.setStyle("-fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

            // LẤY THỜI GIAN THẬT CỦA GIAO DỊCH (Thay thế dòng Lỗi LocalDateTime.now() của bạn)
            // LƯU Ý: Bạn cần thay `tx.createdAt` hoặc biến chứa ngày giờ thật trong Model của bạn vào đây.
            // Nếu Model của bạn không có, nó sẽ tạm thời lấy giờ hiện tại.
            String displayDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            // Ví dụ sửa lại thành:
            // if (tx.transactionDate != null) displayDate = tx.transactionDate;

            VBox infoColumn = new VBox(8);
            infoColumn.setStyle("-fx-font-size: 15px;");

            infoColumn.getChildren().addAll(
                    createDetailRow("Ngày ghi nhận:", displayDate, null),
                    createDetailRow("Địa chỉ giao hàng:", "Chưa cập nhật", null),
                    createDetailRow("Trạng thái:", statusText, colorHex)
            );

            detailBox.getChildren().addAll(imageBox, infoColumn);
            card.getChildren().addAll(headerBox, detailBox);
            container.getChildren().add(card);
        }
    }

    private HBox createDetailRow(String title, String value, String customColor) {
        HBox row = new HBox(10);
        Label lblTitle = new Label(title);
        lblTitle.setPrefWidth(200);
        lblTitle.getStyleClass().add("muted-text");
        lblTitle.setStyle("-fx-font-weight: bold;");

        Label lblValue = new Label(value);
        if (customColor == null) {
            lblValue.getStyleClass().add("row-title-bold");
        } else {
            lblValue.setStyle("-fx-text-fill: " + customColor + "; -fx-font-weight: bold;");
        }

        row.getChildren().addAll(lblTitle, lblValue);
        return row;
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f", amount).replace(",", ".") + " VND";
    }

    @FXML
    public void handleBackToWallet(javafx.scene.input.MouseEvent event) {
        log.info("\u25B6 Controller Action - Execute: handleBackToWallet()");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/wallet/Wallet.fxml"));
            Node view = loader.load();
            Node source = (Node) event.getSource();
            Pane contentArea = (Pane) source.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            log.error("Exception occurred", e);
        }
    }
}