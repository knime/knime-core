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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Parses a PMML DataDictionary and converts it into a {@link DataTableSpec}.
 * Each DataField is converted into a {@link DataColumnSpec}.
 * <ul>
 * <li> &lt;xs:attribute name=&quot;name&quot; type=&quot;FIELD-NAME&quot;
 * use=&quot;required&quot; /&gt; will be the column name </li>
 * <li> &lt;xs:attribute name=&quot;dataType&quot; type=&quot;DATATYPE&quot;
 * use=&quot;required&quot; /> will be the column type </li>
 * <li> if the dataType attribute is not available (optional until PMML v3.2),
 * it is tried to be inferred from
 * &lt;xs:attribute name=&quot;optype&quot; type=&quot;OPTYPE&quot; /&gt;,
 * which is one of
 * <ul>
 * <li>categorical</li>
 * <li>ordinal</li>
 * <li>continuous.</li>
 * </ul>
 * Continuous is mapped to {@link DoubleCell#TYPE}, the others are assumed to be
 * of type {@link StringCell#TYPE}.
 * </li>
 * </ul>
 * The {@link DataColumnDomain} is also created by using either the Interval or
 * the Value fields from PMML:
 *
 * <pre>
 *  &lt;xs:choice&gt;
 *  &lt;xs:element ref=&quot;Interval&quot; minOccurs=&quot;0&quot;
 *  maxOccurs=&quot;unbounded&quot; /&gt; &lt;xs:element ref=&quot;Value&quot;
 *  minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot; /&gt;
 *  &lt;/xs:choice&gt;
 * </pre>
 *
 * Some examples simply list each occurring value also for continuous columns,
 * thus they are all collected, sorted and then the minimal and maximal values
 * are chosen as the lower and upper bound. If they are not numerical they are
 * used as nominal values.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class DataDictionaryContentHandler extends PMMLContentHandler {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DataDictionaryContentHandler.class);

    /** ID to identify registered handler. */
    public static final String ID = "DataDictionary";

    private String m_currentName;

    private DataType m_currentType;

    private double m_currentMin = Double.NaN;

    private double m_currentMax = Double.NaN;

    private final List<String> m_currentValues;

    private final List<DataColumnSpec> m_colSpecs;

    private DataTableSpec m_spec;

    private boolean m_read;

    private final Stack<String> m_elemStack;

    /**
     *
     */
    public DataDictionaryContentHandler() {
        m_elemStack = new Stack<String>();
        m_colSpecs = new ArrayList<DataColumnSpec>();
        m_currentValues = new ArrayList<String>();
        m_read = false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException {
        if (name.equals("DataField")) {
            m_read = true;
            if (atts.getValue("usageType") == null
                    || "active".equals(atts.getValue("usageType"))) {
                m_currentName = atts.getValue("name");
                handleOpType(name, atts);
                handleDataType(name, atts);
                // if we could not infer a valid DataType (either String,
                // Double or Int) we must throw an exception
                if (m_currentType == null) {
                    throw new SAXException("Could not infer valid data type!"
                            + " Found optype: " + atts.getValue("optype")
                            + ". And dataType: " + atts.getValue("dataType"));
                }
            }
        } else if (m_read && m_elemStack.peek().equals("DataField")
                && name.equals("Interval")) {
            // retrieve left and right margin from attributes
            if (atts.getValue("leftMargin") != null) {
                m_currentMin = Double.parseDouble(atts.getValue("leftMargin"));
            }
            if (atts.getValue("rightMargin") != null) {
                m_currentMax = Double.parseDouble(atts.getValue("rightMargin"));
            }
        } else if (m_read && m_elemStack.peek().equals("DataField")
                && name.equals("Value")) {
            m_currentValues.add(atts.getValue("value"));
        }
        m_elemStack.push(name);
    }

    private void handleOpType(final String name, final Attributes atts)
            throws SAXException {
        String opType = atts.getValue("optype");
        if (opType == null) {
            throw new SAXException("Invalid PMML!"
                    + " Attribute \"optype\" not found, "
                    + "which is required since PMML v.2.0");
        } else if (opType.equals("categorical")) {
            m_currentType = StringCell.TYPE;
        } else if (opType.equals("ordinal")) {
            // there is no direct KNIME equivalent for ordinal type
            // but integers and doubles are implicitly assumed to be continuous,
            // thus we use a StringCell here, since the StringCell has a
            // comparator and therefore a defined order.
            m_currentType = StringCell.TYPE;
        } else if (opType.equals("continuous")) {
            m_currentType = DoubleCell.TYPE;
        } else {
            // opType is neither of categorical, ordinal, or continuous
            // -> invalid PMML
            throw new SAXException("Invalid PMML! Attribute \"opType\" "
                    + "has invalid value \"" + opType + "\"");
        }
    }

    private void handleDataType(@SuppressWarnings("unused") final String name,
            final Attributes atts)
        throws SAXException {
        String typeName = atts.getValue("dataType");
        if (typeName == null) {
            LOGGER.warn("Invalid PMML! Attribute \"dataType\" is required as "
                    + "of PMML 3.1. "
                    + "Inferring correct data type from \"optype\"");
            return;
        }
        if (typeName.equals("string")) {
            m_currentType = StringCell.TYPE;
        } else if (typeName.equals("integer") || typeName.equals("boolean")) {
            m_currentType = IntCell.TYPE;
        } else if (typeName.equals("double") || typeName.equals("float")) {
            m_currentType = DoubleCell.TYPE;
        } else {
            throw new SAXException("Found unsupported data type: " + typeName
                    + ". Only string, integer, boolean, double, and float are"
                    + " supported.");
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        if (name.equals("DataField") && m_currentName != null
                && m_currentType != null) {
            DataColumnSpecCreator creator =
                    new DataColumnSpecCreator(m_currentName, m_currentType);
            DataColumnDomainCreator domainCreator =
                    new DataColumnDomainCreator();
            // if we found <code>Value</code> elements in PMML
            if (m_currentValues.size() > 0) {
                // if we have a nominal column
                if (m_currentType.isCompatible(StringValue.class)) {
                    // nominal column -> set values as nominal values
                    Set<DataCell> values = new LinkedHashSet<DataCell>();
                    for (String s : m_currentValues) {
                        values.add(new StringCell(s));
                    }
                    domainCreator.setValues(values);
                    // if we have Value elements in PMML and a numeric column
                } else if (m_currentType.isCompatible(DoubleValue.class)) {
                    // we have to parse the values and determine the lower and
                    // upper bound
                    parseBoundsFromValues();
                }
            }
            if (!Double.isNaN(m_currentMin)) {
                DataCell lowerBound;
                if (m_currentType.isCompatible(IntValue.class)) {
                    lowerBound = new IntCell((int)m_currentMin);
                } else {
                    lowerBound = new DoubleCell(m_currentMin);
                }
                domainCreator.setLowerBound(lowerBound);
            }
            if (!Double.isNaN(m_currentMax)) {
                DataCell upperBound;
                if (m_currentType.isCompatible(IntValue.class)) {
                    upperBound = new IntCell((int)m_currentMax);
                } else {
                    upperBound = new DoubleCell(m_currentMax);
                }
                domainCreator.setUpperBound(upperBound);
            }
            creator.setDomain(domainCreator.createDomain());
            m_colSpecs.add(creator.createSpec());
            // clean up
            resetDataFieldParsing();
        }
        m_elemStack.pop();
    }

    private void resetDataFieldParsing() {
        m_currentMax = Double.NaN;
        m_currentMin = Double.NaN;
        m_currentValues.clear();
        m_currentType = null;
        m_currentName = null;
        m_read = false;
    }

    /**
     * This method is called if for a numeric data field not the interval was
     * used but an enumeration of all occurring values (as for example seen
     * here: <a href="http://dmg.org/pmml_examples/CenterbasedClustering.xml">
     * http://dmg.org/pmml_examples/CenterbasedClustering.xml</a>).
     */
    private void parseBoundsFromValues() throws SAXException {
        if (!Double.isNaN(m_currentMin) || !Double.isNaN(m_currentMax)) {
            throw new SAXException(
                    "Invalid PMML! Found \"Interval\" and \"Value\" element "
                    + "for DataField " + m_currentName);
        }
        List<Double> parsedValues = new ArrayList<Double>();
        for (String s : m_currentValues) {
            // if parsing fails try log warning and try next
            try {
                parsedValues.add(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new SAXException(
                        "Error while parsing enumeration of values "
                        + "for numeric data field \"" + m_currentName + "\"");
            }
        }
        m_currentMin = Collections.min(parsedValues);
        m_currentMax = Collections.max(parsedValues);
    }

    /**
     *
     * @return the loaded dta table spec
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        // ignore character data
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        DataColumnSpec[] array = new DataColumnSpec[m_colSpecs.size()];
        array = m_colSpecs.toArray(array);
        m_spec = new DataTableSpec(array);

        m_colSpecs.clear();
        m_currentMax = Double.NaN;
        m_currentMax = Double.NaN;
        m_currentName = null;
        m_currentType = null;
        m_elemStack.clear();
        m_read = false;
        m_currentValues.clear();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getSupportedVersions() {
        Set<String> versions = new TreeSet<String>();
        versions.add(PMMLPortObject.PMML_V3_0);
        versions.add(PMMLPortObject.PMML_V3_1);
        versions.add(PMMLPortObject.PMML_V3_2);
        return versions;
    }

}
