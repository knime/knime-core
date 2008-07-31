/*
 * -------------------------------------------------------------------
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
 */
package org.knime.base.node.preproc.partition;

import org.knime.base.node.preproc.filter.row.RowFilterIterator;
import org.knime.base.node.preproc.filter.row.rowfilter.NegRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeModel;
import org.knime.base.node.preproc.sample.StratifiedSamplingRowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PartitionNodeModel extends AbstractSamplingNodeModel {
    /** Outport for training data: 0. */
    static final int OUTPORT_A = 0;

    /** Outport for test data: 1. */
    static final int OUTPORT_B = 1;

    /**
     * Creates node model, sets outport count to 2.
     */
    public PartitionNodeModel() {
        super(2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        BufferedDataTable in = inData[0];
        BufferedDataTable[] outs = new BufferedDataTable[2];
        RowFilter filterTrain = getSamplingRowFilter(in, exec);
        RowFilter filterTest = new NegRowFilter((RowFilter)filterTrain.clone());
        RowFilter[] filters = new RowFilter[]{filterTrain, filterTest};
        String[] partitionNames = {"first", "second"};
        for (int i = 0; i < outs.length; i++) {
            ExecutionMonitor subExec = exec.createSubProgress(0.5);
            BufferedDataContainer container = exec.createDataContainer(in
                    .getDataTableSpec());
            try {
                int count = 0;
                RowFilterIterator it = new RowFilterIterator(in, filters[i],
                        subExec);
                while (it.hasNext()) {
                    DataRow row = it.next();
                    StringBuilder b = new StringBuilder("Creating ");
                    b.append(partitionNames[i]);
                    b.append(" partition: ");
                    b.append("Adding row ");
                    b.append(count);
                    b.append(" (\"");
                    b.append(row.getKey().toString());
                    b.append("\")");
                    subExec.setMessage(b.toString());
                    count++;
                    container.addRowToTable(row);
                }
            } catch (RowFilterIterator.RuntimeCanceledExecutionException rce) {
                throw rce.getCause();
            } finally {
                container.close();
            }
            subExec.setProgress(1.0);
            outs[i] = container.getTable();
            if (filters[i] instanceof StratifiedSamplingRowFilter) {
                int classCount =
                        ((StratifiedSamplingRowFilter)filters[i])
                                .getClassCount();
                if (classCount > outs[i].getRowCount()) {
                    setWarningMessage("Class column contains more classes ("
                            + classCount + ") than sampled rows ("
                            + outs[i].getRowCount() + ")");
                }
            }
        }
        return outs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkSettings(inSpecs[0]);
        DataTableSpec[] outs = new DataTableSpec[2];
        outs[OUTPORT_A] = inSpecs[0];
        outs[OUTPORT_B] = inSpecs[0];
        return outs;
    }
}
