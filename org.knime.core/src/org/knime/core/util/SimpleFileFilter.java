/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2006 (gabriel): created
 */
package org.knime.core.util;

import java.io.File;

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
     * mathcing the given extensions.
     * 
     * @param exts allowed extensions
     */
    public SimpleFileFilter(final String... exts) {
        m_validExtensions = exts;
    }

    /**
     * @see java.io.FileFilter#accept(java.io.File)
     */
    @Override
    public boolean accept(final File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            String fileName = f.getName();
            for (String ext : m_validExtensions) {
                if (fileName.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    @Override
    public String getDescription() {
        String descr = "";
        for (String ext : m_validExtensions) {
            descr = descr + " " + ext.toString();
        }
        return descr;
    }
}
