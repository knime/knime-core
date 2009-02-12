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
package org.knime.base.node.io.pmml.read;

import java.io.File;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLReaderNodeDialog extends DefaultNodeSettingsPane {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            PMMLReaderNodeDialog.class);
//        
//    
    private final SettingsModelString m_fileNameModel;
    
    /**
     * 
     */
    public PMMLReaderNodeDialog() {
        m_fileNameModel = createFileChooserModel();
        addDialogComponent(new DialogComponentFileChooser(
                m_fileNameModel, "pmml.reader", ".pmml", ".xml"));
    }
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // adding the specific port object class name. 
        try {
            PMMLImport.isModelSupported(new File(
                    m_fileNameModel.getStringValue()));
        } catch (SAXException e) {
            throw new InvalidSettingsException(e);
        }
        super.saveAdditionalSettingsTo(settings);
    }
    
    
    /**
     * 
     * @return model for PMML file
     */
    static SettingsModelString createFileChooserModel() {
        return new SettingsModelString("pmml.reader.file", "");

    }
}
