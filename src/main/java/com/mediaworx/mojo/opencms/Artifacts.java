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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Collection filter operations on a set of {@link Artifact}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class Artifacts extends ArrayList<Artifact> {
  public Artifacts() {
  }

  public Artifacts(Collection<? extends Artifact> c) {
    super(c);
  }

  /**
   * Return the {@link Artifact}s representing dependencies of the given project.
   * <p/>
   * A thin-wrapper of p.getArtifacts()
   */
  @SuppressWarnings("unchecked")
  public static Artifacts of(MavenProject p) {
    return new Artifacts(p.getArtifacts());
  }
  @SuppressWarnings("unchecked")
  public static Artifacts ofDirectDependencies(MavenProject p) {
    return new Artifacts(p.getDependencyArtifacts());
  }
}
