/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   22 May 2023 (carlwitt): created
 */
package org.knime.core.data.property;

import java.util.NoSuchElementException;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.config.base.ConfigBase;

/**
 * Wrapper for {@link ValueFormatModel}s that takes care of serializing to and from {@link ConfigBase}.
 *
 * The name Handler is for consistency reasons with the analogous classes for color models etc.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 */
public final class ValueFormatHandler implements PropertyHandler {

    /** Config key for the format model class. */
    private static final String CFG_FORMAT_MODEL_CLASS = "format_model_class";

    /** Config key for the format model extension name. */
    private static final String CFG_FORMAT_MODEL_PROVIDER = "format_model_provider";

    /** Config key for the format model config. */
    private static final String CFG_FORMAT_MODEL = "format_model";

    private final ValueFormatModel m_model;

    /**
     * @param model defines an html representation for each data cell
     * @throws IllegalArgumentException if the model is <code>null</code>
     */
    public ValueFormatHandler(final ValueFormatModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Format model must not be null.");
        }
        m_model = model;
    }

    /**
     * @param dv to format
     * @return a string representation of the data value with optional markup,
     * i.e., no html or body tags but may contain styled spans. Never <code>null</code>.
     */
    public String get(final DataValue dv) {
        return m_model.getHTML(dv);
    }

    /**
     * @param dv to format
     * @return a plaintext string representation of the data value. Never <code>null</code>.
     * @since 5.3
     */
    public String getPlaintext(final DataValue dv) {
        return m_model.getPlaintext(dv);
    }

    /**
     * Saves the parameters of the value formatter.
     * @param config empty subtree to save to
     * @throws NullPointerException if the <i>config</i> is <code>null</code>
     */
    public void save(final ConfigWO config) {
        var id = m_model.getClass().getName();
        config.addString(CFG_FORMAT_MODEL_CLASS, id);
        config.addString(CFG_FORMAT_MODEL_PROVIDER,
            ValueFormatModelRegistry.getFactoryProvider(id).orElse("<unknown>"));
        m_model.save(config.addConfig(CFG_FORMAT_MODEL));
    }

    /**
     * @param config to read value formatter settings from
     * @return a new formatter
     * @throws InvalidSettingsException if either the class or model settings could not be read
     * @throws NullPointerException if the <code>config</code> is <code>null</code>
     */
    public static ValueFormatHandler load(final ConfigRO config) throws InvalidSettingsException {
        try {
            var fac = ValueFormatModelRegistry.getFactory(config.getString(CFG_FORMAT_MODEL_CLASS)).orElseThrow();
            return new ValueFormatHandler(fac.getFormatter(config.getConfig(CFG_FORMAT_MODEL)));
        } catch (NoSuchElementException nsee) {
            final var msg = String.format("Can't load attached formatter \"%s\" from the bundle \"%s\".",
                config.getString(CFG_FORMAT_MODEL_CLASS), config.getString(CFG_FORMAT_MODEL_PROVIDER));
            throw new InvalidSettingsException(msg, nsee);
        }
    }

    /**
     * @return the FormatModel of this handler
     */
    public ValueFormatModel getFormatModel() {
        return m_model;
    }

    /**
     * @return a summary of the model type and its parameters.
     */
    @Override
    public String toString() {
        return m_model.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || ((obj instanceof ValueFormatHandler hdl) && m_model.equals(hdl.m_model));
    }

    @Override
    public int hashCode() {
        return m_model.hashCode();
    }

}
