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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.port.pmml;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.knime.core.node.NodeLogger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLMasterContentHandler extends PMMLContentHandler {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            "PMMLErrorHandler");

    /** The PMML version.*/
    private String m_version = "";

    private final Map<String, PMMLContentHandler>m_registeredHandlers
        = new HashMap<String, PMMLContentHandler>();

    /**
     * Adds a default PMML content handler, that is able to extract the
     * newcessary information from the PMML file for the referring model.
     * @param id id in order to retrieve the handler after parsing
     * @param defaultHandler handler that understands a specific model
     * @return false, if the id is already in use, true if the handler was
     *  successfully registered
     */
    public boolean addContentHandler(final String id,
            final PMMLContentHandler defaultHandler) {
        if (m_registeredHandlers.get(id) != null) {
            return false;
        }
        m_registeredHandlers.put(id, defaultHandler);
        return true;
    }

    /**
     *
     * @param id the id under which the handler is registered
     * @return true if handler successfully removed,
     *  false if it wasn't registered
     */
    public boolean removeContentHandler(final String id) {
        return m_registeredHandlers.remove(id) != null ? true : false;
    }

    /**
     *
     * @param id id under which the handler is registered
     * @return the handler if it was found under this id, null otherwise
     */
    public PMMLContentHandler getDefaultHandler(final String id) {
        return m_registeredHandlers.get(id);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start,
            final int length) throws SAXException {
        for (ContentHandler hdl : m_registeredHandlers.values()) {
            hdl.characters(ch, start, length);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        for (ContentHandler hdl : m_registeredHandlers.values()) {
            hdl.endDocument();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        for (ContentHandler hdl : m_registeredHandlers.values()) {
            hdl.endElement(uri, localName, name);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException {
        if ("PMML".equals(name)) {
            m_version = atts.getValue("version");
            if (!canReadPMMLVersion(m_version)) {
                throw new SAXException("PMML model seems to be of an "
                        + "unsupported version. Only PMML versions "
                        + getSupportedVersions()
                        + " are supported. Found " + m_version);
            }
        }
        for (ContentHandler hdl : m_registeredHandlers.values()) {
            hdl.startElement(uri, localName, name, atts);
        }
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void error(final SAXParseException exception) throws SAXException {
        LOGGER.error("Error during validation of PMML port object: ",
                exception);
//        throw exception;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void fatalError(final SAXParseException exception)
            throws SAXException {
        LOGGER.fatal("Error during validation of PMML port object: ",
                exception);
//        throw exception;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void warning(final SAXParseException exception)
            throws SAXException {
        LOGGER.warn("Error during validation of PMML port object: ",
                exception);
//        throw exception;
    }

    /**
    * @return the version of the parsed PMML file or an empty string if the
    *       version has not been set.
    */
   public String getVersion() {
       return m_version;
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getSupportedVersions() {
        /* The master content handler only supports the intersection of
         * the supported versions of all registered handlers. */
        Set<String> versions = new TreeSet<String>();
        versions.add(PMMLPortObject.PMML_V3_0);
        versions.add(PMMLPortObject.PMML_V3_1);
        versions.add(PMMLPortObject.PMML_V3_2);
        for (PMMLContentHandler handler : m_registeredHandlers.values()) {
            Set<String> v = handler.getSupportedVersions();
            versions.retainAll(v);
        }
        return versions;
    }
}
