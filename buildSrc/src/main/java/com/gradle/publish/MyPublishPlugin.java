package com.gradle.publish;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Groovydoc;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;

public class MyPublishPlugin implements Plugin<Project> {
    public static final String LOGIN_TASK_NAME = "login";
    private static final String PUBLISH_TASK_DESCRIPTION = "Publishes this plugin to the Gradle Plugin portal.";
    private static final String LOGIN_TASK_DESCRIPTION = "Update the gradle.properties files so this machine can publish to the Gradle Plugin portal.";
    private static final String PORTAL_BUILD_GROUP_NAME = "Plugin Portal";
    private static String BASE_TASK_NAME = "publishPlugin";
    private static String SOURCES_JAR_TASK_NAME;
    private static final String JAVA_DOCS_TASK_NAME;
    private static final String GROOVY_DOCS_TASK_NAME;
    private static final String PUBLISH_TASK_NAME = "publishPlugins";
    public static final String PLUGIN_BUNDLE_EXTENSION_NAME = "pluginBundle";
    static final String SOURCES_CLASSIFIER = "sources";
    static final String JAVADOC_CLASSIFIER = "javadoc";
    static final String GROOVYDOC_CLASSIFIER = "groovydoc";
    static final String POM_EXT = "pom";
    public static final String JAR_TASK_NAME = "jar";
    private static final Logger LOGGER;

    public MyPublishPlugin() {
    }

    @Override
    public void apply(final Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        PluginBundleExtension bundle = new PluginBundleExtension(project);
        project.getExtensions().add("pluginBundle", bundle);
        final MyPublishTask publishTask = (MyPublishTask)project.getTasks().create("publishPlugins", MyPublishTask.class);
        publishTask.setDescription("Publishes this plugin to the Gradle Plugin portal.");
        publishTask.setGroup("Plugin Portal");
        publishTask.setBundleConfig(bundle);
        File pomFile = new File(new File(project.getBuildDir(), "publish-generated-resources"), "pom.xml");
        publishTask.setPomFile(pomFile);
        LOGGER.debug("Setup: publishPlugins of " + this.getClass().getName());
        LoginTask loginTask = (LoginTask)project.getTasks().create("login", LoginTask.class);
        loginTask.setDescription("Update the gradle.properties files so this machine can publish to the Gradle Plugin portal.");
        loginTask.setGroup("Plugin Portal");
        LOGGER.debug("Created task: login of " + this.getClass().getName());
        project.afterEvaluate(new Action<Project>() {
            public void execute(Project finalProject) {
                Configuration archives = project.getConfigurations().getByName("archives");
                MyPublishPlugin.this.configureDefaultArtifacts(project, archives);
                MyPublishPlugin.this.setupPublishTaskDependencies(publishTask, archives);
            }
        });
    }

    private void configureDefaultArtifacts(Project project, Configuration archivesConfiguration) {
        Jar jarTask = (Jar)project.getTasks().getByPath("jar");
        if (this.isDefaultArchiveConfiguration(archivesConfiguration, jarTask)) {
            if (!this.hasOnlyJarTask(archivesConfiguration, jarTask)) {
                this.addArchiveTaskArtifact(archivesConfiguration, jarTask);
            }

            this.addArchiveTaskArtifact(archivesConfiguration, this.createAndSetupJarSourcesTask(project));
            this.addArchiveTaskArtifact(archivesConfiguration, this.createAndSetupJavaDocsTask(project));
            this.addArchiveTaskArtifact(archivesConfiguration, this.createAndSetupGroovyDocsTask(project));
        }

    }

    private void addArchiveTaskArtifact(Configuration archivesConfiguration, Jar task) {
        if (task != null) {
            archivesConfiguration.getArtifacts().add(new ArchivePublishArtifact(task));
        }

    }

    private boolean isDefaultArchiveConfiguration(Configuration archivesConfiguration, Jar jarTask) {
        return archivesConfiguration.getArtifacts().isEmpty() || this.hasOnlyJarTask(archivesConfiguration, jarTask);
    }

    private boolean hasOnlyJarTask(Configuration archivesConfiguration, Jar jarTask) {
        PublishArtifactSet arts = archivesConfiguration.getAllArtifacts();
        return arts.size() == 1 && ((PublishArtifact)arts.iterator().next()).getFile().equals(jarTask.getArchivePath());
    }

    private void setupPublishTaskDependencies(MyPublishTask publishTask, Configuration archivesConfiguration) {
        Iterator i$ = archivesConfiguration.getAllArtifacts().iterator();

        while(i$.hasNext()) {
            PublishArtifact artifact = (PublishArtifact)i$.next();
            publishTask.dependsOn(new Object[]{artifact});
        }

    }

    private Jar createAndSetupGroovyDocsTask(Project project) {
        if (!project.getPlugins().withType(GroovyPlugin.class).isEmpty()) {
            Jar docsJarTask = this.createBasicDocJarTask(project, GROOVY_DOCS_TASK_NAME, "groovydoc", "Assembles a jar archive containing the documentation for Groovy code.");
            Groovydoc groovydoc = (Groovydoc)project.getTasks().findByName("groovydoc");
            docsJarTask.dependsOn(new Object[]{groovydoc});
            docsJarTask.from(new Object[]{groovydoc.getDestinationDir()});
            return docsJarTask;
        } else {
            return null;
        }
    }

    private Jar createAndSetupJavaDocsTask(Project project) {
        Jar docsJarTask = this.createBasicDocJarTask(project, JAVA_DOCS_TASK_NAME, "javadoc", "Assembles a jar archive containing the documentation for the main Java source code.");
        Javadoc javadoc = (Javadoc)project.getTasks().findByName("javadoc");
        docsJarTask.dependsOn(new Object[]{javadoc});
        docsJarTask.from(new Object[]{javadoc.getDestinationDir()});
        return docsJarTask;
    }

    private Jar createBasicDocJarTask(Project project, String name, String classifier, String description) {
        Jar docsJarTask = (Jar)project.getTasks().create(name, Jar.class);
        docsJarTask.setDescription(description);
        docsJarTask.setGroup("build");
        docsJarTask.setClassifier(classifier);
        return docsJarTask;
    }

    private Jar createAndSetupJarSourcesTask(Project project) {
        Jar sourcesJarTask = (Jar)project.getTasks().create(SOURCES_JAR_TASK_NAME, Jar.class);
        sourcesJarTask.setDescription("Assembles a jar archive containing the main source code.");
        sourcesJarTask.setGroup("build");
        sourcesJarTask.setClassifier("sources");
        JavaPluginConvention javaPluginConvention = (JavaPluginConvention)project.getConvention().findPlugin(JavaPluginConvention.class);
        SourceSet mainSourceSet = (SourceSet)javaPluginConvention.getSourceSets().getByName("main");
        SourceDirectorySet allMainSources = mainSourceSet.getAllSource();
        sourcesJarTask.from(new Object[]{allMainSources});
        return sourcesJarTask;
    }

    static {
        SOURCES_JAR_TASK_NAME = BASE_TASK_NAME + "Jar";
        JAVA_DOCS_TASK_NAME = BASE_TASK_NAME + "JavaDocsJar";
        GROOVY_DOCS_TASK_NAME = BASE_TASK_NAME + "GroovyDocsJar";
        LOGGER = LoggerFactory.getLogger(PublishPlugin.class);
    }
}
