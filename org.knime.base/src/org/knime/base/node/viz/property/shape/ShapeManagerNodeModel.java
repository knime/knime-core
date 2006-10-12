/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.property.shape;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

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
import org.knime.core.node.NodeSettings;
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
    
    /** Logger for this package. */
    static final NodeLogger LOGGER = NodeLogger.getLogger("Shape Manager");

    /** Stores the mapping from column value to shape. */
    private final LinkedHashMap<DataCell, Shape> m_map;

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
        assert (data != null && data.length == 1 && data[INPORT] != null);
        DataTableSpec inSpec = data[INPORT].getDataTableSpec();
        DataColumnSpec[] newColSpecs = new DataColumnSpec[inSpec
                .getNumColumns()];
        for (int i = 0; i < newColSpecs.length; i++) {
            DataColumnSpec cspec = inSpec.getColumnSpec(i);
            DataColumnSpecCreator dtsCont = new DataColumnSpecCreator(cspec);
            if (cspec.getName().equals(m_column)) {
                ShapeHandler shapeHdl = new ShapeHandler(
                        new ShapeModelNominal(m_map));
                dtsCont.setShapeHandler(shapeHdl);
            }
            newColSpecs[i] = dtsCont.createSpec();
        }
        final DataTableSpec newSpec = new DataTableSpec(newColSpecs);
        BufferedDataTable changedSpecTable = exec.createSpecReplacerTable(
                data[INPORT], newSpec);
        // return original table with ShapeHandler
        return new BufferedDataTable[]{changedSpecTable};
    }

    /**
     * Saves the shape settings to <code>ModelContent</code> object.
     * 
     * @see NodeModel#saveModelContent(int, ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        assert index == 0;
        if (predParams != null) {
            NodeSettings settings = new NodeSettings(predParams.getKey());
            saveSettingsTo(settings);
            settings.copyTo(predParams);
        }
    }

    /**
     * @return the selected column or <code>null</code> if none
     */
    protected String getSelectedColumn() {
        return m_column;
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {

    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      ExecutionMonitor)
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
        return new DataTableSpec[]{inSpecs[INPORT]};
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
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
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
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
     * @see NodeModel#validateSettings(NodeSettingsRO)
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
