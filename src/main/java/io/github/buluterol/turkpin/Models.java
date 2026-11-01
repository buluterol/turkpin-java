package io.github.buluterol.turkpin;

import java.math.BigDecimal;
import java.util.List;

/** Response types. Money values are BigDecimal to keep them exact. */
public final class Models {
    private Models() {
    }

    /** Values of SIPARIS_DURUMU returned by siparisDurumu and siparisListesi. */
    public static final class OrderStatus {
        public static final int PROCESSING = 1;
        public static final int COMPLETED = 2;
        public static final int CANCELLED = 3;
        public static final int DELIVERING = 99;
        public static final int PRE_ORDER_DELIVERING = 199;

        private OrderStatus() {
        }
    }

    public static final class Game {
        public final int id;
        public final String name;

        Game(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static final class Product {
        public final int id;
        public final String name;
        public final int stock;
        public final int minOrder;
        /** 0 means no upper limit. */
        public final int maxOrder;
        public final BigDecimal price;
        public final String taxType;
        public final boolean preOrder;
        /** Tiered (barem) products only; null otherwise. */
        public final BigDecimal minBarem;
        public final BigDecimal maxBarem;
        public final BigDecimal baremStep;

        Product(int id, String name, int stock, int minOrder, int maxOrder, BigDecimal price, String taxType,
                boolean preOrder, BigDecimal minBarem, BigDecimal maxBarem, BigDecimal baremStep) {
            this.id = id;
            this.name = name;
            this.stock = stock;
            this.minOrder = minOrder;
            this.maxOrder = maxOrder;
            this.price = price;
            this.taxType = taxType;
            this.preOrder = preOrder;
            this.minBarem = minBarem;
            this.maxBarem = maxBarem;
            this.baremStep = baremStep;
        }

        public boolean isTiered() {
            return minBarem != null;
        }
    }

    public static final class Epin {
        public final String code;
        public final String description;
        public final String id;

        Epin(String code, String description, String id) {
            this.code = code;
            this.description = description;
            this.id = id;
        }
    }

    public static final class Order {
        public final String orderNo;
        /** "Success" or "Pending". */
        public final String result;
        public final BigDecimal amount;
        public final List<Epin> epins;

        Order(String orderNo, String result, BigDecimal amount, List<Epin> epins) {
            this.orderNo = orderNo;
            this.result = result;
            this.amount = amount;
            this.epins = epins;
        }

        /** True for pre-order / tiered orders: poll getOrderStatus for the codes. */
        public boolean isPending() {
            return !"Success".equals(result);
        }
    }

    public static final class OrderInfo {
        public final String orderNo;
        /** One of the OrderStatus constants. */
        public final int status;
        public final String description;
        /** "000" normally, "999" when Turkpin is handling a problem with the order. */
        public final String stateCode;
        public final String checkedAt;
        public final BigDecimal amount;
        /** Cancellation reason or problem note. */
        public final String extra;
        public final List<Epin> epins;

        OrderInfo(String orderNo, int status, String description, String stateCode, String checkedAt,
                  BigDecimal amount, String extra, List<Epin> epins) {
            this.orderNo = orderNo;
            this.status = status;
            this.description = description;
            this.stateCode = stateCode;
            this.checkedAt = checkedAt;
            this.amount = amount;
            this.extra = extra;
            this.epins = epins;
        }
    }

    public static final class Balance {
        public final BigDecimal balance;
        public final BigDecimal credit;
        public final BigDecimal bonus;
        public final BigDecimal spending;

        Balance(BigDecimal balance, BigDecimal credit, BigDecimal bonus, BigDecimal spending) {
            this.balance = balance;
            this.credit = credit;
            this.bonus = bonus;
            this.spending = spending;
        }
    }
}
