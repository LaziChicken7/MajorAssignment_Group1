package com.auction.controller.addauctionitem;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle; // IMPORT THƯ VIỆN BO GÓC ẢNH
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

    @FXML private VBox paneImageZoom;
    @FXML private ImageView zoomedImageView;

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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg"));

        java.util.List<File> files = fileChooser.showOpenMultipleDialog(txtProductName.getScene().getWindow());

        if (files != null && !files.isEmpty()) {
            // Kiểm tra dung lượng
            long maxFileSize = 5 * 1024 * 1024; // Giới hạn 5MB
            for (File f : files) {
                if (f.length() > maxFileSize) {
                    showAlert(Alert.AlertType.WARNING, "Ảnh quá lớn",
                            "Bức ảnh '" + f.getName() + "' nặng hơn 5MB.\nVui lòng chọn ảnh nhẹ hơn để tải lên!");
                    return;
                }
            }

            // Thêm danh sách ảnh mới vào List tổng
            selectedFiles.addAll(files);

            // Gọi hàm Render để vẽ lại toàn bộ ảnh ra màn hình
            renderImagePreviews();
        }
    }

    // HÀM MỚI: Tách logic vẽ ảnh ra riêng để dùng chung khi Thêm và Xóa
    private void renderImagePreviews() {
        imagePreviewBox.getChildren().clear();

        for (File f : selectedFiles) {
            Image imageFile = new Image("file:" + f.getAbsolutePath());
            ImageView imgView = new ImageView(imageFile);
            imgView.setFitHeight(200);
            imgView.setFitWidth(200);
            imgView.setPreserveRatio(false);

            // Gắn class css để rê chuột vào đổi thành hình bàn tay báo hiệu có thể click
            imgView.setStyle("-fx-cursor: hand;");

            // XỬ LÝ SỰ KIỆN CLICK VÀO ẢNH ĐỂ MỞ POPUP ZOOM
            imgView.setOnMouseClicked(e -> {
                zoomedImageView.setImage(imageFile);
                paneImageZoom.setVisible(true);
            });

            Rectangle clip = new Rectangle(200, 200);
            clip.setArcWidth(25);
            clip.setArcHeight(25);
            imgView.setClip(clip);

            // NÚT XÓA ẢNH (X)
            Button btnDelete = new Button("✕");
            btnDelete.setStyle("-fx-background-color: rgba(231, 76, 60, 0.9); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 50; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-size: 14px;");

            StackPane.setAlignment(btnDelete, javafx.geometry.Pos.TOP_RIGHT);
            StackPane.setMargin(btnDelete, new javafx.geometry.Insets(10, 10, 0, 0));

            btnDelete.setOnAction(e -> {
                selectedFiles.remove(f);
                renderImagePreviews();
            });

            StackPane imageContainer = new StackPane(imgView, btnDelete);
            imageContainer.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");

            imagePreviewBox.getChildren().add(imageContainer);
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
        // 1. Lấy dữ liệu cơ bản để validate
        String name = txtProductName.getText().trim();
        String desc = txtDescription.getText().trim();
        String priceStr = txtStartPrice.getText().replace(".", "").replace(",", "").trim();

        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin Tên, Giá, Ngày Giờ!");
            return;
        }

        // 2. Tạo Popup Hợp đồng hiện đại
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Điều khoản và Dịch vụ");

        DialogPane dialogPane = dialog.getDialogPane();

        // ========================================================
        // PHÉP THUẬT 1: ÉP POPUP NẠP FILE CSS & DARK MODE TỪ MÀN HÌNH MẸ
        // ========================================================
        // Truy ngược lên Node gốc cùng để lấy file style.css truyền vào Popup
        javafx.scene.Parent parent = txtProductName.getParent();
        while (parent != null) {
            if (!parent.getStylesheets().isEmpty()) {
                dialogPane.getStylesheets().addAll(parent.getStylesheets());
            }
            parent = parent.getParent();
        }

        if (txtProductName.getScene() != null) {
            dialogPane.getStylesheets().addAll(txtProductName.getScene().getStylesheets());
            if (txtProductName.getScene().getRoot() != null) {
                dialogPane.getStylesheets().addAll(txtProductName.getScene().getRoot().getStylesheets());

                // Nếu App đang ở Dark Mode -> Ép Popup bật Dark Mode
                if (txtProductName.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                    dialogPane.getStyleClass().add("dark-theme");
                }
            }
        }

        // Gắn class "card" để Popup tự đổi màu nền Đen/Trắng
        dialogPane.getStyleClass().add("card");
        dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Tiêu đề của Dialog
        Label lblTitle = new Label("HỢP ĐỒNG ĐĂNG BÁN SẢN PHẨM");
        lblTitle.getStyleClass().add("section-title");
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        lblTitle.setMaxWidth(Double.MAX_VALUE);
        lblTitle.setAlignment(javafx.geometry.Pos.CENTER);

        // Nội dung hợp đồng
        Label lblContent = new Label(getContractText());
        lblContent.setWrapText(true);
        lblContent.getStyleClass().add("row-text-normal");
        lblContent.setStyle("-fx-font-size: 14.5px; -fx-line-spacing: 6px; -fx-padding: 10 15 10 10;");
        lblContent.setMaxWidth(560);

        // Khung cuộn văn bản
        ScrollPane scrollPane = new ScrollPane(lblContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(600, 350);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("contract-scroll-pane");

        // Ép viền bo tròn trực tiếp phòng hờ CSS tải chậm
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-padding: 5;");

        // Checkbox xác nhận
        CheckBox chkPopupAgree = new CheckBox("Tôi xác nhận đã đọc và đồng ý với các điều khoản dịch vụ");
        chkPopupAgree.setStyle("-fx-font-size: 15px; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 0 0 0;");

        VBox dialogContent = new VBox(15);
        dialogContent.setStyle("-fx-padding: 20;");
        dialogContent.getChildren().addAll(lblTitle, scrollPane, chkPopupAgree);
        dialogPane.setContent(dialogContent);

        // 3. Khởi tạo cụm nút bấm
        ButtonType btnTypeAgree = new ButtonType("Đồng ý", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnTypeCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(btnTypeAgree, btnTypeCancel);

        Button btnAgree = (Button) dialogPane.lookupButton(btnTypeAgree);
        Button btnCancel = (Button) dialogPane.lookupButton(btnTypeCancel);

        // ========================================================
        // PHÉP THUẬT 2: BO TRÒN NÚT (VIÊN THUỐC) VÀ TÔ MÀU TRỰC TIẾP
        // ========================================================
        // Nút Hủy (Màu đỏ)
        btnCancel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14.5px; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;");

        // Style nút Đồng ý lúc CHƯA TÍCH (Màu xám)
        String disableStyle = "-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-size: 14.5px; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20;";
        // Style nút Đồng ý lúc ĐÃ TÍCH (Màu xanh dương)
        String enableStyle  = "-fx-background-color: #0A439D; -fx-text-fill: white; -fx-font-size: 14.5px; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;";

        btnAgree.setStyle(disableStyle);
        btnAgree.setDisable(true);

        // Đổi màu viên thuốc ngay khi ấn Checkbox
        chkPopupAgree.selectedProperty().addListener((observable, oldValue, newValue) -> {
            btnAgree.setDisable(!newValue);
            btnAgree.setStyle(newValue ? enableStyle : disableStyle);
        });

        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        // 4. Hiển thị Popup và xử lý kết quả
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

                // =========================================================
                // FIX 1: QUAN TRỌNG - THÊM TOKEN XÁC THỰC VÀO HEADER
                // =========================================================
                try {
                    if (SessionManager.token != null && !SessionManager.token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + SessionManager.token);
                    }
                } catch (Exception ignored) {}

                java.io.OutputStream out = conn.getOutputStream();
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(out, "UTF-8"), true);

                for (File file : selectedFiles) {
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"files\"; filename=\"").append(file.getName()).append("\"\r\n");

                    // =========================================================
                    // FIX 2: BẮT LỖI NULL CONTENT-TYPE CỦA WINDOWS
                    // =========================================================
                    String contentType = java.nio.file.Files.probeContentType(file.toPath());
                    if (contentType == null) {
                        contentType = "image/jpeg"; // Mặc định nếu HĐH không tự nhận diện được ảnh
                    }
                    writer.append("Content-Type: ").append(contentType).append("\r\n\r\n");
                    writer.flush();

                    // Copy file đẩy lên server
                    java.nio.file.Files.copy(file.toPath(), out);
                    out.flush();
                    writer.append("\r\n").flush();
                }
                writer.append("--").append(boundary).append("--\r\n").flush();

                // Đợi upload xong thì kiểm tra kết quả
                int code = conn.getResponseCode();
                System.out.println("API Upload Image Response Code: " + code);

                if (code >= 200 && code < 300) {
                    // THÀNH CÔNG -> Chuyển sang Bước C: Tạo phiên đấu giá
                    createAuctionForData(itemId, formattedStartTime, formattedEndTime);
                } else {
                    java.io.InputStream errStream = conn.getErrorStream();
                    String errorMessage = "Lỗi không xác định";
                    if (errStream != null) {
                        java.util.Scanner s = new java.util.Scanner(errStream).useDelimiter("\\A");
                        errorMessage = s.hasNext() ? s.next() : "";
                        System.out.println("Chi tiết lỗi từ Server: " + errorMessage);
                    }
                    final String errorTitle = "Mã lỗi: " + code;
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi Upload", "Không thể tải ảnh (" + errorTitle + ").\nSản phẩm đã tạo thành công nhưng không có ảnh."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Đứt mạng hoặc server từ chối kết nối khi tải ảnh."));
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
        return "ĐIỀU KHOẢN VÀ DỊCH VỤ DÀNH CHO NGƯỜI BÁN (SELLER AGREEMENT)\n\n"
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/MyAuctionList.fxml"));
            Node view = loader.load();
            StackPane contentArea = (StackPane) txtProductName.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg);
        com.auction.util.AlertUtils.applyStyle(alert);
        alert.showAndWait();
    }

    @FXML
    private void closeImageZoom() {
        paneImageZoom.setVisible(false);
        zoomedImageView.setImage(null); // Giải phóng RAM ảnh
    }
}