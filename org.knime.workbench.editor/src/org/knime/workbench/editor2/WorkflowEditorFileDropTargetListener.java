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
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
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

    private static Map<String, Class<? extends NodeFactory<NodeModel>>>
            EXTENSION_REGISTRY;

    private final EditPartViewer m_viewer;

    /**
     *
     */
    public WorkflowEditorFileDropTargetListener(final EditPartViewer viewer) {
        if (EXTENSION_REGISTRY == null) {
            initRegistry();
        }
        m_viewer = viewer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOperationChanged(final DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragEnter(final DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragLeave(final DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOver(final DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {
        if (!isEnabled(event)) {
            return;
        }

        TransferData transferType = event.currentDataType;

        final LocalSelectionTransfer transfer
                = LocalSelectionTransfer.getTransfer();
        if (transfer.isSupportedType(
                transferType)) {
            IStructuredSelection selection
                    = (IStructuredSelection)transfer.getSelection();
           // TODO implement
            LOGGER.error("File drop is not yet implemented from KNIME Explorer."
                    + selection.toString());

        } else if (FileTransfer.getInstance().isSupportedType(
                        transferType)) {
            String[] filePaths = (String[])event.data;
            if (filePaths.length > 1) {
                LOGGER.warn("Can currently only drop one item at a time");
            }
            String file = filePaths[0];
            Point dropLocation = getDropLocation(event);

            dropNode(file, dropLocation);
        }
    }

    private void dropNode(final String file, final Point dropLocation) {
        String extension = file.substring(file.lastIndexOf(".") + 1);
        Class<? extends NodeFactory<NodeModel>> clazz
                = EXTENSION_REGISTRY.get(extension);
        if (clazz == null) {
            LOGGER.warn("No node factory is registered for handling files "
                    + "of type \"" + extension + "\"");
            return;
        }
        NodeFactory<NodeModel> nodeFact;
        try {
            nodeFact = clazz.newInstance();
        } catch (InstantiationException e) {
            LOGGER.error("Can't create node " + clazz.getName() + ".", e);
            return;
        } catch (IllegalAccessException e) {
            LOGGER.error(e);
            return;
        }

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
        WorkflowRootEditPart root = (WorkflowRootEditPart)
                m_viewer.getRootEditPart().getContents();
        DropNodeCommand cmd = new DropNodeCommand(root.getWorkflowManager(),
                nodeFact, ncc, dropLocation);
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
        TransferData transferType = event.currentDataType;
        if (!LocalSelectionTransfer.getTransfer().isSupportedType(
                transferType) && !FileTransfer.getInstance().isSupportedType(
                        transferType)) {
            LOGGER.info("Only LocalSelectionTransfer can be dropped. Got "
                    + transferType + ".");
            return false;
        }
        return true;
    }

    private static void initRegistry() {
        EXTENSION_REGISTRY = new TreeMap<String,
                Class<? extends NodeFactory<NodeModel>>>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        for (IConfigurationElement element : registry
                .getConfigurationElementsFor(
                        "org.knime.workbench.editor.editor_filedrop")) {
            try {
                /*
                 * Use the configuration element method to load an object of the
                 * given class name. This method ensures the correct classpath
                 * is used providing access to all extension points.
                 */
                final Object o =
                        element.createExecutableExtension("NodeFactory");
                @SuppressWarnings("unchecked")
                Class<? extends NodeFactory<NodeModel>> clazz =
                        (Class<? extends NodeFactory<NodeModel>>)o.getClass();

                for (IConfigurationElement child : element.getChildren()) {
                    String extension = child.getAttribute("extension");
                    if (EXTENSION_REGISTRY.get(extension) == null) {
                        // add class
                        EXTENSION_REGISTRY.put(extension, clazz);
                    } // else already registered -> first come first serve
                }
            } catch (InvalidRegistryObjectException e) {
                throw new IllegalArgumentException(e);
            } catch (CoreException e) {
                throw new IllegalArgumentException(e);
            }
        }
        for (String key : EXTENSION_REGISTRY.keySet()) {
            LOGGER.debug("File extension: \"" + key + "\" registered for "
                    + "Node Factory: "
                    + EXTENSION_REGISTRY.get(key).getSimpleName() + ".");
        }
    }
}
