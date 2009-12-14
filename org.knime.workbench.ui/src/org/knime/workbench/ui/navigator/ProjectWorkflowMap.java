/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.workbench.ui.navigator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.JobManagerChangedEvent;
import org.knime.core.node.workflow.JobManagerChangedListener;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * This class represents a link between projects (file system representation
 * of workflows by {@link IProject}s) and opened workflows (represented by a
 * {@link WorkflowManager}). The <code>WorkflowEditor</code> puts and removes
 * the name of the opened project together with the referring
 * {@link WorkflowManager} instance. The {@link KnimeResourceNavigator} uses
 * this information to display opened instances differently.
 *
 * @see KnimeResourceNavigator
 * @see KnimeResourceContentProvider
 * @see KnimeResourceLabelProvider
 * @see KnimeResourcePatternFilter
 *
 * @author Fabian Dill, University of Konstanz
 */
public final class ProjectWorkflowMap {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ProjectWorkflowMap.class);
    
    private ProjectWorkflowMap() {
        // Utility class
    }
    
    /**
     * A map which keeps track of the number of registered clients to the 
     * referring workflow. Registration is done by local path - since this is
     * the key used in the project workflow map and the workflow manager 
     * instance might be replaced. Registered clients in this map prevent the 
     * workflow from being removed from the {@link #PROJECTS} map with 
     * {@link #remove(IPath)}, only if there are no registered clients for this
     * workflow, {@link #remove(IPath)} will actually remove the workflow from 
     * {@link #PROJECTS}.
     * 
     */
    private static final Map<IPath, Set<Object>>WORKFLOW_CLIENTS 
        = new HashMap<IPath, Set<Object>>();

    /*
     * Map with name of workflow path and referring workflow manager
     * instance. Maintained by WorkflowEditor, used by KnimeResourceNavigator.
     * (This map contains only open workflows.)
     */
    private static final Map<IPath, NodeContainer> PROJECTS
        = new LinkedHashMap<IPath, NodeContainer>() {
        
        @Override
        public NodeContainer put(final IPath key, final NodeContainer value) {
            NodeContainer old = super.put(key, value);
            if (old != null) {
                LOGGER.debug("Removing \"" + key 
                        + "\" from project map");
            }
            if (value != null) {
                LOGGER.debug("Adding \"" + key 
                        + "\" to project map (" + size() + " in total)");
            }
            return old;
        };
        
        @Override
        public NodeContainer remove(final Object key) {
            NodeContainer old = super.remove(key);
            if (old != null) {
                LOGGER.debug("Removing \"" + key 
                        + "\" from project map (" + size() + " remaining)");
            }
            return old;
        };
    };
    
    /**
     * 
     * @param workflow the path to the workflow which is used by the client
     * @param client any object which uses the workflow
     *  
     * @see #unregisterClientFrom(IPath, Object)
     */
    public static final void registerClientTo(final IPath workflow, 
            final Object client) {
        Set<Object> callers = WORKFLOW_CLIENTS.get(workflow);
        if (callers == null) {            
            callers = new HashSet<Object>();
        }
        callers.add(client);
        WORKFLOW_CLIENTS.put(workflow, callers);
        LOGGER.debug("registering " + client + " to " + workflow
                + ". " + callers.size() + " registered clients now.");
    }
    
    /**
     * 
     * @param workflow path to the workflow which is not used anymore by this 
     * client (has no effect if the client was not yet registered for this 
     * workflow path)
     * @param client the client which has registered before with the 
     * {@link #registerClientTo(IPath, Object)} method
     * @see #registerClientTo(IPath, Object)
     */
    public static final void unregisterClientFrom(final IPath workflow, 
            final Object client) {
        if (workflow == null) {
            return;
        }
        if (!WORKFLOW_CLIENTS.containsKey(workflow)) {
            return;
        }
        Set<Object> callers = WORKFLOW_CLIENTS.get(workflow);
        callers.remove(client);
        if (callers.isEmpty()) {
            WORKFLOW_CLIENTS.remove(workflow);
        } else {            
            WORKFLOW_CLIENTS.put(workflow, callers); 
        }
        LOGGER.debug("unregistering " + client + " from " + workflow
                + ". " + callers.size() + " left.");
    }
    
    /*
     * All registered workflow listeners (KnimeResourceNavigator) which reflect
     * changes on opened workflows (display new nodes).
     */
    private static final Set<WorkflowListener>WF_LISTENERS
        = new LinkedHashSet<WorkflowListener>();

    /*
     * NodeStateChangeListeners (projects) to reflect states of projects
     * (idle, executing, executed). See KnimeResourceLabelProvider.
     */
    private static final Set<NodeStateChangeListener>NSC_LISTENERS
        = new LinkedHashSet<NodeStateChangeListener>();

    // forwards events to registered listeners
    private static final NodeStateChangeListener NSC_LISTENER
    = new NodeStateChangeListener() {

        @Override
        public void stateChanged(final NodeStateEvent state) {
            for (NodeStateChangeListener listener : NSC_LISTENERS) {
                listener.stateChanged(state);
            }
        }

    };

    private static final Set<JobManagerChangedListener> JOB_MGR_LISTENERS =
        new LinkedHashSet<JobManagerChangedListener>();

    // forwards events to registered listeners
    private static final JobManagerChangedListener JOB_MGR_LISTENER =
        new JobManagerChangedListener() {
        @Override
        public void jobManagerChanged(final JobManagerChangedEvent e) {
            for (JobManagerChangedListener l : JOB_MGR_LISTENERS) {
                l.jobManagerChanged(e);
            }
        }
    };

    private static final Set<NodeMessageListener> MSG_LISTENERS
        = new LinkedHashSet<NodeMessageListener>();

    private static final NodeMessageListener MSG_LISTENER
        = new NodeMessageListener() {

            @Override
            public void messageChanged(final NodeMessageEvent messageEvent) {
                for (NodeMessageListener l : MSG_LISTENERS) {
                    l.messageChanged(messageEvent);
                }
            }

    };

    private static final WorkflowListener WF_LISTENER = new WorkflowListener() {

        /**
         * Forwards events to registered listeners, if a meta node is added a
         * workflow listener is also added to the meta node in order to reflect
         * changes on the meta node's workflow.
         *
         * {@inheritDoc}
         */
        @Override
        public void workflowChanged(final WorkflowEvent event) {
            // add as listener
            if (event.getType().equals(WorkflowEvent.Type.NODE_ADDED)
                    && event.getNewValue() instanceof WorkflowManager) {
                WorkflowManager manager = (WorkflowManager)event.getNewValue();
                manager.addListener(WF_LISTENER);
                manager.addNodeStateChangeListener(NSC_LISTENER);
                for (NodeContainer cont : manager.getNodeContainers()) {
                    if (cont instanceof WorkflowManager) {
                        WorkflowManager wfm = (WorkflowManager)cont;
                        wfm.addListener(WF_LISTENER);
                        wfm.addNodeStateChangeListener(NSC_LISTENER);
                    }
                }
            }
            // inform registered listeners
            for (WorkflowListener listener : WF_LISTENERS) {
                listener.workflowChanged(event);
            }
            // unregister referring node
            if (event.getType().equals(WorkflowEvent.Type.NODE_REMOVED)
                    && event.getOldValue() instanceof WorkflowManager) {
                WorkflowManager wfm = (WorkflowManager)event.getOldValue();
                wfm.removeListener(WF_LISTENER);
                wfm.removeNodeStateChangeListener(NSC_LISTENER);
            }
        }

    };

    /**
     *
     * @param newPath the new path of the {@link IProject} after renaming
     * @param nc the {@link WorkflowManager} with a project new associated name
     * @param oldPath the old {@link IProject} path, under which the opened
     *  {@link WorkflowManager} is stored in the map
     */
    public static void replace(final IPath newPath,
            final WorkflowManager nc, final IPath oldPath) {
        if (oldPath != null) {
            PROJECTS.remove(oldPath);
        }
        putWorkflow(newPath, nc);
        WF_LISTENER.workflowChanged(new WorkflowEvent(
                WorkflowEvent.Type.NODE_ADDED, nc.getID(),
                null, nc));
        NSC_LISTENER.stateChanged(new NodeStateEvent(nc.getID(),
                nc.getState()));
    }

    /**
     * Removes the {@link WorkflowManager} from the map, typically when the
     *  referring editor is closed and the WorkflowEditor is disposed.
     * @param path path of the {@link IProject} under which the
     * {@link WorkflowManager} is stored in the map.
     */
    public static void remove(final IPath path) {
        WorkflowManager manager = (WorkflowManager)PROJECTS.get(path);
        // workflow is only in client map if there is at least one client
        if (manager != null && !WORKFLOW_CLIENTS.containsKey(path)) {
            PROJECTS.remove(path);
            WF_LISTENER.workflowChanged(new WorkflowEvent(
                    WorkflowEvent.Type.NODE_REMOVED, manager.getID(),
                    manager, null));
            manager.removeListener(WF_LISTENER);
            manager.removeNodeStateChangeListener(NSC_LISTENER);
            manager.removeNodeMessageListener(MSG_LISTENER);
            manager.removeJobManagerChangedListener(JOB_MGR_LISTENER);
        }
    }

    /**
     * Adds a {@link WorkflowManager} of an opened workflow with the path of
     * the referring {@link IProject} to the map. Used by the WorkflowEditor.
     *
     * @param path path of the referring {@link IProject}
     * @param manager open {@link WorkflowManager}
     */
    public static void putWorkflow(final IPath path,
            final WorkflowManager manager) {
        // in case the manager is replaced 
        // -> unregister listeners from the old one
        NodeContainer oldOne = PROJECTS.get(path);
        if (oldOne != null) {
            oldOne.removeNodeStateChangeListener(NSC_LISTENER);
            ((WorkflowManager)oldOne).removeListener(WF_LISTENER);
            oldOne.removeNodeMessageListener(MSG_LISTENER);
            oldOne.removeJobManagerChangedListener(JOB_MGR_LISTENER);
        }
        PROJECTS.put(path, manager);
        manager.addNodeStateChangeListener(NSC_LISTENER);
        manager.addListener(WF_LISTENER);
        manager.addNodeMessageListener(MSG_LISTENER);
        manager.addJobManagerChangedListener(JOB_MGR_LISTENER);
        WF_LISTENER.workflowChanged(new WorkflowEvent(
                WorkflowEvent.Type.NODE_ADDED, manager.getID(), null,
                manager));
    }

    /**
     * Returns the {@link WorkflowManager} instance which is registered under
     * the project file resource. Might be <code>null</code> if the 
     * {@link WorkflowManager} was not registered under this path or 
     * is closed.
     *
     * @see KnimeResourceContentProvider
     * @see KnimeResourceLabelProvider
     *
     * @param path project file resource (usually the directory)
     * @return the referring {@link WorkflowManager} or <code>null</code> if the
     * workflow manager is not registered under the passed name.
     */
    public static NodeContainer getWorkflow(final IPath path) {
        return PROJECTS.get(path);
    }

    /**
     * Finds the the project file resource based on the {@link NodeID}
     * of the referring workflow manager.
     * @param workflowID id of the {@link WorkflowManager} for which the name of
     * the project should be found
     * @return path of the {@link IProject}'s file resource
     */
    public static final IPath findProjectFor(final NodeID workflowID) {
        for (Map.Entry<IPath, NodeContainer> entry : PROJECTS.entrySet()) {
            if (entry.getValue().getID().equals(workflowID)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Adds a workflow listener, which gets informed on every workflow changed
     * event of meta nodes and projects.
     *
     * @param listener to be added
     */
    public static void addWorkflowListener(final WorkflowListener listener) {
        WF_LISTENERS.add(listener);
    }

    /**
     *
     * @param listener to be removed
     */
    public static void removeWorkflowListener(final WorkflowListener listener) {
        WF_LISTENERS.remove(listener);
    }

    /**
     *
     * @param listener listener to be informed about state changes of projects
     */
    public static void addStateListener(
            final NodeStateChangeListener listener) {
        NSC_LISTENERS.add(listener);
    }

    /**
     *
     * @param listener to be removed
     */
    public static void removeStateListener(
            final NodeStateChangeListener listener) {
        NSC_LISTENERS.remove(listener);
    }

    /**
     *
     * @param l listener to be informed about message changes
     */
    public static void addNodeMessageListener(final NodeMessageListener l) {
        MSG_LISTENERS.add(l);
    }

    /**
     *
     * @param l listener to be removed
     */
    public static void removeNodeMessageListener(final NodeMessageListener l) {
        MSG_LISTENERS.remove(l);
    }

    /**
     * @param l The listener to add.
     */
    public static void addJobManagerChangedListener(
            final JobManagerChangedListener l) {
        JOB_MGR_LISTENERS.add(l);
    }

    /**
     * @param l the job manager listener to remove
     */
    public static void removeJobManagerChangedListener(
            final JobManagerChangedListener l) {
        JOB_MGR_LISTENERS.remove(l);
    }

}
