/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 *
 * History
 *   14.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;

import java.util.ArrayList;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

/**
 * The single page of the {@link AddMetaNodeWizard}.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class AddMetaNodePage extends WizardPage {

    private static final String TITLE = "Add Meta Node";
    private static final String DESCRIPTION = "Define the number and type of "
        + "the desired in and out ports.";

    private Text m_name;
    private List m_inPorts;
    private List m_outPorts;
    private final java.util.List<Port>m_inPortList = new ArrayList<Port>();
    private final java.util.List<Port>m_outPortList = new ArrayList<Port>();

    private boolean m_isComplete;

    /**
     * Creates the page and sets title and description.
     */
    public AddMetaNodePage() {
        super(TITLE);
        setDescription(DESCRIPTION);
    }

    /**
     *
     * {@inheritDoc}
     */
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        createTopPart(composite);
        createCenterPart(composite);
        setControl(composite);
    }

    /**
     *
     * @return list of entered out ports
     */
    public java.util.List<Port> getOutPorts() {
        return m_outPortList;
    }

    /**
     *
     * @return list of entered in ports
     */
    public java.util.List<Port>getInports() {
        return m_inPortList;
    }


    /**
     *
     * @return the entered name for the meta node
     */
    public String getMetaNodeName() {
        return m_name.getText();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isPageComplete() {
        return m_isComplete;
    }

    private void updateStatus() {
        m_isComplete = true;
        // node name no longer mandatory
        /*
        if (m_name.getText() == null || m_name.getText().isEmpty()) {
            m_isComplete = false;
        }
        */
        if (m_inPortList.size() == 0 && m_outPortList.size() == 0) {
            m_isComplete = false;
        }
        setPageComplete(m_isComplete);
    }

    private void createCenterPart(final Composite parent) {
        Composite composite = new Composite(parent, SWT.TOP);
        composite.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        createPortPart(composite, true);
        createPortPart(composite, false);
    }

    private void createPortPart(final Composite parent, final boolean in) {
        // over all
        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        // left (label and list)
        Composite left = new Composite(composite, SWT.LEFT);
        left.setLayout(new GridLayout(1, false));
        gridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        left.setLayoutData(gridData);
        // right (buttons)
        Composite right = new Composite(composite, SWT.RIGHT);
        right.setLayout(new GridLayout(1, false));

        Label label = new Label(left, SWT.NONE);
        Button add;
        Button remove;
        if (in) {
            label.setText("In Ports:");
            m_inPorts = new List(left,
                    SWT.SINGLE | SWT.SCROLL_PAGE | SWT.BORDER);
            add = new Button(right, SWT.PUSH);
            add.setText("Add");
            remove = new Button(right, SWT.PUSH);
            remove.setText("Remove");
        } else {
            label.setText("Out Ports:");
            m_outPorts = new List(left,
                    SWT.SINGLE | SWT.SCROLL_PAGE | SWT.BORDER);
            add = new Button(right, SWT.PUSH);
            add.setText("Add");
            remove = new Button(right, SWT.PUSH);
            remove.setText("Remove");
        }
        // layout label
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                GridData.VERTICAL_ALIGN_CENTER, false, false);
        label.setLayoutData(gridData);
        // layout list
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER
                | GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        gridData.widthHint = 100;
        if (in) {
            m_inPorts.setLayoutData(gridData);
        } else {
            m_outPorts.setLayoutData(gridData);
        }
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER,
                GridData.VERTICAL_ALIGN_CENTER, false, true);
        gridData.widthHint = 80;
        add.setLayoutData(gridData);

        add.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
                MetaPortDialog dialog = new MetaPortDialog(
                        Display.getDefault().getActiveShell());
                Port port = dialog.open();
                if (port != null) {
                    if (in) {
                        m_inPorts.add(port.toString());
                        m_inPortList.add(port);
                    } else {
                        m_outPorts.add(port.toString());
                        m_outPortList.add(port);
                    }
                }
                updateStatus();
            }

            public void widgetSelected(final SelectionEvent e) {
                widgetDefaultSelected(e);
            }

        });

        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER,
                GridData.VERTICAL_ALIGN_CENTER, false, true);
        gridData.widthHint = 80;
        remove.setLayoutData(gridData);
        remove.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                if (in) {
                    remove(true, m_inPorts.getItem(
                            m_inPorts.getSelectionIndex()));
                    m_inPorts.remove(m_inPorts.getSelectionIndex());
                } else {
                    remove(false, m_outPorts.getItem(
                            m_outPorts.getSelectionIndex()));
                    m_outPorts.remove(m_inPorts.getSelectionIndex());
                }
                updateStatus();
            }

        });
    }

    private void createTopPart(final Composite parent) {
        // top part
        Composite composite = new Composite(parent, SWT.TOP);
        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 2;
        composite.setLayout(topLayout);
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL);
        composite.setLayoutData(gridData);
        // label
        Label label = new Label(composite, SWT.NONE);
        label.setText("Meta Node Name:");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                GridData.VERTICAL_ALIGN_CENTER, false, false);
        label.setLayoutData(gridData);
        // text field
        m_name = new Text(composite, SWT.BORDER);
        gridData = new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
                | GridData.VERTICAL_ALIGN_CENTER);
        m_name.setLayoutData(gridData);
        m_name.addFocusListener(new FocusAdapter() {
           @Override
            public void focusLost(final FocusEvent e) {
                   updateStatus();
            }
        });
    }

    private void remove(final boolean in, final String port) {
        if (in) {
            for (Port p : m_inPortList) {
                if (p.toString().equals(port)) {
                    m_inPortList.remove(p);
                    break;
                }
            }
        } else {
            for (Port p : m_outPortList) {
                if (p.toString().equals(port)) {
                    m_outPortList.remove(p);
                    break;
                }
            }
        }
    }

}
