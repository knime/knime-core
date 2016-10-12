/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 11, 2016 (hornm): created
 */
package org.knime.core.api.node.workflow;

import org.w3c.dom.Element;

/**
 * TODO
 *
 * @author Martin Horn, University of Konstanz
 */
public interface ISingleNodeContainer {

//    /** Name of the sub-directory containing node-local files. These files
//     * manually copied by the user and the node will automatically list those
//     * files as node-local flow variables in its configuration dialog.
//     */
//    String DROP_DIR_NAME = "drop";
//
//    /**
//     * @return ...
//     */
//    ExecutionContext createExecutionContext();
//
//    /**
//     * Calls configure in the node, whereby it also updates the settings in case the node is driven by flow variables.
//     * It also allows the current job manager to modify the output specs according to its  settings
//     * (in case it modifies the node's output).
//     *
//     * <p>This method is KNIME private API and is called from the framework and the streaming executor (which is why
//     * it has public scope).
//     *
//     * @param inSpecs the input specs to node configure
//     * @param keepNodeMessage see {@link SingleNodeContainer#configure(PortObjectSpec[], boolean)}
//     * @return true of configure succeeded.
//     * @since 2.12
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    boolean callNodeConfigure(PortObjectSpec[] inSpecs, boolean keepNodeMessage);
//
//    /**
//     * Check if node can be safely reset.
//     *
//     * @return if node can be reset.
//     * @since 3.2
//     */
//    boolean isResetable();
//
//    /**
//     * Execute underlying Node asynchronously. Make sure to give Workflow-
//     * Manager a chance to call pre- and postExecuteNode() appropriately and
//     * synchronize those parts (since they changes states!).
//     *
//     * @param inObjects input data
//     * @return whether execution was successful.
//     * @throws IllegalStateException in case of illegal entry state.
//     */
//    NodeContainerExecutionStatus performExecuteNode(PortObject[] inObjects);
//
//    /** {@inheritDoc} */
//    boolean areDialogAndNodeSettingsEqual();
//
//    /** Delegates to node to get flow variables that are added or modified
//     * by the node.
//     * @return The list of outgoing flow variables.
//     * @see org.knime.core.node.Node#getOutgoingFlowObjectStack()
//     */
//    FlowObjectStack getOutgoingFlowObjectStack();

    /** Check if the given node is part of a scope (loop, try/catch...).
     *
     * @return true if node is part of a scope context.
     * @since 2.8
     */
    boolean isMemberOfScope();

//    /** Creates a copy of the stack held by the Node and modifies the copy
//     * by pushing all outgoing flow variables onto it. If the node represents
//     * a scope end node, it will also pop the corresponding scope context
//     * (and thereby all variables added in the scope's body).
//     *
//     * @return Such a (new!) stack. */
//    FlowObjectStack createOutFlowObjectStack();
//
//    /**
//     * @param nodeModelClass ...
//     * @return return true if underlying NodeModel (if it exists) implements the given class/interface
//     * @since 2.8
//     */
//    boolean isModelCompatibleTo(Class<?> nodeModelClass);

    /**
     * @return true if configure or execute were skipped because node is
     *   part of an inactive branch.
     * @see Node#isInactive()
     */
    boolean isInactive();

//    /** @return <code>true</code> if the underlying node is able to consume
//     * inactive objects (implements
//     * {@link org.knime.core.node.port.inactive.InactiveBranchConsumer}).
//     * @see Node#isInactiveBranchConsumer()
//     */
//    boolean isInactiveBranchConsumer();

    /**
     * @return the XML description of the node for the NodeDescription view
     */
    Element getXMLDescription();

//    /**
//     * Get the policy for the data outports, that is, keep the output in main
//     * memory or write it to disc. This method is used from within the
//     * ExecutionContext when the derived NodeModel is executing.
//     *
//     * @return The memory policy to use.
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    MemoryPolicy getOutDataMemoryPolicy();
//
//    /**
//     * @param portIndex ...
//     * @return ...
//     * @since 2.9
//     */
//    PortType getOutputType(int portIndex);
//
//    /**
//     * @param portIndex ...
//     * @return ...
//     * @since 2.9
//     */
//    PortObjectSpec getOutputSpec(int portIndex);
//
//    /**
//     * @param portIndex ...
//     * @return ...
//     * @since 2.9
//     */
//    PortObject getOutputObject(int portIndex);
//
//    /**
//     * @param portIndex ...
//     * @return ...
//     * @since 2.9
//     */
//    String getOutputObjectSummary(int portIndex);
//
//    /**
//     * @param portIndex ...
//     * @return ...
//     * @since 2.9
//     */
//    HiLiteHandler getOutputHiLiteHandler(int portIndex);

}