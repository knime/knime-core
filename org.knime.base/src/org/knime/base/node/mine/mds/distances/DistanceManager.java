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
 *   Nov 23, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds.distances;

import org.knime.base.node.mine.mds.DataPoint;
import org.knime.core.data.DataRow;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
public interface DistanceManager {

    /**
     * Returns the distance between the given <code>DataRow</code>s, row1 and
     * row2. The distance metric is up to the concrete implementation.
     *
     * @param row1 First <code>DataRow</code> to compute distance.
     * @param row2 Second <code>DataRow</code> to compute distance.
     * @return The distance between given <code>DataRow</code>s.
     */
    public double getDistance(DataRow row1, DataRow row2);

    /**
     * Returns the distance between the given <code>DataPoint</code>s, point1
     * and point2. The distance metric is up to the concrete implementation.
     *
     * @param point1 First <code>DataPoint</code> to compute distance.
     * @param point2 Second <code>DataPoint</code> to compute distance.
     * @return The distance between given <code>DataRow</code>s.
     */
    public double getDistance(DataPoint point1, DataPoint point2);

    /**
     * @return The type of the <code>DistanceManager</code>. See
     *         <code>DistanceManagerFactory</code> for valid types.
     *
     * @see org.knime.base.node.mine.mds.distances.DistanceManagerFactory
     *      #COS_DIST
     * @see org.knime.base.node.mine.mds.distances.DistanceManagerFactory
     *      #EUCLIDEAN_DIST
     * @see org.knime.base.node.mine.mds.distances.DistanceManagerFactory
     *      #MANHATTAN_DIST
     */
    public String getType();
}
