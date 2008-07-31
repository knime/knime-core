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
 *   23.05.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node model to append color settings to a column selected in the dialog.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorAppender2NodeModel extends NodeModel {
    
    private ColorHandler m_colorHandler = null;
    
    private final SettingsModelString m_column = 
        ColorAppender2NodeDialogPane.createColumnModel();
    
    /**
     * Create a new color appender model.
     * @param dataIns data ins
     * @param dataOuts data outs
     * @param modelIns model ins
     * @param modelOuts modelOuts
     */
    public ColorAppender2NodeModel(final int dataIns, final int dataOuts,
            final int modelIns, final int modelOuts) {
        super(dataIns, dataOuts, modelIns, modelOuts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (!inSpecs[0].containsName(m_column.getStringValue())) {
            throw new InvalidSettingsException("Column not available.");
        }
        if (m_colorHandler == null) {
            throw new InvalidSettingsException("Color model not available.");
        }
        DataTableSpec spec = ColorManager2NodeModel.appendColorManager(
                inSpecs[0], m_column.getStringValue(), m_colorHandler);
        return new DataTableSpec[]{spec};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = ColorManager2NodeModel.appendColorManager(
                inData[0].getDataTableSpec(), m_column.getStringValue(), 
                m_colorHandler);
        BufferedDataTable table = exec.createSpecReplacerTable(inData[0], spec);
        return new BufferedDataTable[]{table};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_column.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_column.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_column.validateSettings(settings);
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadModelContent(final int index, 
            final ModelContentRO predParams)
            throws InvalidSettingsException {
        if (predParams != null) {
            m_colorHandler = ColorHandler.load(predParams);
        } else {
            m_colorHandler = null;
        }
    }

}
