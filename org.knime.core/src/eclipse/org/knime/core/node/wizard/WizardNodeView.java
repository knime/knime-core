/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 * ---------------------------------------------------------------------
 *
 * Created on Apr 22, 2013 by Berthold
 */
package org.knime.core.node.wizard;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
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
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeModel;
import org.knime.core.node.interactive.ConfigureCallback;
import org.knime.core.node.interactive.DefaultConfigureCallback;
import org.knime.core.node.interactive.DefaultReexecutionCallback;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.InteractiveViewDelegate;
import org.knime.core.node.interactive.ReexecutionCallback;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WizardExecutionController;
import org.knime.core.node.workflow.WorkflowManager;

/** Standard implementation for interactive views which are launched on the client side via
 * an integrated browser. They only have indirect access to the NodeModel via get and
 * setViewContent methods and therefore simulate the behavior of the same view in the
 * WebPortal.
 *
 * @author B. Wiswedel, M. Berthold, Th. Gabriel, C. Albrecht
 * @param <T> requires a {@link NodeModel} implementing {@link WizardNode} as well
 * @param <REP> the {@link WebViewContent} implementation used
 * @param <VAL>
 * @since 2.9
 */
public final class WizardNodeView<T extends NodeModel & WizardNode<REP, VAL>, REP extends WebViewContent,
        VAL extends WebViewContent> extends AbstractNodeView<T> implements InteractiveView<T, REP, VAL> {

    private final InteractiveViewDelegate<VAL> m_delegate;
    private final WebTemplate m_template;
    private File m_tempFolder;

    /**
     * @param nodeModel the underlying model
     * @since 2.10
     */
    public WizardNodeView(final T nodeModel) {
        super(nodeModel);
        m_template = WizardExecutionController.getWebTemplateFromJSObjectID(getNodeModel().getJavascriptObjectID());
        m_delegate = new InteractiveViewDelegate<VAL>();
    }

    @Override
    public void setWorkflowManagerAndNodeID(final WorkflowManager wfm, final NodeID id) {
        m_delegate.setWorkflowManagerAndNodeID(wfm, id);
    }

    @Override
    public boolean canReExecute() {
        return m_delegate.canReExecute();
    }

    @Override
    public void triggerReExecution(final VAL value, final ReexecutionCallback callback) {
        m_delegate.triggerReExecution(value, callback);
    }

    @Override
    public void setNewDefaultConfiguration(final ConfigureCallback callback) {
        m_delegate.setNewDefaultConfiguration(callback);
    }

    /**
     * Load a new ViewValue into the underlying NodeModel.
     *
     * @param value the new value of the view.
     */
    protected final void loadViewValueIntoNode(final VAL value) {
        triggerReExecution(value, new DefaultReexecutionCallback());
    }

    /** Set current ViewContent as new default settings of the underlying NodeModel.
     */
    protected final void makeViewContentNewDefault() {
        m_delegate.setNewDefaultConfiguration(new DefaultConfigureCallback());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged() {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void callOpenView(final String title) {
        final String jsonViewRepresentation;
        final String jsonViewValue;
        try {
            REP rep = getNodeModel().getViewRepresentation();
            if (rep != null) {
                jsonViewRepresentation = ((ByteArrayOutputStream)rep.saveToStream()).toString("UTF-8");
            } else {
                jsonViewRepresentation = "null";
            }
            VAL val = getNodeModel().getViewValue();
            if (val != null) {
                jsonViewValue = ((ByteArrayOutputStream)val.saveToStream()).toString("UTF-8");
            } else {
                jsonViewValue = "null";
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("No view content available!");
        }

        Display display = Display.getCurrent();
        final Shell shell = new Shell(display);
        shell.setText(title);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        shell.setLayout(layout);

        final Browser browser = new Browser(shell, SWT.NONE);
        browser.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        Composite buttonComposite = new Composite(shell, SWT.NONE);
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
                applyTriggered(browser);
            }
        });

        newDefaultButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                makeViewContentNewDefault();
            }
        });

        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                shell.dispose();
            }
        });

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(final ProgressEvent event) {
                browser.evaluate(wrapInTryCatch(initJSView(jsonViewRepresentation, jsonViewValue)));
            }
        });

        try {
            browser.setUrl(createWebResources());
        } catch (IOException e) {
            if (m_tempFolder != null) {
                deleteTempFolder(m_tempFolder);
            }
            browser.setText(createErrorHTML(e));
        }

        shell.setSize(800, 600);
        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                callCloseView();
            }
        });
        shell.open();
    }

    /**
     * @param e
     * @return
     */
    private String createErrorHTML(final IOException e) {
        String setIEVersion = "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">";

        StringBuilder pageBuilder = new StringBuilder();
        pageBuilder.append("<!doctype html><html><head>");
        pageBuilder.append(setIEVersion);
        pageBuilder.append("</head><body>");
        // content
        pageBuilder.append(e.getMessage());
        // content end
        pageBuilder.append("</body></html>");

        return pageBuilder.toString();
    }

    /**
     * @return
     */
    private String createWebResources() throws IOException {
        m_tempFolder = createTempFolder();
        File htmlFile = new File(m_tempFolder, "index.html");
        htmlFile.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile));
        writer.write(buildHTMLResource());
        writer.flush();
        writer.close();
        copyWebResources();
        return htmlFile.getAbsolutePath();
    }

    private String buildHTMLResource() throws IOException {

        String setIEVersion = "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">";
        //String debugScript = "<script type=\"text/javascript\" "
        //        + "src=\"https://getfirebug.com/firebug-lite.js#startOpened=true\"></script>";
        String scriptString = "<script type=\"text/javascript\" src=\"%s\" charset=\"UTF-8\"></script>";
        String cssString = "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\">";

        StringBuilder pageBuilder = new StringBuilder();
        pageBuilder.append("<!doctype html><html><head>");
        pageBuilder.append(setIEVersion);
        pageBuilder.append("<meta charset=\"UTF-8\">");
        //pageBuilder.append(debugScript);

        for (WebResourceLocator resFile : getResourceFileList()) {
            String path;
            switch (resFile.getType()) {
                case CSS:
                    path = resFile.getRelativePathTarget();
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    pageBuilder.append(String.format(cssString, path));
                    break;
                case JAVASCRIPT:
                    path = resFile.getRelativePathTarget();
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    pageBuilder.append(String.format(scriptString, path));
                    break;
                case FILE:
                    break;
                default:
                    throw new IOException("Unrecognized resource type " + resFile.getType());
            }
        }
        pageBuilder.append("</head><body></body></html>");
        return pageBuilder.toString();
    }

    /**
     * @param scriptString
     * @param pageBuilder
     */
    private ArrayList<WebResourceLocator> getResourceFileList() {
        ArrayList<WebResourceLocator> resourceFiles = new ArrayList<WebResourceLocator>();

        if (m_template.getWebResources() != null) {
            resourceFiles.addAll(Arrays.asList(m_template.getWebResources()));
        }

        return resourceFiles;
    }

    private void copyWebResources() throws IOException {
        for (WebResourceLocator resFile : getResourceFileList()) {
            FileUtils.copyFileToDirectory(resFile.getResource(),
                new File(m_tempFolder, FilenameUtils.separatorsToSystem(resFile.getRelativePathTarget()))
                    .getParentFile());
        }
    }

    private File createTempFolder() throws IOException {
        File tempDir = null;
        String javaTmpDir = System.getProperty("java.io.tmpdir");
        do {
            tempDir = new File(javaTmpDir, m_template.getNamespace() + System.currentTimeMillis());
        } while (tempDir.exists());
        if (!tempDir.mkdirs()) {
            throw new IOException("Cannot create temporary directory '" + tempDir.getCanonicalPath() + "'.");
        }

        return tempDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void callCloseView() {
        if (m_tempFolder != null && m_tempFolder.exists()) {
            deleteTempFolder(m_tempFolder);
        }
    }

    private void deleteTempFolder(final File tempFolder) {
        for (File tempFile : tempFolder.listFiles()) {
            if (tempFile.isDirectory()) {
                deleteTempFolder(tempFile);
            } else {
                tempFile.delete();
            }
        }
        tempFolder.delete();
    }

    private String initJSView(final String jsonViewRepresentation, final String jsonViewValue) {
        String escapedRepresentation = jsonViewRepresentation.replace("\\", "\\\\").replace("'", "\\'");
        String escapedValue = jsonViewValue.replace("\\", "\\\\").replace("'", "\\'");
        String initMethod = m_template.getInitMethodName();
        return getNamespacePrefix() + initMethod + "(JSON.parse('" + escapedRepresentation + "'), JSON.parse('"
            + escapedValue + "'));";
    }

    private String getNamespacePrefix() {
        String namespace = m_template.getNamespace();
        if (namespace != null && !namespace.isEmpty()) {
            namespace += ".";
        } else {
            namespace = "";
        }
        return namespace;
    }

    /**
     * @param jsonViewContent
     * @return
     */
    private String wrapInTryCatch(final String jsCode) {
        StringBuilder builder = new StringBuilder();
        builder.append("try {");
        builder.append(jsCode);
        builder.append("} catch(err) {alert(err);}");
        return builder.toString();
    }

    private void applyTriggered(final Browser browser) {
        boolean valid = true;
        String validateMethod = m_template.getValidateMethodName();
        if (validateMethod != null && !validateMethod.isEmpty()) {
            String evalCode = wrapInTryCatch("return JSON.stringify(" + getNamespacePrefix() + validateMethod + "());");
            String jsonString = (String)browser.evaluate(evalCode);
            valid = Boolean.parseBoolean(jsonString);
        }
        if (valid) {
            String pullMethod = m_template.getPullViewContentMethodName();
            String evalCode = wrapInTryCatch("return JSON.stringify(" + getNamespacePrefix() + pullMethod + "());");
            String jsonString = (String)browser.evaluate(evalCode);
            try {
                VAL viewValue = getNodeModel().createEmptyViewValue();
                viewValue.loadFromStream(new ByteArrayInputStream(jsonString.getBytes(Charset.forName("UTF-8"))));
                loadViewValueIntoNode(viewValue);
            } catch (Exception e) {
                //TODO error message
            }
        }
    }
}
