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
 * -------------------------------------------------------------------
 *
 * History
 *   14.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.subnode;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * A WizardPage to set a subnode description
 *
 * @author Ferry Abt, KNIME GmbH, Konstanz, Germany
 */
public class SubnodeOutputPortDescriptionPage extends WizardPage {

    private static final String DESCRIPTION = "Provide descriptions for the output ports";

    private Combo m_portSelection;

    private Text m_portName;

    private Text m_portDescription;

    private String[] m_names;

    private String[] m_descriptions;

    private SubNodeContainer m_subNode;

    private int m_lastIndex;

    /**
     * Creates the page and sets title and description.
     *
     * @param title of the wizard page
     */
    public SubnodeOutputPortDescriptionPage(final String title) {
        super(title);
        setDescription(DESCRIPTION);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        Label title = new Label(composite, SWT.BOLD);
        title.setText("Select Port: ");
        m_portSelection = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        m_portSelection.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectionChanged();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                selectionChanged();
            }
        });
        m_portSelection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        title = new Label(composite, SWT.BOLD);
        title.setText("Port Name: ");
        m_portName = new Text(composite, SWT.BORDER);
        m_portName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        title = new Label(composite, SWT.BOLD);
        title.setText("Port Description: ");
        m_portDescription = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        m_portDescription.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (m_descriptions != null) {
            String[] items = new String[m_descriptions.length];
            for (int i = 0; i < m_descriptions.length; i++) {
                items[i] = "port" + (i + 1);
            }
            m_portSelection.setItems(items);
            m_lastIndex = -1;
            m_portSelection.select(0);
            selectionChanged();
        }
        setControl(composite);
    }

    private void selectionChanged() {
        int selectedIndex = m_portSelection.getSelectionIndex();
        if (m_lastIndex != selectedIndex) {
            if (m_lastIndex >= 0) {
                m_names[m_lastIndex] = m_portName.getText();
                m_descriptions[m_lastIndex] = m_portDescription.getText();
            }
            m_portName.setText(m_names[selectedIndex]);
            m_portDescription.setText(m_descriptions[selectedIndex]);
            m_lastIndex = selectedIndex;
        }
    }

    /**
     * This page initializes from the sub node.
     *
     * @param subNode the sub node to initialize the description from
     */
    public void setSubNode(final SubNodeContainer subNode) {
        m_subNode = subNode;
        m_descriptions = new String[m_subNode.getNrOutPorts() - 1];
        m_names = new String[m_descriptions.length];
        String[] outDescriptions = m_subNode.getOutPortDescriptions();
        for (int i = 0; i < m_descriptions.length; i++) {
            m_descriptions[i] = i < outDescriptions.length ? outDescriptions[i] : "";
            m_names[i] = m_subNode.getOutPort(i + 1).getPortName();
        }
    }

    /**
     * @return the entered port descriptions
     */
    public String[] getPortDescriptions() {
        m_descriptions[m_portSelection.getSelectionIndex()] = m_portDescription.getText();
        return m_descriptions;
    }

    /**
     * @return the entered port descriptions
     */
    public String[] getPortNames() {
        m_names[m_portSelection.getSelectionIndex()] = m_portName.getText();
        return m_names;
    }

}
