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
 * Created on Jan 29, 2014 by wiswedel
 */
package org.knime.core.node.workflow;

import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.interactive.ViewRequestHandlingException;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.web.DefaultWebTemplate;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.wizard.WizardViewRequest;
import org.knime.core.node.wizard.WizardViewRequestHandler;
import org.knime.core.node.wizard.WizardViewResponse;
import org.knime.core.node.wizard.util.LayoutUtil;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WebResourceController.WizardPageContent.WizardPageNodeInfo;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;

/**
 * An abstract utility class received from the workflow manager that allows defining wizard execution or generating
 * combined views on subnodes. Used for example for the 2nd generation wizard execution based on SubNodes.
 *
 * <p>
 * Do not use, no public API.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 * @since 3.4
 */
public abstract class WebResourceController {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WebResourceController.class);

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

    /** Host WFM. */
    protected final WorkflowManager m_manager;

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
        return new DefaultWebTemplate(webResList.toArray(new WebResourceLocator[0]), namespace, initMethodName,
            validateMethodName, valueMethodName, setValidationErrorMethodName);
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
        for (IConfigurationElement resElement : resConfig.getChildren(ID_WEB_RESOURCE)) {
            resMap.put(resElement.getAttribute(ATTR_RELATIVE_PATH_TARGET),
                resElement.getAttribute(ATTR_RELATIVE_PATH_SOURCE));
        }
        return resMap;
    }

    private static Set<WebResourceLocator> getResourcesFromExtension(final IConfigurationElement resConfig) {
        if (resConfig == null) {
            return Collections.emptySet();
        }
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

    /**
     * Temporary workaround to check if the argument workflow contains sub nodes and hence can be used with the
     * {@link WizardNode} execution.
     *
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

    /**
     * Created from workflow.
     *
     * @param manager ...
     */
    WebResourceController(final WorkflowManager manager) {
        m_manager = CheckUtils.checkArgumentNotNull(manager);
    }

    /**
     * Checks different criteria to determine if a combined page view is available for a given subnode.
     *
     * @param subnodeId the {@link NodeID} of the subnode to check
     * @return true, if a view on the subnode is available, false otherwise
     * @since 3.4
     */
    protected boolean isSubnodeViewAvailable(final NodeID subnodeId) {
        // potentially null when queried from contained metanode
        NodeContainer sourceNC = m_manager.getWorkflow().getNode(subnodeId);
        // only consider nodes that are...SubNodes and...
        if (!(sourceNC instanceof SubNodeContainer)) {
            return false;
        }
        SubNodeContainer subnodeSource = (SubNodeContainer)sourceNC;
        // ...active and not hidden.
        if (subnodeSource.isInactive() || subnodeSource.isHideInWizard()) {
            return false;
        }
        // Now check if the active SubNode contains active QuickForm nodes:
        WorkflowManager subNodeWFM = subnodeSource.getWorkflowManager();
        @SuppressWarnings("rawtypes")
        Map<NodeID, WizardNode> wizardNodeSet = subNodeWFM.findNodes(WizardNode.class, NOT_HIDDEN_FILTER, false);
        boolean allInactive = true;
        for (NodeID id : wizardNodeSet.keySet()) {
            if (!subNodeWFM.getNodeContainer(id, NativeNodeContainer.class, true).isInactive()) {
                allInactive = false;
                break;
            }
        }
        // also consider nested SubNodes which might have views to display
        Map<NodeID, SubNodeContainer> sncSet = findSubnodeContainers(subNodeWFM);
        for (NodeID id : sncSet.keySet()) {
            if (isSubnodeViewAvailable(id)) {
                allInactive = false;
                break;
            }
        }
        return !allInactive;
    }

    private static Map<NodeID, SubNodeContainer> findSubnodeContainers(final WorkflowManager subnodeManager) {
        try(WorkflowLock lock = subnodeManager.lock()) {
            Map<NodeID, SubNodeContainer> result = new LinkedHashMap<NodeID, SubNodeContainer>();
            for (NodeContainer nc : subnodeManager.getWorkflow().getNodeValues()) {
                if (nc instanceof SubNodeContainer) {
                    result.put(nc.getID(), (SubNodeContainer)nc);
                }
            }
            return result;
        }
    }

    /**
     * Crates the wizard page for a given node id. Throws exception if no wizard page available.
     *
     * @param subnodeID the node id for the subnode to retrieve the wizard page for
     * @return The wizard page for the given node id
     */
    @SuppressWarnings("rawtypes")
    protected WizardPageContent getWizardPageInternal(final NodeID subnodeID) {
        if (subnodeID == null) {
            LOGGER.error("No node ID supplied for creating wizard page");
            return null;
        }
        final WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();

        //        int currentSubnodeIDSuffix = m_promptedSubnodeIDSuffixes.peek();
        //        final NodeID subNodeID = toNodeID(currentSubnodeIDSuffix);
        SubNodeContainer subNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        WorkflowManager subWFM = subNC.getWorkflowManager();
        Map<NodeID, WizardNode> wizardNodeMap = subWFM.findNodes(WizardNode.class, NOT_HIDDEN_FILTER, false);
        //TODO: get applicable nested subnodes
        LinkedHashMap<NodeIDSuffix, WizardNode> resultMap = new LinkedHashMap<NodeIDSuffix, WizardNode>();
        //LinkedHashMap<NodeIDSuffix, WizardNode> errorMap = new LinkedHashMap<NodeIDSuffix, WizardNode>();
        LinkedHashMap<NodeIDSuffix, WizardPageNodeInfo> infoMap = new LinkedHashMap<NodeIDSuffix, WizardPageNodeInfo>();
        Set<HiLiteHandler> initialHiliteHandlerSet = new HashSet<HiLiteHandler>();
        for (Map.Entry<NodeID, WizardNode> entry : wizardNodeMap.entrySet()) {
            NodeContainer nc = subWFM.getNodeContainer(entry.getKey());
            if ((nc instanceof SingleNodeContainer) && ((SingleNodeContainer)nc).isInactive()) {
                //skip nodes in inactive branches
                continue;
            }
            NodeID.NodeIDSuffix idSuffix = NodeID.NodeIDSuffix.create(manager.getID(), entry.getKey());
            WizardPageNodeInfo nodeInfo = new WizardPageNodeInfo();
            nodeInfo.setNodeName(nc.getName());
            nodeInfo.setNodeAnnotation(nc.getNodeAnnotation().toString());
            nodeInfo.setNodeState(nc.getInternalState());
            nodeInfo.setNodeMessage(nc.getNodeMessage());
            infoMap.put(idSuffix, nodeInfo);
            if (EXECUTED.equals(nc.getInternalState())) {
                //regular viewable nodes need to be executed
                resultMap.put(idSuffix, entry.getValue());
            }
            for (int i = 0; i < nc.getNrInPorts() - 1; i++) {
                HiLiteHandler hiLiteHandler = ((NodeModel)entry.getValue()).getInHiLiteHandler(i);
                if (hiLiteHandler != null) {
                    initialHiliteHandlerSet.add(hiLiteHandler);
                }
            }
        }
        NodeID.NodeIDSuffix pageID = NodeID.NodeIDSuffix.create(manager.getID(), subWFM.getID());
        String pageLayout = subNC.getLayoutJSONString();
        if (StringUtils.isEmpty(pageLayout)) {
            try {
                pageLayout = LayoutUtil.createDefaultLayout(resultMap);
            } catch (IOException ex) {
                LOGGER.error("Default page layout could not be created: " + ex.getMessage(), ex);
            }
        }
        Set<HiLiteHandler> knownHiLiteHandlers = new HashSet<HiLiteHandler>();
        Set<HiLiteTranslator> knownTranslators = new HashSet<HiLiteTranslator>();
        Set<HiLiteManager> knownManagers = new HashSet<HiLiteManager>();
        for (HiLiteHandler initialHandler : initialHiliteHandlerSet) {
            getHiLiteTranslators(initialHandler, knownHiLiteHandlers, knownTranslators, knownManagers);
        }
        List<HiLiteTranslator> translatorList =
            knownTranslators.size() > 0 ? new ArrayList<HiLiteTranslator>(knownTranslators) : null;
        List<HiLiteManager> managerList = knownManagers.size() > 0 ? new ArrayList<HiLiteManager>(knownManagers) : null;
        WizardPageContent page = new WizardPageContent(pageID, resultMap, pageLayout, translatorList, managerList);
        page.setInfoMap(infoMap);
        //TODO page.setNestedContent(nestedContent);
        return page;
    }

    protected Map<NodeIDSuffix, WebViewContent> getWizardPageViewValueMapInternal(final NodeID subnodeID) {
        if (subnodeID == null) {
            LOGGER.error("No node ID supplied for creating wizard page");
            return null;
        }
        final WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        SubNodeContainer subNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        WorkflowManager subWFM = subNC.getWorkflowManager();
        return subWFM.findExecutedNodes(WizardNode.class, NOT_HIDDEN_FILTER).entrySet().stream()
            .filter(e -> !subWFM.getNodeContainer(e.getKey(), NativeNodeContainer.class, true).isInactive())
            .collect(Collectors.toMap(e -> NodeID.NodeIDSuffix.create(manager.getID(), e.getKey()),
                e -> e.getValue().getViewValue()));
    }

    private void getHiLiteTranslators(final HiLiteHandler handler, final Set<HiLiteHandler> knownHiLiteHandlers,
        final Set<HiLiteTranslator> knownTranslators, final Set<HiLiteManager> knownManagers) {
        if (handler == null || !knownHiLiteHandlers.add(handler)) {
            return;
        }
        Set<HiLiteTranslator> translators = handler.getHiLiteTranslators();
        for (HiLiteTranslator translator : translators) {
            if (translator != null && knownTranslators.add(translator)) {
                followHiLiteTranslator(translator, knownHiLiteHandlers, knownTranslators, knownManagers);
            }
        }
        Set<HiLiteManager> managers = handler.getHiLiteManagers();
        for (HiLiteManager manager : managers) {
            if (manager != null && knownManagers.add(manager)) {
                followHiLiteManager(manager, knownHiLiteHandlers, knownTranslators, knownManagers);
            }
        }
    }

    private void followHiLiteTranslator(final HiLiteTranslator translator, final Set<HiLiteHandler> knownHiLiteHandlers,
        final Set<HiLiteTranslator> knownTranslators, final Set<HiLiteManager> knownManagers) {
        getHiLiteTranslators(translator.getFromHiLiteHandler(), knownHiLiteHandlers, knownTranslators, knownManagers);
        if (translator.getToHiLiteHandlers() != null) {
            for (HiLiteHandler toHiLiteHandler : translator.getToHiLiteHandlers()) {
                getHiLiteTranslators(toHiLiteHandler, knownHiLiteHandlers, knownTranslators, knownManagers);
            }
        }
    }

    private void followHiLiteManager(final HiLiteManager manager, final Set<HiLiteHandler> knownHiLiteHandlers,
        final Set<HiLiteTranslator> knownTranslators, final Set<HiLiteManager> knownManagers) {
        getHiLiteTranslators(manager.getFromHiLiteHandler(), knownHiLiteHandlers, knownTranslators, knownManagers);
        if (manager.getToHiLiteHandlers() != null) {
            for (HiLiteHandler toHiLiteHandler : manager.getToHiLiteHandlers()) {
                getHiLiteTranslators(toHiLiteHandler, knownHiLiteHandlers, knownTranslators, knownManagers);
            }
        }
    }

    /**
     * Parameterizes {@link InputNode}s in the workflow (URL parameters).
     *
     * @param input non-null input
     * @throws InvalidSettingsException if wfm chokes
     * @see WorkflowManager#setInputNodes(Map)
     * @since 3.2
     */
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

    /**
     * Tries to load a map of view values to all appropriate views contained in a given subnode.
     *
     * @param viewContentMap the values to load
     * @param subnodeID the id fo the subnode containing the appropriate view nodes
     * @param validate true, if validation is supposed to be done before applying the values, false otherwise
     * @param useAsDefault true, if the given value map is supposed to be applied as new node defaults (overwrite node
     *            settings), false otherwise (apply temporarily)
     * @return Null or empty map if validation succeeds, map of errors otherwise
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Map<String, ValidationError> loadValuesIntoPageInternal(final Map<String, String> viewContentMap,
        final NodeID subnodeID, final boolean validate, final boolean useAsDefault) {
        if (subnodeID == null) {
            LOGGER.error("No node ID supplied for loading values into wizard page");
            return null;
        }
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        LOGGER.debugWithFormat("Loading view content into wizard nodes (%d)", viewContentMap.size());

        SubNodeContainer subNodeNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        Map<NodeID, WizardNode> wizardNodeSet = getWizardNodeSetForVerifiedID(subnodeID);

        if (validate) {
            Map<String, ValidationError> validationResult =
                validateViewValuesInternal(viewContentMap, subnodeID, wizardNodeSet);
            if (!validationResult.isEmpty()) {
                return validationResult;
            }
        }

        // validation succeeded, reset subnode and apply
        if (!subNodeNC.getInternalState().isExecuted()) { // this used to be an error but see SRV-745
            LOGGER.warnWithFormat(
                "Wrapped metanode (%s) not fully executed on appyling new values -- "
                    + "consider to change wrapped metanode layout to have self-contained executable units",
                subNodeNC.getNameWithID());
        }
        manager.resetSubnodeForViewUpdate(subnodeID, this);
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
                newViewValue.loadFromStream(new ByteArrayInputStream(entry.getValue().getBytes(Charset.forName("UTF-8"))));
                wizardNode.loadViewValue(newViewValue, useAsDefault);
                if (useAsDefault) {
                    subNodeNC.getWorkflowManager().getNodeContainer(id, SingleNodeContainer.class, true)
                        .saveNodeSettingsToDefault();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load view value into node \"" + id + "\" although validation succeeded", e);
            }
        }
        manager.configureNodeAndSuccessors(subnodeID, true);
        return Collections.emptyMap();
    }

    /** Before applying view values the subnode needs to possibly reset downstream nodes. For the wizard execution
     * downstream nodes should be executing (state = executing), whereas for single page mode/wrapped metanode view
     * none of the downstream nodes should be executing.
     * @param subNCId The id of the subnode container.
     * @throws IllegalStateException If state isn't correct.
     */
    abstract void stateCheckWhenApplyingViewValues(final SubNodeContainer snc);

    /** Similiar to {@link #stateCheckWhenApplyingViewValues(SubNodeContainer)} this will test the state of the
     * downstream nodes when applying new view values. For wizard execution downstream nodes must be in the
     * {@link NodeContainerState#isHalted() halted} state.
     * @throws IllegalStateException If state isn't correct.
     */
    abstract void stateCheckDownstreamNodesWhenApplyingViewValues(SubNodeContainer snc, NodeContainer downstreamNC);

    /** @return for wizard execution the downstream nodes are not reset when applying the view value on a subnode. */
    abstract boolean isResetDownstreamNodesWhenApplyingViewValue();

    /**
     * Validates a given set of serialized view values for a given subnode.
     *
     * @param viewValues the values to validate
     * @param subnodeID the id of the subnode containing the appropriate view nodes
     * @param wizardNodeSet the set of view nodes that the view values correspond to.
     * @return Null or empty map if validation succeeds, map of errors otherwise
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Map<String, ValidationError> validateViewValuesInternal(final Map<String, String> viewValues,
        final NodeID subnodeID, final Map<NodeID, WizardNode> wizardNodeSet) {
        if (subnodeID == null) {
            LOGGER.error("No node ID supplied for validating view values of wizard page");
            return null;
        }
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        Map<String, ValidationError> resultMap = new LinkedHashMap<String, ValidationError>();
        for (Map.Entry<String, String> entry : viewValues.entrySet()) {
            NodeID.NodeIDSuffix suffix = NodeID.NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(manager.getID());
            CheckUtils.checkState(id.hasPrefix(subnodeID), "The wizard page content for ID %s (suffix %s) "
                + "does not belong to the current Wrapped Metanode (ID %s)", id, entry.getKey(), subnodeID);
            WizardNode wizardNode = wizardNodeSet.get(id);
            CheckUtils.checkState(wizardNode != null,
                "No wizard node with ID %s in Wrapped Metanode, valid IDs are: " + "%s", id,
                ConvenienceMethods.getShortStringFrom(wizardNodeSet.entrySet(), 10));
            @SuppressWarnings("null")
            WebViewContent newViewValue = wizardNode.createEmptyViewValue();
            if (newViewValue == null) {
                // node has no view value
                continue;
            }
            ValidationError validationError = null;
            try {
                newViewValue.loadFromStream(new ByteArrayInputStream(entry.getValue().getBytes(Charset.forName("UTF-8"))));
                validationError = wizardNode.validateViewValue(newViewValue);
            } catch (Exception e) {
                resultMap.put(entry.getKey(),
                    new ValidationError("Could not deserialize JSON value: " + entry.getValue() + ": \n" + e.getMessage()));
            }
            if (validationError != null) {
                resultMap.put(entry.getKey(), validationError);
            }
        }
        if (!resultMap.isEmpty()) {
            return resultMap;
        }
        return Collections.emptyMap();
    }

    /**
     * Queries a subnode and returns all appropriate view nodes contained within.
     *
     * @param subnodeID the subnode id, not null
     * @return a map of view nodes
     */
    @SuppressWarnings("rawtypes")
    protected Map<NodeID, WizardNode> getWizardNodeSetForVerifiedID(final NodeID subnodeID) {
        if (subnodeID == null) {
            LOGGER.error("No node ID supplied while trying to retrieve node set for wizard page");
            return null;
        }
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        SubNodeContainer subNodeNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        WorkflowManager subNodeWFM = subNodeNC.getWorkflowManager();
        return subNodeWFM.findNodes(WizardNode.class, NOT_HIDDEN_FILTER, false);
    }

    /**
     * Returns a wizard node to a given subnode and node id
     * @param subnodeID the subnode id, which is the container for the wizard node
     * @param wizardNodeID the node id of the wizard node
     * @return the resolved wizard node or null, if node id does not denote a wizard node
     * @since 3.7
     */
    @SuppressWarnings("rawtypes")
    protected WizardNode getWizardNodeForVerifiedID(final NodeID subnodeID, final NodeID wizardNodeID) {
        CheckUtils.checkNotNull(subnodeID);
        CheckUtils.checkNotNull(wizardNodeID);
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        SubNodeContainer subNodeNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        NodeContainer cont = subNodeNC.getWorkflowManager().getNodeContainer(wizardNodeID);
        if (cont instanceof NativeNodeContainer) {
            NodeModel model = ((NativeNodeContainer)cont).getNodeModel();
            if (model instanceof WizardNode) {
                return (WizardNode)model;
            } else {
                LOGGER.error("Node model is not of type WizardNode");
            }
            LOGGER.error("Node container is not of type NativeNodeContainer");
        }
        return null;
    }

    /**
     * Retrieves the response for a view request, which is from a node within a subnode
     *
     * @param subnodeID the node id of the subnode container
     * @param nodeID the node id of the wizard node, as fetched from the combined view
     * @param viewRequest the JSON serialized view request string
     * @param exec the execution monitor to set progress and check possible cancellation
     * @return a {@link WizardViewResponse} which is generated by the concrete node
     * @throws ViewRequestHandlingException If the request handling or response generation fails for any
     * reason.
     * @throws InterruptedException If the thread handling the request is interrupted.
     * @throws CanceledExecutionException If the handling of the request was canceled e.g. by user
     * intervention.
     * @since 3.7
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected WizardViewResponse processViewRequestInternal(final NodeID subnodeID,
        final String nodeID, final String viewRequest, final ExecutionMonitor exec)
                throws ViewRequestHandlingException, InterruptedException, CanceledExecutionException {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        NodeID.NodeIDSuffix suffix = NodeID.NodeIDSuffix.fromString(nodeID);
        NodeID id = suffix.prependParent(manager.getID());
        WizardNode model = getWizardNodeForVerifiedID(subnodeID, id);
        if (model == null || !(model instanceof WizardViewRequestHandler)) {
            throw new ViewRequestHandlingException(
                "Node model can not process view requests. Possible implementation error.");
        }
        WizardViewRequest req = ((WizardViewRequestHandler)model).createEmptyViewRequest();
        try {
            req.loadFromStream(new ByteArrayInputStream(viewRequest.getBytes(Charset.forName("UTF-8"))));
        } catch (IOException ex) {
            throw new ViewRequestHandlingException("Error deserializing request: " + ex.getMessage(), ex);
        }
        return (WizardViewResponse)((WizardViewRequestHandler)model).handleRequest(req, exec);
    }

    /**
     * Composes the NodeID of a subnode.
     *
     * @param subnodeIDSuffix ...
     * @return new NodeID(m_manager.getID(), subnodeIDSuffix);
     */
    protected NodeID toNodeID(final int subnodeIDSuffix) {
        return new NodeID(m_manager.getID(), subnodeIDSuffix);
    }

    /**
     * Checks if the associated workflow manager has been discarded.
     *
     * @throws IllegalArgumentException if workflow manager is discarded
     */
    protected void checkDiscard() {
        CheckUtils.checkArgument(m_manager != null, "%s has been disconnected from workflow",
            WebResourceController.class.getSimpleName());
    }

    /** Sets manager to null. Called when new wizard is created on top of workflow. */
    void discard() {
    }

    /**
     * Result value of {@link WizardExecutionController#getCurrentWizardPage()} and
     * {@link SinglePageWebResourceController#getWizardPage()}.
     */
    public static final class WizardPageContent {

        private final NodeIDSuffix m_pageNodeID;

        @SuppressWarnings("rawtypes")
        private final Map<NodeIDSuffix, WizardNode> m_pageMap;

        private Map<NodeIDSuffix, WizardPageContent> m_nestedContent;

        @SuppressWarnings("rawtypes")
        private Map<NodeIDSuffix, WizardPageNodeInfo> m_infoMap;

        private final String m_layoutInfo;

        private final List<HiLiteTranslator> m_hiLiteTranslators;

        private final List<HiLiteManager> m_hiliteManagers;

        /**
         * @param pageNodeID
         * @param pageMap
         * @param layoutInfo
         */
        @SuppressWarnings("rawtypes")
        WizardPageContent(final NodeIDSuffix pageNodeID, final Map<NodeIDSuffix, WizardNode> pageMap,
            final String layoutInfo, final List<HiLiteTranslator> hiLiteTranslators,
            final List<HiLiteManager> hiLiteManagers) {
            m_pageNodeID = pageNodeID;
            m_pageMap = pageMap;
            m_infoMap = new LinkedHashMap<NodeIDSuffix, WizardPageNodeInfo>();
            m_layoutInfo = layoutInfo;
            m_hiLiteTranslators = hiLiteTranslators;
            m_hiliteManagers = hiLiteManagers;
        }

        /**
         * @return the pageNodeID
         * @since 3.3
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
         * @return the nestedContent
         * @since 3.7
         */
        public Map<NodeIDSuffix, WizardPageContent> getNestedContent() {
            return m_nestedContent;
        }

        /**
         * @param nestedContent the nestedContent to set
         * @since 3.7
         */
        public void setNestedContent(final Map<NodeIDSuffix, WizardPageContent> nestedContent) {
            m_nestedContent = nestedContent;
        }

        /**
         * @return the layoutInfo
         * @since 3.1
         */
        public String getLayoutInfo() {
            return m_layoutInfo;
        }

        /**
         * @return the hiLiteTranslators
         * @since 3.4
         */
        public List<HiLiteTranslator> getHiLiteTranslators() {
            return m_hiLiteTranslators;
        }

        /**
         * @return the hiliteManagers
         * @since 3.4
         */
        public List<HiLiteManager> getHiliteManagers() {
            return m_hiliteManagers;
        }

        /**
         * @param infoMap the infoMap to set
         * @since 3.5
         */
        public void setInfoMap(final Map<NodeIDSuffix, WizardPageNodeInfo> infoMap) {
            m_infoMap = infoMap;
        }

        /**
         * @return the infoMap
         * @since 3.5
         */
        public Map<NodeIDSuffix, WizardPageNodeInfo> getInfoMap() {
            return m_infoMap;
        }

        /**
         * Info object for individual nodes, containing e.g. node state
         * and possible warn/error messages.
         * @since 3.5
         */
        public static final class WizardPageNodeInfo {

            private String m_nodeName;
            private String m_nodeAnnotation;
            private NodeContainerState m_nodeState;
            private NodeMessage m_nodeMessage;

            /**
             * @return the nodeName
             */
            public String getNodeName() {
                return m_nodeName;
            }

            /**
             * @param nodeName the nodeName to set
             */
            public void setNodeName(final String nodeName) {
                m_nodeName = nodeName;
            }

            /**
             * @return the nodeAnnotation
             */
            public String getNodeAnnotation() {
                return m_nodeAnnotation;
            }

            /**
             * @param nodeAnnotation the nodeAnnotation to set
             */
            public void setNodeAnnotation(final String nodeAnnotation) {
                m_nodeAnnotation = nodeAnnotation;
            }

            /**
             * @return the nodeState
             */
            public NodeContainerState getNodeState() {
                return m_nodeState;
            }

            /**
             * @param nodeState the nodeState to set
             */
            public void setNodeState(final NodeContainerState nodeState) {
                m_nodeState = nodeState;
            }

            /**
             * @return the nodeMessage
             */
            public NodeMessage getNodeMessage() {
                return m_nodeMessage;
            }

            /**
             * @param nodeMessage the nodeMessage to set
             */
            public void setNodeMessage(final NodeMessage nodeMessage) {
                m_nodeMessage = nodeMessage;
            }

        }

    }

}
