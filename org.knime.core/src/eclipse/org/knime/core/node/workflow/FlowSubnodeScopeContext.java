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
 *   Sep 5, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.util.CheckUtils;

/** A scope context used by the subnode to have all contained nodes inherit an inactive flag.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class FlowSubnodeScopeContext extends FlowScopeContext {

    private SubNodeContainer m_snc;

    FlowSubnodeScopeContext(final SubNodeContainer snc) {
        m_snc = CheckUtils.checkArgumentNotNull(snc, "Owning SNC must not be null");
        setOwner(snc.getID());
    }

    /** Get the flow loop context this subnode is part of or null if not in a loop. It also unwraps if this subnode
     * is nested.
     * @return flow loop context or null
     */
    <C extends FlowScopeContext> C getOuterFlowScopeContext(final Class<C> contextClass) {
        FlowObjectStack flowObjectStack = m_snc.getFlowObjectStack();
        C flowScopeContext = flowObjectStack.peek(contextClass);
        if (flowScopeContext != null) {
            return flowScopeContext;
        }
        FlowSubnodeScopeContext outerSubnodeScope = flowObjectStack.peek(FlowSubnodeScopeContext.class);
        if (outerSubnodeScope != null) {
            return outerSubnodeScope.getOuterFlowScopeContext(contextClass);
        }
        return null;
    }

    SubNodeContainer getSubNodeContainer() {
        CheckUtils.checkState(m_snc != null, "Subnode container has been disposed");
        return m_snc;
    }

    /**
     * Added as part of AP-23380 - NodeModel instance might leak (bugs in 3rd party code), and hold on to their flow
     * variable stack, which might contain objects of this type.
     */
    void cleanup() {
        m_snc = null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Component Context";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        return super.equals(obj);
    }

}
