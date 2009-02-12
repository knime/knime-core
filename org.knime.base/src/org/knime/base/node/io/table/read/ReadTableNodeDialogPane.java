/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   May 19, 2006 (wiswedel): created
 */
package org.knime.base.node.io.table.read;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Simple dialog with just a file browser.
 * @author wiswedel, University of Konstanz
 */
public class ReadTableNodeDialogPane extends DefaultNodeSettingsPane {

    /**
     */
    public ReadTableNodeDialogPane() {
        addDialogComponent(new DialogComponentFileChooser(
                new SettingsModelString(ReadTableNodeModel.CFG_FILENAME, ""),
                ReadTableNodeDialogPane.class.getName(), 
                JFileChooser.OPEN_DIALOG, 
                ReadTableNodeModel.PREFERRED_FILE_EXTENSION));
    }

}
