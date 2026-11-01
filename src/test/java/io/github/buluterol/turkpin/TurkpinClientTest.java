package io.github.buluterol.turkpin;

import static org.junit.jupiter.api.Assertions.*;

import io.github.buluterol.turkpin.Models.Balance;
import io.github.buluterol.turkpin.Models.Game;
import io.github.buluterol.turkpin.Models.Order;
import io.github.buluterol.turkpin.Models.OrderInfo;
import io.github.buluterol.turkpin.Models.OrderStatus;
import io.github.buluterol.turkpin.Models.Product;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class TurkpinClientTest {
    private final List<String[]> sent = new ArrayList<>();

    private TurkpinClient client(String fixture) throws Exception {
        String body;
        if (fixture.startsWith("<")) {
            body = fixture;
        } else {
            try (InputStream in = getClass().getResourceAsStream("/fixtures/" + fixture)) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        String b = body;
        return new TurkpinClient("dealer@example.com", "s3cr<et", TurkpinClient.TEST_URL, Duration.ofSeconds(5),
                (url, form, timeout) -> {
                    sent.add(new String[] {url, form});
                    return b;
                });
    }

    private String sentParam(String name) throws Exception {
        String form = sent.get(sent.size() - 1)[1];
        String xml = URLDecoder.decode(form.substring("DATA=".length()), StandardCharsets.UTF_8);
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
        var nodes = root.getElementsByTagName(name);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    @Test
    void requestFormat() throws Exception {
        client("games.xml").getGames();
        assertEquals(TurkpinClient.TEST_URL, sent.get(0)[0]);
        assertEquals("epinOyunListesi", sentParam("cmd"));
        assertEquals("s3cr<et", sentParam("password"));
    }

    @Test
    void urls() {
        assertEquals(TurkpinClient.LIVE_URL, new TurkpinClient("u", "p", false).getUrl());
        assertEquals(TurkpinClient.TEST_URL, new TurkpinClient("u", "p", true).getUrl());
    }

    @Test
    void games() throws Exception {
        List<Game> games = client("games.xml").getGames();
        assertEquals(2, games.size());
        assertEquals(1, games.get(0).id);
        assertEquals("Game 1", games.get(0).name);
    }

    @Test
    void products() throws Exception {
        List<Product> list = client("products.xml").getProducts(1, null);
        assertEquals("1", sentParam("oyunKodu"));
        assertNull(sentParam("urunKodu"));
        Product a = list.get(0);
        Product b = list.get(1);
        assertEquals(26278, a.stock);
        assertEquals(0, a.maxOrder);
        assertEquals(new BigDecimal("0.001"), a.price);
        assertFalse(a.preOrder);
        assertFalse(a.isTiered());
        assertTrue(b.preOrder && b.isTiered());
        assertEquals(new BigDecimal("25"), b.minBarem);
        assertEquals(new BigDecimal("1250"), b.maxBarem);
        assertEquals(new BigDecimal("0.01"), b.baremStep);
        assertEquals(5, b.maxOrder);
    }

    @Test
    void createOrder() throws Exception {
        Order o = client("order_success.xml").createOrder(1, 4, 2, "Player1", false, null);
        assertEquals("2", sentParam("adet"));
        assertEquals("Player1", sentParam("character"));
        assertNull(sentParam("pre_order"));
        assertEquals("25031013105101", o.orderNo);
        assertFalse(o.isPending());
        assertEquals("S3XS-EEDY-APAE-62EM", o.epins.get(0).code);
        assertEquals("Test Ürün", o.epins.get(0).description);
    }

    @Test
    void preOrderWithBarem() throws Exception {
        Order o = client("order_pending.xml").createOrder(1, 4, 1, null, true, new BigDecimal("30.50"));
        assertEquals("true", sentParam("pre_order"));
        assertEquals("30.50", sentParam("barem"));
        assertTrue(o.isPending());
        assertTrue(o.epins.isEmpty());
    }

    @Test
    void apiErrors() throws Exception {
        TurkpinClient c = client("order_out_of_stock.xml");
        TurkpinException e = assertThrows(TurkpinException.class, () -> c.createOrder(1, 2, 1));
        assertEquals("012", e.getCode());
        assertEquals("Urun stok yetersiz.", e.getMessage());
        TurkpinClient c2 = client("error_auth.xml");
        assertEquals("001", assertThrows(TurkpinException.class, c2::getGames).getCode());
    }

    @Test
    void invalidResponse() throws Exception {
        TurkpinClient c = client("<html>blocked</html");
        assertThrows(TurkpinException.class, c::getBalance);
    }

    @Test
    void orderStatus() throws Exception {
        OrderInfo s = client("status_completed.xml").getOrderStatus("25031013105101");
        assertEquals("25031013105101", sentParam("siparisNo"));
        assertEquals(OrderStatus.COMPLETED, s.status);
        assertEquals(new BigDecimal("0.0010"), s.amount);
        assertEquals("S3XS-EEDY-APAE-62EM", s.epins.get(0).code);
        OrderInfo c = client("status_cancelled.xml").getOrderStatus("25030620073001");
        assertEquals(OrderStatus.CANCELLED, c.status);
        assertEquals("Stok yok", c.extra);
    }

    @Test
    void listOrders() throws Exception {
        List<OrderInfo> list = client("orders.xml").listOrders(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 30));
        assertEquals("2026-01-01", sentParam("baslangicTarihi"));
        assertEquals("2026-01-30", sentParam("bitisTarihi"));
        assertEquals(2, list.size());
        assertEquals(OrderStatus.PROCESSING, list.get(0).status);
        assertEquals("999", list.get(1).stateCode);
        assertEquals("CANCELLATION REASON", list.get(1).extra);
    }

    @Test
    void balance() throws Exception {
        Balance b = client("balance.xml").getBalance();
        assertEquals(new BigDecimal("98689.0000"), b.balance);
        assertEquals(new BigDecimal("1311.0000"), b.spending);
    }
}
