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
 */
package org.knime.workbench.editor2;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author ohl, University of Konstanz
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class WorkflowEditorFileDropTargetListener
        extends WorkflowEditorDropTargetListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditorFileDropTargetListener.class);

    /**
     * @param viewer the edit part viewer this drop target listener is attached
     *      to
     */
    public WorkflowEditorFileDropTargetListener(final EditPartViewer viewer) {
        super(viewer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {
        String file = getFile(event);
        // Set the factory on the current request
        URL url;
        try {
            url = new URI("file", file, null).toURL();
        } catch (MalformedURLException e1) {
            LOGGER.error("Unable to create URI from file location (" + file
                    + ")");
            return;
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to create URI from file location (" + file
                    + ")");
            return;
        }
        Point dropLocation = getDropLocation(event);

        dropNode(url, dropLocation);
    }
    /**
     * @param event
     * @return
     */
    private String getFile(final DropTargetEvent event) {
        String[] filePaths = (String[])event.data;
        if (filePaths.length > 1) {
            LOGGER.warn("Can currently only drop one item at a time");
        }
        return filePaths[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transfer getTransfer() {
        return FileTransfer.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        event.feedback = DND.FEEDBACK_SELECT;
        event.operations = DND.DROP_COPY;
        event.detail = DND.DROP_COPY;
        return true;
    }


}
