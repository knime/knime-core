/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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

/**
 *
 * @author sieb, University of Konstanz
 */
public class WorkflowSelectionTool extends SelectionTool {

    /** button for panning the editor contents with the mouse. */
    public static final int PAN_BUTTON = 2; // middle mouse button dragging scrolls the viewport

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
        if (button == PAN_BUTTON && isInState(STATE_INITIAL)) {
            if (getCurrentViewer().getControl() instanceof FigureCanvas) {
                m_viewLocation = ((FigureCanvas)getCurrentViewer().getControl()).getViewport().getViewLocation();
            }
        }
        return super.handleButtonDown(button);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleButtonUp(final int button) {
        boolean result = super.handleButtonUp(button);
        refreshCursor();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleDrag() {
        if (getCurrentInput().isMouseButtonDown(PAN_BUTTON)
                && getCurrentViewer().getControl() instanceof FigureCanvas) {
            boolean pan = false;
            if (stateTransition(STATE_DRAG, STATE_DRAG_IN_PROGRESS)) {
                setCursor(Cursors.HAND);
                pan = true;
            } else if (isInState(STATE_DRAG_IN_PROGRESS)) {
                pan = true;
            }
            if (pan) {
                FigureCanvas canvas = (FigureCanvas)getCurrentViewer().getControl();
                canvas.scrollTo(m_viewLocation.x - getDragMoveDelta().width, m_viewLocation.y
                    - getDragMoveDelta().height);
                return true;
            }
        }
        return super.handleDrag();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleFocusLost() {
        setState(STATE_INITIAL);
        return super.handleFocusLost();
    }

}
