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
 *   Sep 3, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.xml.sax.SAXException;

/**
 * Base class for Predicate as specified in PMML
 * (<a>http://www.dmg.org/v4-0/TreeModel.html</a>).
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public abstract class PMMLPredicate {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER
        = NodeLogger.getLogger(PMMLPredicate.class);

    /** For formatting the predicates toString output. */
    protected static NumberFormat NUMBERFORMAT = NumberFormat.getInstance();

    /** The key to store the predicate type in configurations. */
    protected static final String TYPE_KEY = "type";
    /** The key to store the attribute in configurations. */
    protected static final String ATTRIBUTE_KEY = "value";
    /** The key to store the operator in configurations. */
    protected static final String OPERATOR_KEY = "operator";

    /** Constant for CDATA. */
    protected static final String CDATA = "CDATA";

    /** The PMML operator used for this predicate. */
    private PMMLOperator m_op;

    /** The name of the field that is evaluated. */
    private String m_splitAttribute;



    /* Remember previous table spec and index used for classification to save
        time. */
    private transient DataTableSpec m_previousSpec = null;
    private transient int m_previousIndex = -1;

    /**
     * @return the operator used for this predicate
     */
    public PMMLOperator getOperator() {
        return m_op;
    }

    /**
     * @param op the op to set
     */
    public void setOperator(final String op) {
        try {
            m_op = PMMLOperator.get(op);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @param op the op to set
     */
    public void setOperator(final PMMLOperator op) {
        m_op = op;
    }

    /**
     * Returns the name of the field the predicate operates on, an empty String
     * if it works on multiple attributes, or null if not applicable.
     *
     * @return the name of the field the predicate operates on, "" if working on
     *         multiple attributes or null if not applicable
     */
    protected String getSplitAttribute() {
        return m_splitAttribute;
    }

    /**
     * @param splitAttribute the splitAttribute to set
     */
    public void setSplitAttribute(final String splitAttribute) {
        m_splitAttribute = splitAttribute;
    }

    /**
     * @return the previousSpec
     */
    protected DataTableSpec getPreviousSpec() {
        return m_previousSpec;
    }

    /**
     * @param previousSpec the previousSpec to set
     */
    protected void setPreviousSpec(final DataTableSpec previousSpec) {
        m_previousSpec = previousSpec;
    }

    /**
     * @return the previousIndex
     */
    protected int getPreviousIndex() {
        return m_previousIndex;
    }

    /**
     * @param previousIndex the previousIndex to set
     */
    protected void setPreviousIndex(final int previousIndex) {
        m_previousIndex = previousIndex;
    }

    /**
     * Evaluates the predicate for the passed parameters and returns the result.
     * If values are missing and an evaluation is not possible null is returned.
     *
     * @param row The data row containing the data cells to be evaluated.
     * @param spec The spec for the row.
     * @return true if the the predicates evaluates to true, false if it
     *         evaluates to false, null on missing values
     */
    public abstract Boolean evaluate(final DataRow row,
            final DataTableSpec spec);

    /**
     * Store the spec and index position to speedup subsequent evaluations.
     *
     * @param spec the spec to be stored
     */
    protected void cacheSpec(final DataTableSpec spec) {
        assert m_splitAttribute != null && m_splitAttribute != "";
        if (spec != m_previousSpec) {
            m_previousIndex = spec.findColumnIndex(m_splitAttribute);
            if (m_previousIndex == -1) {
                LOGGER.error("Decision Tree Prediction failed."
                        + " Could not find attribute '"
                        + m_splitAttribute + "'");
            }
            m_previousSpec = spec;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String toString();

    /**
     * Returns the name of the predicate.
     *
     * @return the name of the predicate
     */
    public abstract String getName();

    /**
     * Writes the PMML XML object for the predicate.
     *
     * @param handler TransformerHandler for parsing and transforming events
     * @throws SAXException - any SAX exception, possibly wrapping another
     *             exception
     */
    public abstract void writePMML(TransformerHandler handler)
            throws SAXException;

    /**
     * Save internal predicate settings to a config object.
     *
     * @param conf the config object to write into
     *
     */
    public abstract void saveToPredParams(final Config conf);

    /**
     * Load internal predicate settings from a config object.
     *
     * @param conf the config object to load from
     * @throws InvalidSettingsException - if invalid settings are provided
     *
     */
    public abstract void loadFromPredParams(final Config conf)
            throws InvalidSettingsException;


    /**
     * Returns the correct PMMLPredicate for a config object. Based on the
     * config it determines the correct PMMLPredicate type, creates a new
     * predicate instance and initializes it with the settings from the
     * configuration.
     *
     * @param conf the config to get a predicate for
     * @return the initialized predicate
     * @throws InvalidSettingsException - if no predicate can be instantiated
     *             with the provided settings
     */
    public static PMMLPredicate getPredicateForConfig(final Config conf)
            throws InvalidSettingsException {
        PMMLPredicate pred;
        String type = conf.getString(TYPE_KEY);
        try {
            pred = getPredicateForType(type);
        } catch (InstantiationException e) {
            throw new InvalidSettingsException("Invalid type " + type
                    + " provided in config. Predicate cannot be instantiated.",
                    e);
        } catch (IllegalAccessException e) {
            throw new InvalidSettingsException("Invalid type " + type
                    + "provided in config. Type has "
                    + "no public nullary constructor.", e);
        }
        pred.loadFromPredParams(conf);
        return pred;
    }

    /**
     * Returns the correct PMMLPredicate for its string representation.
     *
     * @param type the string representation of the predicate
     * @return the predicate
     * @throws InstantiationException - if the instantiation fails for some
     *             reason
     * @throws IllegalAccessException - if the class or its nullary constructor
     *             is not accessible.
     */
    public static PMMLPredicate getPredicateForType(final String type)
            throws InstantiationException, IllegalAccessException {
        Class<? extends PMMLPredicate> predClass = PMMLPredicates.get(type);
        return predClass.newInstance();
    }


    /**
     * Enumeration of all subclasses of PMMLPredicate. New implementations must
     * be added here for being able to instantiate the correct subclass when
     * loading from predictor params.
     *
     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     */
    enum PMMLPredicates {
        /** @see org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate */
        SIMPLE_PREDICATE(PMMLSimplePredicate.NAME, PMMLSimplePredicate.class),
        /** @see org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate*/
        SIMPLE_SET_PREDICATE(
                PMMLSimpleSetPredicate.NAME, PMMLSimpleSetPredicate.class),
        /** @see org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate */
        COMPOUND_PREDICATE(
                PMMLCompoundPredicate.NAME, PMMLCompoundPredicate.class),
        /** @see org.knime.base.node.mine.decisiontree2.PMMLTruePredicate */
        TRUE_PREDICATE(
                PMMLTruePredicate.NAME, PMMLTruePredicate.class),
        /** @see org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate */
        FALSE_PREDICATE(
                PMMLFalsePredicate.NAME, PMMLFalsePredicate.class);

        private final String m_type;

        private final Class<? extends PMMLPredicate> m_class;

        private PMMLPredicates(final String type,
                final Class<? extends PMMLPredicate> predClass) {
            m_type = type;
            m_class = predClass;
        }

        private static final Map<String, PMMLPredicates> LOOKUP =
                new HashMap<String, PMMLPredicates>();

        static {
            for (PMMLPredicates pred : EnumSet.allOf(PMMLPredicates.class)) {
                LOOKUP.put(pred.toString(), pred);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (m_type != null) {
                return m_type;
            }
            return super.toString();
        }

        /**
         * Returns the predicate class.
         *
         * @return the class
         */
        public Class<? extends PMMLPredicate> getPredClass() {
            return m_class;
        }

        /**
         * Returns the corresponding class for the passed predicate type.
         *
         * @param type the predicate type to find the class for
         * @return the predicate class
         * @throws InstantiationException - if no such PMML predicate exists
         */
        public static Class<? extends PMMLPredicate> get(final String type)
                throws InstantiationException {
            PMMLPredicates pmmlPredicates = LOOKUP.get(type);
            if (pmmlPredicates == null) {
                throw new InstantiationException("Illegal PMMLPredicate "
                        + "type '" + type);
            }
            return pmmlPredicates.getPredClass();
        }

    }

}
