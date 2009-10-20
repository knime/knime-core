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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.property.shape;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ShapeFactory;
import org.knime.core.data.property.ShapeHandler;
import org.knime.core.data.property.ShapeModelNominal;
import org.knime.core.data.property.ShapeFactory.Shape;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.ShapeHandlerPortObject;

/**
 * Model used to set shapes by nominal values retrieved from the
 * {@link org.knime.core.data.DataColumnSpec} domain. The created
 * {@link org.knime.core.data.property.ShapeHandler} is then set in the column
 * spec.
 * 
 * @see ShapeManagerNodeDialogPane
 * @see ShapeHandler
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class ShapeManagerNodeModel extends NodeModel {

    /** Logger for this package. */
    static final NodeLogger LOGGER = NodeLogger.getLogger("Shape Manager");

    /** Stores the mapping from column value to shape. */
    private final Map<DataCell, Shape> m_map;

    /** The selected column. */
    private String m_column;

    /** Keeps port number for the single input port. */
    static final int INPORT = 0;

    /** Keeps port number for the single input port. */
    static final int OUTPORT = 0;

    /** Keeps the selected column. */
    static final String SELECTED_COLUMN = "selected_column";

    /** The nominal column values. */
    static final String VALUES = "values";

    /**
     * Creates a new model for mapping shapes.
     */
    ShapeManagerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
                BufferedDataTable.TYPE, ShapeHandlerPortObject.TYPE});
        m_map = new LinkedHashMap<DataCell, Shape>();
    }

    /**
     * Is invoked during the node's execution to make the shape settings.
     * 
     * @param data the input data array
     * @param exec the execution monitor
     * @return the same input data table with assigned shapes to one column
     * @throws CanceledExecutionException if user canceled execution
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected PortObject[] execute(final PortObject[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        BufferedDataTable inData = (BufferedDataTable)data[INPORT];
        ShapeHandler shapeHandler =
                new ShapeHandler(new ShapeModelNominal(m_map));
        final DataTableSpec newSpec =
                appendShapeHandler(inData.getSpec(), m_column, shapeHandler);
        BufferedDataTable changedSpecTable =
                exec.createSpecReplacerTable(inData, newSpec);
        DataTableSpec modelSpec =
                new DataTableSpec(newSpec.getColumnSpec(m_column));
        ShapeHandlerPortObject viewPort =
                new ShapeHandlerPortObject(modelSpec, shapeHandler.toString()
                        + " based on column \"" + m_column + "\"");
        return new PortObject[]{changedSpecTable, viewPort};
    }

    /**
     * Appends the given <code>ShapeHandler</code> to the given
     * <code>DataTableSpec</code> for the given column. If the spec already
     * contains a ShapeHandler, it will be removed and replaced by the new one.
     * 
     * @param spec to which the ShapeHandler is appended
     * @param column for this column
     * @param shapeHandler ShapeHandler
     * @return a new spec with ShapeHandler
     */
    static final DataTableSpec appendShapeHandler(final DataTableSpec spec,
            final String column, final ShapeHandler shapeHandler) {
        DataColumnSpec[] cspecs = new DataColumnSpec[spec.getNumColumns()];
        for (int i = 0; i < cspecs.length; i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            DataColumnSpecCreator cr = new DataColumnSpecCreator(cspec);
            if (cspec.getName().equals(column)) {
                cr.setShapeHandler(shapeHandler);
            } else {
                // delete other ShapeHandler
                cr.setShapeHandler(null);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inPorts)
            throws InvalidSettingsException {
        // check null column
        if (m_column == null) {
            throw new InvalidSettingsException("No column selected.");
        }
        // check column in spec
        DataTableSpec inSpec = (DataTableSpec)inPorts[INPORT];
        if (!inSpec.containsName(m_column)) {
            throw new InvalidSettingsException("Column " + m_column
                    + " not found.");
        }
        if (m_map.isEmpty()) {
            throw new InvalidSettingsException("No shapes defined to apply.");
        }
        ShapeHandler shapeHandler =
                new ShapeHandler(new ShapeModelNominal(m_map));
        DataTableSpec outSpec =
                appendShapeHandler(inSpec, m_column, shapeHandler);
        DataTableSpec modelSpec =
                new DataTableSpec(outSpec.getColumnSpec(m_column));
        return new DataTableSpec[]{outSpec, modelSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // remove all mappings
        m_map.clear();
        // read settings and write into the map
        m_column = settings.getString(SELECTED_COLUMN);
        if (m_column != null) {
            DataCell[] values = settings.getDataCellArray(VALUES);
            for (DataCell val : values) {
                String shape = settings.getString(val.toString());
                m_map.put(val, ShapeFactory.getShape(shape));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_column != null) {
            settings.addString(SELECTED_COLUMN, m_column);
            DataCell[] vals = new DataCell[m_map.size()];
            int idx = 0;
            for (DataCell value : m_map.keySet()) {
                vals[idx] = value;
                String domValue = vals[idx].toString();
                settings.addString(domValue, m_map.get(vals[idx]).toString());
                idx++;
            }
            settings.addDataCellArray(ShapeManagerNodeModel.VALUES, vals);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String column = settings.getString(SELECTED_COLUMN);
        if (column != null) {
            DataCell[] values = settings.getDataCellArray(VALUES);
            for (DataCell val : values) {
                if (val == null) {
                    throw new InvalidSettingsException("Domain value"
                            + " must not be null.");
                } else {
                    settings.getString(val.toString());
                }
            }
        }
    }

}
