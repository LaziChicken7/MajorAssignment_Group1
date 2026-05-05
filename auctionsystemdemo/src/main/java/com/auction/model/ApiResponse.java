package com.auction.model;

import com.google.gson.JsonElement;

public class ApiResponse {
    public int code;
    public String message;
    // JsonElement giúp hứng mọi loại data (User, List, String...) linh hoạt
    public JsonElement result;
}