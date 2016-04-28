/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.timeseries.node.dateshift;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.node.diff.Granularity;
import org.knime.timeseries.util.DialogComponentCalendar;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * Dialog for the Date/Time Shift node with a column selection for the numerical
 * column, one for the  date column, a text field for the new column name, a
 * selection list for the desired granularity (year, quarter, month, week, day,
 * hour, minute) of the numerical value and a selection for a fixed date.
 *
 *
 * @author Iris Adae, University Konstanz
 */
public class DateShiftNodeDialog extends DefaultNodeSettingsPane {

    /** the key for using the execution time. */
    public static final String CFG_NOW = "Use execution time";
    /** the key for using a column as the  time. */
    public static final String CFG_COLUMN = "Use date/time column";
    /** the key for using a specific time as the second time. */
    public static final String CFG_FIXDATE = "Use fixed date";
    /** the key for using a value column for the shift. */
    public static final String CFG_COLUMN_SHIFT = "Use shift value from column";
    /** the key for using a constant value for the shift. */
    public static final String CFG_VALUE_SHIFT = "Use static shift value";


    private final SettingsModelBoolean m_replaceColumn = DateShiftConfigure.createReplaceColumnModel();
    private final SettingsModelString m_appendedColName = DateShiftConfigure.createNewColNameModel(m_replaceColumn);
    private final SettingsModelString m_referencemodel = DateShiftConfigure.createReferenceTypeModel();
    private final SettingsModelCalendar m_fixTimeComponent = DateShiftConfigure.createCalendarModel();
    private final SettingsModelString m_columnSelComponent = DateShiftConfigure.createDateColumnModel();

    private final SettingsModelString m_shiftmodel = DateShiftConfigure.createShiftTypeModel();
    private final SettingsModelInteger m_shiftValueModel = DateShiftConfigure.createShiftValueModel();
    private final SettingsModelString m_shiftColumnModel = DateShiftConfigure.createNumColmodel();


    /**
     * New pane for configuring the TimeDifference node.
     */
    @SuppressWarnings("unchecked")
    protected DateShiftNodeDialog() {
        createNewGroup("Shift value:");
        addDialogComponent(new DialogComponentButtonGroup(m_shiftmodel, false, "", CFG_COLUMN_SHIFT, CFG_VALUE_SHIFT));

        addDialogComponent(new DialogComponentNumberEdit(m_shiftValueModel, "Shift value", 15));

        // en- and disable the selection of the time and the
        // second column, as selected by user.
        m_shiftmodel.addChangeListener(new ChangeListener() {
             @Override
            public void stateChanged(final ChangeEvent e) {
                 updateComponentVisibility(m_shiftmodel.getStringValue());
            }
        });

        // numerical column
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_shiftColumnModel, "Select shift column", 0, false, false, IntValue.class));
        List<String> gran = new LinkedList<String>();
        gran.add(Granularity.DAY.getName());
        gran.add(Granularity.HOUR.getName());
        gran.add(Granularity.MILLISECOND.getName());
        gran.add(Granularity.MINUTE.getName());
        gran.add(Granularity.MONTH.getName());
        gran.add(Granularity.SECOND.getName());
        gran.add(Granularity.WEEK.getName());
        gran.add(Granularity.YEAR.getName());

        // granularity selection
        addDialogComponent(new DialogComponentStringSelection(
                DateShiftConfigure.createGranularityModel(), "Select granularity of shift", gran));

        addDialogComponent(new DialogComponentBoolean(m_replaceColumn, "Replace column"));

        // new column name
        addDialogComponent(new DialogComponentString(m_appendedColName, "Appended column name:"));
        m_appendedColName.setEnabled(!m_replaceColumn.getBooleanValue());

        setHorizontalPlacement(true);
        closeCurrentGroup();
        createNewGroup("Date reference:");
        // en- and disable the selection of the time and the
        // second column, as selected by user.
        m_referencemodel.addChangeListener(new ChangeListener() {
             @Override
            public void stateChanged(final ChangeEvent e) {
                 updateComponentVisibility(m_referencemodel.getStringValue());
            }
        });
        addDialogComponent(new DialogComponentButtonGroup(m_referencemodel,
                false, "", CFG_NOW, CFG_COLUMN, CFG_FIXDATE));

        setHorizontalPlacement(false);

        //  date column
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_columnSelComponent, "Select a date column", 0, false, false, DateAndTimeValue.class));
        // time selection
        addDialogComponent(new DialogComponentCalendar(m_fixTimeComponent, "Fixed time "));

        closeCurrentGroup();
        createNewGroup("Output date options:");

        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(DateShiftConfigure.createHasDateModel(), "Use date"));
        addDialogComponent(new DialogComponentBoolean(DateShiftConfigure.createHasTimeModel(), "Use time"));
        addDialogComponent(new DialogComponentBoolean(
                                          DateShiftConfigure.createHasMiliSecondsModel(), "Use milliseconds"));
        closeCurrentGroup();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        try {
            m_referencemodel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_referencemodel.setStringValue(DateShiftConfigure.createReferenceTypeModel().getStringValue());
        }

        try {
            m_shiftmodel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_shiftmodel.setStringValue(DateShiftConfigure.createShiftTypeModel().getStringValue());
        }
        updateComponentVisibility(m_referencemodel.getStringValue());
        updateComponentVisibility(m_shiftmodel.getStringValue());
    }


    /**
     * @param string the identifier of the type of second time selection.
     */
    private void updateComponentVisibility(final String string) {
        if (string.equals(CFG_FIXDATE)) {
            m_fixTimeComponent.setEnabled(true);
            m_columnSelComponent.setEnabled(false);
            m_replaceColumn.setEnabled(false);
        } else if (string.equals(CFG_NOW)) {
            m_fixTimeComponent.setEnabled(false);
            m_columnSelComponent.setEnabled(false);
            m_replaceColumn.setEnabled(false);
        } else if (string.equals(CFG_COLUMN)) {  //default: use a second column
            m_fixTimeComponent.setEnabled(false);
            m_columnSelComponent.setEnabled(true);
            m_replaceColumn.setEnabled(true);
        } else if (string.equals(CFG_COLUMN_SHIFT)) {
            m_shiftValueModel.setEnabled(false);
            m_shiftColumnModel.setEnabled(true);
        } else if (string.equals(CFG_VALUE_SHIFT)) {
            m_shiftValueModel.setEnabled(true);
            m_shiftColumnModel.setEnabled(false);
        }
    }
}
