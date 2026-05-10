package com.auction.model;

import java.util.List;

public class ItemCreationRequest {
    public String sellerUserName;
    public String name;
    public String description;
    public double startPrice;
    public String itemType;

    public List<String> imageUrls;

    // Các trường đặc thù (Tùy loại mà gán, không thì để null)
    public String nameAuthor;
    public Integer creationYear; // Dùng Integer thay vì int để có thể gửi null
    public String brand;
    public Integer warrantyMonths;
    public String engineType;
    public Integer mileage;
}