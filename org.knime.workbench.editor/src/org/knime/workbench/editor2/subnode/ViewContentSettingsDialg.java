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
 *   19 Jan 2017 (albrecht): created
 */
package org.knime.workbench.editor2.subnode;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;
import org.knime.js.core.layout.bs.JSONLayoutViewContent.ResizeMethod;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public class ViewContentSettingsDialg extends TitleAreaDialog {

    private JSONLayoutViewContent m_viewSettings;

    private Combo m_resizeMethodCombo;


    /**
     * @param viewSettings
     * @param parentShell
     */
    public ViewContentSettingsDialg(final JSONLayoutViewContent viewSettings, final Shell parentShell) {
        super(parentShell);
        m_viewSettings = viewSettings.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        super.create();
        setTitle("Layout view settings for node ID: " + m_viewSettings.getNodeID());
        setMessage("Choose the appropriate layout settings for the node view", IMessageProvider.NONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        createResizeMethodCombo(container);
        return area;
    }

    private void createResizeMethodCombo(final Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("Resize Method");

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        m_resizeMethodCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        m_resizeMethodCombo.setLayoutData(gridData);
        for (ResizeMethod resizeMethod : ResizeMethod.values()) {
            m_resizeMethodCombo.add(resizeMethod.toValue());
        }
        m_resizeMethodCombo.setText(m_viewSettings.getResizeMethod().toValue());
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
    protected void okPressed() {
        saveViewSettings();
        super.okPressed();
    }

    /**
     *
     */
    private void saveViewSettings() {
        try {
            m_viewSettings.setResizeMethod(ResizeMethod.forValue(m_resizeMethodCombo.getText()));
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
        }
    }

    /**
     * @return the viewSettings
     */
    public JSONLayoutViewContent getViewSettings() {
        return m_viewSettings;
    }

}
