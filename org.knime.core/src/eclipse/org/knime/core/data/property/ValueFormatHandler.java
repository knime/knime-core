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

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Final <code>FormatHandler</code> implementation as container which forwards
 * html conversion requests for a {@link org.knime.core.data.DataCell} to its underlying
 * {@link org.knime.core.data.property.ColorModel}.
 * The <code>ColorModel</code> can be loaded and saved
 * from <code>Config</code> object.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 */
public final class ValueFormatHandler implements PropertyHandler {

    /** Config key for the format model class. */
    private static final String CFG_FORMAT_MODEL_CLASS = "format_model_class";

    /** Config key for the format model config. */
    private static final String CFG_FORMAT_MODEL = "format_model";

    private final ValueFormatModel m_model;

    /**
     * @param model defines a html representation for each data cell
     * @throws IllegalArgumentException if the model is <code>null</code>
     */
    public ValueFormatHandler(final ValueFormatModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Format model must not be null.");
        }
        m_model = model;
    }

    /**
     * TODO
     * Returns a <code>ColorAttr</code> object as specified by the content
     * of the given <code>DataCell</code>. Requests are forwarded to the
     * underlying <code>ColorModel</code>. If no <code>ColorAttr</code>
     * is assigned to the given <code>dc</code>, this method returns the
     * {@link ColorAttr#DEFAULT} as default color, but never <code>null</code>.
     *
     * @param dc <code>DataCell</code> used to generate color
     * @return a <code>ColorAttr</code> object assigned to the given cell
     * @see ColorAttr#DEFAULT
     */
    public String get(final DataCell dc) {
        return m_model.getHTML(dc);
    }

    /**
     * Saves the underlying <code>ColorModel</code> to the given
     * <code>Config</code> by adding the <code>ColorModel</code> class as
     * String and calling
     * {@link ColorModel#save(ConfigWO)} within the model.
     * @param config color settings are saved to
     * @throws NullPointerException if the <i>config</i> is <code>null</code>
     */
    public void save(final ConfigWO config) {
        config.addString(CFG_FORMAT_MODEL_CLASS, m_model.getClass().getName());
        m_model.save(config.addConfig(CFG_FORMAT_MODEL));
    }

    /**
     * Reads the color model settings from the given <code>Config</code>, inits
     * a new <code>ColorModel</code>, and returns a new
     * <code>ColorHandler</code>.
     * @param config read color settings from
     * @return a new <code>ColorHandler</code> object created with the color
     *         model settings read from <code>config</code>
     * @throws InvalidSettingsException if either the class or color model
     *         settings could not be read
     * @throws NullPointerException if the <code>config</code> is
     *         <code>null</code>
     */
    public static ValueFormatHandler load(final ConfigRO config)
            throws InvalidSettingsException {
        String modelClass = config.getString(CFG_FORMAT_MODEL_CLASS);
        if (modelClass.equals(ValueFormatModelNumber.class.getName())) {
            ConfigRO subConfig = config.getConfig(CFG_FORMAT_MODEL);
            return new ValueFormatHandler(ValueFormatModelNumber.load(subConfig));
        } else {
            throw new InvalidSettingsException("Unknown FormatModel class: " + modelClass);
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
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ValueFormatHandler)) {
            return false;
        }
        return m_model.equals(((ValueFormatHandler)obj).m_model);
    }

    @Override
    public int hashCode() {
        return m_model.hashCode();
    }

}
