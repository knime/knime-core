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
 * History
 *   Nov 23, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.distances;

import org.knime.base.node.mine.sota.logic.SotaTreeCell;
import org.knime.core.data.DataRow;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public interface DistanceManager {
    /**
     * Returns the distance between the given cell and row. The distance metric
     * is up to the concrete implementation.
     * 
     * @param row row to compute distance
     * @param cell SotaTreeCell to compute distance
     * @return the distance between given row and cell
     */
    public double getDistance(DataRow row, SotaTreeCell cell);

    /**
     * Returns the distance between the given row1 and row2. The distance metric
     * is up to the concrete implementation.
     * 
     * @param row1 first row to compute distance
     * @param row2 second row to compute distance
     * @return the distance between given row1 and row2
     */
    public double getDistance(DataRow row1, DataRow row2);
}
