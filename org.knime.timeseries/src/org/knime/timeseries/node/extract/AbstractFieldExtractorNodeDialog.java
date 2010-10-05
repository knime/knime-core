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
 * 
 * History
 *   05.10.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class AbstractFieldExtractorNodeDialog 
    extends DefaultNodeSettingsPane {

    /** Warning message that no field is selected. */
    public static final String NOTHING_SELECTED_MESSAGE = 
        "No field selected. Output table will be same as input table!";
    
    // general helper methods
    
    /**
     * 
     * @param settings settings to read from
     * @param enabledModel the check box model in order to validate only active
     * column name models
     * @param colNameModel the column name model for which the value should be 
     * validated
     * @return true if the name is enabled and valid, falsei f the name is not 
     * enabled 
     * @throws InvalidSettingsException if the string value of the column model
     * is either <code>null</code> or empty 
     */
    public static boolean validateColumnName(final NodeSettingsRO settings,
            final SettingsModelBoolean enabledModel,
            final SettingsModelString colNameModel) 
    throws InvalidSettingsException {
        SettingsModelBoolean isEnabled = enabledModel
            .createCloneWithValidatedValue(settings);
        if (!isEnabled.getBooleanValue()) {
            return false;
        }
        SettingsModelString colNameClone = colNameModel
                .createCloneWithValidatedValue(settings);
        String colName = colNameClone.getStringValue();
        if (colName == null || colName.isEmpty()) {
            throw new InvalidSettingsException(
                    "A column name must not be empty!");
        }
        return true;
    }    
    
    /** Constant to return the month as a string. */ 
    public static final String AS_STRING = "Text";
    /** Constant to return the month as an int. */
    public static final String AS_INT = "Number";
    
    /**
     * @param timeField name of the time field the representation is for
     * @return settings model for the representation model (either as text or 
     * as number) for a specific time field (month, day of week)
     */
    public static SettingsModelString createRepresentationModelFor(
            final String timeField) {
        return new SettingsModelString(timeField + ".representation", 
                AS_STRING);
    }
    
    /**
     * 
     * @param timeFieldName name of the time field (one of the static fields)
     * @return the settings model for the checkbox whether the time field 
     * should be extracted
     */
    public static SettingsModelBoolean createUseTimeFieldModel(
            final String timeFieldName) {
        return new SettingsModelBoolean("include." + timeFieldName, true);
    }

    /**
     * 
     * @param timeFieldName name of the time field (one of the static fields)
     * @return the settings model for the text field for the new column name
     */
    public static SettingsModelString createTimeFieldColumnNameModel(
            final String timeFieldName) {
        return new SettingsModelString(timeFieldName + ".column.name", 
                timeFieldName);
    }
    
    /**
     * 
     * @return settings model for the timestamp column selection
     */
    public static SettingsModelString createColumnSelectionModel() {
        return new SettingsModelString("selected.timestamp.column", "");
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
    public static void addListener(final SettingsModelBoolean checkBoxModel,
            final SettingsModelString columnNameModel) {
        checkBoxModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                columnNameModel.setEnabled(checkBoxModel.getBooleanValue());
            }
        });
    }
    
    
    /**
     * Creates the necessary {@link SettingsModel}s, adds the listener to the
     * checkbox and creates the UI component with a horizontally oriented group 
     * containing the checkbox and text field for the new column name. 
     * Then closes the group. 
     * 
     * @param timeField name of the time field for which a checkbox, 
     * a text field and a format selection (int or string) should be created
     * 
     */
    protected void createUIComponentWithFormatSelection(
            final String timeField) {
        // create the settings models and add listener
        SettingsModelBoolean checkBoxModel = createUseTimeFieldModel(timeField);
        SettingsModelString colNameModel = createTimeFieldColumnNameModel(
                timeField);
        SettingsModelString formatModel = createRepresentationModelFor(
                timeField);
        addListener(checkBoxModel, colNameModel);
        addListener(checkBoxModel, formatModel);
        createNewGroup("");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(checkBoxModel, 
                timeField));
        // add radio buttons for string or int representation
        addDialogComponent(new DialogComponentButtonGroup(
                formatModel, true, "Value as", AS_STRING, AS_INT));
        addDialogComponent(new DialogComponentString(colNameModel, 
                "Column name:", true, 20));
        closeCurrentGroup();
        setHorizontalPlacement(false);        
    }
    
    
    /**
     * Creates the necessary {@link SettingsModel}s, adds the listener to the
     * checkbox and creates the UI component with a horizontally oriented group 
     * containing the checkbox and text field. Then closes the group.
     * @param timeField name of the time field for which the ui component 
     * should be created
     */
    protected void createUIComponentFor(final String timeField) {
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
