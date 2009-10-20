/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2006 (gabriel): created
 */
package org.knime.core.util;

import java.io.File;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

/**
 * Helper class filtering out all files not matching extensions.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class SimpleFileFilter extends FileFilter {
    private final String[] m_validExtensions;

    /**
     * Creates a new simple file filter that filters out all files not
     * matching the given extensions.
     * 
     * @param exts allowed extensions
     * @throws NullPointerException if the extensions array or one of its 
     *          elements is null
     */
    public SimpleFileFilter(final String... exts) {
        if (exts == null) {
            throw new NullPointerException("Extensions must not be null");
        }
        if (Arrays.asList(exts).contains(null)) {
            throw new NullPointerException("Extensions must not contain null"
                + " elements: " + Arrays.toString(exts));
        }
        m_validExtensions = exts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(final File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            if (m_validExtensions.length == 0) {
                return true;
            }
            String fileName = f.getName();
            for (String ext : m_validExtensions) {
                if (fileName.toLowerCase().endsWith(ext.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return all extensions which are accepted by this filter
     */
    public String[] getValidExtensions() {
        return m_validExtensions;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        String descr = "";
        for (String ext : m_validExtensions) {
            descr = descr + " ; *" + ext.toString();
        }
        if (descr.length() > 3) {
            return descr.substring(3);
        }
        return descr;
    }
}
