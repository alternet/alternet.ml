package ml.alternet.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

@Test
public class JAXBTest {

    public static class Item {

        public Item() { }

        public Item(int val) {
            this.value = val;
        }

        @XmlAttribute
        public int value;

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Item && ((Item) obj).value == this.value;
        }

        @Override
        public int hashCode() {
            return this.value;
        }

        @Override
        public String toString() {
            return "<item value=" + value + "/>";
        }
    }

    static QName ITEM = new QName("http://example.com/items", "item");
    static Supplier<InputStream> LIST = () -> JAXBTest.class.getResourceAsStream("items.xml");

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void xmlNotCached_Should_UnmarshallToItems() {
        JAXBStream<Item> xml = new JAXBStream<Item>(LIST, ITEM, Item.class,
            JAXBStream.CacheStrategy.noCache);
        List<Item> items = xml.stream().collect(Collectors.toList());
        assertThat(items).containsExactly(new Item(42), new Item(0), new Item(-24));
        assertThat(xml.source).isInstanceOf(JAXBStream.StreamSource.class);
        assertThat(((JAXBStream.StreamSource) xml.source).getLocation()).isEmpty(); // because JAXBTest.class.getResourceAsStream()
        assertThat(xml.cacheStrategy).isSameAs(JAXBStream.CacheStrategy.noCache);
    }

    public void xmlInMemoryCache_Should_UnmarshallToItems() {
        JAXBStream<Item> xml = new JAXBStream<Item>(LIST, ITEM, Item.class,
            JAXBStream.CacheStrategy.memoryCache);
        assertThat(xml.source).isInstanceOf(JAXBStream.StreamSource.class);

        List<Item> items = xml.stream().collect(Collectors.toList());
        assertThat(items).containsExactly(new Item(42), new Item(0), new Item(-24));
        assertThat(xml.source).isInstanceOf(JAXBStream.ListSource.class);
        assertThat(xml.cacheStrategy).isSameAs(JAXBStream.CacheStrategy.noCache);

        // check reading through the cache
        List<Item> itemsInMemory = xml.stream().collect(Collectors.toList());
        assertThat(itemsInMemory).containsExactly(new Item(42), new Item(0), new Item(-24));

        xml.clearCache();

        // ensure it was rebuilt properly
        assertThat(xml.source).isInstanceOf(JAXBStream.StreamSource.class);
        assertThat(xml.cacheStrategy).isSameAs(JAXBStream.CacheStrategy.memoryCache);

        // ensure it works fine again
        List<Item> itemsCleared = xml.stream().collect(Collectors.toList());
        assertThat(itemsCleared).containsExactly(new Item(42), new Item(0), new Item(-24));
        assertThat(xml.source).isInstanceOf(JAXBStream.ListSource.class);
        assertThat(xml.cacheStrategy).isSameAs(JAXBStream.CacheStrategy.noCache);

        List<Item> itemsClearedInMemory = xml.stream().collect(Collectors.toList());
        assertThat(itemsClearedInMemory).containsExactly(new Item(42), new Item(0), new Item(-24));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void xmlInTmpFileCache_Should_UnmarshallToItems() throws URISyntaxException {
        JAXBStream<Item> xml = new JAXBStream<Item>(LIST, ITEM, Item.class,
            JAXBStream.CacheStrategy.localTmpFileCache);
        assertThat(xml.source).isInstanceOf(JAXBStream.StreamSource.class);

        List<Item> items = xml.stream().collect(Collectors.toList());
        assertThat(items).containsExactly(new Item(42), new Item(0), new Item(-24));
        assertThat(xml.source).isInstanceOf(JAXBStream.StreamSource.class);
        assertThat(xml.cacheStrategy).isSameAs(JAXBStream.CacheStrategy.noCache);
        Optional<URL> tmp = ((JAXBStream.StreamSource) xml.source).getLocation();
        assertThat(tmp).isPresent();
        File file = new File(tmp.get().toURI());
        // ensure TMP file exists
        assertThat(file).exists();
        assertThat(file.getName()).contains("JAXBStream_");

        // check reading through the cache
        List<Item> itemsInFile = xml.stream().collect(Collectors.toList());
        assertThat(itemsInFile).containsExactly(new Item(42), new Item(0), new Item(-24));

        xml.clearCache();
        // ensure TMP file was deleted
        assertThat(file).doesNotExist();

        // ensure it was rebuilt properly
        assertThat(xml.source).isInstanceOf(JAXBStream.StreamSource.class);
        assertThat(xml.cacheStrategy).isSameAs(JAXBStream.CacheStrategy.localTmpFileCache);
        assertThat(((JAXBStream.StreamSource) xml.source).getLocation()).isEmpty(); // because JAXBTest.class.getResourceAsStream()

        // ensure it works fine again
        List<Item> itemsCleared = xml.stream().collect(Collectors.toList());
        assertThat(itemsCleared).containsExactly(new Item(42), new Item(0), new Item(-24));
        assertThat(xml.source).isInstanceOf(JAXBStream.StreamSource.class);
        assertThat(xml.cacheStrategy).isSameAs(JAXBStream.CacheStrategy.noCache);
        Optional<URL> tmpClear = ((JAXBStream.StreamSource) xml.source).getLocation();
        assertThat(tmpClear).isPresent();
        assertThat(tmpClear.get().getPath()).contains("JAXBStream_");

        List<Item> itemsClearedInMemory = xml.stream().collect(Collectors.toList());
        assertThat(itemsClearedInMemory).containsExactly(new Item(42), new Item(0), new Item(-24));
    }

}
