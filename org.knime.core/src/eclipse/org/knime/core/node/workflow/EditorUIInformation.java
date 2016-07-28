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
package org.knime.core.node.workflow;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Stores workflow editor specific settings (like grid settings and zoom level, etc.).
 * Can be attached to the node UI info object of workflow managers.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 * @since 2.6
 */
public class EditorUIInformation implements UIInformation {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(EditorUIInformation.class);

    private static final String KEY_SNAP_GRID = "workflow.editor.snapToGrid";

    private static final String KEY_SHOW_GRID = "workflow.editor.ShowGrid";

    private static final String KEY_X_GRID = "workflow.editor.gridX";

    private static final String KEY_Y_GRID = "workflow.editor.gridY";

    private static final String KEY_ZOOM = "workflow.editor.zoomLevel";

    private static final String KEY_CURVED_CONNECTIONS = "workflow.editor.curvedConnections";

    private static final String KEY_CONNECTION_WIDTH = "workflow.editor.connectionWidth";

    private boolean m_snapToGrid;

    private boolean m_showGrid;

    private int m_gridX;

    private int m_gridY;

    private double m_zoomLevel;

    private boolean m_hasCurvedConnections;

    private int m_connectionLineWidth;

    /**
     * Constructor with defaults.
     */
    public EditorUIInformation() {
        m_snapToGrid = false;
        m_showGrid = false;
        m_gridX = -1;
        m_gridY = -1;
        m_zoomLevel = 1.0;
        m_hasCurvedConnections = false;
        m_connectionLineWidth = 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EditorUIInformation clone() {
        EditorUIInformation clone = new EditorUIInformation();
        clone.m_snapToGrid = m_snapToGrid;
        clone.m_showGrid = m_showGrid;
        clone.m_gridX = m_gridX;
        clone.m_gridY = m_gridY;
        clone.m_zoomLevel = m_zoomLevel;
        clone.m_hasCurvedConnections = m_hasCurvedConnections;
        clone.m_connectionLineWidth = m_connectionLineWidth;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final NodeSettingsWO config) {
        config.addBoolean(KEY_SNAP_GRID, m_snapToGrid);
        config.addBoolean(KEY_SHOW_GRID, m_showGrid);
        config.addInt(KEY_X_GRID, m_gridX);
        config.addInt(KEY_Y_GRID, m_gridY);
        config.addDouble(KEY_ZOOM, m_zoomLevel);
        config.addBoolean(KEY_CURVED_CONNECTIONS, m_hasCurvedConnections);
        config.addInt(KEY_CONNECTION_WIDTH, m_connectionLineWidth);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final NodeSettingsRO config, final FileWorkflowPersistor.LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.ordinal() < FileWorkflowPersistor.LoadVersion.V260.ordinal()) {
            m_snapToGrid = false;
            m_showGrid = false;
            m_gridX = -1;
            m_gridY = -1;
            m_zoomLevel = 1.0;
        } else {
            m_snapToGrid = config.getBoolean(KEY_SNAP_GRID);
            m_showGrid = config.getBoolean(KEY_SHOW_GRID);
            m_gridX = config.getInt(KEY_X_GRID);
            m_gridY = config.getInt(KEY_Y_GRID);
            m_zoomLevel = config.getDouble(KEY_ZOOM);
        }
        if(config.containsKey(KEY_CURVED_CONNECTIONS)) {
            m_hasCurvedConnections = config.getBoolean(KEY_CURVED_CONNECTIONS);
        }
        if(config.containsKey(KEY_CONNECTION_WIDTH)) {
            m_connectionLineWidth = config.getInt(KEY_CONNECTION_WIDTH);
        }
    }

    /**
     * @return the snapToGrid
     */
    public boolean getSnapToGrid() {
        return m_snapToGrid;
    }

    /**
     * @param snapToGrid the snapToGrid to set
     */
    public void setSnapToGrid(final boolean snapToGrid) {
        m_snapToGrid = snapToGrid;
    }

    /**
     * @return the showGrid
     */
    public boolean getShowGrid() {
        return m_showGrid;
    }

    /**
     * @param showGrid the showGrid to set
     */
    public void setShowGrid(final boolean showGrid) {
        m_showGrid = showGrid;
    }

    /**
     * @return the gridX
     */
    public int getGridX() {
        return m_gridX;
    }

    /**
     * @param gridX the gridX to set
     */
    public void setGridX(final int gridX) {
        m_gridX = gridX;
    }

    /**
     * @return the gridY
     */
    public int getGridY() {
        return m_gridY;
    }

    /**
     * @param gridY the gridY to set
     */
    public void setGridY(final int gridY) {
        m_gridY = gridY;
    }

    /**
     * @return the zoomLevel
     */
    public double getZoomLevel() {
        return m_zoomLevel;
    }

    /**
     * @param zoomLevel the zoomLevel to set
     */
    public void setZoomLevel(final double zoomLevel) {
        m_zoomLevel = zoomLevel;
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
     */
    public void setConnectionLineWidth(final int width) {
        m_connectionLineWidth = width;
    }

    /**
     * @return the width of the line connecting two nodes
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



}
