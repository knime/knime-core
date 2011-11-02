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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.ccil.cowan.tagsoup.Parser;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * This class is a modal dialog that displays KNIME tips and tricks in a browser
 * widget.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TipsAndTricksDialog extends Dialog {
    
    private static final Point INITIAL_SIZE = new Point(400, 325);

    private static final URL TIPS_AND_TRICKS_URL;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(TipsAndTricksDialog.class);

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
     * Creates a new dialog.
     *
     * @param parentShell the parent shell, or <code>null</code> to create a
     *            top-level shell
     */
    public TipsAndTricksDialog(final Shell parentShell) {
        super(parentShell);
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
            browser.setText(getHtml(parent));
            browser.addLocationListener(new LocationListener() {
                @Override
                public void changing(final LocationEvent event) {
                    if (event.location.startsWith("about")) {
                        // ignore about:blank
                        return;
                    }
                    try {
                        // Open default external browser
                        PlatformUI.getWorkbench().getBrowserSupport()
                                .getExternalBrowser()
                                .openURL(new URL(event.location));
                    } catch (PartInitException e) {
                        LOGGER.error("Could not open external webbrowser for "
                                + "URL \"" + event.location + "\"." + e);
                    } catch (MalformedURLException e) {
                        LOGGER.error("Invalid URL or unknown protocol: \""
                                + event.location + "\"" + e);
                    } finally {
                        event.doit = false;
                    }
                }

                @Override
                public void changed(final LocationEvent event) {
                    // do nothing
                }
            });
        } catch (TransformerFactoryConfigurationError ex) {
            LOGGER.error(ex.getMessage(), ex);
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
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
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
    }

    private String getHtml(final Composite parent) throws IOException,
            TransformerFactoryConfigurationError {
        URL cssUrl =
                FileLocator.toFileURL(FileLocator.find(FrameworkUtil
                        .getBundle(getClass()), new Path(
                        "/intro/css/tipstricks.css"), null));

        RGB bgColor = parent.getBackground().getRGB();

        String content = null;
        try {
            HttpURLConnection conn =
                    (HttpURLConnection)TIPS_AND_TRICKS_URL.openConnection();
            conn.setConnectTimeout(500);
            conn.connect();

            Transformer transformer =
                    TransformerFactory.newInstance().newTransformer();
            XMLReader reader = new Parser();
            reader.setFeature(Parser.namespacesFeature, false);
            reader.setFeature(Parser.namespacePrefixesFeature, false);
            DOMResult res = new DOMResult();
            transformer.transform(
                    new SAXSource(reader,
                            new InputSource(conn.getInputStream())), res);

            Source xslt =
                    new StreamSource(getClass().getResourceAsStream(
                            "tipsAndTricks.xslt"));
            transformer = TransformerFactory.newInstance().newTransformer(xslt);
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
                    "yes");
            transformer.setParameter("cssUrl", cssUrl.toString());
            transformer.setParameter("bgColor", "rgb(" + bgColor.red + ","
                    + bgColor.green + "," + bgColor.blue + ")");
            transformer.setParameter("linkBase",
                    TIPS_AND_TRICKS_URL.getProtocol() + "://"
                            + TIPS_AND_TRICKS_URL.getHost());

            ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
            transformer.transform(new DOMSource(res.getNode()),
                    new StreamResult(bos));
            content = new String(bos.toByteArray(), Charset.forName("UTF-8"));

            conn.disconnect();
        } catch (IOException ex) {
            // timeout, unknown host, ...
            LOGGER.warn("Cannot connect to knime.org", ex);
        } catch (Exception ex) {
            LOGGER.error("Cannot get tips and tricks", ex);
        }

        return content;
    }
}
