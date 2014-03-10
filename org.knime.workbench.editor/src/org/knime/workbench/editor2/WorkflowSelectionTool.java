/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *   28.06.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;

/**
 *
 * @author sieb, University of Konstanz
 */
public class WorkflowSelectionTool extends SelectionTool {

    private static final int STATE_PREPAN = SelectionTool.MAX_STATE << 1;

    private static final int STATE_PANNING = STATE_PREPAN << 1;

    private static final int PAN_BUTTON = 3; // right mouse button dragging scrolls the viewport

    private int m_xLocation;

    private int m_yLocation;

    private boolean m_rightButton;

    private Point m_viewLocation;

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDown(final MouseEvent e, final EditPartViewer viewer) {

        // remember the location during mouse down
        m_xLocation = e.x;
        m_yLocation = e.y;
        if (e.button == 3) {
            m_rightButton = true;
        } else {
            m_rightButton = false;
        }
        super.mouseDown(e, viewer);
    }

    public boolean getRightButton() {
        return m_rightButton;
    }

    public int getXLocation() {
        return m_xLocation;
    }

    public int getYLocation() {
        return m_yLocation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleButtonDown(final int button) {
        if (button == PAN_BUTTON && getCurrentViewer().getControl() instanceof FigureCanvas
            && stateTransition(STATE_INITIAL, STATE_PREPAN)) {
            m_viewLocation = ((FigureCanvas)getCurrentViewer().getControl()).getViewport().getViewLocation();
            refreshCursor();
            return true;
        } else if (isInState(STATE_PREPAN | STATE_PANNING)) {
            // any other button leaves the panning state
            setState(STATE_INITIAL);
            refreshCursor();
            return true;
        }
        return super.handleButtonDown(button);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleButtonUp(final int button) {

        if (button == PAN_BUTTON && isInState(STATE_PREPAN | STATE_PANNING)) {
            // prevent the context menu from showing if pan button is the context trigger
            if (isInState(STATE_PANNING) && PAN_BUTTON == 3) {    
                getCurrentViewer().getContextMenu().getMenu().setVisible(false);
            }
            setState(STATE_INITIAL);
            refreshCursor();
            return true;
        }
        return super.handleButtonUp(button);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Cursor getDefaultCursor() {
        if (isInState(STATE_PANNING | STATE_PREPAN)) {
            return Cursors.HAND;
        }
        return super.getDefaultCursor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleDrag() {
        if ((stateTransition(STATE_PREPAN, STATE_PANNING) || isInState(STATE_PANNING))
            && getCurrentViewer().getControl() instanceof FigureCanvas) {
            FigureCanvas canvas = (FigureCanvas)getCurrentViewer().getControl();
            canvas.scrollTo(m_viewLocation.x - getDragMoveDelta().width, m_viewLocation.y - getDragMoveDelta().height);
            return true;
        } else {
            return super.handleDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleFocusLost() {
        if (isInState(STATE_PREPAN | STATE_PANNING)) {
            setState(STATE_INITIAL);
            refreshCursor();
            return true;
        }
        return super.handleFocusLost();
    }
}
