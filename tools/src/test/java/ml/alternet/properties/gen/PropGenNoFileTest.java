package ml.alternet.properties.gen;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import ml.alternet.misc.Type;
import ml.alternet.properties.Generator;

public class PropGenNoFileTest {

    static class DumpOutputFactory implements Function<Type, OutputStream> {

        Map<Type, ByteArrayOutputStream> code = new HashMap<>();

        @Override
        public OutputStream apply(Type cl) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
            code.put(cl, buf);
            return buf;
        }

        public void flush() {
            code.forEach((cl, buf) -> {
                System.out.println(">>>> START " + cl);
                System.out.println(new String(buf.toByteArray(), StandardCharsets.UTF_8));
                System.out.println("<<<< END " + cl + "\n");
            });
        }

    }

    @Test
    public void test() {
        DumpOutputFactory of = new DumpOutputFactory();
        String template = "/ml/alternet/properties/gen/conf-cases.template.properties";
        InputStream in = PropGenNoFileTest.class.getResourceAsStream(template);
        new Generator().setOutputFactory(of)
            .setPropertiesTemplates(Stream.of(in))
            .generate();
        of.flush();
    }

}
