package com.auction.util;

public class SessionManager {
    public static String userName;
    public static String fullName;
    public static String role;
    public static String token;
    public static boolean isDarkMode = false;

    // THÊM BIẾN NÀY ĐỂ GHI NHỚ TRẠNG THÁI YÊU CẦU LÊN SELLER
    public static boolean isUpgradePending = false;

    public static void logout() {
        userName = null;
        fullName = null;
        role = null;
        token = null;
        isUpgradePending = false; // Reset khi đăng xuất
    }
}