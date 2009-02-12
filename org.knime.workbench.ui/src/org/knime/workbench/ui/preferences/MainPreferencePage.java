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
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage </samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class MainPreferencePage extends FieldEditorPreferencePage implements
IWorkbenchPreferencePage {

    private RadioGroupFieldEditor m_consoleLogEditor;

    /**
     * Constructor .
     */
    public MainPreferencePage() {
        super(GRID);
//      setDescription("KNIME GUI preferences");
    }


    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        // Specify the minimum log level for the console
        m_consoleLogEditor = new RadioGroupFieldEditor(
                KNIMECorePlugin.P_LOGLEVEL_CONSOLE, 
                "Console View Log Level", 4,
                new String[][] {
                        {"&DEBUG", LEVEL.DEBUG.name()},

                        {"&INFO", LEVEL.INFO.name()},

                        {"&WARN", LEVEL.WARN.name()},

                        {"&ERROR", LEVEL.ERROR.name()} },
                        parent);
        addField(m_consoleLogEditor);

        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RESET, 
                "Confirm Node Reset", parent));

        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_DELETE, 
                "Confirm Node/Connection Deletion", parent));

        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RECONNECT,
                "Confirm reconnection of already connected nodes", parent));


        IntegerFieldEditor freqHistorySizeEditor = new IntegerFieldEditor(
                PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE,
                "Maximal size for most frequently used nodes", parent, 3);
        freqHistorySizeEditor.setValidRange(1, 50);
        freqHistorySizeEditor.setTextLimit(3);
        freqHistorySizeEditor.load();

        IntegerFieldEditor usedHistorySizeEditor = new IntegerFieldEditor(
                PreferenceConstants.P_FAV_LAST_USED_SIZE,
                "Maximal size for last used nodes", parent, 3);
        usedHistorySizeEditor.setValidRange(1, 50);
        usedHistorySizeEditor.setTextLimit(3);
        usedHistorySizeEditor.load();

        addField(usedHistorySizeEditor);
        addField(freqHistorySizeEditor);

    }

    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench) {
        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        super.initialize();
        m_consoleLogEditor.setPreferenceStore(KNIMECorePlugin.getDefault()
                .getPreferenceStore());
        m_consoleLogEditor.load();
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void performDefaults() {
        super.performDefaults();
        m_consoleLogEditor.setPreferenceStore(KNIMECorePlugin.getDefault()
                .getPreferenceStore());
        m_consoleLogEditor.loadDefault();
    }
}
