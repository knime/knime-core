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
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;

/**
 * Extension of {@link ScrollingGraphicalViewer} that will (un)register a
 * {@link WorkflowEditorTemplateDragSourceListener} upon request (shift key
 * down).
 *
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class WorkflowEditorViewer extends ScrollingGraphicalViewer {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(WorkflowEditorViewer.class);

    /** The listener, registered on demand. See Bug 2844 for details and
     * also WorkflowGraphicalViewer#createViewer. */
    private WorkflowEditorTemplateDragSourceListener
        m_templateDragSourceListener;

    private boolean m_isRegistered;

    /** Activate listener (register it). */
    public synchronized void registerTemplateDragSourceListener() {
        if (!m_isRegistered) {
            if (m_templateDragSourceListener == null) {
                m_templateDragSourceListener =
                    new WorkflowEditorTemplateDragSourceListener(this);
            }
            LOGGER.debug("Registering workflow template listener");
            addDragSourceListener(m_templateDragSourceListener);
            m_isRegistered = true;
        }
    }

    /** Inactivate listener (unregister it). */
    public void unregisterTemplateDragSourceListener() {
        // delay removal as a drag operation may be in progress
        Display.getDefault().asyncExec(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                if (m_isRegistered) {
                    m_isRegistered = false;
                    LOGGER.debug("Unregistering workflow template listener");
                    removeDragSourceListener(
                            (org.eclipse.jface.util.TransferDragSourceListener)
                            m_templateDragSourceListener);
                }
            }
        });
    }

}
