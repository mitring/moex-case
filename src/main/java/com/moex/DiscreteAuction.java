package com.moex;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Дискретный аукцион
 *
 * Общая идея алгоритма:
 *
 * 1. Во время чтения цены заявок конвертируются из рублей в копейки, что гарантирует точное
 *    отношение "больше-меньше" между двумя разными ценами.
 * 2. После чтения данных вычистить заявки самых "жадных" игроков, которые гарантированно
 *    не могут быть исполнены; а однотипные заявки с одинаковыми ценами объединить
 *    {@link #cleanSellBids(java.util.List, long)} и {@link #cleanBuyBids(java.util.List, long)}).
 * 3. Данные заявок трансформируются: для каждой цены вычисляется общий ("кумулятивный") объем ценных
 *    бумаг, которые могут быть куплены/проданы по такой цене; при этом данные о заявках на продажу для
 *    быстрого поиска по ним складываются в дерево ({@link #transformSellBids(java.util.List)} и
 *    {@link #transformBuyBids(java.util.List)})
 * 4. Осуществляется определение оптимальной цены с моиощью метода {@link #makeDeal()}, сложность
 *    алгоритма O(n * log(n))
 *
 * @author Alexey Prudnikov
 */
public class DiscreteAuction {

    private static final int BIDS_MAX_COUNT = 1_000_000;

    private List<Bid> cumulativeSellBids;
    private NavigableMap<Long, Integer> cumulativeBuyBids;
    private DealResult result;

    /**
     * Чтение, очистка и трансформация для быстрого поиска входных данных
     *
     * @param dataSource Поток с входными данными
     */
    public void readData(InputStream dataSource) {
        Map<Bid.BidType, List<Bid>> bids = read(dataSource);
        if (bids.values().stream().anyMatch(List::isEmpty)) {
            // если хоть один список пуст, сделку сформировать не получится
            result = new DealResult();
            return;
        }

        List<Bid> sellBids = bids.get(Bid.BidType.S);
        List<Bid> buyBids = bids.get(Bid.BidType.B);
        cumulativeSellBids = transformSellBids(
            cleanSellBids(sellBids, buyBids.get(buyBids.size() - 1).getPrice())
        );
        cumulativeBuyBids = transformBuyBids(
            cleanBuyBids(buyBids, sellBids.get(0).getPrice())
        );
    }

    /**
     * Начитка данных из входящего потока; пустые и не
     * соответствующие формату строки отбрасываются
     *
     * @param dataSource Поток с входными данными
     *
     * @return Два списка заявок (на продажу и на покупку),
     *         отсортированные по возрастанию цены
     */
    private Map<Bid.BidType, List<Bid>> read(InputStream dataSource) {
        BufferedReader in = new BufferedReader(new InputStreamReader(dataSource));
        return
            in.lines()
              .limit(BIDS_MAX_COUNT)
              .map(new ParseBidFunction())
              .filter(Objects::nonNull)
              .sorted()
              .collect(
                  Collectors.groupingBy(Bid::getType)
              );
    }

    /**
     * Очистка исходных данных по заявкам на продажу:
     * - удаляются заявки, которые никогда не будут выполнены
     * (с ценой продажи больше самой высокой цены покупки);
     * - заявки с одинаковыми суммами продажи объединяются между собой
     *
     * @param sellBids Список заявок на продажу
     * @param maxPrice Самая высокая цена покупки
     *
     * @return Очищенный от лишних данных и отсортированный по возрастанию цены
     *         список заявок на продажу
     */
    private List<Bid> cleanSellBids(List<Bid> sellBids, long maxPrice) {
        return cleanBids(sellBids, Bid.BidType.S, maxPrice);
    }

    /**
     * Очистка исходных данных по заявкам на покупку:
     * - удаляются заявки, которые никогда не будут выполнены
     * (с ценой покупки меньше самой низкой цены продажи);
     * - заявки с одинаковыми суммами покупки объединяются между собой
     *
     * @param buyBids Список заявок на покупку
     * @param minPrice Самая низкая цена продажи
     *
     * @return Очищенный от лишних данных и отсортированный по убыванию цены
     *         список заявок на покупку
     */
    private List<Bid> cleanBuyBids(List<Bid> buyBids, long minPrice) {
        return cleanBids(buyBids, Bid.BidType.B, minPrice);
    }

    /**
     * Очистка исходных данных по заявкам:
     * - удаляются заявки, которые никогда не будут выполнены;
     * - заявки с одинаковыми суммами покупки объединяются между собой;
     * - заявки на продажу сортируются по возрастанию цены, на покупку - по убыванию цены
     *
     * @param bids Список заявок, которые нужно очистить
     * @param bidType Тип заявок в списке {@code bids}
     * @param thresholdPrice Максимальная (минимальная) цена, выше (ниже) которой
     *                       заявка не может быть исполнена
     *
     * @return Очищенный от лишних данных и отсортированный в определенном порядке список заявок
     */
    private List<Bid> cleanBids(List<Bid> bids, Bid.BidType bidType, long thresholdPrice) {
        return
            bids
                .stream()
                .filter(new FeasibleBidPredicate(bidType, thresholdPrice))
                .collect(
                    Collectors.groupingBy(
                        Bid::getPrice,
                        Collectors.summingInt(Bid::getAmount)
                    )
                )
                .entrySet()
                    .stream()
                    .map(e -> new Bid(bidType, e.getValue(), e.getKey()))
                    .sorted(bidType == Bid.BidType.S ? Comparator.naturalOrder() : Comparator.reverseOrder())
                    .collect(Collectors.toList());
    }

    /**
     * Трансформация заявок на продажу: на выходе каждая заявка будет содержаь общее
     * (кумулятивное) количество ценных бумаг, которые могут быть проданы за указанную
     * в заявке сумму

     * @param sellBids Очищенный от лишних данных и отсортированный по возрастанию цены
     *                 список заявок на продажу
     *
     * @return Список заявок на продажу, содержащий кумулятивные значения количества ценных
     *         бумаг, и отсортированный по возрастанию цены
     */
    private List<Bid> transformSellBids(List<Bid> sellBids) {
        if (sellBids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Bid> result = new ArrayList<>(sellBids.size());
        int cumulativeAmount = 0;
        for (Bid b : sellBids) {
            cumulativeAmount += b.getAmount();
            result.add(new Bid(b.getType(), cumulativeAmount, b.getPrice()));
        }

        return result;
    }

    /**
     * Трансформация заявок на покупку: на выходе каждая заявка будет содержаь общее
     * (кумулятивное) количество ценных бумаг, которые могут быть куплены за указанную
     * в заявке сумму; при этом для быстрого поиска заявки будут складываться в {@link TreeMap}:
     * ключ - сумма заявки, значение - общее количество заявок на эту сумму

     * @param buyBids Очищенный от лишних данных и отсортированный по убыванию цены
     *                список заявок на покупку
     *
     * @return Дерево поиска для "кумулятивных" заявок на покупку
     */
    private NavigableMap<Long, Integer> transformBuyBids(List<Bid> buyBids) {
        if (buyBids.isEmpty()) {
            return Collections.emptyNavigableMap();
        }

        NavigableMap<Long, Integer> result = new TreeMap<>();
        int amount = 0;
        for (Bid b : buyBids) {
            amount += b.getAmount();
            result.put(b.getPrice(), amount);
        }

        return result;
    }

    /**
     * Определение оптимальной цены: осуществляется проход по всем заявкам на продажу от
     * наибольшей цены к наименьшей, и для каждой цены продажи ищется наиболее оптимальная
     * цена покупки с помощью дерева поиска; если такое соответствие находится, то вычисляется объем сделки.
     *
     * Таким образом, общая сложность этого алгоритма O(n * log(n)): проход по массиву
     * выполняется за O(n), на каждой итерации выполняется поиск в дереве за O(log(n)).
     *
     * @return Результат подбора оптимальной цены в виде экземпляра объекта {@link DealResult}
     */
    public DealResult makeDeal() {
        if (result != null) {
            return result;
        }

        if (cumulativeSellBids.isEmpty() || cumulativeBuyBids.isEmpty()) {
            // если в результате чисток и трансформаций данных не осталось, то сразу выходим
            result = new DealResult();
            return result;
        }

        int bestAmount = 0;
        List<Long> optimalPrices = new ArrayList<>();

        // проход в обратном порядке (от наибольшей цены к наименьшей), чтобы не тратить ресурсы на сортировку
        for (int i = cumulativeSellBids.size() - 1; i >= 0; i--) {
            Bid sellBid = cumulativeSellBids.get(i);
            Map.Entry<Long, Integer> matchedBuy = cumulativeBuyBids.ceilingEntry(sellBid.getPrice());
            if (matchedBuy == null) {
                continue;
            }

            int dealAmount = Math.min(matchedBuy.getValue(), sellBid.getAmount());
            if (dealAmount >= bestAmount) {
                if (dealAmount > bestAmount) {
                    optimalPrices.clear();
                    bestAmount = dealAmount;
                }
                optimalPrices.add(sellBid.getPrice());
                optimalPrices.add(matchedBuy.getKey());
            }
        }

        result = new DealResult(optimalPrices, bestAmount);
        return result;
    }

}
