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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
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
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
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
import org.knime.core.util.FileUtil;

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
public final class WizardNodeView<T extends NodeModel & WizardNode<REP, VAL>, REP extends WebViewContent, VAL extends WebViewContent>
    extends AbstractNodeView<T> implements InteractiveView<T, REP, VAL> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WizardNodeView.class);
    private static File tempFolder;

    private final InteractiveViewDelegate<VAL> m_delegate;
    private Shell m_shell;
    private Browser m_browser;
    private WebTemplate m_template;
    private File m_tempIndexFile;
    private String m_title;
    private ProgressListener m_completedListener;

    /**
     * @param nodeModel the underlying model
     * @since 2.10
     */
    public WizardNodeView(final T nodeModel) {
        super(nodeModel);
        m_template = WizardExecutionController.getWebTemplateFromJSObjectID(getNodeModel().getJavascriptObjectID());
        m_delegate = new InteractiveViewDelegate<VAL>();
    }

    private static boolean isDebug() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        if (runtimeBean != null && runtimeBean.getInputArguments() != null) {
            String inputArguments = runtimeBean.getInputArguments().toString();
            if (inputArguments != null && !inputArguments.isEmpty()) {
                return inputArguments.indexOf("jdwp") >= 0;
            }
        }
        return false;
    }

    @Override
    public void setWorkflowManagerAndNodeID(final WorkflowManager wfm, final NodeID id) {
        m_delegate.setWorkflowManagerAndNodeID(wfm, id);
    }

    @Override
    public boolean canReExecute() {
        return m_delegate.canReExecute();
    }

    /**
     * @since 2.10
     */
    @Override
    public void triggerReExecution(final VAL value, final boolean useAsNewDefault, final ReexecutionCallback callback) {
        m_delegate.triggerReExecution(value, useAsNewDefault, callback);
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
                        final String jsonViewRepresentation = getViewRepresentationFromModel();
                        final String jsonViewValue = getViewValueFromModel();
                        try {
                            /*if (m_completedListener != null) {
                                m_browser.removeProgressListener(m_completedListener);
                            }
                            m_completedListener = new ProgressAdapter() {
                                @Override
                                public void completed(final ProgressEvent event) {
                                    m_browser
                                        .evaluate(wrapInTryCatch(initJSView(jsonViewRepresentation, jsonViewValue)));
                                }
                            };
                            m_browser.addProgressListener(m_completedListener);*/
                            m_browser.setUrl(createWebResources(jsonViewRepresentation, jsonViewValue));
                            //org.eclipse.swt.program.Program.launch(createWebResources(jsonViewRepresentation, jsonViewValue));
                        } catch (IOException e) {
                            if (m_tempIndexFile != null) {
                                deleteTempFile(m_tempIndexFile);
                            }
                            m_browser.setText(createErrorHTML(e));
                        }
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
        final String jsonViewRepresentation = getViewRepresentationFromModel();
        final String jsonViewValue = getViewValueFromModel();

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

        /*m_completedListener = new ProgressAdapter() {
            @Override
            public void completed(final ProgressEvent event) {
                Display d = getDisplay();
                if (d != null) {
                    d.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            m_browser.evaluate(wrapInTryCatch(initJSView(jsonViewRepresentation, jsonViewValue)));
                        }
                    });
                }
            }
        };
        m_browser.addProgressListener(m_completedListener);*/

        try {
            m_browser.setUrl(createWebResources(jsonViewRepresentation, jsonViewValue));
            //org.eclipse.swt.program.Program.launch(createWebResources(jsonViewRepresentation, jsonViewValue));
        } catch (IOException e) {
            if (m_tempIndexFile != null) {
                deleteTempFile(m_tempIndexFile);
            }
            m_browser.setText(createErrorHTML(e));
        }

        m_shell.setSize(800, 600);
        m_shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                callCloseView();
            }
        });
        m_shell.open();
    }

    private String getViewRepresentationFromModel() {
        try {
            REP rep = getNodeModel().getViewRepresentation();
            if (rep != null) {
                return ((ByteArrayOutputStream)rep.saveToStream()).toString("UTF-8");
            } else {
                return "null";
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("No view representation available!");
        }

    }

    private String getViewValueFromModel() {
        try {
            VAL val = getNodeModel().getViewValue();
            if (val != null) {
                return ((ByteArrayOutputStream)val.saveToStream()).toString("UTF-8");
            } else {
                return "null";
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("No view value available!");
        }
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
    private String createWebResources(final String jsonViewRepresentation, final String jsonViewValue) throws IOException {
        if (!viewTempDirExists()) {
            tempFolder = FileUtil.createTempDir("knimeViewContainer", null, true);
            try {
                copyWebResources();
            } catch (IOException e) {
                deleteTempFile(tempFolder);
                tempFolder = null;
                throw e;
            }
        }
        m_tempIndexFile = new File(tempFolder, "index_" + System.currentTimeMillis() + ".html");
        m_tempIndexFile.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(m_tempIndexFile));
        writer.write(buildHTMLResource(jsonViewRepresentation, jsonViewValue));
        writer.flush();
        writer.close();
        return m_tempIndexFile.getAbsolutePath();
    }

    /**
     * @return
     */
    private boolean viewTempDirExists() {
        return !isDebug()
               && tempFolder != null
               && tempFolder.exists()
               && tempFolder.isDirectory();
    }

    private String buildHTMLResource(final String jsonViewRepresentation, final String jsonViewValue) throws IOException {

        String setIEVersion = "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">";
        String inlineScript = "<script type=\"text/javascript\" charset=\"UTF-8\">%s</script>";
        //String debugScript = "<script type=\"text/javascript\" "
        //        + "src=\"https://getfirebug.com/firebug-lite.js#startOpened=true\"></script>";
        String scriptString = "<script type=\"text/javascript\" src=\"%s\" charset=\"UTF-8\"></script>";
        String cssString = "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\">";

        StringBuilder pageBuilder = new StringBuilder();
        pageBuilder.append("<!doctype html><html><head>");
        pageBuilder.append("<meta charset=\"UTF-8\">");
        pageBuilder.append(setIEVersion);
        //pageBuilder.append(String.format(inlineScript, "BASE_DIR = " + m_tempFolder));
        //pageBuilder.append(debugScript);

        String bodyText = "";
        if (m_template == null || m_template.getWebResources() == null || m_template.getWebResources().length < 1) {
            bodyText = "ERROR: No view implementation available!";
            LOGGER.error("No JavaScript view implementation available for view: " + m_title);
        }

        for (WebResourceLocator resFile : getResourceFileList()) {
            String path = resFile.getRelativePathTarget();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            switch (resFile.getType()) {
                case CSS:
                    pageBuilder.append(String.format(cssString, path));
                    break;
                case JAVASCRIPT:
                    pageBuilder.append(String.format(scriptString, path));
                    break;
                case FILE:
                    break;
                default:
                    LOGGER.error("Unrecognized resource type " + resFile.getType());
            }
        }
        String loadScript = "function loadWizardNodeView(){%s};";
        loadScript = String.format(loadScript, wrapInTryCatch(initJSView(jsonViewRepresentation, jsonViewValue)));
        pageBuilder.append(String.format(inlineScript, loadScript));
        pageBuilder.append("</head><body onload=\"loadWizardNodeView();\">");
        pageBuilder.append(bodyText);
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
            resourceFiles.addAll(Arrays.asList(m_template.getWebResources()));
        }

        return resourceFiles;
    }

    private void copyWebResources() throws IOException {
        for (Entry<File, String> copyEntry : getAllWebResources().entrySet()) {
            File src = copyEntry.getKey();
            File dest = new File(tempFolder, FilenameUtils.separatorsToSystem(copyEntry.getValue()));
            if (src.isDirectory()) {
                FileUtils.copyDirectory(src, dest);
            } else {
                FileUtils.copyFile(src, dest);
            }
        }
    }

    private static final String ID_WEB_RES = "org.knime.js.core.webResources";

    private static final String ELEM_BUNDLE = "webResourceBundle";

    private static final String ELEM_RES = "webResource";

    private static final String ATTR_BUNDLE_ID = "webResourceBundleID";

    private static final String ATTR_SOURCE = "relativePathSource";

    private static final String ATTR_TARGET = "relativePathTarget";

    private Map<File, String> getAllWebResources() throws IOException {
        Map<File, String> copyLocations = new HashMap<File, String>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(ID_WEB_RES);
        if (point == null) {
            throw new IllegalStateException("Invalid extension point : " + ID_WEB_RES);
        }
        IExtension[] webResExtensions = point.getExtensions();
        for (IExtension ext : webResExtensions) {

            // get plugin path
            File pluginFile = null;
            String pluginName = ext.getContributor().getName();
            URL pluginURL = FileLocator.find(Platform.getBundle(pluginName), new Path("/"), null);
            if (pluginURL != null) {
                try {
                    pluginFile = new File(FileLocator.resolve(pluginURL).toURI());
                } catch (URISyntaxException e) {
                    throw new IOException("Plugin path could not be resolved: " + pluginURL.toString());
                }
            }
            if (pluginFile == null) {
                throw new IOException("Plugin path could not be resolved: " + pluginName);
            }

            // get relative paths and collect in map
            IConfigurationElement[] bundleElements = ext.getConfigurationElements();
            for (IConfigurationElement bundleElem : bundleElements) {
                assert bundleElem.getName().equals(ELEM_BUNDLE);
                for (IConfigurationElement resElement : bundleElem.getChildren(ELEM_RES)) {
                    String relSource = resElement.getAttribute(ATTR_SOURCE);
                    File source = new File(pluginFile, relSource);
                    if (!source.exists()) {
                        LOGGER.errorWithFormat("CODING ERROR: Source file does not exist: %s for bundle %s",
                            source.getAbsolutePath(), bundleElem.getAttribute(ATTR_BUNDLE_ID));
                        continue;
                        //throw new IOException("Source file does not exist: " + source.getAbsolutePath());
                    }
                    String relTarget = resElement.getAttribute(ATTR_TARGET);
                    if (relTarget == null || relTarget.isEmpty()) {
                        relTarget = relSource;
                    }
                    copyLocations.put(source, relTarget);
                }
            }
        }
        return copyLocations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void callCloseView() {
        m_shell = null;
        m_browser = null;
        m_template = null;
        if (m_tempIndexFile != null && m_tempIndexFile.exists()) {
            deleteTempFile(m_tempIndexFile);
        }
    }

    private void deleteTempFile(final File tempFile) {
        if (tempFile.isDirectory()) {
            for (File file : tempFile.listFiles()) {
                if (file.isDirectory()) {
                    deleteTempFile(file);
                } else {
                    file.delete();
                }
            }
        }
        tempFile.delete();
    }

    private String initJSView(final String jsonViewRepresentation, final String jsonViewValue) {
        String escapedRepresentation = jsonViewRepresentation.replace("\\", "\\\\").replace("'", "\\'");
        String escapedValue = jsonViewValue.replace("\\", "\\\\").replace("'", "\\'");
        String repParseCall = /*"console.time('parse representation');" +*/ "var parsedRepresentation = JSON.parse('" + escapedRepresentation + "');"/* + "console.timeEnd('parse representation');"*/;
        String valParseCall = /*"console.time('parse value');" +*/ "var parsedValue = JSON.parse('" + escapedValue + "');"/* + "console.timeEnd('parse value');"*/;
        String initMethod = m_template.getInitMethodName();
        /*String initCall = getNamespacePrefix() + initMethod + "(JSON.parse('" + escapedRepresentation
                + "'), JSON.parse('" + escapedValue + "'));";*/
        //LOGGER.warn(initCall);
        String initCall = /*"console.time('init view');" +*/ getNamespacePrefix() + initMethod + "(parsedRepresentation, parsedValue);"/* + "console.timeEnd('init view');"*/;
        return repParseCall + valParseCall + initCall;
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

    private void applyTriggered(final Browser browser, final boolean useAsDefault) {
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
                triggerReExecution(viewValue, useAsDefault, new DefaultReexecutionCallback());
            } catch (Exception e) {
                //TODO error message
            }
        }
    }
}
