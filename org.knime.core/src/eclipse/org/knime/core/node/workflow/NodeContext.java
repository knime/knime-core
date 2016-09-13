/*
 * ------------------------------------------------------------------------
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
 * Created on 31.05.2013 by thor
 */
package org.knime.core.node.workflow;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;

/**
 * A {@link NodeContext} holds information about the context in which an operation on a node is executed. This is used
 * for internal purposes, node implementors should not use this class.<br>
 * The node context is local to the current thread and is set by the workflow manager or the node container in which a
 * node is contained. Each thread has a stack of {@link NodeContext} objects, only the last set context can be retrieved
 * via {@link #getContext()}. You must absolutely make sure that if you push a new context that you remove it
 * afterwards. Therefore the common usage pattern is a follows:
 *
 * <pre>
 * NodeContext.pushContext(nodeContainer);
 * try {
 *     doSomething();
 * } finally {
 *     NodeContext.removeLastContext():
 * }
 * </pre>
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.8
 * @noreference
 */
public final class NodeContext {
    private static final ThreadLocal<Deque<NodeContext>> threadLocal = new ThreadLocal<Deque<NodeContext>>();

    private static final NodeLogger logger = NodeLogger.getLogger(NodeContext.class);

    private final WeakReference<NodeContainer> m_nodeContainerRef;

    private static final NodeContext NO_CONTEXT = new NodeContext(null);

    @SuppressWarnings("unused")
    private StackTraceElement[] m_callStack; // only used for debugging

    private NodeContext(final NodeContainer nodeContainer) {
        m_nodeContainerRef = new WeakReference<NodeContainer>(nodeContainer);
        if (KNIMEConstants.ASSERTIONS_ENABLED) {
            m_callStack = Thread.currentThread().getStackTrace();
        }
    }

    /**
     * Returns the workflow manager which currently does an operation on a node. The result may be <code>null</code> if
     * the workflow manager does not exist any more, i.e. its workflow has been closed. This is very likely an
     * implementation error because nobody should hold a node context for a closed workflow.
     *
     * @return the workflow manager associated with the current node or <code>null</code>
     */
    public WorkflowManager getWorkflowManager() {
        NodeContainer nc = getNodeContainer();
        if (nc == null) {
            return null;
        }

        // find the actual workflow and not the metanode the container may be in
        NodeContainerParent parent = nc instanceof WorkflowManager ? (WorkflowManager)nc : nc.getDirectNCParent();

        while (!(parent instanceof WorkflowManager && ((WorkflowManager)parent).isProject())) {
            assert parent != null : "Parent item can't be null as a project parent is expected";
            parent = parent.getDirectNCParent();
        }
        return (WorkflowManager)parent;
    }

    /**
     * Returns the node container which is currently executing something. The result may be <code>null</code> if the
     * node container does not exist any more, i.e. its workflow has been closed. This is very likely an implementation
     * error because nobody should hold a node context for a closed workflow.
     *
     * @return a node container or <code>null</code>
     */
    public NodeContainer getNodeContainer() {
        NodeContainer cont = m_nodeContainerRef.get();
        if (KNIMEConstants.ASSERTIONS_ENABLED && cont == null) {
            logger.debugWithoutContext(
                "Node container has been garbage collected, you should not have such a context available");
        }
        return cont;
    }

    /**
     * Returns the current node context or <code>null</code> if no context exists.
     *
     * @return a node context or <code>null</code>
     */
    public static NodeContext getContext() {
        NodeContext ctx = getContextStack().peek();
        if (ctx == NO_CONTEXT) {
            return null;
        } else {
            return ctx;
        }
    }

    /**
     * Pushes a new context on the context stack for the current thread using the given node container.
     *
     * @param nodeContainer the node container for the current thread, must not be <code>null</code>
     */
    public static void pushContext(final NodeContainer nodeContainer) {
        assert (nodeContainer != null) : "Node container must not be null";
        Deque<NodeContext> stack = getContextStack();
        stack.push(new NodeContext(nodeContainer));
    }

    /**
     * Removes the top-most context from the context stack.
     *
     * @throws IllegalStateException if no context is available / if the context stack is empty
     */
    public static void removeLastContext() {
        Deque<NodeContext> stack = getContextStack();
        if (stack.isEmpty()) {
            throw new IllegalStateException("No node context registered with the current thread");
        } else {
            stack.pop();
        }
    }

    /**
     * Pushes the given context on the context stack for the current thread. The context may be <code>null</code>.
     *
     * @param context an existing context, may be <code>null</code>
     */
    public static void pushContext(final NodeContext context) {
        Deque<NodeContext> stack = getContextStack();
        if (context == null) {
            stack.push(NO_CONTEXT);
        } else {
            stack.push(context);
        }
    }

    /**
     * Returns the context stack for the current thread. This methods has package scope on purpose so that the
     * {@link NodeExecutionJob} has access to the stack and can save and restore it. See {@link NodeExecutionJob#run()}.
     *
     * @return a stack with node contexts
     */
    static Deque<NodeContext> getContextStack() {
        Deque<NodeContext> stack = threadLocal.get();
        if (stack == null) {
            stack = new ArrayDeque<NodeContext>(4);
            threadLocal.set(stack);
        } else if (stack.size() > 10) {
            logger.coding("Node context stack has more than 10 elements (" + stack.size()
                + "), looks like we are leaking contexts somewhere");
        }
        return stack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (this == NO_CONTEXT) {
            return "NO CONTEXT";
        } else {
            NodeContainer cont = m_nodeContainerRef.get();
            if (cont == null) {
                return "Node Container (garbage collected)";
            } else {
                return cont.toString();
            }
        }
    }
}
