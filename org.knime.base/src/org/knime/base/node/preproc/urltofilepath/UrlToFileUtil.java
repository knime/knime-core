/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   17.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.urltofilepath;

import java.io.File;

/**
 * Utility class to extract parent folder, file name, and file extension from 
 * file.
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
public final class UrlToFileUtil {
    
    private UrlToFileUtil() { /*empty*/ }

    /**
     * Extracts parent folder from file.
     * @param f The file to extract parent folder from.
     * @return The parent folder of file as string.
     */
    public static String getParentFolder(final File f) {
        return f.getParent();
    }
    
    /**
     * Extracts file name from file.
     * @param f The file to extract file name, without extension from.
     * @return The file name without extension of file as string.
     */
    public static String getFileName(final File f) {
        String fileName = f.getName();
        int i = fileName.lastIndexOf(".");
        if (i >= 0) {
            fileName = fileName.substring(0, i);
        }
        return fileName;
    }
    
    /**
     * Extracts file extension from file.
     * @param f The file to extract file extension from.
     * @return The file extension of file as string.
     */    
    public static String getFileExtension(final File f) {
        String extension = f.getName();
        int i = extension.lastIndexOf(".");
        if (i >= 0) {
            extension = extension.substring(i + 1);
        } else {
            extension = "";
        }
        return extension;
    }
}
