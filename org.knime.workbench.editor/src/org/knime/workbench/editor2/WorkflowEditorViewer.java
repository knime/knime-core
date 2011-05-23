/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
  * KNIME.com, Zurich, Switzerland
  *
  * You may not modify, publish, transmit, transfer or sell, reproduce,
  * create derivative works from, distribute, perform, display, or in
  * any way exploit any of the content, in whole or in part, except as
  * otherwise expressly permitted in writing by the copyright owner or
  * as specified in the license file distributed with this product.
  *
  * If you have any questions please contact the copyright holder:
  * website: www.knime.com
  * email: contact@knime.com
  * ---------------------------------------------------------------------
  *
  * History
  *   May 20, 2011 (morent): created
  */

package org.knime.workbench.editor2;

import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;

/**
 * Extension of {@link ScrollingGraphicalViewer} that can store the state
 * of the SHIFT modifier key. This is necessary to allow dragging meta nodes
 * out of the editor.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class WorkflowEditorViewer extends ScrollingGraphicalViewer {
    private boolean m_shiftPressed = false;

    /**
     * @return true if the shift key is pressed, false otherwise
     */
    public boolean isShiftPressed() {
        return m_shiftPressed;
    }

    /**
     * @param shiftPressed true if the shift key is pressed, false otherwise
     */
    public void setShiftPressed(final boolean shiftPressed) {
        m_shiftPressed = shiftPressed;
    }


}
