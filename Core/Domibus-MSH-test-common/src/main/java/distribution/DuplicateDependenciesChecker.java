package distribution;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 5.0.8
 */
public class DuplicateDependenciesChecker {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DuplicateDependenciesChecker.class);

    /**
     * Checks for duplicate dependencies between the main artifact and the other artifact
     * Typically the main artifact is a Domibus extension/plugin extension and the other artifact is the Tomcat distribution
     *
     * @param artifactWithDependencies      The main artifact for which we check the dependencies
     * @param otherArtifactWithDependencies The other artifact to which we compare the dependencies of the main artifact
     * @param listOfEquivalentDependencies  The list of equivalent dependencies
     */
    public void checkDuplicateDependencies(DomibusArtifactWithDependencies artifactWithDependencies,
                                           DomibusArtifactWithDependencies otherArtifactWithDependencies,
                                           List<List<String>> listOfEquivalentDependencies) {
        LOG.info("Checking for duplicate dependencies in artifact [{}]", artifactWithDependencies.getArtifactId());

        //compute dependencies for the main artifact
        computeDependencies(artifactWithDependencies);

        LOG.info("Dependency list aggregated for artifact [{}]: [{}]", artifactWithDependencies.getArtifactId(), artifactWithDependencies.getDependenciesAggregated());

        //check first for duplicates in the main artifact
        checkForDuplicateDependencies(artifactWithDependencies, null);
        checkForDuplicateDependenciesWithDifferentArtifactId(artifactWithDependencies, null, listOfEquivalentDependencies);

        //check for duplicates between the main artifact and the server artifact
        if(otherArtifactWithDependencies != null) {
            checkForDuplicateDependencies(artifactWithDependencies, otherArtifactWithDependencies);
            checkForDuplicateDependenciesWithDifferentArtifactId(artifactWithDependencies, otherArtifactWithDependencies, listOfEquivalentDependencies);
        }
    }

    protected void computeDependencies(DomibusArtifactWithDependencies artifactDependencies) {
        //a map with key=dependency name without version, value=list of dependency names with version
        Map<String, List<String>> dependenciesAggregated = new HashMap<>();

        //a list of all dependencies without version
        List<String> dependenciesListWithoutVersion = new ArrayList<>();

        for (DomibusArtifact artifact : artifactDependencies.getArtifacts()) {
            final String groupId = artifact.getGroupId();
            final String artifactId = artifact.getArtifactId();
            final String version = artifact.getVersion();
            String dependencyNameWithVersion = groupId + ":" + artifactId + "-" + version;

            final String dependencyNameWithoutVersion = groupId + ":" + artifactId;

            List<String> dependenciesWithVersion = dependenciesAggregated.get(dependencyNameWithoutVersion);
            if (dependenciesWithVersion == null) {
                dependenciesWithVersion = new ArrayList<>();
                dependenciesAggregated.put(dependencyNameWithoutVersion, dependenciesWithVersion);
            }
            dependenciesWithVersion.add(dependencyNameWithVersion);
            dependenciesListWithoutVersion.add(dependencyNameWithoutVersion);
        }
        artifactDependencies.setDependenciesAggregated(dependenciesAggregated);
        artifactDependencies.setDependenciesListWithoutVersion(dependenciesListWithoutVersion);
    }

    private void checkForDuplicateDependenciesWithDifferentArtifactId(DomibusArtifactWithDependencies artifactWithDependencies,
                                                                      DomibusArtifactWithDependencies otherArtifactWithDependencies,
                                                                      List<List<String>> listOfEquivalentDependencies) {
        if (CollectionUtils.isEmpty(listOfEquivalentDependencies)) {
            LOG.info("Skip checking for equivalent dependencies: no equivalent dependencies configured");
            return;
        }

        LOG.info("Checking for duplicate dependencies with different artifact ids in [{}]", artifactWithDependencies.getArtifactId());

        for (List<String> equivalentDependencies : listOfEquivalentDependencies) {
            checkForDuplicateDependencies(artifactWithDependencies, otherArtifactWithDependencies, equivalentDependencies);
        }

        LOG.info("No duplicate dependencies detected with different artifact id");
    }

    private void checkForDuplicateDependencies(DomibusArtifactWithDependencies artifactWithDependencies,
                                               DomibusArtifactWithDependencies otherArtifactWithDependencies,
                                               List<String> equivalentDependencies) {
        final List<String> totalDependenciesListWithoutVersion = new ArrayList<>();
        totalDependenciesListWithoutVersion.addAll(artifactWithDependencies.getDependenciesListWithoutVersion());
        if (otherArtifactWithDependencies != null) {
            totalDependenciesListWithoutVersion.addAll(otherArtifactWithDependencies.getDependenciesListWithoutVersion());
        }

        LOG.info("Checking for duplicate dependencies with different artifact id [{}]", equivalentDependencies);
        int numberOfMatches = 0;
        for (String equivalentDependency : equivalentDependencies) {
            if (totalDependenciesListWithoutVersion.contains(equivalentDependency)) {
                numberOfMatches++;
            }
        }
        if (numberOfMatches > 1) {
            String errorMessage = "Found duplicate dependencies with different artifact ids [" + equivalentDependencies + "] in artifact id [" + artifactWithDependencies.getArtifactId() + "]";
            if (otherArtifactWithDependencies != null) {
                errorMessage = errorMessage + " and server artifact [" + otherArtifactWithDependencies.getArtifactId() + "]";
            }
            throw new RuntimeException(errorMessage);
        }
        LOG.info("No duplicate dependencies with different artifact id [{}] found", equivalentDependencies);
    }

    /**
     * Check for duplicate dependencies having the same artifact id
     */
    private void checkForDuplicateDependencies(DomibusArtifactWithDependencies artifactWithDependencies,
                                               DomibusArtifactWithDependencies otherArtifactWithDependencies) throws RuntimeException {
        LOG.info("Checking for duplicate dependencies with the same artifact id in artifact [{}]", artifactWithDependencies.getArtifactId());

        Map<String, List<String>> totalDependenciesAggregated = new HashMap<>();
        totalDependenciesAggregated.putAll(artifactWithDependencies.getDependenciesAggregated());

        if (otherArtifactWithDependencies != null) {
            //aggregated the other dependencies in the total dependencies
            final Map<String, List<String>> dependenciesAggregated = otherArtifactWithDependencies.getDependenciesAggregated();
            for (Map.Entry<String, List<String>> otherDependencyEntry : dependenciesAggregated.entrySet()) {
                final String otherDependencyEntryKey = otherDependencyEntry.getKey();
                final List<String> otherDependencyEntries = otherDependencyEntry.getValue();

                List<String> dependenciesWithVersion = totalDependenciesAggregated.get(otherDependencyEntryKey);
                if (dependenciesWithVersion == null) {
                    dependenciesWithVersion = new ArrayList<>();
                    dependenciesAggregated.put(otherDependencyEntryKey, dependenciesWithVersion);
                }
                dependenciesWithVersion.addAll(otherDependencyEntries);
            }
        }

        //check if there are duplicate dependencies
        for (Map.Entry<String, List<String>> dependencyEntry : totalDependenciesAggregated.entrySet()) {
            final List<String> listOfDependenciesPerDependency = dependencyEntry.getValue();
            if (listOfDependenciesPerDependency.size() > 1) {
                String errorMessage = "Multiple dependencies detected [" + listOfDependenciesPerDependency + "] in " + artifactWithDependencies.getArtifactId();
                if (otherArtifactWithDependencies != null) {
                    errorMessage = errorMessage + " and " + otherArtifactWithDependencies.getArtifactId();
                }
                throw new RuntimeException(errorMessage);
            }
        }
        LOG.info("No duplicate dependencies detected with the same artifact id in artifact id [{}]", artifactWithDependencies.getArtifactId());
    }
}
