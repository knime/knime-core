/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: 28.06.2012
 * Author: Peter Ohl
 */
package org.knime.core.node.workflow;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;

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

    private boolean m_snapToGrid;

    private boolean m_showGrid;

    private int m_gridX;

    private int m_gridY;

    private double m_zoomLevel;

    /**
     * Constructor with defaults.
     */
    public EditorUIInformation() {
        m_snapToGrid = false;
        m_showGrid = false;
        m_gridX = -1;
        m_gridY = -1;
        m_zoomLevel = 1.0;
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final NodeSettingsRO config, final LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.ordinal() < LoadVersion.V260.ordinal()) {
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Grid: " + (m_snapToGrid?"on/":"off/") + (m_showGrid?"show":"hide") + "(" + m_gridX + "/" + m_gridY
        + "), Zoom: " + m_zoomLevel;
    }

}
