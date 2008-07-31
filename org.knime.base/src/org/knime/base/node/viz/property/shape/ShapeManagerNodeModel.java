/*
 * --------------------------------------------------------------------- *
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
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Model used to set shapes by nominal values retrieved from the 
 * {@link org.knime.core.data.DataColumnSpec} domain.
 * The created {@link org.knime.core.data.property.ShapeHandler} is then
 * set in the column spec.
 * 
 * @see ShapeManagerNodeDialogPane
 * @see ShapeHandler
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class ShapeManagerNodeModel extends NodeModel {
    
    /** ShapeHandler generated during executed and save into the model port. */
    private ShapeHandler m_shapeHandler;
    
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
     * 
     * @param dataIns number of data ins
     * @param dataOuts number of data outs
     * @param modelIns number of model ins
     * @param modelOuts number of model outs
     */
    ShapeManagerNodeModel(final int dataIns, final int dataOuts,
            final int modelIns, final int modelOuts) {
        super(dataIns, dataOuts, modelIns, modelOuts);
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
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        final DataTableSpec inSpec = data[INPORT].getDataTableSpec();
        m_shapeHandler = new ShapeHandler(new ShapeModelNominal(m_map));
        final DataTableSpec newSpec = appendShapeHandler(inSpec, m_column, 
                m_shapeHandler);
        BufferedDataTable changedSpecTable = exec.createSpecReplacerTable(
                data[INPORT], newSpec);
        // return original table with ShapeHandler
        return new BufferedDataTable[]{changedSpecTable};
    }
    
    /**
     * Appends the given <code>ShapeHandler</code> to the given 
     * <code>DataTableSpec</code> for the given column. If the spec
     * already contains a ShapeHandler, it will be removed and replaced by
     * the new one.
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
     * Saves the shape settings to <code>ModelContent</code> object.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        m_shapeHandler.save(predParams);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_shapeHandler = null;
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
     * @param inSpecs the input specs passed to the output port
     * @return the same as the input spec
     * 
     * @throws InvalidSettingsException if a column is not available
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert (inSpecs.length == 1);
        // check null column
        if (m_column == null) {
            throw new InvalidSettingsException("No column selected.");
        }
        // check column in spec
        if (!inSpecs[INPORT].containsName(m_column)) {
            throw new InvalidSettingsException("Column " + m_column
                    + " not found.");
        }
        if (m_map.isEmpty()) {
            throw new InvalidSettingsException("No shapes defined to apply.");
        }
        ShapeHandler shapeHandler = 
            new ShapeHandler(new ShapeModelNominal(m_map));
        DataTableSpec outSpec = appendShapeHandler(inSpecs[INPORT], m_column, 
                shapeHandler);
        return new DataTableSpec[]{outSpec};
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
        m_column = settings.getString(SELECTED_COLUMN, null);
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
        String column = settings.getString(SELECTED_COLUMN, null);
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
