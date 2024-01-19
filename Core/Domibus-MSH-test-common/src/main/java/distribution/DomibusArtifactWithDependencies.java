package distribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 5.0.8
 */
public class DomibusArtifactWithDependencies {

    protected String artifactId;
    protected List<DomibusArtifact> artifacts;

    //a map with key=dependency name without version, value=list of dependency names with version
    protected Map<String, List<String>> dependenciesAggregated = new HashMap<>();

    //a list of all dependencies without version
    protected List<String> dependenciesListWithoutVersion = new ArrayList<>();

    public DomibusArtifactWithDependencies() {
    }

    public DomibusArtifactWithDependencies(String artifactId, List<DomibusArtifact> artifacts) {
        this.artifactId = artifactId;
        this.artifacts = artifacts;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public List<DomibusArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<DomibusArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    public Map<String, List<String>> getDependenciesAggregated() {
        return dependenciesAggregated;
    }

    public void setDependenciesAggregated(Map<String, List<String>> dependenciesAggregated) {
        this.dependenciesAggregated = dependenciesAggregated;
    }

    public List<String> getDependenciesListWithoutVersion() {
        return dependenciesListWithoutVersion;
    }

    public void setDependenciesListWithoutVersion(List<String> dependenciesListWithoutVersion) {
        this.dependenciesListWithoutVersion = dependenciesListWithoutVersion;
    }
}
