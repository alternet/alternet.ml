package ml.alternet.properties.gen;

import java.io.File;

import org.testng.annotations.Test;

import ml.alternet.properties.Generator;

public class PropGenTest {

    /**
     * Use : src/test/properties/*.properties
     * to generate to : target/generated-test-sources/prop-bind
     *
     * NOTE : after code update, copy the generated classes to
     *      src/test/java/org/example/conf/generated/test
     *      src/test/java/org/example/conf/generated/test/advanced
     *      src/test/java/org/example/conf/generated/test/usescases
     *      src/test/java/org/example/conf/generated/test/usescases/status
     * accordingly
     */
    @Test
    public void generator_should_createJavaCode() {
        File templatePropertiesDirectory = new File("src/test/properties/");
        File outputDirectory = new File("target/generated-test-sources/prop-bind");
        new Generator()
//            .setSchemaFile(new File(templatePropertiesDirectory, "config.template.properties"), null)
            .setPropertiesTemplatesDirectory(templatePropertiesDirectory)
            .setOutputDirectory(outputDirectory)
            .generate();
    }

}
