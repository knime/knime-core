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

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.knime.core.node.NodeLogger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class MiningSchemaContentHandler extends PMMLContentHandler {

    private static final Set<String>UNSUPPORTED
            = new LinkedHashSet<String>();

    private static final Set<String>IGNORED
            = new LinkedHashSet<String>();


    static {
        UNSUPPORTED.add("order");
        UNSUPPORTED.add("group");

        IGNORED.add("supplementary");
        IGNORED.add("frequencyWeight");
        IGNORED.add("analysisWeight");
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MiningSchemaContentHandler.class);

    private final List<String>m_learningFields;
    private final List<String>m_ignoredFields;
    private final List<String>m_targetFields;

    /* Stores if we are processing a model produced with an KNIME version
     * < 2.3.3. See below for details. */
    private boolean m_oldKNIMESchema = false;

    /** ID of this handler. */
    public static final String ID = "MiningSchemaContentHandler";

    /**
     *
     */
    public MiningSchemaContentHandler() {
        m_learningFields = new LinkedList<String>();
        m_ignoredFields = new LinkedList<String>();
        m_targetFields = new LinkedList<String>();
    }

    /**
     *
     * @return the names of the columns used for learning
     */
    public List<String>getLearningFields() {
        return m_learningFields;
    }

    /**
     *
     * @return the names of the ignored columns
     */
    public List<String>getIgnoredFields() {
        return m_ignoredFields;
    }

    /**
     *
     * @return the names of the target columns
     */
    public List<String>getTargetFields() {
        return m_targetFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch,
            final int start, final int length)
            throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri,
            final String localName, final String name)
            throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri,
            final String localName, final String name,
            final Attributes atts) throws SAXException {
        if ("MiningField".equals(name)) {
            // get attributes
            // check for unsupported attributes:
            if (atts.getValue("missingValueReplacement") != null) {
                LOGGER.warn("\"missingValueReplacement\" is not supported and "
                        + "will be ignored. Skipping it");
            }
            if (atts.getValue("missingValueTreatment") != null) {
                LOGGER.warn("\"missingValueTreatment\" is not supported and "
                        + "will be ignored. Skipping it");
            }
            if (atts.getValue("outliers") != null) {
                LOGGER.warn("\"outliers\" is not supported and "
                        + "will be ignored. Skipping it");
            }
            String colName = atts.getValue("name");
            String treatment = atts.getValue("invalidValueTreatment");
            if (!((m_oldKNIMESchema && treatment == null)
                    || "asIs".equalsIgnoreCase(treatment))) {
                String treatmentText = treatment == null
                        ? "<default>" : treatment;
                String msg = "MiningField \"" + colName + "\": Only \"asIs\" "
                        + "is supported for invalidValueTreatment. "
                        + "invalidValueTreatment=\""
                        + treatmentText +  "\" is treated as \"asIs\".";
                /* At this point the predition does not
                 * give the expected result for outliers (invalid values) from
                 * a PMML point of view. But as this is very restrictive and
                 * causes the RtoPMML functionality to fail and might be
                 * unnecessary if there are no outliers. Hence only a warning
                 * message is issued.
                 * TODO: Extend the functionality of the PMML predictors to
                 * support more invalid value treatment strategies. */
//              throw new RuntimeException(msg);
                LOGGER.warn(msg);
            }
            String usageType = atts.getValue("usageType");
            if (usageType == null) {
                usageType = "active";
            }
            if ("active".equals(usageType)) {
                m_learningFields.add(colName);
            } else if (IGNORED.contains(usageType)) {
                m_ignoredFields.add(colName);
                LOGGER.info("Field \"" + colName + "\" with usage type \""
                        + usageType + "\" has been ignored as it is not "
                        + "needed for scoring.");
            } else if (UNSUPPORTED.contains(usageType)) {
                throw new IllegalArgumentException(
                        "Fields with usage type \""
                        + usageType + "\" are not supported.");
            }
        } else if ("Application".equals(name)) {
            /* This check is only necessary to stay backward compatible. Before
             * KNIME 2.3.3 we did not set the invalidValueTreatment attribute of
             * the MiningField which defaults to "returnInvalid" but treated it
             * as "asIs". To maintain this behavior "old" schemes produced by
             * KNIME take "asIs" as default if the attribute is not specified.
             */
            if ("KNIME".equals(atts.getValue("name"))) {
                String version = atts.getValue("version");
                m_oldKNIMESchema = isOlderThanVersion233(version);
            }

        }
    }

    private boolean isOlderThanVersion233(final String version) {
        if (version != null) {
            try {
                StringTokenizer token = new StringTokenizer(
                        version, ".");
                for (int v  : new Integer[]{2, 3, 3}) {
                    if (!token.hasMoreTokens()) {
                        /* The parsed version is less specific and therefore
                         * older. */
                        return true;
                    }
                    int parsedRev = Integer.parseInt(token.nextToken());
                    if (parsedRev > v) {
                        return false;
                    } else if (parsedRev < v) {
                        return true;
                    } /* else we have the same version so far and
                        continue */
                }
            } catch (NumberFormatException e) {
               /* An invalid version string is not older. */
            }
        }
        return false;
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
        versions.add(PMMLPortObject.PMML_V4_0);
        return versions;
    }

}
