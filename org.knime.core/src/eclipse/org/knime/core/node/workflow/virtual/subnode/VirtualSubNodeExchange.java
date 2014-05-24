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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 31, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.subnode;

import java.util.Collection;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Utility object that represents the output objects of virtual output nodes.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class VirtualSubNodeExchange {

    private final PortObjectSpec[] m_portSpecs;
    private final PortObject[] m_portObjects;
    private final Collection<FlowVariable> m_flowVariables;

    /** Called when subnode's input changes (configure).
     * @param inputSpecs  ... (may be null or contain null elements)
     * @param unfilteredFlowVariables Input stack, to be filtered.
     */
    public VirtualSubNodeExchange(final PortObjectSpec[] inputSpecs,
        final Collection<FlowVariable> unfilteredFlowVariables) {
        this(inputSpecs, null, unfilteredFlowVariables);
    }

    /** Called during execution of subnode.
     * @param portObjects Input objects must not be null. May contain null only if input is optional.
     * @param flowVariables Input stack, to be filtered.
     */
    public VirtualSubNodeExchange(final PortObject[] portObjects,
        final Collection<FlowVariable> flowVariables) {
        this(specsFromPortObjects(portObjects), portObjects, flowVariables);
    }

    private  VirtualSubNodeExchange(final PortObjectSpec[] portSpecs, final PortObject[] portObjects,
        final Collection<FlowVariable> flowVariables) {
        m_portSpecs = portSpecs;
        m_portObjects = portObjects;
        m_flowVariables = flowVariables;
    }

    private static PortObjectSpec[] specsFromPortObjects(final PortObject[] portObjects) {
        CheckUtils.checkNotNull(portObjects, "PortObject[] must not be null");
        PortObjectSpec[] result = new PortObjectSpec[portObjects.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = portObjects[i] == null ? null : portObjects[i].getSpec();
        }
        return result;
    }

    /**
     * @return the port objects or null
     */
    public PortObject[] getPortObjects() {
        return m_portObjects;
    }

    /**
     * @return the specs or null
     */
    public PortObjectSpec[] getPortSpecs() {
        return m_portSpecs;
    }

    /**
     * @return the flowVariables
     */
    public Collection<FlowVariable> getFlowVariables() {
        return m_flowVariables;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (m_portSpecs == null) {
            b.append("<No Specs>\n");
        } else {
            b.append("Specs:\n");
            for (PortObjectSpec s : m_portSpecs) {
                b.append("  ").append(s == null ? "<null>" : s.toString()).append('\n');
            }
        }
        if (m_portObjects == null) {
            b.append("<No Objects>\n");
        } else {
            b.append("Objects:\n");
            for (PortObject o : m_portObjects) {
                b.append("  ").append(o == null ? "<null>" : o.getSummary()).append('\n');
            }
        }
        if (m_flowVariables == null) {
            b.append("<No Variables>\n");
        } else {
            b.append("Variables:\n");
            for (FlowVariable v : m_flowVariables) {
                b.append("  ").append(v.toString()).append('\n');
            }
        }
        return b.toString();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder();
        b.append(m_portSpecs).append(m_portObjects).append(m_flowVariables);
        return b.toHashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VirtualSubNodeExchange)) {
            return false;
        }
        VirtualSubNodeExchange other = (VirtualSubNodeExchange)obj;
        return new EqualsBuilder().append(m_portSpecs, other.m_portSpecs)
            .append(m_portObjects, other.m_portObjects).append(m_flowVariables, other.m_flowVariables).isEquals();
    }

}
