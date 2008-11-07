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
 *   09.07.2005 (Florian Georg): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Default implementation of a connection extra info.
 * 
 * By now it only stores bendpoints used by the modelling editor.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionUIInformation implements UIInformation {
    /** The key under which the type is registered. * */
    public static final String KEY_VERSION = "extrainfo.conn.version";

    /** The key under which the bounds are registered. * */
    public static final String KEY_BENDPOINTS = "extrainfo.conn.bendpoints";

    private final ArrayList<int[]> m_bendpoints = new ArrayList<int[]>();

    /**
     * Constructs a <code>ConnectionUIInformation</code>.
     * 
     */
    public ConnectionUIInformation() {

    }

    /**
     * Add a bendpoint.
     * 
     * @param x x coordinate
     * @param y y cordinate
     * @param index index of the point
     */
    public void addBendpoint(final int x, final int y, final int index) {
        m_bendpoints.add(index, new int[]{x, y});
    }

    /**
     * Removes a bendpoint.
     * 
     * @param index The point index
     */
    public void removeBendpoint(final int index) {
        m_bendpoints.remove(index);
    }

    /**
     * Returns a bendpoint.
     * 
     * @param index The point index
     * @return the point (int[]{x,y}), or <code>null</code>
     */
    public int[] getBendpoint(final int index) {
        return m_bendpoints.get(index);
    }

    /**
     * Gets all bendpoints.
     * 
     * @return all bendpoints
     */
    public int[][] getAllBendpoints() {
        return m_bendpoints.toArray(new int[m_bendpoints.size()][]);
    }

    /**
     * {@inheritDoc}
     */
    public void save(final NodeSettingsWO config) {
        config.addInt(KEY_BENDPOINTS + "_size", m_bendpoints.size());
        for (int i = 0; i < m_bendpoints.size(); i++) {
            config.addIntArray(KEY_BENDPOINTS + "_" + i, m_bendpoints.get(i));
        }

    }

    /**
     * {@inheritDoc}
     */
    public void load(final NodeSettingsRO config)
            throws InvalidSettingsException {
        int size = config.getInt(KEY_BENDPOINTS + "_size");
        for (int i = 0; i < size; i++) {
            m_bendpoints.add(i, config.getIntArray(KEY_BENDPOINTS + "_" + i));
        }
    }

    /**
     * Changes the position by setting the bend points according to the given
     * moving distance.
     * 
     * @param moveDist the distance to change the bend points
     */
    public void changePosition(final int[] moveDist) {

        for (int[] point : m_bendpoints) {
            point[0] += moveDist[0];
            point[1] += moveDist[1];
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder("bendpoints: ");
        for (int[] bendpoints : m_bendpoints) {
            for (int i = 0; i < bendpoints.length; i++) {
                bld.append(bendpoints[i] + ", ");
            }
        }
        return bld.toString();
    }
    
    /** {@inheritDoc} */
    @Override
    public ConnectionUIInformation clone() {
        ConnectionUIInformation newObject 
            = new ConnectionUIInformation();
        newObject.m_bendpoints.clear();
        for (int [] bendpoint : this.m_bendpoints) {
            newObject.m_bendpoints.add(new int[] {
                    bendpoint[0], bendpoint[1]
            });
        }
        return newObject;
    }
}
