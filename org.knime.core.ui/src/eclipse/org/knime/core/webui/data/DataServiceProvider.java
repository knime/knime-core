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
 *   Oct 16, 2021 (hornm): created
 */
package org.knime.core.webui.data;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.webui.data.text.TextApplyDataService;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;

/**
 * Provides different types of data services.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface DataServiceProvider {

    /**
     * @return optional service that provides data for initialization of the node view
     */
    Optional<InitialDataService> getInitialDataService();

    /**
     * @return optional service generally providing data to the node view
     */
    Optional<DataService> getDataService();

    /**
     * @return optional service to apply new data
     */
    Optional<ApplyDataService> getApplyDataService();

    /**
     * Helper to call the {@link TextInitialDataService}.
     *
     * @return the initial data
     * @throws IllegalStateException if there is not initial data service available
     */
    default String callTextInitialDataService() {
        var service = getInitialDataService().filter(TextInitialDataService.class::isInstance).orElse(null);
        if (service != null) {
            return ((TextInitialDataService)service).getInitialData();
        } else {
            throw new IllegalStateException("No text initial data service available");
        }
    }

    /**
     * Helper to call the {@link TextDataService}.
     *
     * @param request the data service request
     * @return the data service response
     * @throws IllegalStateException if there is no text data service
     */
    default String callTextDataService(final String request) {
        var service = getDataService().filter(TextDataService.class::isInstance).orElse(null);
        if (service != null) {
            return ((TextDataService)service).handleRequest(request);
        } else {
            throw new IllegalStateException("No text data service available");
        }
    }

    /**
     * Helper to call the {@link TextApplyDataService}.
     *
     * @param request the data service request representing the data to apply
     * @throws IOException if applying the data failed
     * @throws IllegalStateException if there is no text apply data service
     */
    default void callTextAppyDataService(final String request) throws IOException {
        var service = getApplyDataService().orElse(null);
        if (service instanceof TextReExecuteDataService) {
            ((TextReExecuteDataService)service).reExecute(request);
        } else if (service instanceof TextApplyDataService) {
            ((TextApplyDataService)service).applyData(request);
        } else {
            throw new IllegalStateException("No text apply data service available");
        }
    }

}
