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
 *   May 4, 2011 (morent): created
 */

package org.knime.workbench.editor2;

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
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.workbench.editor2.commands.DropNodeCommand;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class WorkflowEditorDropTargetListener implements
        TransferDropTargetListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditorFileDropTargetListener.class);

    private static Map<String, Class<? extends NodeFactory<NodeModel>>>
            extensionRegistry;

    private final EditPartViewer m_viewer;

    /**
     * @param viewer the edit part viewer this drop target listener is attached
     *            to
     */
    protected WorkflowEditorDropTargetListener(final EditPartViewer viewer) {
        m_viewer = viewer;
    }


   private static void initRegistry() {
       extensionRegistry = new TreeMap<String,
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
                   if (extensionRegistry.get(extension) == null) {
                       // add class
                       extensionRegistry.put(extension, clazz);
                   } // else already registered -> first come first serve
               }
           } catch (InvalidRegistryObjectException e) {
               throw new IllegalArgumentException(e);
           } catch (CoreException e) {
               throw new IllegalArgumentException(e);
           }
       }
       for (String key : extensionRegistry.keySet()) {
           LOGGER.debug("File extension: \"" + key + "\" registered for "
                   + "Node Factory: "
                   + extensionRegistry.get(key).getSimpleName() + ".");
       }
   }

    /**
     * @param extension the file extension
     * @return the node factory registered for this extension, or null if
     *      the extension is not registered.
     */
    protected static Class<? extends NodeFactory<NodeModel>> getNodeFactory(
            final String extension) {
        if (extensionRegistry == null) {
            initRegistry();
        }
        return extensionRegistry.get(extension);
    }

    /**
     * Drops a node for a URL to the drop location.
     * @param url the URL of the resource to drop a node for
     * @param dropLocation the location to drop the node
     */
    protected final void dropNode(final URL url, final Point dropLocation) {
        String path = url.getPath();
        String extension = path.substring(path.lastIndexOf(".") + 1);
        Class<? extends NodeFactory<NodeModel>> clazz
                = WorkflowEditorDropTargetListener.getNodeFactory(extension);
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
   public void dragOperationChanged(final DropTargetEvent event) {
       // do nothing for now
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void dragEnter(final DropTargetEvent event) {
       LOGGER.debug("dragEnter with from source " + event.getSource());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void dragLeave(final DropTargetEvent event) {
       // do nothing for now
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void dragOver(final DropTargetEvent event) {
       // do nothing for now
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEnabled(final DropTargetEvent event) {
       return true;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void dropAccept(final DropTargetEvent event) {
       //TODO think about operation restrictions later. For now everything is ok
//       if (DND.DROP_LINK != event.operations) {
//           event.detail = DND.DROP_NONE;
//           LOGGER.warn("Only linking operation is allowed.");
//       }
   }

}
