/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   22.08.2011 (meinl): created
 */
package org.knime.product.rcp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.ccil.cowan.tagsoup.Parser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;
import org.eclipse.ui.intro.config.IIntroXHTMLContentProvider;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Provider for the intro page that adds dynamic content (news, tips&amp;tricks,
 * ...) to the static page.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TipsAndTrickProvider implements IIntroXHTMLContentProvider {
    static final URL TIPS_AND_TRICKS_URL;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(TipsAndTrickProvider.class);

    private Element m_tipsAndTricks;

    static {
        URL url = null;
        try {
            url = new URL("http://tech.knime.org/tips-and-tricks");
        } catch (MalformedURLException ex) {
            // does not happen
        }
        TIPS_AND_TRICKS_URL = url;
    }


    /**
     *
     */
    public TipsAndTrickProvider() {
        try {
            HttpURLConnection conn =
                (HttpURLConnection)TIPS_AND_TRICKS_URL.openConnection();
            conn.setConnectTimeout(500);
            conn.connect();
            extractOnlineTips(new BufferedInputStream(conn.getInputStream()));
            conn.disconnect();
        } catch (IOException ex) {
            // timeout, unknown host, ...
            LOGGER.warn("Cannot connect to knime.org", ex);
        } catch (Exception ex) {
            LOGGER.error("Cannot get tips and tricks", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IIntroContentProviderSite site) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createContent(final String id, final PrintWriter out) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createContent(final String id, final Element parent) {
        Document doc = parent.getOwnerDocument();
        if (m_tipsAndTricks != null) {
            parent.appendChild(doc.importNode(m_tipsAndTricks, true));
        } else {
            parent.appendChild(getOfflineMessage(doc));
        }
    }

    private Element getOfflineMessage(final Document doc) {
        Element div = doc.createElement("div");
        div.setAttribute("id", "noConnection");
        div.setTextContent("The KNIME webserver cannot be reached. Maybe your internet connection is down. Therefore we cannot show you the latest tips and tricks.");
        return div;
    }

    private void extractOnlineTips(final InputStream in)
            throws TransformerFactoryConfigurationError, TransformerException,
            SAXException {
        XMLReader reader = new Parser();
        reader.setFeature(Parser.namespacesFeature, false);
        reader.setFeature(Parser.namespacePrefixesFeature, false);
        Transformer transformer =
                TransformerFactory.newInstance().newTransformer();
        DOMResult result = new DOMResult();
        transformer.transform(new SAXSource(reader, new InputSource(in)),
                result);

        Document doc = (Document)result.getNode();

        // extract the right div
        NodeList nl = doc.getDocumentElement().getElementsByTagName("div");
        for (int i = 0; i < nl.getLength(); i++) {
            Element div = (Element)nl.item(i);
            if ("colWrapper".equals(div.getAttribute("id"))) {
                m_tipsAndTricks = div;
                break;
            }
        }
        if (m_tipsAndTricks == null) {
            return;
        }

        // fix relative links
        String linkBase =
                TIPS_AND_TRICKS_URL.getProtocol() + "://"
                        + TIPS_AND_TRICKS_URL.getHost();
        nl = m_tipsAndTricks.getElementsByTagName("a");
        for (int i = 0; i < nl.getLength(); i++) {
            Element a = (Element)nl.item(i);
            String href = a.getAttribute("href");
            if (!href.startsWith("http")) {
                a.setAttribute("href", linkBase + "/" + href);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createContent(final String id, final Composite parent,
            final FormToolkit toolkit) {
    }

}
