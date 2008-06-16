/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DatabasePortObject;
import org.knime.core.node.ModelPortObject;

/**
 * Dialog to enter the port name and type.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class MetaPortDialog extends Dialog {

    private enum PortTypes {
        /** Data port. */
        DataPort,
        /** Model port. */
        ModelPort,
        /** Database port. */
        DatabasePort;
        
        private static String[] names;
        
        static {
            names = new String[3];
            names[0] = DataPort.name();
            names[1] = ModelPort.name();
            names[2] = DatabasePort.name();
        }
        
        /**
         * 
         * @return the enunm fields as a string array
         */
        public static String[] getNames() {
            return names;
        }
    }

    private Shell m_shell;
    private Label m_error;
    private Text m_name;
    private Combo m_type;

    private static final int WIDTH = 200;
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

        Label label1 = new Label(composite, SWT.NONE);
        label1.setText("Port Name: ");
        m_name = new Text(composite, SWT.BORDER | SWT.SHADOW_IN);
        m_name.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                resetError();
            }

        });

        Label label2 = new Label(composite, SWT.NONE);
        label2.setText("Port Type:");
        m_type = new Combo(composite,
                SWT.DROP_DOWN | SWT.SIMPLE | SWT.READ_ONLY | SWT.BORDER);
        m_type.setItems(PortTypes.names);
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
                if (m_name.getText() == null
                        || m_name.getText().trim().isEmpty()) {
                    setError("Please enter port name");
                    return;
                }
                resetError();
                String selected = m_type.getItem(m_type.getSelectionIndex());
                if (PortTypes.valueOf(selected).equals(PortTypes.DataPort)) {
                    m_port = new Port(BufferedDataTable.TYPE, m_name.getText());
                } else if (PortTypes.valueOf(selected).equals(
                        PortTypes.ModelPort)) {
                    m_port = new Port(ModelPortObject.TYPE, m_name.getText());
                } else if (PortTypes.valueOf(selected).equals(
                        PortTypes.DatabasePort)) {
                    m_port = new Port(DatabasePortObject.TYPE, 
                            m_name.getText());
                }
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
