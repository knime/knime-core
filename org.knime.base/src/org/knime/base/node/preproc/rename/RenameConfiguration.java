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
 *   29.06.2015 (koetter): created
 */
package org.knime.base.node.preproc.rename;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Column rename configuration class.
 * @author Tobias Koetter, KNIME.com
 * @since 2.12
 */
public class RenameConfiguration {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RenameConfiguration.class);

    private final Map<String, RenameColumnSetting> m_settings = new LinkedHashMap<>();

    private final List<String> m_missingColumnNames = new LinkedList<>();

    /**
     * Constructor with an empty column map.
     */
    public RenameConfiguration() {
        //nothing to do
    }

    /**
     * @param settings the NodeSettingsRO to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    public RenameConfiguration(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (String identifier : settings) {
            NodeSettingsRO col = settings.getNodeSettings(identifier);
            m_settings.put(identifier, RenameColumnSetting.createFrom(col));
        }
    }

    /**
     * @param settings the NodeSettingsWO to write to
     */
    public void save(final NodeSettingsWO settings) {
        for (Entry<String, RenameColumnSetting> entry : m_settings.entrySet()) {
            NodeSettingsWO subSub = settings.addNodeSettings(entry.getKey());
            entry.getValue().saveSettingsTo(subSub);
        }
    }

    /**
     * @return the RenameColumnSettings
     */
    public Collection<RenameColumnSetting> getRenameColumnSettings() {
        return m_settings.values();
    }

    /**
     * To get the missing column names call {@link #getMissingColumnNames()} after this method.
     * @param inSpec the input DataTableSpec
     * @return the renamed input DataTableSpec
     * @throws InvalidSettingsException if the settings are invalid
     */
    public DataTableSpec getNewSpec(final DataTableSpec inSpec) throws InvalidSettingsException {
        m_missingColumnNames.clear();
        DataColumnSpec[] colSpecs = new DataColumnSpec[inSpec.getNumColumns()];

        HashMap<String, Integer> duplicateHash = new HashMap<>();

        List<RenameColumnSetting> renameSettings =
            m_settings == null ? new ArrayList<RenameColumnSetting>() : new ArrayList<>(
                m_settings.values());

        for (int i = 0; i < colSpecs.length; i++) {
            DataColumnSpec current = inSpec.getColumnSpec(i);
            String name = current.getName();
            RenameColumnSetting set = findAndRemoveSettings(name, renameSettings);
            DataColumnSpec newColSpec;
            if (set == null) {
                LOGGER.debug("No rename settings for column \"" + name + "\", leaving it untouched.");
                newColSpec = current;
            } else {
                newColSpec = set.configure(current);
            }
            String newName = newColSpec.getName();
            CheckUtils.checkSetting(StringUtils.isNotEmpty(newName), "Column name at index '%d' is empty.", i);

            Integer duplIndex = duplicateHash.put(newName, i);
            CheckUtils.checkSetting(duplIndex == null, "Duplicate column name '%s' at index '%d' and '%d'", newName,
                duplIndex, i);

            colSpecs[i] = newColSpec;
        }

        if (!renameSettings.isEmpty()) {
            for (RenameColumnSetting setting : renameSettings) {
                String name = setting.getName();
                if (StringUtils.isNotEmpty(name)) {
                    m_missingColumnNames.add(name);
                }
            }
        }
        return new DataTableSpec(colSpecs);
    }

    /**
     * @return the missingColumnNames
     */
    public List<String> getMissingColumnNames() {
        return m_missingColumnNames;
    }

    /**
     * Traverses the array m_settings and finds the settings object for a given column.
     *
     * @param colName The column name.
     * @param renameSettings
     * @return The settings to the column (if any), otherwise null.
     */
    private RenameColumnSetting findAndRemoveSettings(final String colName,
        final List<RenameColumnSetting> renameSettings) {
        Iterator<RenameColumnSetting> listIterator = renameSettings.iterator();
        while (listIterator.hasNext()) {
            RenameColumnSetting set = listIterator.next();
            if (set.getName().equals(colName)) {
                listIterator.remove();
                return set;
            }
        }
        return null;
    }
}
