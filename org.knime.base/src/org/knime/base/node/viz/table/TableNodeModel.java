/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
import org.knime.core.node.port.PortType;
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
    implements BufferedDataTableHolder {

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
    
    /** Subclass constructor which overrides the port types. Subclasses must 
     * also override the execute and configure methods. 
     *
     * @param inPortTypes the input port types
     */
    protected TableNodeModel(final PortType[] inPortTypes) {
    	super(inPortTypes, new PortType[]{});
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
