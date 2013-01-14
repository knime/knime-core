/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   10.11.2011 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.image;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The settings of the Decision Tree To Image node.
 *
 * @author Heiko Hofer
 */
public class DecTreeToImageNodeSettings {
    private static final String UNFOLD_METHOD = "unfoldMethod";
    private static final String UNFOLD_WITH_COVERAGE = "unfoldWithCoverage";
    private static final String UNFOLD_TO_Level = "unfoldToLevel";
    private static final String DISPLAY_TABLE = "displayTable";
    private static final String DISPLAY_CHART = "displayChart";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SCALE_FACTOR = "scaleFactor";
    private static final String SCALING = "scaling";

    /**
     * Scaling method.
     *
     * @author Heiko Hofer
     */
    enum Scaling {
        /** fixed scaling using m_scale_factor */
        fixed,
        /** Fit To Image Area */
        fit,
        /** Shrink To Image Area */
        shrink
    }

    /**
     * Defines which method is used to determine the tree nodes that should
     * be displayed.
     *
     * @author Heiko Hofer
     */
    enum UnfoldMethod {
        /** Display all nodes from the root (level 0) to a predefined level. */
        level,
        /** Display nodes with a coverage of trainings data greater
         * than a predefined value. */
        totalCoverage,
    }

    private UnfoldMethod m_unfoldMethod = UnfoldMethod.totalCoverage;
    private int m_unfoldToLevel = 2;
    private double m_unfoldWithCoverage = 0.05;

    private boolean m_displayTable = true;
    private boolean m_displayChart = true;

    private int m_width = 800;
    private int m_height = 600;
    private float m_scaleFactor = 1.0f;
    private Scaling m_scaling = Scaling.shrink;


    /**
     * @return the unfoldToLevel
     */
    int getUnfoldToLevel() {
        return m_unfoldToLevel;
    }

    /**
     * @param unfoldToLevel the unfoldToLevel to set
     */
    void setUnfoldToLevel(final int unfoldToLevel) {
        m_unfoldToLevel = unfoldToLevel;
    }



    /**
     * @return the unfoldMethod
     */
    UnfoldMethod getUnfoldMethod() {
        return m_unfoldMethod;
    }

    /**
     * @param unfoldMethod the unfoldMethod to set
     */
    void setUnfoldMethod(final UnfoldMethod unfoldMethod) {
        m_unfoldMethod = unfoldMethod;
    }

    /**
     * @return the unfoldWidthCoverage
     */
    double getUnfoldWithCoverage() {
        return m_unfoldWithCoverage;
    }

    /**
     * @param unfoldWidthCoverage the unfoldWidthCoverage to set
     */
    void setUnfoldWidthCoverage(final double unfoldWidthCoverage) {
        m_unfoldWithCoverage = unfoldWidthCoverage;
    }

    /**
     * @return the displayTable
     */
    boolean getDisplayTable() {
        return m_displayTable;
    }

    /**
     * @param displayTable the displayTable to set
     */
    void setDisplayTable(final boolean displayTable) {
        m_displayTable = displayTable;
    }

    /**
     * @return the displayChart
     */
    boolean getDisplayChart() {
        return m_displayChart;
    }

    /**
     * @param displayChart the displayChart to set
     */
    void setDisplayChart(final boolean displayChart) {
        m_displayChart = displayChart;
    }

    /**
     * @return the width
     */
    int getWidth() {
        return m_width;
    }

    /**
     * @param width the width to set
     */
    void setWidth(final int width) {
        m_width = width;
    }

    /**
     * @return the height
     */
    int getHeight() {
        return m_height;
    }

    /**
     * @param height the height to set
     */
    void setHeight(final int height) {
        m_height = height;
    }

    /**
     * @return the scaleFactor
     */
    float getScaleFactor() {
        return m_scaleFactor;
    }

    /**
     * @param scaleFactor the scaleFactor to set
     */
    void setScaleFactor(final float scaleFactor) {
        m_scaleFactor = scaleFactor;
    }

    /**
     * @return the scaling
     */
    Scaling getScaling() {
        return m_scaling;
    }

    /**
     * @param scaling the scaling to set
     */
    void setScaling(final Scaling scaling) {
        m_scaling = scaling;
    }


    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_unfoldMethod = UnfoldMethod.valueOf(
                settings.getString(UNFOLD_METHOD));
        m_unfoldToLevel = settings.getInt(UNFOLD_TO_Level);
        m_unfoldWithCoverage = settings.getDouble(UNFOLD_WITH_COVERAGE);
        m_displayTable = settings.getBoolean(DISPLAY_TABLE);
        m_displayChart = settings.getBoolean(DISPLAY_CHART);
        m_width = settings.getInt(WIDTH);
        m_height = settings.getInt(HEIGHT);
        m_scaleFactor = settings.getFloat(SCALE_FACTOR);
        m_scaling = Scaling.valueOf(settings.getString(SCALING));

    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_unfoldMethod = UnfoldMethod.valueOf(settings.getString(
                UNFOLD_METHOD, UnfoldMethod.totalCoverage.name()));
        m_unfoldToLevel = settings.getInt(UNFOLD_TO_Level, 1);
        m_unfoldWithCoverage = settings.getDouble(UNFOLD_WITH_COVERAGE, 0.05);
        m_displayTable = settings.getBoolean(DISPLAY_TABLE, true);
        m_displayChart = settings.getBoolean(DISPLAY_CHART, true);
        m_width = settings.getInt(WIDTH, 600);
        m_height = settings.getInt(HEIGHT, 400);
        m_scaleFactor = settings.getFloat(SCALE_FACTOR, 0.8333f);
        m_scaling = Scaling.valueOf(settings.getString(
                        SCALING, Scaling.fixed.name()));
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(UNFOLD_METHOD, m_unfoldMethod.name());
        settings.addInt(UNFOLD_TO_Level, m_unfoldToLevel);
        settings.addDouble(UNFOLD_WITH_COVERAGE, m_unfoldWithCoverage);
        settings.addBoolean(DISPLAY_TABLE, m_displayTable);
        settings.addBoolean(DISPLAY_CHART, m_displayChart);
        settings.addInt(WIDTH, m_width);
        settings.addInt(HEIGHT, m_height);
        settings.addFloat(SCALE_FACTOR, m_scaleFactor);
        settings.addString(SCALING, m_scaling.name());
    }

}
