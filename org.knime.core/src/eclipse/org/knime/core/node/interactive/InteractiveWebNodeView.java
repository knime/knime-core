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
 * ---------------------------------------------------------------------
 *
 * Created on Apr 22, 2013 by Berthold
 */
package org.knime.core.node.interactive;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
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
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/** Standard implementation for interactive views which are launched on the client side via
 * an integrated browser. They only have indirect access to the NodeModel via get and
 * setViewContent methods and therefore simulate the behaviour of the same view in the
 * WebPortal.
 *
 * @author B. Wiswedel, M. Berthold, Th. Gabriel, C. Albrecht
 * @param <T> requires a {@link NodeModel} implementing {@link InteractiveWebNode} as well
 * @param <VC> the {@link ViewContent} implementation used
 * @since 2.8
 */
public final class InteractiveWebNodeView<T extends NodeModel & InteractiveWebNode<VC>, VC extends ViewContent>
                extends AbstractNodeView<T> implements InteractiveView<T> {

    private static final String CONTAINER_ID = "view";
    private final InteractiveViewDelegate m_delegate;
    private final WebViewTemplate m_template;
    private File m_tempFolder;
    private Class<VC> m_viewContentClass;

    /**
     * @param nodeModel the underlying model
     * @param wvt the template to be used for the web view
     * @param viewContentClass the class of the view content
     */
    public InteractiveWebNodeView(final T nodeModel, final WebViewTemplate wvt, final Class<VC> viewContentClass) {
        super(nodeModel);
        m_template = wvt;
        if (viewContentClass == null) {
            throw new NullPointerException("ViewContent class can not be null!");
        }
        m_viewContentClass = viewContentClass;
        m_delegate = new InteractiveViewDelegate();
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
    public void triggerReExecution(final ReexecutionCallback callback) {
        m_delegate.triggerReExecution(callback);
    }

    @Override
    public void setNewDefaultConfiguration(final ConfigureCallback callback) {
        m_delegate.setNewDefaultConfiguration(callback);
    }

    /**
     * Load a new ViewContent into the underlying NodeModel.
     *
     * @param vc the new content of the view.
     */
    protected final void loadViewContentIntoNode(final VC vc) {
        getNodeModel().loadViewContent(vc);
        triggerReExecution(new DefaultReexecutionCallback());
    }

    /**
     * @return ViewContent of the underlying NodeModel.
     */
    protected final VC getViewContentFromNode() {
        return getNodeModel().createViewContent();
    }

    /** Set current ViewContent as new default settings of the underlying NodeModel.
     */
    protected final void makeViewContentNewDefault() {
        // TODO
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
        final String jsonViewContent;
        try {
            VC vc = getViewContentFromNode();
            jsonViewContent = ((ByteArrayOutputStream)vc.saveTo()).toString("UTF-8");
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


        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                shell.dispose();
            }
        });

        browser.addProgressListener(new ProgressListener() {

            @Override
            public void completed(final ProgressEvent event) {
                browser.evaluate(wrapInTryCatch(initJSView(jsonViewContent)));
            }



            @Override
            public void changed(final ProgressEvent event) {
                // do nothing
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
        pageBuilder.append("<div id=\"" + CONTAINER_ID + "\" class=\"container\">");
        // content
        pageBuilder.append(e.getMessage());
        // content end
        pageBuilder.append("</div>");
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
        String debugScript = "<script type=\"text/javascript\" "
                + "src=\"https://getfirebug.com/firebug-lite.js#startOpened=true\"></script>";
        String scriptString = "<script type=\"text/javascript\" src=\"%s\"></script>";
        String cssString = "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\">";

        StringBuilder pageBuilder = new StringBuilder();
        pageBuilder.append("<!doctype html><html><head>");
        pageBuilder.append(setIEVersion);
        pageBuilder.append(debugScript);

        for (WebResourceLocator resFile : getResourceFileList()) {
            switch (resFile.getType()) {
                case CSS:
                    pageBuilder.append(String.format(cssString, resFile.getResource().getName()));
                    break;
                case JAVASCRIPT :
                    pageBuilder.append(String.format(scriptString, resFile.getResource().getName()));
                    break;
                default:
                    throw new IOException("Unrecognized resource type " + resFile.getType());
            }
        }
        pageBuilder.append("</head><body>");
        pageBuilder.append("<div id=\"view\" class=\"container\"></div>");
        pageBuilder.append("</body></html>");
        return pageBuilder.toString();
    }

    /**
     * @param scriptString
     * @param pageBuilder
     */
    private ArrayList<WebResourceLocator> getResourceFileList() {
        ArrayList<WebResourceLocator> resourceFiles = new ArrayList<WebResourceLocator>();
        if (m_template.getWebResources() != null) {
            for (WebResourceLocator resFile : m_template.getWebResources()) {
                resourceFiles.add(resFile);
            }
        }
        if (m_template.getDependencies() != null) {
            for (WebDependency dependency : m_template.getDependencies()) {
                if (dependency.getResourceLocators() != null) {
                    for (WebResourceLocator resFile : dependency.getResourceLocators()) {
                        resourceFiles.add(resFile);
                    }
                }
            }
        }
        return resourceFiles;
    }

    private void copyWebResources() throws IOException {
        for (WebResourceLocator resFile : getResourceFileList()) {
            FileUtils.copyFileToDirectory(resFile.getResource(), m_tempFolder);
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
        if (m_tempFolder.exists()) {
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

    private String initJSView(final String jsonViewContent) {
        String initMethod = m_template.getInitMethodName();
        return getJSMethodName(initMethod, jsonViewContent, CONTAINER_ID);
    }

    private String getJSMethodName(final String method, final String... parameters) {
        String namespace = m_template.getNamespace();
        StringBuilder methodCallBuilder = new StringBuilder();

        if (namespace != null && !namespace.isEmpty()) {
            methodCallBuilder.append(namespace);
            methodCallBuilder.append(".");
        }
        methodCallBuilder.append(method);

        methodCallBuilder.append("(");
        if (parameters != null && parameters.length > 0) {
            for (String parameter : parameters) {
                methodCallBuilder.append("'");
                methodCallBuilder.append(parameter);
                methodCallBuilder.append("', ");
            }
            methodCallBuilder.delete(methodCallBuilder.length() - 2, methodCallBuilder.length());
        }
        methodCallBuilder.append(")");

        return methodCallBuilder.toString();
    }

    /**
     * @param jsonViewContent
     * @return
     */
    private String wrapInTryCatch(final String jsCode) {
        StringBuilder builder = new StringBuilder();
        builder.append("try {");
        builder.append(jsCode);
        builder.append("} catch(err) {}");
        return builder.toString();
    }


    private void applyTriggered(final Browser browser) {
        String pullMethod = m_template.getPullViewContentMethodName();
        String evalCode = wrapInTryCatch("return " + getJSMethodName(pullMethod, CONTAINER_ID));
        String jsonString = (String)browser.evaluate(evalCode);
        try {
            VC viewContent = m_viewContentClass.newInstance();
            viewContent.createFrom(new ByteArrayInputStream(jsonString.getBytes(Charset.forName("UTF-8"))));
            loadViewContentIntoNode(viewContent);
        } catch (Exception e) {
            //TODO error message
        }
    }
}
