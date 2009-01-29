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
 * History
 *   01.08.2005 (cebron): created
 */
package org.knime.base.node.preproc.shuffle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
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
        
    /**
     * The seed to use or null to use always a different one.
     */
    private Long m_seed;

    /**
     * 
     */
    public ShuffleNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return inSpecs;
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        RandomNumberAppendFactory randomnumfac =
                RandomNumberAppendFactory.create(m_seed, inData[0]);
        ColumnRearranger colre =
                new ColumnRearranger(inData[0].getDataTableSpec());
        colre.append(randomnumfac);
        BufferedDataTable intermediate =
                exec.createColumnRearrangeTable(inData[0], colre, exec
                        .createSubProgress(.2));
        List<String> include = new ArrayList<String>();
        String randomcol = randomnumfac.getColumnSpecs()[0].getName();
        include.add(randomcol);
        SortedTable sort =
                new SortedTable(intermediate, include, new boolean[]{true},
                        exec.createSubExecutionContext(.75));
        BufferedDataTable sorted = sort.getBufferedDataTable();
        colre = new ColumnRearranger(sorted.getDataTableSpec());
        colre.remove(randomcol);
        BufferedDataTable result =
                exec.createColumnRearrangeTable(sorted, colre, exec
                        .createSubProgress(.05));
        return new BufferedDataTable[]{result};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        String seedText = m_seed != null ? Long.toString(m_seed) : null;
        settings.addString(CFG_SEED, seedText);
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
    
    /*
     * The CellFactory adds a shuffled number to each input DataRow.
     */
    private static final class RandomNumberAppendFactory 
        extends SingleCellFactory {

        /** Shuffled row number array. */
        private int[] m_shuffle;
        
        /** Position in array. */
        private int m_pos = 0;
        
        /** Constructor. */
        private RandomNumberAppendFactory(final Long seed, 
                final int rowCount, final DataColumnSpec appendSpec) {
            super(appendSpec);
            Random random;
            if (seed != null) {
                random = new Random(seed.longValue());
            } else {
                random = new Random();
            }
            int nrRows = rowCount;
            
            // initialize
            m_shuffle = new int[nrRows];
            for (int i = 0; i < nrRows; i++) {
                m_shuffle[i] = i;
            }
            
            // let's shuffle
            for (int i = 0; i < m_shuffle.length; i++) {
                int r = random.nextInt(i + 1);
                int swap = m_shuffle[r];
                m_shuffle[r] = m_shuffle[i];
                m_shuffle[i] = swap;
            }
       }
        
        /** {@inheritDoc} */
        @Override
        public DataCell getCell(final DataRow row) {
           assert (m_pos <= m_shuffle.length);
           DataCell nextRandomNumberCell = new IntCell(m_shuffle[m_pos]);
           m_pos++;
           return nextRandomNumberCell;
        }
        
        /** Factory method to create a new random number append factory. */
        private static RandomNumberAppendFactory create(final Long seed, 
                final BufferedDataTable inData) {
            final DataTableSpec spec = inData.getDataTableSpec();
            final int rowCount = inData.getRowCount();
            String appendName = "random_row_number";
            int uniquifier = 1;
            while (spec.containsName(appendName)) {
                appendName = "random_row_number_#" + uniquifier++;
            }
            DataColumnSpec s = new DataColumnSpecCreator(
                    appendName, IntCell.TYPE).createSpec();
            return new RandomNumberAppendFactory(seed, rowCount, s);
        }

    }
}
