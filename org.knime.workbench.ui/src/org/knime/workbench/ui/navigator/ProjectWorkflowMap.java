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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
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

    private ProjectWorkflowMap() {
        // Utility class
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ProjectWorkflowMap.class);

    /*
     * Map with name of IProject to lowercase and referring workflow manager
     * instance. Maintained by WorkflowEditor, used by KnimeResourceNavigator.
     */
    private static final Map<String, NodeContainer>PROJECTS
        = new LinkedHashMap<String, NodeContainer>();

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
     * @param newName the new name of the {@link IProject} after a rename
     *  operation
     * @param nc the {@link WorkflowManager} with a project new associated name
     * @param oldName the old {@link IProject} name, under which the opened
     *  {@link WorkflowManager} is stored in the map
     */
    public static void replace(final String newName,
            final WorkflowManager nc, final String oldName) {
        if (oldName != null) {
            PROJECTS.remove(oldName);
        }
        putWorkflow(newName, nc);
        WF_LISTENER.workflowChanged(new WorkflowEvent(
                WorkflowEvent.Type.NODE_ADDED, nc.getID(),
                oldName, nc));
        NSC_LISTENER.stateChanged(new NodeStateEvent(nc.getID(),
                nc.getState()));
    }

    /**
     * Removes the {@link WorkflowManager} from the map, typically when the
     *  referring editor is closed and the WorkflowEditor is disposed.
     * @param name nameof the {@link IProject} under which the
     * {@link WorkflowManager} is stored in the map.
     */
    public static void remove(final String name) {
        WorkflowManager manager = (WorkflowManager)PROJECTS.get(name);
        if (manager != null) {
            PROJECTS.remove(name);
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
     * Adds a {@link WorkflowManager} of an opened workflow with the name of
     * the referring {@link IProject} name to the map. Used by the
     * WorkflowEditor.
     *
     * @param name name of the referring {@link IProject}
     * @param manager open {@link WorkflowManager}
     */
    public static void putWorkflow(final String name,
            final WorkflowManager manager) {
        LOGGER.debug("putting " + name + " onto map");
        PROJECTS.put(name, manager);
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
     * the name of the project file resource (usually the directory name).
     * Might be <code>null</code> if the {@link WorkflowManager} was not
     * registered under this name or is already closed.
     *
     * @see KnimeResourceContentProvider
     * @see KnimeResourceLabelProvider
     *
     * @param projectName name of the project file resource (usually the
     *  directory name)
     * @return the referring {@link WorkflowManager} or <code>null</code> if the
     * workflow manager is not registered under the passed name.
     */
    public static NodeContainer getWorkflow(final String projectName) {
        return PROJECTS.get(projectName);
    }

    /**
     * Finds the name of the project file resource based on the {@link NodeID}
     * of the referring workflow manager.
     * @param workflowID id of the {@link WorkflowManager} for which the name of
     * the project should be found
     * @return name of the {@link IProject}'s file resource
     */
    public static final String findProjectFor(final NodeID workflowID) {
        for (Map.Entry<String, NodeContainer> entry : PROJECTS.entrySet()) {
            if (entry.getValue().getID().equals(workflowID)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Clears the mapping.
     */
    public static void clearMap() {
        PROJECTS.clear();
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
     * w@param l
     */
    public static void removeJobManagerChangedListener(
            final JobManagerChangedListener l) {
        JOB_MGR_LISTENERS.remove(l);
    }

}
