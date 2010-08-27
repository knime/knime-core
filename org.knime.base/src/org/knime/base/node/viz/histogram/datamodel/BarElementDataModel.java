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
 *    13.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;

/**
 * This class represents the smallest element of a histogram and corresponds
 * to the color in which the rows of this element are colored. The BarElements
 * belong to one
 * {@link org.knime.base.node.viz.histogram.datamodel.BarDataModel}.
 * @author Tobias Koetter, University of Konstanz
 */
public class BarElementDataModel extends AggregationValSubModel <Rectangle2D,
Rectangle2D>
implements Serializable {

    private static final long serialVersionUID = 2537898631338523620L;

    /**Constructor for class BarElementDataModel.
     * @param color the color of this element
     */
    protected BarElementDataModel(final Color color) {
        this(color, false);
    }

    /**Constructor for class BarElementDataModel.
     * @param color the color to use for this bar element
     * @param supportHiliting if hiliting should be supported
     */
    protected BarElementDataModel(final Color color,
            final boolean supportHiliting) {
       super(color, supportHiliting);
    }

    /**Constructor for class BarElementDataModel (used for cloning).
     * @param color
     * @param aggrSum
     * @param valueCounter
     * @param rowCounter
     */
    private BarElementDataModel(final Color color, final boolean enableHiliting,
            final double aggrSum, final int valueCounter,
            final int rowCounter) {
        super(color, enableHiliting, aggrSum, valueCounter, rowCounter);
    }

    private BarElementDataModel(final ConfigRO config)
    throws InvalidSettingsException {
        super(config);
    }

    /**
     * @return the {@link Rectangle} the element should be drawn on the
     * screen
     */
    public Rectangle2D getElementRectangle() {
        return getShape();
    }

    /**
     * @param rect the {@link Rectangle} to set or <code>null</code>
     * @param calculator the hilite shape calculator
     */
    protected void setRectangle(final Rectangle2D rect,
            final HistogramHiliteCalculator calculator) {
        setShape(rect, calculator);
    }

    /**
     * @param xCoord the new x coordinate
     * @param elementWidth the new element width
     * @param calculator the hilite shape calculator
     */
    public void updateElementWidth(final int xCoord, final int elementWidth,
            final HistogramHiliteCalculator calculator) {
        final Rectangle2D rectangle = getShape();
        if (rectangle == null) {
            return;
        }
        final int yCoord = (int)rectangle.getY();
        final int elementHeight = (int)rectangle.getHeight();
        final Rectangle rect = new Rectangle(xCoord, yCoord,
                elementWidth, elementHeight);
        setRectangle(rect, calculator);
//        m_elementRectangle.setBounds(xCoord, yCoord,
//                elementWidth, elementHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BarElementDataModel clone() {
        final BarElementDataModel clone = new BarElementDataModel(getColor(),
                supportsHiliting(), getAggregationSum(), getValueCount(),
                getRowCount());
        return clone;
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the loaded {@link BarElementDataModel}
     * @throws CanceledExecutionException if the operation is canceled
     * @throws InvalidSettingsException if the config object is invalid
     */
    public static BarElementDataModel loadFromFile(final Config config,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        exec.checkCanceled();
        return new BarElementDataModel(config);
    }
}
