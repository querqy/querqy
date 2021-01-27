package querqy.solr.it;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.SolrClientUtils;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

public class QuerqySolrContainer extends SolrContainer {

    private static final String PROP_PROJECT_BUILD_FINALNAME = "project.build.finalName";
    private static final String PROP_PROJECT_BUILD_DIRECTORY = "project.build.directory";
    private static final String PROP_QUERQY_SOLR_TEST_IMAGES = "solr.test.version";

    public static final String QUERQY_IT_CONFIGSET = "querqy-it";
    public static final String QUERQY_IT_COLLECTION_NAME = "querqy";

    /**
     * Returns the image names that have been configured to test with.
     */
    public static Collection<DockerImageName> getSolrTestVersions() {
        return Arrays.asList(System.getProperty(QuerqySolrContainer.PROP_QUERQY_SOLR_TEST_IMAGES).split(",")).stream()
            .map(image -> DockerImageName.parse(image))
            .collect(Collectors.toList());
    }


    /**
     * Sets up a Solr Docker container running the current querqy version
     */
    public QuerqySolrContainer(DockerImageName image) {
        super(image);

        checkMavenFailsafeSetup();

        String querqyBinaryPath = String.format("%s/%s-jar-with-dependencies.jar",
                System.getProperty(PROP_PROJECT_BUILD_DIRECTORY), System.getProperty(PROP_PROJECT_BUILD_FINALNAME));
        String querqyConfigurationPath = String.format("%s/test-classes/integration-test/%s",
                System.getProperty(PROP_PROJECT_BUILD_DIRECTORY), QUERQY_IT_CONFIGSET);

        // link querqy binary into container
        addFileSystemBind(querqyBinaryPath, "/opt/solr/server/solr-webapp/webapp/WEB-INF/lib/querqy.jar", BindMode.READ_ONLY);

        // link conf directory into container
        addFileSystemBind(querqyConfigurationPath,
                String.format("/opt/solr/server/solr/configsets/%s", QUERQY_IT_CONFIGSET), BindMode.READ_ONLY);
    }

    /**
     * Checks whether all properties needed have been configured via the
     * maven-failsafe-plugin
     */
    private void checkMavenFailsafeSetup() {
        Objects.requireNonNull(System.getProperty(PROP_PROJECT_BUILD_FINALNAME));
        Objects.requireNonNull(System.getProperty(PROP_PROJECT_BUILD_DIRECTORY));
    }

    /**
     * Overwrite how the Solr container is initialized. We use the internal solr
     * command to upload our whole mounted configset folder.
     */
    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {

        try {
            // upload configset that we linked into the container
            ExecResult result = execInContainer("solr", "zk", "upconfig", "-z", "localhost:9983", "-n",
                    QUERQY_IT_CONFIGSET, "-d",
                    String.format("/opt/solr/server/solr/configsets/%s", QUERQY_IT_CONFIGSET));
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(
                        String.format("Could not upload %s configset to Zookeeper: %s", QUERQY_IT_CONFIGSET, result));
            }

            // create collection with said configset
            SolrClientUtils.createCollection(getContainerIpAddress(), getSolrPort(), QUERQY_IT_COLLECTION_NAME,
                    QUERQY_IT_CONFIGSET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
