package ml.alternet.misc;

import static ml.alternet.misc.Thrower.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.xml.sax.InputSource;

import ml.alternet.facet.Localizable;
import ml.alternet.io.TeeInputStream;
import ml.alternet.io.TeeReader;

/**
 * Stream repeatable elements from an XML document to objects with JAXB.
 *
 * Typical XML document is :
 * <pre>&lt;list xmlns="http://example.com/foo"&gt;
 *    &lt;item&gt;...&lt;/item&gt;
 *    &lt;item&gt;...&lt;/item&gt;
 *    &lt;item&gt;...&lt;/item&gt;
 *    ...
 *&lt;/list&gt;</pre>
 *
 * <h1>Usage</h1>
 * <p>For unmarshalling each <code>&lt;item&gt;</code> to <code>com.example.foo.Item</code>, use :</p>
 * <p><code>QName &lt;- {http://example.com/foo}item</code></p>
 * <p><code>class &lt;- com.example.foo.Item</code></p>
 * <p><code>URL   &lt;- http://example.com/api/foo/list.xml</code></p>
 *
 * This class embeds a cache strategy but doesn't define a retention strategy ;
 * however the cache can be clear at will.
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of the items.
 */
public class JAXBStream<T> {

    static Logger LOG = Logger.getLogger(JAXBStream.class.getName());

    Source source; // supply a stream of T
    QName elem; // XML element on which to iterate
    Class<T> clazz; // class to produce
    CacheStrategy cacheStrategy;
    Supplier<JAXBStream<T>> rebuilder; // after clearing the cache, the same init parameters have to be rebuilt
    BooleanSupplier cleaner = () -> false; // no cache to clear so far
    Optional<EventFilter> useFilter = Optional.empty();

    /**
     * Initialize a JAXB stream.
     *
     * @param xml The URL of the XML source.
     * @param elem The name of the element to fetch.
     * @param clazz The class of the object to fetch.
     * @param cacheStrategy The cache strategy for reading again.
     * @throws MalformedURLException When the URL is malformed.
     */
    public JAXBStream(String xml, QName elem, Class<T> clazz, CacheStrategy cacheStrategy)
            throws MalformedURLException
    {
        this(new URL(xml), elem, clazz, cacheStrategy);
    }

    /**
     * Initialize a JAXB stream.
     *
     * @param xml The URL of the XML source.
     * @param elem The name of the element to fetch.
     * @param clazz The class of the object to fetch.
     * @param cacheStrategy The cache strategy for reading again.
     */
    public JAXBStream(URL xml, QName elem, Class<T> clazz, CacheStrategy cacheStrategy) {
        this.source = new StreamSource(xml);
        this.elem = elem;
        this.clazz = clazz;
        this.cacheStrategy = cacheStrategy;
        this.rebuilder = () -> new JAXBStream<T>(xml, elem, clazz, cacheStrategy);
    }

    /**
     * Initialize a JAXB stream.
     *
     * @param xml Give an XML source.
     * @param elem The name of the element to fetch.
     * @param clazz The class of the object to fetch.
     * @param cacheStrategy The cache strategy for reading again.
     */
    public JAXBStream(Supplier<InputStream> xml, QName elem, Class<T> clazz, CacheStrategy cacheStrategy) {
        this.source = new StreamSource(() -> new InputSource(xml.get()));
        this.elem = elem;
        this.clazz = clazz;
        this.cacheStrategy = cacheStrategy;
        this.rebuilder = () -> new JAXBStream<T>(xml, elem, clazz, cacheStrategy);
    }

    /**
     * Initialize a JAXB stream.
     *
     * @param elem The name of the element to fetch.
     * @param clazz The class of the object to fetch.
     * @param cacheStrategy The cache strategy for reading again.
     * @param xml Give an XML source.
     */
    public JAXBStream(QName elem, Class<T> clazz, CacheStrategy cacheStrategy, Supplier<Reader> xml) {
        this.source = new StreamSource(() -> new InputSource(xml.get()));
        this.elem = elem;
        this.clazz = clazz;
        this.cacheStrategy = cacheStrategy;
        this.rebuilder = () -> new JAXBStream<T>(elem, clazz, cacheStrategy, xml);
    }

    /**
     * Get the stream of the expected objects.
     *
     * @return A stream of unmarshalled XML elements.
     */
    public Stream<T> stream() {
        return this.source.stream();
    }

    /**
     * Set an optional XML filter.
     *
     * @param xmlEventFilter To filter the XML input before unmarshalling.
     *
     * @return <code>this</code>, for chaining.
     */
    public JAXBStream<T> useFilter(EventFilter xmlEventFilter) {
        this.useFilter = Optional.of(xmlEventFilter);
        return this;
    }

    /**
     * Clear the cache, if any. Don't use during reading
     * the stream.
     */
    @SuppressWarnings("unchecked")
    public void clearCache() {
        if (this.cleaner.getAsBoolean()) { // do clear the cache
            // it's a bit ugly, but it works
            JAXBStream<T> copy = this.rebuilder.get();
            this.cacheStrategy = copy.cacheStrategy;
            this.useFilter = copy.useFilter;
            // in the copy we lost the reference of the source to "this", let's repair :
            this.source = new StreamSource(((StreamSource) copy.source).input);
            ((StreamSource) this.source).url = ((StreamSource) copy.source).url;
        } // else : no cache
    }

    // the source knows how to supply the stream
    // it starts with a StreamSource and may be replaced with a ListSource
    // if the cache strategy is memoryCache
    abstract class Source {

        abstract Stream<T> stream();

    }

    // allow to store items in memory
    class ListSource extends Source {

        List<T> list;

        ListSource(List<T> list) {
            this.list = list;
        }

        @Override
        Stream<T> stream() {
            return this.list.stream();
        }

    }

    // items are read from an XML document
    class StreamSource extends Source implements Localizable<URL> {

        Optional<URL> url;
        Supplier<InputSource> input;

        StreamSource(URL url) {
            this.url = Optional.of(url);
            this.input = () -> safeCall(() -> new InputSource(url.openStream()));
        }

        StreamSource(Supplier<InputSource> input) {
            this.url = Optional.empty();
            this.input = input;
        }

        @Override
        Stream<T> stream() {
            LOG.info(() -> "⚙ Getting " + JAXBStream.this.clazz.getName()
                + (getLocation().isPresent() ? " from " + getLocation().get() + " " : ""));
            // put items in cache or intercept the input
            Interceptor interceptor = cacheStrategy.newInterceptor(JAXBStream.this);
            try {
                return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE,
                        Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL) {

                    // useful when the interceptor is a tee that save the XML document to a TMP file
                    InputSource is = interceptor.intercept(input.get());
                    XMLInputFactory xif = XMLInputFactory.newFactory();
                    XMLEventReader xmlSource = is.getCharacterStream() == null
                        ? xif.createXMLEventReader(is.getByteStream())
                        : xif.createXMLEventReader(is.getCharacterStream());
                    Unmarshaller unmarshaller = JAXBContext.newInstance(clazz).createUnmarshaller();
                    XMLEventReader xml = JAXBStream.this.useFilter.map(
                            filter -> safeCall(() -> xif.createFilteredReader(xmlSource, filter))
                        ).orElse(xmlSource);

                    @Override
                    public boolean tryAdvance(Consumer<? super T> action) {
                        try {
                            while (xml.hasNext()) {
                                XMLEvent xmlEvent = xml.peek();
                                if (xmlEvent.isStartElement() && ((StartElement) xmlEvent).getName().equals(elem)) {
                                    T item = unmarshaller.unmarshal(xml, clazz).getValue();
                                    // useful when the interceptor save items to a list in memory
                                    interceptor.cache(item);
                                    action.accept(item);
                                    return true;
                                } else {
                                    xmlEvent = xml.nextEvent();
                                }
                            }
                            // useful when the interceptor has intercepted something
                            interceptor.cacheSource();
                            return false;
                        } catch (XMLStreamException | JAXBException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, false);
            } catch (XMLStreamException | FactoryConfigurationError | JAXBException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Optional<URL> getLocation() {
            return this.url;
        }

        @Override
        public void setLocation(URL location) {
            this.url = Optional.ofNullable(location);
        }

    }

    // the interceptor sits between the input stream or the items
    // by default, it does not intercept anything (see subclasses for details)
    class Interceptor {

        // when completed
        void cacheSource() throws MalformedURLException, IOException { }

        // cache unmarshalled item
        void cache(T item) { }

        // cache raw input stream with a tee
        InputSource intercept(InputSource input) {
            return input;
        }

    }

    /**
     * Indicates the caching strategy when the stream is read several times.
     * The cache is effective only when the stream is read entirely.
     *
     * @author Philippe Poulard
     */
    public enum CacheStrategy {

        // TODO : add encryptedLocalTmpFileCache, for sensible data

        /**
         * Doesn't cache anything. The same URL will be used for every stream read.
         */
        noCache {
            @Override
            <U> JAXBStream<U>.Interceptor newInterceptor(JAXBStream<U> jaxbStream) {
                return jaxbStream.new Interceptor();
            }
        },
        /**
         * Store unmarshalled items in memory, suitable for small amount of data.
         */
        memoryCache {
            @Override
            <U> JAXBStream<U>.Interceptor newInterceptor(JAXBStream<U> jaxbStream) {
                LOG.info(() -> "📝 Starting storing " + jaxbStream.clazz.getName() + " items in memory");
                return jaxbStream.new Interceptor() {

                    List<U> list = new LinkedList<>();

                    @Override
                    public void cache(U item) {
                        this.list.add(item);
                    }

                    @Override
                    public void cacheSource() {
                        LOG.info(() -> "📝 Will use memory for " + jaxbStream.clazz.getName() + " items.");
                        jaxbStream.cacheStrategy = CacheStrategy.noCache; // to be coherent
                        jaxbStream.source = jaxbStream.new ListSource(this.list);
                        jaxbStream.cleaner = () -> {
                            list.clear();
                            jaxbStream.cleaner = () -> false;
                            return true;
                        };
                    }

                };
            }
        },
        /**
         * Store the XML stream in a temporary local file, suitable for large amount of data,
         * not suitable for sensitive data.
         * The next reads will use the temporary file.
         * The temporary file is deleted on exit.
         */
        localTmpFileCache {
            @Override
            <U> JAXBStream<U>.Interceptor newInterceptor(JAXBStream<U> jaxbStream) {
                File tmp = safeCall(() -> File.createTempFile("JAXBStream_", ".xml"));
                tmp.deleteOnExit();
                LOG.info(() -> "🗄 Starting storing " + jaxbStream.clazz.getName() + " items in temp file " + tmp);
                return jaxbStream.new Interceptor() {

                    @Override
                    InputSource intercept(InputSource input) {
                        try {
                            if (input.getCharacterStream() == null) {
                                return new InputSource(
                                    new TeeInputStream(input.getByteStream(), new FileOutputStream(tmp))
                                );
                            } else {
                                return new InputSource(
                                    new TeeReader(input.getCharacterStream(), new FileWriter(tmp))
                                );
                            }
                        } catch (IOException e) {
                            return doThrow(e);
                        }
                    }

                    @Override
                    public void cacheSource() throws IOException {
                        jaxbStream.cacheStrategy = noCache;
                        jaxbStream.source = jaxbStream.new StreamSource(tmp.toURI().toURL());
                        jaxbStream.cleaner = () -> {
                            tmp.delete();
                            jaxbStream.cleaner = () -> false;
                            return true;
                        };
                    }

                };
            }
        };

        // create the relevant interceptor according to the cache strategy
        abstract <U> JAXBStream<U>.Interceptor newInterceptor(JAXBStream<U> jaxbStream);

    }

}
