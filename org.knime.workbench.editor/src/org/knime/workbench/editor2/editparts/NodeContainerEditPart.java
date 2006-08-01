/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodePort;
import org.knime.core.node.NodeStateListener;
import org.knime.core.node.NodeStatus;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.directnodeedit.NodeEditManager;
import org.knime.workbench.editor2.directnodeedit.UserNodeNameCellEditorLocator;
import org.knime.workbench.editor2.directnodeedit.UserNodeNameDirectEditPolicy;
import org.knime.workbench.editor2.editparts.policy.NodeContainerComponentEditPolicy;
import org.knime.workbench.editor2.editparts.policy.PortGraphicalRoleEditPolicy;
import org.knime.workbench.editor2.extrainfo.ModellingNodeExtraInfo;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 * Edit part for node containers. This also listens to interesting events, like
 * changed extra infos or execution states
 * 
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeContainerEditPart extends AbstractWorkflowEditPart implements
        NodeStateListener, MouseListener {
    /**
     * The time (in ms) within two clicks are treated as double click. TODO: get
     * the system double click time
     */
    // private static final long DOUBLE_CLICK_TIME = 400;
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodeContainerEditPart.class);

    private static final long DOUBLE_CLICK_TIME = 500;

    /**
     * Remembers the time of the last <code>MousePressed</code> event. This is
     * neccessary to implement manually the double click, as the
     * <code>DoubleClick</code> event is not processed by draw2D for some
     * reason.
     */
    // private long m_lastClick;
    /**
     * true, if the figure was initialized from the node extra info object.
     */
    private boolean m_figureInitialized;

    /**
     * this is set while executing. DeleteCommands mustn't be executed while
     * node is busy *
     */
    private boolean m_isLocked;

    /**
     * To implement manually the double click event.
     */
    private long m_lastClick;

    /**
     * The manager for the direct editing of the node name.
     */
    private NodeEditManager m_directEditManager;

    /**
     * @return The <code>NodeContainer</code>(= model)
     */
    public NodeContainer getNodeContainer() {
        return (NodeContainer)getModel();
    }

    /**
     * Returns the parent WFM.
     * 
     * @return The hosting WFM
     */
    public WorkflowManager getWorkflow() {
        return (WorkflowManager)getParent().getModel();
    }

    /**
     * @return Returns if this edit part is locked (=busy). Important for
     *         commands because e.g. the node mustn't be deleted while
     *         executing.
     */
    public boolean isLocked() {
        return m_isLocked;
    }

    /**
     * 
     * @see org.eclipse.gef.EditPart#activate()
     */
    @Override
    public void activate() {
        super.activate();

        // listen to node container (= model object)
        getNodeContainer().addListener(this);

        // If we already have extra info, init figure now
        // 
        // 
        if (getNodeContainer().getExtraInfo() != null) {
            initFigureFromExtraInfo((ModellingNodeExtraInfo)getNodeContainer()
                    .getExtraInfo());
            m_figureInitialized = true;
        } else {
            // set the initial settings to the figure on the next "stateChanged"
            // event.
            m_figureInitialized = false;
            // create default extra info and set it
            // NOTE: This is done for nodes that are created from code
            // e.g. cross validation node creates a partitioner and
            // has no knowledge about a extrainfo
            ModellingNodeExtraInfo info = new ModellingNodeExtraInfo();
            info.setNodeLocation(0, 0, -1, -1);
            
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // TODO: very bad hack for version 1.0.0!!!!
            // just to set the x-partitioner of the x-validation meta node
            // to a suitable position; this is done as the meta nodes
            // can not determine locations and set them for a node 
            // which is (like the x-partitioner) created from the code (not
            // from the user)
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if (getNodeContainer().getName().equals("X-Partitioner")) {
                info.setNodeLocation(120, 0, -1, -1);
            }

            getNodeContainer().setExtraInfo(info);

        }
    }

    /**
     * 
     * @see org.eclipse.gef.EditPart#deactivate()
     */
    @Override
    public void deactivate() {
        super.deactivate();

        getNodeContainer().removeListener(this);
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
     */
    @Override
    protected IFigure createFigure() {

        // create the visuals for the node container
        NodeContainerFigure nodeFigure = new NodeContainerFigure();

        // register a listener to open a nodes dialog when double clicked
        nodeFigure.addMouseListener(this);

        // init the user specified node name
        nodeFigure.setCustomName(getNodeContainer().getCustomName());

        return nodeFigure;
    }

    /**
     * Return the content pane for the model children (= ports).
     * 
     * @see org.eclipse.gef.GraphicalEditPart#getContentPane()
     */
    @Override
    public IFigure getContentPane() {
        return ((NodeContainerFigure)getFigure()).getContentFigure();

    }

    private void performDirectEdit() {

        if (m_directEditManager == null) {
            m_directEditManager = new NodeEditManager(this,
                    new UserNodeNameCellEditorLocator(
                            (NodeContainerFigure)getFigure()));
        }

        m_directEditManager.show();
    }

    /**
     * @see org.eclipse.gef.EditPart#performRequest(org.eclipse.gef.Request)
     */
    @Override
    public void performRequest(final Request request) {

        if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {

            performDirectEdit();
        }
    }

    /**
     * Installs the COMPONENT_ROLE for this edit part.
     * 
     * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
     */
    @Override
    protected void createEditPolicies() {

        // Handles the creation of DeleteCommands for nodes in a workflow
        this.installEditPolicy(EditPolicy.COMPONENT_ROLE,
                new NodeContainerComponentEditPolicy());

        // This policy provides create/reconnect commands for connections that
        // are associated with ports of this node
        this.installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
                new PortGraphicalRoleEditPolicy());

        // Installs the edit policy to directly edit the user node name
        // inside the node figture (by a CellEditor)
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE,
                new UserNodeNameDirectEditPolicy());
    }

    /**
     * Returns the model children (= the ports) of the
     * <code>NodeContainer</code> managed by this edit part. Note that in/out
     * ports are handled the same.
     * 
     * @see org.eclipse.gef.editparts.AbstractEditPart#getModelChildren()
     */
    @Override
    protected List getModelChildren() {
        ArrayList<NodePort> ports = new ArrayList<NodePort>();
        NodeContainer container = getNodeContainer();

        ports.addAll(container.getInPorts());

        ports.addAll(container.getOutPorts());

        return ports;
    }

    /**
     * Refreshes the visuals for this node representation.
     * 
     * @see org.eclipse.gef.editparts.AbstractEditPart#refreshVisuals()
     */
    @Override
    protected void refreshVisuals() {
        // TODO Auto-generated method stub
        super.refreshVisuals();
    }

    /**
     * Handles state changes for the underlying node.
     * 
     * @see org.knime.core.node.NodeStateListener#stateChanged(NodeStatus,
     *      int)
     */
    public void stateChanged(final NodeStatus state, final int id) {

        //
        // As this code updates the UI it must be executed in the UI thread.
        //
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                NodeContainer nodeContainer = getNodeContainer();
                NodeContainerFigure fig = (NodeContainerFigure)getFigure();

                if (state instanceof NodeStatus.ExtrainfoChanged) {
                    // case NodeContainer.EVENT_EXTRAINFO_CHANGED:
                    LOGGER.debug("ExtraInfo changed, "
                            + "updating bounds and visuals...");

                    WorkflowRootEditPart parent = null;
                    parent = (WorkflowRootEditPart)getParent();

                    // provide some info from the extra info object to the
                    // figure
                    ModellingNodeExtraInfo ei = null;
                    ei = (ModellingNodeExtraInfo)getNodeContainer()
                            .getExtraInfo();

                    //
                    // if not already initialized, do this now.
                    // Necessary 'cause the extra info is only available after
                    // the node
                    // has been added to the WFM via the CreateNodeCommand
                    // All of this data is static and should not change after
                    // creation
                    // (icon, type)
                    if (!m_figureInitialized) {
                        initFigureFromExtraInfo(ei);
                        m_figureInitialized = true;
                    }
                    // set the new constraints in the parent edit part
                    int[] bounds = ei.getBounds();
                    parent.setLayoutConstraint(NodeContainerEditPart.this,
                            getFigure(), new Rectangle(bounds[0], bounds[1],
                                    bounds[2], bounds[3]));

                    // check status of node
                    updateNodeStatus();
                } else if (state instanceof NodeStatus.Configured) {
                    if (getNodeContainer().isExecutableUpToHere()) {
                        fig.setState(NodeContainerFigure.STATE_READY, state
                                .getMessage());
                    } else {
                        fig.setState(NodeContainerFigure.STATE_NOT_CONFIGURED,
                                state.getMessage());
                    }
                } else if (state instanceof NodeStatus.Reset) {
                    if (getNodeContainer().isExecutableUpToHere()) {
                        fig.setState(NodeContainerFigure.STATE_READY, state
                                .getMessage());
                    } else {
                        fig.setState(NodeContainerFigure.STATE_NOT_CONFIGURED,
                                state.getMessage());
                    }
                } else if (state instanceof NodeStatus.StartExecute) {
                    fig.setState(NodeContainerFigure.STATE_EXECUTING, state
                            .getMessage());

                    // deactivate edit part and set locking flag
                    // NodeContainerEditPart.this.deactivateEditPolicies();
                    m_isLocked = true;
                } else if (state instanceof NodeStatus.EndExecute) {
                    if (nodeContainer.isExecuted()) {
                        fig.setState(NodeContainerFigure.STATE_EXECUTED, state
                                .getMessage());
                    } else {
                        if (getNodeContainer().isExecutableUpToHere()) {
                            fig.setState(NodeContainerFigure.STATE_READY, state
                                    .getMessage());
                        } else {
                            fig.setState(
                                    NodeContainerFigure.STATE_NOT_CONFIGURED,
                                    state.getMessage());
                        }
                    }

                    // re-activate edit part and clear locking flag
                    // NodeContainerEditPart.this.activateEditPolicies();
                    m_isLocked = false;
                } else if (state instanceof NodeStatus.Warning) {
                    fig.setState(NodeContainerFigure.STATE_WARNING, state
                            .getMessage());
                } else if (state instanceof NodeStatus.ExecutionCanceled) {
                    fig.setState(NodeContainerFigure.STATE_WARNING, state
                            .getMessage() != null ? state.getMessage()
                            : "Execution canceled");
                } else if (state instanceof NodeStatus.Error) {
                    fig.setState(NodeContainerFigure.STATE_ERROR, state
                            .getMessage());
                } else if (state instanceof NodeStatus.CustomName) {
                    fig.setCustomName(getNodeContainer().getCustomName());
                } else if (state instanceof NodeStatus.CustomDescription) {
                    fig.setCustomDescription(getNodeContainer()
                            .getDescription());
                }
                updateNodeStatus();

                // always refresh visuals
                refreshVisuals();

            }

        });

    }

    /**
     * Initializes the figure with data from the node extra info object. This
     * must be done only once, but after the node has been added to the WFM
     * (otherwise the extra info object is not available).
     * 
     * @param ei Extra info to provide to the figure
     */
    private void initFigureFromExtraInfo(final ModellingNodeExtraInfo ei) {

        LOGGER.debug("Initializing figure from NodeExtraInfo..");
        m_figureInitialized = true;

        NodeContainerFigure f = (NodeContainerFigure)getFigure();
        int[] b = ei.getBounds();
        f.setBounds(new Rectangle(b[0], b[1], b[2], b[3]));

        // String plugin = ei.getPluginID();
        // String iconPath = ei.getIconPath();
        NodeType type = getNodeContainer().getType();
        String name = getNodeContainer().getName();
        String userName = getNodeContainer().getCustomName();
        String description = getNodeContainer().getDescription();

        // get the icon
        Image icon = null;
        // URL iconURL = getNodeContainer().getIcon();
        // try {
        // icon = new Image(Workbench.getInstance().getDisplay(),
        // iconURL.openStream());
        // } catch (Exception e) {
        // try {
        // icon = new Image(Workbench.getInstance().getDisplay(),
        // NodeFactory.getDefaultIcon().openStream());
        // } catch (Exception innerE) {
        // // in this case the icon is null
        // icon = null;
        // }
        // }
        icon = ImageRepository.getScaledImage(getNodeContainer().getIcon(), 16,
                16);
        // get default image if null
        if (icon == null) {
            icon = ImageRepository.getScaledImage(NodeFactory.getDefaultIcon(),
                    16, 16);
        }
        if (icon != null) {
            f.setIcon(icon);
        }
        f.setType(type);
        f.setLabelText(name);
        f.setCustomName(userName);
        f.setCustomDescription(description);

        // TODO FIXME construct initial state here (after loading) - this should
        // be made nicer
        boolean isExecuted = getNodeContainer().isExecuted();
        if (isExecuted) {
            f.setState(NodeContainerFigure.STATE_EXECUTED, null);
        } else {
            boolean isExecutable = getNodeContainer().isExecutableUpToHere();
            if (isExecutable) {
                f.setState(NodeContainerFigure.STATE_READY, null);
            } else {
                f.setState(NodeContainerFigure.STATE_NOT_CONFIGURED, null);
            }
        }
        updateNodeStatus();
    }

    /**
     * Checks the status of the this node and if there is a message in the
     * <code>NodeStatus</code> object the messsage is set. Otherwise the
     * currently displayed message is removed.
     */
    private void updateNodeStatus() {
        NodeStatus status = getNodeContainer().getStatus();
        NodeContainerFigure containerFigure = (NodeContainerFigure)getFigure();

        if (status != null) {
            String message = status.getMessage();

            // if there is a message, set it, else remove the current message
            // if set
            if (message != null && !message.trim().equals("")) {

                int messageType;

                // message type tranlation for workbench
                if (status instanceof NodeStatus.Error) {
                    messageType = NodeContainerFigure.STATE_ERROR;
                } else {
                    messageType = NodeContainerFigure.STATE_WARNING;
                }
                containerFigure.setState(messageType, message);
            } else {
                containerFigure.removeMessages();
            }
        } else {
            containerFigure.removeMessages();
        }
    }

    /**
     * Marks this node parts figure. Used to hilite it from the rest of the
     * parts.
     * 
     * @see NodeContainerEditPart#unmark()
     */
    public void mark() {

        ((NodeContainerFigure)getFigure()).mark();
    }

    /**
     * Resets the marked part.
     * 
     * @see NodeContainerEditPart#mark()
     */
    public void unmark() {

        ((NodeContainerFigure)getFigure()).unmark();
    }

    /**
     * Implements a manual double click to open a nodes dialog. TODO: at the
     * moment every 4th pressed event is not submitted to this listener. Find
     * out why. Seems to be a draw2D problme.
     * 
     * @see org.eclipse.draw2d.MouseListener#
     *      mousePressed(org.eclipse.draw2d.MouseEvent)
     */
    public void mousePressed(final MouseEvent me) {

        // only left click matters
        if (me.button != 1) {
            return;
        }

        if (System.currentTimeMillis() - m_lastClick < DOUBLE_CLICK_TIME) {

            NodeContainer container = (NodeContainer)getModel();

            // if this node does not have a dialog
            if (!container.hasDialog()) {

                LOGGER.debug(container.getName()
                        + ": Opening node dialog after double "
                        + "click not possible");
                return;
            }

            LOGGER.debug(container.getName()
                    + ": Opening node dialog after double click...");

            //  
            // This is embedded in a special JFace wrapper dialog
            //
            try {
                WrappedNodeDialog dlg = new WrappedNodeDialog(Display
                        .getCurrent().getActiveShell(), container);
                dlg.open();
            } catch (NotConfigurableException ex) {
                MessageBox mb = new MessageBox(Display.getDefault()
                        .getActiveShell(), SWT.ICON_WARNING | SWT.OK);
                mb.setText("Dialog cannot be opened");
                mb.setMessage("The dialog cannot be opened for the following"
                        + " reason:\n" + ex.getMessage());
                mb.open();
            }

            // me.consume();
        }
        m_lastClick = System.currentTimeMillis();

    }

    /**
     * Does nothing.
     * 
     * @see org.eclipse.draw2d.MouseListener#
     *      mouseReleased(org.eclipse.draw2d.MouseEvent)
     */
    public void mouseReleased(final MouseEvent me) {
        // do nothing yet
    }

    /**
     * Does nothing.
     * 
     * @see org.eclipse.draw2d.MouseListener#
     *      mouseDoubleClicked(org.eclipse.draw2d.MouseEvent)
     */
    public void mouseDoubleClicked(final MouseEvent me) {

        // do nothing yet
    }

    /**
     * Overridden to return a custom <code>DragTracker</code> for
     * NodeContainerEditParts.
     * 
     * @see org.eclipse.gef.EditPart#getDragTracker(Request)
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        return new WorkflowSelectionDragEditPartsTracker(this);
    }

    // TODO: double click event can not be received (maybe due to draw2d bug
    // fix in later release
    // /**
    // * @see
    // org.eclipse.draw2d.MouseListener#
    // mousePressed(org.eclipse.draw2d.MouseEvent)
    // */
    // public synchronized void mousePressed(final MouseEvent me) {
    //
    // //me.consume();
    // // System.err.println("Var if");
    // // if (System.currentTimeMillis() - m_lastClick <=
    // Display.getCurrent().getDoubleClickTime()) {
    // // System.err.println("in if");
    // // doubleClicked(me);
    // //
    // // // m_lastClick = 0;
    // // }
    // //
    // // // set the time of the last click to the current time
    // // m_lastClick = System.currentTimeMillis();
    // // System.err.println("Ende mouse pressed");
    // }
    //
    // /**
    // * @see
    // org.eclipse.draw2d.MouseListener#
    // mouseReleased(org.eclipse.draw2d.MouseEvent)
    // */
    // public void mouseReleased(final MouseEvent me) {
    // // do nothing
    // }
    //
    // /**
    // * Opens this nodes dialog if double clicked. Unfortunately draw2D does
    // not
    // * process this event, so the double click event is checked manually in
    // the
    // * <code>mousePressed</code> method. The <code>mouseDoubleClicke</code>
    // * method is then invoked manually from the <code>mousePressed</code>
    // * method.
    // *
    // * @see
    // org.eclipse.draw2d.MouseListener#
    // mouseDoubleClicked(org.eclipse.draw2d.MouseEvent)
    // */
    // public void doubleClicked(final MouseEvent me) {
    // // open this nodes dialog
    // LOGGER.debug("Opening node dialog...");
    // NodeContainer container = getNodeContainer();
    //
    // //
    // // This is embedded in a special JFace wrapper dialog
    // //
    // WrappedNodeDialog dlg = new WrappedNodeDialog(Display.getCurrent()
    // .getActiveShell(), container);
    // dlg.open();
    //
    // }
    //
    // public void mouseDoubleClicked(final MouseEvent me) {
    // doubleClicked(me);
    // }

    // /**
    // * Creates a new <code>OpenDialogAction</code> action on the current
    // * active editor and instantly runs it.
    // *
    // * @see org.eclipse.draw2d.MouseListener
    // * #mouseDoubleClicked(org.eclipse.draw2d.MouseEvent)
    // */
    // public void mouseDoubleClicked(final MouseEvent me) {
    // // get the current active editor
    // WorkflowEditor editor = (WorkflowEditor) Workbench.getInstance()
    // .getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    // // ... and create a new action
    // OpenDialogAction action = new OpenDialogAction(editor);
    // action.run();
    //
    // }
}
