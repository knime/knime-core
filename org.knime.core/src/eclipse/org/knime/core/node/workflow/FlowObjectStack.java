/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   15.03.2007 (berthold): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.knime.core.internal.KNIMEPath;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.util.Pair;


/**
 * Container for the stack that keeps for an individual node the
 * flow variables and flow loop information.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class FlowObjectStack implements Iterable<FlowObject> {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(FlowObjectStack.class);

    /** Root stack with all constants. */
    private static FlowObjectStack rootStack = new FlowObjectStack();

    /** Stack of FlowObjects, which is shared among nodes along the
     * workflow. */
    private final Vector<FlowObject> m_stack;
    /** Owner of FlowObject object, which are put onto m_stack via this
     * StackWrapper. */
    private final NodeID m_nodeID;

    /** Root stack. */
    private FlowObjectStack() {
        m_nodeID = WorkflowManager.ROOT.getID();
        m_stack = new Vector<FlowObject>();
        File wsDirPath = KNIMEPath.getWorkspaceDirPath();
        if (wsDirPath != null) {
            push(new FlowVariable("knime.workspace",
                    wsDirPath.getAbsolutePath(), Scope.Global));
        }
        for (Map.Entry<Object, Object> p : System.getProperties().entrySet()) {
            String name = p.getKey().toString();
            Pair<String, Type> varDef = getVariableDefinition(name);
            if (varDef != null) {
                String value = p.getValue().toString();
                String key = varDef.getFirst();
                switch (varDef.getSecond()) {
                case INTEGER:
                    try {
                        int i = Integer.parseInt(value);
                        push(new FlowVariable("knime." + key, i, Scope.Global));
                    } catch (Exception e) {
                        LOGGER.warn("Can't parse assignment of integer "
                                + "constant \"" + key + "\": " + value);
                    }
                    break;
                case DOUBLE:
                    try {
                        double d = Double.parseDouble(value);
                        push(new FlowVariable("knime." + key, d, Scope.Global));
                    } catch (Exception e) {
                        LOGGER.warn("Can't parse assignment of double"
                                + "constant\"" + key + "\": " + value);
                    }
                    break;
                case STRING:
                    push(new FlowVariable("knime." + key, value, Scope.Global));
                    break;
                }
            }
        }
    }

    /**
     * Creates new stack based. If the argument stack is empty, null or
     * contains only null elements, an empty stack is created. If there is
     * more than one argument stack available, the stacks will be merged.
     * @param id The Node's ID, must not be null.
     * @param predStacks The stacks from the predecessor nodes, may be null or
     * empty.
     * @throws NullPointerException If <code>id</code> is <code>null</code>.
     * @throws IllegalFlowObjectStackException If the stacks can't be merged.
     */
    @SuppressWarnings("unchecked")
    FlowObjectStack(final NodeID id,
            final FlowObjectStack... predStacks) {
        if (id == null) {
            throw new NullPointerException("NodeID argument must not be null.");
        }
        List<Vector<FlowObject>> predecessors =
            new ArrayList<Vector<FlowObject>>();
        for (int i = 0; i < predStacks.length; i++) {
            if (predStacks[i] != null) {
                predecessors.add(predStacks[i].m_stack);
            }
        }
        if (predecessors.isEmpty()) {
            predecessors.add(rootStack.m_stack);
        }
        Vector<FlowObject>[] sos = predecessors.toArray(
                new Vector[predecessors.size()]);
        m_stack = merge(resortInputStacks(sos));
        m_nodeID = id;
    }

    /** Resorts the array elements so that the first element is the last
     * element in the returned array. This became necessary with introducing
     * the flow variable ports, which need to overrule all incoming flow
     * variables.
     * <p>
     * Method was added to address bug 2392.
     * @param sos The input stacks
     * @return sos if there is at most one element in the input stack or
     *         a copy, whereby the copy will be shifted by one and the last
     *         element is the first element of sos.
     */
    private static Vector<FlowObject>[] resortInputStacks(
            final Vector<FlowObject>[] sos) {
        if (sos.length <= 1) {
            return sos;
        }
        @SuppressWarnings("unchecked")
        Vector<FlowObject>[] result = new Vector[sos.length];
        System.arraycopy(sos, 1, result, 0, sos.length - 1);
        result[sos.length - 1] = sos[0];
        return result;
    }

    private static Vector<FlowObject> merge(final Vector<FlowObject>[] sos) {
        Vector<FlowObject> result = new Vector<FlowObject>();
        @SuppressWarnings("unchecked") // no generics in array definition
        Iterator<FlowObject>[] its = new Iterator[sos.length];
        FlowObject[] nexts = new FlowObject[sos.length];
        boolean hasMoreElements = false;
        for (int i = 0; i < sos.length; i++) {
            its[i] = new FilteredScopeIterator(sos[i].iterator(), Scope.Local);
            hasMoreElements = hasMoreElements ||  its[i].hasNext();
        }
        while (hasMoreElements) {
            hasMoreElements = false;
            // hash of variables to fix bug 1959 (constants are duplicated
            // on nodes with 2+ inports)
            LinkedHashSet<FlowObject> variableSet =
                new LinkedHashSet<FlowObject>();
            FlowObject commonFlowO = null;
            /* for each input stack, traverse the stack bottom up until either
             * the top or a FlowLoopContext control object is found. The loop
             * controls must come in the same order on each of the stacks (if
             * present). Repeat that until the top of the stack is reached. For
             * each of the buckets, put the variables into a hash and add the
             * hash set content to the result list. */
            for (int i = 0; i < sos.length; i++) {
                while (nexts[i] != null || its[i].hasNext()) {
                    FlowObject o = nexts[i] != null ? nexts[i] : its[i].next();
                    nexts[i] = null;
                    if (o instanceof FlowLoopContext) {
                        // make sure loop contexts belong to same loops
                        // (can be different objects, though - see bug #3208)
                        if (commonFlowO != null && !commonFlowO.equals(o)) {
                            throw new IllegalFlowObjectStackException(
                                    "Conflicting FlowObjects: " + o + " vs. "
                                    + commonFlowO
                                    + " (loops not properly nested?)");
                        }
                        commonFlowO = o;
                        nexts[i] = o;
                        hasMoreElements = true;
                        break;
                    }
                    // remove any previously added variable first to update
                    // variable order ("add" overwrites variables but does not
                    // update insertion order)
                    variableSet.remove(o);
                    variableSet.add(o);
                }
            }
            result.addAll(variableSet);
            if (commonFlowO != null) {
                result.add(commonFlowO);
                for (int i = 0; i < nexts.length; i++) {
                    nexts[i] = null;
                }
            }
        }
        return result;
    }

    /**
     * @return The top-most element on the stack that complies with the given
     * class argument or <code>null</code> if no such element is found.
     * @param <T> The class type of the context object
     * @param type The desired FlowObject class
     * @see java.util.Stack#peek()
     */
    public <T extends FlowObject> T peek(final Class<T> type) {
        for (int i = m_stack.size() - 1; i >= 0; i--) {
            FlowObject e = m_stack.get(i);
            if (type.isInstance(e)) {
                return type.cast(e);
            }
        }
        return null;
    }

    /**
     * Removes all elements from the stack whose class is not of the given type.
     * It also removes the top-most element complying with the given class.
     * If no such element exists, the stack will be empty after this method is
     * called.
     * @param <T> The desired FlowObject type.
     * @param type The class of that type.
     * @return The first (top-most) element on the stack of class
     * <code>type</code> or <code>null</code> if no such element is available.
     * @see java.util.Stack#pop()
     */
    public <T extends FlowObject> T pop(final Class<T> type) {
        for (int i = m_stack.size() - 1; i >= 0; i--) {
            FlowObject e = m_stack.remove(i);
            if (type.isInstance(e)) {
                return type.cast(e);
            }
        }
        return null;
    }

    /** Get the variable with the given name or null if no such variable
     * is on the stack.
     * @param name To peek
     * @param type The type of the variable to seek.
     * @return the variable or null
     */
    public FlowVariable peekFlowVariable(final String name, final Type type) {
        for (int i = m_stack.size() - 1; i >= 0; i--) {
            FlowObject e = m_stack.get(i);
            if (!(e instanceof FlowVariable)) {
                continue;
            }
            FlowVariable v = (FlowVariable)e;
            if (v.getName().equals(name) && v.getType().equals(type)) {
                return v;
            }
        }
        throw new NoSuchElementException("No such variable \"" + name + "\" of"
                + " type " + type);
    }

    /** Get all (visible!) variables on the stack in a non-modifiable map.
     * This method is used to show available variables to the user.
     * @return Such a map.
     */
    public Map<String, FlowVariable> getAvailableFlowVariables() {
        LinkedHashMap<String, FlowVariable> hash =
            new LinkedHashMap<String, FlowVariable>();
        for (int i = m_stack.size() - 1; i >= 0; i--) {
            FlowObject e = m_stack.get(i);
            if (!(e instanceof FlowVariable)) {
                continue;
            }
            FlowVariable v = (FlowVariable)e;
            if (!hash.containsKey(v.getName())) {
                hash.put(v.getName(), v);
            }
        }
        return Collections.unmodifiableMap(hash);
    }

    /** Get all objects on the stack that are owned by the node with the given
     * id. This method is used to persist the stack.
     * @param id identifies objects of interest.
     * @param ignoredScopes List of scopes that are skipped
     *        (e.g. local variables are ignored in successor nodes)
     * @return list of all elements that are put onto the stack by
     *         the argument node
     */
    List<FlowObject> getFlowObjectsOwnedBy(final NodeID id,
            final Scope... ignoredScopes) {
        List<FlowObject> result = new ArrayList<FlowObject>();
        boolean isInSequence = true;
        FilteredScopeIterator it =
            new FilteredScopeIterator(m_stack.iterator(), ignoredScopes);
        while (it.hasNext()) {
            FlowObject v = it.next();
            if (v.getOwner().equals(id)) {
                isInSequence = false;
                result.add(v);
            }
            assert isInSequence || v.getOwner().equals(id)
                : "FlowObjects are not ordered";
        }
        return result;
    }


    /**
     * @param item FlowObject to be put onto stack.
     * @see java.util.Stack#push(java.lang.Object)
     */
    public void push(final FlowObject item) {
        if ((item.getOwner() != null) && (!item.getOwner().equals(m_nodeID))) {
            throw new IllegalArgumentException(
                    "Can't put a FlowObject item onto stack, which already "
                    + "has a different owner.");
        }
        item.setOwner(m_nodeID);
        m_stack.add(item);
    }

    /**
     * @return true if stack is empty
     */
    boolean isEmpty() {
        return m_stack.isEmpty();
    }

    /** Get number of elements in the stack.
     * @return size of stack. */
    int size() {
        return m_stack.size();
    }

    /** Get iterator on elements, top of stack first. The iterator is
     * read only and not affected by potential modifications of the stack
     * after this method returns (iterator on copy).
     * {@inheritDoc} */
    @Override
    public Iterator<FlowObject> iterator() {
        Vector<FlowObject> copy = new Vector<FlowObject>(m_stack);
        Collections.reverse(copy);
        return Collections.unmodifiableList(copy).iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toDeepString();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // stacks are not really used in hashs ... but since we implement equals
        int hash = m_nodeID.hashCode();
        for (FlowObject o : m_stack) {
            hash += o.hashCode();
        }
        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FlowObjectStack)) {
            return false;
        }
        FlowObjectStack o = (FlowObjectStack)obj;
        return o.m_nodeID.equals(m_nodeID)
            && o.m_stack.equals(m_stack); // deep equals!
    }

    /**
     * @return String representation with list of all objects on stack.
     */
    public String toDeepString() {
        StringBuilder b = new StringBuilder();
        b.append("---");
        b.append(m_nodeID);
        b.append("---");
        b.append('\n');
        for (int i = m_stack.size() - 1; i >= 0; --i) {
            FlowObject o = m_stack.get(i);
            b.append(o);
            b.append('\n');
        }
        b.append("--------");
        return b.toString();
    }

    private static final Pair<String, Type> getVariableDefinition(
            final String propKey) {
        String varName;
        Type varType;
        if (propKey.startsWith("knime.constant.double.")) {
            varName = propKey.substring("knime.constant.double.".length());
            varType = Type.DOUBLE;
        } else if (propKey.startsWith("knime.constant.integer.")) {
            varName = propKey.substring("knime.constant.integer.".length());
            varType = Type.INTEGER;
        } else if (propKey.startsWith("knime.constant.string.")) {
            varName = propKey.substring("knime.constant.string.".length());
            varType = Type.STRING;
        } else {
            return null;
        }
        if (varName.length() == 0) {
            LOGGER.warn("Ignoring constant defintion \"" + propKey + "\": "
                    + "missing suffix, e.g. \"" + propKey + "somename\"");
            return null;
        }
        return new Pair<String, Type>(varName, varType);
    }

    /** Iterator that removes flow variables with given scopes from an
     * underlying iterator. Used, for instance to remove "local" variables when
     * merging stacks of predecessor nodes.
     */
    private static final class FilteredScopeIterator
        implements Iterator<FlowObject> {

        private final Iterator<FlowObject> m_it;
        private final List<Scope> m_ignoredScopes;
        private FlowObject m_next;

        /**
         *
         */
        public FilteredScopeIterator(final Iterator<FlowObject> it,
                final Scope... ignoredScopes) {
            m_it = it;
            m_ignoredScopes = Arrays.asList(ignoredScopes);
            m_next = internalNext();
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return m_next != null;
        }

        /** {@inheritDoc} */
        @Override
        public FlowObject next() {
            FlowObject result = m_next;
            if (result == null) {
                throw new NoSuchElementException("Iterator at end");
            }
            m_next = internalNext();
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported");
        }

        private FlowObject internalNext() {
            while (m_it.hasNext()) {
                FlowObject next = m_it.next();
                if (next instanceof FlowVariable) {
                    FlowVariable v = (FlowVariable)next;
                    if (m_ignoredScopes.contains(v.getScope())) {
                        continue;
                    }
                }
                return next;
            }
            return null;
        }

    }


}
