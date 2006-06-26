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
 */
package de.unikn.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.ui.IEditorInput;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.meta.MetaInputModel;
import de.unikn.knime.core.node.meta.MetaOutputModel;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.workbench.editor2.extrainfo.ModellingNodeExtraInfo;
import de.unikn.knime.workbench.repository.model.NodeTemplate;

/**
 * This is the implementation of the Eclipse Editor used for editing a
 * <code>WorkflowManager</code> object in the context of a meta-node.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class MetaWorkflowEditor extends WorkflowEditor {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MetaWorkflowEditor.class);

    /**
     * The key used to save meta-workflow settings.
     */
    public static final String SETTING_ID = "meta-workflow";

    /**
     * The <code>NodeContainer</code> representing this meta-workflow.
     */
    private NodeContainer m_metaNodeContainer;

    /**
     * No arg constructor, creates the edit domain for this editor.
     */
    public MetaWorkflowEditor() {
        super();

        LOGGER.debug("Creating MetaWorkflowEditor...");
    }

    /**
     * Sets the editor input, that is the underlying <code>NodeContainer</code>
     * representing this meta-workflow.
     * 
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(final IEditorInput input) {
        LOGGER.debug("Setting input into editor...");

        setDefaultInput(input);
        String name = null;

        // else it is an settings object representing a meta-workflow

        // get the node container from the editor input wrapper
        m_metaNodeContainer = ((MetaWorkflowEditorInput)input)
                .getNodeContainer();

        name = "Meta-workflow";
        
        setWorkflowManager(m_metaNodeContainer.getEmbeddedWorkflowManager());

        setUpMetaPortNodeExtrainfos();

        // Editor name (title)
        setPartName(name);

        // update Actions, as now there's everything available
        // done in superclass
        updateActions();
    }

    /**
     * Checks wether the extrainfo for the input and output meta nodes has been
     * already set and if not it arranges the nodes.
     */
    private void setUpMetaPortNodeExtrainfos() {
        Collection<NodeContainer> nodes = getWorkflowManager().getNodes();
        List<NodeContainer> inputContainer = new ArrayList<NodeContainer>();
        List<NodeContainer> outputContainer = new ArrayList<NodeContainer>();

        for (NodeContainer nc : nodes) {
            Class<? extends NodeModel> clazz = nc.getModelClass();
            if (MetaInputModel.class.isAssignableFrom(clazz)) {
                inputContainer.add(nc);
            } else if (MetaOutputModel.class.isAssignableFrom(clazz)) {
                outputContainer.add(nc);
            }
        }

        // the assumed height and width of the editor window
        // TODO: make it dynamic corresponding to the current editor
        int height = 300;
        int width = 400;

        // set the position of the meta input nodes
        // the y position is a running variable to arrange the input nodes
        if (inputContainer.size() > 0) {
            int stepSize = height / inputContainer.size();
            int yPos = 0;
            for (NodeContainer container : inputContainer) {
                setUpSingleInfo(container, yPos, 0);
                yPos += stepSize;
            }
        }

        // set the position of the meta output nodes
        // the y position is a running variable to arrange the output nodes
        if (outputContainer.size() > 0) {
            int stepSize = height / outputContainer.size();
            int yPos = 0;
            for (NodeContainer container : outputContainer) {
                setUpSingleInfo(container, yPos, width - 40);
                yPos += stepSize;
            }
        }
    }

    /**
     * Sets the extra info of a given node container. The node is initialized as
     * a meta node port.
     * 
     * @param container the container to set the extra info for
     * @param yPos the y position to set the node in the editor
     * @param xPos the x position to set the node in the editor
     * 
     */
    private void setUpSingleInfo(final NodeContainer container,
            final int yPos, final int xPos) {
        // check if the extraninfo has been set already
        if (container.getExtraInfo() != null
                && container.getExtraInfo().isFilledProperly()) {
            return;
        }

        ModellingNodeExtraInfo info = new ModellingNodeExtraInfo();
        info.setNodeLocation(xPos, yPos, 40, 40);
        info.setType(NodeTemplate.TYPE_META);
        container.setExtraInfo(info);
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * Checks if the given container is the same object as the underlying
     * container of this meta workflow editor.
     * 
     * @param container the container to compare
     * 
     * @return true if the given container is the underlying container of this
     *         meta workflow
     */
    public boolean representsNodeContainer(final NodeContainer container) {
        return m_metaNodeContainer == container;
    }
}
