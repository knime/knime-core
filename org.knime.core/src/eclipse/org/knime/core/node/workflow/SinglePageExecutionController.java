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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   24 Mar 2017 (albrecht): created
 */
package org.knime.core.node.workflow;

import java.util.Map;

import org.knime.core.node.web.ValidationError;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public class SinglePageExecutionController extends AbstractExecutionController {

    private final NodeID m_nodeID;

    /**
     * @param manager
     */
    SinglePageExecutionController(final WorkflowManager manager, final NodeID nodeID) {
        super(manager);
        m_nodeID = nodeID;
    }

    /**
     * Gets the wizard page for a given node id. Throws exception if no wizard page available.
     * @param subnodeID the node id for the subnode to retrieve the wizard page for
     * @return The wizard page for the given node id
     */
    public WizardPageContent getWizardPage() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            NodeContext.pushContext(manager);
            try {
                return getWizardPageInternal(m_nodeID);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * @param viewContentMap
     * @param subnodeID
     * @return
     */
    public Map<String, ValidationError> loadValuesIntoPage(final Map<String, String> viewContentMap) {
        return loadValuesIntoPage(viewContentMap, true, false);
    }

    /**
     * @param viewContentMap
     * @param subnodeID
     * @param validate
     * @param useAsDefault
     * @return
     */
    public Map<String, ValidationError> loadValuesIntoPage(final Map<String, String> viewContentMap, final boolean validate, final boolean useAsDefault) {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return loadValuesIntoPageInternal(viewContentMap, m_nodeID, validate, useAsDefault);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void checkNodeExecutedState(final SubNodeContainer snc, final NodeContainer destNC) throws IllegalStateException {
        /* no checks done here */
    }

    /**
     * @param viewContentMap
     * @param subnodeID
     * @return
     * @since 3.4
     */
    public Map<String, ValidationError> validateViewValuesInPage(final Map<String, String> viewContentMap) {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return validateViewValuesInternal(viewContentMap, m_nodeID, getWizardNodeSetForVerifiedID(m_nodeID));
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    public void reexecuteSinglePage() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                m_manager.executeUpToHere(m_nodeID);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

}
