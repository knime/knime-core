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

import org.knime.core.data.DataRow;

/**
 * Calculates the distance for two data rows based on the euclidean distance.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class EuclideanDist extends MinkowskiDist {
    
    /**
     * An instance of this distance function.
     */
    public static final EuclideanDist EUCLIDEAN_DISTANCE
         = new EuclideanDist();

    /**
     * Creates a Euclidean distance object from an Minkowski distance
     * which means the power is two.
     */
    protected EuclideanDist() {
        // generates minkowski with power 2
        super(2);
    }

    /**
     * Calculates the distance between two data rows based on the 
     * Euclidean distance.
     * 
     * @param firstDataRow the first data row used to calculate the distance
     * @param secondDataRow the second data row used to calculate the distance
     * @param includedCols the columns to include into the distance calculation
     * 
     * @return the distance of the two rows
     */
    @Override
    public double calcDistance(final DataRow firstDataRow,
            final DataRow secondDataRow, final int[] includedCols) {
        return super.calcDistance(firstDataRow, secondDataRow, includedCols);
    }

    /**
     * Returns the String representation of this distance function.
     * 
     * @return the String representation
     */
    @Override
    public String toString() {

        return "Euclidean Distance";
    }
}
