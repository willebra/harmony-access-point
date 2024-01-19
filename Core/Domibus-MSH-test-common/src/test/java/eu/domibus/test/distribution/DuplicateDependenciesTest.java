package eu.domibus.test.distribution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import distribution.DomibusArtifact;
import distribution.DomibusArtifactWithDependencies;
import distribution.DuplicateDependenciesChecker;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 5.0.8
 */
public class DuplicateDependenciesTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DuplicateDependenciesTest.class);

    @Test
    public void checkDuplicateDependencies() {
        DuplicateDependenciesChecker duplicateDependenciesChecker = new DuplicateDependenciesChecker();
        final DomibusArtifactWithDependencies artifactDependencies = createArtifactDependencies();

        List<List<String>> equivalentDependencies = new ArrayList<>();
        List<String> equivalentDependenciesSet1 = new ArrayList<>();
        equivalentDependencies.add(equivalentDependenciesSet1);

        duplicateDependenciesChecker.checkDuplicateDependencies(artifactDependencies, null, equivalentDependencies);
    }

    @Test
    public void checkDuplicateDependenciesAgainstServerDependencies() {
        DuplicateDependenciesChecker duplicateDependenciesChecker = new DuplicateDependenciesChecker();

        //main artifact
        final DomibusArtifactWithDependencies artifactWithDependencies = createArtifactDependencies();

        List<DomibusArtifact> serverDependencies = new ArrayList<>();
        //adding a duplicate dependency
        DomibusArtifact server1 = new DomibusArtifact("eu.domibus", "artifact1", "1.0", "compile");
        DomibusArtifact server2 = new DomibusArtifact("eu.domibus", "server1", "1.0", "compile");
        serverDependencies.add(server1);
        serverDependencies.add(server2);
        final DomibusArtifactWithDependencies serverArtifactWithDependencies = new DomibusArtifactWithDependencies("tomcat", serverDependencies);

        duplicateDependenciesChecker.checkDuplicateDependencies(artifactWithDependencies, serverArtifactWithDependencies, null);
    }

    @Test
    public void checkDuplicateDependenciesWithEquivalentDependencies() {
        DuplicateDependenciesChecker duplicateDependenciesChecker = new DuplicateDependenciesChecker();

        final DomibusArtifactWithDependencies artifactDependencies = createArtifactDependencies();

        List<List<String>> equivalentDependencies = new ArrayList<>();
        List<String> equivalentDependenciesSet1 = new ArrayList<>();
        equivalentDependenciesSet1.add("eu.domibus:artifact1");
        equivalentDependenciesSet1.add("eu.domibus:artifact2");
        equivalentDependencies.add(equivalentDependenciesSet1);

        Assert.assertThrows(RuntimeException.class, () -> duplicateDependenciesChecker.checkDuplicateDependencies(artifactDependencies, null, equivalentDependencies));
    }

    @Test
    public void serializeFromAndToJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        final DomibusArtifactWithDependencies artifactDependencies = createArtifactDependencies();
        final String jsonValue = objectMapper.writeValueAsString(artifactDependencies);
        LOG.info("ArtifactWithDependencies as JSON: [{}]", jsonValue);

        final DomibusArtifactWithDependencies artifactWithDependencies = objectMapper.readValue(jsonValue, DomibusArtifactWithDependencies.class);
        Assert.assertEquals(artifactWithDependencies.getArtifacts().size(), artifactWithDependencies.getArtifacts().size());
    }

    private DomibusArtifactWithDependencies createArtifactDependencies() {
        final List<DomibusArtifact> artifacts = createArtifacts();
        return new DomibusArtifactWithDependencies("myArtifact", artifacts);
    }

    private List<DomibusArtifact> createArtifacts() {
        DomibusArtifact artifact1 = new DomibusArtifact("eu.domibus", "artifact1", "1.0", "compile");
        DomibusArtifact artifact2 = new DomibusArtifact("eu.domibus", "artifact2", "1.1", "compile");
        DomibusArtifact artifact3 = new DomibusArtifact("eu.domibus", "artifact3", "2.0", "compile");
        DomibusArtifact artifact4 = new DomibusArtifact("eu.domibus", "artifact4", "3.1", "compile");

        List<DomibusArtifact> artifacts = new ArrayList<>();
        artifacts.add(artifact1);
        artifacts.add(artifact2);
        artifacts.add(artifact3);
        artifacts.add(artifact4);
        return artifacts;
    }
}
