package com.moex;

import java.util.function.Predicate;

/**
 * Предикат, который отбирает только выполнимые заявки:
 * - у которых цена продажи не больше максимальной цены покупки;
 * - у которых цена покупки не меньше минимальной цены продажи
 *
 * @author Alexey Prudnikov
 */
public final class FeasibleBidPredicate implements Predicate<Bid> {

    private final Bid.BidType bidType;
    private final long thresholdPrice;

    public FeasibleBidPredicate(Bid.BidType bidType, long thresholdPrice) {
        this.bidType = bidType;
        this.thresholdPrice = thresholdPrice;
    }

    @Override
    public boolean test(Bid bid) {
        if (bidType == Bid.BidType.S) {
            return bid.getPrice() <= thresholdPrice;
        }
        return bid.getPrice() >= thresholdPrice;
    }
}
