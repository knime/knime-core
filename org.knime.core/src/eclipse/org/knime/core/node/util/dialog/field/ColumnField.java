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
 *   21 Oct 2016 (albrecht): created
 */
package org.knime.core.node.util.dialog.field;

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * Settings for dialog column list field.
 *
 * <p>This class might change and is not meant as public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class ColumnField extends AbstractField {

    private static final String KNIME_TYPE = "Type";

    private DataType m_knimeType;

    /**
     * @return the knimeType
     */
    public DataType getKnimeType() {
        return m_knimeType;
    }

    /**
     * @param knimeType the knimeType to set
     */
    public void setKnimeType(final DataType knimeType) {
        m_knimeType = knimeType;
    }

    /** Saves current parameters to settings object.
     * @param config To save to.
     */
    @Override
    public void saveSettings(final Config config) {
        super.saveSettings(config);
        config.addDataType(KNIME_TYPE, m_knimeType);
    }

    /** Loads parameters in NodeModel.
     * @param config To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    @Override
    public void loadSettings(final Config config)
            throws InvalidSettingsException {
        super.loadSettings(config);
        m_knimeType = config.getDataType(KNIME_TYPE);
    }


    /** Loads parameters in Dialog.
     * @param config To load from.
     */
    @Override
    public void loadSettingsForDialog(final Config config) {
        super.loadSettingsForDialog(config);
        m_knimeType = config.getDataType(KNIME_TYPE, null);
    }

    /**
     * A marker class for a field in the java snippet that represents an input column.
     */
    public static class InColumnField extends ColumnField {
        // no additional fields
    }

}
