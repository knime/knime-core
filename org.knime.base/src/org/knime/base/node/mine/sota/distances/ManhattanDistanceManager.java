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
 *   02.02.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.distances;

import org.knime.base.node.mine.sota.logic.SotaTreeCell;
import org.knime.core.data.DataRow;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class ManhattanDistanceManager implements DistanceManager {

    private boolean m_fuzzy;

    /**
     * Creates instance of <code>ManhattanDistanceManager</code>, which
     * computes manhattan distances between <code>DataRow</code>s and
     * <code>SotaTreeCell</code>s. If fuzzy is set <code>true</code>, only
     * fuzzy columns are considered, if <code>false</code> only number
     * columns.
     * 
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     */
    public ManhattanDistanceManager(final boolean fuzzy) {
        m_fuzzy = fuzzy;
    }

    /**
     * {@inheritDoc}
     */
    public double getDistance(final DataRow row, final SotaTreeCell cell) {
        return Distances.getManhattanDistance(row, cell, m_fuzzy);
    }

    /**
     * {@inheritDoc}
     */
    public double getDistance(final DataRow row1, final DataRow row2) {
        return Distances.getManhattanDistance(row1, row2, m_fuzzy);
    }

}
