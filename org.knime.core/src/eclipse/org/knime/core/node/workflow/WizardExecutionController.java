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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.web.DefaultWebTemplate;
import org.knime.core.node.web.JSONViewContent;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;

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

    /** Host WFM. */
    private final WorkflowManager m_manager;
    /** Lazy initialized breadth first sorted list of subnodes containing wizard nodes. */
    private List<NodeID> m_subNodesWithWizardNodesList;
    /**  The pointer in the subnode list representing the subnode to show next. */
    private int m_levelIndex;

    private static final String ID_WEB_RES = "org.knime.js.core.webResources";

    private static final String ID_JS_COMP = "org.knime.js.core.javascriptComponents";

    private static final String ID_IMPL_BUNDLE = "implementationBundleID";

    private static final String ID_IMPORT_RES = "importResource";

    private static final String ID_DEPENDENCY = "webDependency";

    private static final String ATTR_JS_ID = "jsObjectID";

    private static final String ATTR_NAMESPACE = "namespace";

    private static final String ATTR_RES_BUNDLE_ID = "webResourceBundleID";

    private static final String ATTR_PATH = "relativePath";

    private static final String ATTR_TYPE = "type";

    private static final String ATTR_INIT_METHOD_NAME = "init-method-name";

    private static final String ATTR_VALIDATE_METHOD_NAME = "validate-method-name";

    private static final String ATTR_GETCOMPONENTVALUE_METHOD_NAME = "getComponentValue-method-name";

    private static final String ID_WEB_RESOURCE = "webResource";

    private static final String ATTR_RELATIVE_PATH_SOURCE = "relativePathSource";

    private static final String ATTR_RELATIVE_PATH_TARGET = "relativePathTarget";

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
        String namespace = jsComponentExtension.getAttribute(ATTR_NAMESPACE);
        String initMethodName = jsComponentExtension.getAttribute(ATTR_INIT_METHOD_NAME);
        String validateMethodName = jsComponentExtension.getAttribute(ATTR_VALIDATE_METHOD_NAME);
        String valueMethodName = jsComponentExtension.getAttribute(ATTR_GETCOMPONENTVALUE_METHOD_NAME);
        return new DefaultWebTemplate(webResList.toArray(new WebResourceLocator[0]), namespace, initMethodName, validateMethodName, valueMethodName);
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

    private static Map<String, String> getWebResources(final IConfigurationElement resConfig) {
        Map<String, String> resMap = new HashMap<String, String>();
        for (IConfigurationElement resElement: resConfig.getChildren(ID_WEB_RESOURCE)) {
            resMap.put(resElement.getAttribute(ATTR_RELATIVE_PATH_TARGET), resElement.getAttribute(ATTR_RELATIVE_PATH_SOURCE));
        }
        return resMap;
    }

    private static List<WebResourceLocator> getResourcesFromExtension(final IConfigurationElement resConfig) {
        String pluginName = resConfig.getContributor().getName();
        List<WebResourceLocator> locators = new ArrayList<WebResourceLocator>();
        Map<String, String> resMap = getWebResources(resConfig);
        Set<String> imports = new HashSet<String>();
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
                String parent = path.substring(0, path.lastIndexOf('/') + 1);
                String newParent = resMap.get(parent);
                String sourcePath;
                if (newParent != null) {
                    sourcePath = path.replace(parent, newParent);
                } else {
                    sourcePath = path;
                }
                locators.add(new WebResourceLocator(pluginName, sourcePath, path, resType));
                imports.add(sourcePath);
            }
        }
        for (Entry<String, String> entry :resMap.entrySet()) {
            String targetPath = entry.getKey();
            String sourcePath = entry.getValue();
            try {
                URL url = new URL("platform:/plugin/" + pluginName);
                File dir = new File(FileLocator.resolve(url).getFile());
                File file = new File(dir, sourcePath);
                if (file.exists()) {
                    addLocators(pluginName, locators, imports, file, sourcePath, targetPath);
                }
            } catch (MalformedURLException e) {
                LOGGER.warn("", e);
            } catch (IOException e) {
                LOGGER.warn("Could not resolve web resource " + sourcePath, e);
            }
        }
        return locators;
    }

    /**
     * Adds locators to all files contained in the given file.
     *
     * @param pluginName Plugin of the web resource
     * @param locators The list of locators to add to
     * @param imports Set of files that have already been added as import resource
     * @param file The file that will be added (if it is a directory contained files will be added recursively)
     * @param sourcePath The source path of the locator
     * @param targetPath The target path of the locator
     */
    private static void addLocators(final String pluginName, final List<WebResourceLocator> locators,
        final Set<String> imports, final File file, final String sourcePath, final String targetPath) {
        if (file.isDirectory()) {
            for (File innerFile : file.listFiles()) {
                String innerSource = (sourcePath + "/" + innerFile.getName()).replace("//", "/");
                String innerTarget = (targetPath + "/" + innerFile.getName()).replace("//", "/");
                addLocators(pluginName, locators, imports, innerFile, innerSource, innerTarget);
            }
        } else {
            if (!imports.contains(sourcePath)) {
                locators.add(new WebResourceLocator(pluginName, sourcePath, targetPath, WebResourceType.FILE));
            }
        }
    }

    /** Temporary workaround to check if the argument workflow contains sub nodes and hence can be used
     * with the {@link WizardNode} execution.
     * @param manager To check, not null.
     * @return That property.
     */
    public static boolean hasWizardExecution(final WorkflowManager manager) {
        synchronized (manager.getWorkflowMutex()) {
            Collection<NodeContainer> nodes = manager.getWorkflow().getNodeValues();
            for (NodeContainer nc : nodes) {
                if (nc instanceof SubNodeContainer) {
                    return true;
                }
            }
            return false;
        }
    }

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
            m_levelIndex = -1;
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

    public WizardPageContent getCurrentWizardPage() {
        synchronized (m_manager.getWorkflowMutex()) {
            NodeContext.pushContext(m_manager);
            try {
                return getCurrentWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private WizardPageContent getCurrentWizardPageInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        lazyInit();
        final NodeID subNodeID = m_subNodesWithWizardNodesList.get(m_levelIndex);
        SubNodeContainer subNC = (SubNodeContainer)m_manager.getNodeContainer(subNodeID);
        WorkflowManager subWFM = subNC.getWorkflowManager();
        Map<NodeID, WizardNode> executedWizareNodeMap = subWFM.findExecutedNodes(WizardNode.class, NOT_HIDDEN_FILTER);
        LinkedHashMap<String, WizardNode> resultMap = new LinkedHashMap<String, WizardNode>();
        for (Map.Entry<NodeID, WizardNode> entry : executedWizareNodeMap.entrySet()) {
            NodeIDSuffix idSuffix = NodeIDSuffix.create(m_manager.getID(), entry.getKey());
            resultMap.put(idSuffix.toString(), entry.getValue());
        }
        return new WizardPageContent(resultMap);
    }

    public static final NodeModelFilter<WizardNode> NOT_HIDDEN_FILTER = new NodeModelFilter<WizardNode>() {

        @Override
        public boolean include(final WizardNode nodeModel) {
            return true;
        }
    };

    public boolean hasNextWizardPage() {
        synchronized (m_manager.getWorkflowMutex()) {
            NodeContext.pushContext(m_manager);
            try {
                return hasNextWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private boolean hasNextWizardPageInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        lazyInit();
        return m_levelIndex < m_subNodesWithWizardNodesList.size();
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

    private void stepNextInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        lazyInit();
        if (++m_levelIndex < m_subNodesWithWizardNodesList.size()) {
            LOGGER.debugWithFormat("Stepping wizard execution to sub node %s (step %d/%d)",
                m_subNodesWithWizardNodesList.get(m_levelIndex), m_levelIndex, m_subNodesWithWizardNodesList.size());
            m_manager.executeUpToHere(m_subNodesWithWizardNodesList.get(m_levelIndex));
        }
    }


    public Map<String, ValidationError> loadValuesIntoCurrentPage(final Map<String, String> viewContentMap) {
        synchronized (m_manager.getWorkflowMutex()) {
            NodeContext.pushContext(m_manager);
            try {
                return loadValuesIntoCurrentPageInternal(viewContentMap);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked" })
    private Map<String, ValidationError> loadValuesIntoCurrentPageInternal(
        final Map<String, String> viewContentMap) {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        lazyInit();
        LOGGER.debugWithFormat("Loading view content into wizard nodes (%d)", viewContentMap.size());
        NodeID currentID = m_subNodesWithWizardNodesList.get(m_levelIndex);
        SubNodeContainer subNodeNC = (SubNodeContainer)m_manager.getNodeContainer(currentID);
        WorkflowManager subNodeWFM = subNodeNC.getWorkflowManager();
        Map<NodeID, WizardNode> wizardNodeSet = subNodeWFM.findNodes(WizardNode.class, false);
        Map<String, ValidationError> resultMap = new LinkedHashMap<String, ValidationError>();
        for (Map.Entry<String, String> entry : viewContentMap.entrySet()) {
            NodeIDSuffix suffix = NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(m_manager.getID());
            if (!id.hasPrefix(currentID)) {
                throw new IllegalStateException(String.format("The wizard page content for ID %s (suffix %s) "
                        + "does not belong to the current subnode (ID %s)", id, entry.getKey(), currentID));
            }
            WizardNode wizardNode = wizardNodeSet.get(id);
            if (wizardNode == null) {
                throw new IllegalStateException(String.format("No wizard node with ID %s in sub node, valid IDs are: "
                        + "%s", id, ConvenienceMethods.getShortStringFrom(wizardNodeSet.entrySet(), 10)));
            }
            JSONViewContent newViewValue = (JSONViewContent)wizardNode.createEmptyViewValue();
            if (newViewValue == null) {
                // node has no view value
                continue;
            }
            ValidationError validationError = null;
            try {
                newViewValue.loadFromStream(new ByteArrayInputStream(entry.getValue().getBytes()));
                validationError = wizardNode.validateViewValue(newViewValue);
            } catch (IOException e) {
                resultMap.put(entry.getKey(), new ValidationError("Could not deserialize JSON value: " + entry.getValue()));
            }
            if (validationError != null) {
                resultMap.put(entry.getKey(), validationError);
            }
        }
        if (!resultMap.isEmpty()) {
            return resultMap;
        }
        // validation succeeded, reset subnode and apply
        m_manager.resetAndConfigureNode(currentID);
        for (Map.Entry<String, String> entry : viewContentMap.entrySet()) {
            NodeIDSuffix suffix = NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(m_manager.getID());
            WizardNode wizardNode = wizardNodeSet.get(id);
            JSONViewContent newViewValue = (JSONViewContent)wizardNode.createEmptyViewValue();
            if (newViewValue == null) {
                // node has no view value
                continue;
            }
            try {
                newViewValue.loadFromStream(new ByteArrayInputStream(entry.getValue().getBytes()));
                wizardNode.loadViewValue(newViewValue);
            } catch (IOException e) {
                // do nothing, exception not possible
            }
        }
        m_manager.executeUpToHere(currentID);
        return Collections.emptyMap();
    }

    public boolean hasPreviousWizardPage() {
        synchronized (m_manager.getWorkflowMutex()) {
            NodeContext.pushContext(m_manager);
            try {
                return hasNextWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private boolean hasPreviousWizardPageInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        lazyInit();
        return m_levelIndex > 0;
    }


    public void stepBack() {
        synchronized (m_manager.getWorkflowMutex()) {
            NodeContext.pushContext(m_manager);
            try {
                stepNextInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private void stepPreviousInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        lazyInit();
        if (m_levelIndex > 0) {
            LOGGER.debugWithFormat("Stepping back wizard execution to sub node %s (step %d/%d)",
                m_subNodesWithWizardNodesList.get(m_levelIndex - 1), m_levelIndex - 1,
                m_subNodesWithWizardNodesList.size());
            m_levelIndex--;
            m_manager.resetAndConfigureNode(m_subNodesWithWizardNodesList.get(m_levelIndex));
        }
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

        /** Outputs the underlying string array, e.g. "2:5:4:2"
         * {@inheritDoc} */
        @Override
        public String toString() {
            return StringUtils.join(ArrayUtils.toObject(m_suffixes), ':');
        }

        /** Reverse operation of {@link #toString()}.
         * @param string The string as returned by {@link #toString()}.
         * @return A new {@link NodeIDSuffix}.
         * @throws IllegalArgumentException If parsing fails.
         */
        public static NodeIDSuffix fromString(final String string) {
            String[] splitString = StringUtils.split(string, ':');
            int[] suffixes = new int[splitString.length];
            for (int i = 0; i < suffixes.length; i++) {
                try {
                    suffixes[i] = Integer.parseInt(splitString[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Can't parse node id suffix string \""
                            + string + "\": " + e.getMessage(), e);
                }
            }
            return new NodeIDSuffix(suffixes);
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
