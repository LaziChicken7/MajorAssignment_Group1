package com.auction.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ItemCreationRequest {
    public String sellerUserName;
    public String name;
    public String description;
    public double startPrice;
    public String itemType;

    public List<String> imageUrls;

    // Các trường đặc thù (Tùy loại mà gán, không thì để null)
    @SerializedName("nameAuthor")
    public String nameAuthor;

    @SerializedName("creationYear")
    public Integer creationYear;

    // 2. Dành cho Đồ điện tử
    @SerializedName("brand")
    public String brand;

    @SerializedName("warrantyMonths")
    public Integer warrantyMonths;

    // 3. Dành cho Phương tiện
    @SerializedName("engineType")
    public String engineType;

    @SerializedName("mileage")
    public Integer mileage;
}