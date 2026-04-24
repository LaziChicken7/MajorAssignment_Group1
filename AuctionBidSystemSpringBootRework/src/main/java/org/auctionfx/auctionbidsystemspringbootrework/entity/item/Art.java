package org.auctionfx.auctionbidsystemspringbootrework.entity.item;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "arts")
public class Art extends Item {
    private String nameAuthor;
    private int creationYear;

    // GETTER VÀ SETTER
    public String getNameAuthor() {
        return nameAuthor;
    }

    public void setNameAuthor(String nameAuthor) {
        this.nameAuthor = nameAuthor;
    }
}
