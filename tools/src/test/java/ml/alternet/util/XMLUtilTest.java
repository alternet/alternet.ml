package ml.alternet.util;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

public class XMLUtilTest {

    @Test
    public void applicationXML_ShouldBe_XMLMimeType() {
    	Assertions.assertThat(XMLUtil.isXMLMimeType("application/xml")).isTrue();
    }

    @Test
    public void textXML_ShouldBe_XMLMimeType() {
    	Assertions.assertThat(XMLUtil.isXMLMimeType("text/xml")).isTrue();
    }

    @Test
    public void imageSVGXML_ShouldBe_XMLMimeType() {
    	Assertions.assertThat(XMLUtil.isXMLMimeType("image/svg+xml")).isTrue();
    }

    @Test
    public void applicationXHTMLXML_ShouldBe_XMLMimeType() {
    	Assertions.assertThat(XMLUtil.isXMLMimeType("application/xhtml+xml")).isTrue();
    }

    @Test
    public void applicationXMLDTD_ShouldNotBe_XMLMimeType() {
    	Assertions.assertThat(XMLUtil.isXMLMimeType("application/xml-dtd")).isFalse();
    }

    @Test
    public void applicationXMLExternalParsedEntity_ShouldNotBe_XMLMimeType() {
    	Assertions.assertThat(XMLUtil.isXMLMimeType("application/xml-external-parsed-entity")).isFalse();
    }

    @Test
    public void qname_ShouldNot_HavePrefix() {
    	Assertions.assertThat(XMLUtil.getQualifiedName(new QName("urn:foo", "bar", XMLConstants.DEFAULT_NS_PREFIX)).equals("bar"));
    }

    @Test
    public void qname_Should_HavePrefix() {
    	Assertions.assertThat(XMLUtil.getQualifiedName(new QName("urn:foo", "bar", "pref")).equals("pref:bar"));
    }

    @Test
    public void unboundLocalName_ShouldNot_HavePrefix() {
    	Assertions.assertThat(XMLUtil.getQualifiedName(new QName("bar")).equals("bar"));
    }

    @Test
    public void unboundQName_ShouldNot_HavePrefix() {
    	Assertions.assertThat(XMLUtil.getQualifiedName(new QName(XMLConstants.NULL_NS_URI, "bar", XMLConstants.DEFAULT_NS_PREFIX)).equals("bar"));
    }

}
