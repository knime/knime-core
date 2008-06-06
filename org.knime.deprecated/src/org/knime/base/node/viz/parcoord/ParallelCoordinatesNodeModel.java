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
