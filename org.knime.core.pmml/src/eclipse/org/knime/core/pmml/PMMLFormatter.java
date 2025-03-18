/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 * History
 *   Apr 14, 2011 (morent): created
 */
package org.knime.core.pmml;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptionCharEscapeMap;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.dmg.pmml.PMMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 *
 */
public final class PMMLFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PMMLFormatter.class);

    private static final XmlOptions TEXT_SAVE_XML_OPTIONS;

    static {
        Map<String, String> suggestedNamespaces = new HashMap<String, String>();
        suggestedNamespaces.put("http://www.w3.org/2001/XMLSchema-instance",
                "xsi");
        suggestedNamespaces.put(PMMLDocument.type.getDocumentElementName()
                .getNamespaceURI(), "");
        TEXT_SAVE_XML_OPTIONS = new XmlOptions();
        TEXT_SAVE_XML_OPTIONS.setSaveOuter();
        TEXT_SAVE_XML_OPTIONS.setSaveAggressiveNamespaces();
        TEXT_SAVE_XML_OPTIONS.setSavePrettyPrint();
        TEXT_SAVE_XML_OPTIONS.setCharacterEncoding("UTF-8");
        TEXT_SAVE_XML_OPTIONS.setSaveSubstituteCharacters(new PMMLXMLCharEscapeMap());
        TEXT_SAVE_XML_OPTIONS.setSaveSuggestedPrefixes(suggestedNamespaces);
    }

    public static class PMMLXMLCharEscapeMap extends XmlOptionCharEscapeMap{

        @Override
        public boolean containsChar(final char ch) {
            if (isBadChar(ch)) {
                return true;
            }
            return super.containsChar(ch);
        }

        @Override
        public String getEscapedString(final char ch) {
            if (isBadChar(ch)) {
                LOGGER.warn("Illegal control character: #x{} was escaped with '?'", Integer.toHexString(ch));
                return "?";
            }
            return super.getEscapedString(ch);
        }

        /**
         * Test if a character is valid in xml character content. See http://www.w3.org/TR/REC-xml#NT-Char
         */
        private static boolean isBadChar(final char ch) {
            // AP-24108: We allow surrogates (0xD800 - 0xDFFF) here and let XMLBeans deal with unmatched ones.
            //           The ranges in http://www.w3.org/TR/REC-xml#NT-Char talk about Unicode code points, not UTF-16.
            final var isValidUTF16Char = (ch == 0x9) || (ch == 0xA) || (ch == 0xD) || (ch >= 0x20 && ch <= 0xFFFD);
            return !isValidUTF16Char;
        }
    }

    private PMMLFormatter() {
        // utility class with static methods only
    }

    /**
     * Generates a nicely formatted string out of a PMML fragment.
     *
     * @param xmlObject the xml object to format into a string.
     * @return the string representation of the xml object
     */
    public static String xmlText(final XmlObject xmlObject) {
        return xmlObject.xmlText(TEXT_SAVE_XML_OPTIONS);
    }

    /**
     * @param xml the XMLTokenSource to be written
     * @param out the output stream to write to. The stream does not need to be buffered.
     * @throws IOException if the xml cannot be written to the stream
     */
    public static void save(final XmlTokenSource xml, final OutputStream out)
            throws IOException {
        if ((out instanceof BufferedOutputStream) || (out instanceof ByteArrayOutputStream)) {
            xml.save(out, TEXT_SAVE_XML_OPTIONS);
        } else {
            OutputStream os = new BufferedOutputStream(out);
            xml.save(os, TEXT_SAVE_XML_OPTIONS);
            os.flush();
        }
    }

    /**
     * @return  Xml options for a nice output of a xml document.
     */
    public static XmlOptions getOptions() {
        return TEXT_SAVE_XML_OPTIONS;
    }

}
