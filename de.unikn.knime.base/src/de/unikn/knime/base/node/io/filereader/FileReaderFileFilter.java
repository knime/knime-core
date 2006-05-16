/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.node.io.filereader;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Our File Filter for the JFileChooser Dialog
 * You can create one accepting all files ending with one extension ".ext".
 * Other pattern matches are not supported.
 * 
 * @author ohl University of Konstanz
 */
public class FileReaderFileFilter extends FileFilter {

    private final String m_ext;
    private final String m_description;

    /**
     * creates a new FileFilter that accepts files matching "*.'ext'".
     * @param ext the extension 
     * @param description the readable description (like "XML files")
     */
    public FileReaderFileFilter(final String ext, final String description) {
        if ((ext == null) || (description == null)) {
            throw new NullPointerException();
        }
        if (ext.lastIndexOf('.') >= 0) {
            throw new IllegalArgumentException(
                "FileFilter: Ext must not contain '.'");
        }
        m_ext = ext;
        m_description = description;
    }

    /**
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    public boolean accept(final File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            return match(f.getName());
        }
        return true;

    }

    private boolean match(final String name) {
        if ((name == null) || (name.length() == 0)) {
            return false;
        }
        int dot = name.lastIndexOf('.');
        if ((dot >= 0) && (dot < name.length())) {
            return m_ext.equalsIgnoreCase(name.substring(dot + 1));
        }
        return false;
    }
    /**
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    public String getDescription() {
        return m_description;
    }

}
