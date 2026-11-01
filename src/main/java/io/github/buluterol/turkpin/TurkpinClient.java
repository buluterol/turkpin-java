package io.github.buluterol.turkpin;

import io.github.buluterol.turkpin.Models.Balance;
import io.github.buluterol.turkpin.Models.Epin;
import io.github.buluterol.turkpin.Models.Game;
import io.github.buluterol.turkpin.Models.Order;
import io.github.buluterol.turkpin.Models.OrderInfo;
import io.github.buluterol.turkpin.Models.Product;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Turkpin dealer API client. Every call is an HTTP POST to api.php with one form field, DATA, holding
 * {@code <APIRequest><params>...</params></APIRequest>}. Responses are XML. Docs: https://dev.turkpin.com
 */
public final class TurkpinClient {
    public static final String LIVE_URL = "https://www.turkpin.com/api.php";
    public static final String TEST_URL = "https://www.turkpin.net/api.php";

    /** Sends a form body and returns the response text; replaceable for tests or custom HTTP. */
    @FunctionalInterface
    public interface Transport {
        String post(String url, String formBody, Duration timeout);
    }

    private final String username;
    private final String password;
    private final String url;
    private final Duration timeout;
    private final Transport transport;

    /** Client for the live system, or the sandbox when {@code test} is true. */
    public TurkpinClient(String username, String password, boolean test) {
        this(username, password, test ? TEST_URL : LIVE_URL, Duration.ofSeconds(30), null);
    }

    public TurkpinClient(String username, String password, String url, Duration timeout, Transport transport) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("username and password are required");
        }
        this.username = username;
        this.password = password;
        this.url = url;
        this.timeout = timeout;
        this.transport = transport != null ? transport : new DefaultTransport();
    }

    public String getUrl() {
        return url;
    }

    public List<Game> getGames() {
        List<Game> out = new ArrayList<>();
        for (Element g : list(child(call("epinOyunListesi", Map.of()), "oyunListesi"), "oyun")) {
            out.add(new Game(toInt(text(g, "id")), text(g, "name")));
        }
        return out;
    }

    /** Lists e-pin products of a game; {@code productId} may be null to list all. */
    public List<Product> getProducts(int gameId, Integer productId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("oyunKodu", gameId);
        if (productId != null) {
            params.put("urunKodu", productId);
        }
        List<Product> out = new ArrayList<>();
        for (Element u : list(child(call("epinUrunleri", params), "epinUrunListesi"), "urun")) {
            out.add(new Product(toInt(text(u, "id")), text(u, "name"), toInt(text(u, "stock")),
                    Math.max(1, toInt(text(u, "min_order"))), toInt(text(u, "max_order")), dec(text(u, "price")),
                    text(u, "tax_type"), "true".equalsIgnoreCase(text(u, "pre_order")), decOrNull(text(u, "min_barem")),
                    decOrNull(text(u, "max_barem")), decOrNull(text(u, "barem_step"))));
        }
        return out;
    }

    public Order createOrder(int gameId, int productId, int quantity) {
        return createOrder(gameId, productId, quantity, null, false, null);
    }

    /**
     * Places an e-pin order. {@code preOrder} must be true for products listed with preOrder; {@code barem} is the
     * amount for tiered products (between minBarem and maxBarem, in baremStep steps). Optional values may be null.
     */
    public Order createOrder(int gameId, int productId, int quantity, String character, boolean preOrder,
                             BigDecimal barem) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("oyunKodu", gameId);
        params.put("urunKodu", productId);
        params.put("adet", quantity);
        if (character != null) {
            params.put("character", character);
        }
        if (preOrder) {
            params.put("pre_order", "true");
        }
        if (barem != null) {
            params.put("barem", barem.toPlainString());
        }
        Element p = call("epinSiparisYarat", params);
        return new Order(text(p, "siparisNo"), text(p, "siparisSonuc"), dec(text(p, "siparisTutari")), epins(p));
    }

    public OrderInfo getOrderStatus(String orderNo) {
        return orderInfo(call("siparisDurumu", Map.of("siparisNo", orderNo)));
    }

    /** Orders between two dates (inclusive, at most 30 days apart). */
    public List<OrderInfo> listOrders(LocalDate start, LocalDate end) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("baslangicTarihi", start.toString());
        params.put("bitisTarihi", end.toString());
        List<OrderInfo> out = new ArrayList<>();
        for (Element s : list(child(call("siparisListesi", params), "SIPARISLER"), "SIPARIS")) {
            out.add(orderInfo(s));
        }
        return out;
    }

    public Balance getBalance() {
        Element b = child(call("balance", Map.of()), "balanceInformation");
        return new Balance(dec(text(b, "balance")), dec(text(b, "credit")), dec(text(b, "bonus")),
                dec(text(b, "spending")));
    }

    /** Sends one command and returns the {@code <params>} element of the response. */
    public Element call(String cmd, Map<String, ?> params) {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("cmd", cmd);
        all.put("username", username);
        all.put("password", password);
        all.putAll(params);
        StringBuilder xml = new StringBuilder("<APIRequest><params>");
        all.forEach((k, v) -> xml.append('<').append(k).append('>').append(escape(String.valueOf(v)))
                .append("</").append(k).append('>'));
        xml.append("</params></APIRequest>");
        String body = "DATA=" + URLEncoder.encode(xml.toString(), StandardCharsets.UTF_8);
        String raw = transport.post(url, body, timeout);

        Element p = null;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            javax.xml.parsers.DocumentBuilder builder = f.newDocumentBuilder();
            builder.setErrorHandler(null);
            Document doc = builder.parse(new ByteArrayInputStream(raw.trim().getBytes(StandardCharsets.UTF_8)));
            p = child(doc.getDocumentElement(), "params");
        } catch (Exception ignored) {
            // handled below
        }
        if (p == null) {
            throw new TurkpinException("Invalid response from Turkpin API: "
                    + raw.substring(0, Math.min(200, raw.length())), "");
        }
        String code = text(p, "error");
        if (code.isEmpty()) {
            code = text(p, "HATA_NO");
        }
        if (!code.isEmpty() && !"000".equals(code)) {
            String msg = text(p, "error_desc");
            if (msg.isEmpty()) {
                msg = text(p, "HATA_ACIKLAMA");
            }
            throw new TurkpinException(msg.isEmpty() ? "Turkpin API error" : msg, code);
        }
        return p;
    }

    private static final class DefaultTransport implements Transport {
        private final HttpClient http = HttpClient.newHttpClient();

        @Override
        public String post(String url, String formBody, Duration timeout) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(timeout)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody)).build();
            try {
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (res.statusCode() >= 400 && res.statusCode() != 401) {
                    throw new TurkpinException("HTTP " + res.statusCode() + " from Turkpin API", "");
                }
                return res.body();
            } catch (IOException e) {
                throw new TurkpinException("HTTP request failed: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TurkpinException("HTTP request interrupted", e);
            }
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static Element child(Element el, String name) {
        if (el == null) {
            return null;
        }
        for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && name.equals(n.getNodeName())) {
                return (Element) n;
            }
        }
        return null;
    }

    private static List<Element> list(Element el, String name) {
        List<Element> out = new ArrayList<>();
        if (el != null) {
            for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element && name.equals(n.getNodeName())) {
                    out.add((Element) n);
                }
            }
        }
        return out;
    }

    private static String text(Element el, String name) {
        Element c = child(el, name);
        return c == null ? "" : c.getTextContent().trim();
    }

    private static int toInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal dec(String s) {
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal decOrNull(String s) {
        return s.isEmpty() ? null : dec(s);
    }

    private static List<Epin> epins(Element el) {
        List<Epin> out = new ArrayList<>();
        for (Element e : list(child(el, "epin_list"), "epin")) {
            out.add(new Epin(text(e, "code"), text(e, "desc"), text(e, "id")));
        }
        return out;
    }

    private static OrderInfo orderInfo(Element s) {
        String extra = text(s, "EKSTRA");
        if (extra.isEmpty()) {
            extra = text(s, "EXTRA");
        }
        return new OrderInfo(text(s, "SIPARIS_NO"), toInt(text(s, "SIPARIS_DURUMU")),
                text(s, "SIPARIS_DURUMU_ACIKLAMA"), text(s, "DURUM_KODU"), text(s, "KONTROL_TARIHI"),
                dec(text(s, "SIPARIS_TUTARI")), extra, epins(s));
    }
}
