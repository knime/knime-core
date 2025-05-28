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
 * Created on 31.05.2013 by thor
 */
package org.knime.core.node.workflow;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.virtual.VirtualNodeContext;
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;

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
 * With 3.8 the node context has been extended to be able to hold other objects then {@link NodeContainer} (and
 * {@link WorkflowManager}), too. The other objects are usually the implementations of, e.g., the
 * NodeContainerUI-interface for the remote workflow editor. These objects are pushed on the stack via
 * {@link #pushContext(Object)} and retrieved with {@link #getContextObjectForClass(Class)}. A
 * {@link ContextObjectSupplier} is responsible to turn the pushed objects into an object of the request class, if
 * possible (and the requested class differs from the one pushed). A context object supplier can be registered via
 * {@link #addContextObjectSupplier(ContextObjectSupplier)}.
 *
 * <b>Note:</b> Take care not to define {@link NodeLogger} as a static member of this class. See
 *              https://knime-com.atlassian.net/browse/AP-12159
 *      for background and discussion about this.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.8
 * @noreference
 */
public final class NodeContext {
    private static final ThreadLocal<Deque<NodeContext>> THREAD_LOCAL = new ThreadLocal<Deque<NodeContext>>();

    private static final List<ContextObjectSupplier> CONTEXT_OBJECT_SUPPLIERS = new ArrayList<ContextObjectSupplier>();

    private final WeakReference<Object> m_contextObjectRef;

    // This was originally static final, constructed here - now you should use getNoContext().  See  https://knime-com.atlassian.net/browse/AP-12159
    private static NodeContext NO_CONTEXT = null;

    @SuppressWarnings("unused")
    private String m_fullStackTraceAtConstructionTime; // only used for debugging

    static {
        CONTEXT_OBJECT_SUPPLIERS.add(new DefaultContextObjectSupplier());
    }

    // See  https://knime-com.atlassian.net/browse/AP-12159
    private static synchronized NodeContext getNoContext() {
        if (NO_CONTEXT == null) {
            NO_CONTEXT = new NodeContext(null);
        }

        return NO_CONTEXT;
    }


    private NodeContext(final NodeContainer nodeContainer) {
        this((Object)nodeContainer);
    }

    private NodeContext(final Object contextObject) {
        m_contextObjectRef = new WeakReference<Object>(contextObject);
        if (KNIMEConstants.ASSERTIONS_ENABLED) {
            m_fullStackTraceAtConstructionTime = getStackTrace();
        }
    }

    private static String getStackTrace() {
        Thread thread = Thread.currentThread();
        return new StringBuilder()
                                            .append(thread.getName())
                                            .append(" (")
                                            .append(thread.getId())
                                            .append("):\n")
                                            .append(
                                                Arrays.stream(thread.getStackTrace()).map(s -> s.toString()).collect(Collectors.joining("\n  ")))
                                            .toString();
    }

    /**
     * Registered a new context object suppliers to be used for object retrieval via
     * #{@link NodeContext#getContextObjectForClass(Class)}.
     *
     * @param supplier object to register
     */
    public static void addContextObjectSupplier(final ContextObjectSupplier supplier) {
        CONTEXT_OBJECT_SUPPLIERS.add(supplier);
        if(CONTEXT_OBJECT_SUPPLIERS.size() > 2) {
            NodeLogger.getLogger(NodeContext.class).debugWithoutContext(
                "There are more than 2 context object suppliers registered which is likely not necessary.");
        }
    }

    /**
     * Removes a context object suppliers that was used for object retrieval via
     * #{@link NodeContext#getContextObjectForClass(Class)}.
     *
     * @param supplier object to remove
     */
    public static void removeContextObjectSupplier(final ContextObjectSupplier supplier) {
        CONTEXT_OBJECT_SUPPLIERS.remove(supplier);
        if(CONTEXT_OBJECT_SUPPLIERS.isEmpty()) {
            NodeLogger.getLogger(NodeContext.class).debugWithoutContext(
                "There are no context object suppliers registered which is likely not intended.");
        }
    }

    /**
     * Returns the context object, usually a node which is currently executing or the (root) workflow the node is
     * contained in.
     *
     * Returns an empty optional if the context is not available or it cannot be turned into the request class by any of
     * the register {@link ContextObjectSupplier}s.
     *
     * @param contextObjectClass the class of the context object to retrieve or to be turned into
     * @return the context object or an empty optional if not available or cannot be turned into the requested class
     */
    public <C> Optional<C> getContextObjectForClass(final Class<C> contextObjectClass) {
        Object obj = getContextObject();
        if (obj == null) {
            return Optional.empty();
        }

        Optional<C> res = Optional.empty();
        for(ContextObjectSupplier cos : CONTEXT_OBJECT_SUPPLIERS) {
           res = cos.getObjOfClass(contextObjectClass, obj);
           if(res.isPresent()) {
               break;
           }
        }

        if (!res.isPresent()) {
            NodeLogger.getLogger(NodeContext.class).debugWithoutContext("Context object is available but cannot be turned into an object of class "
                + contextObjectClass.getSimpleName() + ".");
        }
        return res;
    }

    /**
     * Returns the workflow manager which currently does an operation on a node. The result may be <code>null</code> if
     * the workflow manager does not exist any more, i.e. its workflow has been closed. This is very likely an
     * implementation error because nobody should hold a node context for a closed workflow.
     *
     * However, the result is also <code>null</code> it's the context for a workflow opened in the remote workflow
     * editor. In that case, the {@link #getContextObjectForClass(Class)}-method must be used in order to access the
     * desired context object.
     *
     * @return the workflow manager associated with the current node or <code>null</code>
     */
    public WorkflowManager getWorkflowManager() {
        return getContextObjectForClass(WorkflowManager.class).orElse(null);
    }

    /**
     * Returns the node container which is currently executing something. The result may be <code>null</code> if the
     * node container does not exist any more, i.e. its workflow has been closed. This is very likely an implementation
     * error because nobody should hold a node context for a closed workflow.
     *
     * @return a node container or <code>null</code>
     */
    public NodeContainer getNodeContainer() {
        return getContextObjectForClass(NodeContainer.class).orElse(null);
    }

    private final Object getContextObject() {
        Object obj = m_contextObjectRef.get();
        if (KNIMEConstants.ASSERTIONS_ENABLED && obj == null) {
            final NodeLogger logger = NodeLogger.getLogger(NodeContext.class);
            logger.debugWithoutContext("The context object has been garbage collected, you should not have such a context available");
//          logger.debugWithoutContext("Current stacktrace: " + getStackTrace());
//          logger
//              .debugWithoutContext("Stacktrace at context construction time: " + m_fullStackTraceAtConstructionTime);
        }
        return obj;
    }

    /**
     * Returns the current node context or <code>null</code> if no context exists.
     *
     * @return a node context or <code>null</code>
     */
    public static NodeContext getContext() {
        final NodeContext ctx = getContextStack().peek();
        if (ctx == getNoContext()) {
            return null;
        } else {
            return ctx;
        }
    }

    /**
     * Wraps the result of {@link #getContext()} in an {@link Optional}.
     * @return An optional representing the current node context.
     * @since 5.4
     */
    public static Optional<NodeContext> getContextOptional() {
        return Optional.ofNullable(getContext());
    }

    /**
     * Pushes a new context on the context stack for the current thread using the given node container.
     *
     * @param nodeContainer the node container for the current thread, must not be <code>null</code>
     */
    public static void pushContext(final NodeContainer nodeContainer) {
        assert (nodeContainer != null) : "Node container must not be null";
        pushContext((Object)nodeContainer);
    }

    /**
     * Pushes a new context on the context stack for the current thread using the given context object. The context
     * object can be retrieved with the {@link #getContextObjectForClass(Class)}.
     *
     * @param contextObject the context object for the current thread, must not be <code>null</code>
     */
    public static void pushContext(final Object contextObject) {
        assert (contextObject != null) : "Context object must not be null";
        Deque<NodeContext> stack = getContextStack();
        stack.push(new NodeContext(contextObject));
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
        final Deque<NodeContext> stack = getContextStack();
        if (context == null) {
            stack.push(getNoContext());
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
        Deque<NodeContext> stack = THREAD_LOCAL.get();
        if (stack == null) {
            stack = new ArrayDeque<>(4);
            THREAD_LOCAL.set(stack);
        } else if (stack.size() > 10) {
            NodeLogger.getLogger(NodeContext.class)
                .codingWithoutContext("Node context stack has more than 10 elements (" + stack.size()
                    + "), looks like we are leaking contexts somewhere");
        }
        return stack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (this == getNoContext()) {
            return "NO CONTEXT";
        } else {
            Object obj = m_contextObjectRef.get();
            if (obj == null) {
                return "Context Object (garbage collected)";
            } else {
                return obj.toString();
            }
        }
    }

    /**
     * Returns the user of the current workflow. If the current workflow cannot be determined, e.g. because no context
     * is available in the current thread, an empty optional is returned.
     *
     * @return the workflow user or an empty optional
     */
    public static Optional<String> getWorkflowUser() {
        final NodeContext context = NodeContext.getContext();
        if (context != null) {
            final WorkflowManager workflowManager = context.getWorkflowManager();
            if (workflowManager != null) {
                final WorkflowContext workflowContext = workflowManager.getContext();
                if (workflowContext != null) {
                    NodeLogger.getLogger(NodeContext.class).debug("Workflow user found: " + workflowContext.getUserid());
                    return Optional.of(workflowContext.getUserid());
                } else {
                    NodeLogger.getLogger(NodeContext.class).warn("Workflow context not available");
                }
            } else {
                NodeLogger.getLogger(NodeContext.class).warn("Workflow manager not available");
            }
        } else {
            NodeLogger.getLogger(NodeContext.class).warn("Node context not available");
        }
        return Optional.empty();
    }

    /**
     * Turns a context object into an object of expected class.
     *
     * TODO priorities could be added, but not necessary, yet
     * TODO supplier could return list of supported classes
     */
    public static interface ContextObjectSupplier {

        /**
         * Turns the given context object into the expected class.
         *
         * @param contextObjClass the class expected
         * @param srcObj the actual context object
         * @return the object for the given class or an empty optional if there is no mapping
         */
        <C> Optional<C> getObjOfClass(Class<C> contextObjClass, Object srcObj);
    }

    /**
     * The default context object supplier for {@link NodeContainer} and {@link WorkflowManager}.
     */
    private static final class DefaultContextObjectSupplier implements ContextObjectSupplier {

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <C> Optional<C> getObjOfClass(final Class<C> contextObjClass, final Object srcObj) {
            if (srcObj instanceof NodeContainer nc) {
                //order of checking important
                if (WorkflowManager.class.isAssignableFrom(contextObjClass)) {
                    return Optional.of((C)NodeContainerParent.getProjectWFM(nc));
                } else if (NodeContainer.class.isAssignableFrom(contextObjClass)) {
                    return Optional.of((C)srcObj);
                } else if (WorkflowContextV2.class.isAssignableFrom(contextObjClass)) {
                    return Optional.ofNullable((C)NodeContainerParent.getProjectWFM(nc).getContextV2());
                } else if (VirtualNodeContext.class.isAssignableFrom(contextObjClass)
                    && nc instanceof NativeNodeContainer nnc) {
                    var virtualScope =
                        NativeNodeContainer.getFlowScopeContextFromHierarchy(FlowVirtualScopeContext.class, nnc.getFlowObjectStack());
                    return Optional.ofNullable((C)virtualScope);
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }
    }
}
