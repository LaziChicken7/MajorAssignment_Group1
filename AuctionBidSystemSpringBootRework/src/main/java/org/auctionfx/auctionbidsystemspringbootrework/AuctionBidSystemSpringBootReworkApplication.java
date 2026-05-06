package org.auctionfx.auctionbidsystemspringbootrework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // BẬT TÍNH NĂNG LẬP LỊCH
public class AuctionBidSystemSpringBootReworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionBidSystemSpringBootReworkApplication.class, args);
    }

}
