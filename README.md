# turkpin-java

[![test](https://github.com/buluterol/turkpin-java/actions/workflows/test.yml/badge.svg)](https://github.com/buluterol/turkpin-java/actions/workflows/test.yml)

Turkpin bayi API'si için Java istemcisi; Spring Boot entegrasyonu dahil: e-pin oyun ve ürün listesi, sipariş verme, sipariş durumu, sipariş listesi ve bakiye.

> Bu paket topluluk tarafından yazılmıştır; Turkpin'in resmi paketi değildir. API'nin güncel ve bağlayıcı tanımı için [dev.turkpin.com](https://dev.turkpin.com) dokümanına bakın.

Diğer diller / Other languages: [PHP](https://github.com/buluterol/turkpin-php) · [Node.js](https://github.com/buluterol/turkpin-node) · [Python](https://github.com/buluterol/turkpin-python) · [Go](https://github.com/buluterol/turkpin-go) · [Java](https://github.com/buluterol/turkpin-java) · [.NET](https://github.com/buluterol/turkpin-dotnet) · [Ruby](https://github.com/buluterol/turkpin-ruby)

## Gereksinimler

Java 11+ (java.net.http), çalışma zamanı bağımlılığı yok

## Kurulum

```sh
git clone https://github.com/buluterol/turkpin-java && cd turkpin-java && mvn install
```

Paket Maven Central'da yayınlı değil. Yerel depoya kurduktan sonra:

```xml
<dependency>
  <groupId>io.github.buluterol</groupId>
  <artifactId>turkpin</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Kullanım

```java
import io.github.buluterol.turkpin.Models.*;
import io.github.buluterol.turkpin.TurkpinClient;
import io.github.buluterol.turkpin.TurkpinException;

TurkpinClient tp = new TurkpinClient(System.getenv("TURKPIN_USER"), System.getenv("TURKPIN_PASS"), true);

List<Game> games = tp.getGames();
List<Product> products = tp.getProducts(games.get(0).id, null);

try {
    Order order = tp.createOrder(games.get(0).id, products.get(0).id, 1);
    if (order.isPending()) {
        OrderInfo info = tp.getOrderStatus(order.orderNo); // info.status == OrderStatus.COMPLETED olunca kodlar gelir
    }
    order.epins.forEach(e -> System.out.println(e.code));
} catch (TurkpinException e) {
    System.err.println("Hata " + e.getCode() + ": " + e.getMessage());
}

System.out.println(tp.getBalance().balance); // BigDecimal
```

## Spring Boot

Otomatik yapılandırma Spring Boot 2.7 ve 3.x ile çalışır; `turkpin.username` tanımlıysa bir `TurkpinClient` bean'i oluşturulur.

`application.yml`:

```yaml
turkpin:
  username: ${TURKPIN_USERNAME}
  password: ${TURKPIN_PASSWORD}
  test: false
  timeout: 30 # saniye
```

```java
@Service
public class OrderService {
    private final TurkpinClient turkpin;

    public OrderService(TurkpinClient turkpin) {
        this.turkpin = turkpin;
    }
}
```

Kendi `TurkpinClient` bean'inizi tanımlarsanız otomatik yapılandırma devre dışı kalır.

Spring kullanmayan projeler (Quarkus, Micronaut, Jakarta EE, düz Java) istemciyi doğrudan oluşturur; Spring bağımlılığı `optional` olduğu için projeye taşınmaz. Kotlin projeleri de aynı paketi kullanabilir.

## Desteklenen komutlar

| API komutu | Açıklama |
|---|---|
| `epinOyunListesi` | Oyun listesi |
| `epinUrunleri` | Bir oyunun e-pin ürünleri (stok, fiyat, min/max adet, ön sipariş, barem) |
| `epinSiparisYarat` | Sipariş verme (karakter adı, ön sipariş ve barem tutarı isteğe bağlı) |
| `siparisDurumu` | Sipariş durumu ve teslim edilen kodlar |
| `siparisListesi` | İki tarih arasındaki siparişler (en çok 30 gün) |
| `balance` | Bakiye, kredi, bonus ve harcama |

Listede olmayan komutlar için istemcideki genel `call` metodu kullanılabilir.

**Sipariş durumları (`SIPARIS_DURUMU`):** 1 işleniyor, 2 tamamlandı, 3 iptal, 99 teslimatta, 199 ön sipariş teslimatta. `DURUM_KODU` 999 ise Turkpin siparişle ilgili bir sorunu inceliyor demektir; iptal sebebi ya da not `EKSTRA` alanında gelir.

**Ön sipariş ve barem:** Ürünün `pre_order` alanı `true` ise sipariş ön sipariş olarak verilmelidir. Barem tutarlı ürünlerde tutar `min_barem` ile `max_barem` arasında, `barem_step` adımlarıyla seçilir. Bu siparişlerin sonucu `Pending` döner; kodlar için sipariş durumunu sorgulayın.

**Hatalar:** API `000` dışında bir hata kodu döndürdüğünde istemci, kodu ve açıklamayı taşıyan bir hata fırlatır. HTTP ya da XML hatalarında kod boştur.

## Test ortamı

- Test: `https://www.turkpin.net/api.php`, canlı: `https://www.turkpin.com/api.php`.
- Test ortamına erişmek için sunucu IP adresinizin Turkpin'e tanıtılması gerekir; tanıtılmamış IP'den gelen isteklere API `002` hata koduyla cevap verir. Test hesabı ve IP tanımı için [dev.turkpin.com](https://dev.turkpin.com) adresindeki iletişim bilgilerini kullanın.
- Kullanıcı adı ve şifreyi koda yazmayın; ortam değişkeni ya da gizli anahtar deposundan okuyun.

## Geliştirme

Testler gerçek API'ye bağlanmaz; örnek XML cevaplarıyla çalışır.

```sh
mvn test
mvn test -Pspring-boot-3  # Java 17+
```

## English

Java client for the Turkpin dealer API with Spring Boot auto-configuration (e-pin games, products, orders, order status, order history, balance). Community-maintained, not an official Turkpin package; see [dev.turkpin.com](https://dev.turkpin.com) for the authoritative API reference. The sandbox (`turkpin.net`) only accepts requests from server IP addresses registered with Turkpin (otherwise error `002`). Install and usage are shown above; method names are in English, while API command and field names follow the original Turkish API.

## Lisans

MIT
