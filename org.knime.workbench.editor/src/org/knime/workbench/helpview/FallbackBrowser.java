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
 *   27.08.2007 (Fabian Dill): created
 */
package org.knime.workbench.helpview;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.helpview.HelpviewPlugin;

/**
 * Converts HTML text into normal text by using its own XSLT transformation.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class FallbackBrowser {

    private Transformer m_transformer;
    
    private final StyledText m_text;
    
    private final StyleRange m_styleRange;
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            FallbackBrowser.class);
    private static final String XSLT = "HTML2Text.xslt";
    
    private static final String WARNING = "The operating systems web browser" 
        + " could not be found!\n" 
        + "Using fall back (text-only) browser"; 
    
    /**
     * @param parent parent
     * @param style SWT constants
     */
    public FallbackBrowser(final Composite parent, final int style) {
        m_text = new StyledText(parent, style);
        m_styleRange = new StyleRange();
        m_styleRange.start = 0;
        m_styleRange.length = WARNING.length();
        m_styleRange.fontStyle = SWT.BOLD;
        Color red = new Color(m_text.getDisplay(), 255, 0, 0);
        m_styleRange.foreground = red; 
        try {
            File xslt = new File(
                    FileLocator.toFileURL(FileLocator.find(
                    HelpviewPlugin.getDefault().getBundle(), 
                    new Path("/" + XSLT), null)).getFile());
            StreamSource stylesheet = new StreamSource(xslt);
            m_transformer =
                    TransformerFactory.newInstance().newTemplates(stylesheet)
                            .newTransformer();
            m_transformer.setOutputProperty(
                    OutputKeys.OMIT_XML_DECLARATION, "yes");
        } catch (TransformerConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Converts HTML to plain text.
     * @param string if HTML it is converted into plain text
     */
    public void setText(final String string) {
        m_text.setText("");
        StringBuilder result = new StringBuilder(WARNING + "\n");
        if (string.startsWith("<")) {
            result.append(convertHTML(string));
        } else {
            result.append(string);
        }
        m_text.setText(result.toString());
        m_text.setStyleRange(m_styleRange);
    }
    
    /**
     * Delegate to the wrapped Text component.
     */
    public void setFocus() {
        m_text.setFocus();
    }
    
    
    /**
     * Delegate to the wrapped Text component.
     * @return the display of the Text
     */
    public Display getDisplay() {
        return m_text.getDisplay();
    }
    
    /**
     * 
     * @param html HTML description.
     * @return the HTML as plain text
     */
    public String convertHTML(final String html) {
        if (html == null) {
            return "No description available. Please add an XML description"; 
        }
        StreamResult result = new StreamResult(new StringWriter());
        StreamSource source = new StreamSource(new StringReader(html));
        try {
            m_transformer.transform(source, result);
        } catch (TransformerException ex) {
            ex.printStackTrace();
            LOGGER.coding("Unable to process fullDescription in " + "xml: "
                    + ex.getMessage(), ex);
            return "Unable to process fullDescription in " + "xml: "
                + ex.getMessage();
        }
        return result.getWriter().toString();
    }
}
