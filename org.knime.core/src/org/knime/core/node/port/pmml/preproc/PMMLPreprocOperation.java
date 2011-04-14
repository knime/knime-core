/*
 *
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
 *   Mar 16, 2010 (morent): created
 */
package org.knime.core.node.port.pmml.preproc;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Abstract base class for all preprocessing operations in KNIME.
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public abstract class PMMLPreprocOperation {
    protected static final String LOCAL_TRANS = "LocalTransformations";
    protected static final String DATA_DICT = "DataDictionary";
    protected static final String LINEAR_NORM = "LinearNorm";
    protected static final String DERIVED_FIELD = "DerivedField";
    protected static final String FIELD = "field";
    protected static final String EXTENSION = "Extension";
    protected static final String NORM = "norm";
    protected static final String ORIG = "orig";
    protected static final String NAME = "name";
    protected static final String VALUE = "value";
    protected static final String DATATYPE = "dataType";
    protected static final String OPTYPE = "optype";
    protected static final String OP_TYPE_CONTINOUS = "continuous";
    /** Constant for CDATA. */
    protected static final String CDATA = "CDATA";

    /**
     * @param handler
     * @param executionMonitor
     * @throws SAXException
     */
    public abstract void save(final TransformerHandler handler,
            ExecutionMonitor executionMonitor) throws SAXException;

    /**
     * @return the name of this operation
     */
    public abstract String getName();

    /**
     * @return the summary of this operation
     */
    public abstract String getSummary();

    /**
     * This method will probably be remove soon when the
     * {@link PMMLPreprocPortObject} is gone.
     * @return The PMMLContentHandler for loading the operation
     */
    @Deprecated
    public abstract PMMLContentHandler getHandlerForLoad();


    /**
     * Parses the passed PMMLTransformElement to retrieve the contained {@link
     * PMMLPreprocOperation}s. Elements that can be interpreted are "consumed"
     * (removed from the transformation element.
     * @param transformElement an xml element containing a
     *      {@link PMMLTransformElement} to be parsed.
     */
    public abstract void parse(final Element transformElement);

    /**
     * @return The PMML Element this operation should be written into.
     */
    public abstract PMMLTransformElement getTransformElement();

    /**
     * @return a list with the names of the columns to be preprocessed
     */
    public abstract List<String> getColumnNames();

    /**
     * Enumeration of the PMML elements the preprocessing operation can be
     * written to.
     *
     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     */
    public enum PMMLTransformElement {
        /** The LocalTransformation element. */
        LOCALTRANS(LOCAL_TRANS),
        /** The DataDictionary element. */
        DATADICT(DATA_DICT);

        private final String m_represent;
        private static final HashMap<String, PMMLTransformElement> LOOKUP =
            new HashMap<String, PMMLTransformElement>();

        /**
         * Create a new PMMLTransformElement.
         *
         * @param rep the string representation of the strategy
         * @param supported true if the strategy is supported, false otherwise
         */
        private PMMLTransformElement(final String rep) {
            m_represent = rep;
        }

        static {
            for (PMMLTransformElement elem
                    : EnumSet.allOf(PMMLTransformElement.class)) {
                LOOKUP.put(elem.toString(), elem);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_represent;
        }

        /**
         * Returns the corresponding PMML transformation element for the string
         * representation.
         * @param represent the representation to retrieve the write element for
         * @return the PMML write element
         */
        public static PMMLTransformElement get(final String represent) {
            if (represent == null) {
                return null;
            }

            PMMLTransformElement elem = LOOKUP.get(represent);
            if (elem == null) {
                throw new IllegalArgumentException(
                        "PMML Transformation element " + represent
                        + " is unknown.");
            }
            return elem;
        }
    }
}
