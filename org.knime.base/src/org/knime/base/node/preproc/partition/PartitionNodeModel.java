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

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeModel;
import org.knime.base.node.preproc.sample.StratifiedSamplingRowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
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
        RowFilter filter = getSamplingRowFilter(in, exec);
        BufferedDataContainer firstOutCont = 
            exec.createDataContainer(in.getDataTableSpec());
        BufferedDataContainer secondOutCont = 
            exec.createDataContainer(in.getDataTableSpec());
        double rowCount = in.getRowCount(); // floating point op. below
        // one of the flags will be set if one of the exceptions below
        // is thrown.
        boolean putRestInOut1 = false;
        boolean putRestInOut2 = false;
        try {
            int count = 0;
            for (DataRow row : in) {
                boolean matches = putRestInOut1;
                try {
                    // conditional check, will call "matches" only if necessary
                    matches |= (!putRestInOut2
                            && filter.matches(row, count));
                } catch (IncludeFromNowOn icf) {
                    assert !putRestInOut2;
                    putRestInOut1 = true;
                    matches = true;
                } catch (EndOfTableException ete) {
                    assert !putRestInOut1;
                    putRestInOut2 = true;
                    matches = false;
                }
                if (matches) {
                    firstOutCont.addRowToTable(row);
                } else {
                    secondOutCont.addRowToTable(row);
                }
                exec.setProgress(count / rowCount, "Processed row " + count
                        + " (\"" + row.getKey() + "\")");
                exec.checkCanceled();
                count++;
            }
        } finally {
            firstOutCont.close();
            secondOutCont.close();
        }
        outs[0] = firstOutCont.getTable();
        outs[1] = secondOutCont.getTable();
        if (filter instanceof StratifiedSamplingRowFilter) {
            int classCount = 
                ((StratifiedSamplingRowFilter)filter).getClassCount();
            if (classCount > outs[0].getRowCount()) {
                setWarningMessage("Class column contains more classes ("
                        + classCount + ") than sampled rows ("
                        + outs[0].getRowCount() + ")");
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
