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
 * ------------------------------------------------------------------------
 */
package org.knime.core.def.node.workflow;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Stores workflow editor specific settings (like grid settings and zoom level, etc.).
 * Can be attached to the node UI info object of workflow managers.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 * @since 2.6
 */
public class EditorUIInformation {

    private final boolean m_snapToGrid;

    private final boolean m_showGrid;

    private final int m_gridX;

    private final int m_gridY;

    private final double m_zoomLevel;

    private final boolean m_hasCurvedConnections;

    private final int m_connectionLineWidth;

    EditorUIInformation(final Builder editorUIInfo) {
        m_snapToGrid = editorUIInfo.m_snapToGrid;
        m_showGrid = editorUIInfo.m_showGrid;
        m_gridX = editorUIInfo.m_gridX;
        m_gridY = editorUIInfo.m_gridY;
        m_zoomLevel = editorUIInfo.m_zoomLevel;
        m_hasCurvedConnections = editorUIInfo.m_hasCurvedConnections;
        m_connectionLineWidth = editorUIInfo.m_connectionLineWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EditorUIInformation clone() {
        //we should not provide a clone method
        //e.g. conflicts with the final-fields
        //see also https://stackoverflow.com/questions/2427883/clone-vs-copy-constructor-which-is-recommended-in-java
        throw new UnsupportedOperationException();
    }

    /**
     * @return the snapToGrid
     */
    public boolean getSnapToGrid() {
        return m_snapToGrid;
    }

    /**
     * @return the showGrid
     */
    public boolean getShowGrid() {
        return m_showGrid;
    }

    /**
     * @return the gridX
     */
    public int getGridX() {
        return m_gridX;
    }

    /**
     * @return the gridY
     */
    public int getGridY() {
        return m_gridY;
    }

    /**
     * @return the zoomLevel
     */
    public double getZoomLevel() {
        return m_zoomLevel;
    }

    /**
     * @return whether connections are rendered as curves
     * @since 3.3
     */
    public boolean getHasCurvedConnections() {
        return m_hasCurvedConnections;
    }

    /**
     * @param curved if <code>true</code> connections are rendered as curves, otherwise as straight lines
     * @since 3.3
     */
    public void setHasCurvedConnections(final boolean curved) {
        m_hasCurvedConnections = curved;
    }

    /**
     * @param width the width of the line connection two nodes
     * @since 3.3
     */
    public void setConnectionLineWidth(final int width) {
        m_connectionLineWidth = width;
    }

    /**
     * @return the width of the line connecting two nodes
     * @since 3.3
     */
    public int getConnectionLineWidth() {
        return m_connectionLineWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Grid: " + (m_snapToGrid ? "on/" : "off/") + (m_showGrid ? "show" : "hide") + "(" + m_gridX + "/"
            + m_gridY + "), Zoom: " + m_zoomLevel + ", Curved Connections: " + m_hasCurvedConnections
            + ", Connection Line Width: " + m_connectionLineWidth;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // auto-generated (eclipse action)
        final int prime = 31;
        int result = 1;
        result = prime * result + m_gridX;
        result = prime * result + m_gridY;
        result = prime * result + (m_showGrid ? 1231 : 1237);
        result = prime * result + (m_snapToGrid ? 1231 : 1237);
        long temp;
        temp = Double.doubleToLongBits(m_zoomLevel);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + Boolean.hashCode(m_hasCurvedConnections);
        result = prime * result + m_connectionLineWidth;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        // auto-generated (eclipse action)
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EditorUIInformation other = (EditorUIInformation)obj;
        if (m_gridX != other.m_gridX) {
            return false;
        }
        if (m_gridY != other.m_gridY) {
            return false;
        }
        if (m_showGrid != other.m_showGrid) {
            return false;
        }
        if (m_snapToGrid != other.m_snapToGrid) {
            return false;
        }
        if (Double.doubleToLongBits(m_zoomLevel) != Double.doubleToLongBits(other.m_zoomLevel)) {
            return false;
        }
        if (m_hasCurvedConnections != other.m_hasCurvedConnections) {
            return false;
        }
        if (m_connectionLineWidth != other.m_connectionLineWidth) {
            return false;
        }
        return true;
    }

    /** @return new Builder with defaults. */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * @param editorUIInfo the instance to take the initial values from
     * @return a new {@link Builder} initialized with all the values copied from the passed argument
     */
    public static final Builder builder(final EditorUIInformation editorUIInfo) {
        return new Builder().copyFrom(editorUIInfo);
    }


    /** Builder pattern for {@link EditorUIInformation}. */
    public static final class Builder {

        private boolean m_snapToGrid = false;
        private boolean m_showGrid = false;
        private int m_gridX = -1;
        private int m_gridY = -1;
        private double m_zoomLevel = 1.0;
        private boolean m_hasCurvedConnections = false;
        private int m_connectionLineWidth = 1;

        /** Builder with defaults. */
        Builder() {
        }

        /** Copy all fields from argument and return this.
         * @param editorUIInfo to copy from, not null.
         * @return this
         */
        public Builder copyFrom(final EditorUIInformation editorUIInfo) {
            m_snapToGrid = editorUIInfo.m_snapToGrid;
            m_showGrid = editorUIInfo.m_showGrid;
            m_gridX = editorUIInfo.m_gridX;
            m_gridY = editorUIInfo.m_gridY;
            m_zoomLevel = editorUIInfo.m_zoomLevel;
            m_hasCurvedConnections = editorUIInfo.m_hasCurvedConnections;
            m_connectionLineWidth = editorUIInfo.m_connectionLineWidth;
            return this;
        }

        /** @param snapToGrid the snapToGrid to set
         * @return this */
        public Builder setSnapToGrid(final boolean snapToGrid) {
            m_snapToGrid = snapToGrid;
            return this;
        }

        /** @param showGrid the showGrid to set
         * @return this */
        public Builder setShowGrid(final boolean showGrid) {
            m_showGrid = showGrid;
            return this;
        }

        /** @param gridX the gridX to set
         * @return this */
        public Builder setGridX(final int gridX) {
            m_gridX = gridX;
            return this;
        }

        /** @param gridY the gridY to set
         * @return this */
        public Builder setGridY(final int gridY) {
            m_gridY = gridY;
            return this;
        }

        /** @param zoomLevel the zoomLevel to set
         * @return this */
        public Builder setZoomLevel(final double zoomLevel) {
            m_zoomLevel = zoomLevel;
            return this;
        }

        /**@param hasCurvedConnections the hasCurvedConnections to set
         * @return this */
        public Builder setHasCurvedConnections(final boolean hasCurvedConnections) {
            m_hasCurvedConnections = hasCurvedConnections;
            return this;
        }

        /** @param connectionLineWidth the connectionLineWidth to set
         * @return this */
        public Builder setConnectionLineWidth(final int connectionLineWidth) {
            m_connectionLineWidth = connectionLineWidth;
            return this;
        }

        /** @return {@link EditorUIInformation} with current values. */
        public EditorUIInformation build() {
            return new EditorUIInformation(this);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

    }

}
