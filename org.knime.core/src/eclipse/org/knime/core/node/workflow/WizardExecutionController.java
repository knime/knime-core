/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * Created on Jan 29, 2014 by wiswedel
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.web.DefaultWebTemplate;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.WizardNode;

/**
 * A utility class received from the workflow manager that allows to step back and forth in a wizard execution.
 *
 * <p>Do not use, no public API.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Christian Albrecht, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public final class WizardExecutionController {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WizardExecutionController.class);

    /** Host WFM */
    private final WorkflowManager m_manager;
    /** Lazy initialized breadth first sorted list of subnodes containing wizard nodes. */
    private List<NodeID> m_subNodesWithWizardNodesList;
    /**  The pointer in the subnode list representing the subnode to show next. */
    private int m_levelIndex;

    /** Created from workflow.
     * @param manager ...
     */
    WizardExecutionController(final WorkflowManager manager) {
        CheckUtils.checkArgumentNotNull(manager, "Argument must not be null");
        m_manager = manager;
    }


    private void lazyInit() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        if (m_subNodesWithWizardNodesList == null) {
            LOGGER.debugWithFormat("Indexing wizard nodes for workflow %s", m_manager.getNameWithID());
            m_subNodesWithWizardNodesList = new ArrayList<NodeID>();
            m_levelIndex = 0;
            final Workflow workflow = m_manager.getWorkflow();
            LinkedHashMap<NodeID, Set<Integer>> breadthFirstSortedList =
                    workflow.createBreadthFirstSortedList(workflow.getNodeIDs(), true);
            for (Map.Entry<NodeID, Set<Integer>> entry : breadthFirstSortedList.entrySet()) {
                NodeContainer nc = workflow.getNode(entry.getKey());
                if (nc instanceof SubNodeContainer) {
                    SubNodeContainer subnodeNC = (SubNodeContainer)nc;
                    WorkflowManager subnodeMgr = subnodeNC.getWorkflowManager();
                    @SuppressWarnings("rawtypes")
                    final Map<NodeID, WizardNode> wizardNodes = subnodeMgr.findNodes(WizardNode.class, false);
                    if (!wizardNodes.isEmpty()) {
                        m_subNodesWithWizardNodesList.add(entry.getKey());
                    }
                }
            }
            LOGGER.debugWithFormat("Indexing for worklow %s complete, found wizard sub node(s) %s",
                m_manager.getNameWithID(), ConvenienceMethods.getShortStringFrom(m_subNodesWithWizardNodesList, 8));
        }
    }

    public Map<String, ValidationError> loadValuesIntoCurrentPage(final Map<String, WebViewContent> viewContentMap) {
        return Collections.emptyMap();
    }

    public void stepNext() {
        synchronized (m_manager.getWorkflowMutex()) {
            NodeContext.pushContext(m_manager);
            try {
                stepNextInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    public void stepBack() {

    }

    public boolean hasNextWizardPage() {
        return false;
    }

    public boolean hasPreviousWizardPage() {
        return false;
    }

    private void stepNextInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        lazyInit();
        if (m_levelIndex < m_subNodesWithWizardNodesList.size()) {
            LOGGER.debugWithFormat("Stepping wizard execution to sub node %s (step %d/%d)",
                m_subNodesWithWizardNodesList.get(m_levelIndex), m_levelIndex, m_subNodesWithWizardNodesList.size());
            m_manager.executeUpToHere(m_subNodesWithWizardNodesList.get(m_levelIndex++));
        }
    }

    public WizardPageContent getCurrentWizardPage() {
        return null;
    }

    private static final String ID_WEB_RES = "org.knime.js.core.webResources";

    private static final String ID_JS_COMP = "org.knime.js.core.javascriptComponents";

    private static final String ID_IMPL_BUNDLE = "implementationBundleID";

    private static final String ID_IMPORT_RES = "importResource";

    private static final String ID_DEPENDENCY = "webDependency";

    private static final String ATTR_JS_ID = "jsObjectID";

    private static final String ATTR_RES_BUNDLE_ID = "webResourceBundleID";

    private static final String ATTR_PATH = "relativePath";

    private static final String ATTR_TYPE = "type";

    /**
     * @param jsObjectID The JavaScript object ID used for locating the extension point.
     * @return A template object, being used to assamble views.
     */
    public static WebTemplate getWebTemplateFromJSObjectID(final String jsObjectID) {
        List<WebResourceLocator> webResList = new ArrayList<WebResourceLocator>();
        IConfigurationElement jsComponentExtension = getConfigurationFromID(ID_JS_COMP, ATTR_JS_ID, jsObjectID);
        String bundleID = jsComponentExtension.getAttribute(ID_IMPL_BUNDLE);
        IConfigurationElement implementationExtension =
            getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, bundleID);
        List<WebResourceLocator> implementationRes = getResourcesFromExtension(implementationExtension);
        for (IConfigurationElement dependencyConf : jsComponentExtension.getChildren(ID_DEPENDENCY)) {
            String dependencyID = dependencyConf.getAttribute(ATTR_RES_BUNDLE_ID);
            IConfigurationElement dependencyExtension = getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, dependencyID);
            List<WebResourceLocator> dependencyRes = getResourcesFromExtension(dependencyExtension);
            webResList.addAll(dependencyRes);
        }
        webResList.addAll(implementationRes);
        return new DefaultWebTemplate(webResList.toArray(new WebResourceLocator[0]), null, jsObjectID);
    }

    private static IConfigurationElement getConfigurationFromID(final String extensionPointId,
        final String configurationID, final String jsObjectID) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] configurationElements = registry.getConfigurationElementsFor(extensionPointId);
        for (IConfigurationElement element : configurationElements) {
            if (jsObjectID.equals(element.getAttribute(configurationID))) {
                return element;
            }
        }
        return null;
    }

    private static List<WebResourceLocator> getResourcesFromExtension(final IConfigurationElement resConfig) {
        String pluginName = resConfig.getContributor().getName();
        List<WebResourceLocator> locators = new ArrayList<WebResourceLocator>();
        for (IConfigurationElement resElement : resConfig.getChildren(ID_IMPORT_RES)) {
            String path = resElement.getAttribute(ATTR_PATH);
            String type = resElement.getAttribute(ATTR_TYPE);
            if (path != null && type != null) {
                WebResourceType resType = WebResourceType.FILE;
                if (type.equalsIgnoreCase("javascript")) {
                    resType = WebResourceType.JAVASCRIPT;
                } else if (type.equalsIgnoreCase("css")) {
                    resType = WebResourceType.CSS;
                }
                locators.add(new WebResourceLocator(pluginName, path, resType));
            }
        }
        return locators;
    }

    /** Result value of {@link WizardExecutionController#getCurrentWizardPage()}. */
    public static final class WizardPageContent {

        @SuppressWarnings("rawtypes")
        private final Map<String, WizardNode> m_pageMap;

        /**
         * @param pageMap
         */
        @SuppressWarnings("rawtypes")
        WizardPageContent(final Map<String, WizardNode> pageMap) {
            m_pageMap = pageMap;
        }

        /**
         * @return the pageMap
         */
        @SuppressWarnings("rawtypes")
        public Map<String, WizardNode> getPageMap() {
            return m_pageMap;
        }

    }

    /** Utility class that only stores the workflow relative NodeID path. If the NodeID of the workflow is
     * 0:3 and the quickforms in there are 0:3:1:1 and 0:3:1:2 then it only saves {1,1} and {1,2}. We must not
     * save the wfm ID with the NodeIDs as those may change when the workflow is swapped out/read back in.
     * See also bug 4478.
     */
    static final class NodeIDSuffix {
        /* This class makes com.knime.enterprise.server.WorkflowInstance.NodeIDSuffix obsolete. */

        private final int[] m_suffixes;

        /** @param suffixes ... */
        NodeIDSuffix(final int[] suffixes) {
            m_suffixes = suffixes;
        }

        /** Create the suffix object by cutting the parentID from the argument nodeID.
         * @param parentID ...
         * @param nodeID ..
         * @return The extracted suffix object
         * @throws IllegalArgumentException If the parentID is not a prefix of the nodeID.
         *
         */
        static NodeIDSuffix create(final NodeID parentID, final NodeID nodeID) {
            if (!nodeID.hasPrefix(parentID)) {
                throw new IllegalArgumentException("The argument node ID \"" + nodeID
                    + "\" does not have the expected parent prefix \"" + parentID + "\"");
            }
            List<Integer> suffixList = new ArrayList<Integer>();
            NodeID traverse = nodeID;
            do {
                suffixList.add(traverse.getIndex());
                traverse = traverse.getPrefix();
            } while (!parentID.equals(traverse));
            Collections.reverse(suffixList);
            return new NodeIDSuffix(ArrayUtils.toPrimitive(suffixList.toArray(new Integer[suffixList.size()])));
        }

        /** Reverse operation to {@link #create(NodeID, NodeID)}. Prepends the parentID to this suffix and
         * returns a valid (new) NodeID.
         * @param parentID ...
         * @return ...
         */
        NodeID prependParent(final NodeID parentID) {
            NodeID result = parentID;
            for (int i : m_suffixes) {
                result = new NodeID(result, i);
            }
            return result;
        }

        /** Utility function to convert a set of suffixes into a set of IDs. */
        static Set<NodeID> toNodeIDSet(final NodeID parentID, final Set<NodeIDSuffix> suffixSet) {
            LinkedHashSet<NodeID> resultSet = new LinkedHashSet<NodeID>();
            for (NodeIDSuffix sID: suffixSet) {
                resultSet.add(sID.prependParent(parentID));
            }
            return resultSet;
        }

        /** Utility function to convert a set of IDs into a set of suffixes. */
        static Set<NodeIDSuffix> fromNodeIDSet(final NodeID parentID, final Set<NodeID> idSet) {
            LinkedHashSet<NodeIDSuffix> resultSet = new LinkedHashSet<NodeIDSuffix>();
            for (NodeID id: idSet) {
                resultSet.add(NodeIDSuffix.create(parentID, id));
            }
            return resultSet;
        }

        /** @return the stored indices - used by the persistor. */
        int[] getSuffixes() {
            return m_suffixes;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return Arrays.toString(m_suffixes);
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return Arrays.hashCode(m_suffixes);
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof NodeIDSuffix)) {
                return false;
            }
            return Arrays.equals(m_suffixes, ((NodeIDSuffix)obj).m_suffixes);
        }

    }

}
