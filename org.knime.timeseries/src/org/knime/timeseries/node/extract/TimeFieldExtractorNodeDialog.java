/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   24.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog for the time extractor node that configures which of the time 
 * fields (year, month, day, hour, minute, second, millisecond) should be 
 * appended as an int column.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimeFieldExtractorNodeDialog extends DefaultNodeSettingsPane {

    /** Year name. */
    static final String YEAR = "Year";
    /** Quarter name.*/
    static final String QUARTER = "Quarter";
    /** Month name. */
    static final String MONTH = "Month";
    /** Day name. */
    static final String DAY = "Day";
    /** Hour name. */
    static final String HOUR = "Hour";
    /** Minute name. */
    static final String MINUTE = "Minute";
    /** Second name. */
    static final String SECOND = "Second";
    /** Millisecond. */
    static final String MILLISECOND = "Millisecond";
    
    /** Constant to return the month as a string. */ 
    static final String MONTH_AS_STRING = "Text";
    /** Constant to return the month as an int. */
    static final String MONTH_AS_INT = "Number";
    
    /**
     * 
     * @param timeFieldName name of the time field (one of the static fields)
     * @return the settings model for the checkbox whether the time field 
     * should be extracted
     */
    static SettingsModelBoolean createUseTimeFieldModel(
            final String timeFieldName) {
        return new SettingsModelBoolean("include." + timeFieldName, true);
    }

    /**
     * 
     * @param timeFieldName name of the time field (one of the static fields)
     * @return the settings model for the text field for the new column name
     */
    static SettingsModelString createTimeFieldColumnNameModel(
            final String timeFieldName) {
        return new SettingsModelString(timeFieldName + ".column.name", 
                timeFieldName);
    }
    
    /**
     * 
     * @return settings model for the timestamp column selection
     */
    static SettingsModelString createColumnSelectionModel() {
        return new SettingsModelString("selected.timestamp.column", "");
    }
    
    /**
     * 
     * @return settings model for the month representation (either as text or 
     * as number)
     */
    static SettingsModelString createMonthRepresentationModel() {
        return new SettingsModelString("month.representation", MONTH_AS_STRING);
    }

    /**
     * Adds a listener to the {@link SettingsModelBoolean} which represents the
     * checkbox to include the time field and enables the text field model for
     * the column name dependent on the checkbox.
     * 
     * @param checkBoxModel check box model to listen to
     * @param columnNameModel text field for the new column name that should be
     * enabled or disabled
     */
    static void addListener(final SettingsModelBoolean checkBoxModel,
            final SettingsModelString columnNameModel) {
        checkBoxModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                columnNameModel.setEnabled(checkBoxModel.getBooleanValue());
            }
        });
    }
    
    /**
     * 
     */
    public TimeFieldExtractorNodeDialog() {
        // create the UI components
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColumnSelectionModel(), 
                "Column to extract time fields from:", 0, 
                DateAndTimeValue.class));
        createUIComponentFor(YEAR);
        createUIComponentFor(QUARTER);
        // the month UI component looks differently because of 
        // the string or int radio buttons
        createMonthUIComponent();
        createUIComponentFor(DAY);
        createUIComponentFor(HOUR);
        createUIComponentFor(MINUTE);
        createUIComponentFor(SECOND);
        createUIComponentFor(MILLISECOND);
    }
    
    /**
     * Creates the necessary {@link SettingsModel}s, adds the listener to the
     * checkbox and creates the UI component with a horizontally oriented group 
     * containing the checkbox and text field for the new column name. 
     * Then closes the group. For the month 
     */
    private void createMonthUIComponent() {
        // create the settings models and add listener
        SettingsModelBoolean checkBoxModel = createUseTimeFieldModel(MONTH);
        SettingsModelString colNameModel = createTimeFieldColumnNameModel(
                MONTH);
        addListener(checkBoxModel, colNameModel);
        createNewGroup("");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(checkBoxModel, 
                MONTH));
        // add radio buttons for string or int representation
        addDialogComponent(new DialogComponentButtonGroup(
                createMonthRepresentationModel(), true, "Month as",
                MONTH_AS_STRING, MONTH_AS_INT));
        addDialogComponent(new DialogComponentString(colNameModel, 
                "Column name:", true, 20));
        closeCurrentGroup();
        setHorizontalPlacement(false);        
    }
    
    /**
     * Creates the necessary {@link SettingsModel}s, adds the listener to the
     * checkbox and creates the UI component with a horizontally oriented group 
     * containing the checkbox and text field. Then closes the group.
     */
    private void createUIComponentFor(final String timeField) {
        // create the settings models and add listener
        SettingsModelBoolean checkBoxModel = createUseTimeFieldModel(timeField);
        SettingsModelString colNameModel = createTimeFieldColumnNameModel(
                timeField);
        addListener(checkBoxModel, colNameModel);
        createNewGroup("");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(checkBoxModel, 
                timeField));
        addDialogComponent(new DialogComponentString(colNameModel, 
                "Column name:", true, 20));
        closeCurrentGroup();
        setHorizontalPlacement(false);
    }
    
}
