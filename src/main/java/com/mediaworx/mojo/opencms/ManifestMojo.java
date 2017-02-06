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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Creates or copies a module manifest.
 */
@Mojo(name = "manifest",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ManifestMojo extends AbstractOpenCmsMojo {
    /**
     * Creates a OpenCms manifest either by copying and filtering an existing manifest.xml given with ${manifestFile}
     * or using the stub file ${manifestMetaDir} and additional meta information generated by the IntelliJ OpenCms plugin
     * in the same directory as the stub file.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        addManifest();
    }
}
