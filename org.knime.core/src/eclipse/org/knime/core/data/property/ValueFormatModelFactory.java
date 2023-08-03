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
 *   2 Aug 2023 (jasper): created
 */
package org.knime.core.data.property;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;

/**
 * A factory for a {@link ValueFormatModel}. Implementing classes across repositories must be registered for the
 * extension point {@code org.knime.core.DataValueFormatter}. Every implementation must have a zero-argument constructor
 * so that it can be instantiated by the platform registry.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @param <T> The {@link ValueFormatModel} class that this factory will "produce"
 * @since 5.2
 */
public interface ValueFormatModelFactory<T extends ValueFormatModel> {
    /**
     * Get a description of the attached formatter
     *
     * @return a string describing the formatter
     */
    String getDescription();

    /**
     * Get an instance of the appropriate {@link ValueFormatModel} with the settings that were stored by
     * {@link ValueFormatModel#save(org.knime.core.node.config.ConfigWO)}.
     *
     * @param config The settings for the formatter
     * @return an instance of the formatter
     * @throws InvalidSettingsException If the provided settings are invalid for this formatter
     */
    T getFormatter(ConfigRO config) throws InvalidSettingsException;

    /**
     * Returns the formatter class that is also used to parametrise the factory class. This must be consistent with the
     * return type of {@link #getFormatter(ConfigRO)}.
     *
     * @return A {@link Class} instance of the type {@code T}
     */
    Class<T> getFormatterClass();
}
