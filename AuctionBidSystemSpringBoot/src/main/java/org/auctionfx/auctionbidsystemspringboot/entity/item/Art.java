package org.auctionfx.auctionbidsystemspringboot.entity.item;

import java.math.BigDecimal;

public class Art extends Item{
    private String nameAuthor;
    private int creationYear;

    public Art(String id, String name, String description, BigDecimal startPrice, String nameAuthor, int creationYear) {
        super(id, name, description, startPrice);
        this.nameAuthor = nameAuthor;
        this.creationYear = creationYear;
    }

    // GET & SET _ ĐỂ LẤY THUỘC TÍNH PRIVATE
    public String getNameAuthor() {
        return nameAuthor;
    }
    public void setNameAuthor(String nameAuthor) {
        this.nameAuthor = nameAuthor;
    }
    public int getCreationYear() {
        return creationYear;
    }
    public void setCreationYear(int creationYear) {
        this.creationYear = creationYear;
    }
}
