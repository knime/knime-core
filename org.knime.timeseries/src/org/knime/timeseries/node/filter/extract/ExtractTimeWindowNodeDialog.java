/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ------------------------------------------------------------------------
 * 
 * History
 *   January 24, 2007 (rosaria): created 
 */
package org.knime.timeseries.node.filter.extract;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.util.DialogComponentCalendar;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * This dialog lets the user choose the column that contains the string values
 * that should be converted into Smiles values. 
 * 
 * @author Rosaria Silipo
 * @author Fabian Dill, KNIME.com GmbH
 */
public class ExtractTimeWindowNodeDialog extends DefaultNodeSettingsPane {

    private final SettingsModelString m_colName;
    private final SettingsModelCalendar m_from;
    private final SettingsModelCalendar m_to;
    
    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    public ExtractTimeWindowNodeDialog() {
        m_colName = createColumnNameModel();
        m_from = createFromModel();
        m_to = createToModel();
        DialogComponent columnChooser =
            new DialogComponentColumnNameSelection(m_colName,
                    "Columns containing Timestamp: ", 0, 
                    DateAndTimeValue.class);
        addDialogComponent(columnChooser);
        addDialogComponent(new DialogComponentCalendar(m_from, 
                "Select starting point:"));
        addDialogComponent(new DialogComponentCalendar(
                m_to, "Select end point:"));
    }
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        // get the spec for minimum and maximum date
    }
    
    /**
     * 
     * @return settings model to store the selected column
     */
    static SettingsModelString createColumnNameModel() {
        return new SettingsModelString("column_name", null); 
    }
    
    /** @return settings model for the "from" date.*/
    static SettingsModelCalendar createFromModel() {
        return new SettingsModelCalendar("timestamp_from", null);
    }
    /** @return settings model for the "to" date.*/
    static SettingsModelCalendar createToModel() {
        return new SettingsModelCalendar("timestamp_to", null);
    }
    
}
