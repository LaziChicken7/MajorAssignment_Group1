package com.auction.util;

public class SessionManager {
    public static String userName;
    public static String fullName;
    public static String role;
    public static String token;
    public static boolean isDarkMode = false;

    public static void logout() {
        userName = null;
        fullName = null;
        role = null;
    }
}