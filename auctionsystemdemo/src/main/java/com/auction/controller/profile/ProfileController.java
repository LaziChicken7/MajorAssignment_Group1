package com.auction.controller.profile;

import com.auction.controller.dashboard.MainController;
import com.auction.model.ApiResponse;
import com.auction.model.UserProfile;
import com.auction.model.UserUpdateRequest;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Modality;
import javax.imageio.ImageIO;

public class ProfileController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtCitizenId;
    @FXML private PasswordField txtPassword; // Nơi nhập mật khẩu mới

    @FXML private Button btnAdminControl;
    @FXML private ImageView imgAvatar;
    @FXML private Label lblAvatarPlaceholder;

    @FXML
    public void initialize() {
        loadUserData();

        // KIỂM TRA QUYỀN ADMIN ĐỂ HIỆN NÚT
        if ("ADMIN".equals(SessionManager.role)) {
            btnAdminControl.setVisible(true);
            btnAdminControl.setManaged(true);
        }
    }

    // THÊM HÀM CHUYỂN TRANG ADMIN NÀY:
    @FXML
    public void handleOpenAdminControl(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/dashboard/AdminDashboard.fxml"));
            javafx.scene.Node view = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) txtUsername.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==========================================
    // 1. HÀM LẤY DỮ LIỆU TỪ SERVER ĐỔ VÀO UI
    // ==========================================
    private void loadUserData() {
        String currentUser = SessionManager.userName;
        if (currentUser == null) return;

        // Gọi API GET lấy thông tin profile
        ApiService.getAsync("/users/profile/" + currentUser)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                UserProfile profile = ApiService.gson.fromJson(apiResponse.result, UserProfile.class);

                                txtUsername.setText(profile.userName);
                                txtFullName.setText(profile.fullName);
                                txtEmail.setText(profile.email);
                                txtPhone.setText(profile.numberPhone);
                                txtCitizenId.setText(profile.citizenId);

                                // --- HIỂN THỊ AVATAR ---
                                String avatarPath = profile.avatarUrl;
                                if (avatarPath == null || !avatarPath.startsWith("/uploads") || avatarPath.contains("default-")) {
                                    avatarPath = "/uploads/images/avatar/avatarmacdinh.png";
                                }

                                // 1. SỬ DỤNG ApiService.BASE_URL (Đừng dùng localhost)
                                // 2. Thêm "?t=" + System.currentTimeMillis() để ép JavaFX phải tải lại ảnh mới nhất từ Server, không được dùng Cache
                                String fullImageUrl = ApiService.BASE_URL + avatarPath + "?t=" + System.currentTimeMillis();

                                // Gọi hàm bo tròn ảnh
                                displayCircularAvatar(fullImageUrl);

                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu", apiResponse.message);
                            }
                        } else {
                            System.out.println("Lỗi load dữ liệu: " + response.body());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ."));
                    return null;
                });
    }

    // ==========================================
    // 2. HÀM LƯU THÔNG TIN (UPDATE PROFILE)
    // ==========================================
    @FXML
    public void handleUpdateProfile(ActionEvent event) {
        String currentUser = SessionManager.userName;
        if (currentUser == null) return;

        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String newPassword = txtPassword.getText();

        if (fullName.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Họ tên và Email không được để trống!");
            return;
        }

        UserUpdateRequest request = new UserUpdateRequest();
        request.fullName = fullName;
        request.email = email;
        request.numberPhone = phone;
        request.password = newPassword.isEmpty() ? null : newPassword;

        ApiService.putAsync("/users/profile/" + currentUser, request)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            ApiResponse apiResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                            if (apiResponse.code == 1000) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật thông tin thành công!");
                                SessionManager.fullName = fullName;
                                txtPassword.clear();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", apiResponse.message);
                            }
                        } else {
                            try {
                                ApiResponse errResponse = ApiService.gson.fromJson(response.body(), ApiResponse.class);
                                showAlert(Alert.AlertType.ERROR, "Lỗi xác thực", errResponse.message);
                            } catch (Exception e) {
                                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Mã lỗi: " + response.statusCode());
                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Mất kết nối", "Không thể gọi tới máy chủ."));
                    return null;
                });
    }

    // ==========================================
    // 3. HÀM ĐĂNG XUẤT
    // ==========================================
    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/view/dashboard/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Không thể mở trang đăng nhập!");
        }
    }

    // ==========================================
    // 4. HÀM TIỆN ÍCH (HIỂN THỊ THÔNG BÁO)
    // ==========================================
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ==========================================
    // 5. MỞ HỘP THOẠI CHỌN FILE
    // ==========================================
    @FXML
    public void handleUploadAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            openCropDialog(selectedFile, stage);
        }
    }

    // ==========================================
    // 6. TẠO CỬA SỔ DI CHUYỂN, PHÓNG TO VÀ CẮT ẢNH
    // ==========================================
    private void openCropDialog(File imageFile, Stage parentStage) {
        Stage cropStage = new Stage();
        cropStage.initOwner(parentStage);
        cropStage.initModality(Modality.APPLICATION_MODAL);
        cropStage.setTitle("Chỉnh sửa ảnh đại diện");

        double cropSize = 300;

        Image originalImage = new Image(imageFile.toURI().toString());
        ImageView imageView = new ImageView(originalImage);
        imageView.setPreserveRatio(true);

        double imgW = originalImage.getWidth();
        double imgH = originalImage.getHeight();
        double scale = (imgW < imgH) ? (cropSize / imgW) : (cropSize / imgH);

        double actualW = imgW * scale;
        double actualH = imgH * scale;

        imageView.setFitWidth(actualW);
        imageView.setFitHeight(actualH);

        imageView.setTranslateX(-(actualW - cropSize) / 2);
        imageView.setTranslateY(-(actualH - cropSize) / 2);

        Pane imageContainer = new Pane(imageView);
        imageContainer.setMinSize(cropSize, cropSize);
        imageContainer.setMaxSize(cropSize, cropSize);
        imageContainer.setClip(new Rectangle(cropSize, cropSize));

        Runnable clampPosition = () -> {
            double currentW = actualW * imageView.getScaleX();
            double currentH = actualH * imageView.getScaleY();

            double minX = cropSize - (actualW + currentW) / 2;
            double maxX = (currentW - actualW) / 2;
            double minY = cropSize - (actualH + currentH) / 2;
            double maxY = (currentH - actualH) / 2;

            double tx = imageView.getTranslateX();
            double ty = imageView.getTranslateY();

            if (tx > maxX) imageView.setTranslateX(maxX);
            else if (tx < minX) imageView.setTranslateX(minX);

            if (ty > maxY) imageView.setTranslateY(maxY);
            else if (ty < minY) imageView.setTranslateY(minY);
        };

        double[] mouseLastPos = new double[2];
        imageView.setOnMousePressed((MouseEvent e) -> {
            mouseLastPos[0] = e.getSceneX();
            mouseLastPos[1] = e.getSceneY();
        });

        imageView.setOnMouseDragged((MouseEvent e) -> {
            double deltaX = e.getSceneX() - mouseLastPos[0];
            double deltaY = e.getSceneY() - mouseLastPos[1];

            imageView.setTranslateX(imageView.getTranslateX() + deltaX);
            imageView.setTranslateY(imageView.getTranslateY() + deltaY);

            clampPosition.run();

            mouseLastPos[0] = e.getSceneX();
            mouseLastPos[1] = e.getSceneY();
        });

        imageView.setOnScroll((ScrollEvent e) -> {
            double zoomFactor = 1.05;
            if (e.getDeltaY() < 0) {
                zoomFactor = 1 / zoomFactor;
            }
            double newScale = imageView.getScaleX() * zoomFactor;

            if (newScale < 1.0) newScale = 1.0;

            imageView.setScaleX(newScale);
            imageView.setScaleY(newScale);

            clampPosition.run();
        });

        Rectangle darkBackground = new Rectangle(cropSize, cropSize);
        Circle transparentHole = new Circle(cropSize / 2, cropSize / 2, cropSize / 2);
        Shape mask = Shape.subtract(darkBackground, transparentHole);
        mask.setFill(Color.rgb(0, 0, 0, 0.5));
        mask.setMouseTransparent(true);

        StackPane cropArea = new StackPane(imageContainer, mask);
        cropArea.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-background-color: white;");

        String pillButtonStyle = "-fx-background-radius: 20; -fx-padding: 8 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;";

        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #333333; " + pillButtonStyle);

        Button btnCrop = new Button("Cắt & Tải lên");
        btnCrop.setStyle("-fx-background-color: #0A439D; -fx-text-fill: white; " + pillButtonStyle);

        btnCrop.setOnAction(e -> {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage snapshot = imageContainer.snapshot(params, null);

            try {
                File tempFile = File.createTempFile("avatar_cropped", ".png");
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", tempFile);
                uploadFileToServer(tempFile);
                cropStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu ảnh đã cắt!");
            }
        });

        btnCancel.setOnAction(e -> cropStage.close());

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(15, btnCancel, btnCrop);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20,
                new Label("Di chuyển và lăn chuột để canh chỉnh ảnh"),
                cropArea,
                buttonBox
        );
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(25));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Scene scene = new Scene(root, 400, 480);
        cropStage.setScene(scene);
        cropStage.showAndWait();
    }

    // ==========================================
    // 7. UPLOAD FILE LÊN SERVER
    // ==========================================
    private void uploadFileToServer(File file) {
        String boundary = "===" + System.currentTimeMillis() + "===";
        // Sửa urlString để nó linh động lấy IP từ ApiService
        String urlString = ApiService.BASE_URL + "/users/" + SessionManager.userName + "/avatar";

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream outputStream = conn.getOutputStream();
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");

                    String contentType = Files.probeContentType(file.toPath());
                    if (contentType == null) contentType = "image/png";
                    writer.append("Content-Type: ").append(contentType).append("\r\n\r\n");
                    writer.flush();

                    Files.copy(file.toPath(), outputStream);
                    outputStream.flush();

                    writer.append("\r\n").flush();
                    writer.append("--").append(boundary).append("--\r\n").flush();
                }

                int responseCode = conn.getResponseCode();

                // -----------------------------------------------------
                // MỚI: ĐỌC LỖI TỪ SERVER NẾU UPLOAD THẤT BẠI
                // -----------------------------------------------------
                String serverErrorMessage = "Không thể tải ảnh. Mã lỗi HTTP: " + responseCode;
                if (responseCode >= 400) {
                    try (java.io.InputStream errorStream = conn.getErrorStream()) {
                        if (errorStream != null) {
                            String responseBody = new String(errorStream.readAllBytes(), "UTF-8");
                            try {
                                // Cố gắng ép kiểu JSON về ApiResponse để lấy message
                                ApiResponse errRes = ApiService.gson.fromJson(responseBody, ApiResponse.class);
                                if (errRes != null && errRes.message != null) {
                                    serverErrorMessage = errRes.message;
                                } else {
                                    serverErrorMessage = responseBody; // Nếu không có message, in toàn bộ text
                                }
                            } catch (Exception parseEx) {
                                // Nếu server không trả về JSON mà trả về text/html
                                serverErrorMessage = responseBody;
                            }
                        }
                    } catch (Exception ignored) { }
                }

                final String finalErrorMsg = serverErrorMessage;

                Platform.runLater(() -> {
                    if (responseCode == 200 || responseCode == 201) {

                        // 1. GỌI HÀM CẬP NHẬT GIAO DIỆN TRƯỚC!!!
                        loadUserData();
                        if (MainController.getInstance() != null) {
                            MainController.getInstance().loadUserInfo();
                        }

                        // 2. HIỆN THÔNG BÁO SAU
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tải ảnh lên thành công!");

                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi tải ảnh", finalErrorMsg);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace(); // In ra console cho lập trình viên (Terminal)

                // Lấy thông điệp lỗi cụ thể từ Java
                String detailedError = e.getMessage() != null ? e.getMessage() : e.toString();

                Platform.runLater(() -> {
                    // Mở rộng Alert để có thể hiển thị văn bản dài
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi hệ thống");
                    alert.setHeaderText("Không thể gửi dữ liệu lên máy chủ.");

                    // Tạo một TextArea để hiển thị chi tiết lỗi (Phòng trường hợp lỗi quá dài)
                    TextArea textArea = new TextArea(detailedError);
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setMaxWidth(Double.MAX_VALUE);
                    textArea.setMaxHeight(Double.MAX_VALUE);

                    // Thiết lập giao diện để nó có thể tự co giãn
                    javafx.scene.layout.GridPane.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
                    javafx.scene.layout.GridPane.setHgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
                    javafx.scene.layout.GridPane expContent = new javafx.scene.layout.GridPane();
                    expContent.setMaxWidth(Double.MAX_VALUE);
                    expContent.add(new Label("Chi tiết lỗi:"), 0, 0);
                    expContent.add(textArea, 0, 1);

                    // Gắn vào phần mở rộng (Show Details) của thông báo
                    alert.getDialogPane().setExpandableContent(expContent);
                    alert.getDialogPane().setExpanded(true); // Tự động mở rộng sẵn

                    alert.showAndWait();
                });
            }
        }).start();
    }

    // ==========================================
    // 8. HÀM HIỂN THỊ VÀ BO TRÒN ẢNH VÀO KHUNG
    // ==========================================
    private void displayCircularAvatar(String imageUrl) {
        // Tham số true, true ở cuối giúp ảnh load mượt mà dưới nền, không làm giật ứng dụng
        Image image = new Image(imageUrl, 200, 200, true, true, true);

        imgAvatar.setImage(image);

        // Bo tròn khung ảnh
        Circle clip = new Circle(100, 100, 100);
        imgAvatar.setClip(clip);

        // Ẩn chữ icon 👤 đi khi đã có ảnh
        if (lblAvatarPlaceholder != null) {
            lblAvatarPlaceholder.setVisible(false);
        }
    }
}