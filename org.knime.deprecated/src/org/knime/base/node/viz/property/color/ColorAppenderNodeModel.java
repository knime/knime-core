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

import static org.knime.base.node.viz.property.color.ColorNodeModel.SELECTED_COLUMN;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Node model to append color settings to a (new) column selected in the dialog.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorAppenderNodeModel extends NodeModel {

    private final ColorNodeModel m_colorModel;
    
    /**
     * Create model.
     * 
     * @param dataIns number of data ins
     * @param dataOuts number of data outs
     * @param modelIns number of model ins
     * @param modelOuts number of model outs
     */
    public ColorAppenderNodeModel(final int dataIns, final int dataOuts,
            final int modelIns, final int modelOuts) {
        super(dataIns, dataOuts, modelIns, modelOuts);
        m_colorModel = new ColorNodeModel();
    }

    /**
     * Load color model into the super color model.
     * 
     * @param index always the first model input
     * @param predParams the color model to load
     * @throws InvalidSettingsException if the color settings could not be
     *             loaded in the super color model
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        assert index == 0;
        if (predParams == null) {
            m_colorModel.resetColorMapping();
        } else {
            m_colorModel.validateSettings(predParams);
            String column = m_colorModel.getSelectedColumn();
            m_colorModel.loadValidatedSettingsFrom(predParams);
            if (column != null) {
                m_colorModel.setSelectedColumn(column);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String column = settings.getString(SELECTED_COLUMN, null);
        if (column == null) {
            throw new InvalidSettingsException("No column selected.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String column = settings.getString(SELECTED_COLUMN);
        m_colorModel.setSelectedColumn(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        String column = m_colorModel.getSelectedColumn();
        settings.addString(SELECTED_COLUMN, column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return m_colorModel.configure(inSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, 
            final ExecutionContext exec) throws Exception {
        return m_colorModel.execute(inData, exec);
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
}
