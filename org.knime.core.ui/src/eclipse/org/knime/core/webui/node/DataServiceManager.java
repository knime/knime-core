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
 *   Feb 24, 2022 (hornm): created
 */
package org.knime.core.webui.node;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.text.TextApplyDataService;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <N> the node wrapper this manager operates on
 */
public interface DataServiceManager<N extends NodeWrapper<? extends NodeContainer>> {

    /**
     * Returns data service instance of the given type or an empty optional of no data service of that type is
     * associated with the node.
     *
     * @param <S> the data service type being returned
     * @param nodeWrapper node to get the data service for
     * @param dataServiceClass A type (or sub-type) of {@link InitialDataService}, {@link DataService} or
     *            {@link ApplyDataService}.
     * @return the data service instance or an empty optional if no data service is available
     */
    <S> Optional<S> getDataServiceOfType(N nodeWrapper, Class<S> dataServiceClass);

    /**
     * Helper to call the {@link TextInitialDataService}.
     *
     * @param nodeWrapper the node to call the data service for
     * @return the initial data
     * @throws IllegalStateException if there is not initial data service available
     */
    String callTextInitialDataService(N nodeWrapper);

    /**
     * Helper to call the {@link TextDataService}.
     *
     * @param nodeWrapper the node to call the data service for
     * @param request the data service request
     * @return the data service response
     * @throws IllegalStateException if there is no text data service
     */
    String callTextDataService(N nodeWrapper, String request);

    /**
     * Helper to call the {@link TextApplyDataService}.
     *
     * @param nodeWrapper the node to call the data service for
     * @param request the data service request representing the data to apply
     * @throws IOException if applying the data failed
     * @throws IllegalStateException if there is no text apply data service
     */
    void callTextApplyDataService(N nodeWrapper, String request) throws IOException;

}