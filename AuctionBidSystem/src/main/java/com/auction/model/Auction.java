package com.auction.model;

//TOÀN BỘ CÁC HÀNH ĐỘNG ĐẤU GIÁ
public class Auction extends Entity {
    //KHAI BÁO ĐỐI TƯỢNG
    protected static long transactionCounter = 0;
    protected final Bidder bidUser;
    protected final Item bidProduct;
    protected final double originalPrice;
    protected final double currentPrice;
    protected boolean validAuction;
    public Auction(Bidder bidUser, Item bidProduct, double originalPrice, double currentPrice) throws Exception {
        super("TRS" + (++transactionCounter));
        this.bidUser = bidUser;
        this.bidProduct = bidProduct;
        this.originalPrice = originalPrice;
        this.currentPrice = currentPrice;
        this.validAuction = false;
    }

    //LẤY THUỘC TÍNH

    public String getUserName() { return bidUser.userName; }
    
    public double getOriginalPrice() { return originalPrice; }
    
    public double getCurrentPrice() { return currentPrice; }

    //KIỂM TRA TRANSACTION HỢP LỆ KHÔNG?

    public boolean checkTransaction(Auction previousAuction) {
        if (previousAuction.currentPrice < this.currentPrice) {
            System.err.println("Giao dịch hợp lệ!");
            return true;
        } else {
            System.err.println("Giao dịch không hợp lệ!");
            return false;
        }
        //Check Database/DAO xem có sản phẩm hợp lệ không, username có hợp lệ không
        /*
        Các trường hợp không hợp lệ:
        1. Giá tiền hiện tại thấp hơn giá tiền trước đó đấu giá
        2. Thời gian của người trước trùng với người sau (cái này cập nhật sau vậy)
        3. Đấu giá khi phiên đã đóng
        4. Sản phẩm (productID) không có trong database
        5. UserName của người đăng (Seller) trùng với UserName của người bid
        ... (còn nữa nhưng chưa nhớ ra)
        */
    }

    //HÀNH ĐỘNG SAU KHI TRANSACTION HỢP LỆ (TRỪ TIỀN NGƯỜI ĐẤU GIÁ ĐỂ NGÂM VÀO SẢN PHẨM)
    
    public void validTransaction(Auction previousAuction) {
        this.validAuction = true;
        bidUser.moneyOnWallet -= currentPrice;
        bidUser.moneyinFrozen += currentPrice;
        if (previousAuction.validAuction) {
            previousAuction.bidUser.moneyOnWallet += previousAuction.currentPrice;
            previousAuction.bidUser.moneyinFrozen -= previousAuction.currentPrice;
            previousAuction.validAuction = false;
            System.err.println("Hoàn tiền cho " + previousAuction.bidUser.userName + " số tiền " + previousAuction.currentPrice + " thành công!");
        }
        //Nhớ update trong Database/DAO
    }
    
}
