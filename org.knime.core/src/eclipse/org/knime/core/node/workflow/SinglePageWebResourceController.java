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
import java.util.function.BiConsumer;

import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebViewContent;

/**
 * A utility class received from the workflow manager that allows controlling wizard execution and combined view creation on a single subnode.
 *
 * <p>Do not use, no public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public class SinglePageWebResourceController extends WebResourceController {

    private final NodeID m_nodeID;

    /**
     * @param manager
     * @param nodeID
     */
    public SinglePageWebResourceController(final WorkflowManager manager, final NodeID nodeID) {
        super(manager);
        m_nodeID = nodeID;
    }

    /**
     * Checks different criteria to determine if a combined page view is available for a given subnode.
     * @return true, if a view on the subnode is available, false otherwise
     */
    public boolean isSubnodeViewAvailable() {
        return super.isSubnodeViewAvailable(m_nodeID);
    }

    /**
     * Gets the wizard page for a given node id. Throws exception if no wizard page available.
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
     * Retrieves all available view values from the available wizard nodes for the given node id.
     * @return a map from NodeID to view value for all appropriate wizard nodes.
     */
    public Map<NodeID, WebViewContent> getWizardPageViewValueMap() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            NodeContext.pushContext(manager);
            try {
                return getWizardPageViewValueMapInternal(m_nodeID);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * Tries to load a map of view values to all appropriate views contained in the given subnode.
     * @param viewContentMap the values to load
     * @return Null or empty map if validation succeeds, map of errors otherwise
     */
    public Map<String, ValidationError> loadValuesIntoPage(final Map<String, String> viewContentMap) {
        return loadValuesIntoPage(viewContentMap, true, false);
    }

    /**
     * Tries to load a map of view values to all appropriate views contained in the given subnode.
     * @param viewContentMap the values to validate
     * @param validate true, if validation is supposed to be done before applying the values, false otherwise
     * @param useAsDefault true, if the given value map is supposed to be applied as new node defaults (overwrite node settings), false otherwise (apply temporarily)
     * @return Null or empty map if validation succeeds, map of errors otherwise
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

    /** {@inheritDoc} */
    @Override
    BiConsumer<SubNodeContainer, NodeContainer> createStateChecker() {
        return (snc, nc) -> {/* no checks done here */};
    }

    /**
     * Validates a given set of serialized view values for the given subnode.
     * @param viewContentMap the values to validate
     * @return Null or empty map if validation succeeds, map of errors otherwise
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

    /**
     * Triggers workflow execution up until the given subnode.
     */
    public void reexecuteSinglePage() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            m_manager.executeUpToHere(m_nodeID);
        }
    }

}
