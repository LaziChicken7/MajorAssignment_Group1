module com.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // Cấp quyền cho JavaFX truy cập vào package chính
    opens com.auction to javafx.fxml;
    exports com.auction;

    // Cấp quyền khởi tạo và ánh xạ FXML cho thư mục controller
    opens com.auction.controller to javafx.fxml;
    exports com.auction.controller;

    // (Tuỳ chọn thêm) Cấp quyền cho thư mục model để sau này bạn dùng TableView (Bảng) không bị lỗi hiển thị
    opens com.auction.model to javafx.base;
    exports com.auction.model;
    exports com.auction.service;
    opens com.auction.service to javafx.base;
}