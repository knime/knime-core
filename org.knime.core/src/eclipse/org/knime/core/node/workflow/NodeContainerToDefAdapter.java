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
 *   May 20, 2021 (hornm): created
 */
package org.knime.core.node.workflow;

import java.util.Optional;

import org.knime.shared.workflow.def.BaseNodeDef;
import org.knime.shared.workflow.def.BoundsDef;
import org.knime.shared.workflow.def.JobManagerDef;
import org.knime.shared.workflow.def.NodeAnnotationDef;
import org.knime.shared.workflow.def.NodeLocksDef;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 *
 * @author hornm
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
public abstract class NodeContainerToDefAdapter implements BaseNodeDef {

    private NodeContainer m_nc;

    protected final PasswordRedactor m_passwordHandler;

    /**
     * @param nc
     * @param passwordHandler
     */
    protected NodeContainerToDefAdapter(final NodeContainer nc, final PasswordRedactor passwordHandler) {
        m_nc = nc;
        m_passwordHandler = passwordHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getCustomDescription() {
        return Optional.ofNullable(m_nc.getCustomDescription());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<NodeAnnotationDef> getAnnotation() {
        return Optional.ofNullable(CoreToDefUtil.toNodeAnnotationDef(m_nc.getNodeAnnotation()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BoundsDef> getBounds() {
        return Optional.ofNullable(m_nc.getUIInformation()).map(CoreToDefUtil::toBoundsDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<NodeLocksDef> getLocks() {
        return Optional.ofNullable(CoreToDefUtil.toNodeLocksDef(m_nc.getNodeLocks()));
    }

    /**
     * @return a def if a job manager is present, null otherwise
     */
    @Override
    public Optional<JobManagerDef> getJobManager() {
        return Optional.ofNullable(CoreToDefUtil.toJobManager(m_nc.getJobManager(), m_passwordHandler));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getId() {
        return m_nc.getID().getIndex();
    }

}
