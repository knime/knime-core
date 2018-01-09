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
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.knime.workbench.editor2.WorkflowEditor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Prepares the template and injects the MRU list. This injector must be the first injector that is executed!
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class MRUInjector extends AbstractInjector {
    protected MRUInjector(final File templateFile, final ReentrantLock introFileLock,
        final IEclipsePreferences preferences, final boolean isFreshWorkspace,
        final DocumentBuilderFactory parserFactory, final XPathFactory xpathFactory,
        final TransformerFactory transformerFactory) {
        super(templateFile, introFileLock, preferences, isFreshWorkspace, parserFactory, xpathFactory,
            transformerFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void injectData(final Document doc, final XPath xpath) throws Exception {
        Bundle myBundle = FrameworkUtil.getBundle(getClass());
        URL cssBaseUrl = FileLocator.toFileURL(myBundle.getEntry("/intro/css"));

        Element base = (Element)xpath.evaluate("/html/head/base", doc.getDocumentElement(), XPathConstants.NODE);
        base.setAttribute("href", cssBaseUrl.toExternalForm());

        Element mruList =
            (Element)xpath.evaluate("/html/body//ul[@id='mruList']", doc.getDocumentElement(), XPathConstants.NODE);
        insertMRUList(mruList);

        String quickstartPath = Platform.getInstallLocation().getURL().getPath() + "quickstart.pdf";
        NodeList nl = (NodeList)xpath.evaluate("//a[@class='quickstart-guide']", doc.getDocumentElement(),
            XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) {
            Element link = (Element)nl.item(i);
            link.setAttribute("href", "file:" + quickstartPath);
        }
    }

    /**
     * Inserts the most recently used workflows in the MRU list on the intro page. It will insert &lt;li> elements.
     *
     * @param mruList the element for the mru list (an &lt;ul>)
     */
    private void insertMRUList(final Element mruList)
        throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        if (m_isFreshWorkspace) {
            return; // no workflows used in a fresh workspace
        }

        // if it's not a fresh workspace, the workbench.xml file exists (already checked in IntroPage constructor)
        Path path = IntroPage.getWorkbenchStateFile();
        DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = parser.parse(path.toFile());

        XPath xpath = m_xpathFactory.newXPath();
        String mruMemento =
            (String)xpath.evaluate("persistedState[@key = 'memento']/@value", doc.getDocumentElement(), XPathConstants.STRING);

        doc = parser.parse(new InputSource(new StringReader(mruMemento)));

        NodeList workflowList =
            (NodeList)xpath.evaluate("//mruList/file[@id = '" + WorkflowEditor.ID + "']/persistable",
                doc.getDocumentElement(), XPathConstants.NODESET);

        if (workflowList.getLength() == 0) {
            Element mru = (Element)xpath.evaluate("//div[@id='mru']", mruList.getOwnerDocument().getDocumentElement(),
                XPathConstants.NODE);
            mru.setAttribute("style", "display: none");
        } else {
            for (int i = 0; i < workflowList.getLength(); i++) {
                Element e = (Element)workflowList.item(i);
                String uri = e.getAttribute("uri"); // knime://MP/.../WorkflowName/workflow.knime
                String[] parts = uri.split("/");
                if (parts.length > 2) {
                    String workflowName = parts[parts.length - 2];
                    workflowName = URLDecoder.decode(workflowName, "UTF-8");

                    Element li = mruList.getOwnerDocument().createElement("li");
                    mruList.appendChild(li);

                    Element a = mruList.getOwnerDocument().createElement("a");
                    a.setAttribute("href", "intro://openWorkflow/" + uri);
                    a.setAttribute("title", URLDecoder.decode(uri, "UTF-8").replaceAll("/workflow\\.knime$", ""));
                    a.setTextContent(workflowName);
                    li.appendChild(a);
                }
            }
        }
    }
}
