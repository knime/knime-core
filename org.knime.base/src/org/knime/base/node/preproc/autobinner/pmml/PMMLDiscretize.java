/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   22.06.2010 (hofer): created
 */
package org.knime.base.node.preproc.autobinner.pmml;

import java.util.List;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encapsulates a DiscretizeBin in PMML.
 *
 * @author Heiko Hofer
 */
final public class PMMLDiscretize {
    private String m_field;
    private String m_mapMissingTo;
    private String m_defaultValue;
    private String m_dataType;
    private List<PMMLDiscretizeBin> m_bins;


    /**
     * @param field the column that is discretized
     * @param bins the bins
     */
    public PMMLDiscretize(final String field,
            final List<PMMLDiscretizeBin> bins) {
        this(field, null, null, null, bins);
    }

    /**
     * @param field the column that is discretized
     * @param mapMissingTo value of output when input is the missing value
     * @param defaultValue value of output when input does not match any interval
     * @param dataType data type
     * @param bins the bins
     */
    public PMMLDiscretize(final String field,
            final String mapMissingTo, final String defaultValue,
            final String dataType, final List<PMMLDiscretizeBin> bins) {
        m_field = field;
        m_mapMissingTo = mapMissingTo;
        m_defaultValue = defaultValue;
        m_dataType = dataType;
        m_bins = bins;
    }


    /**
     * @return the field
     */
    public String getField() {
        return m_field;
    }

    /**
     * @return the mapMissingTo
     */
    public String getMapMissingTo() {
        return m_mapMissingTo;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return m_defaultValue;
    }

    /**
     * @return the dataType
     */
    public String getDataType() {
        return m_dataType;
    }

    /**
     * @return the bins
     */
    public List<PMMLDiscretizeBin> getBins() {
        return m_bins;
    }

    /**
     * Writes the PMML to the given handler.
     *
     * @param handler the transformation handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    void writePMML(final TransformerHandler handler)
    throws SAXException {
        AttributesImpl a = new AttributesImpl();
        a.addAttribute("", "", "field", "CDATA", m_field);
        if (null != m_mapMissingTo) {
            a.addAttribute("", "", "mapMissingTo", "CDATA", m_mapMissingTo);
        }
        if (null != m_defaultValue) {
            a.addAttribute("", "", "defaultValue", "CDATA", m_defaultValue);
        }
        if (null != m_dataType) {
            a.addAttribute("", "", "dataType", "CDATA", m_dataType);
        }
        handler.startElement("", "", "Discretize", a);
        for (PMMLDiscretizeBin p : m_bins) {
            p.writePMML(handler);
        }
        handler.endElement("", "", "Discretize");
    }


}
