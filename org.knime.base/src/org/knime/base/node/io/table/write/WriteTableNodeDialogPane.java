/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   May 19, 2006 (wiswedel): created
 */
package org.knime.base.node.io.table.write;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentFileChooser;

import org.knime.base.node.io.table.read.ReadTableNodeModel;

/**
 * Dialog for the node to write arbitrary tables to a file. It only shows
 * a file chooser dialog.
 * @author wiswedel, University of Konstanz
 */
public class WriteTableNodeDialogPane extends DefaultNodeDialogPane {

    /**
     * Creates new dialog.
     */
    public WriteTableNodeDialogPane() {
        super();
        addDialogComponent(new DialogComponentFileChooser(
                WriteTableNodeModel.CFG_FILENAME, JFileChooser.SAVE_DIALOG, 
                ReadTableNodeModel.PREFERRED_FILE_EXTENSION));
    }

}
