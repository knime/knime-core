/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
