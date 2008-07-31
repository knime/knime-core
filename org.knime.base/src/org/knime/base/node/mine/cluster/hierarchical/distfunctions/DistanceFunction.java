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
package org.knime.base.node.mine.cluster.hierarchical.distfunctions;

import java.io.Serializable;

import org.knime.core.data.DataRow;


// TODO: has to changed to the knime framework distance functions when available
/**
 * The interface a distance function must implement.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public interface DistanceFunction extends Serializable {
    
    /**
     * The name sof the implemented distance functions.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public enum Names {
        /** Euclidean distance function. */
        Euclidean,
        /** Manhattan distance function. */
        Manhattan
    }

     /**
     * Calculates the distance between two data rows.
     * 
     * @param firstDataRow the first data row used to calculate the distance
     * @param secondDataRow the second data row used to calculate the distance
     * @param includedCols the columns to use for the distance calculation
     * 
     * @return the distance of the two rows
     */
    public double calcDistance(DataRow firstDataRow, DataRow secondDataRow,
            int[] includedCols);    
}
