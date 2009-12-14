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
