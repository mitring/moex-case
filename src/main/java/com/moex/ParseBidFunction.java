package com.moex;

import org.apache.commons.lang3.Range;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.moex.App.ONE_HUNDRED;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Разбор строки, описывающей заявку на аукцион;
 * строки, не соответствующие заданному формату,
 * пропускаются
 *
 * @author Alexey Prudnikov
 */
public final class ParseBidFunction implements Function<String, Bid> {

    private static final Pattern INPUT_LINE_SPLIT_PATTERN = Pattern.compile("\\s+");
    private static final byte BID_TYPE_POSITION = 0;
    private static final byte BID_AMOUNT_POSITION = 1;
    private static final byte BID_PRICE_POSITION = 2;
    private static final byte SPLIT_LINE_SIZE = 3;

    private static final Range<Integer> ACCEPTABLE_AMOUNT = Range.between(1, 1_000);
    private static final Range<Long> ACCEPTABLE_PRICE = Range.between(100L, 10_000L);

    @Override
    public Bid apply(String line) {
        if (isBlank(line)) {
            return null;
        }

        String[] values = INPUT_LINE_SPLIT_PATTERN.split(line);
        if (values.length != SPLIT_LINE_SIZE) {
            return null;
        }

        try {
            int amount = Integer.parseInt(values[BID_AMOUNT_POSITION]);
            long price = ONE_HUNDRED.multiply(new BigDecimal(values[BID_PRICE_POSITION])).longValue();

            if (ACCEPTABLE_AMOUNT.contains(amount) && ACCEPTABLE_PRICE.contains(price)) {
                return new Bid(
                    Bid.BidType.valueOf(values[BID_TYPE_POSITION]),
                    amount,
                    price
                );
            }
        }
        catch (IllegalArgumentException ex) {
            return null;
        }

        return null;
    }
}
