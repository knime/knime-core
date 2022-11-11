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
 *   Jan 11, 2022 (hornm): created
 */
package org.knime.core.webui.node;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.DataServiceProvider;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.text.TextApplyDataService;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;
import org.knime.core.webui.node.util.NodeCleanUpCallback;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.PageUtil.PageType;
import org.knime.core.webui.page.Resource;

/**
 * Common logic for classes that manage node ui extensions (e.g. views, dialogs or port views).
 *
 * It manages
 * <p>
 * (i) the data services (i.e. {@link InitialDataService}, {@link DataService} and {@link ApplyDataService}). Data
 * service instances are only created once and cached until the respective node is disposed or the node state changes in
 * case of port views.
 * <p>
 * (ii) the page resources. I.e. keeps track of already accessed pages (to be able to also access page-related resources
 * - see {@link Page#getResource(String)}) and provides methods to determine page urls and paths.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @param <N> the node wrapper this manager operates on
 */
@SuppressWarnings("java:S1170")
public abstract class AbstractNodeUIManager<N extends NodeWrapper>
    implements DataServiceManager<N>, PageResourceManager<N> {

    private final Map<N, InitialDataService> m_initialDataServices = new WeakHashMap<>();

    private final Map<N, DataService> m_dataServices = new WeakHashMap<>();

    private final Map<N, ApplyDataService> m_applyDataServices = new WeakHashMap<>();

    private final Map<String, Page> m_pageMap = new HashMap<>();

    private final String m_pageType = getPageType().toString();

    /*
     * Domain name used to identify resources requested for a node view, node dialog or node port.
     */
    private final String m_domainName = "org.knime.core.ui." + m_pageType;

    private final String m_baseUrl = "http://" + m_domainName + "/";

    private final String m_nodeDebugPatternProp = "org.knime.ui.dev.node." + m_pageType + ".url.factory-class";

    private final String m_nodeDebugUrlProp = "org.knime.ui.dev.node." + m_pageType + ".url";

    /**
     * @param nodeWrapper
     * @return the data service provide for the given node
     */
    protected abstract DataServiceProvider getDataServiceProvider(N nodeWrapper);

    /**
     * The type of pages the node ui manager implementation manages. E.g. whether these are view-pages or dialog-pages.
     *
     * @return the page type
     */
    protected abstract PageType getPageType();

    /**
     * @return whether to clean-up the page- and data-service-instances on node state change
     */
    protected boolean shouldCleanUpPageAndDataServicesOnNodeStateChange() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <S> Optional<S> getDataServiceOfType(final N nodeWrapper, final Class<S> dataServiceClass) {
        Object ds = null;
        if (InitialDataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getInitialDataService(nodeWrapper).orElse(null);
        } else if (DataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getDataService(nodeWrapper).orElse(null);
        } else if (ApplyDataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getApplyDataService(nodeWrapper).orElse(null);
        }
        if (ds != null && !dataServiceClass.isAssignableFrom(ds.getClass())) {
            ds = null;
        }
        return Optional.ofNullable((S)ds);
    }

    /**
     * Calls {@link DataService#cleanUp()} if there is a data service instance available for the given node (wrapper).
     *
     * @param nodeWrapper
     */
    public final void cleanUpDataService(final N nodeWrapper) {
        if (m_dataServices.containsKey(nodeWrapper)) {
            m_dataServices.get(nodeWrapper).cleanUp();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String callTextInitialDataService(final N nodeWrapper) {
        var service = getInitialDataService(nodeWrapper).filter(TextInitialDataService.class::isInstance).orElse(null);
        if (service != null) {
            return ((TextInitialDataService)service).getInitialData();
        } else {
            throw new IllegalStateException("No text initial data service available");
        }
    }

    private Optional<InitialDataService> getInitialDataService(final N nodeWrapper) {
        InitialDataService ds;
        if (!m_initialDataServices.containsKey(nodeWrapper)) {
            ds = getDataServiceProvider(nodeWrapper).createInitialDataService().orElse(null);
            m_initialDataServices.put(nodeWrapper, ds);
            NodeCleanUpCallback.builder(nodeWrapper.get(), () -> m_initialDataServices.remove(nodeWrapper))
                .cleanUpOnNodeStateChange(shouldCleanUpPageAndDataServicesOnNodeStateChange()).build();
        } else {
            ds = m_initialDataServices.get(nodeWrapper);
        }
        return Optional.ofNullable(ds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String callTextDataService(final N nodeWrapper, final String request) {
        var service = getDataService(nodeWrapper).filter(TextDataService.class::isInstance).orElse(null);
        if (service != null) {
            return ((TextDataService)service).handleRequest(request);
        } else {
            throw new IllegalStateException("No text data service available");
        }
    }

    private Optional<DataService> getDataService(final N nodeWrapper) {
        DataService ds;
        if (!m_dataServices.containsKey(nodeWrapper)) {
            ds = getDataServiceProvider(nodeWrapper).createDataService().orElse(null);
            m_dataServices.put(nodeWrapper, ds);
            NodeCleanUpCallback.builder(nodeWrapper.get(), () -> {
                var dataService = m_dataServices.remove(nodeWrapper);
                if (dataService != null) {
                    dataService.cleanUp();
                }
            }).cleanUpOnNodeStateChange(shouldCleanUpPageAndDataServicesOnNodeStateChange()).build();
        } else {
            ds = m_dataServices.get(nodeWrapper);
        }
        return Optional.ofNullable(ds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void callTextApplyDataService(final N nodeWrapper, final String request) throws IOException {
        var service = getApplyDataService(nodeWrapper).orElse(null);
        if (service instanceof TextReExecuteDataService) {
            ((TextReExecuteDataService)service).reExecute(request);
        } else if (service instanceof TextApplyDataService) {
            ((TextApplyDataService)service).applyData(request);
        } else {
            throw new IllegalStateException("No text apply data service available");
        }
    }

    private Optional<ApplyDataService> getApplyDataService(final N nodeWrapper) {
        ApplyDataService ds;
        if (!m_applyDataServices.containsKey(nodeWrapper)) {
            ds = getDataServiceProvider(nodeWrapper).createApplyDataService().orElse(null);
            m_applyDataServices.put(nodeWrapper, ds);
            NodeCleanUpCallback.builder(nodeWrapper.get(), () -> m_applyDataServices.remove(nodeWrapper))
                .cleanUpOnNodeStateChange(shouldCleanUpPageAndDataServicesOnNodeStateChange()).build();
        } else {
            ds = m_applyDataServices.get(nodeWrapper);
        }
        return Optional.ofNullable(ds);
    }

    /**
     * Clears the page map.
     */
    protected final void clearPageMap() {
        m_pageMap.clear();
    }

    /**
     * For testing purposes only.
     *
     * @return the page map size
     */
    protected int getPageMapSize() {
        return m_pageMap.size();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getDebugUrl(final N nodeWrapper) {
        String pattern = System.getProperty(m_nodeDebugPatternProp);
        String url = System.getProperty(m_nodeDebugUrlProp);
        if (url == null) {
            return Optional.empty();
        }
        var nodeContainer = nodeWrapper.get();
        if (nodeContainer instanceof NativeNodeContainer) {
            @SuppressWarnings("rawtypes")
            final Class<? extends NodeFactory> nodeFactoryClass =
                ((NativeNodeContainer)nodeContainer).getNode().getFactory().getClass();
            if (pattern == null || Pattern.matches(pattern, nodeFactoryClass.getName())) {
                return Optional.of(url);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(url);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getDomainName() {
        return m_domainName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getPagePath(final N nodeWrapper) {
        var page = getPage(nodeWrapper);
        var pageId = getPageId(nodeWrapper, page);
        registerPage(nodeWrapper.get(), pageId, page);
        return pageId + "/" + page.getRelativePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Optional<String> getBaseUrl() {
        if (isRunAsDesktopApplication()) {
            return Optional.of(m_baseUrl);
        } else {
            return Optional.empty();
        }
    }

    private void registerPage(final NodeContainer nc, final String pageId, final Page page) {
        m_pageMap.computeIfAbsent(pageId, id -> {
            NodeCleanUpCallback.builder(nc, () -> m_pageMap.remove(pageId))
                .cleanUpOnNodeStateChange(shouldCleanUpPageAndDataServicesOnNodeStateChange()).build();
            return page;
        });
    }

    @Override
    public final Optional<Resource> getPageResource(final String resourceId) {
        var split = resourceId.indexOf("/");
        if (split <= 0) {
            return Optional.empty();
        }

        var pageId = resourceId.substring(0, split);
        var page = m_pageMap.get(pageId);
        if (page == null) {
            return Optional.empty();
        }

        var relPath = resourceId.substring(split + 1, resourceId.length());
        if (page.getRelativePath().equals(relPath)) {
            return Optional.of(page);
        } else {
            return page.getResource(relPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Optional<Resource> getPageResourceFromUrl(final String url) {
        return getPageResource(getResourceIdFromUrl(url));
    }

    private String getResourceIdFromUrl(final String url) {
        return url.replace(m_baseUrl, "");
    }

    private static boolean isRunAsDesktopApplication() {
        return !"true".equals(System.getProperty("java.awt.headless"));
    }

}
