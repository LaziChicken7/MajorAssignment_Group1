package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.model.WalletDataResponse;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    @FXML private ComboBox<String> cbFilter;
    @FXML private ComboBox<String> cbSort;

    // List gốc để lưu trữ toàn bộ dữ liệu tải về từ API
    private List<AuctionModel> allAuctions = new ArrayList<>();

    @FXML
    public void initialize() {
        // Cấu hình ComboBox Lọc (Khớp hoàn toàn với Enum AuctionStatus ở Backend)
        cbFilter.setItems(FXCollections.observableArrayList(
                "Tất cả trạng thái",
                "Sắp diễn ra (OPEN)",
                "Đang diễn ra (RUNNING)",
                "Đã kết thúc (FINISHED)",
                "Đã thanh toán (PAID)",
                "Đã hủy (CANCELLED)"
        ));
        cbFilter.setValue("Tất cả trạng thái");

        // Cấu hình ComboBox Sắp xếp
        cbSort.setItems(FXCollections.observableArrayList(
                "Mặc định",
                "Kết thúc sớm nhất (Tăng dần)",
                "Kết thúc muộn nhất (Giảm dần)"
        ));
        cbSort.setValue("Mặc định");

        // Bắt sự kiện khi người dùng thay đổi lựa chọn
        cbFilter.setOnAction(e -> applyFilterAndSort());
        cbSort.setOnAction(e -> applyFilterAndSort());

        // Cấu hình cách hiển thị từng dòng trong ListView
        auctionListView.setCellFactory(param -> new ListCell<AuctionModel>() {
            @Override
            protected void updateItem(AuctionModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AuctionItem.fxml"));
                        setGraphic(loader.load());
                        AuctionItemController controller = loader.getController();
                        controller.setData(item);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        });

        // Bắt sự kiện Click vào 1 sản phẩm
        auctionListView.setOnMouseClicked(event -> {
            AuctionModel selected = auctionListView.getSelectionModel().getSelectedItem();
            if (selected != null) showDetail(selected);
        });

        loadData();
    }

    @FXML
    public void loadData() {
        // 1. Load Số dư ví
        if (SessionManager.userName != null) {
            ApiService.getAsync("/payments/" + SessionManager.userName + "/history").thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                        if (apiRes.code == 1000) {
                            WalletDataResponse wallet = ApiService.gson.fromJson(apiRes.result, WalletDataResponse.class);
                            lblBalance.setText(String.format("%,.0f VND", wallet.moneyOnWallet).replace(",", "."));
                        }
                    }
                });
            });
        }

        // 2. Load Danh sách đấu giá
        ApiService.getAsync("/auctions").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<AuctionModel>>(){}.getType();
                        allAuctions = ApiService.gson.fromJson(apiRes.result, listType);

                        // Áp dụng bộ lọc và sắp xếp ngay sau khi load xong data
                        applyFilterAndSort();
                    }
                }
            });
        });
    }

    private void applyFilterAndSort() {
        if (allAuctions == null || allAuctions.isEmpty()) return;

        Stream<AuctionModel> stream = allAuctions.stream();

        // XỬ LÝ LỌC (Filter theo 5 trạng thái)
        String filterValue = cbFilter.getValue();
        if (filterValue != null && !filterValue.equals("Tất cả trạng thái")) {
            if (filterValue.contains("OPEN")) {
                stream = stream.filter(a -> "OPEN".equals(a.status));
            } else if (filterValue.contains("RUNNING")) {
                stream = stream.filter(a -> "RUNNING".equals(a.status));
            } else if (filterValue.contains("FINISHED")) {
                stream = stream.filter(a -> "FINISHED".equals(a.status));
            } else if (filterValue.contains("PAID")) {
                stream = stream.filter(a -> "PAID".equals(a.status));
            } else if (filterValue.contains("CANCELLED")) {
                stream = stream.filter(a -> "CANCELLED".equals(a.status));
            }
        }

        // XỬ LÝ SẮP XẾP (Sort)
        String sortValue = cbSort.getValue();
        if ("Kết thúc sớm nhất (Tăng dần)".equals(sortValue) || "Kết thúc muộn nhất (Giảm dần)".equals(sortValue)) {
            stream = stream.sorted((a1, a2) -> {
                try {
                    // Xử lý chuỗi thời gian an toàn
                    String timeStr1 = (a1.endTime != null) ? a1.endTime.replace(" ", "T") : "9999-12-31T23:59:59";
                    String timeStr2 = (a2.endTime != null) ? a2.endTime.replace(" ", "T") : "9999-12-31T23:59:59";

                    LocalDateTime t1 = LocalDateTime.parse(timeStr1);
                    LocalDateTime t2 = LocalDateTime.parse(timeStr2);

                    if ("Kết thúc sớm nhất (Tăng dần)".equals(sortValue)) {
                        return t1.compareTo(t2);
                    } else {
                        return t2.compareTo(t1);
                    }
                } catch (DateTimeParseException e) {
                    return 0; // Bỏ qua nếu parse lỗi
                }
            });
        }

        // Đẩy list đã xử lý vào giao diện
        List<AuctionModel> filteredList = stream.collect(Collectors.toList());
        ObservableList<AuctionModel> observableList = FXCollections.observableArrayList(filteredList);
        auctionListView.setItems(observableList);
    }

    private void showDetail(AuctionModel item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/AuctionDetail.fxml"));
            Node detailView = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(item);

            StackPane contentArea = (StackPane) auctionListView.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(detailView);
        } catch (IOException e) { e.printStackTrace(); }
    }
}