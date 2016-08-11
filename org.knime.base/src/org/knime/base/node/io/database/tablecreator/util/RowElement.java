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
 *   Dec 3, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Abstract class of a row element in the table
 *
 * @author Budi Yanto, KNIME.com
 */
abstract class RowElement {

    private final String m_prefix;

    /**
     * Creates a new instance of RowElement
     *
     * @param prefix prefix of the RowElement
     */
    protected RowElement(final String prefix) {
        this(prefix, null);

    }

    /**
     * Creates a new instance of RowElement
     *
     * @param prefix prefix of the RowElement
     * @param settings the NodeSettingsRO instance to load from
     */
    protected RowElement(final String prefix, final NodeSettingsRO settings) {
        m_prefix = prefix;
        if (settings != null) {
            loadSettingsFrom(settings);
        }
    }

    /**
     * Returns the prefix of the RowElement
     *
     * @return the prefix of the RowElement
     */
    String getPrefix() {
        return m_prefix;
    }

    /**
     * Load the attributes of RowElement from the given NodeSettingsRO instance
     *
     * @param settings the NodeSettingsRO instance to load from
     */
    protected abstract void loadSettingsFrom(final NodeSettingsRO settings);

    /**
     * Save the properties of RowElement to the NodeSettingsWO instance
     *
     * @param settings the NodeSettingsWO instance to save to
     */
    protected abstract void saveSettingsTo(final NodeSettingsWO settings);

}
