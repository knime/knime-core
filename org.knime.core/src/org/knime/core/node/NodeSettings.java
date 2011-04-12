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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.node;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.knime.core.node.config.Config;


/**
 * This class overwrites the general <code>Config</code> object and
 * specializes some method to access <code>NodeSettings</code> object. This
 * object is used within the node packages.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class NodeSettings extends Config
        implements NodeSettingsRO, NodeSettingsWO {

    /**
     * Creates a new instance of this object with the given key.
     *
     * @param key An identifier.
     */
    public NodeSettings(final String key) {
        super(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Config getInstance(final String key) {
        return new NodeSettings(key);
    }

    /**
     * @see Config#readFromFile(java.io.ObjectInputStream)
     */
    public static NodeSettings readFromFile(
            final ObjectInputStream ois) throws IOException {
        return (NodeSettings)Config.readFromFile(ois);
    }

    /**
     * Reads <code>NodeSettings</code> object from a given XML input stream and
     * writes them into the given <code>NodeSettings</code> object. The stream
     * will be closed by this call.
     *
     * @param in XML input stream to read settings from.
     * @return A new settings object.
     * @throws IOException If the stream could not be read.
     * @throws NullPointerException If one of the arguments is
     *         <code>null</code>.
     */
    public static NodeSettingsRO loadFromXML(
            final InputStream in) throws IOException {
        NodeSettings tmp = new NodeSettings("ignored");
        return (NodeSettingsRO) Config.loadFromXML(tmp, in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NodeSettingsWO addNodeSettings(final String key) {
        return (NodeSettings)super.addConfig(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNodeSettings(final NodeSettings settings) {
        super.addConfig(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NodeSettings getNodeSettings(final String key)
            throws InvalidSettingsException {
        return (NodeSettings)super.getConfig(key);
    }

}
