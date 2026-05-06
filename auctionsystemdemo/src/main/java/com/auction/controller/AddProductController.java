package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionCreationRequest;
import com.auction.model.ItemCreationRequest;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class AddProductController {

    @FXML private ImageView productImage;
    @FXML private Label lblPhotoIcon;
    @FXML private TextField txtProductName, txtStartPrice;
    @FXML private TextArea txtDescription;

    // Các ô nhập Thời gian đã được chia nhỏ
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private TextField txtStartHour, txtStartMinute, txtStartSecond;
    @FXML private TextField txtEndHour, txtEndMinute, txtEndSecond;

    // Các thành phần động
    @FXML private ComboBox<String> cbItemType;
    @FXML private VBox vboxArt, vboxElectronic, vboxVehicle;
    @FXML private TextField txtAuthor, txtYear;
    @FXML private TextField txtBrand, txtWarranty;
    @FXML private TextField txtEngine, txtMileage;

    @FXML
    public void initialize() {
        if (!"SELLER".equals(SessionManager.role)) {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Từ chối", "Chỉ SELLER mới được phép đưa sản phẩm lên sàn!");
                goBack();
            });
            return;
        }

        cbItemType.getItems().addAll("Tác phẩm Nghệ thuật (ART)", "Đồ Điện tử (ELECTRONIC)", "Phương tiện (VEHICLE)");
        cbItemType.getSelectionModel().selectFirst();

        cbItemType.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateDynamicForm(newValue);
        });

        updateDynamicForm(cbItemType.getValue());
    }

    private void updateDynamicForm(String selectedType) {
        vboxArt.setVisible(false); vboxArt.setManaged(false);
        vboxElectronic.setVisible(false); vboxElectronic.setManaged(false);
        vboxVehicle.setVisible(false); vboxVehicle.setManaged(false);

        if (selectedType.contains("ART")) {
            vboxArt.setVisible(true); vboxArt.setManaged(true);
        } else if (selectedType.contains("ELECTRONIC")) {
            vboxElectronic.setVisible(true); vboxElectronic.setManaged(true);
        } else if (selectedType.contains("VEHICLE")) {
            vboxVehicle.setVisible(true); vboxVehicle.setManaged(true);
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
        File file = fileChooser.showOpenDialog(txtProductName.getScene().getWindow());
        if (file != null) {
            productImage.setImage(new Image(file.toURI().toString()));
            lblPhotoIcon.setVisible(false);
        }
    }

    // Hàm tiện ích: Đảm bảo format số luôn có 2 chữ số (VD: 9 -> 09)
    private String formatTimePart(String timePart) {
        timePart = timePart.trim();
        if (timePart.isEmpty()) return "00";
        if (timePart.length() == 1) return "0" + timePart;
        return timePart;
    }

    @FXML
    private void handleConfirmAdd() {
        try {
            String name = txtProductName.getText().trim();
            String desc = txtDescription.getText().trim();
            String priceStr = txtStartPrice.getText().replace(".", "").replace(",", "").trim();

            if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin Tên, Giá, Ngày Giờ!");
                return;
            }

            double startPrice = Double.parseDouble(priceStr);

            // Gộp chuỗi thời gian (Giờ:Phút:Giây)
            String startTimeStr = formatTimePart(txtStartHour.getText()) + ":" +
                    formatTimePart(txtStartMinute.getText()) + ":" +
                    formatTimePart(txtStartSecond.getText());

            String endTimeStr = formatTimePart(txtEndHour.getText()) + ":" +
                    formatTimePart(txtEndMinute.getText()) + ":" +
                    formatTimePart(txtEndSecond.getText());

            // Format Giờ gửi lên Spring Boot (VD: 2026-12-31T23:59:59)
            String formattedStartTime = dpStartDate.getValue().toString() + "T" + startTimeStr;
            String formattedEndTime = dpEndDate.getValue().toString() + "T" + endTimeStr;

            // BƯỚC 1: TẠO ITEM
            ItemCreationRequest itemReq = new ItemCreationRequest();
            itemReq.sellerUserName = SessionManager.userName;
            itemReq.name = name;
            itemReq.description = desc;
            itemReq.startPrice = startPrice;

            String typeStr = cbItemType.getValue();
            if (typeStr.contains("ART")) {
                itemReq.itemType = "ART";
                itemReq.nameAuthor = txtAuthor.getText().trim();
                itemReq.creationYear = txtYear.getText().isEmpty() ? null : Integer.parseInt(txtYear.getText().trim());
            } else if (typeStr.contains("ELECTRONIC")) {
                itemReq.itemType = "ELECTRONIC";
                itemReq.brand = txtBrand.getText().trim();
                itemReq.warrantyMonths = txtWarranty.getText().isEmpty() ? null : Integer.parseInt(txtWarranty.getText().trim());
            } else if (typeStr.contains("VEHICLE")) {
                itemReq.itemType = "VEHICLE";
                itemReq.engineType = txtEngine.getText().trim();
                itemReq.mileage = txtMileage.getText().isEmpty() ? null : Integer.parseInt(txtMileage.getText().trim());
            }

            // GỌI API
            ApiService.postAsync("/items/create", itemReq).thenAccept(res1 -> {
                Platform.runLater(() -> {
                    if (res1.statusCode() == 200) {
                        ApiResponse apiRes1 = ApiService.gson.fromJson(res1.body(), ApiResponse.class);
                        if (apiRes1.code == 1000) {

                            String resStr = apiRes1.result.getAsString();
                            String itemId = resStr.substring(resStr.lastIndexOf(":") + 2).trim();

                            AuctionCreationRequest aucReq = new AuctionCreationRequest(itemId, formattedStartTime, formattedEndTime);

                            ApiService.postAsync("/auctions/create", aucReq).thenAccept(res2 -> {
                                Platform.runLater(() -> {
                                    if (res2.statusCode() == 200) {
                                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo sản phẩm và lên lịch đấu giá thành công!");
                                        goBack();
                                    } else {
                                        showAlert(Alert.AlertType.ERROR, "Lỗi tạo Phiên", "Tạo sản phẩm thành công nhưng không thể lên lịch.");
                                    }
                                });
                            });
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi máy chủ", "Mã lỗi: " + res1.statusCode());
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Mất kết nối", "Không thể gọi tới máy chủ."));
                return null;
            });

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá tiền, Năm, Số Km, Bảo hành... chỉ được nhập bằng số.");
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/MyAuctionList.fxml"));
            Node view = loader.load();
            StackPane contentArea = (StackPane) txtProductName.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg);
        alert.showAndWait();
    }
}