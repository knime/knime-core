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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;
import org.knime.js.core.layout.bs.JSONLayoutViewContent.ResizeMethod;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public class ViewContentSettingsDialog extends TitleAreaDialog {

    private JSONLayoutViewContent m_viewSettings;
    private JSONLayoutViewContent m_defaultSettings;

    private Combo m_resizeMethodCombo;
    private Button m_autoResizeButton;
    private Button m_sizeHeightButton;
    private Button m_sizeWidthButton;
    private Button m_scrollingButton;
    private Text m_resizeIntervalText;
    private Text m_resizeToleranceSpinner;
    private Text m_minWidthText;
    private Text m_maxWidthText;
    private Text m_minHeightText;
    private Text m_maxHeightText;
    private Label m_errorText;

    /**
     * Creates a new additional settings dialog
     * @param parentShell the parent SWT shell
     * @param viewSettings the current view settings to display
     * @param nodeDefaultSettings the node's default settings
     */
    public ViewContentSettingsDialog(final Shell parentShell, final JSONLayoutViewContent viewSettings, final JSONLayoutViewContent nodeDefaultSettings) {
        super(parentShell);
        m_viewSettings = viewSettings.clone();
        m_defaultSettings = nodeDefaultSettings;
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
        GridLayout layout = new GridLayout(4, false);
        layout.marginWidth = 10;
        layout.horizontalSpacing = 10;
        container.setLayout(layout);

        createResizeMethodCombo(container);
        createAdditionalResizeFields(container);
        createDimensionFields(container);
        createErrorLabelField(container);
        fillFields();
        return area;
    }

    private void createResizeMethodCombo(final Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("Resize Method:");

        GridData gridData = new GridData();
        gridData.horizontalSpan = 3;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;

        m_resizeMethodCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        m_resizeMethodCombo.setLayoutData(gridData);
        for (ResizeMethod resizeMethod : ResizeMethod.values()) {
            m_resizeMethodCombo.add(resizeMethod.toValue());
        }
    }

    private void createAdditionalResizeFields(final Composite container) {
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;

        Label autoResizeLabel = new Label(container, SWT.NONE);
        autoResizeLabel.setText("Auto Resize:");
        m_autoResizeButton = new Button(container, SWT.CHECK);

        Label scrollingLabel = new Label(container, SWT.NONE);
        scrollingLabel.setText("Enable Scrolling:");
        m_scrollingButton = new Button(container, SWT.CHECK);

        Label sizeHeightLabel = new Label(container, SWT.NONE);
        sizeHeightLabel.setText("Size Height:");
        m_sizeHeightButton = new Button(container, SWT.CHECK);

        Label sizeWidthLabel = new Label(container, SWT.NONE);
        sizeWidthLabel.setText("Size Width:");
        m_sizeWidthButton = new Button(container, SWT.CHECK);

        Label resizeIntervalLabel = new Label(container, SWT.NONE);
        resizeIntervalLabel.setText("Resize Interval:");
        m_resizeIntervalText = new Text(container, SWT.SINGLE | SWT.BORDER);
        m_resizeIntervalText.setLayoutData(gridData);

        Label resizeToleranceLabel = new Label(container, SWT.NONE);
        resizeToleranceLabel.setText("Resize Tolerance:");
        m_resizeToleranceSpinner = new Text(container, SWT.SINGLE | SWT.BORDER);
        m_resizeToleranceSpinner.setLayoutData(gridData);
    }

    private void createDimensionFields(final Composite container) {
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;

        Label minWidthLabel = new Label(container, SWT.NONE);
        minWidthLabel.setText("Minimum Width:");
        m_minWidthText = new Text(container, SWT.SINGLE | SWT.BORDER);
        m_minWidthText.setLayoutData(gridData);

        Label maxWidthLabel = new Label(container, SWT.NONE);
        maxWidthLabel.setText("Maximum Width:");
        m_maxWidthText = new Text(container, SWT.SINGLE | SWT.BORDER);
        m_maxWidthText.setLayoutData(gridData);

        Label minHeightLabel = new Label(container, SWT.NONE);
        minHeightLabel.setText("Minimum Height:");
        m_minHeightText = new Text(container, SWT.SINGLE | SWT.BORDER);
        m_minHeightText.setLayoutData(gridData);

        Label maxHeightLabel = new Label(container, SWT.NONE);
        maxHeightLabel.setText("Maximum Height:");
        m_maxHeightText = new Text(container, SWT.SINGLE | SWT.BORDER);
        m_maxHeightText.setLayoutData(gridData);
    }

    private void createErrorLabelField(final Composite container) {
        GridData gridData = new GridData();
        gridData.horizontalSpan = 4;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;

        m_errorText = new Label(container, SWT.NONE);
        m_errorText.setLayoutData(gridData);
        m_errorText.setForeground(new Color(container.getDisplay(), new RGB(211, 17, 17)));
    }

    private void fillFields() {
        m_resizeMethodCombo.setText(m_viewSettings.getResizeMethod().toValue());
        m_autoResizeButton.setSelection(m_viewSettings.getAutoResize());
        m_scrollingButton.setSelection(m_viewSettings.getScrolling());
        m_sizeHeightButton.setSelection(m_viewSettings.getSizeHeight());
        m_sizeWidthButton.setSelection(m_viewSettings.getSizeWidth());

        Integer resizeInterval = m_viewSettings.getResizeInterval();
        m_resizeIntervalText.setText(resizeInterval == null ? "" : resizeInterval.toString());
        Integer resizeTolerance = m_viewSettings.getResizeTolerance();
        m_resizeToleranceSpinner.setText(resizeTolerance == null ? "" : resizeTolerance.toString());

        Integer minWidth = m_viewSettings.getMinWidth();
        m_minWidthText.setText(minWidth == null ? "" : minWidth.toString());
        Integer maxWidth = m_viewSettings.getMaxWidth();
        m_maxWidthText.setText(maxWidth == null ? "" : maxWidth.toString());
        Integer minHeight = m_viewSettings.getMinHeight();
        m_minHeightText.setText(minHeight == null ? "" : minHeight.toString());
        Integer maxHeight = m_viewSettings.getMaxHeight();
        m_maxHeightText.setText(maxHeight == null ? "" : maxHeight.toString());
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
        if (saveViewSettings()) {
            super.okPressed();
        }
    }

    /**
     *
     */
    private boolean saveViewSettings() {
        try {
            m_viewSettings.setResizeMethod(ResizeMethod.forValue(m_resizeMethodCombo.getText()));
            m_viewSettings.setAutoResize(m_autoResizeButton.getSelection());
            m_viewSettings.setSizeHeight(m_sizeHeightButton.getSelection());
            m_viewSettings.setSizeWidth(m_sizeWidthButton.getSelection());
            m_viewSettings.setScrolling(m_scrollingButton.getSelection());
            String resizeInterval = m_resizeIntervalText.getText();
            m_viewSettings.setResizeInterval(StringUtils.isEmpty(resizeInterval) ? null : Integer.parseInt(resizeInterval));
            String resizeTolerance = m_resizeToleranceSpinner.getText();
            m_viewSettings.setResizeTolerance(StringUtils.isEmpty(resizeTolerance) ? null : Integer.parseInt(resizeTolerance));
            String minWidth = m_minWidthText.getText();
            m_viewSettings.setMinWidth(StringUtils.isEmpty(minWidth) ? null : Integer.parseInt(minWidth));
            String maxWidth = m_maxWidthText.getText();
            m_viewSettings.setMaxWidth(StringUtils.isEmpty(maxWidth) ? null : Integer.parseInt(maxWidth));
            String minHeight = m_minHeightText.getText();
            m_viewSettings.setMinHeight(StringUtils.isEmpty(minHeight) ? null : Integer.parseInt(minHeight));
            String maxHeight = m_maxHeightText.getText();
            m_viewSettings.setMaxHeight(StringUtils.isEmpty(maxHeight) ? null : Integer.parseInt(maxHeight));
            return true;
        } catch (Exception e) {
            String error = e.getMessage();
            if (e instanceof NumberFormatException) {
                error = "Value needs to be integer. " + error;
            }
            m_errorText.setText(error);
            return false;
        }
    }

    /**
     * Retrieves the view settings as currently represented by the dialog
     * @return the viewSettings the current view settings
     */
    public JSONLayoutViewContent getViewSettings() {
        return m_viewSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        // Change parent layout data to fill the whole bar
        parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button resetButton = createButton(parent, IDialogConstants.NO_ID, "Reset", false);
        resetButton.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/reset.png"));
        resetButton.setToolTipText("Reset settings to node's current default configuration");
        resetButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_viewSettings = m_defaultSettings.clone();
                fillFields();
            }
        });

        // Create a spacer label
        Label spacer = new Label(parent, SWT.NONE);
        spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Update layout of the parent composite to count the spacer
        GridLayout layout = (GridLayout)parent.getLayout();
        layout.numColumns++;
        layout.makeColumnsEqualWidth = false;

        // create OK and Cancel buttons
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

}
