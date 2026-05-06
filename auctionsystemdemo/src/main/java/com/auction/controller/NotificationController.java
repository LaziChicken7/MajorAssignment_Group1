package com.auction.controller;

import com.auction.model.ApiResponse;
import com.auction.model.NotificationModel;
import com.auction.util.ApiService;
import com.auction.util.SessionManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class NotificationController {

    @FXML private ListView<NotificationModel> lvNotifications;

    @FXML
    public void initialize() {
        lvNotifications.setCellFactory(param -> new ListCell<NotificationModel>() {
            @Override
            protected void updateItem(NotificationModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/view/NotificationItem.fxml"));
                        setGraphic(loader.load());

                        NotificationItemController controller = loader.getController();
                        // Truyền dữ liệu và truyền cả Hàm loadData() để Item có thể gọi Load lại trang
                        controller.setData(item, () -> loadData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        loadData();
    }

    @FXML
    public void loadData() {
        if (SessionManager.userName == null) return;

        ApiService.getAsync("/notifications/" + SessionManager.userName).thenAccept(res -> {
            Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    ApiResponse apiRes = ApiService.gson.fromJson(res.body(), ApiResponse.class);
                    if (apiRes.code == 1000) {
                        Type listType = new TypeToken<List<NotificationModel>>(){}.getType();
                        List<NotificationModel> list = ApiService.gson.fromJson(apiRes.result, listType);

                        ObservableList<NotificationModel> observableList = FXCollections.observableArrayList(list);
                        lvNotifications.setItems(observableList);
                    }
                }
            });
        });
    }
}