package com.moex;

/**
 * Заявка на покупку или продажу ценной бумаги,
 * сумма заявки хранится в копейках как целое число
 *
 * @author Alexey Prudnikov
 */
public final class Bid implements Comparable<Bid> {

    enum BidType {B, S}

    private final BidType type;
    private final int amount;
    private final long price;

    public Bid(BidType type, int amount, long price) {
        this.type = type;
        this.amount = amount;
        this.price = price;
    }

    public BidType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public long getPrice() {
        return price;
    }

    @Override
    public int compareTo(Bid bid) {
        return Long.compare(this.price, bid.price);
    }
}
