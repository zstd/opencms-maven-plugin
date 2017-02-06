/**
 * Copyright (c) 2014 mediaworx berlin AG (http://mediaworx.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about mediaworx berlin AG, please see the
 * company website: http://mediaworx.com
 *
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
 * Creates the exploded OpenCms module
 */
@Mojo(name = "module-exploded",
      defaultPhase = LifecyclePhase.PACKAGE,
      requiresProject = true,requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ModuleExplodedMojo extends AbstractOpenCmsMojo {
  /**
   * Creates an exploded OpenCms module in targetDir.
   *
   * @throws org.apache.maven.plugin.MojoExecutionException
   *
   * @throws org.apache.maven.plugin.MojoFailureException
   *
   */
  public void execute() throws MojoExecutionException, MojoFailureException {

    buildModule();

    if (addDependencies) {
      addDependencies();
    }

    addManifest();

  }
}
