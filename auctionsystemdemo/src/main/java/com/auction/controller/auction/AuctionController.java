package com.auction.controller.auction;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuctionController {

    @FXML private ListView<AuctionModel> auctionListView;
    @FXML private Label lblBalance;
    @FXML private Label eyeIconText; // Nút Ẩn/Hiện
    @FXML private ComboBox<String> cbFilter;
    @FXML private ComboBox<String> cbSort;

    private List<AuctionModel> allAuctions = new ArrayList<>();

    // --- BIẾN TRẠNG THÁI ẨN/HIỆN TIỀN ---
    private String realBalanceText = "0 VND";
    private boolean isHidden = true; // Mặc định vào app là giấu tiền

    @FXML
    public void initialize() {
        cbFilter.setItems(FXCollections.observableArrayList(
                "Tất cả trạng thái",
                "Sắp diễn ra (OPEN)",
                "Đang diễn ra (RUNNING)",
                "Đã kết thúc (FINISHED)",
                "Đã thanh toán (PAID)",
                "Đã hủy (CANCELLED)"
        ));
        cbFilter.setValue("Đang diễn ra (RUNNING)");

        cbSort.setItems(FXCollections.observableArrayList(
                "Mặc định",
                "Kết thúc sớm nhất (Tăng dần)",
                "Kết thúc muộn nhất (Giảm dần)"
        ));
        cbSort.setValue("Kết thúc sớm nhất (Tăng dần)");

        cbFilter.setOnAction(e -> applyFilterAndSort());
        cbSort.setOnAction(e -> applyFilterAndSort());

        auctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            @Override
            protected void updateItem(AuctionModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionItem.fxml"));
                        setGraphic(loader.load());
                        AuctionItemController controller = loader.getController();
                        controller.setData(item);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        });

        auctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = auctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) showDetail(selected);
        });

        loadData();
    }

    @FXML
    public void loadData() {
        // 1. Lấy số dư ví (Có xử lý ẩn/hiện)
        if (SessionManager.userName != null) {
            ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            WalletDataResponse wallet = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);

                            // Lưu số tiền thật vào biến
                            realBalanceText = String.format("%,.0f VND", wallet.moneyOnWallet).replace(",", ".");

                            // Cập nhật giao diện tùy theo trạng thái đang khóa hay mở
                            if (isHidden) {
                                lblBalance.setText("****** VND");
                            } else {
                                lblBalance.setText(realBalanceText);
                            }
                        }
                    }
                });
            });
        }

        // 2. Lấy danh sách đấu giá
        ApiService.getAsync("/auctions").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                        allAuctions = ApiService.gson.fromJson(apiRes.result, listType);
                        applyFilterAndSort();
                    }
                }
            });
        });
    }

    // --- HÀM XỬ LÝ CLICK NÚT HIỆN/ẨN ---
    @FXML
    public void toggleBalanceVisibility() {
        isHidden = !isHidden; // Đảo trạng thái
        if (isHidden) {
            lblBalance.setText("****** VND");
            eyeIconText.setText("Hiện");
        } else {
            lblBalance.setText(realBalanceText);
            eyeIconText.setText("Ẩn");
        }
    }

    private void applyFilterAndSort() {
        if (allAuctions == null || allAuctions.isEmpty()) return;
        Stream<AuctionModel> stream = allAuctions.stream();

        String filterValue = cbFilter.getValue();
        if (filterValue != null && !filterValue.equals("Tất cả trạng thái")) {
            if (filterValue.contains("OPEN")) stream = stream.filter(a -> "OPEN".equals(a.status));
            else if (filterValue.contains("RUNNING")) stream = stream.filter(a -> "RUNNING".equals(a.status));
            else if (filterValue.contains("FINISHED")) stream = stream.filter(a -> "FINISHED".equals(a.status));
            else if (filterValue.contains("PAID")) stream = stream.filter(a -> "PAID".equals(a.status));
            else if (filterValue.contains("CANCELLED")) stream = stream.filter(a -> "CANCELLED".equals(a.status));
        }

        String sortValue = cbSort.getValue();
        if ("Kết thúc sớm nhất (Tăng dần)".equals(sortValue) || "Kết thúc muộn nhất (Giảm dần)".equals(sortValue)) {
            stream = stream.sorted((a1, a2) -> {
                try {
                    String timeStr1 = (a1.endTime != null) ? a1.endTime.replace(" ", "T") : "9999-12-31T23:59:59";
                    String timeStr2 = (a2.endTime != null) ? a2.endTime.replace(" ", "T") : "9999-12-31T23:59:59";
                    LocalDateTime t1 = LocalDateTime.parse(timeStr1);
                    LocalDateTime t2 = LocalDateTime.parse(timeStr2);
                    return "Kết thúc sớm nhất (Tăng dần)".equals(sortValue) ? t1.compareTo(t2) : t2.compareTo(t1);
                } catch (DateTimeParseException e) { return 0; }
            });
        }

        List<AuctionModel> filteredList = stream.collect(Collectors.toList());
        auctionListView.setItems(FXCollections.observableArrayList(filteredList));
    }

    private void showDetail(AuctionModel item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/controller/addauctionitem/AuctionDetail.fxml"));
            Node detailView = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(item);

            StackPane contentArea = (StackPane) auctionListView.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(detailView);
        } catch (IOException e) { e.printStackTrace(); }
    }
}