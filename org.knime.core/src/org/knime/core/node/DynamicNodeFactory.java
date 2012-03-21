/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Mar 19, 2012 (hornm): created
 */
package org.knime.core.node;

import java.io.InputStream;
import java.util.Properties;

import noNamespace.KnimeNodeDocument;

/**
 *
 * @author hornm, University of Konstanz
 * @param <T> the node model of the factory
 */
public abstract class DynamicNodeFactory<T extends NodeModel> extends
        NodeFactory<T> {
    /**
     * Key for the property that contains the {@link NodeSetFactory} that
     * created this DynamicNodeFactory.
     */
    private static final String NODE_SET_FACTORY_KEY = "NodeSetFactory";

    /** Key for the after-id property. */
    private static final String AFTER_ID_KEY = "AfterId";

    /** Key for the category property. */
    private static final String CATEGORY_KEY = "Category";

    /* Contains additional properties that are otherwise defined by the
     * node extension point. */
    private Properties m_properties;

    /**
     * Creates a new dynamic node factory. Additional properties should be set
     * later by invoking {@link #setAdditionalProperties(Properties)}.
     */
    public DynamicNodeFactory() {
        // needed for creation by reflection
    }

    /**
     * @return the category the node associated with this node factory belongs
     *         to
     */
    public String getCategory() {
        return m_properties.getProperty(CATEGORY_KEY);
    }

    /**
     * @param category the category to set
     */
    public void setCategory(final String category) {
        m_properties.setProperty(CATEGORY_KEY, category);
    }

    /**
     * @return the ID after which this factory's node is sorted in
     */
    public String getAfterID() {
        return m_properties.getProperty(AFTER_ID_KEY);
    }

    /**
     * @param afterId the afterId to set
     */
    public void setAfterID(final String afterId) {
        m_properties.setProperty(AFTER_ID_KEY, afterId);
    }

    /**
     * @return the fully qualified class name of the {@link NodeSetFactory} that
     *      created this DynamicNodeFactory.
     */
    public String getNodeSetFactory() {
        return m_properties.getProperty(NODE_SET_FACTORY_KEY);
    }

    /**
     * @param nodeSetFactory the fully qualified class name of the
     *          {@link NodeSetFactory} to set
     */
    public void setNodeSetFactory(final String nodeSetFactory) {
        m_properties.setProperty(NODE_SET_FACTORY_KEY, nodeSetFactory);
    }

    /**
     * Allows to get additional properties that are set for the node factory.
     *
     * @return the additional properties set for this node factory or an empty
     *      property list if not available
     */
    public Properties getAdditionalProperties() {
        return m_properties;
    }

    /**
     * Allows to get additional properties that are set for the node factory.
     * @param properties the additional properties to set
     */
    public void setAdditionalProperties(final Properties properties) {
        m_properties.putAll(properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream getPropertiesInputStream() {
        KnimeNodeDocument doc = KnimeNodeDocument.Factory.newInstance();
        addNodeDescription(doc);
        return doc.newInputStream();
    }

    /**
     * Subclasses should add the node description elements.
     * @param doc the document to add the description to
     */
    protected abstract void addNodeDescription(final KnimeNodeDocument doc);
}
