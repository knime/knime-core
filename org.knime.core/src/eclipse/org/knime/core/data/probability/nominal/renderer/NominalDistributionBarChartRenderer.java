/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 27, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability.nominal.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.Collection;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.core.data.renderer.DataValueRenderer;

import com.google.common.collect.Iterators;

/**
 * Renderer for {@link NominalDistributionValue} which shows the probability distribution values as a bar chart.
 * Hovering one of the bars triggers a tooltip which shows the class name and the probability percentage for the
 * corresponding bar.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class NominalDistributionBarChartRenderer extends AbstractPainterDataValueRenderer {

    private static final long serialVersionUID = 1L;

    private static final String DESCRIPTION_PROB_DISTR = "Bar Chart";

    private final NominalDistributionValueMetaData m_metaData;

    private NominalDistributionValue m_value;

    private int m_cellWidth;

    private NominalDistributionBarChartRenderer(final DataColumnSpec spec) {
        m_metaData = spec.getMetaDataOfType(NominalDistributionValueMetaData.class)
            .orElseThrow(() -> new IllegalStateException("Nominal distribution column without meta data encountered."));
    }

    /**
     * Returns true if the {@link DataColumnSpec} selected contains element names, which are not null or empty, since
     * they are needed to define the class names and if the data type of the selected column is compatible with
     * {@link NominalDistributionValue}, which is the expected type.
     *
     * @return {@code true} if the {@link DataColumnSpec} is accepted, {@code false} otherwise.
     */
    @Override
    public boolean accepts(final DataColumnSpec spec) {
        return (spec.getType().isCompatible(NominalDistributionValue.class)
            && spec.getMetaDataOfType(NominalDistributionValueMetaData.class).isPresent());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(m_metaData.size() * 50, 60);
    }

    /**
     *
     * @return "Bar Chart"
     */
    @Override
    public String getDescription() {
        return DESCRIPTION_PROB_DISTR;
    }

    /**
     * Returns the string to be used as a tooltip, while hovering the bars. The tooltip contains the class name followed
     * by the probability percentage shown by the corresponding bar.
     *
     * @return the tooltip corresponding to the hovered bar, or null if no bar is hovered.
     */
    @Override
    public String getToolTipText(final MouseEvent event) {
        final int pointX = event.getX();
        final int whichBar = pointX / (m_cellWidth / m_metaData.size());
        if (whichBar >= 0 && whichBar < m_metaData.size()) {
            final String value = Iterators.get(m_metaData.getValues().iterator(), whichBar);
            return String.format("%s: %.2f%%", value,
                m_value.getProbability(value) * 100);
        }
        return null;
    }

    /**
     * This method is used to paint the bar chart. The height of each bar is adjusted based on the probability value
     * associated with it. Futhermore, the method stores the width of the cell. Since the height of different cell can
     * differ, the width is used to identify which bar is being hovering.
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (m_value == null) {
            return;
        }
        m_cellWidth = Math.abs(getWidth()) - 10;
        final int rectWidth = m_cellWidth / m_metaData.size();
        final Graphics2D g2d = (Graphics2D)g.create();
        final Collection<String> probClasses = m_metaData.getValues();
        double minProb = Double.POSITIVE_INFINITY;
        double maxProb = Double.NEGATIVE_INFINITY;
        for (String value : probClasses) {
            final double prob = m_value.getProbability(value);
            if (prob > maxProb) {
                maxProb = prob;
            }

            if (prob < minProb) {
                minProb = prob;
            }
        }
        int i = 0;
        for (String value : probClasses) {
            final int cellHeight = Math.abs(getHeight()) - 10;
            final int rectHeight =
                (int)calculateRectHeight(m_value.getProbability(value), cellHeight, minProb, maxProb);
            g2d.setPaint(Color.ORANGE);
            g2d.setStroke(new BasicStroke(1));
            final int barStartPoint = cellHeight + 5 - rectHeight;
            g2d.fillRect(rectWidth * i + 5, barStartPoint, rectWidth, rectHeight);
            g2d.setPaint(Color.BLACK);
            g2d.drawRect(rectWidth * i + 5, barStartPoint, rectWidth, rectHeight);
            i++;
        }
        g2d.dispose();
    }

    private static double calculateRectHeight(final double classProbability, final double cellHeight,
        final double minProb, final double maxProb) {
        if (minProb == maxProb) {
            return cellHeight;
        } else if (classProbability == 0) {
            return 0;
        } else if (classProbability == minProb) {
            return 0.2 * cellHeight;
        } else {
            final double range = maxProb - minProb;
            return (((classProbability - minProb) / range) * cellHeight);
        }
    }

    @Override
    protected void setValue(final Object value) {
        if (value instanceof NominalDistributionValue) {
            m_value = (NominalDistributionValue)value;
        } else {
            m_value = null;
        }
    }

    /**
     * Renderer factory registered through extension point.
     *
     * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
     */
    public static final class BarChartRendererFactory extends AbstractDataValueRendererFactory {

        @Override
        public String getDescription() {
            return DESCRIPTION_PROB_DISTR;
        }

        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new NominalDistributionBarChartRenderer(colSpec);
        }
    }
}
