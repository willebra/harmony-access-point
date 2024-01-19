package distribution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author François Gautier
 * @author Cosmin Baciu
 * @since 5.0
 * <p>
 * Check for duplicate dependencies with different version
 * <p>
 * The build will fail if the folder /target/.../WEB-INF/lib has a duplicate
 */
@Named("duplicateDependenciesRule")
public class DuplicateDependenciesRule extends AbstractEnforcerRule {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DuplicateDependenciesRule.class);
    public static final String TOMCAT = "tomcat";
    public static final String WEBLOGIC = "weblogic";
    public static final String WILDFLY = "wildfly";

    @Inject
    private MavenProject project;

    protected ObjectMapper objectMapper = new ObjectMapper();

    private List<String> equivalentDuplicateDependencies;


    /**
     * Indicates if the dependency checker should cross-check the dependencies with specific dependencies possible values: tomcat,weblogic,wildfly
     */
    private List<String> checkAgainstServerDependencies;

    public void execute() throws EnforcerRuleException {
        final List<List<String>> configuredEquivalentDependencies = parseEquivalentDependencies(equivalentDuplicateDependencies);
        LOG.info("Configured equivalentDuplicateDependencies: " + configuredEquivalentDependencies);
        LOG.info("Target Folder: " + project.getBuild().getDirectory());
        final String artifactId = project.getArtifactId();
        LOG.info("ArtifactId: " + artifactId);
        LOG.info("Project: " + project);

        //getting all dependencies with scope compile
        final List<DomibusArtifact> artifactsWithCompileScope = project.getArtifacts().stream()
                .filter(artifact -> artifact.getScope().equals("compile"))
                .map(artifact -> new DomibusArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getScope()))
                .collect(Collectors.toList());


        //the rule is running for a server
        final boolean checkDependenciesForServer = checkDependenciesForServer();
        if (checkDependenciesForServer) {
            LOG.info("Checking dependencies for a server distribution with artifact id [" + artifactId + "]");

            DomibusArtifactWithDependencies domibusArtifactWithDependencies = new DomibusArtifactWithDependencies(artifactId, artifactsWithCompileScope);
            DuplicateDependenciesChecker duplicateDependenciesChecker = new DuplicateDependenciesChecker();
            duplicateDependenciesChecker.checkDuplicateDependencies(domibusArtifactWithDependencies, null, configuredEquivalentDependencies);

            writeServerDependencyFileToWebInfClasses(artifactId, domibusArtifactWithDependencies);
            return;
        }

        //the rule is running for a plugin/extension
        LOG.info("Checking dependencies for a extension/plugin with artifact id [" + artifactId + "]");

        if (checkAgainstServerDependencies(TOMCAT)) {
            final String dependencyFileName = TOMCAT + "-dependencies.txt";
            checkAgainstServer(dependencyFileName, artifactId, artifactsWithCompileScope, configuredEquivalentDependencies);
        }
        if (checkAgainstServerDependencies(WEBLOGIC)) {
            final String dependencyFileName = WEBLOGIC + "-dependencies.txt";
            checkAgainstServer(dependencyFileName, artifactId, artifactsWithCompileScope, configuredEquivalentDependencies);
        }
        if (checkAgainstServerDependencies(WILDFLY)) {
            final String dependencyFileName = WILDFLY + "-dependencies.txt";
            checkAgainstServer(dependencyFileName, artifactId, artifactsWithCompileScope, configuredEquivalentDependencies);
        }

    }

    private void checkAgainstServer(String dependencyFileName, String artifactId, List<DomibusArtifact> artifactsWithCompileScope, List<List<String>> configuredEquivalentDependencies) {
        LOG.info("Checking dependencies for a extension/plugin with artifact id [{}] against [{}]", artifactId, dependencyFileName);

        final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(dependencyFileName);
        if (resourceAsStream == null) {
            throw new RuntimeException("Could not find [" + dependencyFileName + "] in classpath");
        }
        String serverDependenciesJsonContent = null;
        try {
            serverDependenciesJsonContent = IOUtils.toString(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException("Could not get the content of [" + dependencyFileName + "]");
        }
        final DomibusArtifactWithDependencies serverArtifactWithDependencies = fromJson(serverDependenciesJsonContent);

        DomibusArtifactWithDependencies domibusArtifactWithDependencies = new DomibusArtifactWithDependencies(artifactId, artifactsWithCompileScope);
        DuplicateDependenciesChecker duplicateDependenciesChecker = new DuplicateDependenciesChecker();
        duplicateDependenciesChecker.checkDuplicateDependencies(domibusArtifactWithDependencies, serverArtifactWithDependencies, configuredEquivalentDependencies);
    }

    protected boolean checkAgainstServerDependencies(String serverName) {
        if (CollectionUtils.isEmpty(checkAgainstServerDependencies)) {
            return false;
        }
        if (checkAgainstServerDependencies.contains(serverName)) {
            return true;
        }
        return false;
    }

    protected void writeServerDependencyFileToWebInfClasses(String artifactId, DomibusArtifactWithDependencies domibusArtifactWithDependencies) {
        final String domibusArtifactWithDependenciesJson = toJson(domibusArtifactWithDependencies);
        String webInfClasses = project.getBasedir().getPath() + "/target/" + project.getArtifactId() + "-" + project.getVersion() + "/WEB-INF/classes";
        final String serverDependencyFileName = getServerDependencyFileName(artifactId);

        String serverDependencyFilePath = webInfClasses + "/" + serverDependencyFileName;
        try {
            LOG.info("Writing the server artifact [{}] dependencies to [{}]", artifactId, serverDependencyFilePath);
            FileUtils.writeStringToFile(new File(serverDependencyFilePath), domibusArtifactWithDependenciesJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error writing server dependencies to [" + serverDependencyFilePath + "]", e);
        }
    }

    protected String getServerDependencyFileName(String artifactId) {
        final String serverName = getServerName(artifactId);
        return serverName + "-dependencies.txt";
    }

    protected String getServerName(String artifactId) {
        if (StringUtils.contains(artifactId, TOMCAT)) {
            return TOMCAT;
        }
        if (StringUtils.contains(artifactId, WEBLOGIC)) {
            return WEBLOGIC;
        }
        if (StringUtils.contains(artifactId, WILDFLY)) {
            return WILDFLY;
        }
        throw new RuntimeException("Could not recognize server based on artifact id [" + artifactId + "]");
    }

    protected String toJson(DomibusArtifactWithDependencies domibusArtifactWithDependencies) {
        try {
            return objectMapper.writeValueAsString(domibusArtifactWithDependencies);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize DomibusArtifactWithDependencies to json", e);
        }
    }

    protected DomibusArtifactWithDependencies fromJson(String json) {
        try {
            return objectMapper.readValue(json, DomibusArtifactWithDependencies.class);
        } catch (JsonProcessingException e) {
            LOG.error("Could not deserialize DomibusArtifactWithDependencies from json: [{}]", json);
            throw new RuntimeException("Could not deserialize DomibusArtifactWithDependencies from json", e);
        }
    }


    /**
     * Returns true if the dependency checker runs for Tomcat, WebLogic or WildFly distribution
     */
    protected boolean checkDependenciesForServer() {
        return CollectionUtils.isEmpty(checkAgainstServerDependencies);
    }

    protected List<List<String>> parseEquivalentDependencies(List<String> equivalentDuplicateDependenciesSeparatedByComma) {
        List<List<String>> result = new ArrayList<>();

        for (String equivalentDuplicateDependencyStringSeparatedByComma : equivalentDuplicateDependenciesSeparatedByComma) {
            final List<String> equivalentDuplicateDependencies = Arrays.stream(StringUtils.split(equivalentDuplicateDependencyStringSeparatedByComma, ","))
                    .map(dependencyName -> StringUtils.trim(dependencyName))
                    .collect(Collectors.toList());
            result.add(equivalentDuplicateDependencies);
        }
        return result;
    }


    /**
     * If your rule is cacheable, you must return a unique id when parameters or conditions
     * change that would cause the result to be different. Multiple cached results are stored
     * based on their id.
     * <p>
     * The easiest way to do this is to return a hash computed from the values of your parameters.
     * <p>
     * If your rule is not cacheable, then you don't need to override this method or return null
     */
    @Override
    public String getCacheId() {
        //no hash on boolean...only parameter so no hash is needed.
        return null;
    }

    /**
     * A good practice is provided toString method for Enforcer Rule.
     * <p>
     * Output is used in verbose Maven logs, can help during investigate problems.
     *
     * @return rule description
     */
    @Override
    public String toString() {
        return "duplicateDependenciesRule: [fail the build if a dependency is found twice with different version in the folder WEB-INF/lib";
    }


}
