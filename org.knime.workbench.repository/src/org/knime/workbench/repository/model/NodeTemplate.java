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
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.ui.views.properties.IPropertySource;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.workbench.repository.model.props.NodePropertySource;

/**
 * Class that realizes a (contributed) node in the repository tree. This is used
 * as a "template" for actual instances of a node in the workflow editor.
 * 
 * Note: The type constants *must* match those defined in the "nodes"- extension
 * point (Node.exsd).
 * 
 * TODO introduce new fields: provider, url, license-tag (free/commercial) ...
 * ???
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeTemplate extends AbstractSimpleObject {
    /** Type for nodes that read data. */
    public static final String TYPE_DATA_READER = "data reader";

    /** Type for nodes that transform data. */
    public static final String TYPE_DATA_TRANSFORMER = "data transformer";

    /** Type for learner nodes. */
    public static final String TYPE_LEARNER = "learner";

    /** Type for nodes that use a model make predictions. */
    public static final String TYPE_PREDICTOR = "predictor";

    /** Type for nodes that provide a view on the data/model. */
    public static final String TYPE_VISUALIZER = "visualizer";

    /** Type for nodes that evaluate some model. */
    public static final String TYPE_EVALUATOR = "evaluator";

    /** Type for nodes that are in fact meta nodes. */
    public static final String TYPE_META = "meta";

    
    /** Type for nodes that can't be assigned to one of the other types. */
    public static final String TYPE_OTHER = "other";

    private static final Set<String> TYPES = new HashSet<String>();

    static {
        TYPES.add(TYPE_DATA_READER);
        TYPES.add(TYPE_DATA_TRANSFORMER);
        TYPES.add(TYPE_LEARNER);
        TYPES.add(TYPE_PREDICTOR);
        TYPES.add(TYPE_VISUALIZER);
        TYPES.add(TYPE_EVALUATOR);
        TYPES.add(TYPE_META);
        TYPES.add(TYPE_OTHER);
    }

    private Class<NodeFactory<? extends NodeModel>> m_factory;

    private String m_type;
    

    /**
     * Constructs a new NodeTemplate.
     * 
     * @param id The id, usually parsed from the extension
     */
    public NodeTemplate(final String id) {
        setID(id);
    }

    /**
     * @return Returns the factory.
     */
    @SuppressWarnings("unchecked")
    public Class<NodeFactory<? extends NodeModel>> getFactory() {
        return m_factory;
    }

    /**
     * @param factory The factory to set.
     */
    @SuppressWarnings("unchecked")
    public void setFactory(
            final Class<NodeFactory<? extends NodeModel>> 
            factory) {
        m_factory = factory;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getAdapter(final Class adapter) {
        if (adapter == IPropertySource.class) {
            return new NodePropertySource(this);
        }

        return super.getAdapter(adapter);
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return m_type;
    }

    /**
     * @param type The type to set.
     */
    public void setType(final String type) {
        assert TYPES.contains(type) : "Illegal node type: " + type;
        m_type = type;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // see equals method for comment on this
        return m_factory.getCanonicalName().hashCode();
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NodeTemplate)) {
            return false;
        }
        // avoid duplicate nodes in favorite nodes view
        // to be sure only check for the full class name
        // seems that different built versions of the class have led to 
        // duplicates
        return m_factory.getCanonicalName().equals(
                ((NodeTemplate)obj).getFactory().getCanonicalName());
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_factory == null) {
            return super.toString();
        }
        return m_factory.getName();
    }
    
}
