/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Apr 6, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;



/**
 * DrawingPane that also draws the regression line.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public class LinRegLineDrawingPane extends ScatterPlotterDrawingPane {
    private int m_x1;

    private int m_y1;

    private int m_x2;

    private int m_y2;


    /**
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        g.setColor(Color.BLACK);
        // bug fix#481, minimum thickness 1
        final int hDotSize = Math.max(1, getDotSize() / 3);
        ((Graphics2D)g).setStroke(new BasicStroke(hDotSize));
        g.drawLine(m_x1, m_y1, m_x2, m_y2);
        super.paintContent(g);
    }

    /**
     * Set first point of regression line.
     *
     * @param x1 x-coordinate of first point
     * @param y1 y-coordinate of first point
     */
    void setLineFirstPoint(final int x1, final int y1) {
        m_x1 = x1;
        m_y1 = y1;
    }

    /**
     * Set last point of regression line.
     *
     * @param x2 x-coordinate of last point
     * @param y2 y-coordinate of last point
     */
    void setLineLastPoint(final int x2, final int y2) {
        m_x2 = x2;
        m_y2 = y2;
    }
}
