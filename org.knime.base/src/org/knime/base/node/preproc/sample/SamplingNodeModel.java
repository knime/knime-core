/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.base.node.preproc.sample;

import org.knime.base.node.preproc.filter.row.RowFilterIterator;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/**
 * NodeModel implementation to sample rows from an input table, thus, this node
 * has one in- and one outport.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SamplingNodeModel extends AbstractSamplingNodeModel {
    /**
     * Empty constructor, sets port count in super.
     */
    public SamplingNodeModel() {
        super(1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable in = inData[0];
        // he following line does not need the exec monitor. It's
        // only used when the table is traversed in order to count the rows.
        // This is done only if "in" does not support getRowCount().
        // But the argument in the execute method surely does!
        RowFilter filter = getSamplingRowFilter(in, exec);
        BufferedDataContainer container = exec.createDataContainer(in
                .getDataTableSpec());
        try {
            int count = 0;
            RowFilterIterator it = new RowFilterIterator(in, filter, exec);
            while (it.hasNext()) {
                DataRow row = it.next();
                exec.setMessage("Adding row " + count + " (\"" + row.getKey()
                        + "\")");
                count++;
                container.addRowToTable(row);
            }
        } catch (RowFilterIterator.RuntimeCanceledExecutionException rce) {
            throw rce.getCause();
        } finally {
            container.close();
        }
        BufferedDataTable out = container.getTable();
        if (filter instanceof StratifiedSamplingRowFilter) {
            int classCount =
                    ((StratifiedSamplingRowFilter)filter).getClassCount();
            if (classCount > out.getRowCount()) {
                setWarningMessage("Class column contains more classes ("
                        + classCount + ") than sampled rows ("
                        + out.getRowCount() + ")");
            }
        }

        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkSettings(inSpecs[0]);
        return inSpecs;
    }
}
