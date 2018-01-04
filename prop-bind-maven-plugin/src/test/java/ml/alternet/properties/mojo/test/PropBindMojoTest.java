package ml.alternet.properties.mojo.test;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import ml.alternet.properties.mojo.PropBindMojo;

public class PropBindMojoTest extends AbstractMojoTestCase {

//    public void testGenerate() throws Exception {
//        File pom = getTestFile("src/it/simple-it/pom.xml");
//        assertNotNull(pom);
//        assertTrue(pom.exists());
//
//
//        File pluginPom = new File( getBasedir(), "pom.xml" );
//        Xpp3Dom pluginPomDom = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( pluginPom ) );
//        String artifactId = pluginPomDom.getChild( "artifactId" ).getValue();
////        String groupId = pluginPomDom.getChild( "groupId" ).getValue();
////        String version = pluginPomDom.getChild( "version" ).getValue();
//
//        PlexusConfiguration pluginConfiguration = extractPluginConfiguration( artifactId, pom );
//        PropBindMojo mojo = new PropBindMojo();
//
//        mojo = (PropBindMojo) configureMojo(mojo, pluginConfiguration);
//        assertNotNull(mojo);
//
////        mojo = (PropBindMojo) lookupEmptyMojo("generate", pluginPom);
//
//
//
//
////        mojo.execute();
//    }

}
