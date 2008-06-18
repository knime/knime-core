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
package org.knime.base.node.mine.mds.distances;

import org.knime.base.node.mine.mds.DataPoint;
import org.knime.core.data.DataRow;

/**
 * 
 * @author Kilian thiel, University of Konstanz
 */
public class EuclideanDistanceManager implements DistanceManager {
    
    private boolean m_fuzzy;
    
    private boolean m_ignoreType = false;
    
    /**
     * Creates instance of <code>EuclideanDistanceManager</code>, which computes
     * euclidean distances between rows and cells. If fuzzy is set 
     * <code>true</code>, only fuzzy columns are considered to compute 
     * distance, if <code>false</code> only number columns.
     * 
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     */
    public EuclideanDistanceManager(final boolean fuzzy) {
        m_fuzzy = fuzzy;
    }
    
    /**
     * Creates instance of <code>EuclideanDistanceManager</code>, which 
     * computes the euclidean distances between rows and cells. The type 
     * (fuzzy or number) will be ignored. When dealing with fuzzy values the 
     * center of gravity is used, otherwise the numerical value.
     */
    public EuclideanDistanceManager() {
        m_fuzzy = false;
        m_ignoreType = true;
    }    

    /**
     * {@inheritDoc}
     */
    public double getDistance(final DataRow row1, final DataRow row2) {
        if (m_ignoreType) {
            return Distances.getEuclideanDistance(row1, row2);    
        }
        return Distances.getEuclideanDistance(row1, row2, m_fuzzy);
    }

    /**
     * {@inheritDoc}
     */
    public double getDistance(final DataPoint point1, final DataPoint point2) {
        return Distances.getEuclideanDistance(point1, point2);
    }    
    
    /**
     * {@inheritDoc}
     */
    public String getType() {
        return DistanceManagerFactory.EUCLIDEAN_DIST;
    }

    /**
     * @return the ignoreType
     */
    public boolean getIgnoreType() {
        return m_ignoreType;
    }

    /**
     * @param ignoreType the ignoreType to set
     */
    public void setIgnoreType(final boolean ignoreType) {
        m_ignoreType = ignoreType;
    }
}
