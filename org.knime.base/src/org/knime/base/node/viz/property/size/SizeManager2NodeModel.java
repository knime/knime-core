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
 *   02.02.2006 (mb): created
 */
package org.knime.base.node.viz.property.size;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.data.property.SizeModelDouble;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * A node model for setting sizes using the double values of a column specified
 * using the <code>SizeManagerNodeDialog</code>.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class SizeManager2NodeModel extends NodeModel {
    
    /** SizeHandler generated during executed and save into the model port. */
    private SizeHandler m_sizeHandler;
    
    /** The selected column. */
    private final SettingsModelString m_column = 
        SizeManager2NodeDialogPane.createColumnModel();
    
    private final SettingsModelDouble m_factor =
        SizeManager2NodeDialogPane.createFactorModel();
    
    private final SettingsModelString m_mapping = 
        SizeManager2NodeDialogPane.createMappingModel();

    /** Keeps port number for the single input port. */
    static final int INPORT = 0;

    /** Keeps port number for the single input port. */
    static final int OUTPORT = 0;

    /**
     * Creates a new model for mapping sizes. The model has one input and one
     * output.
     */
    SizeManager2NodeModel() {
        super(1, 1, 0, 1);
    }

    /**
     * Is invoked during the node's execution to make the size settings.
     * 
     * @param data the input data array
     * @param exec the execution monitor
     * @return the same input data table whereby the DataTableSpec contains
     *         additional size infos
     * @throws CanceledExecutionException if user canceled execution
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        final DataTableSpec inSpec = data[INPORT].getDataTableSpec();
        final String columnName = m_column.getStringValue();
        final DataColumnSpec cspec = inSpec.getColumnSpec(columnName);
        m_sizeHandler = createSizeHandler(cspec);
        final DataTableSpec newSpec = appendSizeHandler(inSpec, 
                columnName, m_sizeHandler);
        BufferedDataTable changedSpecTable = exec.createSpecReplacerTable(
                data[INPORT], newSpec);
        // return original table with SizeHandler
        return new BufferedDataTable[]{changedSpecTable};
    }
    
    /**
     * Appends the given <code>SizeHandler</code> to the given 
     * <code>DataTableSpec</code> for the given column. If the spec
     * already contains a SizeHandler, it will be removed and replaced by
     * the new one.
     * @param spec to which the SizeHandler is appended
     * @param column for this column
     * @param sizeHandler SizeHandler
     * @return a new spec with SizeHandler
     */
    static final DataTableSpec appendSizeHandler(final DataTableSpec spec, 
            final String column, final SizeHandler sizeHandler) {
        DataColumnSpec[] cspecs = new DataColumnSpec[spec.getNumColumns()];
        for (int i = 0; i < cspecs.length; i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            DataColumnSpecCreator cr = new DataColumnSpecCreator(cspec);
            if (cspec.getName().equals(column)) {
                cr.setSizeHandler(sizeHandler);
            } else {
                // delete other SizeHandler
                cr.setSizeHandler(null);
            }
            cspecs[i] = cr.createSpec();
        }
        return new DataTableSpec(cspecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_sizeHandler = null;
    }

    /**
     * @param inSpecs the input specs passed to the output port
     * @return the same as the input spec
     * 
     * @throws InvalidSettingsException if a column is not available
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final String column = m_column.getStringValue();
        if (column == null || !inSpecs[INPORT].containsName(column)) {
            throw new InvalidSettingsException("Column " + column
                    + " not found.");
        }
        DataColumnSpec cspec = inSpecs[INPORT].getColumnSpec(column);
        if (!cspec.getDomain().hasBounds()) {
            throw new InvalidSettingsException("No bounds defined for column: "
                    + column);
        }
        SizeHandler sizeHandler = createSizeHandler(cspec);
        DataTableSpec outSpec = appendSizeHandler(inSpecs[0], 
                m_column.getStringValue(), sizeHandler);
        // return original spec with SizeHandler
        return new DataTableSpec[]{outSpec};
    }
    
    /*
     * Create SizeHandler based on given DataColumnSpec. 
     * @param cspec spec with minimum and maximum bound
     * @return SizeHandler
     */
    private SizeHandler createSizeHandler(final DataColumnSpec cspec) {
        // get the domain range for the double size handler
        double minimum = ((DoubleValue) cspec.getDomain().getLowerBound())
                .getDoubleValue();
        double maximum = ((DoubleValue) cspec.getDomain().getUpperBound())
                .getDoubleValue();
        return new SizeHandler(new SizeModelDouble(minimum, maximum, 
                m_factor.getDoubleValue(),
                SizeModelDouble.Mapping.valueOf(m_mapping.getStringValue())));
    }
    
    /**
     * Saves the size settings to <code>ModelContent</code> object.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        m_sizeHandler.save(predParams);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_column.loadSettingsFrom(settings);
        try {
            m_factor.loadSettingsFrom(settings);
            m_mapping.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_factor.setDoubleValue(2);
            m_mapping.setStringValue(SizeModelDouble.Mapping.LINEAR.name());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_column.saveSettingsTo(settings);
        m_factor.saveSettingsTo(settings);
        m_mapping.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_column.validateSettings(settings);
        try {
            m_factor.validateSettings(settings);
            m_mapping.validateSettings(settings);
        } catch (InvalidSettingsException ise) {
            // ignore it
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }
}
