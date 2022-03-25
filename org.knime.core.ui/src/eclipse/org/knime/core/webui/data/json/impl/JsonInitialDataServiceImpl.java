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
 *   Sep 8, 2021 (hornm): created
 */
package org.knime.core.webui.data.json.impl;

import java.util.function.Supplier;

import org.knime.core.node.NodeLogger;
import org.knime.core.webui.data.DataServiceException;
import org.knime.core.webui.data.json.JsonInitialDataService;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the {@link JsonInitialDataService} where the returned initial data is serialized into a JSON object
 * string.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <D> the type of the data object for initialization
 *
 * @since 4.5
 */
public class JsonInitialDataServiceImpl<D> implements JsonInitialDataService<D> {

    private final Supplier<D> m_dataSupplier;

    private final ObjectMapper m_mapper;

    /**
     * @param dataSupplier the data to initialize the ui extension with (lazily supplied)
     */
    public JsonInitialDataServiceImpl(final Supplier<D> dataSupplier) {
        this(dataSupplier, ObjectMapperUtil.getInstance().getObjectMapper());
    }

    /**
     * @param dataSupplier the data to initialize the ui extension with (lazily supplied)
     * @param mapper a custom object mapper
     */
    public JsonInitialDataServiceImpl(final Supplier<D> dataSupplier, final ObjectMapper mapper) {
        m_dataSupplier = dataSupplier;
        m_mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public D getInitialDataObject() {
        return m_dataSupplier.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toJson(final D dataObject) {
        try {
            return m_mapper.writeValueAsString(dataObject);
        } catch (JsonProcessingException ex) {
            NodeLogger.getLogger(getClass()).error(ex);
            final var cause = ex.getCause();
            if (cause instanceof DataServiceException) {
                throw (DataServiceException)cause;
            }
            throw new IllegalStateException("A problem occurred while obtaining initial data.", ex);
        }
    }

}
