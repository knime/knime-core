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
 *   23.05.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.size;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.SizeHandlerPortObject;
import org.knime.core.node.port.viewproperty.ViewPropertyPortObject;

/**
 * Node model to append size settings to a (new) column selected in the dialog.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class SizeAppenderNodeModel extends NodeModel {

    private final SettingsModelString m_column = 
        SizeAppenderNodeDialogPane.createColumnModel();
    
    /**
     * Create size appender model with one data in- and out-port, and one
     * model out-port.
     */
    public SizeAppenderNodeModel() {
        super(new PortType[]{
                SizeHandlerPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec modelSpec = (DataTableSpec)inSpecs[0];
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        DataTableSpec out = createOutputSpec(modelSpec, dataSpec);
        return new DataTableSpec[]{out};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec modelSpec = ((ViewPropertyPortObject)inData[0]).getSpec();
        DataTableSpec dataSpec = ((BufferedDataTable)inData[1]).getSpec();
        DataTableSpec outSpec = createOutputSpec(modelSpec, dataSpec);
        BufferedDataTable table = exec.createSpecReplacerTable(
                (BufferedDataTable)inData[1], outSpec);
        return new BufferedDataTable[]{table};
    }
    
    private DataTableSpec createOutputSpec(final DataTableSpec modelSpec, 
            final DataTableSpec dataSpec) throws InvalidSettingsException {
        if (modelSpec == null || dataSpec == null) {
            throw new InvalidSettingsException("Invalid input.");
        }
        if (modelSpec.getNumColumns() < 1) {
            throw new InvalidSettingsException("No size information in input");
        }
        DataColumnSpec col = modelSpec.getColumnSpec(0);
        SizeHandler sizeHandler = col.getSizeHandler();
        if (col.getSizeHandler() == null) {
            throw new InvalidSettingsException("No size information in input");
        }
        String column = m_column.getStringValue();
        if (column == null) { // auto-configuration/guessing
            if (dataSpec.containsName(col.getName())) {
                column = col.getName();
            }
        }
        if (column == null) {
            throw new InvalidSettingsException("Not configured.");
        }
        if (!dataSpec.containsName(column)) {
            throw new InvalidSettingsException("Column \"" + column 
                    + "\" not available.");
        }
        DataTableSpec spec = SizeManager2NodeModel.appendSizeHandler(
                dataSpec, column, sizeHandler);
        return spec;
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

}
