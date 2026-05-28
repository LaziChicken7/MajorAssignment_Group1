package com.auction.controller.profile;


import lombok.extern.slf4j.Slf4j;
import com.auction.controller.auction.AuctionDetailController;
import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.ReviewRequest;
import com.auction.model.SellerReviewModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
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

@Slf4j
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
        log.info("\u25B6 Controller Action - Execute: initialize()");
        cbStar.getItems().addAll("5 Sao - Tuyệt vời", "4 Sao - Rất tốt", "3 Sao - Tốt", "2 Sao - Trung bình", "1 Sao - Tệ");
    }

    public void setSellerData(AuctionModel.SellerModel seller, AuctionModel previousAuctionItem) {
        this.seller = seller;
        this.previousAuctionItem = previousAuctionItem;

        lblName.setText(seller.fullName != null ? seller.fullName : seller.userName);
        lblRating.setText(seller.rating + "/5.0");
        lblRating.setGraphic(createStar());

        if (seller.avatarUrl != null) {
            imgAvatar.setImage(new Image(ApiService.BASE_URL + seller.avatarUrl, true));
            Rectangle clip = new Rectangle(100, 100);
            clip.setArcWidth(100); clip.setArcHeight(100);
            imgAvatar.setClip(clip);
        }

        if (SessionManager.userName.equals(seller.userName)) {
            btnAddFriend.setVisible(false);
            btnAddFriend.setManaged(false);
        }

        loadReviews();
    }

    @FXML
    private void loadReviews() {
        log.info("\u25B6 Controller Action - Execute: loadReviews()");
        btnLoadMore.setText("Đang tải...");
        btnLoadMore.setDisable(true);

        ApiService.getAsync("/users/" + seller.userName + "/reviews?page=" + currentPage + "&size=5").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {

                        java.lang.reflect.Type listType = new TypeToken<List<SellerReviewModel>>(){}.getType();
                        List<SellerReviewModel> list = ApiService.gson.fromJson(apiRes.result, listType);

                        if (list == null || list.isEmpty()) {
                            if (currentPage == 0) {
                                Label emptyLabel = new Label("Chưa có đánh giá nào.");
                                emptyLabel.getStyleClass().add("muted-text");
                                emptyLabel.setStyle("-fx-font-size: 15px; -fx-font-style: italic;");
                                vboxReviews.getChildren().add(emptyLabel);
                            }
                            btnLoadMore.setVisible(false);
                        } else {
                            for (SellerReviewModel r : list) {
                                vboxReviews.getChildren().add(createReviewNode(r));
                            }
                            currentPage++;
                            btnLoadMore.setText("Tải thêm bình luận ▾");
                            btnLoadMore.setDisable(false);

                            if (list.size() < 5) btnLoadMore.setVisible(false);
                        }
                    }
                }
            });
        });
    }

    private Node createReviewNode(SellerReviewModel r) {
        VBox box = new VBox(5);
        box.getStyleClass().add("custom-row");
        box.setStyle("-fx-padding: 15;");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ImageView avt = new ImageView(new Image(ApiService.BASE_URL + r.reviewer.avatarUrl, true));
        avt.setFitWidth(40); avt.setFitHeight(40);
        Rectangle clip = new Rectangle(40, 40); clip.setArcWidth(40); clip.setArcHeight(40);
        avt.setClip(clip);

        VBox info = new VBox(2);
        Label name = new Label(r.reviewer.fullName != null ? r.reviewer.fullName : r.reviewer.userName);
        name.getStyleClass().add("row-title-bold");
        name.setStyle("-fx-font-size: 15px;");

        HBox starsBox = new HBox(3);
        for (int i = 0; i < r.star; i++) {
            starsBox.getChildren().add(createStar());
        }

        info.getChildren().addAll(name, starsBox);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label date = new Label(r.createdAt.replace("T", " ").substring(0, 16));
        date.getStyleClass().add("row-text-muted");

        header.getChildren().addAll(avt, info, spacer, date);

        Label cmt = new Label(r.comment);
        cmt.setWrapText(true);
        cmt.getStyleClass().add("row-text-normal");
        cmt.setStyle("-fx-padding: 10 0 0 0;");

        box.getChildren().addAll(header, cmt);
        return box;
    }

    @FXML
    private void handleSubmitReview() {
        log.info("\u25B6 Controller Action - Execute: handleSubmitReview()");
        if (cbStar.getValue() == null || txtComment.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn số sao và nhập bình luận!");
            return;
        }

        int star = Integer.parseInt(cbStar.getValue().substring(0, 1));
        ReviewRequest req = new ReviewRequest(SessionManager.userName, seller.userName, star, txtComment.getText().trim());

        ApiService.postAsync("/users/reviews", req).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi đánh giá thành công!");

                    vboxReviews.getChildren().clear();
                    currentPage = 0;
                    btnLoadMore.setVisible(true);
                    loadReviews();
                    txtComment.clear();
                    cbStar.getSelectionModel().clearSelection();

                    if (previousAuctionItem != null && previousAuctionItem.id != null) {
                        ApiService.getAsync("/auctions/" + previousAuctionItem.id).thenAccept(auctionRes -> {
                            Platform.runLater(() -> {
                                if (auctionRes.statusCode() == 200) {
                                    try {
                                        ApiResponse apiResponse = ApiService.gson.fromJson(auctionRes.body(), ApiResponse.class);
                                        if (apiResponse.code == 1000) {
                                            AuctionModel updatedAuction = ApiService.gson.fromJson(apiResponse.result, AuctionModel.class);
                                            if (updatedAuction.seller != null) {
                                                double newRating = updatedAuction.seller.rating;
                                                lblRating.setText(newRating + "/5.0");
                                                seller.rating = newRating;
                                                if (previousAuctionItem.seller != null) {
                                                    previousAuctionItem.seller.rating = newRating;
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.info("Lỗi khi fetch lại rating: " + e.getMessage());
                                    }
                                }
                            });
                        });
                    }

                } else {
                    try {
                        ApiResponse err = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        showAlert(Alert.AlertType.ERROR, "Lỗi", err.message);
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Server", "Không thể gửi đánh giá ngay lúc này.");
                    }
                }
            });
        });
    }

    @FXML
    private void goBack() {
        log.info("\u25B6 Controller Action - Execute: goBack()");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
            Node view = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(previousAuctionItem);

            StackPane contentArea = (StackPane) lblName.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { log.error("Exception occurred", e); }
    }

    private SVGPath createStar() {
        SVGPath star = new SVGPath();
        star.setContent("M 8 0 L 10.46 5.36 L 16 6.24 L 12 10.36 L 12.94 16 L 8 13.24 L 3.06 16 L 4 10.36 L 0 6.24 L 5.54 5.36 Z");
        star.setFill(Color.web("#f39c12"));
        star.setStroke(Color.web("#f39c12"));
        star.setStrokeWidth(1.5);
        star.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return star;
    }

    @FXML
    private void handleSendFriendRequest() {
        log.info("\u25B6 Controller Action - Execute: handleSendFriendRequest()");
        String url = "/chat/friend-request?sender=" + SessionManager.userName + "&receiver=" + seller.userName;

        ApiService.postAsync(url, null).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi lời mời kết bạn!");
                    btnAddFriend.setText("Đã gửi lời mời ✓");
                    btnAddFriend.setDisable(true);
                    btnAddFriend.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 20;");
                } else {
                    try {
                        ApiResponse err = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        showAlert(Alert.AlertType.ERROR, "Lỗi", err.message);
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Gặp sự cố khi gửi kết bạn!");
                    }
                }
            });
        });
    }

    // ==========================================
    // HÀM HIỂN THỊ THÔNG BÁO TÙY CHỈNH (CSS ALERT)
    // ==========================================
    private void showAlert(Alert.AlertType type, String title, String content) {
        log.info("\u25B6 Controller Action - Execute: showAlert()");
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        com.auction.util.AlertUtils.applyStyle(alert); // Gọi CSS Dark Mode & Nút viên thuốc
        alert.showAndWait();
    }
}