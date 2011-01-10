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
 * -------------------------------------------------------------------
 *
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.pie.node.interactive;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;

import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieDataModel;
import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieVizModel;
import org.knime.base.node.viz.pie.node.PieNodeModel;
import org.knime.base.node.viz.pie.util.TooManySectionsException;

import java.io.File;
import java.io.IOException;




/**
 * The interactive implementation of the {@link PieNodeModel} class.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieNodeModel
extends PieNodeModel<InteractivePieVizModel> {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(InteractivePieNodeModel.class);

    private InteractivePieDataModel m_model;

    /**
     * The constructor.
     */
    protected InteractivePieNodeModel() {
        super(); // one input, no outputs
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InteractivePieVizModel getVizModelInternal()
    throws TooManySectionsException  {
        if (m_model == null) {
            return null;
        }
        final InteractivePieVizModel vizModel =
            new InteractivePieVizModel(m_model, getPieColumnName(),
                    getAggregationColumnName());
        return vizModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
    throws InvalidSettingsException {
        try {
            return super.configure(inSpecs);
        } catch (final InvalidSettingsException e) {
            //try to set some default values
            final DataTableSpec spec = (DataTableSpec)inSpecs[0];
            if (spec == null) {
                throw new IllegalArgumentException(
                "No table specification found");
            }
            final int numColumns = spec.getNumColumns();
            if (numColumns < 1) {
                throw new InvalidSettingsException(
                "Input table should have at least 1 column.");
            }
            boolean pieFound = false;
            boolean aggrFound = false;
            for (int i = 0; i < numColumns; i++) {
                final DataColumnSpec columnSpec = spec.getColumnSpec(i);
                if (!pieFound
                        && PieNodeModel.PIE_COLUMN_FILTER.includeColumn(
                                columnSpec)) {
                    setPieColumnName(columnSpec.getName());
                    pieFound = true;
                } else if (!aggrFound
                        && PieNodeModel.AGGREGATION_COLUMN_FILTER.includeColumn(
                                columnSpec)) {
                    setAggregationColumnName(columnSpec.getName());
                    aggrFound = true;
                }
                if (pieFound && aggrFound) {
                    break;
                }
            }
            if (!pieFound) {
                throw new InvalidSettingsException(
                "No column compatible with this node.");
            }
        }
        return new DataTableSpec[0];
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
            final ExecutionMonitor exec) throws CanceledExecutionException {
        try {
            m_model = InteractivePieDataModel.loadFromFile(dataDir, exec);
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
    protected void createModel(final ExecutionContext exec,
            final DataColumnSpec pieColSpec, final DataColumnSpec aggrColSpec,
            final BufferedDataTable dataTable, final int noOfRows,
            final boolean containsColorHandler)
    throws CanceledExecutionException {
        m_model = new InteractivePieDataModel(exec, dataTable, noOfRows,
                containsColorHandler);
    }
}
