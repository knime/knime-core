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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.util.kdtree;

/**
 * This class represents a nearest neighbour found during the search.
 * 
 * @param <T> the type of the data object associated with the pattern
 * @author Thorsten Meinl, University of Konstanz
 */
public class NearestNeighbour<T> implements Comparable<NearestNeighbour<T>> {
    private final T m_data;

    private double m_distance;

    /**
     * Creates a new nearest neighbour.
     * 
     * @param data the data, can be <code>null</code>
     * @param distance the distance from the query pattern
     */
    NearestNeighbour(final T data, final double distance) {
        m_data = data;
        m_distance = distance;
    }

    /**
     * Returns the data associated with the pattern.
     * 
     * @return the data, can be <code>null</code>
     */
    public T getData() {
        return m_data;
    }

    /**
     * Returns the distance from the query pattern.
     * 
     * @return the distance
     */
    public double getDistance() {
        return m_distance;
    }

    /**
     * Sets the distance of this nearest neighbour.
     * 
     * @param newDistance the new distance
     */
    void setDistance(final double newDistance) {
        m_distance = newDistance;
    }
    
    /**
     * {@inheritDoc}
     */
    public int compareTo(final NearestNeighbour<T> o) {
        // query results are sorted by *de*creasing distance
        return (int) Math.signum(o.m_distance - this.m_distance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + m_distance + ", " + m_data + ")";
    }
}
