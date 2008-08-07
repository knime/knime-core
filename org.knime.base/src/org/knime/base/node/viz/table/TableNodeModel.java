/*
 * ---------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.viz.table;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableContentModel;


/**
 * Node model for a table view. This class is implemented in first place to
 * comply with the model-view-controller concept. Thus, this implementation does
 * not have any further functionality than its super class {@link NodeModel}.
 * The content itself resides in
 * {@link org.knime.core.node.tableview.TableContentModel}.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @see org.knime.core.node.tableview.TableContentModel
 */
public class TableNodeModel extends NodeModel
    implements BufferedDataTableHolder
{

    /** Index of the input port (only one anyway). */
    protected static final int INPORT = 0;

    /**
     * This model serves as wrapper for a TableContentModel, this will be the
     * "real" data structure.
     */
    private final TableContentModel m_contModel;

    /**
     * Constructs a new (but empty) model. The model has one input port and no
     * output port.
     */
    public TableNodeModel() {
        super(1, 0);
        // models have empty content
        m_contModel = new TableContentModel();
    }

    /**
     * Get reference to the underlying content model. This model will be the
     * same for successive calls and never be <code>null</code>.
     * 
     * @return reference to underlying content model
     */
    public TableContentModel getContentModel() {
        return m_contModel;
    }

    /**
     * Called when new data is available. Accesses the input data table and the
     * property handler, sets them in the {@link TableContentModel} and starts
     * event firing to inform listeners. Do not call this method separately, it
     * is invoked by this model's node.
     * 
     * @param data the data table at the (single) input port, wrapped in a
     *            one-dimensional array
     * @param exec the execution monitor
     * @return an empty table
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) {
        assert (data != null);
        assert (data.length == 1);
        final DataTable in = data[0];
        assert (in != null);
        HiLiteHandler inProp = getInHiLiteHandler(INPORT);
        m_contModel.setDataTable(in);
        m_contModel.setHiLiteHandler(inProp);
        assert (m_contModel.hasData());
        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        assert m_contModel.getDataTable() instanceof BufferedDataTable;
        return new BufferedDataTable[] {
                (BufferedDataTable)(m_contModel.getDataTable())
        };
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        if (tables.length != 1) {
            throw new IllegalArgumentException();
        }
        m_contModel.setDataTable(tables[0]);
        HiLiteHandler inProp = getInHiLiteHandler(INPORT);
        m_contModel.setHiLiteHandler(inProp);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_contModel.setDataTable(null);
        m_contModel.setHiLiteHandler(null);
        assert (!m_contModel.hasData());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
        return new DataTableSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }
}
