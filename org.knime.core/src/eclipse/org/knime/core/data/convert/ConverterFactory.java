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
 *   25.05.2018 (Jonathan Hale): created
 */
package org.knime.core.data.convert;

import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;

/**
 * Interface for all factory classes which create converters.
 *
 * Serializable via {@link SerializeUtil#storeConverterFactory(ConverterFactory, ConfigBaseWO, String)}.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <ST> Type that is converted
 * @param <DT> Type into which can be converted
 * @since 3.6
 */
public interface ConverterFactory<ST, DT> {

    /**
     * Get the type which converters that were created by this factory are able to convert into.
     *
     * @return type of the values created by the converters produced by this factory.
     */
    public DT getDestinationType();

    /**
     * Get the type which converters that were created by this factory are able to convert from.
     *
     * @return type which the created converters can convert
     */
    public ST getSourceType();

    /**
     * A human readable name for this converter factory to be displayed in user interfaces for example.
     *
     * @return the name of this converter factory
     */
    default String getName() {
        return "";
    }

    /**
     * Get the identifier for this factory.
     *
     * The identifier is a unique string used to unambiguously reference this converter factory. Since this identifier
     * is used for persistence, it is required that the identifier is the same every runtime. If the identifier is not
     * unique, the factory may not be loaded, e.g. from the extension point.
     *
     * <p>
     * <b>Examples:</b>
     * </p>
     * <p>
     * "org.mypackage.MyConverterFactory&lt;MyType>"
     * </p>
     *
     * @return a unique identifier for this factory
     */
    public String getIdentifier();

    /**
     * Called when this factory is being serialized.
     *
     * Any additional configuration can be saved this way. E.g. format strings for a date time related converter.
     *
     * @param factoryConfig Config to save to
     */
    default void storeAdditionalConfig(final ConfigBaseWO factoryConfig) {
    }

    /**
     * Called when this factory is being deserialized.
     *
     * Any additional configuration can be loaded this way. E.g. format strings for a date time related converter.
     *
     * @param config Config to load from
     * @throws InvalidSettingsException If invalid settings were encountered
     */
    default void loadAdditionalConfig(final ConfigBaseRO config) throws InvalidSettingsException {
    }
}
