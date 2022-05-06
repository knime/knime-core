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
package org.knime.core.webui.data.json;

import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.data.DataServiceException;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;
import org.knime.core.webui.data.text.TextInitialDataService;

/**
 * Implementation of the {@link TextInitialDataService} where the returned initial data is serialized into a JSON object
 * string.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <D> the type of the data object for initialization
 *
 * @since 4.5
 */
public interface JsonInitialDataService<D> extends TextInitialDataService {

    /**
     * {@inheritDoc}
     */
    @Override
    default String getInitialData() {
        final var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
        try {
            final var root = mapper.createObjectNode();
            // Since the DataServiceContext is public API, warning messages could have been wrongfully added to it.
            // We clear the context here to make sure there are no "stale" warning messages.
            DataServiceContext.getContext().clear();
            root.set("result", mapper.readTree(toJson(getInitialDataObject())));
            // We have to get the DataServiceContext again here, since the context may have changed since (or as a
            // consequence of) clearing it
            final var warningMessages = DataServiceContext.getContext().getWarningMessages();
            if (warningMessages != null && warningMessages.length > 0) {
                root.set("warningMessages", mapper.valueToTree(warningMessages));
            }
            return root.toString();
        } catch (DataServiceException e) {
            return mapper.createObjectNode().set("userError", mapper.valueToTree(new InitialDataUserError(e)))
                .toString();
        } catch (Throwable t) { // NOSONAR
            return mapper.createObjectNode().set("internalError", mapper.valueToTree(new InitialDataInternalError(t)))
                .toString();
        } finally {
            DataServiceContext.getContext().clear();
        }
    }

    /**
     * Obtains an object representing the initial data.
     * <p>
     * Clients implementing this method can obtain a {@link DataServiceContext}. This context can be used to add warning
     * messages, which will then be added to the initial data.
     *
     * @return a object representing the initial data
     */
    D getInitialDataObject();

    /**
     * Turns the initial data object into a JSON-string.
     * <p>
     * Clients implementing this method can obtain a {@link DataServiceContext}. This context can be used to add warning
     * messages, which will then be added to the initial data.
     *
     * @param dataObject
     * @return the json-serialized initial data object
     */
    String toJson(D dataObject);

}
