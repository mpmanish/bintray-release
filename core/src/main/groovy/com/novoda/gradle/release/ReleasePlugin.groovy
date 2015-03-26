package com.novoda.gradle.release

import com.jfrog.bintray.gradle.BintrayPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

class ReleasePlugin implements Plugin<Project> {

    void apply(Project project) {
        PublishExtension extension = project.extensions.create('publish', PublishExtension)

        project.apply([plugin: 'maven-publish'])
        attachArtifacts(project)

        new BintrayPlugin().apply(project)
        delayBintrayConfigurationUntilPublishExtensionIsEvaluated(project, extension)
    }

    void attachArtifacts(Project project) {
        Artifacts artifacts = project.plugins.hasPlugin('com.android.library') ? new AndroidArtifacts() : new JavaArtifacts()
        String projectVersion = getString(project, 'version', project.publish.version)
        String projectName = project.publish.uploadName ? project.publish.uploadName : project.publish.groupId + ':' + project.publish.artifactId
        String projectDescription = project.publish.description ? project.publish.description : 'no description'
        project.publishing {
            publications {
                maven(MavenPublication) {
                    groupId project.publish.groupId
                    artifactId project.publish.artifactId
                    version projectVersion
                    pom.withXml {
                        asNode().appendNode('name', projectName)
                        asNode().appendNode('description', projectDescription + project.publish.description)
                    }

                    artifacts.all(project).each {
                        delegate.artifact it
                    }

                    from artifacts.from(project)
                }
            }
        }
    }

    private delayBintrayConfigurationUntilPublishExtensionIsEvaluated(Project project, extension) {
        project.afterEvaluate {
            new BintrayConfiguration(extension).configure(project)
        }
    }

    private String getString(Project project, String propertyName, String defaultValue) {
        project.hasProperty(propertyName) ? project.getProperty(propertyName) : defaultValue
    }

}
