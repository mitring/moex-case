package com.moex;

import java.math.BigDecimal;

public class App {

    static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

    public static void main(String[] args) {
        DiscreteAuction auction = new DiscreteAuction();
        auction.readData(System.in);
        System.out.println(auction.makeDeal());
    }

}
