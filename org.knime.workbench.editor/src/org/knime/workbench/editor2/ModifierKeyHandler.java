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

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

/**
 * Notifies the {@link WorkflowEditorViewer} if the shift key has been pressed.
 * This is necessary to allow dragging meta nodes out of the editor.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class ModifierKeyHandler extends GraphicalViewerKeyHandler {

    /**
     * Constructs a key handler for the given viewer.
     * @param viewer the viewer
     */
    public ModifierKeyHandler(final GraphicalViewer viewer) {
        super(viewer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.keyCode == SWT.SHIFT) {
            ((WorkflowEditorViewer)getViewer())
                .registerTemplateDragSourceListener();
        }
        return super.keyPressed(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean keyReleased(final KeyEvent event) {
        if (event.keyCode == SWT.SHIFT) {
            ((WorkflowEditorViewer)getViewer())
                .unregisterTemplateDragSourceListener();
        }
        return super.keyReleased(event);
    }
}
