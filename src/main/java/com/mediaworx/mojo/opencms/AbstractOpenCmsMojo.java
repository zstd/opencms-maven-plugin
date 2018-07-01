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

import com.mediaworx.opencms.moduleutils.manifestgenerator.OpenCmsModuleManifestGenerator;
import com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions.OpenCmsMetaXmlFileWriteException;
import com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions.OpenCmsMetaXmlParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.apache.maven.shared.filtering.MavenProjectValueSource;
import org.codehaus.plexus.interpolation.*;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

//import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;

public abstract class AbstractOpenCmsMojo extends AbstractMojo {

    /**
     * Directory containing the resources to be included, defaults to src/main/vfs.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/vfs")
    protected String vfsRoot = "";

    /**
     * Where the module zip should be created.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected String moduleDir = null;

    /**
     * The directory containing generated classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File classesDirectory;

    /**
     * Whether to include classes in final module zip.
     */
    @Parameter(defaultValue = "true")
    protected Boolean addClasses;

    /**
     * This is the temporary folder where the module is assembled.
     */
    @Parameter(defaultValue = "${project.build.directory}/opencms-module")
    protected String targetDir = null;

    /**
     * The Name of the module, defaults to <tt>${project.groupId}.${project.artifactId}</tt>.
     */
    @Parameter(defaultValue = "${project.groupId}.${project.artifactId}")
    protected String moduleName = "";

    /**
     * The Version of the module. Defaults to <tt>${project.version}</tt> with non numeric characters being removed from the end,
     * e.g. <tt>-SNAPSHOT</tt>.
     */
    @Parameter(defaultValue = "${project.version}")
    protected String moduleVersion = "";

    /**
     * The maven project.
     */
    @Component
    protected MavenProject project;

    /**
     * Directories in sourceResources to exclude (ant-style excludes). You may provide excludes in ant-style patterns. Files
     * matching that pattern will not be included to the module (basically they will not be copied to <tt>${targetDir}</tt>).
     */
    @Parameter(defaultValue = "**/.svn/,**/.cvs/,**/.git/,**/Thumbs.db,**/.DS_STORE")
    protected String excludes = "";

    /**
     * The manifest provided is filtered and copied to <tt>${targetDir}</tt> for inclusion in the module.<br>
     * In addition to the standard Maven project properties and user properties, the following properties will be set up
     * from default Maven project properties as follows:
     * <ul>
     * <li><tt>${moduleversion}</tt> set to <tt>${project.version}</tt> with stripped off <tt>-SNAPSHOT</tt> suffix</li>
     * <li><tt>${modulename}</tt> set to <tt>${project.groupId}.${project.artifactId}</tt></li>
     * <li><tt>${modulenicename}</tt> set to <tt>${project.name}</tt></li>
     * <li><tt>${moduledescription}</tt> set to <tt>${project.description}</tt></li>
     * <li><tt>${opencmsversion}</tt> set to the version of the opencms-core dependency</li>
     * </ul>
     * The defaults can be overridden through POM properties or (except for <tt>${modulenicename}</tt> and
     * <tt>${opencmsversion}</tt>) in the plugin configuration section.
     */
    @Parameter
    protected String manifestFile = null;

    /**
     * The manifest stub provided in conjunction with the meta information gathered is used to generate a manifest file.
     * The same filtering as with <tt>manifestFile</tt> applies.
     */
    @Parameter
    protected String manifestMetaDir = null;

    /**
     * Whether to generate and replace uuids and dates in manifest fragments.
     * Works only when using <tt>manifestMetaDir</tt>.
     */
    @Parameter(defaultValue = "false")
    protected Boolean replaceMetaVariables;
    /**
     * Defines a set of additional resources that are included in the module.
     * By default, files under <tt>vfsRoot</tt> and under <tt>${project.build.outputDirectory}</tt> as well as
     * additional runtime dependencies will be included.
     * Currently only <tt>&lt;directory&gt;</tt> and <tt>&lt;targetPath&gt;</tt> are
     * evaluated. The <tt>&lt;targetPath&gt;</tt> configured is relative to <tt>${targetDir}</tt>.
     * <br>A possible configuration might look like
     * this:
     * <pre>
     * &lt;srcResources&gt;
     * &nbsp;&nbsp;&lt;resource&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;directory&gt;${project.basedir}/web/&lt;/directory&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;targetPath&gt;&lt;/targetPath&gt;
     * &nbsp;&nbsp;&lt;/resource&gt;
     * &lt;/srcResources&gt;
     * </pre>
     *
     */
    @Parameter
    protected List<Resource> srcResources;

    @Parameter(defaultValue = "true")
    protected Boolean failOnMissingResource;

    /**
     * Whether to include project dependencies in the module.
     * This will only add the files to the module zip,
     * they have to be included in the manifest to be imported into OpenCms.
     * You can also use the Maven dependency plugin to add dependencies
     * to your vfs lib directory.
     */
    @Parameter(defaultValue = "false")
    protected Boolean addDependencies;

    @Parameter(defaultValue = "true")
    protected Boolean dependenciesWithVersion;

    @Parameter(defaultValue = "false", property = "skipOpenCms")
    protected Boolean skipExecution;

    protected String opencmsVersion = "";

    @Component
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    @Component
    protected MavenProjectBuilder projectBuilder;

    @Component
    protected MavenProjectHelper projectHelper;

    @Component
    protected ArtifactHandlerManager artifactHandlerManager;

    /* a list of dynamically added resources, like: JARs  */
    protected List<ModuleResource> attachedModuleResources = new LinkedList<>();

    public String getModuleVersion() {
        return moduleVersion;
    }

    public void buildModule() throws MojoFailureException, MojoExecutionException {

        File target = new File(targetDir);

        // only create the target directory when it doesn't exist
        // no deletion
        if (!target.exists() && !target.mkdirs()) {
            throw new MojoFailureException("Couldn' create target directory " + target.getAbsolutePath());
        }

        getLog().info("Target is " + target);

        // check wether vfsRoot exists
        File vfsRootF = new File(vfsRoot);
        if (!vfsRootF.exists() || !vfsRootF.isDirectory()) {
            throw new MojoFailureException("Directory doesn't exist: " + vfsRootF.getAbsolutePath());
        }

        Boolean hasClassesDir = false;
        Boolean isClassesUnderVfs = false;
        if (classesDirectory.exists() && classesDirectory.isDirectory()) {
            hasClassesDir = Boolean.TRUE.equals(addClasses);
            isClassesUnderVfs = classesDirectory.getAbsolutePath().startsWith(vfsRootF.getAbsolutePath());
        }

        Iterator files;
        try {
            Resource vfs = new Resource();
            vfs.setDirectory(vfsRoot);

            List<Resource> res = new ArrayList<Resource>();
            res.add(vfs);

            if (hasClassesDir && !isClassesUnderVfs) {
                Resource classes = new Resource();
                classes.setDirectory(classesDirectory.getAbsolutePath());
                classes.setTargetPath("/system/modules/" + moduleName + "/classes");
                res.add(classes);
            }

            if (null != srcResources) {
                for (Resource srcResource : srcResources) {
                    try {
                        FileUtils.getFiles(new File(srcResource.getDirectory()), null, excludes);
                        res.add(srcResource);
                    } catch (IllegalStateException e) {
                        getLog().warn("Resource doesn't exists: " + e.getMessage());
                        getLog().debug(e);
                        if (failOnMissingResource) {
                            throw e;
                        }
                    }
                }
            }

            for (Resource resource : res) {
                File source = new File(resource.getDirectory());
                files = getFilesAnDDirectories(source, null, excludes).iterator();
                String srcPath = source.getAbsolutePath();
                String targetPath = FilenameUtils.normalize(targetDir + getTargetPath(resource));
                while (files.hasNext()) {
                    File file = (File) files.next();
                    if (file.isFile()) {
                        attachFile(srcPath, targetPath, file);
                    } else if (file.isDirectory()) {
                        if (!file.equals(source)) {
                            attachFolder(srcPath, targetPath, file);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not copy file(s) to directory", ex);
        }
    }

    private List<File> getFilesAnDDirectories(File source, String includes, String excludes) throws IOException {
        List<String> directoryNames = FileUtils.getFileAndDirectoryNames(source, includes, excludes, false, true, false, true);
        List<String> fileNames = FileUtils.getFileAndDirectoryNames(source, includes, excludes, false, true, true, false);

        List<File> files = new ArrayList<>();
        for (String filename : directoryNames) {
            files.add(new File(source, filename));
        }

        for (String filename : fileNames) {
            files.add(new File(source, filename));
        }

        return files;
    }

    private void attachFile(String srcPath, String targetPath, File file) throws IOException {
        getLog().debug("Copying " + file.getAbsolutePath());
        File destination = new File(targetPath, file.getAbsolutePath().substring(srcPath.length() + 1));
        FileUtils.copyFile(file, destination);

        String fileVfsPath = StringUtils.removeStart(FilenameUtils.normalize(destination.getAbsolutePath()), targetPath);
        File metaFile = new File(manifestMetaDir, fileVfsPath + ".ocmsfile.xml");

        // when no meta file exists for file, attache as module resource, so we add a file entry later
        if (!metaFile.exists()) {
            attachModuleResource(ModuleResource.ofFile(destination));
        }
    }

    private void attachFolder(String srcPath, String targetPath, File file) throws IOException {
        getLog().debug("Create Directory " + file.getAbsolutePath());
        File destination = new File(targetPath, file.getAbsolutePath().substring(srcPath.length() + 1));
        if (!destination.exists()) {
            destination.mkdir();
        }

        String fileVfsPath = StringUtils.removeStart(FilenameUtils.normalize(destination.getAbsolutePath()), targetPath);
        File metaFile = new File(manifestMetaDir, fileVfsPath + ".ocmsfolder.xml");

        // when no meta file exists for file, attache as module resource, so we add a file entry later
        if (!metaFile.exists()) {
            attachModuleResource(ModuleResource.ofFolder(destination));
        }
    }

    protected void addDependencies() throws MojoExecutionException {

        Set<MavenArtifact> artifacts = getProjectArtifacts();

        OUTER:
        for (MavenArtifact artifact : artifacts) {
            getLog().debug("Processing: " + artifact.getArtifactId());

            if ("opencms-core".equals(artifact.getArtifactId())) {
                opencmsVersion = artifact.getVersion();
            }

            for (String trail : artifact.getDependencyTrail()) {
                getLog().debug(" Trail: " + trail);
                if (trail.contains(":opencms-core:")) {
                    getLog().debug(" Skipping " + artifact.getArtifactId());
                    continue OUTER;
                }
            }

            ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

            if (!artifact.isOptional() && filter.include(artifact.artifact) && "jar".equals(artifact.getType())) {
                copyLibraryToModule(artifact);
            }
        }
    }

    protected void copyLibraryToModule(MavenArtifact artifact) throws MojoExecutionException {
        String libDirectory = getModuleLibDir();

        String finalArtifactName = getArtifactName(artifact);
        getLog().info("Adding " + finalArtifactName);

        try {
            File destination = new File(libDirectory, finalArtifactName);
            FileUtils.copyFile(artifact.getFile(), destination);
            attachModuleResource(new ModuleResource.Jar(destination));
        } catch (IOException e) {
            throw new MojoExecutionException("Could not copy artifact: " + finalArtifactName + " to " + libDirectory, e);
        }
    }

    protected String getModuleLibDir() {
        return targetDir + "/system/modules/" + moduleName + "/lib";
    }

    private String getArtifactName(MavenArtifact artifact) {
        String finalName;

        if (!dependenciesWithVersion) {
            finalName = artifact.getFinalNameNoVersion();
        } else {
            finalName = artifact.getDefaultFinalName();
        }

        return finalName;
    }

    private void generateManifest(File metaDir) throws MojoExecutionException {

        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "false");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");

        OpenCmsModuleManifestGenerator mg = new OpenCmsModuleManifestGenerator();

        try {
            mg.setReplaceMetaVariables(replaceMetaVariables);
            mg.generateManifest(metaDir);
        } catch (OpenCmsMetaXmlParseException e) {
            throw new MojoExecutionException("Error generating manifest", e);
        } catch (OpenCmsMetaXmlFileWriteException e) {
            throw new MojoExecutionException("Error generating manifest", e);
        }
    }

    private void fixTimestamp(File metaDir) throws MojoExecutionException {
        List<File> metaFiles;
        try {
            String vfsRootCanon = new File(vfsRoot).getCanonicalPath();
            String metaDirCanon = metaDir.getCanonicalPath();
            metaFiles = FileUtils.getFiles(metaDir, "**/*.ocmsfile.xml", excludes);
            for (File metaFile : metaFiles) {
                File realFile = new File(metaFile.getCanonicalPath().replace(metaDirCanon, vfsRootCanon).replaceFirst("\\.ocmsfile\\.xml$", ""));
                if (realFile.isFile() && realFile.exists()) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Set " + metaFile.getCanonicalPath() + " from " + realFile.getCanonicalPath());
                    }
                    if (!metaFile.setLastModified(realFile.lastModified())) {
                        getLog().warn("Failed to set last modified on " + metaFile.getCanonicalPath());
                    }
                } else {
                    getLog().warn("*** Missing file referenced in manifest ***");
                    getLog().warn(realFile.getAbsolutePath() + " for " + metaFile.getAbsolutePath() + " not found");
                    getLog().warn("*** Check your meta files!! ***");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void addManifest() throws MojoExecutionException {

        if ((manifestFile == null || manifestFile.isEmpty()) && (manifestMetaDir == null || manifestMetaDir.isEmpty())) {
            throw new MojoExecutionException("Neither manifestFile nor manifestMetaDir given");
        }

        File src;
        boolean isGenerated = false;

        if (manifestMetaDir != null && !manifestMetaDir.isEmpty()) {
            File metaDir = new File(manifestMetaDir);
            if (!metaDir.exists() || !metaDir.isDirectory()) {
                throw new MojoExecutionException("Could not find manifest meta directory: " + manifestMetaDir);
            }
            fixTimestamp(metaDir);
            isGenerated = true;
            generateManifest(metaDir);
            src = new File(manifestMetaDir, "manifest.xml");
        } else {
            src = new File(manifestFile);
        }

        if (!src.exists() || !src.isFile()) {
            throw new MojoExecutionException("Could not find " + (isGenerated ? "generated" : "" + " manifest: ") + src.getAbsolutePath());
        }

        try {
            File manifestTarget = new File(targetDir, "manifest.xml");
            FileUtils.copyFile(src, manifestTarget, "UTF-8", filters(true));

            addAttachedResourcesToManifest(manifestTarget);
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not copy manifest", ex);
        }
    }

    private void addAttachedResourcesToManifest(File src) throws MojoExecutionException {
        try {
            ModuleManifest moduleManifest = new ModuleManifest(src)
                    .setLog(getLog());
            for (ModuleResource moduleResource : attachedModuleResources) {
                moduleManifest.addResource(moduleResource);
            }

            moduleManifest.write(new FileOutputStream(src));
        } catch (OpenCmsMetaXmlParseException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public File getClassesDirectory() {
        return classesDirectory;
    }

    protected String getTargetPath(Resource r) {
        try {
            return (null == r.getTargetPath()) ? "" : "/" + createPropertyInterpolator().interpolate(r.getTargetPath());
        } catch (InterpolationException e) {
            throw new RuntimeException(e);
        }
    }

    protected Set<MavenArtifact> getProjectArtifacts() {
        return wrap(Artifacts.of(project));
    }

    protected Set<MavenArtifact> wrap(Iterable<Artifact> artifacts) {
        Set<MavenArtifact> r = new HashSet<MavenArtifact>();
        for (Artifact a : artifacts) {
            r.add(wrap(a));
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    protected MavenArtifact wrap(Artifact a) {
        return new MavenArtifact(a, projectBuilder, project.getRemoteArtifactRepositories(), localRepository);
    }

    @SuppressWarnings("unchecked")
    private FileUtils.FilterWrapper[] filters(boolean filter) throws IOException {
        if (!filter) {
            return new FileUtils.FilterWrapper[0];
        }

        FileUtils.FilterWrapper wrapper = new FileUtils.FilterWrapper() {
            public Reader getReader(Reader reader) {
                Interpolator propertiesInterpolator = createPropertyInterpolator();
                InterpolatorFilterReader interpolatorFilterReader = new InterpolatorFilterReader(reader, propertiesInterpolator);
                interpolatorFilterReader.setInterpolateWithPrefixPattern(false);
                return interpolatorFilterReader;
            }
        };
        return new FileUtils.FilterWrapper[]{wrapper};
    }

    public void setModuleVersion(String version) {
        // modify version number to be OpenCms compatible (containing only digits
        this.moduleVersion = extractVersionNumber(version);
    }

    public void setModuleDir(String moduleDir) {
        this.moduleDir = moduleDir;
    }

    public void setTargetDir(String targetDir){
        this.targetDir = targetDir;
    }

    private String extractVersionNumber(String originalVersionNumber) {
        // check if there are non-digit letters at the end
        if (originalVersionNumber != null && originalVersionNumber.matches(".*\\D+$")) {
            // remove anything after the last digit
            originalVersionNumber = originalVersionNumber.replaceAll("\\D+$", "");
        }
        return originalVersionNumber;
    }

    protected Interpolator createPropertyInterpolator() {
        String escapeString = "\\";
        Map<String, String> values = new HashMap<>();

        values.put("moduleversion", project.getProperties().getProperty("moduleversion", moduleVersion));
        values.put("modulename", project.getProperties().getProperty("modulename", moduleName));
        values.put("modulenicename", project.getProperties().getProperty("modulenicename", project.getName()));
        values.put("moduledescription", project.getProperties().getProperty("moduledescription", project.getDescription()));
        values.put("opencmsversion", project.getProperties().getProperty("opencmsversion", opencmsVersion));

        StringSearchInterpolator propertiesInterpolator = new StringSearchInterpolator();
        propertiesInterpolator.addValueSource(new MavenProjectValueSource(project, true));
        propertiesInterpolator.addValueSource(new PropertiesBasedValueSource(project.getProperties()));
        propertiesInterpolator.addValueSource(new MapBasedValueSource(values));
        propertiesInterpolator.setEscapeString(escapeString);

        return propertiesInterpolator;
    }

    protected void attachModuleResource(ModuleResource resource) {
        attachedModuleResources.add(resource);
    }

    public boolean isSkipExecution() {
        return skipExecution != null && skipExecution;
    }
}
