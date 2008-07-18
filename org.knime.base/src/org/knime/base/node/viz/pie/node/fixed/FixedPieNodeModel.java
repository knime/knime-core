/*
 * ------------------------------------------------------------------
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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.pie.node.fixed;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

import org.knime.base.node.viz.pie.datamodel.fixed.FixedPieDataModel;
import org.knime.base.node.viz.pie.datamodel.fixed.FixedPieVizModel;
import org.knime.base.node.viz.pie.node.PieNodeModel;
import org.knime.base.node.viz.pie.util.TooManySectionsException;

import java.awt.Color;
import java.io.File;
import java.io.IOException;




/**
 * The NodeModel class of the fixed pie chart implementation.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPieNodeModel
extends PieNodeModel<FixedPieVizModel> {
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
    protected FixedPieVizModel getVizModelInternal()  {
        if (m_model == null) {
            return null;
        }
        final FixedPieVizModel vizModel = new FixedPieVizModel(
                m_model.getPieColName(), m_model.getAggrColName(),
                m_model.getClonedSections(), m_model.getClonedMissingSection(),
                m_model.supportsHiliting(), m_model.detailsAvailable());
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
     * @throws CanceledExecutionException
     * @throws TooManySectionsException
     */
    @Override
    protected void createModel(final ExecutionContext exec,
            final DataColumnSpec pieColSpec,
            final DataColumnSpec aggrColSpec,
            final BufferedDataTable dataTable, final int noOfRows,
            final boolean containsColorHandler)
    throws CanceledExecutionException, TooManySectionsException {
        m_model = new FixedPieDataModel(pieColSpec, aggrColSpec,
                containsColorHandler);
        final DataTableSpec spec = dataTable.getSpec();
        final int pieColIdx = spec.findColumnIndex(pieColSpec.getName());
        final int aggrColIdx;
        if (aggrColSpec == null) {
            aggrColIdx = -1;
        } else {
            aggrColIdx = spec.findColumnIndex(aggrColSpec.getName());
        }
        final double progressPerRow = 1.0 / noOfRows;
        double progress = 0.0;
        final CloseableRowIterator rowIterator = dataTable.iterator();
        try {
            for (int rowCounter = 0; rowCounter < noOfRows
                    && rowIterator.hasNext(); rowCounter++) {
                final DataRow row = rowIterator.next();
                final Color rowColor =
                    spec.getRowColor(row).getColor(false, false);
                final DataCell pieCell = row.getCell(pieColIdx);
                final DataCell aggrCell;
                if (aggrColIdx >= 0) {
                    aggrCell = row.getCell(aggrColIdx);
                } else {
                    aggrCell = null;
                }
                m_model.addDataRow(row, rowColor, pieCell, aggrCell);
                progress += progressPerRow;
                exec.setProgress(progress, "Adding data rows to pie chart...");
                exec.checkCanceled();
            }
        } finally {
            if (rowIterator != null) {
                rowIterator.close();
            }
        }
    }

}
