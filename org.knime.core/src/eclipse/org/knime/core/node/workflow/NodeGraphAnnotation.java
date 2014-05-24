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
 * Created on May 17, 2013 by Berthold
 */
package org.knime.core.node.workflow;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

/** Wrapper holding graph annotation information for a node in a workflow such
 * as position in nested scopes, layered depth etc. to make subsequent graphs
 * operations more efficient.
 *
 * @author M. Berthold
 * @since 2.8
 */
public final class NodeGraphAnnotation implements Comparable<NodeGraphAnnotation> {

    private final NodeID m_id;
    enum Role { NONE, START, END }
    private Role m_role = Role.NONE;
    private int m_outportIndex;
    private int m_depth;
    private LinkedHashSet<Integer> m_connectedMetaInPorts;
    private Stack<NodeID> m_startNodeStack;
    private LinkedHashSet<Integer> m_connectedMetaOutPorts;
    private Stack<NodeID> m_endNodeStack;
    private String m_error;

    /**
     * Constructor for depth=0 nodes not coming from a metanode inport - stack will be empty.
     *
     * @param node this NGA is associated with.
     */
    NodeGraphAnnotation(final NodeContainer node) {
        this(node, -1, null);
    }

    /**
     * Constructor for depth=0 nodes coming from a metanode inport - stack will be empty.
     *
     * @param id of node
     * @param metanodeInportIx metanode inport this node is connected to.
     */
    NodeGraphAnnotation(final NodeID id, final int metanodeInportIx) {
        m_id = id;
        m_outportIndex = metanodeInportIx;
        m_depth = 0;
        m_connectedMetaInPorts = new LinkedHashSet<Integer>();
        m_connectedMetaInPorts.add(metanodeInportIx);
        m_connectedMetaOutPorts = new LinkedHashSet<Integer>();
        m_startNodeStack = new Stack<NodeID>();
        m_error = null;
    }

    /**
     * Constructor for depth > 0 - depth & stack will be initialized (copied!) from predecessor.
     *
     * @param node associated with this NGA.
     * @param outportIx -1 for a SNC, outport index for a Metanode.
     * @param prec NGA of the predecessor.
     */
    NodeGraphAnnotation(final NodeContainer node, final int outportIx, final NodeGraphAnnotation prec) {
        m_id = node.getID();
        m_outportIndex = outportIx;
        if (prec != null) {
            m_depth = prec.m_depth + 1;
            m_connectedMetaInPorts = (LinkedHashSet<Integer>)prec.m_connectedMetaInPorts.clone();
            m_startNodeStack = prec.cloneScopeStackForSuccessor();
        } else {
            m_depth = 0;
            m_connectedMetaInPorts = new LinkedHashSet<Integer>();
            m_startNodeStack = new Stack<NodeID>();
        }
        m_connectedMetaOutPorts = new LinkedHashSet<Integer>();
        m_error = null;
        if (node instanceof SingleNodeContainer) {
            if (((SingleNodeContainer)node).isModelCompatibleTo(ScopeStartNode.class)) {
                m_startNodeStack.push(m_id);
                m_role = Role.START;
            }
            if (((SingleNodeContainer)node).isModelCompatibleTo(ScopeEndNode.class)) {
                if (m_startNodeStack.size() < 1) {
                    m_error = "Missing Start Node.";
                }
                m_role = Role.END;
            }
        }
    }

    /**
     * @return id of node.
     */
    public NodeID getID() {
        return m_id;
    }

    /**
     * @return outport index.
     */
    public int getOutportIndex() {
        return m_outportIndex;
    }

    /**
     * @return connected input port indices.
     */
    public Set<Integer> getConnectedInportIndices() {
        return m_connectedMetaInPorts == null ? null : Collections.unmodifiableSet(m_connectedMetaInPorts);
    }

    /** Add a new connected outport to the metanode outport indices this node is (directly
     * or more likely: indirectly) connected to.
     *
     * @param index to be added
     */
    void addConnectedOutport(final int index) {
        m_connectedMetaOutPorts.add(index);
    }

    /**
     * @return connected outport port indices.
     */
    public Set<Integer> getConnectedOutportIndices() {
        return m_connectedMetaOutPorts == null ? null : Collections.unmodifiableSet(m_connectedMetaOutPorts);
    }

    /**
     * @return depth of node.
     */
    public int getDepth() {
        return m_depth;
    }

    /**
     * @return top element of stack holding start node ids or null if stack is empty.
     */
    NodeID peekStartNodeStack() {
        return m_startNodeStack.size() > 0 ? m_startNodeStack.peek() : null;
    }

    /**
     * @return top element of end node id stack or null if stack is empty.
     */
    NodeID peekEndNodeStack() {
        return m_endNodeStack.size() > 0 ? m_endNodeStack.peek() : null;
    }

    /**
     * @param id ...
     * @return true if forward scope stack contains given id.
     */
    boolean startNodeStackContains(final NodeID id) {
        return m_startNodeStack.contains(id);
    }

    /**
     * @return error message or null of everything is fine.
     */
    public String getError() {
        return m_error;
    }

    /**
     * @return role of node.
     */
    public String getRole() {
        return m_role.toString();
    }

    /**
     * @return string representation of stack holding start node ids.
     */
    public String getStartNodeStackAsString() {
        return m_startNodeStack.toString();
    }

    /**
     * @return string representation of stack holding end node ids.
     */
    public String getEndNodeStackAsString() {
        return m_endNodeStack == null ? "null" : m_endNodeStack.toString();
    }

    /** Merge with another object arriving at the same node during forward graph analysis.
     *
     * @param nga the node annotations from the new branch.
     * @return true if changes were made (e.g. depth was adjusted, scope stack changed).
     */
    boolean mergeForward(final NodeGraphAnnotation nga) {
        // has to be same node and outport (in case of a metanode)!
        assert m_outportIndex == nga.m_outportIndex;
        assert m_id.equals(nga.m_id);
        boolean changesMade = false;
        // align depth
        if (m_depth < nga.m_depth) {
            m_depth = nga.m_depth;
            changesMade = true;
        }
        // join the lists of meta node inports
        m_connectedMetaInPorts.addAll(nga.m_connectedMetaInPorts);
        // and finally merge the two stacks:
        if (m_startNodeStack.equals(nga.m_startNodeStack)) {
            // a) they are equal: easy - nothing to do.
        } else if (nga.m_startNodeStack.size() == 0) {
            // b) the other one is empty - nothing do to.
        } else if (m_startNodeStack.size() == 0) {
            // c) our own is empty
            m_startNodeStack = (Stack<NodeID>)nga.m_startNodeStack.clone();
            changesMade = true;
        } else {
            changesMade = true;
            // d) two different stacks. Merge them but make sure common IDs appear in the same order in both.
            Stack<NodeID> newStack = new Stack<NodeID>();
            Stack<NodeID>[] oldStacks = new Stack[]{m_startNodeStack, nga.m_startNodeStack};
            int[] pointer = new int[]{0, 0};
            do {
                if (oldStacks[0].get(pointer[0]).equals(oldStacks[1].get(pointer[1]))) {
                    // found same element, add one and increase both pointers:
                    newStack.add(oldStacks[0].get(pointer[0]));
                    pointer[0]++;
                    pointer[1]++;
                }
                int addRestOf = -1;
                NodeID addUntil = null;
                if (oldStacks[0].size() <= pointer[0]) {
                    // stack 0 is done, add rest of stack 1
                    addRestOf = 1;
                } else if (oldStacks[1].size() <= pointer[1]) {
                    // stack 1 is done, add rest of stack 0
                    addRestOf = 0;
                } else if (oldStacks[0].indexOf(oldStacks[1].get(pointer[1]), pointer[0]) != -1) {
                    // stack 0 contains "next" element of stack 1 further down the list, add those until we reach it
                    addRestOf = 0;
                    addUntil = oldStacks[1].get(pointer[1]);
                } else if (oldStacks[1].indexOf(oldStacks[0].get(pointer[0]), pointer[1]) != -1) {
                    // stack 1 contains "next" element of stack 0 further down the list, add those until we reach it
                    addRestOf = 1;
                    addUntil = oldStacks[0].get(pointer[0]);
                } else {
                    m_error = "Node can not be part of different (nested) loops.";
                    m_startNodeStack = newStack;
                    return changesMade;
                }
                // now add the rest until we reach the end of the given element
                while ((oldStacks[addRestOf].size() > pointer[addRestOf])
                        && !oldStacks[addRestOf].get(pointer[addRestOf]).equals(addUntil)) {
                    newStack.add(oldStacks[addRestOf].get(pointer[addRestOf]));
                    pointer[addRestOf]++;
                }
            } while (oldStacks[0].size() > pointer[0] && oldStacks[1].size() > pointer[1]);
            m_startNodeStack = newStack;
        }
        if (m_startNodeStack.size() > 0 && (m_role.equals(Role.END))) {
            m_error = null;  // now we have minimum one element!
        }
        return changesMade;
    }

    /** Set backwards stack using the NGAs from all connected nodes.
     *
     * @param ngas NodeGraphAnnocation from the node connected to each outport.
     */
    void setAndMergeBackwards(final Set<NodeGraphAnnotation> ngas) {
        if (ngas.size() == 0) {
            m_endNodeStack = new Stack<NodeID>();
        } else {
            Iterator<NodeGraphAnnotation> it = ngas.iterator();
            assert it.hasNext();
            NodeGraphAnnotation nga = it.next();
            m_endNodeStack = nga.cloneScopeStackForPredeccessor();
            m_connectedMetaOutPorts.addAll(nga.m_connectedMetaOutPorts);
            while (it.hasNext()) {
                nga = it.next();
                Stack<NodeID> backScopes2 = nga.cloneScopeStackForPredeccessor();
                // merge the two stacks. In principle this is easy: we only consider the intersection of
                // the two stacks and the elements have to appear in exactly the same order. There
                // are a few border cases to check first, though:
                // 1) check if one of the two is a side branch, leaving a loop:
                if ((m_endNodeStack.size() == 0) && m_connectedMetaOutPorts.size() == 0) {
                    // current node is part of side branch use info from the other node
                    m_endNodeStack = backScopes2;
                } else if ((backScopes2.size() == 0) && (nga.m_connectedMetaOutPorts.size() == 0)) {
                    // previous node is part of side branch - use info from the other node
                    // m_endNodeStack already correctly assigned.
                } else if (m_endNodeStack.size() == 0 || backScopes2.size() == 0) {
                    // 2) if one of the two stacks is empty but the ports aren't: empty intersection!
                    //    (this will later result in a failure)
                    m_endNodeStack = new Stack<NodeID>();
                } else if (m_endNodeStack.equals(backScopes2)) {
                    // 3) do nothing, stack the same in both nodes
                } else {
                    // This should only happen if the top of the two stacks are exactly the same
                    // (otherwise the wiring is incorrect as one branch skips over an end node!)
                    Stack<NodeID> newBackwardScopes = new Stack<NodeID>();
                    for (int i = 0; i < Math.min(m_endNodeStack.size(), backScopes2.size()); i++) {
                        if (m_endNodeStack.get(i).equals(backScopes2.get(i))) {
                            newBackwardScopes.add(m_endNodeStack.get(i));
                        } else {
                            m_error = "More than one Scope End node connected to this node.";
                            break;
                        }
                    }
                    m_endNodeStack = newBackwardScopes;
                }
                // and finally merge the connected output ports of the enclosing metanodes: the the union.
                m_connectedMetaOutPorts.addAll(nga.m_connectedMetaOutPorts);
            }
        }
        if (m_role.equals(Role.END)) {
            m_endNodeStack.push(m_id);
        }
        if (m_role.equals(Role.START)) {
            if (m_endNodeStack.size() < 1) {
                m_error = "Missing End Node.";
            }
        }
    }

    /** @return cloned copy of stack (remove top Start-ID if this is a ScopeEnd node.) */
    private Stack<NodeID> cloneScopeStackForSuccessor() {
        Stack<NodeID> st = (Stack<NodeID>)m_startNodeStack.clone();
        if ((m_role == Role.END) && (st.size() > 0)) {
            st.pop();
        }
        return st;
    }

    /** @return cloned copy of stack (remove top End-ID if this is a ScopeStart node.) */
    private Stack<NodeID> cloneScopeStackForPredeccessor() {
        Stack<NodeID> st = (Stack<NodeID>)m_endNodeStack.clone();
        if ((m_role == Role.START) && (st.size() > 0)) {
            st.pop();
        }
        return st;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final NodeGraphAnnotation o2) {
        return (Integer.valueOf(this.m_depth).compareTo(o2.m_depth));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        NodeGraphAnnotation nga = ((NodeGraphAnnotation)obj);
        return     m_id.equals(nga.m_id)
                && (m_outportIndex == nga.m_outportIndex)
                && (m_depth ==  nga.m_depth)
                && (m_role == nga.m_role)
                && (m_error != null && m_error.equals(nga.m_error))
                && (m_connectedMetaInPorts != null && m_connectedMetaInPorts.equals(nga.m_connectedMetaInPorts))
                && (m_startNodeStack != null && m_startNodeStack.equals(nga.m_startNodeStack))
                && (m_connectedMetaOutPorts != null && m_connectedMetaOutPorts.equals(nga.m_connectedMetaOutPorts))
                && (m_endNodeStack != null && m_endNodeStack.equals(nga.m_endNodeStack));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_id.hashCode() + m_outportIndex;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ID=" + m_id + "; P=" + m_outportIndex + "; D=" + m_depth + "; S=" + m_startNodeStack + "; E='"
                + (m_error == null ? "ok" : m_error) + "'";
    }

}
