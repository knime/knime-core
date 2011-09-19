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
 *   19.09.2011 (meinl): created
 */
package org.knime.product.rcp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.ccil.cowan.tagsoup.Parser;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * This class is a modal dialog that displays KNIME tips and tricks in a browser
 * widget.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TipsAndTricksDialog extends Dialog {
    private static final Point INITIAL_SIZE = new Point(400, 300);

    static final URL TIPS_AND_TRICKS_URL;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(TipsAndTricksDialog.class);

    private final String m_tipsAndTricks;

    private static final String NO_TIPS =
            "<div id=\"noConnection\">The KNIME webserver cannot be reached. "
                    + "Maybe your internet connection is down. Therefore we "
                    + "cannot show you the latest tips and tricks.</div>";

    static {
        URL url = null;
        try {
            url = new URL("http://www.knime.org/tips-and-tricks");
        } catch (MalformedURLException ex) {
            // does not happen
        }
        TIPS_AND_TRICKS_URL = url;
    }

    /**
     * Creates a new dialog
     *
     * @param parentShell the parent shell, or <code>null</code> to create a
     *            top-level shell
     */
    public TipsAndTricksDialog(final Shell parentShell) {
        super(parentShell);

        String s = null;
        try {
            HttpURLConnection conn =
                    (HttpURLConnection)TIPS_AND_TRICKS_URL.openConnection();
            conn.setConnectTimeout(500);
            conn.connect();
            s =
                    extractOnlineTips(new BufferedInputStream(
                            conn.getInputStream()));
            conn.disconnect();
        } catch (IOException ex) {
            // timeout, unknown host, ...
            LOGGER.warn("Cannot connect to knime.org", ex);
        } catch (Exception ex) {
            LOGGER.error("Cannot get tips and tricks", ex);
        }
        m_tipsAndTricks = (s != null) ? s : NO_TIPS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Tips & Tricks");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(gd);
        applyDialogFont(composite);

        Browser browser = new Browser(composite, SWT.EMBEDDED | SWT.FILL);
        browser.setLayoutData(gd);
        try {
            browser.setText(getHtml());
        } catch (TransformerFactoryConfigurationError ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (TransformerException ex) {
            ex.printStackTrace();
        }
        return composite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {
        return INITIAL_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
    }

    private String getHtml() throws IOException,
            TransformerFactoryConfigurationError, TransformerException {
        URL cssUrl =
                FileLocator.toFileURL(FileLocator.find(FrameworkUtil
                        .getBundle(getClass()),
                        new Path("/intro/css/knime.css"), null));
        StringBuilder content = new StringBuilder();
        content.append("<html><head>");
        content.append("<meta http-equiv=\"content-type\" "
                + "content=\"text/html; charset=UTF-8\"></meta>");
        content.append("<style>");
        content.append("@import url(\"" + cssUrl + "\");");
        content.append("</style>");
        content.append("</head><body>");
        content.append(m_tipsAndTricks);
        content.append("</body></html>");
        return content.toString();
    }

    private static String extractOnlineTips(final InputStream in)
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
        Element tat = null;

        // extract the right div
        NodeList nl = doc.getDocumentElement().getElementsByTagName("div");
        for (int i = 0; i < nl.getLength(); i++) {
            Element div = (Element)nl.item(i);
            if ("colWrapper".equals(div.getAttribute("id"))) {
                tat = div;
                break;
            }
        }
        if (tat == null) {
            return null;
        }

        // fix relative links
        String linkBase =
                TIPS_AND_TRICKS_URL.getProtocol() + "://"
                        + TIPS_AND_TRICKS_URL.getHost();
        nl = tat.getElementsByTagName("a");
        for (int i = 0; i < nl.getLength(); i++) {
            Element a = (Element)nl.item(i);
            String href = a.getAttribute("href");
            if (!href.startsWith("http")) {
                a.setAttribute("href", linkBase + "/" + href);
                a.setAttribute("target", "_blank");
            }
        }

        // remove inline styles
        nl = tat.getElementsByTagName("style");
        for (int i = 0; i < nl.getLength(); i++) {
            Element style = (Element)nl.item(i);
            style.getParentNode().removeChild(style);
        }

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
        t.transform(new DOMSource(tat), new StreamResult(bos));
        return new String(bos.toByteArray());
    }
}
