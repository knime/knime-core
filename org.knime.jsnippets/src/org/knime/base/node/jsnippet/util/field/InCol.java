/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
import org.knime.core.data.DataType;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * A marker class for a field in the java snippet that represents an input column.
 *
 * @author Heiko Hofer
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class InCol extends JavaColumnField {

    @Override
    public boolean isInput() {
        return true;
    }

    @Override
    public void loadSettings(final Config config) throws InvalidSettingsException {
        super.loadSettings(config);

        if (m_converterFactoryId == null) {
            // backwards compatibility with pre-converters javasnippet
            // Find a converter which can convert given types
            final Optional<DataCellToJavaConverterFactory<?, ?>> factory =
                ConverterUtil.getConverterFactory(getDataType(), getJavaType());
            if (!factory.isPresent()) {
                throw new InvalidSettingsException(
                    "Cannot convert from " + getDataType().getName() + " to " + getJavaType().getName());
            }
            m_converterFactoryId = factory.get().getIdentifier();
        } else {
            final Optional<?> factory = ConverterUtil.getDataCellToJavaConverterFactory(m_converterFactoryId);
            if (!factory.isPresent()) {
                throw new InvalidSettingsException(
                    "Could not find ConverterFactory with ID \"" + m_converterFactoryId + "\"");
            }
        }
    }

    @Override
    public void loadSettingsForDialog(final Config config) {
        super.loadSettingsForDialog(config);

        if (m_converterFactoryId == null) {
            // need some additional magic to provide backwards compatibility with settings
            // that do not contain a converter factory id
            final Class<?> destType = getJavaType();
            final DataType sourceType = getDataType();

            final Optional<?> factory = ConverterUtil.getConverterFactory(sourceType, destType);
            if (factory.isPresent()) {
                m_converterFactoryId = ((DataCellToJavaConverterFactory<?, ?>)factory.get()).getIdentifier();
            } else {
                // TODO: will this produce a warning dialog...?
                throw new IllegalStateException("Was not able to find a ConverterFactory from " + destType.getName()
                    + " to " + sourceType.getName() + " to provide backwards compatibility for output column settings.");
            }
        }
    }

    /**
     * Set the converter factory to use for this field.
     *
     * The given factory must be able to convert cells of the given data type.
     *
     * @param dataType The data type of the field
     * @param factory The converter factory
     */
    public void setConverterFactory(final DataType dataType, final DataCellToJavaConverterFactory<?, ?> factory) {
        if (!dataType.getValueClasses().contains(factory.getSourceType())) {
            throw new IllegalArgumentException("Cells of the given DataType cannot be converted with factory.");
        }
        m_javaType = factory.getDestinationType();
        m_knimeType = dataType;
        m_converterFactoryId = factory.getIdentifier();
    }
}