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
    @FXML private TextField txtStartTime, txtEndTime;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dpStartDate, dpEndDate;

    // Các thành phần động mới thêm
    @FXML private ComboBox<String> cbItemType;
    @FXML private VBox vboxArt, vboxElectronic, vboxVehicle;

    // Các ô nhập liệu phụ
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

        // 1. Cài đặt ComboBox
        cbItemType.getItems().addAll("Tác phẩm Nghệ thuật (ART)", "Đồ Điện tử (ELECTRONIC)", "Phương tiện (VEHICLE)");
        cbItemType.getSelectionModel().selectFirst(); // Chọn mặc định cái đầu tiên

        // 2. Lắng nghe thay đổi để ẩn/hiện form
        cbItemType.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateDynamicForm(newValue);
        });

        // Kích hoạt form lần đầu tiên
        updateDynamicForm(cbItemType.getValue());
    }

    private void updateDynamicForm(String selectedType) {
        // Tắt hết
        vboxArt.setVisible(false); vboxArt.setManaged(false);
        vboxElectronic.setVisible(false); vboxElectronic.setManaged(false);
        vboxVehicle.setVisible(false); vboxVehicle.setManaged(false);

        // Bật cái tương ứng
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

    @FXML
    private void handleConfirmAdd() {
        try {
            String name = txtProductName.getText().trim();
            String desc = txtDescription.getText().trim();
            String priceStr = txtStartPrice.getText().replace(".", "").replace(",", "").trim();
            String timeStartStr = txtStartTime.getText().trim();
            String timeEndStr = txtEndTime.getText().trim();

            if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || dpStartDate.getValue() == null || dpEndDate.getValue() == null || timeStartStr.isEmpty() || timeEndStr.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin Tên, Giá, Ngày Giờ!");
                return;
            }

            double startPrice = Double.parseDouble(priceStr);

            if (timeStartStr.length() == 5) timeStartStr += ":00";
            if (timeEndStr.length() == 5) timeEndStr += ":00";
            String formattedStartTime = dpStartDate.getValue().toString() + "T" + timeStartStr;
            String formattedEndTime = dpEndDate.getValue().toString() + "T" + timeEndStr;

            // BƯỚC 1: LẤY DỮ LIỆU ĐỘNG CHO ITEM
            ItemCreationRequest itemReq = new ItemCreationRequest();
            itemReq.sellerUserName = SessionManager.userName;
            itemReq.name = name;
            itemReq.description = desc;
            itemReq.startPrice = startPrice;

            String typeStr = cbItemType.getValue();
            if (typeStr.contains("ART")) {
                itemReq.itemType = "ART";
                itemReq.nameAuthor = txtAuthor.getText().trim();
                itemReq.creationYear = txtYear.getText().isEmpty() ? 0 : Integer.parseInt(txtYear.getText().trim());
            } else if (typeStr.contains("ELECTRONIC")) {
                itemReq.itemType = "ELECTRONIC";
                itemReq.brand = txtBrand.getText().trim();
                itemReq.warrantyMonths = txtWarranty.getText().isEmpty() ? 0 : Integer.parseInt(txtWarranty.getText().trim());
            } else if (typeStr.contains("VEHICLE")) {
                itemReq.itemType = "VEHICLE";
                itemReq.engineType = txtEngine.getText().trim();
                itemReq.mileage = txtMileage.getText().isEmpty() ? 0 : Integer.parseInt(txtMileage.getText().trim());
            }

            // GỌI API LÊN SÀN
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