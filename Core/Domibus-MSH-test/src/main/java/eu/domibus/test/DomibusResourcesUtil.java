package eu.domibus.test;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DomibusResourcesUtil {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusResourcesUtil.class);

    public static void copyResourceToDisk(File domibusConfigLocation, String resourceFolder, String destFolder) throws IOException {
        LOG.info("Copying from [{}] to [{}]", resourceFolder, destFolder);

        final File destDirectory = new File(domibusConfigLocation, destFolder);
        FileUtils.forceMkdir(destDirectory);

        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        final Resource[] resourceList = resolver.getResources("classpath*:" + resourceFolder + "/*");
        for (Resource resource : resourceList) {
            final String fileName = resource.getFilename();
            final String url = resource.getURL().toString();

            //we only take into account resources from the domibus-msh-test module(the case when the AbstractIT is used by a plugin/extension)
            //when the AbstractIT is running in the tomcat module we don't have to copy the resources because they are already in the src/test/resources directory and they are copied automatically
            if (!StringUtils.containsIgnoreCase(url, "domibus-msh-test")) {
                LOG.debug("Skip copying file [{}]", url);
                continue;
            }

            if (resource.isReadable() && fileName != null) {
                LOG.info("Copying resource [{}] to [{}]", resource, destDirectory);
                final File destinationPath = new File(destDirectory, fileName);
                Files.copy(resource.getInputStream(), destinationPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                LOG.info("Copying directory [{}] to [{}]", resource, destDirectory);
                final String directoryPath = resource.getURL().toString();
                String directoryClasspath = StringUtils.substringAfter(directoryPath, "target/classes");
                if (StringUtils.startsWith(directoryClasspath, "/")) {
                    directoryClasspath = StringUtils.substringAfter(directoryClasspath, "/");
                }

                copyResourceToDisk(domibusConfigLocation, directoryClasspath, directoryClasspath);
            }
        }
    }

    public static void copyActiveMQFile(File domibusConfigLocation) throws IOException {
        LOG.info("Copying activemq.xml file");

        final File internalDirectory = new File(domibusConfigLocation, "internal");
        FileUtils.forceMkdir(internalDirectory);
        final File destActiveMQ = new File(internalDirectory, "activemq.xml");
        try (InputStream inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream("abstractIT/activemq.xml")) {
            if (inputStream != null) {
                LOG.info("Copying activemq.xml file to [{}]", destActiveMQ);
                Files.copy(inputStream, destActiveMQ.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static void copyDomibusProperties(File domibusConfigLocation) throws IOException {
        LOG.info("Copying domibus.properties file");

        final File destDomibusPropertiesFile = new File(domibusConfigLocation, "domibus.properties");
        try (InputStream inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream("abstractIT/domibus.properties")) {
            if (inputStream != null) {
                LOG.info("Copying domibus.properties file to [{}]", destDomibusPropertiesFile);
                Files.copy(inputStream, destDomibusPropertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }


}
