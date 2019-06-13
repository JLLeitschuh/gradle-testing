package com.gradle.publish;

import com.gradle.protocols.ServerResponseBase;
import com.gradle.publish.protocols.v1.models.ClientPostRequest;
import com.gradle.publish.protocols.v1.models.publish.*;
import com.gradle.publish.upload.Uploader;
import org.apache.maven.model.Dependency;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MyPublishTask extends DefaultTask {
    public static final String GRADLE_PUBLISH_SECRET = "gradle.publish.secret";
    public static final String GRADLE_PUBLISH_KEY = "gradle.publish.key";
    private static final String SKIP_NAMESPACE_CHECK_PROPERTY = "gradle.publish.skip.namespace.check";
    private static final Logger LOGGER = Logging.getLogger(com.gradle.publish.MyPublishTask.class);
    private Config ghConfig = new Config(this.getProject());
    private File pomFile;
    private PluginBundleExtension bundleConfig;
    private PluginPublishValidator validator = null;
    private static final Pattern MAVEN_ID_REGEX = Pattern.compile("[A-Za-z0-9_\\-.]+");

    public MyPublishTask() {
    }

    @TaskAction
    void publish() throws Exception {
        this.validator = new PluginPublishValidator(this.getProject().getVersion().toString(), System.getProperty("gradle.publish.skip.namespace.check", "false").equals("true"), this.ghConfig.versionOverride());
//        this.validator.validateBundle(this.bundleConfig);
        List<PublishNewVersionRequest> requests = this.buildPublishRequests();
        PublishMavenCoordinates mavenCoords = this.getMavenCoordinates();
        this.validatePluginDescriptors(requests);
        this.generatePom(mavenCoords);
        Map<PublishArtifact, File> artifacts = this.collectArtifacts();
        this.publishToPortal(requests, mavenCoords, artifacts);
    }

    private void validatePluginDescriptors(List<PublishNewVersionRequest> requests) throws IOException {
        File artifactFile = this.findMainArtifact();

        try (ZipFile zip = new ZipFile(artifactFile)) {
            for (PublishNewVersionRequest request : requests) {
                this.validatePluginDescriptor(zip, request.getPluginId());
            }
        } catch (IOException var9) {
            throw new RuntimeException("Unable to validate plugin jar " + artifactFile.getPath(), var9);
        }

    }

    private void validatePluginDescriptor(ZipFile zip, String pluginId) throws IOException {
        String resPath = String.format("META-INF/gradle-plugins/%s.properties", pluginId);
        ZipEntry descriptorEntry = zip.getEntry(resPath);
        if (descriptorEntry == null) {
            throw new IllegalArgumentException(String.format("No plugin descriptor for plugin ID '%s'.\nCreate a 'META-INF/gradle-plugins/%s.properties' file with a 'implementation-class' property pointing to the plugin class implementation.", pluginId, pluginId));
        } else {
            Properties descriptor = new Properties();
            descriptor.load(zip.getInputStream(descriptorEntry));
            String pluginClassName = descriptor.getProperty("implementation-class");
            if (Util.isBlank(pluginClassName)) {
                throw new IllegalArgumentException(String.format("Plugin descriptor for plugin ID '%s' does not specify a plugin\nclass with the implementation-class property", pluginId));
            } else {
                String pluginClassResourcePath = pluginClassName.replace('.', '/').concat(".class");
                if (zip.getEntry(pluginClassResourcePath) == null) {
                    throw new IllegalArgumentException(String.format("Plugin descriptor for plugin ID '%s' specifies a plugin\nclass '%s' that is not present in the jar file", pluginId, pluginClassName));
                }
            }
        }
    }

    private File findMainArtifact() {
        Configuration archivesConfiguration = this.getProject().getConfigurations().getByName("archives");
        Iterator<org.gradle.api.artifacts.PublishArtifact> i$ = archivesConfiguration.getAllArtifacts().iterator();

        org.gradle.api.artifacts.PublishArtifact artifact;
        do {
            if (!i$.hasNext()) {
                throw new IllegalArgumentException("Cannot determine main artifact to upload - could not find jar artifact with empty classifier");
            }

            artifact = i$.next();
        } while(!Util.isBlank(artifact.getClassifier()) || !"jar".equals(artifact.getExtension()));

        return artifact.getFile();
    }

    private void generatePom(PublishMavenCoordinates coords) throws IOException {
        List<Dependency> deps = (new DependenciesBuilder()).buildMavenDependencies(this.getProject().getConfigurations());
        if (this.bundleConfig.getWithDependenciesBlock() != null) {
            this.bundleConfig.getWithDependenciesBlock().execute(deps);
        }

        (new MyPomWriter()).writePom(this.pomFile, coords, deps);
    }

    void addAndHashArtifact(Map<PublishArtifact, File> artifacts, org.gradle.api.artifacts.PublishArtifact configuredArtifact) throws IOException {
        this.addAndHashArtifact(artifacts, configuredArtifact.getFile(), configuredArtifact.getExtension(), configuredArtifact.getClassifier());
    }

    void addAndHashArtifact(Map<PublishArtifact, File> artifacts, File file, String ext, String classifier) throws IOException {
        if (file != null) {
            FileInputStream fis = new FileInputStream(file);

            try {
                String hash = Hasher.hash(fis);
                ArtifactType type = ArtifactType.find(ext, classifier);
                if (type == null) {
                    LOGGER.warn("Ignoring unknown artifact type with extension \"{}\" and classifier \"{}\"\nYou can only upload normal jars, sources jars, javadoc jars and groovydoc jars\nto the plugin portal at this time.", ext, classifier);
                } else {
                    artifacts.put(new PublishArtifact(type.name(), hash), file);
                }
            } finally {
                fis.close();
            }
        }

    }

    private void publishToPortal(List<PublishNewVersionRequest> requests, PublishMavenCoordinates mavenCoords, Map<PublishArtifact, File> artifacts) throws Exception {
        List<PublishNewVersionResponse> apiResponses = new ArrayList<>();
        for (PublishNewVersionRequest request : requests) {
            LOGGER.lifecycle("Publishing plugin {} version {}", request.getPluginId(), request.getPluginVersion());
            request.setMavenCoordinates(mavenCoords);
            request.setArtifacts(new ArrayList<>(artifacts.keySet()));
            PublishNewVersionResponse apiResponse = this.doSignedPost(request);
            this.handleApiResponse(request.getPluginId(), apiResponse);
            apiResponses.add(apiResponse);
        }

        LOGGER.debug("Uploading artifacts");
        this.publishArtifacts(apiResponses.get(0), artifacts);

        for (PublishNewVersionResponse apiResponse : apiResponses) {
            this.activate(apiResponse.getNextRequest());
        }
    }

    private Map<PublishArtifact, File> collectArtifacts() throws IOException {
        Map<PublishArtifact, File> artifacts = new LinkedHashMap<>();
        Configuration archivesConfiguration = this.getProject().getConfigurations().getByName("archives");

        for (org.gradle.api.artifacts.PublishArtifact artifact : archivesConfiguration.getAllArtifacts()) {
            this.addAndHashArtifact(artifacts, artifact);
        }

        this.addAndHashArtifact(artifacts, this.getPomFile(), "pom", null);
        return artifacts;
    }

    private void publishArtifacts(PublishNewVersionResponse apiResponse, Map<PublishArtifact, File> artifactHashes) throws IOException {
        Map<String, String> publishedHashAndUrls = apiResponse.getPublishTo();

        for (Entry<PublishArtifact, File> art : artifactHashes.entrySet()) {
            String uploadUrl = publishedHashAndUrls.get(art.getKey().getHash());
            File artifactFile = art.getValue();
            this.uploadArtifactIfNecessary(artifactFile, uploadUrl);
        }

    }

    private void handleApiResponse(String pluginId, PublishNewVersionResponse apiResponse) {
        ResponseUtil.assertValidResponse("Request to publish new plugin '" + pluginId + "' failed!", apiResponse);
        if (apiResponse.hasFailed()) {
            throw new RuntimeException("Cannot publish plugin '" + pluginId + "'\n" + "Server responded with: " + apiResponse.getErrorMessage());
        } else {
            if (apiResponse.hasWarning()) {
                LOGGER.warn(apiResponse.getWarningMessage());
            }
        }
    }

    private PublishMavenCoordinates getMavenCoordinates() {
        MavenCoordinates coordinates = this.bundleConfig.getMavenCoordinates();
        String groupId = GroupId.createGroupId(this.getProject(), this.bundleConfig);
        String specifiedArtifactId = coordinates != null ? coordinates.getArtifactId() : null;
        String artifactId = !Util.isBlank(specifiedArtifactId) ? specifiedArtifactId : this.getProject().getName();
        String version = coordinates != null && !Util.isBlank(coordinates.getVersion()) ? coordinates.getVersion() : this.getProject().getVersion().toString();
//        this.validator.validateMavenCoordinates(groupId, artifactId, version, coordinates);

        return new PublishMavenCoordinates(groupId, artifactId, version);
    }

    private List<PublishNewVersionRequest> buildPublishRequests() {
        List<PublishNewVersionRequest> reqs = new ArrayList<>();

        for (PluginConfig pluginConfig : this.bundleConfig.getPlugins()) {
            reqs.add(this.buildPublishRequest(pluginConfig));
        }

        return reqs;
    }

    private List<String> getTags(PluginConfig plugin) {
        Collection<String> configTags = plugin.getTags();
        if (configTags.isEmpty()) {
            configTags = this.bundleConfig.getTags();
        }

        List<String> tags = new ArrayList<>();

        for (String tag : configTags) {
            tag = tag.toLowerCase();
            tags.add(tag);
        }

        return tags;
    }

    private PublishNewVersionRequest buildPublishRequest(PluginConfig plugin) {
        PublishNewVersionRequest request = new PublishNewVersionRequest();
        BuildMetadata buildMetadata = new BuildMetadata(this.getProject().getGradle().getGradleVersion());
        request.setBuildMetadata(buildMetadata);
        request.setPluginId(plugin.getId());
        String pluginVersion = plugin.getVersion() != null ? plugin.getVersion() : this.getProject().getVersion().toString();
        request.setPluginVersion(pluginVersion);
        request.setDisplayName(plugin.getDisplayName());
        String desc = plugin.getDescription();
        desc = desc != null ? desc : this.bundleConfig.getDescription();
        request.setDescription(desc);
        request.setTags(this.getTags(plugin));
        request.setWebSite(this.bundleConfig.getWebsite());
        request.setVcsUrl(this.bundleConfig.getVcsUrl());
        return request;
    }

    private <T extends ServerResponseBase> T doSignedPost(ClientPostRequest<T> postRequest) throws Exception {
        return this.buildOAuthClient().send(postRequest);
    }

    private void activate(PublishActivateRequest activePlugin) throws Exception {
        LOGGER.lifecycle("Activating plugin {} version {}", new Object[]{activePlugin.getPluginId(), activePlugin.getVersion()});
        this.doSignedPost(activePlugin);
    }

    private File getPomFile() {
        return this.pomFile;
    }

    private void uploadArtifactIfNecessary(File artifactFile, String uploadUrl) throws IOException {
        URI filePath = this.getProject().getProjectDir().toURI().relativize(artifactFile.toURI());
        if (uploadUrl != null) {
            LOGGER.lifecycle("Publishing artifact {}", new Object[]{filePath});
            LOGGER.info("Publishing {} to {}", filePath, uploadUrl);
            Uploader.putFile(artifactFile, uploadUrl);
        } else {
            LOGGER.info("Skipping upload of artifact {} as it has been previously uploaded", filePath);
        }

    }

    public void setPomFile(File pomFile) {
        this.pomFile = pomFile;
    }

    public void setBundleConfig(PluginBundleExtension bundleConfig) {
        this.bundleConfig = bundleConfig;
    }

    private OAuthHttpClient buildOAuthClient() throws IOException {
        Properties props = PropertiesStore.all(this.getProject());
        String key = props.getProperty("gradle.publish.key");
        String secret = props.getProperty("gradle.publish.secret");
        if (key != null && !key.trim().isEmpty() && secret != null && !secret.trim().isEmpty()) {
            return new OAuthHttpClient(this.ghConfig.getPortalUrl(), key, secret);
        } else {
            throw new IllegalArgumentException("Missing publishing keys. Please set gradle.publish.key and gradle.publish.secret system properties or login using the login task.");
        }
    }
}

