/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   May 19, 2006 (wiswedel): created
 */
package de.unikn.knime.base.node.io.table.read;

import javax.swing.JFileChooser;

import de.unikn.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import de.unikn.knime.core.node.defaultnodedialog.DialogComponentFileChooser;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ReadTableNodeDialogPane extends DefaultNodeDialogPane {

    /**
     */
    public ReadTableNodeDialogPane() {
        super("Read Table");
        addDialogComponent(new DialogComponentFileChooser(
                ReadTableNodeModel.CFG_FILENAME, 
                JFileChooser.OPEN_DIALOG, ".zip"));
    }

}
