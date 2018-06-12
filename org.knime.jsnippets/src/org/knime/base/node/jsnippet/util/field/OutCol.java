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
 *   17.10.2016 (Jonathan Hale): created
 */
package org.knime.base.node.jsnippet.util.field;

import java.util.Optional;

import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * A class for a field in the java snippet that represents an output column.
 *
 * @author Heiko Hofer
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class OutCol extends JavaColumnField {
    static final String REPLACE_EXISTING = "replaceExisting";

    private boolean m_replaceExisting;

    private Optional<JavaToDataCellConverterFactory<?>> m_factory = Optional.empty();

    /**
     * Create an instance.
     */
    public OutCol() {
        m_replaceExisting = false;
    }

    /**
     * @return the replaceExisting
     */
    public boolean getReplaceExisting() {
        return m_replaceExisting;
    }

    /**
     * @param replaceExisting the replaceExisting to set
     */
    public void setReplaceExisting(final boolean replaceExisting) {
        m_replaceExisting = replaceExisting;
    }

    @Override
    public Class<?> getJavaType() {
        final Optional<JavaToDataCellConverterFactory<?>> factory = getConverterFactory();
        if (factory.isPresent()) {
            return factory.get().getSourceType();
        }
        return super.getJavaType();
    }

    @Override
    public void saveSettings(final Config config) {
        super.saveSettings(config);
        config.addBoolean(REPLACE_EXISTING, m_replaceExisting);
    }

    @Override
    public void loadSettings(final Config config) throws InvalidSettingsException {
        super.loadSettings(config);
        m_replaceExisting = config.getBoolean(REPLACE_EXISTING);

        if (m_converterFactoryId == null) {
            // Throws InvalidSettingsException if type cannot be loaded, hence we can assume its presence
            // later on
            loadJavaType();

            // backwards compatibility with pre-converters javasnippet
            // Find a converter which can convert given types
            m_factory = ConverterUtil.getConverterFactory(getJavaType(), getDataType());
            if (!m_factory.isPresent()) {
                throw new InvalidSettingsException(
                    "Cannot convert from " + getJavaType().getName() + " to " + getDataType().getName());
            }
            m_converterFactoryId = m_factory.get().getIdentifier();
        }
    }

    @Override
    public boolean isInput() {
        return false;
    }

    /**
     * Set the converter factory to use for this field.
     *
     * @param factory
     */
    public void setConverterFactory(final JavaToDataCellConverterFactory<?> factory) {
        m_javaType = factory.getSourceType();
        m_knimeType = factory.getDestinationType();
        m_converterFactoryId = factory.getIdentifier();
        m_factory = Optional.of(factory);
    }

    /**
     * Get the converter factory associated with this input column field.
     *
     * @return An optional converter factory, present if converter factory id setting is valid, empty if not found.
     */
    public Optional<JavaToDataCellConverterFactory<?>> getConverterFactory() {
        if (!m_factory.isPresent() || (m_factory.isPresent() && !m_factory.get().getIdentifier().equals(m_converterFactoryId))) {
            m_factory = ConverterUtil.getJavaToDataCellConverterFactory(m_converterFactoryId);
        }
        return m_factory;
    }
}