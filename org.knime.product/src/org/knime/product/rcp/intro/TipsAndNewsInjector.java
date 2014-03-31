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
 *   26.03.2014 (thor): created
 */
package org.knime.product.rcp.intro;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ccil.cowan.tagsoup.Parser;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Runnable that injects news and tips&amp;tricks downloaded from the KNIME webpage into the intro page. This is run
 * parallel to the startup process. If the KNIME server is not reachable it will just not replace the already existing
 * copy of the template and nothing will be shown.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class TipsAndNewsInjector implements Runnable {
    private final URL m_tipsAndNewsUrl;

    private final File m_templateFile;

    private Node m_news;

    private Node m_tips;

    private final DocumentBuilderFactory m_parserFactory;

    private final XPathFactory m_xpathFactory;

    private final TransformerFactory m_transformerFactory;

    private final ReentrantLock m_introFileLock;

    /**
     * Creates a new injector.
     *
     * @param templateFile the template file in the temporary directory
     * @param parserFactory a parser factory that will be re-used
     * @param xpathFactory an XPath factory that will be re-used
     * @param transformerFactory a transformer factory that will be re-used
     */
    TipsAndNewsInjector(final File templateFile, final ReentrantLock introFileLock,
        final DocumentBuilderFactory parserFactory, final XPathFactory xpathFactory,
        final TransformerFactory transformerFactory) {
        m_templateFile = templateFile;
        m_introFileLock = introFileLock;
        m_parserFactory = parserFactory;
        m_xpathFactory = xpathFactory;
        m_transformerFactory = transformerFactory;

        URL tipsTricksUrl = null;
        try {
            tipsTricksUrl = new URL("http://www.knime.org/tips-and-tricks?knid=" + KNIMEConstants.getKNIMEInstanceID());
        } catch (MalformedURLException ex) {
            // does not happen
        }
        m_tipsAndNewsUrl = tipsTricksUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            getTipsAndNews();

            m_introFileLock.lock();
            try {
                injectTipsAndNews();
            } finally {
                m_introFileLock.unlock();
            }
        } catch (XPathExpressionException | IOException | TransformerFactoryConfigurationError | TransformerException
                | ParserConfigurationException | SAXException ex) {
            NodeLogger.getLogger(getClass()).warn("Could not inject tips and news into intro page: " + ex.getMessage(),
                ex);
        }
    }

    private void injectTipsAndNews() throws XPathExpressionException, IOException,
        TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException, SAXException {

        DocumentBuilder parser = m_parserFactory.newDocumentBuilder();
        Document doc = parser.parse(m_templateFile);

        XPath xpath = m_xpathFactory.newXPath();
        Element newsNode = (Element)xpath.evaluate("//div[@id='news']", doc.getDocumentElement(), XPathConstants.NODE);
        if (m_news != null) {
            Node hr = (Node)xpath.evaluate("hr", m_news, XPathConstants.NODE);
            if (hr != null) {
                // the news block retrieved from the webpage contains an <hr> that we don't need
                hr.getParentNode().removeChild(hr);
            }

            newsNode.removeAttribute("style");
            newsNode.appendChild(doc.adoptNode(m_news));
        } else {
            // no news, so remove it completely
            newsNode.getParentNode().removeChild(newsNode);
        }

        if (m_tips != null) {
            Element tipsNode =
                (Element)xpath.evaluate("//div[@id='tips']", doc.getDocumentElement(), XPathConstants.NODE);

            tipsNode.removeAttribute("style");
            tipsNode.appendChild(doc.adoptNode(m_tips));
        }

        File temp = FileUtil.createTempFile("intro", ".html", true);
        Transformer serializer = m_transformerFactory.newTransformer();
        serializer.setOutputProperty(OutputKeys.METHOD, "xhtml");
        serializer.transform(new DOMSource(doc), new StreamResult(temp));
        Files.move(temp.toPath(), m_templateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Fetches content from the KNIME webpage and assigns it to the member variables.
     */
    private void getTipsAndNews() throws IOException, SAXNotRecognizedException, SAXNotSupportedException,
        TransformerFactoryConfigurationError, TransformerException, XPathExpressionException {
        HttpURLConnection conn = (HttpURLConnection)m_tipsAndNewsUrl.openConnection();
        conn.setReadTimeout(5000);
        conn.setConnectTimeout(2000);
        conn.connect();

        // parse tips&tricks from webpage
        XMLReader reader = new Parser();
        reader.setFeature(Parser.namespacesFeature, false);
        reader.setFeature(Parser.namespacePrefixesFeature, false);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMResult res = new DOMResult();
        transformer.transform(new SAXSource(reader, new InputSource(conn.getInputStream())), res);
        conn.disconnect();

        XPath xpath = XPathFactory.newInstance().newXPath();
        m_news = (Node)xpath.evaluate("//div[@id='knime-client-news']", res.getNode(), XPathConstants.NODE);
        fixRelativeURLs(m_news, xpath);

        m_tips = (Node)xpath.evaluate("//div[@class='contentWrapper']", res.getNode(), XPathConstants.NODE);
        fixRelativeURLs(m_tips, xpath);
    }

    /**
     * Fixes relative URLs in "src" and "href" attributes.
     *
     * @param element any element
     * @param xpath an xpath object that will be used
     * @throws XPathExpressionException if the applied XPath contains errors (very unlikely...)
     */
    private static void fixRelativeURLs(final Node element, final XPath xpath) throws XPathExpressionException {
        NodeList nl = (NodeList)xpath.evaluate(".//*[@src]", element, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element)nl.item(i);
            if (!e.getAttribute("src").startsWith("http")) {
                e.setAttribute("src", "http://www.knime.org/" + e.getAttribute("src"));
            }
        }

        nl = (NodeList)xpath.evaluate(".//*[@href]", element, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element)nl.item(i);
            if (!e.getAttribute("href").startsWith("http")) {
                e.setAttribute("href", "http://www.knime.org/" + e.getAttribute("href"));
            }
        }
    }
}
