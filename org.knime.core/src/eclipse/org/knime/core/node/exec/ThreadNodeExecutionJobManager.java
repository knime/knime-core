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
 *   Oct 10, 2008 (wiswedel): created
 */
package org.knime.core.node.exec;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;

/**
 * This job manager is the default for native nodes. For backwards compatibility it can also execute components, but is
 * not selectable in the component dialog (See {@link ThreadNodeExecutionJobManager#canExecute(NodeContainer)}.
 *
 * @author wiswedel, University of Konstanz
 */
public final class ThreadNodeExecutionJobManager extends AbstractThreadNodeExecutionJobManager {

    /**
     * Singleton instance of this job manager
     */
    static final ThreadNodeExecutionJobManager INSTANCE = new ThreadNodeExecutionJobManager();

    // Hide the implicit public constructor
    private ThreadNodeExecutionJobManager() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public LocalNodeExecutionJob createJob(final NodeContainer nc, final PortObject[] data) {
        if (!(nc instanceof SingleNodeContainer)) {
            throw new IllegalStateException(
                getClass().getSimpleName() + " is not able to execute a metanode: " + nc.getNameWithID());
        }
        return new LocalNodeExecutionJob((SingleNodeContainer)nc, data);
    }

    /** {@inheritDoc} */
    @Override
    public String getID() {
        return ThreadNodeExecutionJobManagerFactory.INSTANCE.getID();
    }

    /**
     * For backwards compatibility it can also execute components, but it returns false here in order not to be
     * selectable in a node's job manager configuration dialog tab (See
     * {@link ThreadNodeExecutionJobManager#canExecute(NodeContainer)}.
     */
    @Override
    public boolean canExecute(final NodeContainer nc) {
        // This job manager should only be advertised to native nodes.
        return nc instanceof NativeNodeContainer;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ThreadNodeExecutionJobManagerFactory.INSTANCE.getLabel();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefault() {
        return true;
    }
}
