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
 * ------------------------------------------------------------------------
 */
package org.knime.timeseries.node.diff;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.util.DialogComponentCalendar;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * 
 * Dialog for the TimeDifference node with a column selection for the first date
 * column, one for the other date column, a text field for the new column, a
 * selection list for the desired granularity (year, quarter, month, week, day,
 * hour, minute) of the difference and a spinner to choose the rounding of the
 * fraction digits of the result.
 * 
 * 
 * @author KNIME GmbH
 * @author Iris Adä, University Konstanz
 */
public class TimeDifferenceNodeDialog extends DefaultNodeSettingsPane {

    private static final String CFG_COL1 = "column.lower";

    private static final String CFG_COL2 = "column.upper";

    private static final String CFG_NEW_COL_NAME = "new.column.name";

    private static final String CFG_GRANULARITY = "granularity";

    private static final String CFG_ROUND = "round.numbers";

    private static final String CFG_REF_TYPE = "seconddate.reference.type";

    private static final String CFG_TIME = "cfg.time";

    /** the key for always using the execution time as the second time. */
    public static final String CFG_NOW = "Use execution time";
    /** the key for using a column as the second time. */
    public static final String CFG_COLUMN = "Use second column";
    /** the key for using a specific time as the second time. */
    public static final String CFG_FIXDATE = "Use fixed date";
    /** the key for generating the difference, between the previous and
     * the current row. */
    public static final String CFG_ROW_DIFF = "Use previous row";
    
    private final SettingsModelString m_referencemodel 
                                = getReferenceTypeModel();
    private final SettingsModelCalendar m_fixTimeComponent 
                                = getCalendarModel();
    private final SettingsModelString m_columnSelComponent
                                = createColumn2Model();


    /**
     * New pane for configuring the TimeDifference node.
     */
    @SuppressWarnings("unchecked")
    protected TimeDifferenceNodeDialog() {
        setHorizontalPlacement(true);
        
        // en- and disable the selection of the time and the
        // second column, as selected by user.
        m_referencemodel.addChangeListener(new ChangeListener() {

             @Override
            public void stateChanged(final ChangeEvent e) {
                 updateComponentVisibility(
                         m_referencemodel.getStringValue());
                
            }
            
        });
        addDialogComponent(new DialogComponentButtonGroup(m_referencemodel,
                false, "", CFG_NOW, CFG_COLUMN, CFG_FIXDATE, CFG_ROW_DIFF));

        setHorizontalPlacement(false);
       
        // first date column
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColmn1Model(), "Select first date column", 0,
                DateAndTimeValue.class));
        // second date column
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_columnSelComponent
                , "Select second date column", 0,
                org.knime.core.data.date.DateAndTimeValue.class));
        // granularity selection
        addDialogComponent(new DialogComponentStringSelection(
                createGranularityModel(),
                "Select granularity of time difference", Granularity
                        .getDefaultGranularityNames()));
        // fraction digits for rounding
        addDialogComponent(new DialogComponentNumber(createRoundingModel(),
                "Rounding to .. digits", 1));
        // new column name
        addDialogComponent(new DialogComponentString(createNewColNameModel(),
                "Appended column name:"));
        // time selection
        addDialogComponent(new DialogComponentCalendar(m_fixTimeComponent,
                "Fixed time "));
    }
    

    /*
     * Models...
     */
    
    /**
     * @return settings model for the selected time
     */
    static SettingsModelCalendar getCalendarModel() {
        return new SettingsModelCalendar(CFG_TIME, null);
    }

    /**
     * @return settings model for the type of the used reference column.
     */
    static SettingsModelString getReferenceTypeModel() {
        return new SettingsModelString(CFG_REF_TYPE, CFG_NOW);
    }

    /**
     * @return settings model for the first time column
     */
    static SettingsModelString createColmn1Model() {
        return new SettingsModelString(CFG_COL1, "");
    }

    /**
     * 
     * @return settings model for the second time column
     */
    static SettingsModelString createColumn2Model() {
        return new SettingsModelString(CFG_COL2, "");
    }

    /**
     * 
     * @return settings model for the new column name
     */
    static SettingsModelString createNewColNameModel() {
        return new SettingsModelString(CFG_NEW_COL_NAME, "time diff");
    }

    /**
     * 
     * @return settings model for the granularity
     */
    static SettingsModelString createGranularityModel() {
        return new SettingsModelString(CFG_GRANULARITY, 
                Granularity.DAY.getName());
    }

    /**
     * 
     * @return settings model for the rounding model
     */
    static SettingsModelInteger createRoundingModel() {
        return new SettingsModelInteger(CFG_ROUND, 0);
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
            m_referencemodel.setStringValue(
                    getReferenceTypeModel().getStringValue());
        }
        updateComponentVisibility(m_referencemodel.getStringValue());
    }


    /**
     * @param string the identifier of the type of second time selection.
     */
    private void updateComponentVisibility(final String string) {
        if (string.equals(CFG_FIXDATE)) {
            m_fixTimeComponent.setEnabled(true);    
            m_columnSelComponent.setEnabled(false);
        } else if (string.equals(CFG_NOW)
                || string.equals(CFG_ROW_DIFF)) {
            m_fixTimeComponent.setEnabled(false);
            m_columnSelComponent.setEnabled(false);
        } else {  //default: use a second column
            m_fixTimeComponent.setEnabled(false);
            m_columnSelComponent.setEnabled(true);
        }
        
    }
}
