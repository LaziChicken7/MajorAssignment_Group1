package org.auctionfx.auctionbidsystemspringbootrework;

import org.auctionfx.auctionbidsystemspringbootrework.service.AuctionServiceTest;
import org.auctionfx.auctionbidsystemspringbootrework.service.ItemServiceTest;
import org.auctionfx.auctionbidsystemspringbootrework.service.NotificationServiceTest;
import org.auctionfx.auctionbidsystemspringbootrework.service.PaymentServiceTest;
import org.auctionfx.auctionbidsystemspringbootrework.service.UserServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        AuctionServiceTest.class,
        ItemServiceTest.class,
        NotificationServiceTest.class,
        PaymentServiceTest.class,
        UserServiceTest.class
})
public class AuctionBidSystemSpringBootReworkApplicationTests {
    // Để trống class này. Khai báo @Suite ở trên sẽ tự động gom các class test lại và chạy.
}