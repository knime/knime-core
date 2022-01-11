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
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.DataServiceProvider;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.text.TextApplyDataService;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;

/**
 * Manages the data services (i.e. {@link InitialDataService}, {@link DataService} and {@link ApplyDataService})
 * available for node views and node dialogs.
 *
 * Data service instances are only created once and cached until the respective node is disposed.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public abstract class DataServiceManager {

    private final Map<NodeContainer, InitialDataService> m_initialDataServices = new WeakHashMap<>();

    private final Map<NodeContainer, DataService> m_dataServices = new WeakHashMap<>();

    private final Map<NodeContainer, ApplyDataService> m_applyDataServices = new WeakHashMap<>();

    /**
     * @param nc
     * @return the data service provide for the given node
     */
    protected abstract DataServiceProvider getDataServiceProvider(NodeContainer nc);

    /**
     * Returns data service instance of the given type or an empty optional of no data service of that type is
     * associated with the node.
     *
     * @param <S> the data service type being returned
     * @param nc node to get the data service for
     * @param dataServiceClass A type (or sub-type) of {@link InitialDataService}, {@link DataService} or
     *            {@link ApplyDataService}.
     * @return the data service instance or an empty optional if no data service is available
     */
    @SuppressWarnings("unchecked")
    public <S> Optional<S> getDataServiceOfType(final NodeContainer nc, final Class<S> dataServiceClass) {
        Object ds = null;
        if (InitialDataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getInitialDataService(nc).orElse(null);
        } else if (DataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getDataService(nc).orElse(null);
        } else if (ApplyDataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getApplyDataService(nc).orElse(null);
        }
        if (ds != null && !dataServiceClass.isAssignableFrom(ds.getClass())) {
            ds = null;
        }
        return Optional.ofNullable((S)ds);
    }

    /**
     * Helper to call the {@link TextInitialDataService}.
     *
     * @param nc the node to call the data service for
     * @return the initial data
     * @throws IllegalStateException if there is not initial data service available
     */
    public String callTextInitialDataService(final NodeContainer nc) {
        var service = getInitialDataService(nc).filter(TextInitialDataService.class::isInstance).orElse(null);
        if (service != null) {
            return ((TextInitialDataService)service).getInitialData();
        } else {
            throw new IllegalStateException("No text initial data service available");
        }
    }

    private Optional<InitialDataService> getInitialDataService(final NodeContainer nc) {
        InitialDataService ds;
        if (!m_initialDataServices.containsKey(nc)) {
            ds = getDataServiceProvider(nc).createInitialDataService().orElse(null);
            m_initialDataServices.put(nc, ds);
        } else {
            ds = m_initialDataServices.get(nc);
        }
        return Optional.ofNullable(ds);
    }

    /**
     * Helper to call the {@link TextDataService}.
     *
     * @param nc the node to call the data service for
     * @param request the data service request
     * @return the data service response
     * @throws IllegalStateException if there is no text data service
     */
    public String callTextDataService(final NodeContainer nc, final String request) {
        var service = getDataService(nc).filter(TextDataService.class::isInstance).orElse(null);
        if (service != null) {
            return ((TextDataService)service).handleRequest(request);
        } else {
            throw new IllegalStateException("No text data service available");
        }
    }

    private Optional<DataService> getDataService(final NodeContainer nc) {
        DataService ds;
        if (!m_dataServices.containsKey(nc)) {
            ds = getDataServiceProvider(nc).createDataService().orElse(null);
            m_dataServices.put(nc, ds);
        } else {
            ds = m_dataServices.get(nc);
        }
        return Optional.ofNullable(ds);
    }

    /**
     * Helper to call the {@link TextApplyDataService}.
     *
     * @param nc the node to call the data service for
     * @param request the data service request representing the data to apply
     * @throws IOException if applying the data failed
     * @throws IllegalStateException if there is no text apply data service
     */
    public void callTextAppyDataService(final NodeContainer nc, final String request) throws IOException {
        var service = getApplyDataService(nc).orElse(null);
        if (service instanceof TextReExecuteDataService) {
            ((TextReExecuteDataService)service).reExecute(request);
        } else if (service instanceof TextApplyDataService) {
            ((TextApplyDataService)service).applyData(request);
        } else {
            throw new IllegalStateException("No text apply data service available");
        }
    }

    private Optional<ApplyDataService> getApplyDataService(final NodeContainer nc) {
        ApplyDataService ds;
        if (!m_applyDataServices.containsKey(nc)) {
            ds = getDataServiceProvider(nc).createApplyDataService().orElse(null);
            m_applyDataServices.put(nc, ds);
        } else {
            ds = m_applyDataServices.get(nc);
        }
        return Optional.ofNullable(ds);
    }

}
