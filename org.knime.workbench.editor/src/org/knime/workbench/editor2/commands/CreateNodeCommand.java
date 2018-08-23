/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import static org.knime.core.ui.wrapper.Wrapper.wraps;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformation.Builder;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.OperationNotAllowedException;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.SWTUtilities;
import org.knime.workbench.ui.async.AsyncUtil;

/**
 * GEF command for adding a <code>Node</code> to the
 * <code>WorkflowManager</code>.
 *
 * @author Florian Georg, University of Konstanz
 */
public class CreateNodeCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateNodeCommand.class);

    private final NodeFactory<? extends NodeModel> m_factory;

    /**
     * Relative location of the new node. Maybe <code>null</code> if the host manager is _not_ of type
     * {@link WorkflowManager}.
     *
     * @since 2.12
     */
    @Deprecated
    protected final Point m_location;

    /**
     * Absolute location of the new node. Maybe <code>null</code> if the host manager is of type
     * {@link WorkflowManager}.
     *
     * @since 3.7
     */
    protected final Point m_absoluteLocation;

    /**
     * Snap node to grid.
     * @since 2.12
     */
    protected final boolean m_snapToGrid;

    /**
     * Container of the new node.
     * @since 2.12
     */
    protected NodeContainerUI m_container;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param relativeLocation Initial visual location in the workflow (i.e. relative to the visible workflow part not
     *            taking the scrolling into account)
     * @param snapToGrid snap new node to grid
     * @deprecated use {@link #CreateNodeCommand(WorkflowManagerUI, NodeFactory, Point, boolean)} instead with absolute
     *             coordinates
     */
    @Deprecated
    public CreateNodeCommand(final WorkflowManager manager, final NodeFactory<? extends NodeModel> factory,
        final Point relativeLocation, final boolean snapToGrid) {
        super(manager);
        m_factory = factory;
        m_location = relativeLocation;
        m_absoluteLocation = null;
        m_snapToGrid = snapToGrid;
    }

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param absoluteLocation the absolute coordinates
     * @param snapToGrid snap new node to grid
     */
    public CreateNodeCommand(final WorkflowManagerUI manager, final NodeFactory<? extends NodeModel> factory,
        final Point absoluteLocation, final boolean snapToGrid) {
        super(manager);
        m_factory = factory;
        m_location = null;
        m_absoluteLocation = absoluteLocation;
        m_snapToGrid = snapToGrid;
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return m_factory != null && (m_location != null || m_absoluteLocation != null) && super.canExecute();
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        WorkflowManagerUI hostWFM = getHostWFMUI();

        // create extra info
        Builder infoBuilder = NodeUIInformation.builder()
                .setSnapToGrid(m_snapToGrid)
                .setIsDropLocation(true);
        if(m_location != null) {
            //relative coords
            infoBuilder.setHasAbsoluteCoordinates(false)
                .setNodeLocation(m_location.x, m_location.y, -1, -1);
        } else {
            assert m_absoluteLocation != null;
            infoBuilder.setHasAbsoluteCoordinates(true)
                .setNodeLocation(m_absoluteLocation.x, m_absoluteLocation.y, -1, -1);
        }

        // Add node to workflow and get the container
        try {
            NodeID id = AsyncUtil.wfmAsyncSwitch(wfm -> {
                return wfm.createAndAddNode(m_factory, infoBuilder.build());
            }, wfm -> {
                return wfm.createAndAddNodeAsync(m_factory, infoBuilder.build());
            }, hostWFM, "Adding new node ...");
            m_container = hostWFM.getNodeContainer(id);
            if (wraps(m_container, NodeContainer.class)) {
                NodeTimer.GLOBAL_TIMER.addNodeCreation(Wrapper.unwrapNC(m_container));
            }
        } catch (Throwable t) {
            // if fails notify the user
            LOGGER.debug("Node cannot be created.", t);
            MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be created.");
            mb.setMessage("The selected node could not be created "
                    + "due to the following reason:\n" + t.getMessage());
            mb.open();
            return;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_container != null
            && getHostWFMUI().canRemoveNode(m_container.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Removing node #" + m_container.getID());
        if (canUndo()) {
            getHostWFMUI().remove(new NodeID[]{m_container.getID()}, null, null);
            try {
                AsyncUtil.wfmAsyncSwitchRethrow(wfm -> {
                    wfm.remove(new NodeID[]{m_container.getID()}, null, null);
                    return null;
                }, wfm -> {
                    return wfm.removeAsync(new NodeID[]{m_container.getID()}, null, null);
                }, getHostWFMUI(), "Removing node ...");
            } catch (OperationNotAllowedException e) {
                //should never happen
            }
        } else {
            MessageDialog.openInformation(SWTUtilities.getActiveShell(),
                    "Operation no allowed", "The node "
                    + m_container.getNameWithID()
                    + " can currently not be removed");
        }
    }
}
