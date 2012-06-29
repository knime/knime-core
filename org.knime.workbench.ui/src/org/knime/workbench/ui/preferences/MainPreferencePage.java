/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 *
 * @author Florian Georg, University of Konstanz
 */
public class MainPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    private RadioGroupFieldEditor m_consoleLogEditor;

    private StringFieldEditor m_nodeLabelPrefix;

    private BooleanFieldEditor m_emptyNodeLabel;

    /**
     * Constructor.
     */
    public MainPreferencePage() {
        super(GRID);
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        // Specify the minimum log level for the console
        m_consoleLogEditor = new RadioGroupFieldEditor(
                KNIMECorePlugin.P_LOGLEVEL_CONSOLE,
                "Console View Log Level", 4,
                new String[][] {
                        {"&DEBUG", LEVEL.DEBUG.name()},
                        {"&INFO",  LEVEL.INFO.name()},
                        {"&WARN",  LEVEL.WARN.name()},
                        {"&ERROR", LEVEL.ERROR.name()}
                }, parent);
        addField(m_consoleLogEditor);

        addField(new HorizontalLineField(parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RESET,
                "Confirm Node Reset", parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_DELETE,
                "Confirm Node/Connection Deletion", parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RECONNECT,
                "Confirm reconnection of already connected nodes", parent));
        addField(new BooleanFieldEditor(
                PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED,
                "Confirm if executing nodes are not saved", parent));
        addField(new BooleanFieldEditor(
                PreferenceConstants.P_CONFIRM_EXEC_NODES_DATA_AWARE_DIALOGS,
                "Confirm execution of upstream nodes when data is "
                + "needed for configuration", parent));

        addField(new HorizontalLineField(parent));
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

        addField(new HorizontalLineField(parent));
        m_emptyNodeLabel = new BooleanFieldEditor(
                PreferenceConstants.P_SET_NODE_LABEL,
                "Set node label prefix", parent) {
            /** {@inheritDoc}  */
            @Override
            protected void valueChanged(final boolean old, final boolean neu) {
                m_nodeLabelPrefix.setEnabled(neu, parent);
            }
        };
        m_nodeLabelPrefix = new StringFieldEditor(
                PreferenceConstants.P_DEFAULT_NODE_LABEL,
                "Default node label (prefix): ", parent);
        addField(m_emptyNodeLabel);
        addField(m_nodeLabelPrefix);
        IntegerFieldEditor fontSizeEditor = new IntegerFieldEditor(
                PreferenceConstants.P_NODE_LABEL_FONT_SIZE,
                "Change node name and label font size", parent);
        addField(fontSizeEditor);

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, "These grid preferences apply to new workflows only."));
        addField(new BooleanFieldEditor(PreferenceConstants.P_GRID_SHOW,
                "Show grid", parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_GRID_SNAP_TO,
                "Snap to grid", parent));
        IntegerFieldEditor gridSizeXEditor = new IntegerFieldEditor(
                PreferenceConstants.P_GRID_SIZE_X, "Horiz. grid size (in px)", parent);
        gridSizeXEditor.setValidRange(3, 500);
        gridSizeXEditor.setTextLimit(3);
        gridSizeXEditor.load();
        addField(gridSizeXEditor);
        IntegerFieldEditor gridSizeYEditor = new IntegerFieldEditor(
                PreferenceConstants.P_GRID_SIZE_Y, "Vertic. grid size (in px)", parent);
        gridSizeYEditor.setValidRange(3, 500);
        gridSizeYEditor.setTextLimit(3);
        gridSizeYEditor.load();
        addField(gridSizeYEditor);
        addField(new LabelField(parent, "To change the grid settings of a workflow, use the 'Editor Grid Settings' "
                + "toolbar button."));
        addField(new HorizontalLineField(parent));
        ComboFieldEditor updateMetaNodeLinkOnLoadEditor = new ComboFieldEditor(
                PreferenceConstants.P_META_NODE_LINK_UPDATE_ON_LOAD,
                "Update meta node links when workflow loads",
                new String[][] {
                        {"Always", MessageDialogWithToggle.ALWAYS},
                        {"Never", MessageDialogWithToggle.NEVER},
                        {"Prompt", MessageDialogWithToggle.PROMPT},
                }, getFieldEditorParent());
        addField(updateMetaNodeLinkOnLoadEditor);

    }

    /** {@inheritDoc} */
    @Override
    public void init(final IWorkbench workbench) {
        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
    }

    /** {@inheritDoc} */
    @Override
    protected void initialize() {
        super.initialize();
        m_consoleLogEditor.setPreferenceStore(
                KNIMECorePlugin.getDefault().getPreferenceStore());
        m_consoleLogEditor.load();
        m_nodeLabelPrefix.setEnabled(m_emptyNodeLabel.getBooleanValue(),
                getFieldEditorParent());
    }

    /** {@inheritDoc} */
    @Override
    protected void performDefaults() {
        super.performDefaults();
        m_consoleLogEditor.setPreferenceStore(
                KNIMECorePlugin.getDefault().getPreferenceStore());
        m_consoleLogEditor.loadDefault();
    }
}
