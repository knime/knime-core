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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.parcoord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * @author Simona Pintilie, University of Konstanz
 */
final class ParallelCoordinatesNodeModel extends NodeModel {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ParallelCoordinatesNodeModel.class);

    /** The underlying view's content model. */
    private ParallelCoordinatesViewContent m_content;

    /** NodeSettings Name for list of hidden columns. */
    public static final String HIDDENCOLUMNS = "HiddenColumns";

    // list of columns not to be shown in ParCor View
    private String[] m_hiddenColumns = new String[0];

    /** NodeSettings Name for list of hidden columns. */
    public static final String MAXNUMROWS = "MaxNumRows";

    // maximum number of rows displayed
    private int m_maxNumRows = 1000;

    /**
     * the constructor.
     */
    ParallelCoordinatesNodeModel() {
        super(1, 0);
    }

    /**
     * @return The view's content model.
     */
    ParallelCoordinatesViewContent getViewContent() {
        return m_content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // create the view's content
        m_content = new ParallelCoordinatesViewContent(inData[0],
                m_hiddenColumns, m_maxNumRows, exec);
        // set warning if not all rows were stored
        if (!m_content.storesAllRows()) {
            this.setWarningMessage("View will not display all Rows. Showing"
                    + " only first " + m_maxNumRows + " rows.");
        }
        // no data on the output
        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_content = null;
        // this.notifyViews(new String(""));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert (inSpecs.length == 1);
        return new DataTableSpec[0];
    }

    /**
     * Get list of hidden columns from settings.
     * 
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_maxNumRows = settings.getInt(MAXNUMROWS);
        try {
            m_hiddenColumns = settings.getStringArray(HIDDENCOLUMNS);
        } catch (InvalidSettingsException ise) {
            m_hiddenColumns = new String[0];
        }
    }

    /**
     * Write list of hidden columns to settings object.
     * 
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(MAXNUMROWS, m_maxNumRows);
        settings.addStringArray(HIDDENCOLUMNS, m_hiddenColumns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do here
    }

    private static final String INTERNALS_FILE_NAME = "ParCorContent.bin";

    /**
     * Load internals.
     * 
     * @param nodeInternDir The intern node directory to load tree from.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(nodeInternDir, INTERNALS_FILE_NAME);
        if (!f.exists()) {
            m_content = null;
        }
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
        try {
            m_content = (ParallelCoordinatesViewContent)in.readObject();
        } catch (ClassNotFoundException e) {
            LOGGER.error(e);
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        } finally {
            in.close();
        }
    }

    /**
     * Save internals.
     * 
     * @param nodeInternDir The intern node directory to save table to.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(nodeInternDir, INTERNALS_FILE_NAME);
        ObjectOutputStream out = 
            new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(m_content);
        out.close();
    }
}
