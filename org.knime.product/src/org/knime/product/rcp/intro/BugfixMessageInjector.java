/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
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
class BugfixMessageInjector extends AbstractInjector {
    private List<String> m_bugfixes;

    protected BugfixMessageInjector(final File templateFile, final ReentrantLock introFileLock,
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
        m_bugfixes = checkNewMinorVersion();
    }

    private List<String> checkNewMinorVersion() throws IOException, URISyntaxException {
        final ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
        RepositoryTracker tracker = provUI.getRepositoryTracker();
        if (tracker == null) {
            // if run from the IDE there will be no tracker
            return Collections.emptyList();
        }

        UpdateOperation op = new UpdateOperation(provUI.getSession());
        op.resolveModal(new NullProgressMonitor());
        return Stream.of(op.getPossibleUpdates()).map(u -> u.toUpdate.getProperty("org.eclipse.equinox.p2.name"))
                .sorted().distinct().collect(Collectors.toList());
    }

    private void injectUpdateMessage(final Document doc) throws ParserConfigurationException, SAXException,
        IOException, XPathExpressionException, TransformerException {
        XPath xpath = m_xpathFactory.newXPath();
        Element updateNode =
            (Element)xpath.evaluate("//div[@id='update-inner']", doc.getDocumentElement(), XPathConstants.NODE);

        Element minorUpdatesAvailableSpan =
            (Element)xpath.evaluate("//span[@id='bugfixes-available']", updateNode, XPathConstants.NODE);
        minorUpdatesAvailableSpan.removeAttribute("style"); // removes the "hidden" style and makes it visible

        Element updateList = (Element)xpath.evaluate(".//ul[@id='bugfixes-list']", updateNode, XPathConstants.NODE);
        updateList.removeAttribute("style"); // removes the "hidden" style and makes it visible

        for (int i = 0; i < Math.min(5, m_bugfixes.size()); i++) {
            Element li = doc.createElement("li");
            li.setTextContent(m_bugfixes.get(i));
            updateList.appendChild(li);
        }
        if (m_bugfixes.size() > 5) {
            Element li = doc.createElement("li");
            li.setTextContent("... and " + (m_bugfixes.size() - 5) + " more");
            updateList.appendChild(li);
        }

        Element updatePossibleNode =
            (Element)xpath.evaluate(".//span[@id='install-bugfixes']", updateNode, XPathConstants.NODE);
        updatePossibleNode.removeAttribute("style");

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
        if (!m_bugfixes.isEmpty()) {
            injectUpdateMessage(doc);
        }
    }
}
