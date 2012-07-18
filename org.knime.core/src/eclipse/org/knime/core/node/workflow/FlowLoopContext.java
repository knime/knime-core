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
 *   14.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

import org.knime.core.data.filestore.internal.ILoopStartWriteFileStoreHandler;
import org.knime.core.node.util.ConvenienceMethods;



/**
 * Special {@link FlowObject} holding loop information.
 *
 * @author M. Berthold, University of Konstanz
 */
public class FlowLoopContext extends FlowObject {

    private int m_iterationIndex = 0;
    private ILoopStartWriteFileStoreHandler m_fileStoreHandler;
    private NodeID m_tailNode;

    public NodeID getHeadNode() {
        return super.getOwner();
    }

    public void setTailNode(final NodeID tail) {
        m_tailNode = tail;
    }

    public NodeID getTailNode() {
        return m_tailNode;
    }

    /** The current iteration index, incremented after each loop iteration.
     * @return the iterationIndex
     * @noreference This method is not intended to be referenced by clients. */
    public int getIterationIndex() {
        return m_iterationIndex;
    }

    /** increment iteration index and return new value.
     * @return new iteration index.
     * @noreference This method is not intended to be referenced by clients. */
    public final int incrementIterationIndex() {
        return ++m_iterationIndex;
    }

    /** {@inheritDoc} */
    @Override
    protected FlowObject cloneAndUnsetOwner() {
        FlowLoopContext clone = (FlowLoopContext)super.cloneAndUnsetOwner();
        clone.setTailNode(null);
        clone.m_iterationIndex = 0;
        clone.m_fileStoreHandler = null;
        return clone;
    }

    /** @param fileStoreHandler the fileStoreHandler to set */
    void setFileStoreHandler(final ILoopStartWriteFileStoreHandler fileStoreHandler) {
        m_fileStoreHandler = fileStoreHandler;
    }

    /** @return the fileStoreHandler */
    ILoopStartWriteFileStoreHandler getFileStoreHandler() {
        return m_fileStoreHandler;
    }

    String getClassSummary() {
        return "Loop Context";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("<");
        b.append(getClassSummary());
        b.append(" (Head ").append(getHeadNode());
        NodeID tailNode = getTailNode();
        b.append(", Tail ");
        if (tailNode == null) {
            b.append("unassigned");
        } else {
            b.append(tailNode);
        }
        b.append(")>");
        b.append(" - iteration ").append(getIterationIndex());
        return b.toString();
    }

    /** Executed start nodes will use this object during workflow load to
     * indicate that a loop was potentially saved in a half-executed state. */
    public static class RestoredFlowLoopContext extends FlowLoopContext {

        // marker class

        /** {@inheritDoc} */
        @Override
        String getClassSummary() {
            return "Restored Loop Context";
        }

    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (m_tailNode == null) {
            return super.hashCode();
        }
        return super.hashCode() + m_tailNode.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        FlowLoopContext flc = (FlowLoopContext) obj;
        return super.equals(obj)
            && ConvenienceMethods.areEqual(flc.m_tailNode, m_tailNode);
    }
}
