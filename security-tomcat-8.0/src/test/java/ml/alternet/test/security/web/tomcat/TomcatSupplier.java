package ml.alternet.test.security.web.tomcat;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.catalina.startup.Tomcat;

public interface TomcatSupplier extends Supplier<Tomcat> {

    default Tomcat get() {
        Tomcat tomcat = new Tomcat();
        tomcat.getEngine().setName(
            // avoid warning messages
            tomcat.getEngine().getName() + "-" + Objects.hashCode(tomcat.getEngine())
            // see https://github.com/spring-projects/spring-boot/pull/160
        );
        return tomcat;
    }

}
