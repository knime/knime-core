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
 * History
 *   24.03.2014 (thor): created
 */
package org.knime.product.rcp.intro;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.browser.SystemBrowserInstance;
import org.eclipse.ui.internal.browser.WebBrowserEditor;
import org.eclipse.ui.internal.browser.WebBrowserUtil;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.NewWorkflowWizard;
import org.knime.workbench.explorer.view.actions.NewWorkflowWizardPage;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.p2.actions.InvokeInstallSiteAction;
import org.knime.workbench.ui.p2.actions.InvokeUpdateAction;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class for showing and handling events in the intro page.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
@SuppressWarnings("restriction")
public class IntroPage implements LocationListener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(IntroPage.class);

    private static final String BROWSER_ID = "org.knime.intro.page";

    /**
     * Singleton instance.
     */
    public static final IntroPage INSTANCE = new IntroPage();

    private boolean m_freshWorkspace;

    private boolean m_workbenchModified;

    private XPathFactory m_xpathFactory;

    private DocumentBuilderFactory m_parserFactory;

    private TransformerFactory m_transformerFactory;

    private File m_introFile;

    private final IEclipsePreferences m_prefs = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass())
        .getSymbolicName());

    private IntroPage() {
        IPath path = WorkbenchPlugin.getDefault().getDataLocation();
        if (path != null) {
            path = path.append("workbench.xml");
            File workbenchFile = path.toFile();
            // in fresh workspaces, workbench.xml does not exist yet
            m_freshWorkspace = !workbenchFile.exists();
        }

        try {
            // workaround for a bug in XPathFinderFactory on MacOS X
            final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            try {
                m_xpathFactory = XPathFactory.newInstance();
                m_parserFactory = DocumentBuilderFactory.newInstance();
                m_parserFactory.setValidating(false);
                m_transformerFactory = TransformerFactory.newInstance();
            } finally {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }

            ReentrantLock introFileLock = new ReentrantLock();
            m_introFile = copyTemplate("intro/intro.xhtml");
            new MRUInjector(m_introFile, introFileLock, m_prefs, m_freshWorkspace, m_parserFactory, m_xpathFactory,
                m_transformerFactory).run();
            KNIMEConstants.GLOBAL_THREAD_POOL.submit(new UpdateMessageInjector(m_introFile, introFileLock, m_prefs,
                m_freshWorkspace, m_parserFactory, m_xpathFactory, m_transformerFactory));
            KNIMEConstants.GLOBAL_THREAD_POOL.submit(new TipsAndNewsInjector(m_introFile, introFileLock, m_prefs,
                m_freshWorkspace, m_parserFactory, m_xpathFactory, m_transformerFactory));
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException
                | TransformerFactoryConfigurationError | TransformerException ex) {
            LOGGER.error("Could not copy intro pages: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            // should not happen
            LOGGER.info("Got interrupted while submitting injector: " + ex.getMessage(), ex);
        }
    }

    /**
     * Copies one of the template files into a temporary directory.
     *
     * @param templateFile the template file that should be copied
     * @return the modified temporary file
     */
    private File copyTemplate(final String templateFile) throws IOException, ParserConfigurationException,
        SAXException, XPathExpressionException, TransformerFactoryConfigurationError, TransformerException {
        File tempTemplate = FileUtil.createTempFile("intro", ".html", true);
        Bundle myBundle = FrameworkUtil.getBundle(getClass());
        URL introUrl = myBundle.getEntry(templateFile);
        try (InputStream is = introUrl.openStream()) {
            Files.copy(is, tempTemplate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return tempTemplate;
    }

    /**
     * Shows the intro page. If the workbench configuration has been modified to show the page (see
     * {@link #modifyWorkbenchState()}, then this method won't do anything as it assumes the intro page is already
     * shown. If you want to force to show it, then pass <code>true</code> as parameter.
     *
     * @param force <code>true</code> if the page should be shown in any case, <code>false</code> if it should only be
     *            shown if the workbench state has not been modified
     */
    public void show(final boolean force) {
        if ((m_introFile != null) && (force || !m_workbenchModified)) {
            try {
                IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(BROWSER_ID);
                if (browser instanceof SystemBrowserInstance) {
                    showMissingBrowserWarning(browser);
                } else {
                    browser.openURL(m_introFile.toURI().toURL());
                }
            } catch (PartInitException ex) {
                LOGGER.error("Could not open web browser with first intro page: " + ex.getMessage(), ex);
            } catch (IOException ex) {
                LOGGER.warn("Could not prepare first intro page: " + ex.getMessage(), ex);
            }
        }

        attachLocationListener();
    }

    private void showMissingBrowserWarning(final IWebBrowser browser) {
        IPersistentPreferenceStore prefStore =
            (IPersistentPreferenceStore)KNIMEUIPlugin.getDefault().getPreferenceStore();
        boolean omitWarnings = prefStore.getBoolean(PreferenceConstants.P_OMIT_MISSING_BROWSER_WARNING);
        if (!omitWarnings) {
            MessageDialogWithToggle dialog =
                MessageDialogWithToggle.openWarning(Display.getDefault().getActiveShell(),
                    "Missing browser integration",
                    "KNIME is unable to display web pages in an internal browser. This may be caused by missing "
                        + "system libraries. Please visit http://www.knime.org/faq#q6 for details.\n"
                        + "Some web pages may open in an external browser.", "Do not show again", false, prefStore,
                    PreferenceConstants.P_OMIT_MISSING_BROWSER_WARNING);
            if (dialog.getToggleState()) {
                prefStore.setValue(PreferenceConstants.P_OMIT_MISSING_BROWSER_WARNING, true);
                try {
                    prefStore.save();
                } catch (IOException ex) {
                    // too bad, ignore it
                }
            }
        }
    }

    /**
     * Looks for the open intro page editor (and HTML editor) any tries to attach a location listener. This
     * (unfortunately) involves some heavy reflection stuff as there is no other way to attach a listener otherwise.
     */
    private void attachLocationListener() {
        Browser browser = AbstractInjector.findIntroPageBrowser(m_introFile);
        if (browser != null) {
            browser.removeLocationListener(this);
            browser.addLocationListener(this);
        }
    }

    /**
     * Modified the workbench state file (workbench.xml) to show the intro page editor as top-most editor. This prevents
     * any previously open workflow to start loading immediately and delay KNIME startup.
     */
    public void modifyWorkbenchState() {
        if (m_freshWorkspace || (m_introFile == null)) {
            return; // we cannot modify the workbench state in this case
        } else if (!WebBrowserUtil.canUseInternalWebBrowser()) {
            return; // no internal browser available, opening an editor in this case will lead to errors
        }

        IPath path = WorkbenchPlugin.getDefault().getDataLocation();
        if (path != null) {
            path = path.append("workbench.xml");
            File workbenchFile = path.toFile();
            if (workbenchFile.canWrite()) {
                try {
                    DocumentBuilder parser = m_parserFactory.newDocumentBuilder();
                    Document doc = parser.parse(workbenchFile);

                    XPath xpath = m_xpathFactory.newXPath();

                    NodeList editorList =
                        (NodeList)xpath.evaluate("//page/editors/editor", doc.getDocumentElement(),
                            XPathConstants.NODESET);
                    if (editorList.getLength() > 0) {
                        resortEditorList(editorList);
                    }
                    editorList =
                        (NodeList)xpath.evaluate("//page/editors/editor", doc.getDocumentElement(),
                            XPathConstants.NODESET);

                    NodeList partList =
                        (NodeList)xpath.evaluate("//editors//part", doc.getDocumentElement(), XPathConstants.NODESET);
                    if (partList.getLength() > 0) {
                        Node parent = partList.item(0).getParentNode();
                        Element newPart = doc.createElement("part");
                        newPart.setAttribute("id", Integer.toString(editorList.getLength() - 1));
                        parent.insertBefore(newPart, parent.getFirstChild());
                    }

                    if (editorList.getLength() > 0) {
                        serializeWorkbenchState(doc, workbenchFile);
                    }
                    // if no editor was open, the intro page will be opened by #show; it's too complicated to re-build
                    // the editor list in the XML file and has no benefit
                } catch (IOException | SAXException | ParserConfigurationException
                        | TransformerFactoryConfigurationError | TransformerException | XPathExpressionException ex) {
                    LOGGER.error("Could not modify workbench state to show intro page: " + ex.getMessage(), ex);
                }
            }
        }
    }

    private void resortEditorList(final NodeList editorList) throws DOMException, MalformedURLException {
        // find last active workflow editor
        Deque<Node> newEditorList = new LinkedList<>();
        for (int i = 0; i < editorList.getLength(); i++) {
            Element editor = (Element)editorList.item(i);

            if (!removeStaleIntroEditor(editor)) {
                if ("true".equals(editor.getAttribute("activePart"))) {
                    newEditorList.addFirst(editor);
                } else {
                    newEditorList.add(editor);
                }

                // "disable" all other (workflow) editors
                editor.removeAttribute("activePart");
                editor.removeAttribute("focus");
            }
        }

        if (!newEditorList.isEmpty()) {
            // move active editor to front so that it gets activated when the intro page is closed
            Node parent = newEditorList.getFirst().getParentNode();
            for (Node n : newEditorList) {
                parent.removeChild(n);
                parent.appendChild(n);
            }

            parent.appendChild(createIntroEditorNode(parent.getOwnerDocument(), m_introFile.toURI().toURL()));
            m_workbenchModified = true;
        }
    }

    /**
     * Check for any entries from previous intro page editors and remove them (usually only happens if the workbench was
     * not shut down properly).
     *
     * @param editor a single editor element from the editors list
     * @return <code>true</code> if this was a stale editor that has been removed, <code>false</code> if it was any
     *         other editor
     */
    private boolean removeStaleIntroEditor(final Element editor) {
        NodeList inputList = editor.getElementsByTagName("input");
        if (inputList.getLength() > 0) {
            for (int j = 0; j < inputList.getLength(); j++) {
                Element input = (Element)inputList.item(j);
                if (BROWSER_ID.equals(input.getAttribute("id"))) {
                    editor.getParentNode().removeChild(editor);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Writes the modified workbench state back into the workbench.xml. We first write into a temp file and then move it
     * to the final destination so that we don't corrupt the file in case something goed wrong during serialization.
     *
     * @param doc the document
     * @param workbenchFile the workbench file
     */
    private void serializeWorkbenchState(final Document doc, final File workbenchFile)
        throws TransformerFactoryConfigurationError, TransformerException, IOException {
        File temp = FileUtil.createTempFile("workbench", ".xml", true);
        Transformer serializer = m_transformerFactory.newTransformer();

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(temp))) {
            serializer.transform(new DOMSource(doc), new StreamResult(os));
        }

        Files.move(temp.toPath(), workbenchFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Creates a new node for the intro page editor.
     *
     * @param doc the document in which the node will be inserted
     * @param url the URL to the temporary intro file
     * @return the new node
     */
    private static Node createIntroEditorNode(final Document doc, final URL url) {
        Element introPageEditor = doc.createElement("editor");
        introPageEditor.setAttribute("id", WebBrowserEditor.WEB_BROWSER_EDITOR_ID);
        introPageEditor.setAttribute("name", "KNIME Intro");
        introPageEditor.setAttribute("partName", "KNIME Intro");
        introPageEditor.setAttribute("title", "Welcome to KNIME");
        introPageEditor.setAttribute("tooltip", "Welcome to KNIME");
        introPageEditor.setAttribute("workbook", "DefaultEditorWorkbook");
        introPageEditor.setAttribute("activePart", "true");
        introPageEditor.setAttribute("focus", "true");

        Element introEditorInput = doc.createElement("input");
        introPageEditor.appendChild(introEditorInput);
        introEditorInput.setAttribute("factoryID", "org.eclipse.ui.browser.elementFactory");
        introEditorInput.setAttribute("url", url.toExternalForm());
        introEditorInput.setAttribute("id", BROWSER_ID);

        return introPageEditor;
    }

    /**
     * Returns whether we have a fresh or already used workspace.
     *
     * @return <code>true</code> if we started with a fresh workspace, <code>false</code> otherwise
     */
    public boolean isFreshWorkspace() {
        return m_freshWorkspace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changing(final LocationEvent event) {
        try {
            URI uri = new URI(event.location);
            if (uri.toString().endsWith(m_introFile.getAbsolutePath().replace("\\", "/"))) {
                return;
            }

            if ("intro".equals(uri.getScheme())) {
                handleIntroCommand(uri);
            } else {
                handleLink(uri);
            }
            event.doit = false;
        } catch (URISyntaxException ex) {
            LOGGER.error("Invalid URI '" + event.location + "': " + ex.getMessage(), ex);
        }
    }

    private void handleLink(final URI link) {
        try {
            URL url = link.toURL();
            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            support.getExternalBrowser().openURL(url);
        } catch (MalformedURLException ex) {
            LOGGER.error("Malformed URL '" + link.toString() + "': " + ex.getMessage(), ex);
        } catch (PartInitException ex) {
            LOGGER.error("Could not open external browser for '" + link.toString() + "': " + ex.getMessage(), ex);
        }

    }

    private void handleIntroCommand(final URI command) {
        switch (command.getHost().toLowerCase()) {
            case "openworkflow":
                String workflowUri = command.getPath();
                if (workflowUri.startsWith("/")) {
                    workflowUri = workflowUri.substring(1);
                }
                try {
                    URL u = new URL(workflowUri);
                    IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
                        new URI(u.getProtocol(), u.getHost(), u.getPath(), u.getQuery()), WorkflowEditor.ID, true);
                } catch (URISyntaxException | MalformedURLException ex) {
                    LOGGER.error("Invalid workflow URI '" + workflowUri + "': " + ex.getMessage(), ex);
                } catch (PartInitException ex) {
                    LOGGER.error("Could not open workflow '" + workflowUri + "': " + ex.getMessage(), ex);
                }
                break;
            case "invokeupdate":
                new InvokeUpdateAction().run();
                break;
            case "newworkflow":
                newWorkflow();
                break;
            case "installextensions":
                new InvokeInstallSiteAction().run();
                break;
            case "closeintro":
                closeIntro();
                break;
            case "browseexamples":
                browseExamples();
                break;
            case "setproperty":
                setIntroProperty(command);
                break;
            default:
                LOGGER.coding("Unknown intro command: " + command.getHost());
        }
    }

    private void setIntroProperty(final URI command) {
        for (NameValuePair param : URLEncodedUtils.parse(command, "UTF-8")) {
            m_prefs.putBoolean("org.knime.product.intro." + param.getName(), Boolean.parseBoolean(param.getValue()));
        }
    }

    private void browseExamples() {
        Bundle serverBundle = Platform.getBundle("com.knime.explorer.server");
        if (serverBundle != null) {
            Class<Action> clazz;
            try {
                clazz = (Class<Action>)serverBundle.loadClass("com.knime.explorer.server.ExampleServerLoginAction");
                Action action = clazz.newInstance();
                action.run();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                LOGGER.error("Could not browse example workflow: " + ex.getMessage(), ex);
            }
        }
    }

    private void newWorkflow() {
        ExplorerView explorerView = null;
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference ref : page.getViewReferences()) {
                    if (ExplorerView.ID.equals(ref.getId())) {
                        explorerView = (ExplorerView)ref.getPart(true);
                        break;
                    }
                }
            }
        }

        Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
        NewWorkflowWizard newWiz = new NewWorkflowWizard();
        newWiz.init(PlatformUI.getWorkbench(), null);

        WizardDialog dialog = new WizardDialog(shell, newWiz);
        dialog.create();
        dialog.getShell().setText("Create new workflow");
        dialog.getShell().setSize(Math.max(470, dialog.getShell().getSize().x), 350);
        int ok = dialog.open();
        if ((ok == Window.OK) && (explorerView != null)) {
            // update the tree
            IWizardPage currentPage = dialog.getCurrentPage();
            if (currentPage instanceof NewWorkflowWizardPage) {
                NewWorkflowWizardPage nwwp = (NewWorkflowWizardPage)currentPage;
                AbstractExplorerFileStore file = nwwp.getNewFile();
                Object p = ContentDelegator.getTreeObjectFor(file.getParent());
                explorerView.setNextSelection(file);
                explorerView.getViewer().refresh(p);
            }
        }
    }

    private void closeIntro() {
        for (IEditorReference ref : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
            .getEditorReferences()) {
            try {
                if (AbstractInjector.isIntroPageEditor(ref, m_introFile)) {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                        .closeEditor(ref.getEditor(false), false);
                }
            } catch (PartInitException ex) {
                LOGGER.error("Could not close intro page: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changed(final LocationEvent event) {
        // nothing to do here
    }
}
