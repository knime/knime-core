/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.SizeHandlerPortObject;

/**
 * A node model for setting sizes using the double values of a column specified
 * using the <code>SizeManagerNodeDialog</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class SizeManager2NodeModel extends NodeModel {
    
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
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
                BufferedDataTable.TYPE, SizeHandlerPortObject.TYPE});
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
    protected PortObject[] execute(final PortObject[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        final DataTableSpec inSpec = (DataTableSpec) data[INPORT].getSpec();
        final String columnName = m_column.getStringValue();
        final DataColumnSpec cspec = inSpec.getColumnSpec(columnName);
        SizeHandler sizeHandler = createSizeHandler(cspec);
        final DataTableSpec newSpec = appendSizeHandler(inSpec, 
                columnName, sizeHandler);
        BufferedDataTable changedSpecTable = exec.createSpecReplacerTable(
                (BufferedDataTable) data[INPORT], newSpec);
        DataTableSpec modelSpec = new DataTableSpec(
                newSpec.getColumnSpec(m_column.getStringValue()));
        SizeHandlerPortObject viewPort = new SizeHandlerPortObject(modelSpec, 
                sizeHandler.toString() + " based on column \"" 
                + m_column.getStringValue() + "\"");
        return new PortObject[]{changedSpecTable, viewPort};
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

    }

    /**
     * @param inSpecs the input specs passed to the output port
     * @return the same as the input spec
     * 
     * @throws InvalidSettingsException if a column is not available
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        final String column = m_column.getStringValue();
        DataTableSpec inSpec = (DataTableSpec) inSpecs[INPORT];
        if (column == null || !inSpec.containsName(column)) {
            throw new InvalidSettingsException("Column " + column
                    + " not found.");
        }
        DataColumnSpec cspec = inSpec.getColumnSpec(column);
        if (!cspec.getDomain().hasBounds()) {
            throw new InvalidSettingsException("No bounds defined for column: "
                    + column);
        }
        SizeHandler sizeHandler = createSizeHandler(cspec);
        DataTableSpec outSpec = appendSizeHandler(inSpec, 
                m_column.getStringValue(), sizeHandler);
        DataTableSpec modelSpec = new DataTableSpec(
                outSpec.getColumnSpec(m_column.getStringValue()));
        return new DataTableSpec[]{outSpec, modelSpec};
    }
    
    /**
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
            // ignore it: added somewhere in between
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
