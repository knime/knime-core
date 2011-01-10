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
