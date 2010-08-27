/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ----------------------------------------------------------------------------
 */
package org.knime.base.node.io.database;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Panel which allows to specify an SQL type for each column.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBSQLTypesPanel extends JPanel {

    /** Keep column name from spec to text field which contains the SQL type. */
    private final Map<String, JTextField> m_map;

    /**
     * Creates new empty panel.
     */
    DBSQLTypesPanel() {
        super(new GridLayout(0, 1));
        m_map = new LinkedHashMap<String, JTextField>();
    }

    /**
     * Reads settings and inits the panel with column name to SQL-type
     * mapping read from spec and settings object. If no type is available
     * to default types are used.
     * @param settings Settings object to read specified SQL-types from.
     * @param specs The data spec to retrieve all column names and types.
     */
    void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        m_map.clear();
        super.removeAll();
        if (specs == null || specs[0] == null) {
            return;
        }
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            String colName = cspec.getName();
            JLabel label = new JLabel(colName.replaceAll("[^a-zA-Z0-9]", "_"));
            int labelHeight = label.getPreferredSize().height;
            label.setIcon(cspec.getType().getIcon());
            label.setPreferredSize(new Dimension(150, labelHeight));
            label.setMinimumSize(new Dimension(200, labelHeight));
            JTextField textFld = new JTextField();
            textFld.setPreferredSize(new Dimension(250, 25));
            textFld.setMinimumSize(new Dimension(250, 25));
            String type = null;
            if (settings != null) {
                type = settings.getString(colName, null);
            }
            if (type != null) {
                textFld.setText(type);
            } else if (cspec.getType().isCompatible(IntValue.class)) {
                textFld.setText(DBWriterNodeModel.SQL_TYPE_INTEGER);
            } else if (cspec.getType().isCompatible(DoubleValue.class)) {
                textFld.setText(DBWriterNodeModel.SQL_TYPE_DOUBLE);
            } else if (cspec.getType().isCompatible(DateAndTimeValue.class)) {
                textFld.setText(DBWriterNodeModel.SQL_TYPE_DATEANDTIME);
            } else {
                textFld.setText(DBWriterNodeModel.SQL_TYPE_STRING);
            }
            p.add(label);
            p.add(textFld);
            super.add(p);
            m_map.put(colName, textFld);
        }
    }

    /**
     * Saves SQL types by column name.
     * @param settings Save column to SQL mapping to.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        for (Map.Entry<String, JTextField> e : m_map.entrySet()) {
            String type = e.getValue().getText().trim();
            settings.addString(e.getKey(), type);
        }
    }
}
