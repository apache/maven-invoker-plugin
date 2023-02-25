/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.invoker;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * Installs the project artifacts of the main build into the local repository as a preparation to run the sub projects.
 * More precisely, all artifacts of the project itself, all its locally reachable parent POMs and all its dependencies
 * from the reactor will be installed to the local repository.
 *
 * @author Paul Gier
 * @author Benjamin Bentmann
 * @since 1.2
 */
@Mojo(
        name = "install",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class InstallMojo extends AbstractMojo {

    // components used in Mojo

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The path to the local repository into which the project artifacts should be installed for the integration tests.
     * If not set, the regular local repository will be used. To prevent soiling of your regular local repository with
     * possibly broken artifacts, it is strongly recommended to use an isolated repository for the integration tests
     * (e.g. <code>${project.build.directory}/it-repo</code>).
     */
    @Parameter(
            property = "invoker.localRepositoryPath",
            defaultValue = "${session.localRepository.basedir}",
            required = true)
    private File localRepositoryPath;

    /**
     * A flag used to disable the installation procedure. This is primarily intended for usage from the command line to
     * occasionally adjust the build.
     *
     * @since 1.4
     */
    @Parameter(property = "invoker.skip", defaultValue = "false")
    private boolean skipInstallation;

    /**
     * Extra dependencies that need to be installed on the local repository.
     * <p>
     * Format:
     * <pre>
     * groupId:artifactId:version:type:classifier
     * </pre>
     * <p>
     * Examples:
     * <pre>
     * org.apache.maven.plugins:maven-clean-plugin:2.4:maven-plugin
     * org.apache.maven.plugins:maven-clean-plugin:2.4:jar:javadoc
     * </pre>
     * <p>
     * If the type is 'maven-plugin' the plugin will try to resolve the artifact using plugin remote repositories,
     * instead of using artifact remote repositories.
     * <p>
     * <b>NOTICE</b> all dependencies will be resolved with transitive dependencies in <code>runtime</code> scope.
     *
     * @since 1.6
     */
    @Parameter
    private String[] extraArtifacts;

    /**
     * Scope to resolve project artifacts.
     *
     * @since 3.5.0
     */
    @Parameter(property = "invoker.install.scope", defaultValue = "runtime")
    private String scope;

    /**
     * Performs this mojo's tasks.
     *
     * @throws MojoExecutionException If the artifacts could not be installed.
     */
    public void execute() throws MojoExecutionException {
        if (skipInstallation) {
            getLog().info("Skipping artifact installation per configuration.");
            return;
        }

        Collection<Artifact> resolvedArtifacts = new ArrayList<>();

        try {

            resolveProjectArtifacts(resolvedArtifacts);
            resolveProjectPoms(project, resolvedArtifacts);
            resolveProjectDependencies(resolvedArtifacts);
            resolveExtraArtifacts(resolvedArtifacts);
            installArtifacts(resolvedArtifacts);

        } catch (DependencyResolutionException
                | InstallationException
                | ArtifactDescriptorException
                | ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void resolveProjectArtifacts(Collection<Artifact> resolvedArtifacts) {

        // pom packaging doesn't have a main artifact
        if (project.getArtifact() != null && project.getArtifact().getFile() != null) {
            resolvedArtifacts.add(RepositoryUtils.toArtifact(project.getArtifact()));
        }

        resolvedArtifacts.addAll(project.getAttachedArtifacts().stream()
                .map(RepositoryUtils::toArtifact)
                .collect(Collectors.toList()));
    }

    private void resolveProjectPoms(MavenProject project, Collection<Artifact> resolvedArtifacts)
            throws ArtifactResolutionException {

        if (project == null) {
            return;
        }

        Artifact projectPom = RepositoryUtils.toArtifact(new ProjectArtifact(project));
        if (projectPom.getFile() != null) {
            resolvedArtifacts.add(projectPom);
        } else {
            Artifact artifact = resolveArtifact(projectPom, project.getRemoteProjectRepositories());
            resolvedArtifacts.add(artifact);
        }
        resolveProjectPoms(project.getParent(), resolvedArtifacts);
    }

    private void resolveProjectDependencies(Collection<Artifact> resolvedArtifacts)
            throws ArtifactResolutionException, MojoExecutionException, DependencyResolutionException {

        DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(scope);

        ArtifactTypeRegistry artifactTypeRegistry =
                session.getRepositorySession().getArtifactTypeRegistry();

        List<Dependency> managedDependencies = Optional.ofNullable(project.getDependencyManagement())
                .map(DependencyManagement::getDependencies)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                .collect(Collectors.toList());

        List<Dependency> dependencies = project.getDependencies().stream()
                .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                .collect(Collectors.toList());

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRootArtifact(RepositoryUtils.toArtifact(project.getArtifact()));
        collectRequest.setDependencies(dependencies);
        collectRequest.setManagedDependencies(managedDependencies);

        collectRequest.setRepositories(project.getRemoteProjectRepositories());

        DependencyRequest request = new DependencyRequest(collectRequest, classpathFilter);

        DependencyResult dependencyResult =
                repositorySystem.resolveDependencies(session.getRepositorySession(), request);

        List<Artifact> artifacts = dependencyResult.getArtifactResults().stream()
                .map(ArtifactResult::getArtifact)
                .collect(Collectors.toList());

        resolvedArtifacts.addAll(artifacts);
        resolvePomsForArtifacts(artifacts, resolvedArtifacts, collectRequest.getRepositories());
    }

    /**
     * Resolve extra artifacts.
     *
     * @return
     */
    private void resolveExtraArtifacts(Collection<Artifact> resolvedArtifacts)
            throws MojoExecutionException, DependencyResolutionException, ArtifactDescriptorException,
                    ArtifactResolutionException {

        if (extraArtifacts == null) {
            return;
        }

        DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);

        for (String extraArtifact : extraArtifacts) {
            String[] gav = extraArtifact.split(":");
            if (gav.length < 3 || gav.length > 5) {
                throw new MojoExecutionException("Invalid artifact " + extraArtifact);
            }

            String groupId = gav[0];
            String artifactId = gav[1];
            String version = gav[2];

            String type = "jar";
            if (gav.length > 3) {
                type = gav[3];
            }

            String classifier = null;
            if (gav.length == 5) {
                classifier = gav[4];
            }

            ArtifactType artifactType =
                    session.getRepositorySession().getArtifactTypeRegistry().get(type);

            List<RemoteRepository> remoteRepositories =
                    artifactType != null && "maven-plugin".equals(artifactType.getId())
                            ? project.getRemotePluginRepositories()
                            : project.getRemoteProjectRepositories();

            Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, null, version, artifactType);

            resolvePomsForArtifacts(Collections.singletonList(artifact), resolvedArtifacts, remoteRepositories);

            CollectRequest collectRequest = new CollectRequest();
            Dependency root = new Dependency(artifact, JavaScopes.COMPILE);
            collectRequest.setRoot(root);
            collectRequest.setRepositories(remoteRepositories);

            DependencyRequest request = new DependencyRequest(collectRequest, classpathFilter);
            DependencyResult dependencyResult =
                    repositorySystem.resolveDependencies(session.getRepositorySession(), request);

            List<Artifact> artifacts = dependencyResult.getArtifactResults().stream()
                    .map(ArtifactResult::getArtifact)
                    .collect(Collectors.toList());

            resolvedArtifacts.addAll(artifacts);
            resolvePomsForArtifacts(artifacts, resolvedArtifacts, collectRequest.getRepositories());
        }
    }

    private void resolvePomsForArtifacts(
            List<Artifact> artifacts, Collection<Artifact> resolvedArtifacts, List<RemoteRepository> remoteRepositories)
            throws ArtifactResolutionException, MojoExecutionException {

        for (Artifact a : artifacts) {
            Artifact artifactResult = resolveArtifact(new SubArtifact(a, "", "pom"), remoteRepositories);
            resolvePomWithParents(artifactResult, resolvedArtifacts, remoteRepositories);
        }
    }

    private void resolvePomWithParents(
            Artifact artifact, Collection<Artifact> resolvedArtifacts, List<RemoteRepository> remoteRepositories)
            throws MojoExecutionException, ArtifactResolutionException {

        if (resolvedArtifacts.contains(artifact)) {
            return;
        }

        Model model = PomUtils.loadPom(artifact.getFile());
        Parent parent = model.getParent();
        if (parent != null) {
            DefaultArtifact pom =
                    new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom", parent.getVersion());
            Artifact resolvedPom = resolveArtifact(pom, remoteRepositories);
            resolvePomWithParents(resolvedPom, resolvedArtifacts, remoteRepositories);
        }

        resolvedArtifacts.add(artifact);
    }

    private Artifact resolveArtifact(Artifact artifact, List<RemoteRepository> remoteRepositories)
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(remoteRepositories);
        ArtifactResult artifactResult = repositorySystem.resolveArtifact(session.getRepositorySession(), request);
        return artifactResult.getArtifact();
    }

    /**
     * Install list of artifacts into local repository.
     */
    private void installArtifacts(Collection<Artifact> resolvedArtifacts) throws InstallationException {

        RepositorySystemSession systemSessionForLocalRepo = createSystemSessionForLocalRepo();

        // we can have on dependency two artifacts with the same groupId:artifactId
        // with different version, in such case when we install both in one request
        // metadata will contain only one version

        Map<String, List<Artifact>> collect = resolvedArtifacts.stream()
                .filter(a -> !hasTheSamePathAsTarget(a, systemSessionForLocalRepo))
                .collect(Collectors.groupingBy(
                        a -> String.format("%s:%s:%s", a.getGroupId(), a.getArtifactId(), a.getVersion()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (List<Artifact> artifacts : collect.values()) {
            InstallRequest request = new InstallRequest();
            request.setArtifacts(artifacts);
            repositorySystem.install(systemSessionForLocalRepo, request);
        }
    }

    private boolean hasTheSamePathAsTarget(Artifact artifact, RepositorySystemSession systemSession) {
        try {
            LocalRepositoryManager lrm = systemSession.getLocalRepositoryManager();
            File targetBasedir = lrm.getRepository().getBasedir();
            if (targetBasedir == null) {
                return false;
            }
            File targetFile = new File(targetBasedir, lrm.getPathForLocalArtifact(artifact)).getCanonicalFile();
            File sourceFile = artifact.getFile().getCanonicalFile();
            if (Objects.equals(targetFile, sourceFile)) {
                getLog().debug("Skip install the same target " + sourceFile);
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a new {@link  RepositorySystemSession} connected with local repo.
     */
    private RepositorySystemSession createSystemSessionForLocalRepo() {
        RepositorySystemSession repositorySystemSession = session.getRepositorySession();
        if (localRepositoryPath != null) {
            // "clone" repository session and replace localRepository
            DefaultRepositorySystemSession newSession =
                    new DefaultRepositorySystemSession(session.getRepositorySession());
            // Clear cache, since we're using a new local repository
            newSession.setCache(new DefaultRepositoryCache());
            // keep same repositoryType
            String contentType = newSession.getLocalRepository().getContentType();
            if ("enhanced".equals(contentType)) {
                contentType = "default";
            }
            LocalRepositoryManager localRepositoryManager = repositorySystem.newLocalRepositoryManager(
                    newSession, new LocalRepository(localRepositoryPath, contentType));

            newSession.setLocalRepositoryManager(localRepositoryManager);
            repositorySystemSession = newSession;
            getLog().debug("localRepoPath: "
                    + localRepositoryManager.getRepository().getBasedir());
        }

        return repositorySystemSession;
    }
}
