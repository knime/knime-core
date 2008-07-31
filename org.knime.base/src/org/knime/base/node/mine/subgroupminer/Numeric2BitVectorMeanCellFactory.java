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
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.01.2007 (dill): created
 */
package org.knime.base.node.mine.subgroupminer;

import java.util.BitSet;

import org.knime.base.data.bitvector.BitVectorCell;
import org.knime.base.data.bitvector.BitVectorCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class Numeric2BitVectorMeanCellFactory extends BitVectorCellFactory {

    private double[] m_meanValues;

    private double m_meanFactor;

    private int m_totalNrOf0s;

    private int m_totalNrOf1s;

    /**
     * 
     * @param bitColSpec the column spec of the column containing the bitvectors
     * @param meanValues the mean values of the numeric columns
     * @param meanThreshold threshold above which the bits should be set
     *            (percentage of the mean)
     */
    public Numeric2BitVectorMeanCellFactory(final DataColumnSpec bitColSpec,
            final double[] meanValues, final double meanThreshold) {
        super(bitColSpec);
        m_meanValues = meanValues;
        m_meanFactor = meanThreshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfNotSetBits() {
        return m_totalNrOf0s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSetBits() {
        return m_totalNrOf1s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasSuccessful() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        incrementNrOfRows();
        BitSet currBitSet = new BitSet(row.getNumCells());
        for (int i = 0; i < row.getNumCells(); i++) {
            if (!row.getCell(i).getType().isCompatible(DoubleValue.class)) {
                continue;
            }
            if (row.getCell(i).isMissing()) {
                m_totalNrOf0s++;
                continue;
            }
            double currValue = ((DoubleValue)row.getCell(i)).getDoubleValue();
            if (currValue >= (m_meanFactor * m_meanValues[i])) {
                currBitSet.set(i);
                m_totalNrOf1s++;
            } else {
                m_totalNrOf0s++;
            }
        }     
        return new BitVectorCell(currBitSet, row.getNumCells());
    }

}
