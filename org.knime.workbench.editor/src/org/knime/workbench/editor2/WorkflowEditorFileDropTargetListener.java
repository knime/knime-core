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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.base.node.io.table.read.ReadTableNodeFactory;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.workbench.editor2.commands.DropNodeCommand;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 *
 * @author ohl, University of Konstanz
 */
public class WorkflowEditorFileDropTargetListener implements
        TransferDropTargetListener {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditorFileDropTargetListener.class);

    private static HashMap<String, List<Class<? extends NodeFactory>>> DROP_RECEIVER =
            null;

    private final EditPartViewer m_viewer;

    /**
     *
     */
    public WorkflowEditorFileDropTargetListener(final EditPartViewer viewer) {
        if (DROP_RECEIVER == null) {
            DROP_RECEIVER =
                    new LinkedHashMap<String, List<Class<? extends NodeFactory>>>();

            String ext = ".csv";
            List<Class<? extends NodeFactory>> nodes =
                    new LinkedList<Class<? extends NodeFactory>>();
            nodes.add(FileReaderNodeFactory.class);
            DROP_RECEIVER.put(ext, nodes);

            ext = ".table";
            nodes = new LinkedList<Class<? extends NodeFactory>>();
            nodes.add(ReadTableNodeFactory.class);
            DROP_RECEIVER.put(ext, nodes);
        }
        m_viewer = viewer;
    }

    /**
     * {@inheritDoc}
     */
    public void dragOperationChanged(final DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    public void dragEnter(final DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    public void dragLeave(final DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    public void dragOver(final DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {

        if (!FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
            return;
        }
        if (!(event.data instanceof String[])) {
            return;
        }
        String[] filePaths = (String[])event.data;

        if (filePaths.length > 1) {
            LOGGER.warn("Can currently only drop one item at a time");
        }
        String file = filePaths[0];
        NodeFactory<NodeModel> nodeFact = null;

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
        NodeCreationContext ncc = new NodeCreationContext(url);
        WorkflowRootEditPart root =
                (WorkflowRootEditPart)m_viewer.getRootEditPart().getContents();
        Point dropLocation = getDropLocation(event);
        DropNodeCommand cmd =
                new DropNodeCommand(root.getWorkflowManager(), nodeFact, ncc,
                        dropLocation);
        m_viewer.getEditDomain().getCommandStack().execute(cmd);
        // NodeUsageRegistry.addNode(template);
        // bugfix: 1500
        m_viewer.getControl().setFocus();
    }

    /**
     *
     * @param event drop target event containing the position (relative to whole
     *            display)
     * @return point converted to the editor coordinates
     */
    protected Point getDropLocation(final DropTargetEvent event) {
        event.x = event.display.getCursorLocation().x;
        event.y = event.display.getCursorLocation().y;
        Point p =
                new Point(m_viewer.getControl().toControl(event.x, event.y).x,
                        m_viewer.getControl().toControl(event.x, event.y).y);
        LOGGER.debug("to control: " + p);
        // subtract this amount in order to have the node more or less centered
        // at the cursor location
        // more or less because the nodes are still of different width depending
        // on their name
        p.x -= 40;
        p.y -= 40;
        return p;
    }

    /**
     * Returns true if all paths have known extensions
     *
     * @param paths
     * @return
     */
    protected boolean hasKnownFileExtension(final String[] paths) {

        return true;
    }

    /**
     * Returns null, if no nodefactory is registered for that file extension.
     *
     * @param filename full file name
     * @return
     */
    private Class<? extends NodeFactory> getNodeClassForExtension(
            final String filename) {
        for (Map.Entry<String, List<Class<? extends NodeFactory>>> e : DROP_RECEIVER
                .entrySet()) {
            String ext = e.getKey();
            if (filename.endsWith(ext)) {
                List<Class<? extends NodeFactory>> nodes = e.getValue();
                return nodes.get(0); // just return the first node registered
            }
        }
        return null;
    }

    private NodeFactory getNodeForExtension(final String filename) {

        for (Map.Entry<String, List<Class<? extends NodeFactory>>> e : DROP_RECEIVER
                .entrySet()) {
            String ext = e.getKey();
            if (filename.endsWith(ext)) {
                List<Class<? extends NodeFactory>> nodes = e.getValue();
                if (nodes.size() > 1) {
                    LOGGER.warn("more than one registered receiver node. "
                            + "Picking first one.");
                }
                for (Class<? extends NodeFactory> c : nodes) {
                    try {
                        return c.newInstance();
                    } catch (Exception ex) {
                        LOGGER.warn("Can't create node " + c.getName()
                                + ". Trying next.", ex);
                    }
                }
                LOGGER.error("None of the receiver nodes could be "
                        + "instantiated. Can't process drop.");
                return null;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dropAccept(final DropTargetEvent event) {
        // this is a FileTransfer we want absolute file paths
        if (!(event.data instanceof String[])) {
            event.detail = DND.DROP_NONE;
        }
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
        return true;
    }

}
