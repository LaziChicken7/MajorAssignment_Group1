package com.auction.controller.profile;

import com.auction.controller.auction.AuctionDetailController;
import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.ReviewRequest;
import com.auction.model.SellerReviewModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.paint.Color;

import java.util.List;

public class SellerProfileController {

    @FXML private ImageView imgAvatar;
    @FXML private Label lblName, lblRating;
    @FXML private ComboBox<String> cbStar;
    @FXML private TextArea txtComment;
    @FXML private VBox vboxReviews;
    @FXML private Button btnLoadMore;
    @FXML private Button btnAddFriend;

    private AuctionModel.SellerModel seller;
    private AuctionModel previousAuctionItem; // Lưu lại để quay về trang cũ

    private int currentPage = 0; // Đếm số trang để Load More

    @FXML
    public void initialize() {
        // Thay emoji rườm rà bằng text chuyên nghiệp
        cbStar.getItems().addAll("5 Sao - Tuyệt vời", "4 Sao - Rất tốt", "3 Sao - Tốt", "2 Sao - Trung bình", "1 Sao - Tệ");
    }

    public void setSellerData(AuctionModel.SellerModel seller, AuctionModel previousAuctionItem) {
        this.seller = seller;
        this.previousAuctionItem = previousAuctionItem;

        lblName.setText(seller.fullName != null ? seller.fullName : seller.userName);
        lblRating.setText(seller.rating + "/5.0");
        lblRating.setGraphic(createStar()); // Gắn ngôi sao xịn vào

        if (seller.avatarUrl != null) {
            imgAvatar.setImage(new Image(ApiService.BASE_URL + seller.avatarUrl, true));
            Rectangle clip = new Rectangle(100, 100);
            clip.setArcWidth(100); clip.setArcHeight(100);
            imgAvatar.setClip(clip);
        }

        // Nếu đang xem profile của chính mình thì ẨN nút Kết bạn đi
        if (SessionManager.userName.equals(seller.userName)) {
            btnAddFriend.setVisible(false);
            btnAddFriend.setManaged(false);
        }

        // Tải 5 bình luận đầu tiên
        loadReviews();
    }

    @FXML
    private void loadReviews() {
        btnLoadMore.setText("Đang tải...");
        btnLoadMore.setDisable(true);

        ApiService.getAsync("/users/" + seller.userName + "/reviews?page=" + currentPage + "&size=5").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        // Spring Boot trả về dạng Page<SellerReview>, data nằm trong mảng "content"
                        JsonObject pageObject = apiRes.result.getAsJsonObject();
                        JsonElement contentArray = pageObject.get("content");

                        java.lang.reflect.Type listType = new TypeToken<List<SellerReviewModel>>(){}.getType();
                        List<SellerReviewModel> list = ApiService.gson.fromJson(contentArray, listType);

                        if (list.isEmpty()) {
                            if (currentPage == 0) vboxReviews.getChildren().add(new Label("Chưa có đánh giá nào."));
                            btnLoadMore.setVisible(false); // Hết dữ liệu thì ẩn nút đi
                        } else {
                            for (SellerReviewModel r : list) {
                                vboxReviews.getChildren().add(createReviewNode(r));
                            }
                            currentPage++; // Tăng trang cho lần click sau
                            btnLoadMore.setText("Tải thêm bình luận ▾");
                            btnLoadMore.setDisable(false);

                            // Nếu list trả về < 5, nghĩa là đã đến trang cuối cùng
                            if (list.size() < 5) btnLoadMore.setVisible(false);
                        }
                    }
                }
            });
        });
    }

    private Node createReviewNode(SellerReviewModel r) {
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 10;");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + r.reviewer.avatarUrl, true));
        avt.setFitWidth(40); avt.setFitHeight(40);
        Rectangle clip = new Rectangle(40, 40); clip.setArcWidth(40); clip.setArcHeight(40);
        avt.setClip(clip);

        VBox info = new VBox(2);
        Label name = new Label(r.reviewer.fullName != null ? r.reviewer.fullName : r.reviewer.userName);
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        HBox starsBox = new HBox(3); // Khoảng cách giữa các sao là 3px
        for (int i = 0; i < r.star; i++) {
            starsBox.getChildren().add(createStar());
        }

        info.getChildren().addAll(name, starsBox); // Nhét khối sao vào Info

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label date = new Label(r.createdAt.replace("T", " ").substring(0, 16));
        date.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");

        header.getChildren().addAll(avt, info, spacer, date);

        Label cmt = new Label(r.comment);
        cmt.setWrapText(true);
        cmt.setStyle("-fx-text-fill: #34495e; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");

        box.getChildren().addAll(header, cmt);
        return box;
    }

    @FXML
    private void handleSubmitReview() {
        if (cbStar.getValue() == null || txtComment.getText().trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng chọn số sao và nhập bình luận!").show();
            return;
        }

        // Cắt lấy ký tự đầu tiên của "5 Sao - Tuyệt vời" rồi biến thành số Integer
        int star = Integer.parseInt(cbStar.getValue().substring(0, 1));
        ReviewRequest req = new ReviewRequest(SessionManager.userName, seller.userName, star, txtComment.getText().trim());

        ApiService.postAsync("/users/reviews", req).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    new Alert(Alert.AlertType.INFORMATION, "Đã gửi đánh giá thành công!").show();
                    // Reset lại danh sách để hiện bình luận vừa đăng
                    vboxReviews.getChildren().clear();
                    currentPage = 0;
                    btnLoadMore.setVisible(true);
                    loadReviews();
                    txtComment.clear();
                    cbStar.getSelectionModel().clearSelection();
                } else {
                    try {
                        ApiResponse err = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        new Alert(Alert.AlertType.ERROR, err.message).show();
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "Lỗi Server").show();
                    }
                }
            });
        });
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
            Node view = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(previousAuctionItem); // Ném ngược dữ liệu về trang cũ

            StackPane contentArea = (StackPane) lblName.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Hàm vẽ Ngôi sao Vector bo tròn siêu mượt
    private SVGPath createStar() {
        SVGPath star = new SVGPath();
        // Tọa độ hình ngôi sao chuẩn 16x16
        star.setContent("M 8 0 L 10.46 5.36 L 16 6.24 L 12 10.36 L 12.94 16 L 8 13.24 L 3.06 16 L 4 10.36 L 0 6.24 L 5.54 5.36 Z");
        star.setFill(Color.web("#f39c12")); // Tô màu cam vàng
        star.setStroke(Color.web("#f39c12")); // Viền màu cam vàng
        star.setStrokeWidth(1.5);
        star.setStrokeLineJoin(StrokeLineJoin.ROUND); // ĐÂY LÀ PHÉP THUẬT: Tự động bo tròn 5 góc nhọn của ngôi sao
        return star;
    }

    // THÊM HÀM NÀY VÀO CUỐI FILE ĐỂ GỌI API KẾT BẠN
    @FXML
    private void handleSendFriendRequest() {
        // Gọi API POST /chat/friend-request?sender=...&receiver=...
        String url = "/chat/friend-request?sender=" + SessionManager.userName + "&receiver=" + seller.userName;

        ApiService.postAsync(url, null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    new Alert(Alert.AlertType.INFORMATION, "Đã gửi lời mời kết bạn!").show();
                    btnAddFriend.setText("Đã gửi lời mời ✓");
                    btnAddFriend.setDisable(true);
                    btnAddFriend.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 20;");
                } else {
                    try {
                        com.auction.model.ApiResponse err = ApiService.gson.fromJson(res.body(), com.auction.model.ApiResponse.class);
                        new Alert(Alert.AlertType.ERROR, err.message).show();
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "Lỗi khi gửi kết bạn!").show();
                    }
                }
            });
        });
    }
}