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
 *   May 27, 2025 (hornm): created
 */
package org.knime.core.node.workflow.virtual;

import java.nio.file.Path;
import java.util.Optional;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;

/**
 * If a node is part of a virtual scope (cp. {@link FlowVirtualScopeContext}), an instance of this interface is
 * available via {@link NodeContext#getContextObjectForClass(Class)}.
 *
 * It determines the conditions that apply in such a 'virtualized' node context.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noreference This method is not intended to be referenced by clients.
 */
public interface VirtualNodeContext {

    /**
     * Virtual context permission.
     */
    enum Restriction {
            /**
             * Access to mountpoint-, space-, workflow-, node-relative resources are is not allowed.
             */
            RELATIVE_RESOURCE_ACCESS,
            /**
             * Access to workflow data area is not allowed. But a virtual data area maybe provided via
             * #getVirtualDataAreaPath().
             */
            WORKFLOW_DATA_AREA_ACCESS,
    }

    /**
     * @param restriction the restriction to test
     * @return whether the given restriction applies in this virtual node context
     */
    boolean hasRestriction(final Restriction restriction);

    /**
     * @return the path to the workflow data area for this virtual context, if available (and if there is no restriction
     *         on data area access)
     */
    Optional<Path> getVirtualDataAreaPath();

    /**
     * Shortcut for {@code NodeContext.getContext().getContextObjectForClass(VirtualNodeContext.class)}.
     *
     * @return the {@link VirtualNodeContext} if available
     */
    static Optional<VirtualNodeContext> getContext() {
        return Optional.ofNullable(NodeContext.getContext())
            .flatMap(nodeContext -> nodeContext.getContextObjectForClass(VirtualNodeContext.class));
    }

}
