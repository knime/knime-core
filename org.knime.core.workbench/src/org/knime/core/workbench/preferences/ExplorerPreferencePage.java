/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.core.workbench.preferences;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.view.preferences.MountPointTableEditor;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.framework.FrameworkUtil;

public class ExplorerPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage, IPreferenceChangeListener {
    /** The id of this preference page. */
    public static final String ID
            = "org.knime.workbench.explorer.view.explorer";
    private MountPointTableEditor m_mountEditor;
    private ComboFieldEditor m_linkTemplateEditor;
    private BooleanFieldEditor m_showEJBWarningEditor;

    private BooleanFieldEditor m_showOlderServerWarningEditor;

    /**
    *
    */
    public ExplorerPreferencePage() {
        super("KNIME Explorer Settings", null, GRID);
        setDescription("Set up mount points for usage in KNIME Explorer view.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(ExplorerActivator.getDefault().getPreferenceStore());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        m_mountEditor = new MountPointTableEditor(getFieldEditorParent());
        addField(m_mountEditor);

        m_linkTemplateEditor = new ComboFieldEditor(
                PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE,
                "Link components when sharing on Server or Local Workspace",
                new String[][] {
                        {"Never", MessageDialogWithToggle.NEVER},
                        {"Prompt", MessageDialogWithToggle.PROMPT},
                }, getFieldEditorParent());
        addField(m_linkTemplateEditor);

        m_showEJBWarningEditor = new BooleanFieldEditor(PreferenceConstants.P_SHOW_EJB_WARNING_DIALOG,
            "Show a warning dialog when connecting to a server via EJB", getFieldEditorParent());
        addField(m_showEJBWarningEditor);

        m_showOlderServerWarningEditor =
            new BooleanFieldEditor(PreferenceConstants.P_SHOW_OLDER_SERVER_WARNING_DIALOG,
                "Show a warning dialog when connecting to an older server", getFieldEditorParent());
        addField(m_showOlderServerWarningEditor);

        DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(ExplorerActivator.class).getSymbolicName())
            .addPreferenceChangeListener(this);
    }

    /**
     * {@inheritDoc}
     * @since 6.3
     */
    @Override
    public void preferenceChange(final PreferenceChangeEvent event) {
        // The default preferences may change while this page is open (e.g. by an action on other preference page, e.g.
        // Novartis). If this editor is showing only default preference it will not upated its contents with the new
        // default preference and then store the old default preference as user settings once the preference dialog is
        // closed with OK. This listener updates the components when the default preferences change.

        if (PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML.equals(event.getKey())) {
            m_mountEditor.load();
        } else if (PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE.equals(event.getKey())) {
            m_linkTemplateEditor.load();
        } else if (PreferenceConstants.P_SHOW_EJB_WARNING_DIALOG.equals(event.getKey())) {
            m_showEJBWarningEditor.load();
        } else if(PreferenceConstants.P_SHOW_OLDER_SERVER_WARNING_DIALOG.equals(event.getKey())) {
            m_showOlderServerWarningEditor.load();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();
        DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(ExplorerActivator.class).getSymbolicName())
            .removePreferenceChangeListener(this);
    }
}
