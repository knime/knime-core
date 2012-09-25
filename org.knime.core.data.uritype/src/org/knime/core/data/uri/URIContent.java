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
 *   Oct 4, 2011 (wiswedel): created
 */
package org.knime.core.data.uri;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class URIContent implements Serializable {

    /**
     * Serial id.
     */
    private static final long serialVersionUID = 3597861354031942354L;

    private final URI m_uri;

    private final String m_extension;

    /**
     * Framework constructor, <b>do not use in client code</b>. Subclasses must
     * inherit this constructor.
     * 
     * 
     * @param model The model to read from
     * @throws InvalidSettingsException If the model contains invalid
     *             information.
     */
    public URIContent(final ModelContentRO model)
            throws InvalidSettingsException {
        try {
            m_uri = new URI(model.getString("uri"));
        } catch (URISyntaxException e) {
            throw new InvalidSettingsException(e);
        }
        m_extension = model.getString("extension");
    }

    /**
     * Constructor for new URI content.
     * 
     * 
     * @param uri URI of this object
     * @param extension File extension of this object
     */
    public URIContent(final URI uri, final String extension) {
        if (uri == null || extension == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        m_uri = uri;
        m_extension = extension;
    }

    /**
     * @return The URI
     */
    public URI getURI() {
        return m_uri;
    }

    /**
     * @return The file extension
     */
    public String getExtension() {
        return m_extension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getURI().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof URIContent)) {
            return false;
        }
        URIContent oCnt = (URIContent)obj;
        return getURI().equals(oCnt.getURI());
    }

    /**
     * @param output The model to save in
     */
    protected void save(final ModelContentWO output) {
        output.addString("uri", m_uri.toString());
        output.addString("extension", m_extension);
    }

    /**
     * @param uri The URI content to save
     * @param model The model to save in
     */
    public static final void saveURIContent(final URIContent uri,
            final ModelContentWO model) {
        String className = uri.getClass().getName();
        // possibly a sub-class of URIContent
        model.addString("className", className);
        ModelContentWO contentChild = model.addModelContent("content");
        uri.save(contentChild);
    }

    /**
     * @param model The model to read from
     * @return URIContent object
     * @throws InvalidSettingsException If the model contains invalid
     *             information.
     */
    public static final URIContent loadURIContent(final ModelContentRO model)
            throws InvalidSettingsException {
        String className = model.getString("className");
        ModelContentRO contentChild = model.getModelContent("content");
        if (className == null) {
            throw new InvalidSettingsException("Class name is null.");
        }
        try {
            // possibly a sub-class of URIContent
            Class<?> cl = Class.forName(className);
            Constructor<?> constr = cl.getConstructor(ModelContentRO.class);
            return (URIContent)constr.newInstance(contentChild);
        } catch (Exception e) {
            throw new InvalidSettingsException(
                    "Failed to instantiate URIContent class \"" + className
                            + "\" using ModelContentRO constructor", e);
        }
    }

}
