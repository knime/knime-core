/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   01.08.2005 (cebron): created
 */
package org.knime.base.node.preproc.shuffle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NoSettingsNodeModel;
import org.knime.core.node.NodeLogger;

import org.knime.base.data.append.row.AppendedRowsTable;

/**
 * Implementation of the Fisher Yates shuffle, that guarantees that all n!
 * possible outcomes are possible and equally likely. The shuffling procedure
 * requires only linear runtime. For further details see "Fisher-Yates shuffle",
 * from Dictionary of Algorithms and Data Structures, Paul E. Black, ed., NIST.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class ShuffleNodeModel extends NoSettingsNodeModel {
    /*
     * Default constant number of rows for each container
     */
    private static final int DEFAULT_CONTAINERSIZE = 1000;

    /*
     * Number of rows for each container
     */
    private static int containersize = DEFAULT_CONTAINERSIZE;

    /*
     * Array of RowKeys to be shuffled.
     */
    private RowKey[] m_shuffleArr;

    /*
     * Logger of this NodeModel
     */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ShuffleNodeModel.class);

    /**
     * 
     */
    public ShuffleNodeModel() {
        super(1, 1);
    }

    /**
     * @see org.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return inSpecs;
    }

    /**
     * An array of row keys of the input table is shuffled. Afterwards, chunks
     * are formed and each one is filled with the according rows of the input
     * datatable. All chunks are merged to form the output datatable.
     * 
     * @see org.knime.core.node.NodeModel#execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        int nrRows = 0;

        Vector<RowKey> tmpKeys = new Vector<RowKey>();
        for (RowIterator r = inData[0].iterator(); r.hasNext(); nrRows++) {
            /*
             * We generate an array of rowkeys from the input table. This array
             * is shuffled.
             */
            tmpKeys.add(r.next().getKey());
            exec.checkCanceled();
        }

        m_shuffleArr = tmpKeys.toArray(new RowKey[]{});

        if (nrRows < containersize) {
            containersize = nrRows;
        }

        for (int i = 0; i < m_shuffleArr.length; i++) {
            int r = (int)(Math.random() * (i + 1)); // int between 0 and i
            RowKey swap = m_shuffleArr[r];
            m_shuffleArr[r] = m_shuffleArr[i];
            m_shuffleArr[i] = swap;
        }

        int nrContainer = (nrRows % containersize == 0) ? nrRows
                / containersize : nrRows / containersize + 1;
        DataContainer[] contArr = new DataContainer[nrContainer];

        // map Rowkeys to Container
        HashMap<RowKey, Integer[]> keymap = new HashMap<RowKey, Integer[]>();

        // pos von key in map
        for (int i = 0; i < m_shuffleArr.length; i++) {
            int actcontainer = i / containersize;
            keymap.put(m_shuffleArr[i], new Integer[]{actcontainer,
                    i - (actcontainer * containersize)});
        }

        for (int i = 0; i < contArr.length; i++) {
            double progress = (double)i / (double)contArr.length;
            exec.setProgress(progress, "Shuffling..." + (int)(progress * 100)
                    + "%");
            exec.checkCanceled();
            DataRow[] rows;
            if (i != nrContainer - 1) {
                rows = new DataRow[containersize];
            } else {
                rows = new DataRow[nrRows - ((nrContainer - 1) * containersize)];
            }
            RowIterator rowIt = inData[0].iterator();
            int nrfound = 0;
            while (rowIt.hasNext() && nrfound < rows.length) {
                DataRow row = rowIt.next();
                RowKey key = row.getKey();
                if (keymap.get(key)[0].intValue() == i) {
                    int pos = keymap.get(key)[1].intValue();
                    rows[pos] = row;
                    nrfound++;
                }
            }
            DataContainer tempCont = new DataContainer(inData[0]
                    .getDataTableSpec());
            for (int j = 0; j < rows.length; j++) {
                tempCont.addRowToTable(rows[j]);
            }
            tempCont.close();
            LOGGER.debug("Container " + i + " of " + contArr.length
                    + " filled.");
            contArr[i] = tempCont;
        }

        // now merge all containers together
        // AppendedRowsTable
        DataTable[] tables = new DataTable[contArr.length];
        int i = 0;
        for (DataContainer con : contArr) {
            tables[i] = con.getTable();
            i++;
        }
        AppendedRowsTable mergeTable = new AppendedRowsTable(tables);
        return new BufferedDataTable[]{exec.createBufferedDataTable(mergeTable,
                exec)};
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals( java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals( java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
}
