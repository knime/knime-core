/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   15.03.2007 (berthold): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.knime.core.internal.KNIMEPath;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ScopeVariable.Type;
import org.knime.core.util.Pair;


/**
 * Container for the stack that keeps for an individual node the scope
 * information.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ScopeObjectStack {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ScopeObjectStack.class);
    
    /** Root stack with all constants. */
    private static ScopeObjectStack rootStack = new ScopeObjectStack();
    
    /** Stack of ScopeContext, which is shared among nodes along the
     * workflow. */
    private final Vector<ScopeObject> m_stack;
    /** Owner of ScopeContext object, which are put onto m_stack via this
     * StackWrapper. */
    private final NodeID m_nodeID;
    
    /** Root stack. */
    private ScopeObjectStack() {
        m_nodeID = WorkflowManager.ROOT.getID();
        m_stack = new Vector<ScopeObject>();
        File wsDirPath = KNIMEPath.getWorkspaceDirPath();
        if (wsDirPath != null) {
            push(new ScopeVariable(
                    "knime.workspace", wsDirPath.getAbsolutePath(), true));
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
                        push(new ScopeVariable("knime." + key, i, true));
                    } catch (Exception e) {
                        LOGGER.warn("Can't parse assignment of integer "
                                + "constant \"" + key + "\": " + value);
                    }
                    break;
                case DOUBLE:
                    try {
                        double d = Double.parseDouble(value);
                        push(new ScopeVariable("knime." + key, d, true));
                    } catch (Exception e) {
                        LOGGER.warn("Can't parse assignment of double"
                                + "constant\"" + key + "\": " + value);
                    }
                    break;
                case STRING:
                    push(new ScopeVariable("knime." + key, value, true));
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
     * @throws IllegalContextStackObjectException If the stacks can't be merged.
     */
    @SuppressWarnings("unchecked")
    ScopeObjectStack(final NodeID id,
            final ScopeObjectStack... predStacks) {
        if (id == null) {
            throw new NullPointerException("NodeID argument must not be null.");
        }
        List<Vector<ScopeObject>> predecessors = 
            new ArrayList<Vector<ScopeObject>>();
        for (int i = 0; i < predStacks.length; i++) {
            if (predStacks[i] != null) {
                predecessors.add(predStacks[i].m_stack);
            }
        }
        if (predecessors.isEmpty()) {
            predecessors.add(rootStack.m_stack);
        }
        if (predecessors.size() == 0) {
            m_stack = new Vector<ScopeObject>();
        } else if (predecessors.size() == 1) {
            m_stack = (Vector<ScopeObject>)predecessors.get(0).clone();
        } else {
            @SuppressWarnings("unchecked")
            Vector<ScopeObject>[] sos = predecessors.toArray(
                    new Vector[predecessors.size()]);
            m_stack = merge(sos);
        }
        m_nodeID = id;
    }

    private static Vector<ScopeObject> merge(final Vector<ScopeObject>[] sos) {
        Vector<ScopeObject> result = new Vector<ScopeObject>();
        @SuppressWarnings("unchecked") // no generics in array definition
        Iterator<ScopeObject>[] its = new Iterator[sos.length];
        ScopeObject[] nexts = new ScopeObject[sos.length];
        boolean hasMoreElements = false;
        for (int i = 0; i < sos.length; i++) {
            its[i] = sos[i].iterator();
            hasMoreElements = hasMoreElements ||  its[i].hasNext();
        }
        while (hasMoreElements) {
            hasMoreElements = false;
            ScopeObject commonScopeO = null;
            for (int i = 0; i < sos.length; i++) {
                while (nexts[i] != null || its[i].hasNext()) {
                    ScopeObject o = nexts[i] != null ? nexts[i] : its[i].next();
                    nexts[i] = null;
                    if (o instanceof ScopeLoopContext) {
                        // we must check for identity here - otherwise
                        // nested loops are not possible
                        if (commonScopeO != null && commonScopeO != o) {
                            throw new IllegalContextStackObjectException(
                                    "Stack can't be merged: Conflicting "
                                    + "ScopeContext objects:" + o + " vs. "
                                    + commonScopeO);
                        }
                        commonScopeO = o;
                        nexts[i] = o;
                        hasMoreElements = true;
                        break;
                    }
                    result.add(o);
                }
            }
            if (commonScopeO != null) {
                result.add(commonScopeO);
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
     * @param type The desired scope class
     * @see java.util.Stack#peek()
     */
    public <T extends ScopeObject> T peek(final Class<T> type) {
        for (int i = m_stack.size() - 1; i >= 0; i--) {
            ScopeObject e = m_stack.get(i);
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
     * @param <T> The desired scope context type.
     * @param type The class of that type.
     * @return The first (top-most) element on the stack of class
     * <code>type</code> or <code>null</code> if no such element is available.
     * @see java.util.Stack#pop()
     */
    public <T extends ScopeObject> T pop(final Class<T> type) {
        for (int i = m_stack.size() - 1; i >= 0; i--) {
            ScopeObject e = m_stack.remove(i);
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
    public ScopeVariable peekScopeVariable(final String name, final Type type) {
        for (int i = m_stack.size() - 1; i >= 0; i--) {
            ScopeObject e = m_stack.get(i);
            if (!(e instanceof ScopeVariable)) {
                continue;
            }
            ScopeVariable v = (ScopeVariable)e;
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
    public Map<String, ScopeVariable> getAvailableVariables() {
        LinkedHashMap<String, ScopeVariable> hash = 
            new LinkedHashMap<String, ScopeVariable>();
        for (int i = m_stack.size() - 1; i >= 0; i--) {
            ScopeObject e = m_stack.get(i);
            if (!(e instanceof ScopeVariable)) {
                continue;
            }
            ScopeVariable v = (ScopeVariable)e;
            if (!hash.containsKey(v.getName())) {
                hash.put(v.getName(), v);
            }
        }
        return Collections.unmodifiableMap(hash);
    }
    
    /** Get all objects on the stack that are owned by the node with the given
     * id. This method is used to persist the stack.
     * @param id identifies objects of interest.
     * @return list of all elements that are put onto the stack by 
     *         the argument node
     */
    List<ScopeObject> getScopeObjectsOwnedBy(final NodeID id) {
        List<ScopeObject> result = new ArrayList<ScopeObject>();
        boolean isInSequence = true;
        for (ScopeObject v : m_stack) {
            if (v.getOwner().equals(id)) {
                isInSequence = false;
                result.add(v);
            }
            assert isInSequence || v.getOwner().equals(id)
                : "Scope objects are not ordered";
        }
        return result;
    }
    

    /**
     * @param item ScopeContext to be put onto stack.
     * @see java.util.Stack#push(java.lang.Object)
     */
    public void push(final ScopeObject item) {
        if ((item.getOwner() != null) && (!item.getOwner().equals(m_nodeID))) {
            throw new IllegalArgumentException(
                    "Can't put a ScopeContext item onto stack, which already "
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

    @Override
    public String toString() {
        return toDeepString();
    }

    public String toDeepString() {
        StringBuilder b = new StringBuilder();
        b.append("---");
        b.append(m_nodeID);
        b.append("---");
        b.append('\n');
        for (int i = m_stack.size() - 1; i >= 0; --i) {
            ScopeObject o = m_stack.get(i);
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
    
}
