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
package org.knime.base.node.mine.cluster.hierarchical.distfunctions;


import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

/**
 * Calculates the distance for two data rows based on the Minkowski distance.
 * 
 * @author som-team, University of Konstanz
 */
public class MinkowskiDist implements DistanceFunction {
    
    /**
     * The power of this type of minkowski distance.
     */
    private final int m_p;

    /**
     * Creates a Minkowski distance object from an Minkowski distance.
     * 
     * @param p the power for this Minkowski distance
     */
    public MinkowskiDist(final int p) {
        m_p = p;
    }
    
    /**
     * Calculates the distance between two data rows based on the 
     * Minkowski distance.
     * 
     * @param firstDataRow the first data row used to calculate the distance
     * @param secondDataRow the second data row used to calculate the distance
     * @param includedCols the columns to include into the calculation
     * 
     * @return the distance of the two rows
     */
    public double calcDistance(final DataRow firstDataRow,
            final DataRow secondDataRow, final int[] includedCols) {
        
        double sumPowDist = 0;

        for (int i : includedCols) {
            // skip not included cells
            DataCell x = firstDataRow.getCell(i);
            DataCell y = secondDataRow.getCell(i);
            if (!x.isMissing() && x instanceof DoubleValue
            &&  !y.isMissing() && y instanceof DoubleValue) {
                DoubleValue clusterValue = (DoubleValue)firstDataRow.getCell(i);
                DoubleValue rowValue = (DoubleValue)secondDataRow.getCell(i);
                double dist = Math.abs(clusterValue.getDoubleValue() 
                        - rowValue.getDoubleValue());
                sumPowDist += Math.pow(dist, m_p);
            }
        }
        
        
        return Math.pow(sumPowDist, (double)1 / (double)m_p);
    }
    
    /**
     * @param o The object to compare with.
     * @return true if both instances (classes) are the same. 
     */
    @Override
    public boolean equals(final Object o) {
        if (o != null) {
            return this.getClass() == o.getClass();
        } else {
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
    /**
     * Returns the String representation of this distance function.
     * 
     * @return the String representation
     */
    @Override
    public String toString() {
        
        return "Minkowski Distance with p=" + m_p;
    }
}
