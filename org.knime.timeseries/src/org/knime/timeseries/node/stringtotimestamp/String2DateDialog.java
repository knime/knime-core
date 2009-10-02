/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   January 13, 2007 (rosaria): created from String2Smileys
 */
package org.knime.timeseries.node.stringtotimestamp;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.StringHistory;

/**
 * This dialog lets the user choose the column that contains the string values
 * that should be converted into Smiles values.
 *
 * @author Rosaria Silipo
 * @author Fabian Dill, KNIME.com GmbH
 */
public class String2DateDialog extends DefaultNodeSettingsPane {
    
    /**
     * Key for the string history to re-use user entered date formats. 
     */
    public static final String FORMAT_HISTORY_KEY = "string2date-formats"; 

    private final SettingsModelString m_colSelectionModel 
        = createColumnSelectionModel();
    private final SettingsModelString m_colNameModel = createColumnNameModel();
    private final SettingsModelBoolean m_replaceModel = createReplaceModel();
    private final SettingsModelString m_formatModel = createFormatModel();
    private final SettingsModelInteger m_failNoModel = createFailNumberModel();
    private final SettingsModelBoolean m_cancelOnFailModel 
        = createCancelOnFailModel();
    
    private final DialogComponentStringSelection m_formatSelectionUI;
    
    /**
     * Predefined date formats.
     */
    public static final Collection<String> PREDEFINED_FORMATS 
        = createPredefinedFormats();
    
    private final String m_suffix;
    
    /** Constructor adding three components.
     * 
     * @param filterClass the allowed type for the column selection (since the 
     * dialog is used by string2time and time2string)
     * @param newColNameSuffix suffix to be appended to the selected column name
     *  and then proposed as the new column name
     *  @param canFail if <code>true</code> a checkbox and a spinner is shown in
     *  order to cancel execution after a number of unsuccessful conversions
     */
    @SuppressWarnings("unchecked")
    public String2DateDialog(final Class<? extends DataValue> filterClass,
            final String newColNameSuffix, final boolean canFail) {
        m_suffix = newColNameSuffix;
        initializeModels();
        // column selection combo box
        DialogComponentColumnNameSelection colSelection
            = new DialogComponentColumnNameSelection(m_colSelectionModel,
                    "Select column: ", 0, filterClass);
        addDialogComponent(colSelection);
        // replace existing column?
        DialogComponentBoolean replaceBox = new DialogComponentBoolean(
                m_replaceModel, "Replace selected column");
        addDialogComponent(replaceBox);
        // if not what is the name of the new column
        // text edit field
        DialogComponentString newColName = new DialogComponentString(
                m_colNameModel, "New column name");

        addDialogComponent(newColName);
        
        // format combo box
        m_formatSelectionUI 
            = new DialogComponentStringSelection(m_formatModel, "Date format",
                    // TODO: later on move this to the node model which also 
                    // loads values from StringHistory
                    PREDEFINED_FORMATS, true);
        
        addDialogComponent(m_formatSelectionUI);
        
        if (canFail) {
            // configure how often the execute method may fail until 
            // cancels execution
            createNewGroup("Abort execution");
            setHorizontalPlacement(true);
            addDialogComponent(new DialogComponentBoolean(m_cancelOnFailModel, 
                    "Abort execution..."));
            addDialogComponent(new DialogComponentNumber(m_failNoModel, 
                    "...after this number of unresolved rows", 10));
        }
        addColSelectionListener(m_colSelectionModel, m_colNameModel, 
                newColNameSuffix);
    }
    
    private void initializeModels() {
        // add listener to replace column 
        m_replaceModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                // -> set new column name model enabled = !m_replace;
                m_colNameModel.setEnabled(!m_replaceModel.getBooleanValue());
            }
        });
        addColSelectionListener(m_colSelectionModel, m_colNameModel, m_suffix);
        if (m_cancelOnFailModel != null && m_failNoModel != null) {
            // if !canFail these models are null
            m_cancelOnFailModel.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    // if fail on cancel -> define max number of fails
                    m_failNoModel.setEnabled(
                            m_cancelOnFailModel.getBooleanValue());
                }
            });
        }
    }
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        // retrieve potential new values from the StringHistory and add them
        // (if not already present) to the combobox...
        m_formatSelectionUI.replaceListItems(createPredefinedFormats(), 
                m_formatModel.getStringValue());        
    }

    /**
     * 
     * @return the settings model whether to replace the string column that is 
     * parsed
     */
    public static SettingsModelBoolean createReplaceModel() {
        return new SettingsModelBoolean("replace-time-column", false);
    }

    /**
     * 
     * @return the settigns model of the selected string column that should be 
     * parsed
     */
    public static SettingsModelString createColumnSelectionModel() {
        return new SettingsModelString("selected-column", "");
    }
    
    /**
     * 
     * @return the settings model for the new column 
     *  (if replace column is not selected)
     */
    public static SettingsModelString createColumnNameModel() {
        return new SettingsModelString("new-column-name", "");
    }
    
    /**
     * 
     * @return the settings model for the date format
     */
    public static SettingsModelString createFormatModel() {
        return new SettingsModelString("date-format", "yyyy-MM-dd;HH:mm:ss.S");
    }
    
    /**
     * 
     * @return the settings model for the maximum number of rows that are 
     * allowed to fail before the execution is canceled   
     */
    public static SettingsModelInteger createFailNumberModel() {
        return new SettingsModelInteger("max-fail-number", 100);
    }
    
    /**
     * 
     * @return the settings model for the checkbox whether the node should fail
     * if a certain number of rows could not be parsed
     */
    public static SettingsModelBoolean createCancelOnFailModel() {
        return new SettingsModelBoolean("cancel-on-fail", true);
    }

    /**
     * Adds a listener to the column selection and then updates the proposed 
     * name for the new column by taking the name of the selected column and 
     * appending the suffix: "&lt;selected-column-name&gt;_suffix". 
     * 
     * @param colSelection the name of the selected column
     * @param newColName settings model holding the new column name
     * @param suffix a default suffix to be appended to the selected column name
     */
    public static final void addColSelectionListener(
            final SettingsModelString colSelection, 
            final SettingsModelString newColName, final String suffix) {
        // add listener to column selection
        colSelection.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                // -> set text selected_col name + _time
                newColName.setStringValue(colSelection.getStringValue() 
                        + "_" + suffix);
            }        });
    }
    
    private static Collection<String> createPredefinedFormats() {
        // unique values
        Set<String> formats = new LinkedHashSet<String>();
        formats.add("yyyy-MM-dd;HH:mm:ss.S");
        formats.add("dd.MM.yyyy;HH:mm:ss.S");
        formats.add("yyyy/dd/MM");
        formats.add("dd.MM.yyyy");
        formats.add("yyyy-MM-dd");
        formats.add("HH:mm:ss");
        // check also the StringHistory....
        String[] userFormats = StringHistory.getInstance(FORMAT_HISTORY_KEY)
            .getHistory();
        for (String userFormat : userFormats) {
            formats.add(userFormat);
        }
        return formats;
    }
}
