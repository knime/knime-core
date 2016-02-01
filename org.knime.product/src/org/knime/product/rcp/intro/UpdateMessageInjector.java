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
 *   28.03.2014 (thor): created
 */
package org.knime.product.rcp.intro;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.knime.core.eclipseUtil.UpdateChecker;
import org.knime.core.eclipseUtil.UpdateChecker.UpdateInfo;
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
class UpdateMessageInjector extends AbstractInjector {
    private List<UpdateInfo> m_newVersions;

    protected UpdateMessageInjector(final File templateFile, final ReentrantLock introFileLock,
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
    protected void prepareData() throws Exception {
        m_newVersions = checkNewVersion();
    }

    private List<UpdateInfo> checkNewVersion() throws IOException, URISyntaxException {
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

    private void injectNoUpdateMessage(final Document doc) throws ParserConfigurationException, SAXException,
        IOException, XPathExpressionException, TransformerException {
        XPath xpath = m_xpathFactory.newXPath();
        Element noUpdatesSpan =
            (Element)xpath.evaluate("//span[@id='no-updates']", doc.getDocumentElement(), XPathConstants.NODE);
        noUpdatesSpan.removeAttribute("style"); // removes the "hidden" style

        Element checkingUpdatesSpan =
            (Element)xpath.evaluate("//span[@id='checking-updates']", doc.getDocumentElement(), XPathConstants.NODE);
        checkingUpdatesSpan.setAttribute("style", "display: none;");

        Element updateDiv =
            (Element)xpath.evaluate("//div[@id='update']", doc.getDocumentElement(), XPathConstants.NODE);
        updateDiv.setAttribute("style", "display: none;");
    }

    private void injectUpdateMessage(final Document doc) throws ParserConfigurationException, SAXException,
        IOException, XPathExpressionException, TransformerException {
        XPath xpath = m_xpathFactory.newXPath();
        Element updateNode =
            (Element)xpath.evaluate("//div[@id='update']", doc.getDocumentElement(), XPathConstants.NODE);

        Element updatesAvailableSpan =
            (Element)xpath.evaluate("//span[@id='updates-available']", updateNode, XPathConstants.NODE);
        updatesAvailableSpan.removeAttribute("style"); // removes the "hidden" style and makes it visible

        Element checkingUpdatesSpan =
            (Element)xpath.evaluate("//span[@id='checking-updates']", doc.getDocumentElement(), XPathConstants.NODE);
        checkingUpdatesSpan.setAttribute("style", "display: none;");

        Element updateList = (Element)xpath.evaluate(".//ul[@id='update-list']", updateNode, XPathConstants.NODE);
        updateList.removeAttribute("style"); // removes the "hidden" style and makes it visible

        boolean updatePossible = true;
        for (UpdateInfo ui : m_newVersions) {
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

        if (m_prefs.getBoolean("org.knime.product.intro.update", true)) {
            updateNode.removeAttribute("style"); // removes the "hidden" style
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void injectData(final Document doc, final XPath xpath) throws XPathExpressionException,
        ParserConfigurationException, SAXException, IOException, TransformerException {
        if (!m_newVersions.isEmpty()) {
            injectUpdateMessage(doc);
        } else {
            injectNoUpdateMessage(doc);
        }
    }
}
