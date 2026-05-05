package com.auction.util;

public class SessionManager {
    public static String userName;
    public static String fullName;
    public static String role;

    public static void logout() {
        userName = null;
        fullName = null;
        role = null;
    }
}