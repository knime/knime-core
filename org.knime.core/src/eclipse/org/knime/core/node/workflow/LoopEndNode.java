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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 16, 2008 (berthold): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.knime.core.node.Node;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.CheckUtils;

/**
 * Complement to @see{LoopStartNode}.
 *
 * @author M. Berthold, University of Konstanz
 */
public interface LoopEndNode extends ScopeEndNode<FlowLoopContext> {

    /**
     * {@inheritDoc}
     */
    @Override
    default Class<FlowLoopContext> getFlowScopeContextClass() {
        return FlowLoopContext.class;
    }

    /**
     * Specifies whether a loop (start and end) should do special treatment of modified variables. This includes:
     * <ul>
     * <li>loop start node to propagate modifications of variables in subsequent loop iterations</li>
     * <li>loop end node to propagate modifications of 'out-of-scope' variables to its output</li>
     * </ul>
     * This property was added in 4.4 (previously this was not possible, i.e. <code>false</code>) and is added
     * individually to all loop end node implementations, hence this default implementation returns <code>false</code>.
     *
     * @return This property (default is <code>false</code>)
     * @since 4.4
     */
    default boolean shouldPropagateModifiedVariables() {
        return false;
    }

    /**
     * Framework code that propagates overwritten variables at a loop end node to its outputs. Any variable that is
     * assigned within the loop (= on top of the loop context on the node's input stack) it checks if the variables was
     * declared outside the loop (= prior the loop context) and publish the modified value. Variables defined by
     * the start node are explicitly excluded (flow control variables causing trouble with nested loops).
     *
     * @param endModel The end node, not null
     *
     * @since 4.4
     */
    static void propagateModifiedVariables(final NodeModel endModel) {
        FlowObjectStack outgoingStackEndNode = Node.invokeGetOutgoingFlowObjectStack(endModel);
        FlowObjectStack incomingStackEndNode = Node.invokeGetFlowObjectStack(endModel);
        Set<String> propagatedVarsSet = incomingStackEndNode //
            .peekOptional(FlowLoopContext.class) //
            .map(FlowLoopContext::getPropagatedVarsNames) //
            .orElseThrow(() -> new IllegalFlowObjectStackException(String.format(
                "Expected to have seen \"%s\" on flow variable stack -- was method called from NodeModel#execute(...)?",
                FlowLoopContext.class.getSimpleName())));
        Set<String> outsideLoopVarsSet = new HashSet<>();
        Iterator<FlowObject> it = incomingStackEndNode.iterator();

        /* this node's input stack looks like so...
         * (lines with indent are special loop controls that the code below searches for)
         * #iterator goes top to bottom
         * ---0:6---
         * "external-sum" (INTEGER: 55)
         * "internal-sum" (INTEGER: 10)
         * "external-sum-injected" (INTEGER: 10)
         * "internal-sum" (INTEGER: 0)
         * "number" (INTEGER: 10)
         * "RowID" (STRING: Iteration10)
         * "currentIteration" (INTEGER: 9)
         * "maxIterations" (INTEGER: 10)
         * "external-sum" (INTEGER: 45)
         *          <Inner Loop Context (#execute) - Owner: 0:2>
         * "number" (INTEGER: 0)
         * "RowID" (STRING: )
         * "currentIteration" (INTEGER: 0)
         * "maxIterations" (INTEGER: 0)
         *          <Loop Context (Head 0:2, Tail 0:6)> - iteration 9
         * "external-sum-injected" (INTEGER: 0)
         * "knime.workspace" (STRING: /data/dev/eclipse/runtime-KNIME)
         * "external-sum" (INTEGER: 0)
         * --------
         */

        Deque<FlowVariable> loopVarsDeque = collectLoopVariables(propagatedVarsSet, it); // consumes it to, incl., 'Loop Context'

        // cache all variables defined outside the loop (strictly upstream or passed in via side-branch)
        while (it.hasNext()) {
            FlowObject obj = it.next();
            if (obj instanceof FlowVariable) {
                FlowVariable v = (FlowVariable)obj;
                outsideLoopVarsSet.add(v.getName());
            }
        }

        // remove all within-loop-variables (e.g. "currentIteration")
        loopVarsDeque.removeIf(var -> !outsideLoopVarsSet.contains(var.getName()));

        // push overwritten variable (in reverse order to retain ordering)
        for (Iterator<FlowVariable> loopVarIt = loopVarsDeque.descendingIterator(); loopVarIt.hasNext();) {
            outgoingStackEndNode.push(FlowObjectStack.cloneUnsetOwner(loopVarIt.next()));
        }
    }

    /**
     * Traverses iterator and collects & returns loop variables (until 'Inner Loop Context'). Special treatment of
     * variables of corresponding start node -- they get filtered out (otherwise overwrites, e.g. "currentIteration").
     *
     * The iterator is positioned to point to variables defined outside the loop.
     *
     */
    private static Deque<FlowVariable> collectLoopVariables(final Set<String> propagatedVarsSet,
        final Iterator<FlowObject> it) {
        final Deque<FlowVariable> loopVarsDeque = new ArrayDeque<>();
        NodeID startNodeID = null;
        while (it.hasNext()) {
            FlowObject obj = it.next();
            if (obj instanceof InnerFlowLoopExecuteMarker) {
                startNodeID = obj.getOwner();
                break;
            } else if (obj instanceof FlowVariable) {
                loopVarsDeque.add((FlowVariable)obj);
            }
        }
        CheckUtils.checkState(startNodeID != null,
            "Expected to have seen \"%s\" on flow variable stack -- was method called from NodeModel#execute(...)?",
            InnerFlowLoopExecuteMarker.class.getSimpleName());

        final NodeID startNodeIDFinal = startNodeID;
        loopVarsDeque.removeIf(
            var -> var.getOwner().equals(startNodeIDFinal) && !propagatedVarsSet.contains(var.getName()));

        // traverse until 'Loop Context' (variables pushed by start node in its #configure method)
        while (it.hasNext() && !(it.next() instanceof FlowLoopContext)) {
            // skip, just move iterator
        }
        CheckUtils.checkState(it.hasNext(),
            "Expected to have seen \"%s\" on flow variable stack -- was method called from NodeModel#execute(...)?",
            FlowLoopContext.class.getSimpleName());

        return loopVarsDeque;
    }

}
