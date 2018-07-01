/**
 * Copyright (c) 2014 mediaworx berlin AG (http://mediaworx.com)
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * <p>
 * For further information about mediaworx berlin AG, please see the
 * company website: http://mediaworx.com
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 * If not, see <http://www.gnu.org/licenses/>
 */

package com.mediaworx.mojo.opencms;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.DefaultMavenProjectHelper;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.artifact.AttachedArtifact;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.File;
import java.io.IOException;

/**
 * Creates OpenCms module zip.
 */
@Mojo(name = "module-zip",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ModuleZipMojo extends AbstractOpenCmsMojo {
    /**
     * The Maven Session Object
     *
     */
    @Parameter(readonly = true, required = true, defaultValue = "${session}")
    protected MavenSession session;

    @Parameter(defaultValue = "true")
    protected Boolean addJarToModule;

    /**
     * Used to create .jar archive for Maven dependency resolution.
     */
    @Component(role = Archiver.class, hint = "jar")
    protected JarArchiver jarArchiver;

    /**
     * The maven archive configuration to use.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Creates an OpenCms module zip and jar containing classes for use as Maven dependency.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        File destination = new File(moduleDir);
        if (!destination.exists() && !destination.mkdirs()) {
            throw new MojoExecutionException("Couldn't create destination directory " + destination.getAbsolutePath());
        }

        if (isSkipExecution()) {
            addJar(destination);
            getLog().info("Skipping create OpenCms Module");
            return;
        }

        buildModule();

        if (addDependencies) {
            addDependencies();
        }
        addJar(destination);

        addManifest();

        addModuleZip(destination);
    }

    private void addJar(File destination) throws MojoExecutionException {
        File jarFile = new File(destination, project.getBuild().getFinalName() + ".jar");

        try {
            MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver(jarArchiver);
            archiver.setOutputFile(jarFile);
            if (getClassesDirectory().exists() && getClassesDirectory().isDirectory()) {
                jarArchiver.addDirectory(getClassesDirectory());
            }
            archiver.createArchive(session, project, archive);

            ArtifactHandler jarHandler = artifactHandlerManager.getArtifactHandler("jar");
            Artifact jarArtifact = new AttachedArtifact(project.getArtifact(), "jar", "opencms", jarHandler);
            jarArtifact.setFile(jarFile);
            jarArtifact.setResolved(true);
            project.addAttachedArtifact(jarArtifact);

            if (Boolean.TRUE.equals(addJarToModule)) {
                copyLibraryToModule(wrap(jarArtifact));
            }
        } catch (ManifestException | IOException e) {
            throw new MojoExecutionException("Cannot create jar", e);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot resolve dependencies", e);
        }
    }

    private void addModuleZip(File destination) throws MojoExecutionException {
        File zipSource = new File(targetDir);
        try {

            ZipArchiver archiver = new ZipArchiver();
            archiver.addDirectory(zipSource);
            archiver.setDestFile(new File(destination, moduleName + "_" + getModuleVersion() + ".zip"));
            archiver.createArchive();

            // set zip as primary project artifact
            ArtifactHandler zipHandler = artifactHandlerManager.getArtifactHandler("zip");
            Artifact zipArtifact = new AttachedArtifact(project.getArtifact(), "zip", null, zipHandler);
            zipArtifact.setFile(archiver.getDestFile());
            zipArtifact.setResolved(true);
            project.setArtifact(zipArtifact);

        } catch (ArchiverException | IOException ex) {
            throw new MojoExecutionException("Could not zip the module directory", ex);
        }
    }
}
