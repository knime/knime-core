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
 *   05.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.node.viz.plotter.AbstractDrawingPane;

/**
 * The <code>BasicDrawingPane</code> stores the 
 * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s and 
 * paints them in the {@link #paintContent(Graphics)} method by calling their 
 * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement
 * #paint(Graphics2D)} method. The 
 * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s can be 
 * added or all can be removed. The mapping of the domain values to the 
 * DrawingPane's dimension is done in the 
 * {@link org.knime.base.node.viz.plotter.basic.BasicPlotter}.  
 * 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicDrawingPane extends AbstractDrawingPane {
    
    private final List<BasicDrawingElement> m_elements;
    
    /**
     * 
     *
     */
    public BasicDrawingPane() {
        super();
        m_elements = new LinkedList<BasicDrawingElement>();
    }
    
    /**
     * Removes all drawing elements. Repaint has to be triggered. 
     *
     */
    public synchronized void clearPlot() {
        m_elements.clear();
    }
    
    /**
     * 
     * @param shape shape to draw
     */
    public synchronized void addDrawingElement(final BasicDrawingElement shape) {
        m_elements.add(shape);
    }
    
    /**
     * 
     * @return the current stored drawing elements
     */
    public synchronized List<BasicDrawingElement> getDrawingElements() {
        return m_elements;
    }

    /**
     * Paints all added 
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s by 
     * calling their 
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement
     * #paint(Graphics2D)} method.
     * If the BasicDrawingPane is extended this method have to be called with 
     * <code>super.paintContent()</code> in order to maintain the 
     * functionality of painting
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s.
     *  
     * @see org.knime.base.node.viz.plotter.AbstractDrawingPane#paintContent(
     * java.awt.Graphics)
     */
    @Override
    public synchronized void paintContent(final Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        // paint paths if there are some
        if (getDrawingElements() != null) {
            for (BasicDrawingElement path : getDrawingElements()) {
                path.paint(g2);
            }
        }
    }
    
    


}
