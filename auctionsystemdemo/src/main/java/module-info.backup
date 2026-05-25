module com.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Thư viện gọi API và ép kiểu JSON
    requires java.net.http;
    requires com.google.gson;
    requires javafx.swing;

    // =======================================================
    // BỔ SUNG: Thư viện WebSocket để chạy tính năng Chat Real-time
    // =======================================================
    requires spring.messaging;
    requires spring.websocket;
    requires jakarta.websocket.client;
    requires spring.core;
    requires com.fasterxml.jackson.databind;
    
    // add : Khắc phục lỗi thiếu SmartLifecycle và HttpHeaders
    requires spring.context;
    requires spring.web;
    requires spring.beans;


    // Cho phép JavaFX truy cập vào thư mục view và root để load giao diện
    opens com.auction to javafx.fxml, javafx.graphics;
    opens com.auction.view to javafx.fxml;

    // Cho phép Gson và Jackson soi vào các file Model để gán data JSON
    // thêm com.fasterxml.jackson.databind để sửa lỗi WebSocket
    opens com.auction.model to com.google.gson, com.fasterxml.jackson.databind;
    exports com.auction.model; // Export để các class khác có thể dùng

    // Export các package tiện ích và gốc
    exports com.auction;
    exports com.auction.util;
    opens com.auction.util to com.google.gson, com.fasterxml.jackson.databind;

    // Khai báo chính xác từng package con có chứa Controller
    exports com.auction.controller.addauctionitem;
    opens com.auction.controller.addauctionitem to javafx.fxml, spring.core;

    exports com.auction.controller.notification;
    opens com.auction.controller.notification to javafx.fxml, spring.core;

    exports com.auction.controller.wallet;
    opens com.auction.controller.wallet to javafx.fxml, spring.core;

    exports com.auction.controller.profile;
    opens com.auction.controller.profile to javafx.fxml, spring.core;

    exports com.auction.controller.home;
    opens com.auction.controller.home to javafx.fxml, spring.core;

    exports com.auction.controller.auction;
    opens com.auction.controller.auction to javafx.fxml, spring.core;

    exports com.auction.controller.dashboard;
    opens com.auction.controller.dashboard to javafx.fxml, spring.core;

    // Cho phép JavaFX truy cập vào package Chat mới tạo

    exports com.auction.controller.chat;
    opens com.auction.controller.chat to javafx.fxml, spring.core;

    exports com.auction.controller.search;
    opens com.auction.controller.search to javafx.fxml, spring.core;
}