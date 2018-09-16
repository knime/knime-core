/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayeredPane;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Image;

/**
 * The root figure, containing potentially a progress tool tip helper and an image representing the job manager.
 *
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowFigure extends FreeformLayeredPane {
    private ProgressToolTipHelper m_progressToolTipHelper;

    private Image m_jobManagerFigure;

    private final TentStakeFigure m_tentStakeFigure;

    /**
     * New workflow root figure.
     */
    public WorkflowFigure() {
        // not opaque, so that we can directly select on the "background" layer
        setOpaque(false);

        m_tentStakeFigure = new TentStakeFigure();
        add(m_tentStakeFigure);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics graphics) {
        super.paint(graphics);
        paintChildren(graphics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintFigure(final Graphics graphics) {
        super.paintFigure(graphics);
        if (m_jobManagerFigure != null) {
            final org.eclipse.swt.graphics.Rectangle imgBox = m_jobManagerFigure.getBounds();
            final Rectangle bounds2 = getBounds();
            graphics.drawImage(m_jobManagerFigure, 0, 0, imgBox.width, imgBox.height, bounds2.width - imgBox.width, 5,
                imgBox.width, imgBox.height + 5);
        }
    }

    /**
     * @param jobManagerFigure the jobManagerFigure to set
     */
    public void setJobManagerFigure(final Image jobManagerFigure) {
        m_jobManagerFigure = jobManagerFigure;
        repaint();
    }

    /**
     * @return returns the instance of ProgressToolTipHelper set via
     *         <code>setProgressToolTipHelper(ProgressToolTipHelper)</code>
     */
    public ProgressToolTipHelper getProgressToolTipHelper() {
        return m_progressToolTipHelper;
    }

    /**
     * @param progressToolTipHelper an instance of ProgressToolTipHelper
     */
    public void setProgressToolTipHelper(final ProgressToolTipHelper progressToolTipHelper) {
        m_progressToolTipHelper = progressToolTipHelper;
    }

    /**
     * This drags out the tent stake to create a white space buffer in the canvas of the specified pixel height (so, if
     * the vertical scroll bar were to be moved to the zero location the user would see a whitespace buffer of the
     * specified pixel height before the start of the actual canvas at (0, 0)).
     *
     * @param pixelHeight the height in pixels of the white space buffer
     */
    public void placeTentStakeToAllowForWhitespaceBuffer(final int pixelHeight) {
        m_tentStakeFigure.setLocation(new Point(0, -pixelHeight));
    }


    /**
     * This figure is an invisible 1 x 1 figure which represents the stake we 'stretch the canvas with by continual
     * re-placement (not replacement) in the northwest corner of the workflow canvas.' We do this trivial subclassing
     * only to allow simple figure sussing when walking the children of the root figure.
     */
    private static class TentStakeFigure extends Figure {

        /**
         * Simply set the size of our figure to be 1 x 1.
         */
        public TentStakeFigure() {
            setSize(new Dimension(1, 1));
        }

    }
}
