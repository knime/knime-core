/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   14.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;


import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to enter the port name and type.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class MetaPortDialog extends Dialog {

    private Shell m_shell;
    private Label m_error;
    private Combo m_type;

    private static final int WIDTH = 220;
    private static final int HEIGHT = 150;

    private Port m_port = null;


    /**
     *
     * @param parent the parent
     */
    public MetaPortDialog(final Shell parent) {
        super(parent);
        setText("Add Meta Port");
    }

    /**
     * Opens the dialog and returns the entered port (with name and type).
     *
     * @return the entered port (with name and type)
     */
    public Port open() {
        Shell parent = getParent();
        m_shell = new Shell(parent, SWT.DIALOG_TRIM
                | SWT.APPLICATION_MODAL);
        Point location = parent.getLocation();
        Point size = parent.getSize();
        m_shell.setLocation(location.x + (size.x / 2) - (WIDTH / 2),
                location.y + (size.y / 2) - (HEIGHT / 2));
        m_shell.setText(getText());
        m_shell.setSize(WIDTH, HEIGHT);
        m_shell.setLayout(new FillLayout());
        createControl(m_shell);
        m_shell.open();
        Display display = parent.getDisplay();
        while (!m_shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        // TODO: grab the selected port type
        return m_port;
    }

    private void createControl(final Shell parent) {
        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayout(new GridLayout(2, false));
        m_error = new Label(composite, SWT.NONE);
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL);
        gridData.horizontalSpan = 2;
        m_error.setLayoutData(gridData);

//        Label label1 = new Label(composite, SWT.NONE);
//        label1.setText("Port Name: ");
//        m_name = new Text(composite, SWT.BORDER | SWT.SHADOW_IN);
//        m_name.addFocusListener(new FocusAdapter() {
//            @Override
//            public void focusGained(final FocusEvent e) {
//                resetError();
//            }
//
//        });

        Label label2 = new Label(composite, SWT.NONE);
        label2.setText("Port Type:");
        m_type = new Combo(composite,
                SWT.DROP_DOWN | SWT.SIMPLE | SWT.READ_ONLY | SWT.BORDER);
        AddMetaNodePortTypeCollector instance =
            AddMetaNodePortTypeCollector.getInstance();
        MetaNodePortType[] types = instance.getTypes();
        String[] names = new String[types.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = types[i].getName();
        }
        m_type.setItems(names);
        m_type.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(final FocusEvent e) {
                resetError();
            }

        });
        Button ok = new Button(composite, SWT.PUSH);
        ok.setText("OK");
        ok.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            public void widgetSelected(final SelectionEvent e) {
                if (m_type.getSelectionIndex() < 0) {
                    setError("Please select port type");
                    return;
                }
//                if (m_name.getText() == null
//                        || m_name.getText().trim().isEmpty()) {
//                    setError("Please enter port name");
//                    return;
//                }
                resetError();
                String selected = m_type.getItem(m_type.getSelectionIndex());
                MetaNodePortType type = null;
                for (MetaNodePortType t
                        : AddMetaNodePortTypeCollector.
                        getInstance().getTypes()) {
                    if (t.getName().equals(selected)) {
                        type = t;
                        break;
                    }
                }
                if (type == null) {
                    throw new IllegalStateException(
                            "Unknown port type: " + selected);
                }
                m_port = new Port(type.getType(), type.getName());
                m_shell.dispose();
            }

        });
        gridData = new GridData();
        gridData.widthHint = 80;
        ok.setLayoutData(gridData);

        Button cancel = new Button(composite, SWT.PUSH);
        cancel.setText("Cancel");
        cancel.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                m_port = null;
                m_shell.dispose();
            }

        });
        gridData = new GridData();
        gridData.widthHint = 80;
        cancel.setLayoutData(gridData);
    }


    private void setError(final String error) {
        m_error.setForeground(ColorConstants.red);
        m_error.setText(error);
    }

    private void resetError() {
        m_error.setText("");
    }
}
