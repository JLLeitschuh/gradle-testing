//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gradle.publish;

import com.gradle.publish.protocols.v1.models.publish.PublishMavenCoordinates;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class MyPomWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyPomWriter.class);
    private Document doc;

    public MyPomWriter() {
    }

    public void writePom(File pomFile, PublishMavenCoordinates coordinates, List<Dependency> mavenDependencies) throws IOException {
        this.createPomDocument(coordinates, mavenDependencies);
        this.writeFile(pomFile);
    }

    private void createPomDocument(PublishMavenCoordinates coordinates, List<Dependency> mavenDependencies) {
        Element domDocument = this.createDomDocument();
        this.appendTextNode(domDocument, "modelVersion", "4.0.0");
        this.appendTextNode(domDocument, "groupId", coordinates.getGroupId());
        this.appendTextNode(domDocument, "artifactId", coordinates.getArtifactId());
        this.appendTextNode(domDocument, "version", coordinates.getVersion());
        if (mavenDependencies != null && !mavenDependencies.isEmpty()) {
            Element dependenciesElement = this.doc.createElement("dependencies");
            domDocument.appendChild(dependenciesElement);
            Iterator i$ = mavenDependencies.iterator();

            while(i$.hasNext()) {
                Dependency mavenDependency = (Dependency)i$.next();
                this.addDependency(dependenciesElement, mavenDependency);
            }
        }

    }

    private void addDependency(Element dependenciesElement, Dependency mavenDependency) {
        Element dependencyElement = this.doc.createElement("dependency");
        dependenciesElement.appendChild(dependencyElement);
        this.appendTextNode(dependencyElement, "groupId", mavenDependency.getGroupId());
        this.appendTextNode(dependencyElement, "artifactId", mavenDependency.getArtifactId());
        if (mavenDependency.getVersion() == null) {
            logMissingDependencyVersionHint(mavenDependency);
            throw new MissingDependencyVersionException("No version found for " + mavenDependency.getGroupId() + ":" + mavenDependency.getArtifactId() + " on pom generation.");
        } else {
            this.appendTextNode(dependencyElement, "version", mavenDependency.getVersion());
            this.appendTextNodeIfPresent(dependencyElement, "classifier", mavenDependency.getClassifier());
            this.appendTextNodeIfPresent(dependencyElement, "scope", mavenDependency.getScope());
            this.appendTextNodeIfPresent(dependencyElement, "type", mavenDependency.getType());
            this.appendTextNodeIfPresent(dependencyElement, "optional", String.valueOf(mavenDependency.isOptional()));
            List exclusions = mavenDependency.getExclusions();
            if (exclusions != null && !exclusions.isEmpty()) {
                Element exclusionsElement = this.doc.createElement("exclusions");
                dependencyElement.appendChild(exclusionsElement);
                Iterator i$ = exclusions.iterator();

                while(i$.hasNext()) {
                    Exclusion exclusion = (Exclusion)i$.next();
                    Element exclusionElement = this.doc.createElement("exclusion");
                    exclusionsElement.appendChild(exclusionElement);
                    this.appendTextNodeIfPresent(exclusionElement, "groupId", exclusion.getGroupId());
                    this.appendTextNodeIfPresent(exclusionElement, "artifactId", exclusion.getArtifactId());
                }
            }

        }
    }

    private static void logMissingDependencyVersionHint(Dependency dependency) {
        LOGGER.info("Could not resolve the version of: " + dependency.getGroupId() + ":" + dependency.getArtifactId());
        LOGGER.info("Use the following workaround to resolve this version for pom generation:");
        LOGGER.info("  http://plugins.gradle.org/help/plugin/missing-dependency-version");
    }

    private static boolean hasValue(String val) {
        return val != null && !val.isEmpty();
    }

    private Element createDomDocument() {
        try {
            this.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument("http://maven.apache.org/POM/4.0.0", "project", (DocumentType)null);
        } catch (ParserConfigurationException var2) {
            handleXmlException(var2);
        }

        Element project = this.doc.getDocumentElement();
        project.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");
        return project;
    }

    private void appendTextNodeIfPresent(Element parent, String name, String value) {
        if (hasValue(value)) {
            this.appendTextNode(parent, name, value);
        }

    }

    private void appendTextNode(Element parent, String name, String value) {
        Element node = this.doc.createElement(name);
        node.appendChild(this.doc.createTextNode(value));
        parent.appendChild(node);
    }

    private void writeFile(File target) {
        target.getParentFile().mkdirs();

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(this.doc);
            StreamResult result = new StreamResult(target);
            transformer.transform(source, result);
        } catch (Exception var5) {
            handleXmlException(var5);
        }

    }

    private static void handleXmlException(Exception e) {
        throw new RuntimeException("Error generating pom", e);
    }
}
