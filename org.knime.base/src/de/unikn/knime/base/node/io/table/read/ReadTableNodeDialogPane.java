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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   May 19, 2006 (wiswedel): created
 */
package de.unikn.knime.base.node.io.table.read;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentFileChooser;


/**
 * Simple dialog with just a file browser.
 * @author wiswedel, University of Konstanz
 */
public class ReadTableNodeDialogPane extends DefaultNodeDialogPane {

    /**
     */
    public ReadTableNodeDialogPane() {
        super();
        addDialogComponent(new DialogComponentFileChooser(
                ReadTableNodeModel.CFG_FILENAME, JFileChooser.OPEN_DIALOG, 
                ReadTableNodeModel.PREFERRED_FILE_EXTENSION));
    }

}
