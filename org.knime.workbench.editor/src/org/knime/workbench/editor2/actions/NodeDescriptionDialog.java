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
 *   08.05.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.workflow.NodeID;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * JFace implementation of a dialog asking for a node description.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeDescriptionDialog extends Dialog {

    private final String m_initDescription;

    private Label m_nodeIdLabel;

    private Text m_descriptionField;

    private String m_description;

    private final String m_title;

    private NodeID m_nodeID;

    /**
     * Creates a dialog to ask for the user specified node description.
     *
     * @param parent the parent shell for this dialog
     * @param dialogTitle title for the dialog (node name, id, custom name)
     * @param descriptionInit the initial description
     * @param nodeID the initial name for the node
     */
    public NodeDescriptionDialog(final Shell parent, final String dialogTitle,
            final String descriptionInit,
            final NodeID nodeID) {
        super(parent);
//        this.setShellStyle(SWT.APPLICATION_MODAL);
        m_nodeID = nodeID;
        m_initDescription = descriptionInit;
        m_title = dialogTitle;
    }

    /**
     * Creates the widgets of this dialog.
     *
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(m_title);
        Image img = KNIMEUIPlugin.getDefault().getImageRegistry().get("knime");
        newShell.setImage(img);

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;

        Composite content = new Composite(newShell, SWT.NONE);
        content.setLayout(gridLayout);

        // NodeID label (bugfix 1402)
        Label idLabel = new Label(content, SWT.LEFT);
        idLabel.setText("NodeID: ");

        GridData nameData = new GridData();
        nameData.minimumWidth = 300;
        nameData.grabExcessHorizontalSpace = true;

        // NodeID value (bugfix 1402)
        m_nodeIdLabel = new Label(content, SWT.LEFT | SWT.READ_ONLY);
        m_nodeIdLabel.setText(m_nodeID.toString());
        m_nodeIdLabel.setLayoutData(nameData);

        // Description label
        Label descriptionLabel = new Label(content, SWT.RIGHT);
        descriptionLabel.setText("Custom description:");
        // Description value
        GridData descrData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL);
        descrData.minimumHeight = 200;
        descrData.minimumWidth = 300;
        descrData.grabExcessHorizontalSpace = true;
        descrData.grabExcessVerticalSpace = true;
        m_descriptionField = new Text(content, SWT.MULTI | SWT.WRAP
                | SWT.V_SCROLL);
        m_descriptionField.setLayoutData(descrData);
        //m_descriptionField.setSize(500, 300);
        if (m_initDescription == null) {
            m_descriptionField.setText("");
        } else {

            m_descriptionField.setText(m_initDescription);
        }
    }

    /**
     * Linux (GTK) hack: must explicitly invoke <code>getInitialSize()</code>.
     *
     * @see org.eclipse.jface.window.Window#create()
     */
    @Override
    public void create() {
        super.create();
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("linux")) {
            getShell().setSize(getInitialSize());
        }
    }

    /**
     * Invoked by the super class if ok is pressed. Copies the text field
     * content to member variable, so that they can be accessed afterwards.
     * (Necessary as the widgets will be disposed)
     *
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        m_description = m_descriptionField.getText();
        super.okPressed();
    }

    /**
     * @return the description input of the dialog.
     */
    public String getDescription() {
        return m_description;
    }

}
