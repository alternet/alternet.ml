package ml.alternet.properties;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.conf.handmade.test.Geo;
import org.example.conf.handmade.test.MyConf;
import org.example.conf.handmade.test.Status;
import org.example.conf.handmade.test.Stuff;
import org.example.conf.handmade.test.MyConf.MyNestedConf.Ext;
import org.example.conf.handmade.test.MyConf.MyNestedConf.NestedConfMember;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ml.alternet.misc.Thrower;
import ml.alternet.properties.$;
import ml.alternet.properties.Binder;
import ml.alternet.properties.Binder.Adapter;

public class BindWithHandmadeClassesTest {

    static Document parseXml(String xml) {
        try {
            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(
                        new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            return Thrower.doThrow(e);
        }
    }

    static Point parsePoint(String coords) {
        String[] xy = coords.split("[{;}]");
        return new Point(Integer.parseInt(xy[1]), Integer.parseInt(xy[2]));
    }

    @Test
    public void unmarshallProperties() throws IOException {
        MyConf conf = Binder.unmarshall(
            BindWithHandmadeClassesTest.class.getResourceAsStream("conf-cases.properties"),
            MyConf.class,
            Adapter.map(Document.class, BindWithHandmadeClassesTest::parseXml),
            Adapter.map(Point.class, BindWithHandmadeClassesTest::parsePoint),
            Adapter.map("subConf.file", File::new),
            Adapter.mapList("subConf.points", BindWithHandmadeClassesTest::parsePoint),
            Adapter.mapList("subConf.booleans", Boolean::valueOf),
            Adapter.mapList("subConf.integers", Integer::parseInt),
            Adapter.map("map.*.geo", Geo::parse)
        );

        assertThat(conf.listOfStrings).containsExactly("a", "b", "cdef ghi", "jkl", "mnoP");

        assertThat(conf.b1).isTrue();
        assertThat(conf.b2).isFalse();

        assertThat(conf.s1).isEqualTo((short) 1);
        assertThat(conf.s2).isEqualTo((short) 2);

        assertThat(conf.subConf).isInstanceOf(MyConf.MyNestedConf.class);
        assertThat(conf.subConf.string).isEqualTo("the actual value");
        assertThat(conf.subConf.someOtherString).isEqualTo("some other value");
        assertThat(conf.subConf.tmpFile).isEqualTo(new File("file:///path/to/some/tmp.txt"));
        assertThat(conf.subConf.<File> $("file")).isEqualTo(new File("file:///path/to/some/other/tmp.txt"));
        assertThat(conf.subConf.<List<Point>> $("points")).containsExactly(new Point(-1, 2), new Point(0, 3), new Point(-2, 1), new Point(-2, 0));
        assertThat(conf.subConf.<List<Boolean>> $("booleans")).containsExactly(true, true, false, true);
        assertThat(conf.subConf.unsetValue).isNull();

        assertThat(conf.subConf.subConf).isInstanceOf(MyConf.MyNestedConf.NestedConfWithGlobal.class);
        assertThat(conf.subConf.subConf.$).isEqualTo("global Value");
        assertThat(conf.subConf.subConf.string).isEqualTo("this is the VALUE");
        assertThat(conf.subConf.subConf.anotherString).isEqualTo("another String");
        assertThat(conf.subConf.subConf.listOfStuff).containsExactly(new Stuff("list"), new Stuff("Of"), new Stuff("Stuff"));
        assertThat(conf.subConf.subConf.end).isEqualTo(Status.Inactive);

        assertThat(conf.start).isEqualTo(Status.Active);
        assertThat(conf.unknown).isNull();

        assertThat(conf.website).isEqualTo(new URL("http://example.com/"));
        assertThat(conf.someStuff).isEqualTo(new Stuff("that Stuff"));

        assertThat(conf.i1).isEqualTo(10001);
        assertThat(conf.i2).isEqualTo(10002);

        assertThat(conf.subConf.<NestedConfMember> $("nestedConfMember").foo).isEqualTo(new Stuff("STUFF"));
        assertThat(conf.subConf.<NestedConfMember> $("nestedConfMember").bar).isEqualTo(123.456);

        assertThat(conf.subConf.subConf.anotherString).isEqualTo("another String");

        assertThat(conf.subConf.<$> $("level").<$> $("subLevel").<String> $("subSubLevel")).isEqualTo("unknown level paths");
        assertThat(conf.subConf.<String> $("level.subLevel.subSubLevel")).isEqualTo("unknown level paths");
        assertThat(conf.subConf.<$> $("level.subLevel").<String> $("subSubLevel")).isEqualTo("unknown level paths");

        assertThat(conf.subConf.<Ext> $("ext").foo).isNull();
        assertThat(conf.subConf.<NestedConfMember> $("ext").bar).isEqualTo(456.789);
        assertThat(conf.subConf.<Ext> $("ext").xml.getDocumentElement().getTextContent()).isEqualTo("need an adapter");
        assertThat(conf.subConf.<Ext> $("ext").xml.getDocumentElement().getAttribute("foo")).isEqualTo("xml attribute");

        assertThat(conf.subConf.<Ext> $("ext").points).containsExactly(new Point(0, 1), new Point(1, 2), new Point(-1, 0), new Point(-1, -1));

        assertThat(conf.map.<Geo> $("paris.geo")).isEqualTo(new Geo(48.864716, 2.349014));

    }

}
