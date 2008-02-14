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
package org.knime.base.node.viz.pie.node.interactive;

import java.awt.Color;
import java.io.File;

import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieDataModel;
import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieVizModel;
import org.knime.base.node.viz.pie.node.PieNodeModel;
import org.knime.base.node.viz.pie.util.TooManySectionsException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;




/**
 * The interactive implementation of the {@link PieNodeModel} class.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieNodeModel
extends PieNodeModel<InteractivePieVizModel> {

    private InteractivePieDataModel m_model;

    /**
     * The constructor.
     */
    protected InteractivePieNodeModel() {
        super(); // one input, no outputs
        //if we set the node to autoExecutable = true the execute method
        //gets also called when the workspace is reloaded from file
        setAutoExecutable(true);
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        try {
            return super.configure(inSpecs);
        } catch (final InvalidSettingsException e) {
            //try to set some default values
            final DataTableSpec spec = inSpecs[0];
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
            final ExecutionMonitor exec) {
       //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void savePieInternals(final File dataDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createModel(final DataColumnSpec pieColSpec,
            final DataColumnSpec aggColSpec, final DataTableSpec spec,
            final int noOfRows, final boolean containsColorHandler) {
        m_model = new InteractivePieDataModel(spec, noOfRows,
                containsColorHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addDataRow(final DataRow row, final Color rowColor,
            final DataCell pieCell, final DataCell aggrCell) {
        m_model.addDataRow(row, rowColor, pieCell, aggrCell);
    }
}
