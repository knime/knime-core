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
 *   Nov 21, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import org.knime.core.data.DataCell;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public interface SotaCell {

    /**
     * Adjusts the cells value related to the given cell with given
     * learningrate.
     * 
     * @param cell cell to adjust SotaCell with
     * @param learningrate learningrate to adjust cell value with
     */
    public void adjustCell(DataCell cell, double learningrate);

    /**
     * Returns a double value of the cell.
     * 
     * @return a double value of the cell
     */
    public double getValue();

    /**
     * Clones the SotaCell instance and returns the clone.
     * 
     * @return the clone of the SotaCell instance
     */
    public SotaCell clone();
}
