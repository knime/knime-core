/*
 * ------------------------------------------------------------------------
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
 * Created on Jan 29, 2014 by wiswedel
 */
package org.knime.core.node.workflow;

import java.io.ByteArrayInputStream;
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
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.web.DefaultWebTemplate;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;

/**
 * A utility class received from the workflow manager that allows stepping back and forth in a wizard execution.
 * USed for the 2nd generation wizard execution based on SubNodes.
 *
 * <p>Do not use, no public API.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Christian Albrecht, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public final class WizardExecutionController extends ExecutionController {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WizardExecutionController.class);

    private static final String ID_WEB_RES = "org.knime.js.core.webResources";
    private static final String ID_JS_COMP = "org.knime.js.core.javascriptComponents";
    private static final String ID_IMPL_BUNDLE = "implementationBundleID";
    private static final String ID_IMPORT_RES = "importResource";
    private static final String ID_DEPENDENCY = "webDependency";

    private static final String ATTR_JS_ID = "javascriptComponentID";
    private static final String ATTR_NAMESPACE = "namespace";
    private static final String ATTR_RES_BUNDLE_ID = "webResourceBundleID";
    private static final String ATTR_PATH = "relativePath";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_INIT_METHOD_NAME = "init-method-name";
    private static final String ATTR_VALIDATE_METHOD_NAME = "validate-method-name";
    private static final String ATTR_GETCOMPONENTVALUE_METHOD_NAME = "getComponentValue-method-name";
    private static final String ATTR_SETVALIDATIONERROR_METHOD_NAME = "setValidationError-method-name";

    private static final String ID_WEB_RESOURCE = "webResource";

    private static final String ATTR_RELATIVE_PATH_SOURCE = "relativePathSource";
    private static final String ATTR_RELATIVE_PATH_TARGET = "relativePathTarget";

    private static final String DEFAULT_DEPENDENCY = "knimeService_1.0";
    private static final Set<WebResourceLocator> DEFAULT_RES =
        getResourcesFromExtension(getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, DEFAULT_DEPENDENCY));

    /** Filter passed to WFM serach methods to find only QF nodes that are to be displayed. */
    @SuppressWarnings("rawtypes")
    public static final NodeModelFilter<WizardNode> NOT_HIDDEN_FILTER = new NodeModelFilter<WizardNode>() {
        @Override
        public boolean include(final WizardNode nodeModel) {
            return !nodeModel.isHideInWizard();
        }
    };

    /** The history of subnodes prompted, current one on top unless {@link #ALL_COMPLETED}. Each int is
     * the subnode ID suffix */
    private final Stack<Integer> m_promptedSubnodeIDSuffixes;

    /** Host WFM. */
    private final WorkflowManager m_manager;

    /** This is the central data structure - it holds all nodes that were halted during
     * execution = nodes that were executed and none of their successors was queued. Those
     * will be active SubNodes with at least one active QF element that can be displayed.
     * After the user got a chance to interact with the Wizard page, those nodes will be
     * re-executed but this time they will not be added/halted again (which is why the status
     * is toggled if they are already in the list - see checkHaltingCriteria()). However, if
     * it is part of a loop it will be executed a second time (after the re-execute) and then
     * execution will be halted again.
     */
    private final List<NodeID> m_waitingSubnodes;

    /**
     * @param jsObjectID The JavaScript object ID used for locating the extension point.
     * @return A template object, being used to assamble views.
     */
    public static WebTemplate getWebTemplateFromJSObjectID(final String jsObjectID) {
        LinkedHashSet<WebResourceLocator> webResList = new LinkedHashSet<WebResourceLocator>();
        IConfigurationElement jsComponentExtension = getConfigurationFromID(ID_JS_COMP, ATTR_JS_ID, jsObjectID);
        if (jsComponentExtension == null) {
            return getEmptyWebTemplate();
        }
        String bundleID = jsComponentExtension.getAttribute(ID_IMPL_BUNDLE);
        IConfigurationElement implementationExtension =
            getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, bundleID);
        if (implementationExtension == null) {
            return getEmptyWebTemplate();
        }
        Set<WebResourceLocator> implementationRes = getResourcesFromExtension(implementationExtension);
        webResList.addAll(DEFAULT_RES);
        for (IConfigurationElement dependencyConf : jsComponentExtension.getChildren(ID_DEPENDENCY)) {
            String dependencyID = dependencyConf.getAttribute(ATTR_RES_BUNDLE_ID);
            IConfigurationElement dependencyExtension =
                    getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, dependencyID);
            if (dependencyExtension == null) {
                LOGGER.error("Web ressource dependency could not be found: " + dependencyID
                    + ". This is most likely an implementation error.");
                continue;
            }
            Set<WebResourceLocator> dependencyRes = getResourcesFromExtension(dependencyExtension);
            webResList.addAll(dependencyRes);
        }
        webResList.addAll(implementationRes);
        String namespace = jsComponentExtension.getAttribute(ATTR_NAMESPACE);
        String initMethodName = jsComponentExtension.getAttribute(ATTR_INIT_METHOD_NAME);
        String validateMethodName = jsComponentExtension.getAttribute(ATTR_VALIDATE_METHOD_NAME);
        String valueMethodName = jsComponentExtension.getAttribute(ATTR_GETCOMPONENTVALUE_METHOD_NAME);
        String setValidationErrorMethodName = jsComponentExtension.getAttribute(ATTR_SETVALIDATIONERROR_METHOD_NAME);
        return new DefaultWebTemplate(webResList.toArray(new WebResourceLocator[0]),
            namespace, initMethodName, validateMethodName, valueMethodName, setValidationErrorMethodName);
    }

    private static WebTemplate getEmptyWebTemplate() {
        return new DefaultWebTemplate(new WebResourceLocator[0], "", "", "", "", "");
    }

    private static IConfigurationElement getConfigurationFromID(final String extensionPointId,
        final String configurationID, final String jsObjectID) {
        if (jsObjectID != null) {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IConfigurationElement[] configurationElements = registry.getConfigurationElementsFor(extensionPointId);
            for (IConfigurationElement element : configurationElements) {
                if (jsObjectID.equals(element.getAttribute(configurationID))) {
                    return element;
                }
            }
        }
        return null;
    }

    private static Map<String, String> getWebResources(final IConfigurationElement resConfig) {
        Map<String, String> resMap = new HashMap<String, String>();
        for (IConfigurationElement resElement: resConfig.getChildren(ID_WEB_RESOURCE)) {
            resMap.put(resElement.getAttribute(ATTR_RELATIVE_PATH_TARGET),
                resElement.getAttribute(ATTR_RELATIVE_PATH_SOURCE));
        }
        return resMap;
    }

    private static Set<WebResourceLocator> getResourcesFromExtension(final IConfigurationElement resConfig) {
        String pluginName = resConfig.getContributor().getName();
        LinkedHashSet<WebResourceLocator> locators = new LinkedHashSet<WebResourceLocator>();
        Map<String, String> resMap = getWebResources(resConfig);
        Set<String> imports = new HashSet<String>();
        // collect dependencies
        for (IConfigurationElement depElement : resConfig.getChildren(ID_DEPENDENCY)) {
            String dependencyID = depElement.getAttribute(ATTR_RES_BUNDLE_ID);
            IConfigurationElement depConfig = getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, dependencyID);
            if (depConfig == null) {
                LOGGER.error("Web ressource dependency could not be found: " + dependencyID
                    + ". This is most likely an implementation error.");
                continue;
            }
            locators.addAll(getResourcesFromExtension(depConfig));
        }
        // collect own import files
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

        // Add additional ressources from directories
        /* for (Entry<String, String> entry :resMap.entrySet()) {
            String targetPath = entry.getKey();
            String sourcePath = entry.getValue();
            try {
                URL url = new URL("platform:/plugin/" + pluginName);
                File dir = new File(FileLocator.resolve(url).getFile());
                File file = new File(dir, sourcePath);
                if (file.exists()) {
                    addLocators(pluginName, locators, imports, file, sourcePath, targetPath);
                }
            } catch (Exception e) {
                LOGGER.warn("Could not resolve web resource " + sourcePath, e);
            }
        }*/
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
    /*private static void addLocators(final String pluginName, final Set<WebResourceLocator> locators,
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
    }*/

    /** Temporary workaround to check if the argument workflow contains sub nodes and hence can be used
     * with the {@link WizardNode} execution.
     * @param manager To check, not null.
     * @return That property.
     */
    public static boolean hasWizardExecution(final WorkflowManager manager) {
        try (WorkflowLock lock = manager.lock()) {
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
        m_manager = CheckUtils.checkArgumentNotNull(manager);
        m_promptedSubnodeIDSuffixes = new Stack<>();
        m_waitingSubnodes = new ArrayList<>();
    }

    /** Restored from settings.
     * @param manager ...
     * @param settings ...
     * @throws InvalidSettingsException ...
     */
    WizardExecutionController(final WorkflowManager manager, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        this(manager);
        int[] levelStack = settings.getIntArray("promptedSubnodeIDs");
        m_promptedSubnodeIDSuffixes.addAll(Arrays.asList(ArrayUtils.toObject(levelStack)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void checkHaltingCriteria(final NodeID source) {
        assert m_manager.isLockedByCurrentThread();
        if (m_waitingSubnodes.remove(source)) {
            // trick to handle re-execution of SubNodes properly: when the node is already
            // in the list it was just re-executed and we don't add it to the list of halted
            // nodes but removed it instead. If we see it again then it is part of a loop and
            // we will add it again).
            return;
        }
        // potentially null when queried from contained metanode
        NodeContainer sourceNC = m_manager.getWorkflow().getNode(source);
        // only consider nodes that are...SubNodes and...
        if (!(sourceNC instanceof SubNodeContainer)) {
            return;
        }
        SubNodeContainer subnodeSource = (SubNodeContainer)sourceNC;
        // ...active.
        if (subnodeSource.isInactive()) {
            return;
        }
        // Now check if the active SubNode contains active QuickForm nodes:
        WorkflowManager subNodeWFM = subnodeSource.getWorkflowManager();
        Map<NodeID, WizardNode> wizardNodeSet = subNodeWFM.findNodes(WizardNode.class, NOT_HIDDEN_FILTER, false);
        boolean allInactive = true;
        for (NodeID id : wizardNodeSet.keySet()) {
            if (!subNodeWFM.getNodeContainer(id, NativeNodeContainer.class, true).isInactive()) {
                allInactive = false;
            }
        }
        if (!allInactive) {
            // add to the list so we can later avoid queuing of successors!
            m_waitingSubnodes.add(source);
        }
    }

    /** {@inheritDoc} */
    @Override
    boolean isHalted(final NodeID source) {
        return m_waitingSubnodes.contains(source);
    }

    /** Get the current wizard page. Throws exception if none is available (as per {@link #hasCurrentWizardPage()}.
     * @return The current wizard page. */
    public WizardPageContent getCurrentWizardPage() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            NodeContext.pushContext(manager);
            try {
                return getCurrentWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private WizardPageContent getCurrentWizardPageInternal() {
        final WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        CheckUtils.checkState(hasCurrentWizardPageInternal(), "No current wizard page - all executed");
        NodeID currentSubnodeID = m_waitingSubnodes.get(0);
//        int currentSubnodeIDSuffix = m_promptedSubnodeIDSuffixes.peek();
//        final NodeID subNodeID = toNodeID(currentSubnodeIDSuffix);
        SubNodeContainer subNC = manager.getNodeContainer(currentSubnodeID, SubNodeContainer.class, true);
        WorkflowManager subWFM = subNC.getWorkflowManager();
        Map<NodeID, WizardNode> executedWizardNodeMap = subWFM.findExecutedNodes(WizardNode.class, NOT_HIDDEN_FILTER);
        LinkedHashMap<NodeIDSuffix, WizardNode> resultMap = new LinkedHashMap<>();
        for (Map.Entry<NodeID, WizardNode> entry : executedWizardNodeMap.entrySet()) {
            if (!subWFM.getNodeContainer(entry.getKey(), NativeNodeContainer.class, true).isInactive()) {
                NodeID.NodeIDSuffix idSuffix = NodeID.NodeIDSuffix.create(manager.getID(), entry.getKey());
                resultMap.put(idSuffix, entry.getValue());
            }
        }
        NodeID.NodeIDSuffix pageID = NodeID.NodeIDSuffix.create(manager.getID(), subWFM.getID());
        return new WizardPageContent(pageID, resultMap, subNC.getLayoutJSONString());
    }

    /** ...
     * @return ...
     * @deprecated Use {@link #hasCurrentWizardPage()} instead.
     */
    @Deprecated
    public boolean hasNextWizardPage() {
        return hasCurrentWizardPage();
    }

    /** Returns true if the wizard was stepped forward and has a subnode awaiting input.
     * @return That property.
     * @since 2.11 */
    public boolean hasCurrentWizardPage() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return hasCurrentWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private boolean hasCurrentWizardPageInternal() {
        assert m_manager.isLockedByCurrentThread();
        return !m_waitingSubnodes.isEmpty();
//        if (m_promptedSubnodeIDSuffixes.isEmpty()) {
//            // stepNext not called
//            return false;
//        } else if (m_promptedSubnodeIDSuffixes.peek() == ALL_COMPLETED) {
//            // all done - result page to be shown
//            return false;
//        }
//        return true;
    }

    /** Parameterizes {@link InputNode}s in the workflow (URL parameters).
     * @param input non-null input
     * @throws InvalidSettingsException if wfm chokes
     * @see WorkflowManager#setInputNodes(Map)
     * @since 3.2 */
    public void setInputNodes(final Map<String, ExternalNodeData> input) throws InvalidSettingsException {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                manager.setInputNodes(input);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /** Continues the execution and executes up to, incl., the next subnode awaiting input. If no such subnode exists
     * it fully executes the workflow. */
    public void stepFirst() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                stepFirstInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private void stepFirstInternal() {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        manager.executeAll();
    }

    public Map<String, ValidationError> loadValuesIntoCurrentPage(final Map<String, String> viewContentMap) {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return loadValuesIntoCurrentPageInternal(viewContentMap);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked" })
    private Map<String, ValidationError> loadValuesIntoCurrentPageInternal(final Map<String, String> viewContentMap) {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        CheckUtils.checkState(hasCurrentWizardPageInternal(), "No current wizard page");
        LOGGER.debugWithFormat("Loading view content into wizard nodes (%d)", viewContentMap.size());
//        int subnodeIDSuffix = m_promptedSubnodeIDSuffixes.peek();
//        NodeID currentID = toNodeID(subnodeIDSuffix);
        NodeID currentID = m_waitingSubnodes.get(0);
        SubNodeContainer subNodeNC = manager.getNodeContainer(currentID, SubNodeContainer.class, true);
        WorkflowManager subNodeWFM = subNodeNC.getWorkflowManager();
        Map<NodeID, WizardNode> wizardNodeSet = subNodeWFM.findNodes(WizardNode.class, NOT_HIDDEN_FILTER, false);
        Map<String, ValidationError> resultMap = new LinkedHashMap<String, ValidationError>();
        for (Map.Entry<String, String> entry : viewContentMap.entrySet()) {
            NodeID.NodeIDSuffix suffix = NodeID.NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(manager.getID());
            CheckUtils.checkState(id.hasPrefix(currentID), "The wizard page content for ID %s (suffix %s) "
                        + "does not belong to the current Wrapped Metanode (ID %s)", id, entry.getKey(), currentID);
            WizardNode wizardNode = wizardNodeSet.get(id);
            CheckUtils.checkState(wizardNode != null, "No wizard node with ID %s in Wrapped Metanode, valid IDs are: "
                        + "%s", id, ConvenienceMethods.getShortStringFrom(wizardNodeSet.entrySet(), 10));
            WebViewContent newViewValue = wizardNode.createEmptyViewValue();
            if (newViewValue == null) {
                // node has no view value
                continue;
            }
            ValidationError validationError = null;
            try {
                newViewValue.loadFromStream(new ByteArrayInputStream(entry.getValue().getBytes()));
                validationError = wizardNode.validateViewValue(newViewValue);
            } catch (Exception e) {
                resultMap.put(entry.getKey(),
                    new ValidationError("Could not deserialize JSON value: " + entry.getValue()));
            }
            if (validationError != null) {
                resultMap.put(entry.getKey(), validationError);
            }
        }
        if (!resultMap.isEmpty()) {
            return resultMap;
        }
        // validation succeeded, reset subnode and apply
        manager.resetHaltedSubnode(currentID);
//        manager.resetAndConfigureNode(currentID);
        for (Map.Entry<String, String> entry : viewContentMap.entrySet()) {
            NodeID.NodeIDSuffix suffix = NodeID.NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(manager.getID());
            WizardNode wizardNode = wizardNodeSet.get(id);
            WebViewContent newViewValue = wizardNode.createEmptyViewValue();
            if (newViewValue == null) {
                // node has no view value
                continue;
            }
            try {
                newViewValue.loadFromStream(new ByteArrayInputStream(entry.getValue().getBytes()));
                wizardNode.loadViewValue(newViewValue, false);
            } catch (Exception e) {
                LOGGER.error("Failed to load view value into node \"" + id + "\" although validation succeeded", e);
            }
        }
        manager.configureNodeAndSuccessors(currentID, true);
        return Collections.emptyMap();
    }

    public void stepNext() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                stepNextInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private void stepNextInternal() {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        CheckUtils.checkState(hasCurrentWizardPageInternal(), "No current wizard page");
        NodeID currentID = m_waitingSubnodes.get(0);
        SubNodeContainer currentNC = manager.getNodeContainer(currentID, SubNodeContainer.class, true);
        if (currentNC.getFlowObjectStack().peek(FlowLoopContext.class) == null) {
            m_promptedSubnodeIDSuffixes.push(currentID.getIndex());
        }
        reexecuteNode(currentID);
    }

    private void reexecuteNode(final NodeID id) {
        if (m_manager.getNodeContainer(id).getInternalState().isExecuted()) {
            m_waitingSubnodes.remove(id);
            m_manager.configureNodeAndSuccessors(id, false);
        } else {
            m_manager.executeUpToHere(id);
        }
        // in case of back-stepping we need to mark all nodes again (for execution)
        m_manager.executeAll();
    }

    public boolean hasPreviousWizardPage() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return hasPreviousWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private boolean hasPreviousWizardPageInternal() {
        assert m_manager.isLockedByCurrentThread();
        return !m_promptedSubnodeIDSuffixes.isEmpty();
    }


    public void stepBack() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                stepBackInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private void stepBackInternal() {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        CheckUtils.checkState(hasPreviousWizardPageInternal(), "No more previous pages");
        int currentPage = m_promptedSubnodeIDSuffixes.pop();
        NodeID currentSNID = toNodeID(currentPage);
        m_waitingSubnodes.clear();
        m_waitingSubnodes.add(currentSNID);
        SubNodeContainer currentSN = manager.getNodeContainer(currentSNID, SubNodeContainer.class, true);
        final Integer previousSNIDSuffix = m_promptedSubnodeIDSuffixes.isEmpty()
                ? null : m_promptedSubnodeIDSuffixes.peek();
        SubNodeContainer previousSN = previousSNIDSuffix == null ? null
            : manager.getNodeContainer(toNodeID(previousSNIDSuffix), SubNodeContainer.class, true);
        LOGGER.debugWithFormat("Stepping back wizard execution - resetting Wrapped Metanode \"%s\" (%s)",
            currentSN.getNameWithID(), previousSN == null ? "no more Wrapped Metanodes to reset"
                : "new current one is \"" + previousSN.getNameWithID() + "\"");
        manager.cancelExecution(currentSN);
        manager.resetAndConfigureNodeAndSuccessors(currentSNID, false);
    }

    /** Composes the NodeID of a subnode.
     * @param subnodeIDSuffix ...
     * @return new NodeID(m_manager.getID(), subnodeIDSuffix);
     */
    private NodeID toNodeID(final int subnodeIDSuffix) {
        return new NodeID(m_manager.getID(), subnodeIDSuffix);
    }

    private void checkDiscard() {
        CheckUtils.checkArgument(m_manager != null, "%s has been disconnected from workflow",
                WizardExecutionController.class.getSimpleName());
    }

    /** Sets manager to null. Called when new wizard is created on top of workflow. */
    void discard() {
    }

    void save(final NodeSettingsWO settings) {
        int[] promptedSubnodeIDs = ArrayUtils.toPrimitive(
            m_promptedSubnodeIDSuffixes.toArray(new Integer[m_promptedSubnodeIDSuffixes.size()]));
        settings.addIntArray("promptedSubnodeIDs", promptedSubnodeIDs);
    }

    /** Result value of {@link WizardExecutionController#getCurrentWizardPage()}. */
    public static final class WizardPageContent {

        private final NodeIDSuffix m_pageNodeID;
        private final Map<NodeIDSuffix, WizardNode> m_pageMap;
        private final String m_layoutInfo;

        /**
         * @param pageNodeID
         * @param pageMap
         * @param layoutInfo
         */
        @SuppressWarnings("rawtypes")
        WizardPageContent(final NodeIDSuffix pageNodeID, final Map<NodeIDSuffix, WizardNode> pageMap,
            final String layoutInfo) {
            m_pageNodeID = pageNodeID;
            m_pageMap = pageMap;
            m_layoutInfo = layoutInfo;
        }

        /**
         * @return the pageNodeID
         */
        public NodeIDSuffix getPageNodeID() {
            return m_pageNodeID;
        }

        /**
         * @return the pageMap
         */
        @SuppressWarnings("rawtypes")
        public Map<NodeIDSuffix, WizardNode> getPageMap() {
            return m_pageMap;
        }

        /**
         * @return the layoutInfo
         * @since 3.1
         */
        public String getLayoutInfo() {
            return m_layoutInfo;
        }

    }

}
