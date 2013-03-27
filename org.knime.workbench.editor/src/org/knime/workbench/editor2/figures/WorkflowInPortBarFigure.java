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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   20.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowInPortBarFigure extends AbstractWorkflowPortBarFigure {

    private final int m_maxXcord;

    /**
     * If no UI info is available. Bar places itself left of all components
     * @param maxXcoord the most left coordinate used by the workflow components
     */
    public WorkflowInPortBarFigure(final int maxXcoord) {
        m_maxXcord = maxXcoord;
        setInitialized(false);
    }

    /**
     * @param uiInfo from the UI info
     */
    public WorkflowInPortBarFigure(final Rectangle uiInfo) {
        m_maxXcord = 0; // not needed
        setBounds(uiInfo);
        setInitialized(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics graphics) {
        Rectangle parent = getParent().getBounds().getCopy();
        if (!isInitialized()) {
            int barWidth = WIDTH + AbstractPortFigure.WF_PORT_SIZE + OFFSET;
            int xLoc;
            if (barWidth + 10 >= m_maxXcord) {
                xLoc = m_maxXcord - 50 - WIDTH - AbstractPortFigure.WF_PORT_SIZE;
            } else {
                xLoc = OFFSET;
            }
            Rectangle newBounds =
                    new Rectangle(xLoc, OFFSET, WIDTH + AbstractPortFigure.WF_PORT_SIZE, parent.height - (2 * OFFSET));
            setInitialized(true);
            setBounds(newBounds);
        }
        super.paint(graphics);
    }

    @Override
    protected void fillShape(final Graphics graphics) {
        graphics.fillRectangle(getBounds().x, getBounds().y,
                getBounds().width - AbstractPortFigure.WF_PORT_SIZE,
                getBounds().height);
    }


    @Override
    protected void outlineShape(final Graphics graphics) {
        Rectangle r = getBounds().getCopy();
        r.width -= AbstractPortFigure.WF_PORT_SIZE;
        int x = r.x + lineWidth / 2;
        int y = r.y + lineWidth / 2;
        int w = r.width - Math.max(1, lineWidth);
        int h = r.height - Math.max(1, lineWidth);
        graphics.drawRectangle(x, y, w, h);
    }
}
