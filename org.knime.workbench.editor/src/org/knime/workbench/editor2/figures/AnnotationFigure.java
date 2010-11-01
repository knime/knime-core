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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   2010 10 11 (ohl): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class AnnotationFigure extends RectangleFigure {

    private MyStyledText m_styledText;

    public AnnotationFigure(final Composite paintArea, final String text) {

        m_styledText = new MyStyledText(paintArea, SWT.None);
        m_styledText.setText(text);
        setLayoutManager(new GridLayout(1, true));

    }

    public void setText(final String text) {
        m_styledText.setText(text);
        repaint();
    }

    // @Override
    // protected void layout() {
    // if (isVisible()) {
    // int width = getClientArea().width;
    // int height = getClientArea().height;
    // // Create any SWT control here
    // Point p1 = new Point(0, 0);
    // translateToAbsolute(p1); // coordinates returned are negative if
    // // canvas is scrolled
    // m_styledText.setBounds(getBounds().x + p1.x, getBounds().y + p1.y,
    // width, height);
    // m_styledText.moveAbove(null);
    // }
    // super.layout();
    // }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {
        super.paintFigure(graphics);
        Rectangle b = getBounds();
        Point p = new Point(0, 0);
        translateToAbsolute(p);
        m_styledText.setBounds(b.x + p.x, b.y + p.y, b.width, b.height);
        // setBounds(new Rectangle(0, b.y, b.width, b.height));
        // if (m_image != null) {
        // Rectangle r = getBounds();
        // graphics.drawImage(m_image, r.x, r.y);
        // }
        m_styledText.forceRedraw();
    }

    /**
     * @see org.eclipse.draw2d.IFigure#removeNotify()
     */
    @Override
    public void removeNotify() {
        if (m_styledText != null) {
            m_styledText.dispose();
            m_styledText = null;
        }
        super.removeNotify();
    }

    class MyStyledText extends StyledText {

        private boolean m_repaintNow = false;

        /**
         *
         */
        public MyStyledText(final Composite parent, final int style) {
            super(parent, style);
        }

        public void forceRedraw() {
            super.redraw();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void redraw() {
            // wait for the forced redraw
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void redraw(final int x, final int y, final int width,
                final int height, final boolean all) {
            // wait for the forced redraw
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void redrawRange(final int start, final int length,
                final boolean clearBackground) {
            // wait for the forced redraw
        }
    }
}
