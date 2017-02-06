/**
 * Copyright (c) 2014 Lars Brandt
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

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class ModuleResource {

    private String type;
    private File file;

    public ModuleResource(String type, File file) {
        this.type = type;
        this.file = file;
    }

    public String getType() {
        return type;
    }

    public File getFile() {
        return file;
    }

    public String getVfsPath(String reference) {
        String thisPath = file.getAbsolutePath();
        String s = StringUtils.removeStart(thisPath, reference + File.separator);
        return s.replaceAll("\\\\", "/"); // windows crap
    }

    public static class Jar extends ModuleResource {

        public Jar(File file) {
            super("binary", file);
        }
    }

    public static class Plain extends ModuleResource {

        public Plain(File file) {
            super("plain", file);
        }
    }

    public static class Image extends ModuleResource {

        public Image(File file) {
            super("image", file);
        }
    }

    public static class Folder extends ModuleResource {
        public Folder(File file) {
            super("folder", file);
        }
    }

    public static ModuleResource ofFile(File file) {
        ModuleResource result = null;
        String lowCaseExt = file.getName();
        int dot = lowCaseExt.lastIndexOf('.');
        if (dot != -1) {
            lowCaseExt = lowCaseExt.substring(dot + 1);
        }

        if ("jpg".equals(lowCaseExt)
            || "jpeg".equals(lowCaseExt)
            || "png".equals(lowCaseExt)
            || "gif".equals(lowCaseExt)) {
            result = new Image(file);
        }

        return (result != null) ? result : new Plain(file);
    }

    public static ModuleResource ofFolder(File file) {
        return new Folder(file);
    }
}
