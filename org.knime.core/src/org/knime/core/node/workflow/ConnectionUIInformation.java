/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
     * @return A new copy of this object with all its bend points shifted
     *         by the argument offset.
     */
    public ConnectionUIInformation createNewWithOffsetPosition(
            final int[] moveDist) {
        ConnectionUIInformation copy = new ConnectionUIInformation();
        for (int[] point : m_bendpoints) {
            copy.m_bendpoints.add(new int[] {
                    point[0] +  moveDist[0], point[1] +  moveDist[1]});
        }
        return copy;
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
