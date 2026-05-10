package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionCreationRequest;
import com.auction.model.ItemCreationRequest;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;

public class AddProductController {

    @FXML private ImageView productImage;
    @FXML private Label lblPhotoIcon;
    @FXML private TextField txtProductName, txtStartPrice;
    @FXML private TextArea txtDescription;    
    @FXML private HBox imagePreviewBox;
    private java.util.List<File> selectedFiles = new java.util.ArrayList<>();

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

    @FXML private CheckBox chkAgree;
    @FXML private Button btnSubmit;

    @FXML
    public void initialize() {
        if (btnSubmit != null && chkAgree != null) {
            btnSubmit.disableProperty().bind(chkAgree.selectedProperty().not());
        }

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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg"));
        
        // Dùng showOpenMultipleDialog để cho phép bôi đen chọn nhiều ảnh
        java.util.List<File> files = fileChooser.showOpenMultipleDialog(txtProductName.getScene().getWindow());
        
        if (files != null && !files.isEmpty()) {
            selectedFiles.addAll(files);
            imagePreviewBox.getChildren().clear(); // Xóa ảnh cũ trên màn hình
            
            // Vòng lặp vẽ từng cái ảnh nhỏ nhỏ xếp cạnh nhau
            for (File f : selectedFiles) {
                ImageView imgView = new ImageView(new javafx.scene.image.Image(f.toURI().toString()));
                imgView.setFitHeight(200);
                imgView.setFitWidth(150);
                imgView.setPreserveRatio(true);
                imagePreviewBox.getChildren().add(imgView);
            }
            if(lblPhotoIcon != null) lblPhotoIcon.setVisible(false);
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
        // 1. Lấy dữ liệu cơ bản để validate trước khi hiện Popup
        String name = txtProductName.getText().trim();
        String desc = txtDescription.getText().trim();
        String priceStr = txtStartPrice.getText().replace(".", "").replace(",", "").trim();

        // Kiểm tra rỗng: Đảm bảo điền đủ mới cho xem Hợp đồng
        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin Tên, Giá, Ngày Giờ!");
            return;
        }

        // 2. Tạo Popup Hợp đồng
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Điều khoản và Dịch vụ");
        dialog.setHeaderText("HỢP ĐỒNG ĐĂNG BÁN SẢN PHẨM ĐẤU GIÁ");

        TextArea textArea = new TextArea(getContractText());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(600, 400);

        CheckBox chkPopupAgree = new CheckBox("Tôi xác nhận đã đọc và đồng ý với các điều khoản");
        chkPopupAgree.setStyle("-fx-font-size: 12px; -fx-padding: 10 0 0 0; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-cursor: hand;");

        VBox dialogContent = new VBox(10);
        dialogContent.getChildren().addAll(textArea, chkPopupAgree);
        dialog.getDialogPane().setContent(dialogContent);

        // Thêm nút Đồng ý và Hủy
        ButtonType btnTypeAgree = new ButtonType("Đồng ý", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnTypeCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeAgree, btnTypeCancel);

        // Khóa nút Đồng ý mặc định
        Node agreeButton = dialog.getDialogPane().lookupButton(btnTypeAgree);
        agreeButton.setDisable(true);

        // Tick Checkbox để mở khóa nút Đồng ý
        chkPopupAgree.selectedProperty().addListener((observable, oldValue, newValue) -> {
            agreeButton.setDisable(!newValue);
        });

        // 3. Hiển thị Popup. Nếu user bấm Đồng ý -> Chạy hàm xử lý gọi API
        dialog.showAndWait().ifPresent(response -> {
            if (response == btnTypeAgree) {
                processActualSubmission(name, desc, priceStr);
            }
        });
    }

    // HÀM 1: Bắt đầu quy trình (Chạy khi user bấm Xác nhận Hợp đồng)
    private void processActualSubmission(String name, String desc, String priceStr) {
        try {
            double startPrice = Double.parseDouble(priceStr);

            // Gộp chuỗi thời gian (Giờ:Phút:Giây)
            String startTimeStr = formatTimePart(txtStartHour.getText()) + ":" + formatTimePart(txtStartMinute.getText()) + ":" + formatTimePart(txtStartSecond.getText());
            String endTimeStr = formatTimePart(txtEndHour.getText()) + ":" + formatTimePart(txtEndMinute.getText()) + ":" + formatTimePart(txtEndSecond.getText());
            String formattedStartTime = dpStartDate.getValue().toString() + "T" + startTimeStr;
            String formattedEndTime = dpEndDate.getValue().toString() + "T" + endTimeStr;

            // BƯỚC A: TẠO ITEM TRƯỚC (KHÔNG CÓ ẢNH) ĐỂ LẤY ID TỪ SERVER
            ItemCreationRequest itemReq = new ItemCreationRequest();
            itemReq.sellerUserName = SessionManager.userName;
            itemReq.name = name;
            itemReq.description = desc;
            itemReq.startPrice = startPrice;
            itemReq.imageUrls = new java.util.ArrayList<>(); // Để rỗng trước

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

            // Gọi API Tạo Item
            ApiService.postAsync("/items/create", itemReq).thenAccept(res1 -> {
                if (res1.statusCode() == 200) {
                    ApiResponse apiRes1 = ApiService.gson.fromJson(res1.body(), ApiResponse.class);
                    if (apiRes1.code == 1000) {

                        // Lấy Item ID từ kết quả trả về
                        String resStr = apiRes1.result.getAsString();
                        String itemId = resStr.substring(resStr.lastIndexOf(":") + 2).trim();

                        // BƯỚC B: KIỂM TRA CÓ ẢNH KHÔNG
                        if (!selectedFiles.isEmpty()) {
                            // Nếu có ảnh -> Gọi hàm upload ảnh vào ID đó
                            uploadImagesToServer(itemId, formattedStartTime, formattedEndTime);
                        } else {
                            // Nếu không có ảnh -> Chuyển thẳng sang tạo Phiên đấu giá
                            createAuctionForData(itemId, formattedStartTime, formattedEndTime);
                        }
                    }
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi máy chủ", "Mã lỗi: " + res1.statusCode()));
                }
            }).exceptionally(ex -> {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Mất kết nối", "Không thể gọi tới máy chủ."));
                return null;
            });

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá tiền, Năm, Số Km... chỉ được nhập bằng số.");
        }
    }

    // HÀM 2: Gắn ảnh vào Item đã tạo
    private void uploadImagesToServer(String itemId, String formattedStartTime, String formattedEndTime) {
        new Thread(() -> {
            try {
                // Gọi API MỚI: /items/{itemId}/upload-images
                String urlStr = ApiService.BASE_URL + "/items/" + itemId + "/upload-images";
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(urlStr).openConnection();
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                java.io.OutputStream out = conn.getOutputStream();
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(out, "UTF-8"), true);

                for (File file : selectedFiles) {
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"files\"; filename=\"" + file.getName() + "\"").append("\r\n");
                    writer.append("Content-Type: " + java.nio.file.Files.probeContentType(file.toPath())).append("\r\n\r\n");
                    writer.flush();
                    java.nio.file.Files.copy(file.toPath(), out);
                    out.flush();
                    writer.append("\r\n").flush();
                }
                writer.append("--" + boundary + "--\r\n").flush();

                // Đợi upload xong thì sang BƯỚC C: Tạo phiên đấu giá
                int code = conn.getResponseCode();
                if(code >= 200 && code < 300) {
                    createAuctionForData(itemId, formattedStartTime, formattedEndTime);
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi Upload", "Không thể tải ảnh, nhưng sản phẩm đã được tạo."));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // HÀM 3: Cuối cùng, tạo phiên đấu giá
    private void createAuctionForData(String itemId, String formattedStartTime, String formattedEndTime) {
        AuctionCreationRequest aucReq = new AuctionCreationRequest(itemId, formattedStartTime, formattedEndTime);

        ApiService.postAsync("/auctions/create", aucReq).thenAccept(res2 -> {
            Platform.runLater(() -> {
                if (res2.statusCode() == 200) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo sản phẩm và tải ảnh lên thành công!");

                    // Dọn dẹp RAM
                    selectedFiles.clear();
                    imagePreviewBox.getChildren().clear();

                    goBack();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi tạo Phiên", "Tạo sản phẩm thành công nhưng không thể lên lịch đấu giá.");
                }
            });
        });
    }

    private String getContractText() {
        return "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\n"
                + "Độc lập - Tự do - Hạnh phúc\n\n"
                + "ĐIỀU KHOẢN VÀ DỊCH VỤ DÀNH CHO NGƯỜI BÁN (SELLER AGREEMENT)\n\n"
                + "Chào mừng bạn đến với Hệ thống Đấu giá Trực tuyến. Bằng việc đăng bán sản phẩm trên nền tảng của chúng tôi, bạn (sau đây gọi là \"Người Bán\") đồng ý tuân thủ toàn bộ các điều khoản và điều kiện dưới đây:\n\n"
                + "ĐIỀU 1: TÍNH TRUNG THỰC VÀ NGUỒN GỐC SẢN PHẨM\n"
                + "1.1. Người Bán cam kết chịu trách nhiệm 100% trước pháp luật về tính hợp pháp, nguồn gốc xuất xứ và quyền sở hữu hợp pháp của tài sản/sản phẩm được đưa lên đấu giá.\n"
                + "1.2. Nghiêm cấm mọi hành vi đăng bán hàng giả, hàng nhái, hàng cấm, hàng vi phạm sở hữu trí tuệ hoặc tài sản đang có tranh chấp.\n"
                + "1.3. Hình ảnh và mô tả sản phẩm phải phản ánh chính xác tình trạng thực tế của tài sản. Mọi hư hỏng (nếu có) phải được mô tả minh bạch.\n\n"
                + "ĐIỀU 2: TRÁCH NHIỆM TRONG QUÁ TRÌNH ĐẤU GIÁ\n"
                + "2.1. Sau khi sản phẩm được hệ thống phê duyệt và bắt đầu phiên đấu giá, Người Bán không được quyền tự ý hủy bỏ phiên đấu giá trừ trường hợp bất khả kháng có sự đồng ý của Ban Quản Trị.\n"
                + "2.2. Người bán không được phép sử dụng tài khoản phụ hoặc nhờ người thân tham gia "
                + "đẩy giá (shill bidding) dưới mọi hình thức. Nếu hệ thống phát hiện, tài sản sẽ bị hủy đấu giá và tài khoản Người Bán sẽ bị khóa vĩnh viễn.\n\n"
                + "ĐIỀU 3: PHÍ DỊCH VỤ VÀ THANH TOÁN\n"
                + "3.1. Nền tảng sẽ thu một khoản phí dịch vụ là X% (tùy thuộc vào danh mục sản phẩm) trên tổng giá trị giao dịch thành công (Giá chốt cuối cùng).\n"
                + "3.2. Tiền thu được từ người mua (sau khi trừ phí dịch vụ) sẽ được chuyển vào ví điện tử của Người Bán trên hệ thống trong vòng 3-5 ngày làm việc kể từ khi người mua xác nhận đã nhận hàng thành công.\n\n"
                + "ĐIỀU 4: BÀN GIAO SẢN PHẨM\n"
                + "4.1. Sau khi phiên đấu giá kết thúc và người mua hoàn tất thanh toán, Người Bán có trách nhiệm đóng gói cẩn thận và giao hàng trong vòng 48 giờ.\n"
                + "4.2. Mọi rủi ro trong quá trình vận chuyển do lỗi đóng gói kém chất lượng sẽ do Người Bán hoàn toàn chịu trách nhiệm bồi thường.\n\n"
                + "ĐIỀU 5: XỬ LÝ VI PHẠM\n"
                + "5.1. Vi phạm bất kỳ điều khoản nào trên đây sẽ dẫn đến việc đình chỉ tài khoản, đóng băng số dư trong ví tạm thời để giải quyết tranh chấp, và trong trường hợp nghiêm trọng, hồ sơ sẽ được chuyển giao cho cơ quan chức năng có thẩm quyền.\n\n"
                + "Bằng việc đánh dấu vào ô \"Tôi đã đọc và đồng ý\", bạn xác nhận đã hiểu rõ và cam kết thi hành nghiêm túc bản thỏa thuận này.";
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