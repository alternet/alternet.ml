package ml.alternet.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.example.conf.generated.test.Conf;
import org.example.conf.generated.test.Mail;
import org.example.conf.generated.test.Synchro;
import org.example.conf.generated.test.Synchro.Synchro_.Some.Service.Category.Filter;
import org.testng.annotations.Test;

public class BindWithGeneratedClassesTest {

    @Test
    public void basicProperties_shouldBe_unmarshalled() throws IOException, URISyntaxException {
        Mail mailConf = Mail.unmarshall(Mail.class.getResourceAsStream("mail.properties"));

        assertThat(mailConf.mail.smtp.starttls.enable).isFalse();
        assertThat(mailConf.mail.smtp.auth).isFalse();
        assertThat(mailConf.mail.smtp.host).isEqualTo("smtp.example.org");
        assertThat(mailConf.mail.smtp.port).isEqualTo((byte) 25);
        assertThat(mailConf.mail.smtp.ssl.trust).isEqualTo("smtp.example.org");
        assertThat(mailConf.username).isEqualTo("mail-contact-rh@example.org");
        assertThat(mailConf.password).isEqualTo("myMailPassword".toCharArray());
        assertThat(mailConf.name.expediteur).isEqualTo("MailExample");
    }

    @Test
    public void complexProperties_shouldBe_unmarshalled() throws IOException, URISyntaxException {
        Synchro synchroConf = Synchro.unmarshall(Synchro.class.getResourceAsStream("config.properties"));

        assertThat(synchroConf.plugin.list.default_.$).isEqualTo("init");
        assertThat(synchroConf.plugin.file.default_).isEqualTo(new File("file:///path/to/file.txt"));
        assertThat(synchroConf.plugin.string).isNull();
        assertThat(synchroConf.db.driver).isEqualTo(com.fakesql.jdbc.Driver.class);
        assertThat(synchroConf.db.url).isEqualTo(new URI("jdbc:fakesql://localhost:3306/localdb?autoReconnect=true"));
        assertThat(synchroConf.synchro.some.service.url).isEqualTo(new URI("https://some-services.example.org/someService"));
        assertThat(synchroConf.synchro.some.service.login).isEqualTo("myServiceLogin");
        assertThat(synchroConf.synchro.some.service.pwd).isEqualTo("myServicePassword".toCharArray());
        assertThat(synchroConf.synchro.some.service.category.filter).containsExactly(Filter.CAT1, Filter.OTHER);
        assertThat(synchroConf.synchro.ldap.url).isEqualTo(new URI("ldaps://ildap.example.org"));
        assertThat(synchroConf.synchro.ldap.search.name).isEqualTo("ou=people,dc=example,dc=org");
        assertThat(synchroConf.synchro.ldap.search.filterExp.$).isEqualTo("(&(exampleLogin=\\$login)(entryStatus=valid)(objectclass=examplePerson))");
        assertThat(synchroConf.synchro.ldap.search.filterExp.uid).isEqualTo("(&(uid=\\$uid)(entryStatus=valid)(objectclass=examplePerson))");
        assertThat(synchroConf.synchro.ldap.unsafe.passwordCheck.disable).isTrue();
        assertThat(synchroConf.synchro.ldap.attribute.uid).isEqualTo("uid");
        assertThat(synchroConf.synchro.ldap.filter.mail).isEqualTo("mail");
        assertThat(synchroConf.synchro.ldap.filter.nom).isEqualTo("sn");
        assertThat(synchroConf.synchro.ldap.filter.prenom).isEqualTo("givenName");
        assertThat(synchroConf.synchro.ldap.unsafe.userNotFound.bypass).isFalse();
        assertThat(synchroConf.jndi.ds).isEqualTo(new URI("java:/comp/env/jdbc/someDB"));
        assertThat(synchroConf.ext.url).isEqualTo(new URI("https://ext.example.com/export.xml"));
    }

    @Test
    public void customProperties_shouldBe_unmarshalled() throws IOException, URISyntaxException {
        Conf conf = Conf.unmarshall(Conf.class.getResourceAsStream("conf.properties"));

        assertThat(conf.gui.window.width).isEqualTo(500);
        assertThat(conf.gui.window.height).isEqualTo((short) 300);

        assertThat(conf.gui.colors.foreground).isEqualTo(Color.decode("#000080"));

        assertThat(conf.db.account.login).isEqualTo("theLogin");
        assertThat(conf.db.account.password).isEqualTo("thePassword".toCharArray());

    }

    @Test
    public void dynamicProperties_shouldBe_unmarshalled() throws IOException, URISyntaxException {
        Conf conf = Conf.unmarshall(Conf.class.getResourceAsStream("conf-dynamic.properties"));

        assertThat(conf.gui.window.width).isEqualTo(500);
        assertThat(conf.gui.window.height).isEqualTo((short) 300);

        assertThat(conf.gui.colors.foreground).isEqualTo(Color.decode("#010203").brighter());

        // loaded from a separate file
        assertThat(conf.db.account.login).isEqualTo("theLogin");
        assertThat(conf.db.account.password).isEqualTo("thePassword".toCharArray());

    }

    @Test
    public void defaultProperties_shouldBe_unmarshalled() throws IOException, URISyntaxException {
        Conf conf = Conf.unmarshall(System.getProperties());
        conf = Conf.unmarshall(Conf.class.getResourceAsStream("conf-dynamic.properties"));

        assertThat(conf.gui.window.width).isEqualTo(500);
        assertThat(conf.gui.window.height).isEqualTo((short) 300);

        assertThat(conf.gui.colors.foreground).isEqualTo(Color.decode("#010203").brighter());

        // loaded from a separate file
        assertThat(conf.db.account.login).isEqualTo("theLogin");
        assertThat(conf.db.account.password).isEqualTo("thePassword".toCharArray());

    }

}
