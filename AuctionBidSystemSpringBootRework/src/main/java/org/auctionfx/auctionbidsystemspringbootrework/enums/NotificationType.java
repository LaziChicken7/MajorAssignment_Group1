package org.auctionfx.auctionbidsystemspringbootrework.enums;

public enum NotificationType {
    PAYMENT_VERIFICATION, // Cần xác thực (Hiện nút xanh đỏ)
    AUCTION_SUCCESS, // Đấu giá thành công (Hiện thùng rác)
    AUCTION_FAILED, // Đấu giá thất bại (Hiện thùng rác)
    ITEM_CANCELLED_BY_ADMIN, // Sản phẩm bị hủy bởi Admin (Admin hủy sản phẩm)
    FRIEND_REQUEST, // Xác nhận kết bạn
    UPGRADE_REQUEST, // Thông báo up seller
    SYSTEM // Thông báo chung hệ thống
}
