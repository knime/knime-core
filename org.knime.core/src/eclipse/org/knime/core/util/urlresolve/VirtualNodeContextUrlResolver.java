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
 *   Jan 13, 2026 (hornm): created
 */
package org.knime.core.util.urlresolve;

import java.net.URL;
import java.util.Optional;

import org.eclipse.core.runtime.IPath;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.virtual.VirtualNodeContext;
import org.knime.core.node.workflow.virtual.VirtualNodeContext.Restriction;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;

/**
 * A URL resolver for a 'virtual workflow', i.e. one with a {@link VirtualNodeContext}.
 *
 * In most cases, the resolution is delegated to an {@link AnalyticsPlatformLocalUrlResolver} (strictly assuming that a
 * 'virtual workflow' context always has a {@link AnalyticsPlatformExecutorInfo}). Only the workflow-relative resolution
 * is adapted to take the virtual context into account. Either by not allowing it all together (depending on
 * {@link VirtualNodeContext#hasRestriction(Restriction)}), or by using the data-area as defined by the virtual context
 * (cp. {@link VirtualNodeContext#getVirtualDataAreaPath()}) - if the workflow-relative path points to the data-area.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.10
 */
final class VirtualNodeContextUrlResolver extends KnimeUrlResolver {

    private final KnimeUrlResolver m_delegate;

    VirtualNodeContextUrlResolver(final KnimeUrlResolver delegate) {
        m_delegate = delegate;
    }

    @Override
    Optional<ContextPaths> getContextPaths() {
        return Optional.empty();
    }

    @Override
    ResolvedURL resolveMountpointAbsolute(final URL url, final String mountId, final IPath path,
        final ItemVersion version) throws ResourceAccessException {
        return m_delegate.resolveMountpointAbsolute(url, mountId, path, version);
    }

    @Override
    ResolvedURL resolveMountpointRelative(final URL url, final IPath path, final ItemVersion version)
        throws ResourceAccessException {
        return m_delegate.resolveMountpointRelative(url, path, version);
    }

    @Override
    ResolvedURL resolveSpaceRelative(final URL url, final IPath path, final ItemVersion version)
        throws ResourceAccessException {
        return m_delegate.resolveSpaceRelative(url, path, version);
    }

    @Override
    ResolvedURL resolveWorkflowRelative(final URL url, final IPath path, final ItemVersion version)
        throws ResourceAccessException {
        final var resolved = m_delegate.resolveWorkflowRelative(url, path, version);
        var virtualNodeContext = VirtualNodeContext.getContext().orElseThrow();

        if (resolved.pathInsideWorkflow() != null && "data".equals(resolved.pathInsideWorkflow().segment(0))) {
            // data-area access in virtual context
            final var virtualDataAreaPath = virtualNodeContext.getVirtualDataAreaPath().orElse(null);
            if (virtualDataAreaPath == null) {
                if (virtualNodeContext.hasRestriction(Restriction.WORKFLOW_DATA_AREA_ACCESS)) {
                    throw new ResourceAccessException("Node is not allowed to access workflow data area "
                        + "because it's executed within a restricted (virtual) scope.");
                }
                return resolved;
            }

            final var pathInsideDataArea = resolved.pathInsideWorkflow().removeFirstSegments(1);
            final var resolvedInVirtualArea = virtualDataAreaPath.resolve(pathInsideDataArea.toOSString()).normalize();
            return new ResolvedURL(resolved.mountID(), resolved.path(), resolved.version(),
                resolved.pathInsideWorkflow(), URLResolverUtil.toURL(resolvedInVirtualArea),
                resolved.canBeRelativized());
        } else {
            if (virtualNodeContext.hasRestriction(Restriction.WORKFLOW_RELATIVE_RESOURCE_ACCESS)) {
                // not allowed to access workflow-relative resources at all
                throw new ResourceAccessException("Node is not allowed to access workflow-relative resources "
                    + "because it's executed within a restricted (virtual) scope.");
            }
            return resolved;
        }
    }

    @Override
    ResolvedURL resolveNodeRelative(final URL url, final IPath path) throws ResourceAccessException {
        throw new ResourceAccessException("Node-relative URL resolution is not supported in virtual node contexts.");
    }

}
