/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   09.07.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.extrainfo;

import java.util.ArrayList;

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;
import de.unikn.knime.core.node.workflow.ConnectionExtraInfo;

/**
 * Default implementation of a connection extra info.
 * 
 * By now it only stores bendpoints used by the modelling editor.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ModellingConnectionExtraInfo implements ConnectionExtraInfo {

    /** The key under which the bounds are registered. * */
    public static final String KEY_BENDPOINTS = "extrainfo.conn.bendpoints";

    private ArrayList<int[]> m_bendpoints = new ArrayList<int[]>();

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
        return (int[][])m_bendpoints.toArray(new int[m_bendpoints.size()][]);
    }

    /**
     * @see de.unikn.knime.core.node.workflow.ConnectionExtraInfo
     *      #save(NodeSettingsWO)
     */
    public void save(final NodeSettingsWO config) {
        config.addInt(KEY_BENDPOINTS + "_size", m_bendpoints.size());
        for (int i = 0; i < m_bendpoints.size(); i++) {
            config.addIntArray(KEY_BENDPOINTS + "_" + i, (int[])m_bendpoints
                    .get(i));
        }

    }

    /**
     * @see de.unikn.knime.core.node.workflow.ConnectionExtraInfo
     *      #load(NodeSettingsRO)
     */
    public void load(final NodeSettingsRO config) throws InvalidSettingsException {
        int size = config.getInt(KEY_BENDPOINTS + "_size");

        for (int i = 0; i < size; i++) {
            m_bendpoints.add(i, config.getIntArray(KEY_BENDPOINTS + "_" + i));
        }

    }

}
