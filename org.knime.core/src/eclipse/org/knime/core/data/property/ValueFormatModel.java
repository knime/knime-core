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
 *  22 May 2023 (carlwitt): created
 */
package org.knime.core.data.property;

import org.knime.core.data.DataValue;
import org.knime.core.node.config.ConfigWO;

/**
 * Interface for all Data Value Formatters that are used to represent Data Values in a WebView (e.g. the Table preview).
 * The containing function maps data values to an HTML String. Missing Values need to be handled separately.
 *
 * TODO replace with DataValueTextRenderer after moving it from core-ui to core
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 */
public interface ValueFormatModel {
    /**
     * Formats a data value to an HTML String, representing the underlying value
     *
     * @param dataValue
     * @return an HTML String that represents the data value
     */
    String getHTML(DataValue dataValue);

    /**
     * Formats a data value to a plaintext / non-HTML String, representing the underlying value. This might not apply
     * all configured formatting options, but only those that can be realized without HTML / CSS Tags
     *
     * This string might not be properly escaped for usage in an HTML context, e.g. it might return "I <3 you", and not
     * "I &lt;3 you". Therefore, if this string is rendered in an HTML context, it needs to be escaped.
     *
     * By default, this method delegates to the "getHTML" method, although it should be implemented by all implementing
     * classes.
     *
     * @param dataValue
     * @return a String that represents the data value
     * @since 5.3
     */
    default String getPlaintext(final DataValue dataValue) {
        return getHTML(dataValue);
    }

    /**
     * Saves the settings of the formatter to a provided {@link ConfigWO} object. Override this method if your formatter
     * has any settings that need to be persisted
     *
     * @param config the configuration object to write to
     */
    default void save(final ConfigWO config) {
        // no-op
    }

}
