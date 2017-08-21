package ml.alternet.security.web.jetty.auth;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ml.alternet.misc.Thrower;
import ml.alternet.security.auth.CryptFormat;

public interface CryptFormatAware {

    void setCryptFormats(List<CryptFormat> formats);

    default void setCryptFormats(CryptFormat... formats) {
        setCryptFormats(Arrays.asList(formats));
    }

    default void setCryptFormats(String... cryptFormatClasses) {
        setCryptFormats(
                Arrays.asList(cryptFormatClasses).stream()
                .map(s -> {
                    try {
                        return (CryptFormat) Class.forName(s).newInstance();
                    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                        return Thrower.doThrow(e);
                    }
                })
                .collect(Collectors.toList())
        );
    }

}
