/*
 *
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 17, 2010 (morent): created
 */
package org.knime.core.node.port.pmml.preproc;

import static org.knime.core.node.port.pmml.preproc.PMMLElements.EXTENSION;
import static org.knime.core.node.port.pmml.preproc.PMMLElements.FIELD;
import static org.knime.core.node.port.pmml.preproc.PMMLElements.LINEAR_NORM;
import static org.knime.core.node.port.pmml.preproc.PMMLElements.NAME;
import static org.knime.core.node.port.pmml.preproc.PMMLElements.NORM;
import static org.knime.core.node.port.pmml.preproc.PMMLElements.NORM_CONT;
import static org.knime.core.node.port.pmml.preproc.PMMLElements.ORIG;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
* TODO: Important: Implementation has not finished yet and this content handler
* is not used so far. We still need other preprocessing content handler, e.g.
* for missing value handling and a master preprocessing content handler that
* delegates the calls.
*
* TODO: Solve problem with dependencies from base (AffineTrans, ...).
*
* @author Dominik Morent, KNIME.com, Zurich, Switzerland
*/
class PMMLLocalTransformationsContentHandler extends PMMLContentHandler {
    /** Constant for CDATA. */
    protected static final String CDATA = "CDATA";
    private static final String SUMMARY = "summary";


   // y = s * x + t
   private List<String> m_fields = new LinkedList<String>();
   private List<Double> m_scale = new LinkedList<Double>();
   private List<Double> m_trans = new LinkedList<Double>();
//   private String m_summary;

   private int m_linNormCnt = 0;
   private double[] m_orig = new double[2];
   private double[] m_norm = new double[2];

   /**
    * {@inheritDoc}
    */
   @Override
   public void startElement(final String uri, final String localName,
           final String name, final Attributes atts) throws SAXException {
       if (name.equals(NORM_CONT)) {
           m_fields.add(atts.getValue(FIELD));
           m_linNormCnt = 0;
       } else if (name.equals(LINEAR_NORM)) {
           m_orig[m_linNormCnt] = Double.parseDouble(atts.getValue(ORIG));
           m_norm[m_linNormCnt] = Double.parseDouble(atts.getValue(NORM));
           m_linNormCnt++;
           if (m_linNormCnt > 2) {
               throw new SAXException("Unexpected " + LINEAR_NORM
                       + " element encountered. Only two elements per "
                       + NORM_CONT + " are allowed.");
           }
       } else if (name.equals(EXTENSION) && atts.getValue(NAME) != null
               && atts.getValue(NAME).equals(SUMMARY)) {
//           m_summary = atts.getValue(VALUE);
       }

   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void endElement(final String uri, final String localName,
           final String name) throws SAXException {
       if (name.equals(NORM_CONT)) {
           m_scale.add(m_norm[0]);
           m_trans.add(m_norm[1] - m_norm[0]);
           m_linNormCnt = 0;
       }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void characters(final char[] ch, final int start,
           final int length) throws SAXException {
       // Ignore. No content is expected.
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void endDocument() throws SAXException {
       double[] nanArray = new double[m_fields.size()];
       Arrays.fill(nanArray, Double.NaN);
       double[] s = new double[m_scale.size()];
       double[] t = new double[m_trans.size()];
       for (int i = 0; i < t.length; i++) {
           s[i] = m_scale.get(i);
           t[i] = m_trans.get(i);
       }
//       m_configuration = new AffineTransConfiguration(
//               m_fields.toArray(new String[m_fields.size()]), s, t,
//               nanArray, nanArray, m_summary);
   }
}
