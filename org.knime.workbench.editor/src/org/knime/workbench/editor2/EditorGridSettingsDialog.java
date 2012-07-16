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
 * History
 *   28.06.2012 (Peter Ohl): created
 */
package org.knime.workbench.editor2;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.workflow.EditorUIInformation;
import org.knime.workbench.KNIMEEditorPlugin;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public class EditorGridSettingsDialog extends Dialog {

    private static final ImageDescriptor IMG_PERMS = AbstractUIPlugin.imageDescriptorFromPlugin(
            KNIMEEditorPlugin.PLUGIN_ID, "icons/grid2_55.png");

    private EditorUIInformation m_settings;

    private Label m_error;

    private Text m_xGrid;

    private Text m_yGrid;

    private Button m_snap;

    private Button m_show;

    private boolean m_enableComponents = false;

    /**
     * Dialog to change the current editor grid settings.
     *
     * @param parentShell
     * @param currentSettings
     */
    public EditorGridSettingsDialog(final Shell parentShell, final EditorUIInformation currentSettings) {
        super(parentShell);
        m_settings = currentSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NONE);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        overall.setLayoutData(fillBoth);
        overall.setLayout(new GridLayout(1, true));

        createHeader(overall);
        createSettingsPanel(overall);
        setValues();
        return overall;
    }

    private void setValues() {
        m_snap.setSelection(m_settings.getSnapToGrid());
        m_show.setSelection(m_settings.getShowGrid());
        m_xGrid.setText("" + m_settings.getGridX());
        m_yGrid.setText("" + m_settings.getGridY());
        settingsChanged();
    }

    /**
     * @return the settings currently (or lastly) entered in the dialog
     */
    public EditorUIInformation getSettings() {
        return m_settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Workflow Editor Grid Settings");
    }

    private void createHeader(final Composite parent) {
        Composite header = new Composite(parent, SWT.FILL);
        Color white = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
        header.setBackground(white);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        header.setLayoutData(gridData);
        header.setLayout(new GridLayout(2, false));
        // 1st row
        Label exec = new Label(header, SWT.NONE);
        exec.setBackground(white);
        exec.setText("Workflow Editor Grid Settings");
        FontData[] fd = parent.getFont().getFontData();
        for (FontData f : fd) {
            f.setStyle(SWT.BOLD);
            f.setHeight(f.getHeight() + 2);
        }
        exec.setFont(new Font(parent.getDisplay(), fd));
        exec.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Label execIcon = new Label(header, SWT.NONE);
        execIcon.setBackground(white);
        execIcon.setImage(IMG_PERMS.createImage());
        execIcon.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, true));
        // 2nd row
        Label txt = new Label(header, SWT.NONE);
        txt.setBackground(white);
        txt.setText("Modify the settings for the grid in the active workflow editor \n"
                + "(To change default settings for new workflow editors go to the preference page.\n"
                + "Snap to grid behavior can be toggled by pressing 'Ctrl-Shift-X')");
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        new Label(header, SWT.NONE);
        // 3rd row
        m_error = new Label(header, SWT.NONE);
        m_error.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
        m_error.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
        m_error.setBackground(white);
        new Label(header, SWT.NONE);
    }

    private void createSettingsPanel(final Composite parent) {
        createEnablePanel(parent);
        createGridPanel(parent);
    }

    private void createEnablePanel(final Composite parent) {
        Group border = new Group(parent, SWT.SHADOW_IN);
        border.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        border.setLayout(new GridLayout(1, true));
        border.setText("Enable Grid:");
        m_snap = new Button(border, SWT.CHECK);
        m_snap.setText("Snap to grid (Alt-key disables snapping temporarily while moving nodes)");
        m_snap.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        m_snap.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event arg0) {
                settingsChanged();
            }
        });
        m_show = new Button(border, SWT.CHECK);
        m_show.setText("Show grid lines");
        m_show.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        m_show.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event arg0) {
                settingsChanged();
            }
        });
    }

    private void createGridPanel(final Composite parent) {
        Group border = new Group(parent, SWT.SHADOW_IN);
        border.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        border.setLayout(new GridLayout(2, true));
        border.setText("Grid Size:");

        Composite horiz = new Composite(border, SWT.BORDER_SOLID);
        horiz.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        horiz.setLayout(new GridLayout(2, false));
        Label h = new Label(horiz, SWT.NONE);
        h.setText("horizontal spacing (px):");
        h.setToolTipText("Preference page default value is " + WorkflowEditor.getPrefGridXSize());
        h.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        m_xGrid = new Text(horiz, SWT.SINGLE | SWT.BORDER);
        m_xGrid.setTextLimit(10);
        m_xGrid.setToolTipText("Preference page default value is " + WorkflowEditor.getPrefGridXSize());
        m_xGrid.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        m_xGrid.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                settingsChanged();
            }
        });

        Composite vert = new Composite(border, SWT.NONE);
        vert.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        vert.setLayout(new GridLayout(2, true));
        Label v = new Label(vert, SWT.NONE);
        v.setText("vertical spacing (px):");
        v.setToolTipText("Preference page default value is " + WorkflowEditor.getPrefGridYSize());
        v.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        m_yGrid = new Text(vert, SWT.SINGLE | SWT.BORDER);
        m_yGrid.setTextLimit(10);
        m_yGrid.setToolTipText("Preference page default value is " + WorkflowEditor.getPrefGridYSize());
        m_yGrid.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        m_yGrid.getFont();
        m_yGrid.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                settingsChanged();
            }
        });

    }

    private void settingsChanged() {

        m_settings.setSnapToGrid(m_snap.getSelection());
        m_settings.setShowGrid(m_show.getSelection());

        String x = m_xGrid.getText().trim();
        if (x.isEmpty()) {
            setError("Please enter a number for the horizontal X grid spacing.");
            return;
        }
        try {
            int xInt = Integer.parseInt(x);
            if (xInt <= 0) {
                setError("Please enter a value larger than zero for the horizontal X grid spacing.");
                m_xGrid.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                return;
            }
            m_settings.setGridX(xInt);
            m_xGrid.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
        } catch (NumberFormatException nfe) {
            setError("Invalid number for horizontal X grid spacing.");
            m_xGrid.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
            return;
        }

        String y = m_yGrid.getText().trim();
        if (y.isEmpty()) {
            setError("Please enter a number for the vertical Y grid spacing.");
            return;
        }
        try {
            int yInt = Integer.parseInt(y);
            if (yInt <= 0) {
                setError("Please enter a value larger than zero for the vertical Y grid spacing.");
                m_yGrid.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                return;
            }
            m_settings.setGridY(yInt);
            m_yGrid.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
        } catch (NumberFormatException nfe) {
            setError("Invalid number for vertical Y grid spacing.");
            m_yGrid.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
            return;
        }

        setError("");
    }

    private void setError(final String err) {
        m_enableComponents = err.isEmpty();
        m_error.setText(err);
        m_error.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        m_error.getParent().layout();
        Button ok = getButton(IDialogConstants.OK_ID);
        if (ok != null) {
            ok.setEnabled(m_enableComponents);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(m_enableComponents);
    }
}
