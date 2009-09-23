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
import org.knime.core.data.StringValue;
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

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    public String2DateDialog() {
        initializeModels();
        // column selection combo box
        DialogComponentColumnNameSelection colSelection
            = new DialogComponentColumnNameSelection(m_colSelectionModel,
                    "Select column: ", 0, StringValue.class);
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
        
        // configure how often the execute method may fail until 
        // cancels execution
        createNewGroup("Abort execution");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(m_cancelOnFailModel, 
                "Abort execution..."));
        addDialogComponent(new DialogComponentNumber(m_failNoModel, 
                "...after this number of unresolved rows", 10));
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
        // add listener to column selection
        m_colSelectionModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                // -> set text selected_col name + _time
                m_colNameModel.setStringValue(
                        m_colSelectionModel.getStringValue() + "_time");
            }
        });
        
        m_cancelOnFailModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                // if fail on cancel -> define max number of fails
                m_failNoModel.setEnabled(m_cancelOnFailModel.getBooleanValue());
            }
        });

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
        // initialize with default selected column name... 
        m_colNameModel.setStringValue(
                m_colSelectionModel.getStringValue() + "_time");
        
    }

    static SettingsModelBoolean createReplaceModel() {
        return new SettingsModelBoolean("replace-time-column", false);
    }

    static SettingsModelString createColumnSelectionModel() {
        return new SettingsModelString("selected-column", "");
    }
    
    static SettingsModelString createColumnNameModel() {
        return new SettingsModelString("new-column-name", "");
    }
    
    static SettingsModelString createFormatModel() {
        return new SettingsModelString("date-format", "yyyy-MM-dd;HH:mm:ss.S");
    }
    
    static SettingsModelInteger createFailNumberModel() {
        return new SettingsModelInteger("max-fail-number", 100);
    }
    
    static SettingsModelBoolean createCancelOnFailModel() {
        return new SettingsModelBoolean("cancel-on-fail", true);
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
