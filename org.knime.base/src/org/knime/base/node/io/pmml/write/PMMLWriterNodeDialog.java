/* This source code, its documentation and all appendant files
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
 */
package org.knime.base.node.io.pmml.write;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLWriterNodeDialog extends DefaultNodeSettingsPane {
    
    /**
     * 
     */
    public PMMLWriterNodeDialog() {
        addDialogComponent(new DialogComponentFileChooser(
                createFileModel(), "pmml.writer.history", 
                JFileChooser.SAVE_DIALOG, ".pmml", ".xml"));
    }
    
    /**
     * 
     * @return name of PMML file model
     */
    static SettingsModelString createFileModel() {
        return new SettingsModelString("PMMLWriterFile", "");
    }

}
