/* 
 * -------------------------------------------------------------------
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
 *   01.08.2005 (cebron): created
 */
package org.knime.base.node.preproc.shuffle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import org.knime.base.data.append.row.AppendedRowsTable;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Implementation of the Fisher Yates shuffle, that guarantees that all n!
 * possible outcomes are possible and equally likely. The shuffling procedure
 * requires only linear runtime. For further details see "Fisher-Yates shuffle",
 * from Dictionary of Algorithms and Data Structures, Paul E. Black, ed., NIST.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class ShuffleNodeModel extends NodeModel {
    
    /** Config identifier for seed field. */
    static final String CFG_SEED = "random_seed";
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
    
    /**
     * The seed to use or null to use always a different one.
     */
    private Long m_seed;

    /*
     * Logger of this NodeModel
     */
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ShuffleNodeModel.class);

    /**
     * 
     */
    public ShuffleNodeModel() {
        super(1, 1);
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
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
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        if (inData[0].getRowCount() == 0) {
            // empty table? do nothing.
            return inData;
        }
        Random random;
        if (m_seed != null) {
            random = new Random(m_seed.longValue());
        } else {
            random = new Random();
        }
        int nrRows = 0;
        
        ExecutionMonitor subExec1 = exec.createSubProgress(0.7);
        ExecutionMonitor subExec2 = exec.createSubProgress(0.3);
        Vector<RowKey> tmpKeys = new Vector<RowKey>();
        for (RowIterator r = inData[0].iterator(); r.hasNext(); nrRows++) {
            /*
             * We generate an array of rowkeys from the input table. This array
             * is shuffled.
             */
            tmpKeys.add(r.next().getKey());
            subExec1.checkCanceled();
        }
        
        m_shuffleArr = tmpKeys.toArray(new RowKey[]{});

        if (nrRows < containersize) {
            containersize = nrRows;
        }

        for (int i = 0; i < m_shuffleArr.length; i++) {
            int r = random.nextInt(i + 1);
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
            subExec1.setProgress(progress, 
                    "Shuffling..." + (int)(progress * 100) + "%");
            subExec1.checkCanceled();
            DataRow[] rows;
            if (i != nrContainer - 1) {
                rows = new DataRow[containersize];
            } else {
                rows = new DataRow[nrRows - ((nrContainer - 1) 
                        * containersize)];
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
        BufferedDataContainer out = 
            exec.createDataContainer(mergeTable.getDataTableSpec(), true);
        int count = 0;
        int totalCount = inData[0].getRowCount();
        for (DataRow row : mergeTable) {
            out.addRowToTable(row);
            subExec2.setProgress(
                    count / (double)totalCount, "Caching output, row " + count 
                    + "/" + totalCount + " (\"" + row.getKey() + "\")");
            count++;
            subExec2.checkCanceled();
        }
        out.close();
        return new BufferedDataTable[]{out.getTable()};
    }

    /**
     * @see NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * @see NodeModel#saveInternals(File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        String seedText = m_seed != null ? Long.toString(m_seed) : null;
        settings.addString(CFG_SEED, seedText);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        // seed was not available in knime 1.1.2, backward compatibility
        String seedText = settings.getString(CFG_SEED, null);
        if (seedText != null) {
            try {
                Long.parseLong(seedText);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Unable to parse seed \""
                        + seedText + "\" as number.");
            }
        }
    }
    
    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // seed was not available in knime 1.1.2, backward compatibility
        String seedText = settings.getString(CFG_SEED, null);
        if (seedText != null) {
            try {
                m_seed = Long.parseLong(seedText);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Unable to parse seed \""
                        + seedText + "\" as number.");
            }
        } else {
            m_seed = null;
        }
    }
}
