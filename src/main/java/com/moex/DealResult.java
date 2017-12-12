package com.moex;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.moex.App.ONE_HUNDRED;
import static java.math.RoundingMode.HALF_UP;
import static java.math.RoundingMode.UP;

/**
 * Описание результата вычисления оптимальной цены
 *
 * @author Alexey Prudnikov
 */
public final class DealResult {

    private static final String NO_DEAL = "0 n/a";
    private static final String OUTPUT_TEMPLATE = "%d %.2f";

    private final BigDecimal bestPrice;
    private final int bestAmount;

    public DealResult() {
        this(Collections.emptyList(), 0);
    }

    public DealResult(List<Long> optimalPrices, int bestAmount) {
        this.bestPrice = optimalPrices.isEmpty() ?
            BigDecimal.ZERO : calculcatePrice(optimalPrices);
        this.bestAmount = bestAmount;
    }

    private BigDecimal calculcatePrice(List<Long> optimalPrices) {
        BigDecimal optimalPricesCnt = new BigDecimal(optimalPrices.size());
        BigDecimal optimalPricesSum = new BigDecimal(optimalPrices.stream().reduce(0L, Long::sum));

        return
            optimalPricesSum
                .divide(optimalPricesCnt, 0, UP)
                .divide(ONE_HUNDRED, 2, HALF_UP);
    }

    @Override
    public String toString() {
        return bestAmount == 0 ?
            NO_DEAL : String.format(Locale.US, OUTPUT_TEMPLATE, bestAmount, bestPrice);
    }
}
