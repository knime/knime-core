/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 1, 2017 (Bjoern Lohrmann): created
 */
package org.knime.core.node.workflow.execresult;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.execresult.NodeExecutionResult.NodeExecutionResultBuilder;
import org.knime.core.node.workflow.execresult.WorkflowExecutionResult.WorkflowExecutionResultBuilder;

/**
 * Utility class to work with {@linke NodeContainerExecutionResult} objects, e.g. make a deep copy.
 *
 * @author Bjoern Lohrmann, KNIME.com GmbH
 * @since 3.5
 */
public class NodeContainerExecutionResultUtil {

    /**
     * Creates a deep copy of the given execution result, where all port objects and port object specs are inactive.
     *
     * @param result The execution result to copy.
     * @return An inactive copy of the given execution result.
     */
    public static NodeContainerExecutionResult
        convertToInactiveExecutionResult(final NodeContainerExecutionResult result) {

        final NodeContainerExecutionResult inactiveResult;

        if (result instanceof NativeNodeContainerExecutionResult) {
            inactiveResult = new NativeNodeContainerExecutionResult((NativeNodeContainerExecutionResult)result);
        } else if (result instanceof SubnodeContainerExecutionResult) {
            inactiveResult = new SubnodeContainerExecutionResult((SubnodeContainerExecutionResult)result);
        } else if (result instanceof WorkflowExecutionResult) {
            inactiveResult = new WorkflowExecutionResult((WorkflowExecutionResult)result);
        } else {
            throw new IllegalArgumentException(
                "Unsupported execution result " + result.getClass().getName() + ". This is a bug.");
        }

        setInactiveRecursively(inactiveResult);

        return inactiveResult;
    }

    private static void setInactiveRecursively(final NodeContainerExecutionResult result) {
        if (result instanceof NativeNodeContainerExecutionResult) {
            final NativeNodeContainerExecutionResult nativeResult = (NativeNodeContainerExecutionResult)result;
            nativeResult.getNodeExecutionResult().setInactive();
        } else if (result instanceof SubnodeContainerExecutionResult) {
            final SubnodeContainerExecutionResult subnodeResult = (SubnodeContainerExecutionResult)result;
            setInactiveRecursively(subnodeResult.getWorkflowExecutionResult());
        } else if (result instanceof WorkflowExecutionResult) {
            final WorkflowExecutionResult wfResult = (WorkflowExecutionResult)result;

            for (NodeContainerExecutionResult subResult : wfResult.getExecutionResultMap().values()) {
                setInactiveRecursively(subResult);
            }
        }
    }


    /**
     * Modifies the given subnode execution result, so that the execution result of the subnode's virtual output node
     * contains the given port objects. This method is called by some external executors to turn the results of a remote
     * subnode execution into something useful on the client, i.e. all ports are inactive except for the last output
     * node which contains the result data.
     *
     * @param subnode The subnode container itself, only required for sanity checking and to inquire the node ID of the
     *            virtual output node.
     * @param subnodeResult The subnode's execution result that shall be modified.
     * @param portObjects The port objects to set on the subnode's virtual output node.
     * @return slightly modified clone of <code>subnodeResult</code>
     */
    public static SubnodeContainerExecutionResult createNewWithModifiedOutputPortObjects(final SubNodeContainer subnode,
        final SubnodeContainerExecutionResult subnodeResult, final PortObject[] portObjects) {

        CheckUtils.checkArgumentNotNull(portObjects);
        CheckUtils.checkArgument(portObjects.length == subnode.getNrOutPorts() - 1,
            "Invalid output length (excl flow vars): " + "%d but expected %d", portObjects.length,
            subnode.getNrOutPorts() - 1);

        WorkflowExecutionResultBuilder workflowExecResultBuilder =
                WorkflowExecutionResult.builder(subnodeResult.getWorkflowExecutionResult());
        NativeNodeContainerExecutionResult outNodeExecResult = (NativeNodeContainerExecutionResult)subnodeResult
            .getWorkflowExecutionResult().getChildStatus(subnode.getVirtualOutNodeID().getIndex());
        final NodeExecutionResultBuilder nodeResult = NodeExecutionResult.builder(outNodeExecResult.getNodeExecutionResult());
        nodeResult.setInternalHeldPortObjects(portObjects);
        nodeResult.setPortObjects(new PortObject[]{FlowVariablePortObject.INSTANCE});
        nodeResult.setPortObjectSpecs(new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE});
        workflowExecResultBuilder.addNodeExecutionResult(subnode.getVirtualOutNodeID(),
            NativeNodeContainerExecutionResult.builder(outNodeExecResult).setNodeExecutionResult(nodeResult.build())
                .build());
        return SubnodeContainerExecutionResult.builder(subnodeResult)
            .setWorkflowExecutionResult(workflowExecResultBuilder.build()).build();
    }
}
