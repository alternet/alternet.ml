package ml.alternet.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.example.conf.generated.test.advanced.Conf;
import org.testng.annotations.Test;

public class BindWithAdvancedGeneratedClassesTest {

    @Test
    public void customProperties_shouldBe_unmarshalled() throws IOException, URISyntaxException {
        Conf conf = Conf.unmarshall(org.example.conf.generated.test.Conf.class.getResourceAsStream("conf.properties"));

        assertThat(conf.gui.window.width).isEqualTo(500);
        assertThat(conf.gui.window.height).isEqualTo((short) 300);

        assertThat(conf.db.account.login).isEqualTo("theLogin");
        assertThat(conf.db.account.password).isEqualTo("thePassword".toCharArray());

    }

    @Test
    public void dynamicProperties_shouldBe_unmarshalled() throws IOException, URISyntaxException {
        Conf conf = Conf.unmarshall(org.example.conf.generated.test.Conf.class.getResourceAsStream("conf-dynamic.properties"));

        assertThat(conf.gui.window.width).isEqualTo(500);
        assertThat(conf.gui.window.height).isEqualTo((short) 300);

        // loaded from a separate file
        assertThat(conf.db.account.login).isEqualTo("theLogin");
        assertThat(conf.db.account.password).isEqualTo("thePassword".toCharArray());

        assertThat(conf.user).isNull();
    }

    @Test
    public void defaultProperties_shouldBe_unmarshalled() throws IOException, URISyntaxException {
        Conf conf = Conf.unmarshall(System.getProperties());
        conf.update(org.example.conf.generated.test.Conf.class.getResourceAsStream("conf-dynamic.properties"));

        assertThat(conf.gui.window.width).isEqualTo(500);
        assertThat(conf.gui.window.height).isEqualTo((short) 300);

        // loaded from a separate file
        assertThat(conf.db.account.login).isEqualTo("theLogin");
        assertThat(conf.db.account.password).isEqualTo("thePassword".toCharArray());

        assertThat(conf.user).isNotNull();
        assertThat(conf.user.name).isEqualTo(System.getProperty("user.name"));
        assertThat(conf.user.dir).isEqualTo(new File(System.getProperty("user.dir")));

    }

}
