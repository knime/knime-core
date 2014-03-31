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
 * History
 *   28.03.2014 (thor): created
 */
package org.knime.product.rcp.intro;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.workbench.ui.p2.actions.UpdateChecker;
import org.knime.workbench.ui.p2.actions.UpdateChecker.UpdateInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Runnable that injects a message about a new KNIME version into the intro page. This is run parallel to the startup
 * process. If the KNIME server is not reachable it will just not replace the already existing copy of the template and
 * nothing will be shown.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class UpdateMessageInjector implements Runnable {
    private final File m_templateFile;

    private final DocumentBuilderFactory m_parserFactory;

    private final XPathFactory m_xpathFactory;

    private final TransformerFactory m_transformerFactory;

    private final ReentrantLock m_introFileLock;

    UpdateMessageInjector(final File templateFile, final ReentrantLock introFileLock,
        final DocumentBuilderFactory parserFactory, final XPathFactory xpathFactory,
        final TransformerFactory transformerFactory) {
        m_templateFile = templateFile;
        m_introFileLock = introFileLock;
        m_parserFactory = parserFactory;
        m_xpathFactory = xpathFactory;
        m_transformerFactory = transformerFactory;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            List<UpdateInfo> newVersions = checkNewVersion();
            if (!newVersions.isEmpty()) {
                m_introFileLock.lock();
                try {
                    injectUpdateMessage(newVersions);
                } finally {
                    m_introFileLock.unlock();
                }
            }
        } catch (IOException | XPathExpressionException | ParserConfigurationException | SAXException
                | TransformerException | URISyntaxException ex) {
            NodeLogger.getLogger(getClass()).warn("Could not check for new KNIME version: " + ex.getMessage(), ex);
        }
    }

    private List<UpdateInfo> checkNewVersion() throws IOException, URISyntaxException {
        // TODO remove these three lines
        if (true) {
            return Collections.singletonList(new UpdateInfo(new URI("http://www.knime.org/update/2.10"), "KNIME 2.10",
                false));
        }

        final ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
        RepositoryTracker tracker = provUI.getRepositoryTracker();
        if (tracker == null) {
            // if run from the IDE there will be no tracker
            return Collections.emptyList();
        }

        List<UpdateInfo> updateList = new ArrayList<>();
        for (URI uri : tracker.getKnownRepositories(provUI.getSession())) {
            if (("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                && uri.getHost().endsWith(".knime.org")) {
                UpdateInfo newRelease = UpdateChecker.checkForNewRelease(uri);
                if (newRelease != null) {
                    updateList.add(newRelease);
                }
            }
        }
        return updateList;
    }

    private void injectUpdateMessage(final List<UpdateInfo> updateInfos) throws ParserConfigurationException,
        SAXException, IOException, XPathExpressionException, TransformerException {
        DocumentBuilder parser = m_parserFactory.newDocumentBuilder();
        Document doc = parser.parse(m_templateFile);

        XPath xpath = m_xpathFactory.newXPath();
        Element updateNode =
            (Element)xpath.evaluate("//div[@id='update']", doc.getDocumentElement(), XPathConstants.NODE);
        updateNode.removeAttribute("style");

        Element updateList =
            (Element)xpath.evaluate(".//ul[@id='update-list']", updateNode, XPathConstants.NODE);
        boolean updatePossible = true;
        for (UpdateInfo ui : updateInfos) {
            updatePossible &= ui.isUpdatePossible();

            Element li = doc.createElement("li");
            li.setTextContent(ui.getName());
            updateList.appendChild(li);
        }


        if (updatePossible) {
            Element updatePossibleNode =
                (Element)xpath.evaluate(".//span[@id='update-possible']", updateNode, XPathConstants.NODE);
            updatePossibleNode.removeAttribute("style");
        } else {
            Element updateNotPossibleNode =
                (Element)xpath.evaluate(".//span[@id='update-not-possible']", updateNode, XPathConstants.NODE);
            updateNotPossibleNode.removeAttribute("style");
        }

        File temp = FileUtil.createTempFile("intro", ".html", true);
        Transformer serializer = m_transformerFactory.newTransformer();
        serializer.setOutputProperty(OutputKeys.METHOD, "xhtml");
        serializer.transform(new DOMSource(doc), new StreamResult(temp));
        Files.move(temp.toPath(), m_templateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
