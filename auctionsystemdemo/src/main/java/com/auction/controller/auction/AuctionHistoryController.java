package com.auction.controller.auction;

import com.auction.model.ApiResponse;
import com.auction.model.AuctionModel;
import com.auction.util.ApiService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuctionHistoryController {

    @FXML private Label lblTitle;
    @FXML private TableView<AuctionModel.BidTransactionModel> tableHistory;
    @FXML private TableColumn<AuctionModel.BidTransactionModel, String> colStt;
    @FXML private TableColumn<AuctionModel.BidTransactionModel, String> colTime;
    @FXML private TableColumn<AuctionModel.BidTransactionModel, String> colUser;
    @FXML private TableColumn<AuctionModel.BidTransactionModel, String> colAmount;
    @FXML private Pagination pagination;

    private AuctionModel currentItem;
    private List<AuctionModel.BidTransactionModel> allTransactions = new ArrayList<>();

    private final int ROWS_PER_PAGE = 100; // Khống chế 100 dòng 1 trang để UI mượt mà

    @FXML
    public void initialize() {
        // Cài đặt dữ liệu cho các cột của Bảng
        colStt.setCellValueFactory(cellData -> {
            // Tính số thứ tự giảm dần (Người mới nhất là số 1)
            int index = allTransactions.indexOf(cellData.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });

        colTime.setCellValueFactory(cellData -> {
            String time = cellData.getValue().bidTimestamp;
            time = time.replace("T", " "); // Format lại ngày giờ
            // Bỏ đi phần thập phân của mili-giây nếu có (Ví dụ: 2024-10-25 15:30:00.123 -> 2024-10-25 15:30:00)
            if(time.contains(".")) time = time.substring(0, time.indexOf("."));
            return new SimpleStringProperty(time);
        });

        colUser.setCellValueFactory(cellData -> {
            String user = "Ẩn danh";
            if (cellData.getValue().bidder != null && cellData.getValue().bidder.userName != null) {
                user = cellData.getValue().bidder.userName;
            }
            return new SimpleStringProperty(user);
        });

        colAmount.setCellValueFactory(cellData -> {
            String amount = String.format("%,.0f", cellData.getValue().bidAmount).replace(",", ".");
            return new SimpleStringProperty(amount);
        });

        // Khi người dùng bấm chuyển trang (Trang 1, 2, 3...)
        pagination.setPageFactory(this::createPage);
    }

    public void setAuctionData(AuctionModel item) {
        this.currentItem = item;
        lblTitle.setText("Tất cả lịch sử - " + item.bidProduct.name);
        fetchData();
    }

    @FXML
    private void refreshData() {
        fetchData();
    }

    private void fetchData() {
        ApiService.getAsync("/auctions/" + currentItem.id + "/price-chart").thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<AuctionModel.BidTransactionModel>>(){}.getType();
                        List<AuctionModel.BidTransactionModel> txs = ApiService.gson.fromJson(apiRes.result, listType);

                        if (txs != null) {
                            // BỔ SUNG: Sắp xếp mảng theo giá GIẢM DẦN (Mức giá cao nhất / Mới nhất sẽ nằm trên cùng)
                            // Loại bỏ triệt để lỗi lộn xộn thứ tự do Bot đánh nhau cùng 1 mili-giây
                            txs.sort((t1, t2) -> Double.compare(t2.bidAmount, t1.bidAmount));

                            allTransactions = txs;

                            // Tính toán số lượng trang
                            int pageCount = (int) Math.ceil((double) allTransactions.size() / ROWS_PER_PAGE);
                            pagination.setPageCount(pageCount == 0 ? 1 : pageCount);
                            pagination.setCurrentPageIndex(0);

                            // Tạo nội dung trang 1
                            createPage(0);
                        }
                    }
                }
            });
        });
    }

    // Logic cắt List để nạp vào TableView theo Trang
    private Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allTransactions.size());

        if (fromIndex <= toIndex && !allTransactions.isEmpty()) {
            tableHistory.setItems(FXCollections.observableArrayList(allTransactions.subList(fromIndex, toIndex)));
        } else {
            tableHistory.setItems(FXCollections.observableArrayList());
        }

        return new VBox(); // Pagination yêu cầu trả về Node, ta trả VBox rỗng vì TableView nằm ngoài Pagination rồi
    }

    // =====================================
    // NÚT BACK VỀ TRANG CHI TIẾT
    // =====================================
    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/auction/AuctionDetail.fxml"));
            Node view = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(currentItem);

            StackPane contentArea = (StackPane) lblTitle.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}