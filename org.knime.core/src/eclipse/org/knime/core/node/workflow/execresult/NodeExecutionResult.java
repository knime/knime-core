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
 *   Jan 8, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow.execresult;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.NodeContentPersistor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class NodeExecutionResult implements NodeContentPersistor {

    private final PortObject[] m_internalHeldPortObjects;
    private final ReferencedFile m_nodeInternDir;
    private final PortObject[] m_portObjects;
    private final PortObjectSpec[] m_portObjectSpecs;
    private final String m_warningMessage;
    private final boolean m_needsResetAfterLoad;
    private final List<FlowVariable> m_flowVariables;


    /**
     * Copy constructor.
     *
     * @param toCopy The instance that should be copied.
     * @since 3.5
     */
    public NodeExecutionResult(final NodeExecutionResult toCopy) {
        m_internalHeldPortObjects = ArrayUtils.clone(toCopy.m_internalHeldPortObjects);
        m_nodeInternDir = toCopy.m_nodeInternDir;
        m_portObjects = ArrayUtils.clone(toCopy.m_portObjects);
        m_portObjectSpecs = ArrayUtils.clone(toCopy.m_portObjectSpecs);
        m_warningMessage = toCopy.m_warningMessage;
        m_needsResetAfterLoad = toCopy.m_needsResetAfterLoad;
        m_flowVariables = toCopy.m_flowVariables;
    }

    /**
     * @param builder
     */
    public NodeExecutionResult(final NodeExecutionResultBuilder builder) {
        m_internalHeldPortObjects = builder.m_internalHeldPortObjects;
        m_nodeInternDir = builder.m_nodeInternDir;
        m_portObjects = builder.m_portObjects;
        m_portObjectSpecs = builder.m_portObjectSpecs;
        m_warningMessage = builder.m_warningMessage;
        m_needsResetAfterLoad = builder.m_needsResetAfterLoad;
        m_flowVariables = builder.m_flowVariables;
    }

    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    public PortObject[] getInternalHeldPortObjects() {
        return m_internalHeldPortObjects;
    }

    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    public ReferencedFile getNodeInternDirectory() {
        return m_nodeInternDir;
    }

    /**
     * @return The number of port objects held by this execution result.
     * @see #getPortObject(int)
     * @since 3.5
     */
    @JsonProperty("nrPortObjects")
    public int getNrOfPortObjects() {
        return ArrayUtils.getLength(m_portObjects);
    }

    @Override
    public void setNeedsResetAfterLoad() {
        // TODO remove
    }

    /**
     *
     * @return The number of internal port objects held by this execution result.
     * @see #getInternalHeldPortObjects()
     * @since 3.5
     */
    @JsonProperty("nrInternalPortObjects")
    public int getNrOfInternalHeldPortObjects() {
        return ArrayUtils.getLength(m_internalHeldPortObjects);
    }

    /** {@inheritDoc} */
    @Override
    public PortObject getPortObject(final int outportIndex) {
        return m_portObjects[outportIndex];
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec getPortObjectSpec(final int outportIndex) {
        return m_portObjectSpecs[outportIndex];
    }

    /** {@inheritDoc} */
    @Override
    public String getPortObjectSummary(final int outportIndex) {
        return m_portObjects[outportIndex].getSummary();
    }

    /**
     * @return A list of saved flow variables that where pushed by the node
     * (or an empty object if the persistor doesn't save the result.)
     * @since 3.5
     */
    @JsonIgnore
    public Optional<List<FlowVariable>> getFlowVariables() {
        return Optional.ofNullable(m_flowVariables);
    }


    /** {@inheritDoc} */
    @Override
    @JsonProperty("warningMsg")
    public String getWarningMessage() {
        return m_warningMessage;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @JsonProperty("needsResetAfterLoad")
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    public boolean hasContent() {
        return m_nodeInternDir != null;
    }

    /** {@inheritDoc}
     * @since 2.6*/
    @Override
    @JsonIgnore
    public IFileStoreHandler getFileStoreHandler() {
        return null;
    }

    /**
     * Factory method for JSON deserialization.
     *
     * @param nrOfPortObjects
     * @param nrOfInternalPortObjects
     * @param needsResetAfterLoad
     * @param warningMessage
     * @return An execution result where port object (spec) arrays are initialized, but contain null values.
     * @since 3.3
     * @noreference This method is not intended to be referenced by clients.
     */
    @JsonCreator
    private static NodeExecutionResult createEmptyNodeExecutionResult(
        @JsonProperty("nrPortObjects") final int nrOfPortObjects,
        @JsonProperty("nrInternalPortObjects") final int nrOfInternalPortObjects,
        @JsonProperty("needsResetAfterLoad") final boolean needsResetAfterLoad,
        @JsonProperty("warningMsg") final String warningMessage) {

        final NodeExecutionResultBuilder toReturn = NodeExecutionResult.builder() //
                .setPortObjects(new PortObject[nrOfPortObjects]) //
                .setPortObjectSpecs(new PortObjectSpec[nrOfPortObjects]) //
                .setInternalHeldPortObjects(new PortObject[nrOfInternalPortObjects]) //
                .setNeedsResetAfterLoad(needsResetAfterLoad) //
                .setWarningMessage(warningMessage);
        return toReturn.build();
    }

    public static NodeExecutionResultBuilder builder() {
        return new NodeExecutionResultBuilder();
    }

    public static NodeExecutionResultBuilder builder(final NodeExecutionResult template) {
        NodeExecutionResultBuilder result = new NodeExecutionResultBuilder();
        result.m_internalHeldPortObjects = template.m_internalHeldPortObjects;
        result.m_nodeInternDir = template.m_nodeInternDir;
        result.m_portObjects = template.m_portObjects;
        result.m_portObjectSpecs = template.m_portObjectSpecs;
        result.m_warningMessage = template.m_warningMessage;
        result.m_needsResetAfterLoad = template.m_needsResetAfterLoad;
        result.m_flowVariables = template.m_flowVariables;
        return result;
    }

    public static final class NodeExecutionResultBuilder {

        private PortObject[] m_internalHeldPortObjects;
        private ReferencedFile m_nodeInternDir;
        private PortObject[] m_portObjects;
        private PortObjectSpec[] m_portObjectSpecs;
        private String m_warningMessage;
        private boolean m_needsResetAfterLoad;
        private List<FlowVariable> m_flowVariables;

        NodeExecutionResultBuilder() {
        }

        /**
         * @param internalHeldTables the internalHeldTables to set
         * @return this
         */
        public NodeExecutionResultBuilder setInternalHeldPortObjects(
                final PortObject[] internalHeldTables) {
            m_internalHeldPortObjects = internalHeldTables;
            return this;
        }

        /**
         * @param nodeInternDir the referencedFile to set
         * @return this
         */
        public NodeExecutionResultBuilder setNodeInternDir(final ReferencedFile nodeInternDir) {
            m_nodeInternDir = nodeInternDir;
            return this;
        }

        /**
         * @param warningMessage the warningMessage to set
         * @return this
         */
        public NodeExecutionResultBuilder setWarningMessage(final String warningMessage) {
            m_warningMessage = warningMessage;
            return this;
        }

        /**
         * @param portObjects the portObjects to set
         * @return this
         */
        public NodeExecutionResultBuilder setPortObjects(final PortObject[] portObjects) {
            m_portObjects = portObjects;
            return this;
        }

        /**
         * @param portObjectSpecs the portObjectSpecs to set
         * @return this
         */
        public NodeExecutionResultBuilder setPortObjectSpecs(final PortObjectSpec[] portObjectSpecs) {
            m_portObjectSpecs = portObjectSpecs;
            return this;
        }

        /**
         * Saves flow variables that where pushed by the node.
         *
         * @param flowVariables A ordered list of flow variables.
         * @return this
         * @since 3.5 */
        public NodeExecutionResultBuilder setFlowVariables(final List<FlowVariable> flowVariables) {
            m_flowVariables = flowVariables;
            return this;
        }

        /**
         * @param needsResetAfterLoad the needsResetAfterLoad to set
         * @return this
         */
        public NodeExecutionResultBuilder setNeedsResetAfterLoad(final boolean needsResetAfterLoad) {
            m_needsResetAfterLoad = needsResetAfterLoad;
            return this;
        }

        /**
         * Sets all ports to inactive.
         * @return this
         *
         * @since 3.5
         */
        public NodeExecutionResultBuilder setInactive() {
            m_nodeInternDir = null;
            if (m_portObjects != null) {
                Arrays.fill(m_portObjects, InactiveBranchPortObject.INSTANCE);
                Arrays.fill(m_portObjectSpecs, InactiveBranchPortObjectSpec.INSTANCE);
            }
            if (m_internalHeldPortObjects != null) {
                Arrays.fill(m_internalHeldPortObjects, InactiveBranchPortObject.INSTANCE);
            }
            return this;
        }

        /**
         * @return Constructed final object.
         */
        public NodeExecutionResult build() {
            return new NodeExecutionResult(this);
        }

    }

}
