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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   16.12.2010 (hofer): created
 */
package org.knime.core.data.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Cell factory that creates XML cells. Depending on the size either normal
 * {@link DataCell}s or {@link BlobDataCell}s are created.
 *
 * @author Heiko Hofer
 */
public class XMLCellFactory {
    /**
     * Minimum size for blobs in bytes. That is, if a given string is at least
     * as large as this value, it will be represented by a blob cell
     */
    public static final int DEF_MIN_BLOB_SIZE_IN_BYTES = 8 * 1024;

    private static final int MIN_BLOB_SIZE_IN_BYTES;

    static {
        int size = DEF_MIN_BLOB_SIZE_IN_BYTES;
        String envVar = "org.knime.xmlminblobsize";
        String property = System.getProperty(envVar);
        if (property != null) {
            String s = property.trim();
            int multiplier = 1;
            if (s.endsWith("m") || s.endsWith("M")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024 * 1024;
            } else if (s.endsWith("k") || s.endsWith("K")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024;
            }
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new NumberFormatException("Size < 0" + newSize);
                }
                size = newSize * multiplier;
                NodeLogger.getLogger(XMLCellFactory.class).debug(
                        "Setting min blob size for XML cells to " + size
                                + " bytes");
            } catch (NumberFormatException e) {
                NodeLogger.getLogger(XMLCellFactory.class).warn(
                        "Unable to parse property " + envVar
                                + ", using default", e);
            }
        }
        MIN_BLOB_SIZE_IN_BYTES = size;
    }

    /** Type for XML cells. */
    public static final DataType TYPE = DataType.getType(XMLCell.class);

    /** Don't instantiate this class. */
    private XMLCellFactory() {
    	// private constructor prevents that an instance is created
    }

    /**
     * Factory method to create {@link DataCell} representing
     * {@link org.w3c.dom.Document}.
     * The returned cell is either of type {@link XMLCell} (for small documents)
     * or {@link XMLBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     *
     * @param xml String representing the XML document
     * @return DataCell representing the XML document
     * @throws IOException if an io error occurs while reading the XML string
     * @throws SAXException if an error occurs while parsing
     * @throws ParserConfigurationException if the parser cannot be instantiated
     * @throws XMLStreamException
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final String xml) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException {
        if (xml == null) {
            throw new NullPointerException("XML must not be null");
        }
        XMLCellContent content = new XMLCellContent(xml, true);
        if (xml.length() >= MIN_BLOB_SIZE_IN_BYTES) {
            return new XMLBlobCell(content);
        } else {
            return new XMLCell(content);
        }
    }

    /**
     * Factory method to create {@link DataCell} representing
     * {@link org.w3c.dom.Document}.
     * The returned cell is either of type {@link XMLCell} (for small documents)
     * or {@link XMLBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     *
     * @param dom the returned data cell encapsulates this DOM
     * @return DataCell representing the XML document
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final Document dom) {
        if (dom == null) {
            throw new NullPointerException("dom must not be null");
        }
        XMLCellContent content = new XMLCellContent(dom);
        if (content.getStringValue().length() >= MIN_BLOB_SIZE_IN_BYTES) {
            return new XMLBlobCell(content);
        } else {
            return new XMLCell(content);
        }

    }

    /**
     * Factory method to create {@link DataCell} representing
     * {@link org.w3c.dom.Document}.
     * The returned cell is either of type {@link XMLCell} (for small documents)
     * or {@link XMLBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     *
     * @param is The stream containing the XML document
     * @return DataCell representing the XML document
     * @throws IOException if an io error occurs while reading the XML string
     * @throws SAXException if an error occurs while parsing
     * @throws ParserConfigurationException if the parser cannot be instantiated
     * @throws XMLStreamException
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final InputStream is) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException {
        if (is == null) {
            throw new NullPointerException("InputStream must not be null");
        }
        XMLCellContent content = new XMLCellContent(is);
        if (content.getStringValue().length() >= MIN_BLOB_SIZE_IN_BYTES) {
            return new XMLBlobCell(content);
        } else {
            return new XMLCell(content);
        }
    }

    /**
     * Factory method to create {@link DataCell} representing
     * {@link org.w3c.dom.Document}.
     * The returned cell is either of type {@link XMLCell} (for small documents)
     * or {@link XMLBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     *
     * @param xml The cell represents this value.
     * @return DataCell representing the XML document
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final XMLValue xml) {
        if (xml == null) {
            throw new NullPointerException("XMLValue must not be null");
        }
    	if(xml instanceof DataCell) {
    		return (DataCell)xml;
    	} else {
    		XMLCellContent content = new XMLCellContent(xml.getDocument());
            if (content.getStringValue().length() >= MIN_BLOB_SIZE_IN_BYTES) {
                return new XMLBlobCell(content);
            } else {
                return new XMLCell(content);
            }
    	}
    }
}
