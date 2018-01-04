package ml.alternet.properties.mojo;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ml.alternet.properties.Generator;

/**
 * Goal which generate Java source code from a property file.
 */
@Mojo( name = "generate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES )
public class PropBindMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project.build.directory}/generated-sources/prop-bind",
            property = "outputDir", required = true )
    public File outputDirectory;

    @Parameter( defaultValue = "${basedir}/src/main/properties", property = "properties", required = true )
    public File propertiesDirectory;

    @Parameter( readonly = true, defaultValue = "${project}" )
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("generate with\n" + propertiesDirectory + "\nto : " + outputDirectory);
        new Generator()
            .setPropertiesTemplatesDirectory(propertiesDirectory)
            .setOutputDirectory(outputDirectory)
            .generate();
        this.project.addCompileSourceRoot( outputDirectory.getAbsolutePath() );
        if ( getLog().isInfoEnabled() ) {
            getLog().info( "Source directory: " + outputDirectory + " added." );
        }
    }

}
