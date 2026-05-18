package com.auction.controller.wallet;

import com.auction.model.ApiResponse;
import com.auction.model.TransactionHistoryResponse;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

public class WalletController {

    @FXML private VBox walletRoot;
    @FXML private VBox historySection;

    @FXML private TextField balanceField;
    @FXML private Label eyeIconText;
    @FXML private Label lblFrozenBalance;
    @FXML private Label lblBankAccount;

    @FXML private VBox vboxSuccess;
    @FXML private VBox vboxFailed;

    @FXML private Label lblCardNumber;
    @FXML private Label lblCardName;

    @FXML private Label eyeIconFrozenText;

    private String realFrozenBalance = "0 VND";
    private boolean isFrozenHidden = true;

    private double actualBalance = 0.0;
    private boolean isBalanceVisible = false;

    @FXML
    public void initialize() {
        loadWalletData();
    }

    private void loadWalletData() {
        if (SessionManager.userName == null) return;

        ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(response -> {
            Platform.runLater(() -> {
                try {
                    if (response.statusCode() == 200) {
                        ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                        if (apiResponse.code == 1000) {
                            WalletDataResponse walletData = ApiService.gson.fromJson(apiResponse.result, WalletDataResponse.class);

                            lblBankAccount.setText(walletData.bankAccountNumber != null ? walletData.bankAccountNumber : "Chưa có");

                            // --- CẬP NHẬT GIAO DIỆN THẺ VISA ---
                            if (SessionManager.fullName != null) {
                                // Gọi thẳng hàm removeAccents private tích hợp sẵn ở cuối class
                                lblCardName.setText(removeAccents(SessionManager.fullName).toUpperCase());
                            } else if (SessionManager.userName != null) {
                                lblCardName.setText(SessionManager.userName.toUpperCase());
                            }

                            // Format số tài khoản thành dạng thẻ tín dụng: cách nhau mỗi 4 số
                            if (walletData.bankAccountNumber != null && !walletData.bankAccountNumber.isEmpty()) {
                                String acc = walletData.bankAccountNumber;
                                // Dùng Regex để chèn khoảng trắng sau mỗi 4 ký tự
                                String formattedAcc = acc.replaceAll(".{4}(?!$)", "$0 ");
                                lblCardNumber.setText(formattedAcc);
                            } else {
                                lblCardNumber.setText("**** **** **** ****");
                            }
                            // -----------------------------------

                            this.actualBalance = walletData.moneyOnWallet;
                            updateBalanceDisplay();

                            realFrozenBalance = String.format("%,.0f VND", walletData.moneyinFrozen).replace(",", ".");
                            if (!isFrozenHidden) {
                                lblFrozenBalance.setText(realFrozenBalance);
                            } else {
                                lblFrozenBalance.setText("****** VND");
                            }

                            renderTransactions(walletData.successTransaction, vboxSuccess, true);
                            renderTransactions(walletData.failedTransaction, vboxFailed, false);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Lỗi parse JSON trong Wallet: " + e.getMessage());
                }
            });
        });
    }

    @FXML
    public void toggleBalanceVisibility(MouseEvent event) {
        isBalanceVisible = !isBalanceVisible;
        updateBalanceDisplay();
    }

    private void updateBalanceDisplay() {
        if (isBalanceVisible) {
            balanceField.setText(formatMoney(actualBalance));
            eyeIconText.setText("Ẩn");
            eyeIconText.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF0000; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            balanceField.setText("****** VND");
            eyeIconText.setText("Hiện");
            eyeIconText.setStyle("-fx-font-size: 16px; -fx-text-fill: #0A439D; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    @FXML
    public void toggleFrozenBalanceVisibility() {
        isFrozenHidden = !isFrozenHidden;
        if (isFrozenHidden) {
            lblFrozenBalance.setText("****** VND");
            eyeIconFrozenText.setText("Hiện");
        } else {
            lblFrozenBalance.setText(realFrozenBalance);
            eyeIconFrozenText.setText("Ẩn");
        }
    }

    private void renderTransactions(List<TransactionHistoryResponse> transactions, VBox container, boolean isSuccess) {
        container.getChildren().clear();

        if (transactions == null || transactions.isEmpty()) {
            Label lblEmpty = new Label("Chưa có giao dịch nào");
            lblEmpty.getStyleClass().add("muted-text");
            lblEmpty.setStyle("-fx-font-style: italic; -fx-font-size: 14px;");
            container.getChildren().add(lblEmpty);
            return;
        }

        int limit = Math.min(transactions.size(), 3);
        for (int i = 0; i < limit; i++) {
            TransactionHistoryResponse tx = transactions.get(i);
            String status = tx.status != null ? tx.status : "FAILED";
            String itemName = tx.itemName != null ? tx.itemName : "Sản phẩm ẩn";

            String colorCode = isSuccess ? "#00C853" : "#FF0000";
            if ("CANCELLED".equals(status)) {
                colorCode = "#e74c3c";
            }

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("custom-row");
            row.setStyle("-fx-padding: 10 20; -fx-background-radius: 25;");

            Label lblName = new Label(itemName);
            lblName.getStyleClass().add("row-title-bold");
            lblName.setStyle("-fx-font-size: 14px;");
            HBox.setHgrow(lblName, Priority.ALWAYS);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblAmount = new Label(formatMoney(tx.amount));
            lblAmount.setStyle("-fx-background-color: " + colorCode + "; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold;");

            row.getChildren().addAll(lblName, spacer, lblAmount);
            container.getChildren().add(row);
        }
    }

    private void switchView(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            Node source = (Node) event.getSource();
            Pane contentArea = (Pane) source.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void handleDeposit(ActionEvent event) { switchView(event, "/com/auction/view/wallet/DepositForm.fxml"); }
    @FXML public void handleWithdraw(ActionEvent event) { switchView(event, "/com/auction/view/wallet/WithDrawForm.fxml"); }
    @FXML public void handleSuccessfulTransaction(ActionEvent event) { switchView(event, "/com/auction/view/wallet/SuccessfulTransaction.fxml"); }
    @FXML public void handleFailureTransaction(ActionEvent event) { switchView(event, "/com/auction/view/wallet/FailureTransaction.fxml"); }

    private String formatMoney(double amount) {
        return String.format("%,.0f", amount).replace(",", ".") + " VND";
    }

    /**
     * Tích hợp sẵn hàm loại bỏ dấu tiếng Việt để in hoa chuẩn xác lên thẻ Visa
     */
    private String removeAccents(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(temp).replaceAll("");
        return result.replace("đ", "d").replace("Đ", "D");
    }
}