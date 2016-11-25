/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.05.2016 (koetter): created
 */
package org.knime.workbench.ui.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.knime.core.node.port.database.DatabaseDriverLoader;

/**
 * {@link ListEditor} implementation that adds an add file button in addition to the add directory button.
 * @author Tobias Koetter, KNIME.com
 */
public class DatabaseDriverListEditor extends ListEditor {

    /**Character sequence that separates two JDBC file/directory entries in the preference value.
     * Don't change it since it is also used in the
     * org.knime.workbench.core/src/org/knime/workbench/core/KNIMECorePlugin.java class in the initDatabaseDriver()
     * method.*/
    private static final String VALUE_SPLIT = ";";

    private FieldEditorPreferencePage m_page;

    /**
     * The last path, or <code>null</code> if none.
     */
    private String lastPath;

    DatabaseDriverListEditor(final String name, final String labelText, final Composite parent,
        final FieldEditorPreferencePage page) {
        super(name, labelText, parent);
        m_page = page;
    }

    /**
     * The subclasses must override this to return the modified entry.
     *
     * @param original the new entry
     * @return the modified entry. Return null to prevent modification.
     */
    protected String getModifiedEntry(final String original) {
        return original;
    }

    private Button m_fileButton;
    private List commandListControl;

    @Override
    public Composite getButtonBoxControl(final Composite parent) {
        Composite buttonBox = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        buttonBox.setLayout(layout);
        if (m_fileButton == null) {
            m_fileButton = createPushButton(buttonBox, "Add file");
            m_fileButton.setEnabled(true);
            m_fileButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    final String dir = getNewJarFile();
                    if (dir != null) {
                        commandListControl.add(dir);
                    }
                }
            });
            Composite buttonBoxControl = super.getButtonBoxControl(buttonBox    );
            buttonBoxControl.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent event) {
                    m_fileButton = null;
                }
            });
        }
        getAddButton().setText("Add directory");
        return buttonBox;
    }
    /*
     * @see FieldEditor.setEnabled(boolean,Composite).
     */
    @Override
    public void setEnabled(final boolean enabled, final Composite parent) {
        super.setEnabled(enabled, parent);
        m_fileButton.setEnabled(enabled);
    }

    /**
     * @return the {@link String} of the selected jar or <code>null</code>
     */
    protected String getNewJarFile() {
        FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell());
        dialog.setFilterExtensions(new String[]{"*.jar", "*.zip"});
        String fileName = dialog.open();
        if (fileName == null) {
            return null;
        }
        try {
            DatabaseDriverLoader.loadDriver(new File(fileName));
            return fileName;
        } catch (IOException ioe) {
            m_page.setErrorMessage(ioe.getMessage());
            return null;
        }
    }

    @Override
    protected String getNewInputObject() {
        final DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SHEET);
        dialog.setMessage("Select directory with JDBC driver files");
        if (lastPath != null) {
            if (new File(lastPath).exists()) {
                dialog.setFilterPath(lastPath);
            }
        }
        String dir = dialog.open();
        if (dir != null) {
            dir = dir.trim();
            if (dir.length() == 0) {
                return null;
            }
            lastPath = dir;
        }
        try {
            DatabaseDriverLoader.loadDriver(new File(dir));
            return dir;
        } catch (IOException ioe) {
            m_page.setErrorMessage(ioe.getMessage());
            return null;
        }
    }

    /**
     * Helper method to create a push button.
     *
     * @param parent the parent control
     * @param key the resource name used to supply the button's label text
     * @return Button
     */
    private Button createPushButton(final Composite parent, final String key) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(key);
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        int widthHint = convertHorizontalDLUsToPixels(button,
                IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT,
                SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        return button;
    }

    @Override
    public List getListControl(final Composite parent) {
        final List listControl = super.getListControl(parent);
        if (commandListControl == null) {
            commandListControl = listControl;
        }
        return listControl;
    }


    @Override
    protected String createList(final String[] string) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < string.length; i++) {
            if (i > 0) {
                res.append(VALUE_SPLIT);
            }
            res.append(string[i]);
        }
        return res.toString();
    }

    @Override
    protected String[] parseString(final String string) {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> failed = new ArrayList<>();
        String[] strings = string.split(VALUE_SPLIT);
        for (String str : strings) {
            try {
                if (str != null && !str.trim().isEmpty()) {
                    DatabaseDriverLoader.loadDriver(new File(str));
                    result.add(str);
                }
            } catch (IOException ioe) {
                failed.add(str);
            }
        }
        if (!failed.isEmpty()) {
            m_page.setErrorMessage("Some driver file(s) are not available anymore: " + failed.toString());
        }
        return result.toArray(new String[0]);
    }

//    //TODO: The used ; as split character isn't a good idea since a file name might contain a;
        //We might want to use the following which is copied from org.eclipse.jface.preference.PathEditor
    //However the split character is also used in the
    //org.knime.workbench.core/src/org/knime/workbench/core/KNIMECorePlugin.java class in the initDatabaseDriver()
    //method
//    protected String createList(String[] items) {
//        StringBuffer path = new StringBuffer("");//$NON-NLS-1$
//
//        for (int i = 0; i < items.length; i++) {
//            path.append(items[i]);
//            path.append(File.pathSeparator);
//        }
//        return path.toString();
//    }
//
//    @Override
//    protected String[] parseString(String stringList) {
//        StringTokenizer st = new StringTokenizer(stringList, File.pathSeparator
//                + "\n\r");//$NON-NLS-1$
//        ArrayList<Object> v = new ArrayList<Object>();
//        while (st.hasMoreElements()) {
//            v.add(st.nextElement());
//        }
//        return v.toArray(new String[v.size()]);
//    }
}
