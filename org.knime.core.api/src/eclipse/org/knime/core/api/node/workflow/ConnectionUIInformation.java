/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.core.api.node.workflow;

import java.util.ArrayList;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Default implementation of a connection extra info.
 *
 * By now it only stores bendpoints used by the modelling editor.
 *
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionUIInformation {

    private final int[][] m_bendpoints;


    ConnectionUIInformation(final Builder connectionUIInfo) {
        m_bendpoints =new int[connectionUIInfo.m_bendpoints.size()][];
        for (int i = 0; i < m_bendpoints.length; i++) {
            m_bendpoints[i] = connectionUIInfo.m_bendpoints.get(i).clone();
        }
    }

    /**
     * Returns a bendpoint.
     *
     * @param index The point index
     * @return the point (int[]{x,y}), or <code>null</code>
     */
    public int[] getBendpoint(final int index) {
        return m_bendpoints[index];
    }

    /**
     * Gets all bendpoints.
     *
     * @return all bendpoints
     */
    public int[][] getAllBendpoints() {
        return m_bendpoints.clone();
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
        //we should not provide a clone method
        //e.g. conflicts with the final-fields
        //see also https://stackoverflow.com/questions/2427883/clone-vs-copy-constructor-which-is-recommended-in-java
        throw new UnsupportedOperationException();
    }

    /** @return new Builder with defaults. */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * @param connectionUIInfo object to copy the values from
     * @return new Builder with the values copied from the passed argument
     */
    public static final Builder builder(final ConnectionUIInformation connectionUIInfo) {
        return new Builder().copyFrom(connectionUIInfo);
    }

    /** Builder pattern for {@link ConnectionUIInformation}. */
    public static final class Builder {

        private ArrayList<int[]> m_bendpoints = new ArrayList<int[]>();

        /** Builder with defaults. */
        Builder() {
        }

        /** Copy all fields from argument and return this.
         * @param connectionUIInfo to copy from, not null.
         * @return this
         */
        public Builder copyFrom(final ConnectionUIInformation connectionUIInfo) {
            m_bendpoints = new ArrayList<int[]>(connectionUIInfo.m_bendpoints.length);
            for (int i = 0; i < connectionUIInfo.m_bendpoints.length; i++) {
                m_bendpoints.add(connectionUIInfo.m_bendpoints[i].clone());
            }
            return this;
        }

        /**
         * Changes the position by setting the bend points according to the given
         * moving distance.
         *
         * @param moveDist the distance to change the bend points
         * @return this
         */
        public Builder translate(final int[] moveDist) {
            m_bendpoints.forEach(point -> {
                point[0] = point[0] + moveDist[0];
                point[1] = point[1] + moveDist[1];
            });
            return this;
        }

        /**
         * Add a bendpoint.
         *
         * @param x x coordinate
         * @param y y cordinate
         * @param index index of the point
         * @return this
         */
        public Builder addBendpoint(final int x, final int y, final int index) {
            m_bendpoints.add(index, new int[]{x, y});
            return this;
        }

        /**
         * Removes a bendpoint.
         *
         * @param index The point index
         * @return this
         */
        public Builder removeBendpoint(final int index) {
            m_bendpoints.remove(index);
            return this;
        }

        /** @return {@link ConnectionUIInformation} with current values. */
        public ConnectionUIInformation build() {
            return new ConnectionUIInformation(this);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

    }
}
