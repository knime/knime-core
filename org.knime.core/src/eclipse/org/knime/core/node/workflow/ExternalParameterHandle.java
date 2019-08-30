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
 *   Oct 16, 2017 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.util.CheckUtils;

/**
 * Class used internally in the {@link WorkflowManager} to describe the relevant pieces of a node that is used as a
 * external parameter (e.g. injected via REST). The parameter values can be, e.g., configuration values for
 * {@link DialogNode}s or input values for {@link InputNode}s. It wraps the parameterized value object along with the
 * parameter name, unified across all parameters in the workflow. The latter is important in case two nodes in the
 * workflow use the same parameter name ... it's suffixed with the node's ID.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
final class ExternalParameterHandle<V> {

    private final String m_parameterNameShort;

    private final String m_parameterNameFullyQualified;

    private final NativeNodeContainer m_ownerNodeContainer;

    private V m_parameterValue;

    /**
     * @param parameterNameShort e.g. "foo-bar" or "foo-bar-9:3"
     * @param parameterNameFullyQualified e.g. "foo-bar-9:3"
     * @param ownerNodeContainer e.g. the node that contributes or receives the REST parameter.
     * @param externalParameterValue the actual data, can be <code>null</code> if this handle is only used to set a new
     *            parameter value
     * @param parameterKey the unique id for the parameter (i.e. parameter name without node id)
     */
    ExternalParameterHandle(final String parameterNameShort, final String parameterNameFullyQualified,
        final NativeNodeContainer ownerNodeContainer, final V externalParameterValue, final String parameterKey) {
        m_parameterNameShort = CheckUtils.checkNotNull(parameterNameShort);
        m_parameterNameFullyQualified = CheckUtils.checkNotNull(parameterNameFullyQualified);
        CheckUtils.checkArgument(StringUtils.startsWith(parameterNameFullyQualified, parameterNameShort),
            "Fully qualified parameter name \"%s\" doesn't start with parameter name (short) \"%s\"",
            parameterNameFullyQualified, parameterNameShort);
        CheckUtils.checkArgument(ExternalNodeData.PARAMETER_NAME_PATTERN.matcher(parameterNameFullyQualified).matches(),
            "No match on \"%s\" (regex \"%s\")", parameterNameShort, ExternalNodeData.PARAMETER_NAME_PATTERN.pattern());

        m_ownerNodeContainer = CheckUtils.checkNotNull(ownerNodeContainer);
        m_parameterValue = externalParameterValue;
        CheckUtils.checkArgument(StringUtils.startsWith(parameterNameShort, parameterKey),
            "Parameter name \"%s\"doesn't start with name as configured in node \"%\"", m_parameterNameShort,
            parameterKey);
    }

    /** @return the parameterNameShort, not null. */
    String getParameterNameShort() {
        return m_parameterNameShort;
    }

    /** @return the parameterNameFullyQualified */
    String getParameterNameFullyQualified() {
        return m_parameterNameFullyQualified;
    }

    /** @return the ownerNodeContainer */
    NativeNodeContainer getOwnerNodeContainer() {
        return m_ownerNodeContainer;
    }

    /** @return the externalNodeData */
    V getParameterValue() {
        return m_parameterValue;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("parameterNameShort", m_parameterNameShort);
        builder.append("parameterNameFullyQualified", m_parameterNameFullyQualified);
        builder.append("ownerNodeContainer", m_ownerNodeContainer);
        builder.append("parameterValue", m_parameterValue);
        return builder.build();
    }

}
