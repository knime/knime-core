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
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;

/**
 * Enhances the default {@link DirectoryFieldEditor} to check for validity after
 * each key stroke.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class TempDirFieldEditor extends DirectoryFieldEditor {

    /**
     * Creates a temp directory field editor.
     * 
     * @param name
     *            the name of the preference this field editor works on
     * @param labelText
     *            the label text of the field editor
     * @param parent
     *            the parent of the field editor's control
     */
    public TempDirFieldEditor(final String name, final String labelText,
            final Composite parent) {
        super(name, labelText, parent);

        // registers a key listener
        getTextControl(parent).addKeyListener(new KeyListener() {
            public void keyPressed(final KeyEvent e) {
                // do nothing
            }

            public void keyReleased(final KeyEvent e) {
                // perform a validity check each time a key is pressed on this
                // directory field
                refreshValidState();
                boolean valid = checkState();
                fireStateChanged(IS_VALID, !valid, valid);
            }
        });
    }
}
