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
 *   23.05.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Node model to append color settings to a (new) column selected in the dialog.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorAppenderNodeModel extends ColorManagerNodeModel {
    private NodeSettings m_settings = null;

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
            m_settings = null;
            throw new InvalidSettingsException("Color model not available.");
        }
        m_settings = new NodeSettings(predParams.getKey());
        predParams.copyTo(m_settings);
        String column = super.getSelectedColumn();
        if (column != null) {
            m_settings.addString(SELECTED_COLUMN, column);
        }
        super.validateSettings(m_settings);
        super.loadValidatedSettingsFrom(m_settings);
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        checkColorModel();
        String column = settings.getString(SELECTED_COLUMN);
        m_settings.addString(SELECTED_COLUMN, column);
        super.validateSettings(m_settings);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        checkColorModel();
        String column = settings.getString(SELECTED_COLUMN);
        m_settings.addString(SELECTED_COLUMN, column);
        super.loadValidatedSettingsFrom(m_settings);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        String column = super.getSelectedColumn();
        if (column == null) {
            if (m_settings != null) {
                column = m_settings.getString(SELECTED_COLUMN, null);
            }
        }
        settings.addString(SELECTED_COLUMN, column);
    }

    /**
     * @see org.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkColorModel();
        return super.configure(inSpecs);
    }

    private void checkColorModel() throws InvalidSettingsException {
        if (m_settings == null) {
            throw new InvalidSettingsException("Color model not available.");
        }
    }
}
