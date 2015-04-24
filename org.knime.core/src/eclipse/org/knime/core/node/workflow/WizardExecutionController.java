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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.web.DefaultWebTemplate;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.wizard.WizardNodeLayoutInfo;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;

/**
 * A utility class received from the workflow manager that allows stepping back and forth in a wizard execution. It's
 * the "new" wizard execution based on SubNodes.
 *
 * <p>Do not use, no public API.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Christian Albrecht, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public final class WizardExecutionController {

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

    /** Pushed onto stack to indicate that the all pages were shown and the result page is "current". */
    private static final int ALL_COMPLETED = -1;

    /** Filter passt to WFM seach methods to find only QF nodes that are to be displayed. */
    @SuppressWarnings("rawtypes")
    public static final NodeModelFilter<WizardNode> NOT_HIDDEN_FILTER = new NodeModelFilter<WizardNode>() {
        @Override
        public boolean include(final WizardNode nodeModel) {
            return !nodeModel.isHideInWizard();
        }
    };


    /** Host WFM. */
    private WorkflowManager m_manager;
    /** The history of subnodes prompted, current one on top unless {@link #ALL_COMPLETED}. Each int is
     * the subnode ID suffix */
    private final Stack<Integer> m_promptedSubnodeIDSuffixes;
    /** Listener that is non-null while stepping forward. It will check if the queued Subnode contains valid QFs
     * after execution and will just do another step if it does not. Member is null while not stepping. */
    private StepForwardNodeStateChangeListener m_stepNextStatusListener;

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
        m_promptedSubnodeIDSuffixes = new Stack<>();
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

    private NodeID findNextWaitingSubnode() {
        final Workflow flow = m_manager.getWorkflow();
        Map<NodeID, Set<Integer>> breadthFirstSortedList = flow.createBreadthFirstSortedList(flow.getNodeIDs(), true);
        for (Map.Entry<NodeID, Set<Integer>> entry : breadthFirstSortedList.entrySet()) {
            NodeContainer nc = flow.getNode(entry.getKey());
            if (nc instanceof SubNodeContainer) {
                SubNodeContainer subnodeNC = (SubNodeContainer)nc;
                WorkflowManager subnodeMgr = subnodeNC.getWorkflowManager();
                if (subnodeMgr.getNodeContainerState().isExecuted()) {
                    continue; // do not prompt executed sub nodes
                }
                if (m_promptedSubnodeIDSuffixes.contains(entry.getKey().getIndex())) {
                    continue;
                }
                @SuppressWarnings("rawtypes")
                final Map<NodeID, WizardNode> wizardNodes =
                subnodeMgr.findNodes(WizardNode.class, NOT_HIDDEN_FILTER, false);
                if (!wizardNodes.isEmpty()) {
                    return subnodeNC.getID();
                }
            }
        }
        return null;
    }

    /** Get the current wizard page. Throws exception if none is available (as per {@link #hasCurrentWizardPage()}.
     * @return The current wizard page. */
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
        CheckUtils.checkState(hasCurrentWizardPageInternal(), "No current wizard page - all executed");
        int currentSubnodeIDSuffix = m_promptedSubnodeIDSuffixes.peek();
        final NodeID subNodeID = toNodeID(currentSubnodeIDSuffix);
        SubNodeContainer subNC = m_manager.getNodeContainer(subNodeID, SubNodeContainer.class, true);
        WorkflowManager subWFM = subNC.getWorkflowManager();
        Map<NodeID, WizardNode> executedWizardNodeMap = subWFM.findExecutedNodes(WizardNode.class, NOT_HIDDEN_FILTER);
        LinkedHashMap<String, WizardNode> resultMap = new LinkedHashMap<String, WizardNode>();
        for (Map.Entry<NodeID, WizardNode> entry : executedWizardNodeMap.entrySet()) {
            if (!subWFM.getNodeContainer(entry.getKey(), NativeNodeContainer.class, true).isInactive()) {
                NodeIDSuffix idSuffix = NodeIDSuffix.create(m_manager.getID(), entry.getKey());
                resultMap.put(idSuffix.toString(), entry.getValue());
            }
        }
        NodeIDSuffix pageID = NodeIDSuffix.create(m_manager.getID(), subWFM.getID());
        return new WizardPageContent(pageID.toString(), resultMap, subNC.getLayoutInfo());
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
        synchronized (m_manager.getWorkflowMutex()) {
            checkDiscard();
            NodeContext.pushContext(m_manager);
            try {
                return hasCurrentWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private boolean hasCurrentWizardPageInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        if (m_promptedSubnodeIDSuffixes.isEmpty()) {
            // stepNext not called
            return false;
        } else if (m_promptedSubnodeIDSuffixes.peek() == ALL_COMPLETED) {
            // all done - result page to be shown
            return false;
        }
        return true;
    }

    /** Continues the execution and executes up to, incl., the next subnode awaiting input. If no such subnode exists
     * it fully executes the workflow. */
    public void stepNext() {
        synchronized (m_manager.getWorkflowMutex()) {
            checkDiscard();
            NodeContext.pushContext(m_manager);
            try {
                stepNextInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private void stepNextInternal() {
        CheckUtils.checkState(m_stepNextStatusListener == null, "Cannot step forward as another processing is "
                + "currently ongoing (listener is not null), status of WFM is:\n%s",
                m_manager.printNodeSummary(m_manager.getID(), 0));
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        final NodeID subnodeID = findNextWaitingSubnode();
        if (subnodeID != null) {
            SubNodeContainer subNodeNC = m_manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
            LOGGER.debugWithFormat("Next subnode awaiting input: \"%s\"", subNodeNC.getNameWithID());
            m_stepNextStatusListener = new StepForwardNodeStateChangeListener(subnodeID.getIndex());
            subNodeNC.addNodeStateChangeListener(m_stepNextStatusListener);
            m_manager.executeUpToHere(subnodeID);
        } else {
            LOGGER.debug("No more subnodes awaiting input - executing all");
            // TODO not sure if that is expected:
            // Last action while stepping is to execute the remainder of the workflow
            m_manager.executeAll();
            m_promptedSubnodeIDSuffixes.push(ALL_COMPLETED);
        }
    }

    private void onExecutionFinishedWhileStepping(final int levelID) {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        CheckUtils.checkState(m_stepNextStatusListener != null,
                "Supposed to be called from listener (not expected to be null)");
        final NodeID subnodeID = toNodeID(levelID);
        SubNodeContainer subNodeNC = m_manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        subNodeNC.removeNodeStateChangeListener(m_stepNextStatusListener);
        m_stepNextStatusListener = null;
        WorkflowManager subNodeWFM = subNodeNC.getWorkflowManager();
        // TODO remove inconsistency: the inner WFM is executed but the subnode is POST_EXECUTE (see listener code)
        if (!subNodeWFM.getNodeContainerState().isExecuted()) {
            LOGGER.warnWithFormat("Subnode \"%s\" expected to be EXECUTED at this point but is %s, "
                + "internal state is:\n%s", subNodeNC.getNameWithID(), subNodeNC.getNodeContainerState(),
                subNodeWFM.printNodeSummary(subNodeWFM.getID(), 0));
        }
        Map<NodeID, WizardNode> wizardNodeSet = subNodeWFM.findNodes(WizardNode.class, NOT_HIDDEN_FILTER, false);
        boolean allInactive = true;
        for (NodeID id : wizardNodeSet.keySet()) {
            if (!subNodeWFM.getNodeContainer(id, NativeNodeContainer.class, true).isInactive()) {
                allInactive = false;
            }
        }
        if (allInactive) {
            LOGGER.debugWithFormat("Subnode \"%s\" doesn't contain QFs to prompt, stepping further",
                subNodeNC.getNameWithID());
            stepNextInternal();
        } else {
            m_promptedSubnodeIDSuffixes.push(levelID);
        }
    }


    public Map<String, ValidationError> loadValuesIntoCurrentPage(final Map<String, String> viewContentMap) {
        synchronized (m_manager.getWorkflowMutex()) {
            checkDiscard();
            NodeContext.pushContext(m_manager);
            try {
                return loadValuesIntoCurrentPageInternal(viewContentMap);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked" })
    private Map<String, ValidationError> loadValuesIntoCurrentPageInternal(final Map<String, String> viewContentMap) {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        CheckUtils.checkState(hasCurrentWizardPageInternal(), "No current wizard page");
        LOGGER.debugWithFormat("Loading view content into wizard nodes (%d)", viewContentMap.size());
        int subnodeIDSuffix = m_promptedSubnodeIDSuffixes.peek();
        NodeID currentID = toNodeID(subnodeIDSuffix);
        SubNodeContainer subNodeNC = m_manager.getNodeContainer(currentID, SubNodeContainer.class, true);
        WorkflowManager subNodeWFM = subNodeNC.getWorkflowManager();
        Map<NodeID, WizardNode> wizardNodeSet = subNodeWFM.findNodes(WizardNode.class, NOT_HIDDEN_FILTER, false);
        Map<String, ValidationError> resultMap = new LinkedHashMap<String, ValidationError>();
        for (Map.Entry<String, String> entry : viewContentMap.entrySet()) {
            NodeIDSuffix suffix = NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(m_manager.getID());
            CheckUtils.checkState(id.hasPrefix(currentID), "The wizard page content for ID %s (suffix %s) "
                        + "does not belong to the current subnode (ID %s)", id, entry.getKey(), currentID);
            WizardNode wizardNode = wizardNodeSet.get(id);
            CheckUtils.checkState(wizardNode != null, "No wizard node with ID %s in sub node, valid IDs are: "
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
        m_manager.resetAndConfigureNode(currentID);
        for (Map.Entry<String, String> entry : viewContentMap.entrySet()) {
            NodeIDSuffix suffix = NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(m_manager.getID());
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
        m_manager.executeUpToHere(currentID);
        return Collections.emptyMap();
    }

    public boolean hasPreviousWizardPage() {
        synchronized (m_manager.getWorkflowMutex()) {
            checkDiscard();
            NodeContext.pushContext(m_manager);
            try {
                return hasPreviousWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private boolean hasPreviousWizardPageInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        // stack contains "current" page and has a previous one -- so two elements at least.
        return m_promptedSubnodeIDSuffixes.size() >= 2;
    }


    public void stepBack() {
        synchronized (m_manager.getWorkflowMutex()) {
            checkDiscard();
            NodeContext.pushContext(m_manager);
            try {
                stepPreviousInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private void stepPreviousInternal() {
        assert Thread.holdsLock(m_manager.getWorkflowMutex());
        CheckUtils.checkState(hasPreviousWizardPageInternal(), "No more previous pages");
        int currentPage = m_promptedSubnodeIDSuffixes.pop();
        if (currentPage == ALL_COMPLETED) {
            LOGGER.debug("Stepping back wizard execution (no-op as previous page is last wizard page)");
        } else {
            NodeID currentSNID = toNodeID(currentPage);
            SubNodeContainer currentSN = m_manager.getNodeContainer(currentSNID, SubNodeContainer.class, true);
            final Integer previousSNIDSuffix = m_promptedSubnodeIDSuffixes.peek();
            SubNodeContainer previousSN = previousSNIDSuffix == null ? null
                : m_manager.getNodeContainer(toNodeID(previousSNIDSuffix), SubNodeContainer.class, true);
            LOGGER.debugWithFormat("Stepping back wizard execution - resetting subnode \"%s\" (%s)",
                currentSN.getNameWithID(), previousSN == null ? "no more subnodes to reset"
                    : "new current one is \"" + previousSN.getNameWithID() + "\"");
            m_manager.resetAndConfigureNodeAndSuccessors(currentSNID, false);
        }
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
        m_manager = null;
    }

    void save(final NodeSettingsWO settings) {
        int[] promptedSubnodeIDs = ArrayUtils.toPrimitive(
            m_promptedSubnodeIDSuffixes.toArray(new Integer[m_promptedSubnodeIDSuffixes.size()]));
        settings.addIntArray("promptedSubnodeIDs", promptedSubnodeIDs);
    }

    /** Listener added to currently queued subnode. Calls back
     * {@link WizardExecutionController#onExecutionFinishedWhileStepping(int)} when it turns EXECUTED
     * (or stops otherwise). It currently also notifies on POSTEXECUTE because we need to queue downstream nodes early
     * as otherwise they will turn CONFIGURED_QUEUED before they get configured as part of doAfterExecution. Here is
     * the pseudo call stack when only notifying on EXECUTED:
     * 1) Subnode in performExecute - finishes and turns EXECUTED
     * 2) State transition to EXECUTED
     * 2.1) Notifies listener, inc. this, which queues downstream subnode (go from CONFIGURED to CONFIGURED_QUEUED)
     * 2.2) doAfterExecution is called on parent WFM
     * 2.3) doAfterExecution doesn't call configure on downstream subnode as it's QUEUED already
     * 3) downstream sub node did not have time to configure and inactive flag is not propagated
     * To see the effect try it with two inactive subnodes in a row, whereby the later needs to have unconnected source
     * nodes.
     */
    private final class StepForwardNodeStateChangeListener implements NodeStateChangeListener {

        private final int m_levelOfSubnode;

        /** @param levelOfSubnode Level of the sub node currently in execution (and monitored). */
        private StepForwardNodeStateChangeListener(final int levelOfSubnode) {
            m_levelOfSubnode = levelOfSubnode;
        }

        /** {@inheritDoc} */
        @Override
        public void stateChanged(final NodeStateEvent state) {
            InternalNodeContainerState inState = state.getInternalNCState();
            // see inner class description
            if (!inState.isExecutionInProgress() || inState.equals(InternalNodeContainerState.POSTEXECUTE)) {
                onExecutionFinishedWhileStepping(m_levelOfSubnode);
            }
        }
    }

    /** Result value of {@link WizardExecutionController#getCurrentWizardPage()}. */
    public static final class WizardPageContent {

        private final String m_pageNodeID;
        private final Map<String, WizardNode> m_pageMap;
        private final Map<Integer, WizardNodeLayoutInfo> m_layoutInfo;

        /**
         * @param pageNodeID
         * @param pageMap
         * @param layoutInfo
         */
        @SuppressWarnings("rawtypes")
        WizardPageContent(final String pageNodeID, final Map<String, WizardNode> pageMap,
            final Map<Integer, WizardNodeLayoutInfo> layoutInfo) {
            m_pageNodeID = pageNodeID;
            m_pageMap = pageMap;
            m_layoutInfo = layoutInfo;
        }

        /**
         * @return the pageNodeID
         */
        public String getPageNodeID() {
            return m_pageNodeID;
        }

        /**
         * @return the pageMap
         */
        @SuppressWarnings("rawtypes")
        public Map<String, WizardNode> getPageMap() {
            return m_pageMap;
        }

        /**
         * @return the layoutInfo
         */
        public Map<Integer, WizardNodeLayoutInfo> getLayoutInfo() {
            return m_layoutInfo;
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
