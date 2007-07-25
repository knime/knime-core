/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.07.2007 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class KnimeContentProvider extends WorkbenchContentProvider {

    private TreeViewer m_viewer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput,
            final Object newInput) {
        super.inputChanged(viewer, oldInput, newInput);
        m_viewer = (TreeViewer)viewer;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processDelta(final IResourceDelta delta) {
        super.processDelta(delta);
        // Workaround to remove the plus signs from the workflow projects
        // the update threads invoke the expandAll method on the viewer
        // this causes the plus signs to disappear
        // the first thread tries to remove the sign quickly
        // if this is too early the second thread removes them later
        new UpdateThread(30).start();
        new UpdateThread(500).start();
    }

    private class UpdateThread extends Thread {
        private long m_waitTime;

        public UpdateThread(final long waitTime) {
            m_waitTime = waitTime;
        }

        public void run() {
            try {
                Thread.sleep(m_waitTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    m_viewer.refresh();
                    m_viewer.expandAll();
                }
            });
        }
    }
}
