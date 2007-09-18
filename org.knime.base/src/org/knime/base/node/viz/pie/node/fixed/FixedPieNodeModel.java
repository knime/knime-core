/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.pie.node.fixed;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.datamodel.fixed.FixedPieDataModel;
import org.knime.base.node.viz.pie.datamodel.fixed.FixedPieVizModel;
import org.knime.base.node.viz.pie.node.PieNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;




/**
 * The NodeModel class of the fixed pie chart implementation.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPieNodeModel extends PieNodeModel {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(FixedPieNodeModel.class);

    private FixedPieDataModel m_model;

    /**
     * The constructor.
     */
    protected FixedPieNodeModel() {
        super(); // one input, no outputs
        //if we set the node to autoExecutable = true the execute method
        //gets also called when the workspace is reloaded from file
//        setAutoExecutable(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PieVizModel getVizModelInternal()  {
        if (m_model == null) {
            return null;
        }
        final PieVizModel vizModel = new FixedPieVizModel(m_model);
        return vizModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetPieData() {
        m_model = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadPieInternals(final File dataDir,
            final ExecutionMonitor exec)
            throws CanceledExecutionException {
        try {
            m_model = FixedPieDataModel.loadFromFile(dataDir, exec);
        } catch (final CanceledExecutionException e) {
            throw e;
        } catch (final Exception e) {
            LOGGER.debug("Error while loading internals: "
                    + e.getMessage());
            m_model = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void savePieInternals(final File dataDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        try {
            if (m_model == null) {
                return;
            }
            m_model.save2File(dataDir, exec);
        } catch (final CanceledExecutionException e) {
            throw e;
        } catch (final Exception e) {
            LOGGER.warn("Error while saving saving internals: "
                    + e.getMessage());
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createModel(final DataColumnSpec pieColSpec) {
        m_model = new FixedPieDataModel(pieColSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addDataRow(final DataCell id, final Color rowColor,
            final DataCell pieCell,
            final DataCell aggrCell) {
        m_model.addDataRow(id, rowColor, pieCell, aggrCell);
    }
}
