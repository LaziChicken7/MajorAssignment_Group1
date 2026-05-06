package com.auction.model;

public class ItemCreationRequest {
    public String sellerUserName;
    public String name;
    public String description;
    public double startPrice;
    public String itemType;

    // Các trường đặc thù (Tùy loại mà gán, không thì để null)
    public String nameAuthor;
    public Integer creationYear; // Dùng Integer thay vì int để có thể gửi null
    public String brand;
    public Integer warrantyMonths;
    public String engineType;
    public Integer mileage;
}