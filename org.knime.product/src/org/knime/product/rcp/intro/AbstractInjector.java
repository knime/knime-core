/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   23.06.2014 (thor): created
 */
package org.knime.product.rcp.intro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.browser.BrowserViewer;
import org.eclipse.ui.internal.browser.WebBrowserEditor;
import org.eclipse.ui.internal.browser.WebBrowserEditorInput;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Abstract base class for all injectors that modify the intro page.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
abstract class AbstractInjector implements Runnable {
    private final File m_templateFile;

    private final DocumentBuilderFactory m_parserFactory;

    protected final XPathFactory m_xpathFactory;

    protected final TransformerFactory m_transformerFactory;

    private final ReentrantLock m_introFileLock;

    protected final IEclipsePreferences m_prefs;

    protected final boolean m_isFreshWorkspace;

    /**
     * Creates a new injector.
     *
     * @param templateFile the template file in the temporary directory
     * @param introFileLock lock for the intro file
     * @param preferences the intro page preferences
     * @param isFreshWorkspace <code>true</code> if we are starting in a fresh workspace, <code>false</code> otherwise
     * @param parserFactory a parser factory that will be re-used
     * @param xpathFactory an XPath factory that will be re-used
     * @param transformerFactory a transformer factory that will be re-used
     */
    protected AbstractInjector(final File templateFile, final ReentrantLock introFileLock,
        final IEclipsePreferences preferences, final boolean isFreshWorkspace,
        final DocumentBuilderFactory parserFactory, final XPathFactory xpathFactory,
        final TransformerFactory transformerFactory) {
        m_templateFile = templateFile;
        m_introFileLock = introFileLock;
        m_prefs = preferences;
        m_isFreshWorkspace = isFreshWorkspace;
        m_parserFactory = parserFactory;
        m_xpathFactory = xpathFactory;
        m_transformerFactory = transformerFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void run() {
        try {
            prepareData();
            m_introFileLock.lock();
            try {
                DocumentBuilder parser = m_parserFactory.newDocumentBuilder();
                parser.setEntityResolver(EmptyDoctypeResolver.INSTANCE);
                Document doc = parser.parse(m_templateFile);
                XPath xpath = m_xpathFactory.newXPath();
                injectData(doc, xpath);
                processIntroProperties(doc, xpath);
                writeFile(doc);
                refreshIntroEditor();
            } finally {
                m_introFileLock.unlock();
            }
        } catch (Exception ex) {
            NodeLogger.getLogger(getClass()).warn("Could not modify intro page: " + ex.getMessage(), ex);
        }
    }

    private void refreshIntroEditor() {
        final Browser browser = findIntroPageBrowser(m_templateFile);
        if (browser != null) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    browser.refresh();
                }
            });
        }
    }

    /**
     * Method that is called before the intro page is locked and read. Subclasses may retrieve remote information to
     * perform other longer-runnning tasks for acquiring information to be injected into the page. The default
     * implementation does nothing.
     *
     * @throws Exception if an error occurs
     */
    protected void prepareData() throws Exception {

    }

    /**
     * Modifies the given intro page document and injects data.
     *
     * @param doc document for the intro page
     * @param xpath an XPath object that can be used to evaluate XPath expressions
     * @throws Exception if an error occurs
     */
    protected abstract void injectData(Document doc, XPath xpath) throws Exception;

    /**
     * Iterates over all checkbox elements in the intro page and looks up the corresponding preference keys. The div
     * corresponding to the checkboxes/preference keys are then either show or hidden.
     *
     * @param xpath an XPath object for reuse
     * @param doc the document
     */
    private void processIntroProperties(final Document doc, final XPath xpath) throws XPathExpressionException {
        NodeList checkBoxes =
            (NodeList)xpath.evaluate("//div[@id='properties']//input[@type='checkbox']", doc.getDocumentElement(),
                XPathConstants.NODESET);
        for (int i = 0; i < checkBoxes.getLength(); i++) {
            Element cb = (Element)checkBoxes.item(i);
            String name = cb.getAttribute("name");
            String key = "org.knime.product.intro." + name;

            Element div =
                (Element)xpath.evaluate("//div[@id='" + name + "']", doc.getDocumentElement(), XPathConstants.NODE);
            if (m_prefs.getBoolean(key, true)) {
                cb.setAttribute("checked", "checked");
                div.removeAttribute("style");
            } else {
                cb.removeAttribute("checked");
                div.setAttribute("style", "display: none");
            }
        }
    }

    private void writeFile(final Document doc) throws IOException, TransformerException {
        File temp = FileUtil.createTempFile("intro", ".html", true);

        try (OutputStream out = new FileOutputStream(temp)) {
            Transformer serializer = m_transformerFactory.newTransformer();
            serializer.setOutputProperty(OutputKeys.METHOD, "xhtml");
            serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "about:legacy-compat");
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            serializer.transform(new DOMSource(doc), new StreamResult(out));
        }
        Files.move(temp.toPath(), m_templateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Looks for the open intro page editor (and HTML editor) and returns the Browser instance. This (unfortunately)
     * involves some heavy reflection stuff as there is no other way to attach a listener otherwise. If the intro page
     * editor cannot be found then <code>null</code> is returned.
     *
     * @param introPageFile the temporary intro page file
     * @return the browser instance showing the intro page or <code>null</code>
     */
    @SuppressWarnings("restriction")
    static Browser findIntroPageBrowser(final File introPageFile) {
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference ref : page.getEditorReferences()) {
                    try {
                        if (isIntroPageEditor(ref, introPageFile)) {
                            IEditorPart part = ref.getEditor(false);
                            if (part instanceof WebBrowserEditor) {
                                WebBrowserEditor editor = (WebBrowserEditor)part;

                                Field webBrowser = editor.getClass().getDeclaredField("webBrowser");
                                webBrowser.setAccessible(true);
                                BrowserViewer viewer = (BrowserViewer)webBrowser.get(editor);

                                Field browserField = viewer.getClass().getDeclaredField("browser");
                                browserField.setAccessible(true);
                                return (Browser)browserField.get(viewer);
                            }
                        }
                    } catch (PartInitException ex) {
                        NodeLogger.getLogger(AbstractInjector.class).error(
                            "Could not open web browser with intro page: " + ex.getMessage(), ex);
                    } catch (SecurityException | NoSuchFieldException | IllegalArgumentException
                            | IllegalAccessException ex) {
                        NodeLogger.getLogger(AbstractInjector.class).error(
                            "Could not attach location listener to web browser: " + ex.getMessage(), ex);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns whether the given editor is an intro editor. This is checked by looking the URL the editor displays.
     *
     * @param ref an editor reference
     * @param introPageFile the temporary intro page file
     * @return <code>true</code> if it is an intro page editor, <code>false</code> otherwise
     * @throws PartInitException if there was an error restoring the editor input
     */
    @SuppressWarnings("restriction")
    static boolean isIntroPageEditor(final IEditorReference ref, final File introPageFile) throws PartInitException {
        if (introPageFile == null) {
            return false;
        }

        try {
            IEditorInput input = ref.getEditorInput();
            return (input instanceof WebBrowserEditorInput)
                && ((WebBrowserEditorInput)input).getURL().getPath()
                    .endsWith(introPageFile.getAbsolutePath().replace("\\", "/"));
        } catch (AssertionFailedException ex) {
            // may happen if the editor "ref" points to a resource that doesn't exist any more
            NodeLogger
                .getLogger(AbstractInjector.class)
                .error(
                    "Could not get editor input, probably the resource was removed outside Eclipse: " + ex.getMessage(),
                    ex);
            return false;
        }
    }
}
