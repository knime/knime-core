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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.ViewPropertyPortObject;

/**
 * Node model to append color settings to a column selected in the dialog.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorAppender2NodeModel extends GenericNodeModel {
    
    private final SettingsModelString m_column = 
        ColorAppender2NodeDialogPane.createColumnModel();
    
    /**
     * Create a new color appender model.
     */
    public ColorAppender2NodeModel() {
        super(new PortType[]{
                ViewPropertyPortObject.TYPE, BufferedDataTable.TYPE},
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
            throw new InvalidSettingsException("No color information in input");
        }
        DataColumnSpec col = modelSpec.getColumnSpec(0);
        ColorHandler colorHandler = col.getColorHandler();
        if (col.getColorHandler() == null) {
            throw new InvalidSettingsException("No color information in input");
        }
        String column = m_column.getStringValue();
        if (column == null) { // auto-configuration/guessing
            // TODO check for nominal/range coloring and guess last
            // suitable column (while setting a warning message)
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
        DataTableSpec spec = ColorManager2NodeModel.getOutSpec(
                dataSpec, m_column.getStringValue(), colorHandler);
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
