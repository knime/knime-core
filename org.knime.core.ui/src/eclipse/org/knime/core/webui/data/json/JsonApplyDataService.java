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
 *   Sep 14, 2021 (hornm): created
 */
package org.knime.core.webui.data.json;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.webui.data.text.TextApplyDataService;

/**
 * A {@link TextApplyDataService} where the data to apply is represented as a JSON-object.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <D> the type of the data object to apply
 *
 * @since 4.5
 */
public interface JsonApplyDataService<D> extends TextApplyDataService {

    /**
     * {@inheritDoc}
     */
    @Override
    default Optional<String> validateData(final String data) throws IOException {
        return validateData(fromJson(data));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void applyData(final String data) throws IOException {
        applyData(fromJson(data));
    }

    /**
     * @param data the data object to validate
     * @return an empty optional if data is valid, otherwise a validation error string
     */
    Optional<String> validateData(D data);

    /**
     * @param data the data object to apply
     */
    void applyData(D data);

    /**
     * Deserializes the data object from a JSON-string.
     *
     * @param data a JSON-string
     * @return the deserialized data object
     * @throws IOException if the deserialization failed
     */
    D fromJson(String data) throws IOException;
}
