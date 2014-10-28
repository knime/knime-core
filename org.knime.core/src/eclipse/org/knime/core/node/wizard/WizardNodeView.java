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
 * ---------------------------------------------------------------------
 *
 * Created on Apr 22, 2013 by Berthold
 */
package org.knime.core.node.wizard;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.interactive.DefaultReexecutionCallback;
import org.knime.core.node.web.WebViewContent;

/**
 * Standard implementation for interactive views which are launched on the client side via an integrated browser. They
 * only have indirect access to the NodeModel via get and setViewContent methods and therefore simulate the behavior of
 * the same view in the WebPortal.
 *
 * @author B. Wiswedel, M. Berthold, Th. Gabriel, C. Albrecht
 * @param <T> requires a {@link NodeModel} implementing {@link WizardNode} as well
 * @param <REP> the {@link WebViewContent} implementation used
 * @param <VAL>
 * @since 2.9
 */
public final class WizardNodeView<T extends NodeModel & WizardNode<REP, VAL>,
        REP extends WebViewContent, VAL extends WebViewContent> extends AbstractWizardNodeView<T, REP, VAL> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WizardNodeView.class);

    private Shell m_shell;
    private Browser m_browser;
    private String m_title;
    private ProgressListener m_completedListener;

    /**
     * @param nodeModel the underlying model
     * @param viewHTML
     * @param template
     * @since 2.10
     */
    public WizardNodeView(final T nodeModel) {
        super(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged() {
        Display display = getDisplay();
        if (display == null) {
            // view most likely disposed
            return;
        }
        display.asyncExec(new Runnable() {

            @Override
            public void run() {
                if (m_browser != null && !m_browser.isDisposed()) {
                    synchronized (m_browser) {
                        setBrowserURL();
                    }
                }
            }
        });
    }

    private Display getDisplay() {
        //Display display = new Display();
        Display display = Display.getCurrent();
        if (display == null && m_browser != null && !m_browser.isDisposed()) {
            display = m_browser.getDisplay();
        }
        return display;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void callOpenView(final String title) {
        m_title = (title == null ? "View" : title);

        Display display = getDisplay();
        m_shell = new Shell(display, SWT.SHELL_TRIM);
        m_shell.setText(m_title);

        if (KNIMEConstants.KNIME16X16_SWT != null) {
            m_shell.setImage(KNIMEConstants.KNIME16X16_SWT);
        }
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        m_shell.setLayout(layout);

        m_browser = new Browser(m_shell, SWT.NONE);
        m_browser.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        m_browser.setText(createMessageHTML("Loading view..."), true);

        Composite buttonComposite = new Composite(m_shell, SWT.NONE);
        buttonComposite.setLayoutData(new GridData(GridData.END, GridData.END, false, false));
        buttonComposite.setLayout(new RowLayout());

        Button applyButton = new Button(buttonComposite, SWT.PUSH);
        applyButton.setText("Apply");
        Button newDefaultButton = new Button(buttonComposite, SWT.PUSH);
        newDefaultButton.setText("Use as new default");
        Button closeButton = new Button(buttonComposite, SWT.PUSH);
        closeButton.setText("Close");

        //action handler
        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                applyTriggered(m_browser, false);
            }
        });

        newDefaultButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                applyTriggered(m_browser, true);
            }
        });

        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_shell.dispose();
            }
        });

        m_shell.setSize(800, 600);
        m_shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                callCloseView();
            }
        });
        m_shell.open();

        display.asyncExec(new Runnable() {

            @Override
            public void run() {
                setBrowserURL();
            }
        });

    }

    private void setBrowserURL() {
        try {
            m_browser.setUrl(getViewSource().getAbsolutePath());
        } catch (Exception e) {
            m_browser.setText(createMessageHTML(e.getMessage()));
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void closeView() {
        m_shell = null;
        m_browser = null;
    }

    /**
     * @param jsonViewContent
     * @return
     */
    private String wrapInTryCatch(final String jsCode) {
        StringBuilder builder = new StringBuilder();
        builder.append("try {");
        builder.append(jsCode);
        builder.append("} catch(err) {if (err.stack) {alert(err.stack);} else {alert (err);}}");
        return builder.toString();
    }

    private void applyTriggered(final Browser browser, final boolean useAsDefault) {
        boolean valid = true;
        String validateMethod = getWebTemplate().getValidateMethodName();
        if (validateMethod != null && !validateMethod.isEmpty()) {
            String evalCode = wrapInTryCatch("return JSON.stringify(" + getNamespacePrefix() + validateMethod + "());");
            String jsonString = (String)browser.evaluate(evalCode);
            valid = Boolean.parseBoolean(jsonString);
        }
        if (valid) {
            String pullMethod = getWebTemplate().getPullViewContentMethodName();
            String evalCode = wrapInTryCatch("return JSON.stringify(" + getNamespacePrefix() + pullMethod + "());");
            String jsonString = (String)browser.evaluate(evalCode);
            try {
                VAL viewValue = getNodeModel().createEmptyViewValue();
                viewValue.loadFromStream(new ByteArrayInputStream(jsonString.getBytes(Charset.forName("UTF-8"))));
                triggerReExecution(viewValue, useAsDefault, new DefaultReexecutionCallback());
            } catch (Exception e) {
                //TODO error message
            }
        }
    }
}
