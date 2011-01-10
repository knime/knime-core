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
 *   05.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.List;

/**
 * Represents a line by a list of points which are connected to one line. Hence,
 * the ordering of the points in the list is important.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicLine extends BasicDrawingElement {

    
    private int m_lineWidth = 1;
    
    
    

    /**
     * Paints the line by connecting all points. 
     * 
     * @see org.knime.base.node.viz.plotter.basic.BasicDrawingElement
     * #paint(java.awt.Graphics2D)
     */
    @Override
    public void paint(final Graphics2D g) {
        Color backupColor = g.getColor();
        Stroke backupStroke = g.getStroke();
        g.setColor(getColor());
        g.setStroke(new BasicStroke(m_lineWidth));
        int[] x = new int[getPoints().size()];
        int[] y = new int[getPoints().size()];
        for (int i = 0; i < getPoints().size(); i++) {
            x[i] = getPoints().get(i).x;
            y[i] = getPoints().get(i).y;
        }
        g.drawPolyline(x, y, getPoints().size());
        g.setColor(backupColor);
        g.setStroke(backupStroke);
    }
    
    
    
   /**
    * {@inheritDoc}
    */
    @Override
    public void setPoints(final List<Point> points) {
        super.setPoints(points);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void setPoints(final Point... points) {
        super.setPoints(points);
    }


    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setStroke(final Stroke stroke) {
        super.setStroke(stroke);
        m_lineWidth = (int)((BasicStroke)stroke).getLineWidth();
    }





}
