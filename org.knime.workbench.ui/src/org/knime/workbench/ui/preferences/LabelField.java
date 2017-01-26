/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.ui.preferences;

import java.net.URL;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

/**
 * Preference page field showing text.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class LabelField extends FieldEditor {

    private Link m_link;

    /**
     * @param parent the parent of the field editor's control
     * @param text to show
     */
    public LabelField(final Composite parent, final String text) {
        super("TXT_FIELD", "", parent);
        m_link.setText(text);
        m_link.addSelectionListener(new SelectionAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    //Open external browser
                    IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                    browser.openURL(new URL(e.text));
                  } catch (Exception ex) {
                      /* do nothing */
                  }
            }
        });
    }

    /**
     * @param parent the parent of the field editor's control
     * @param text to show
     * @param swtFontStyleConstants additional style constants like SWT.BOLD.
     */
    public LabelField(final Composite parent, final String text, final int swtFontStyleConstants) {
        this(parent, text);
        FontData fontData = m_link.getFont().getFontData()[0];
        Font font = new Font(m_link.getDisplay(), new FontData(fontData.getName(), fontData
            .getHeight(), swtFontStyleConstants));
        m_link.setFont(font);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createControl(final Composite parent) {
        m_link = new Link(parent, SWT.NONE);
        super.createControl(parent); // calls doFillIntoGrid!
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void adjustForNumColumns(final int numColumns) {
        Object o = m_link.getLayoutData();
        if (o instanceof GridData) {
            ((GridData)o).horizontalSpan = numColumns;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFillIntoGrid(final Composite parent, final int numColumns) {
        m_link.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, numColumns, 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doLoad() {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doLoadDefault() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStore() {
        // nothing to store
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfControls() {
        return 1;
    }

}
