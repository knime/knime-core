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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   21.08.2008 (cebron): created
 */
package org.knime.base.node.mine.svm;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.data.RowKey;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * {@link PMMLContentHandler} for a SVM PMML model.
 *
 * @author cebron, University of Konstanz
 */
public class PMMLSVMHandler extends PMMLContentHandler {

    private StringBuffer m_buffer;

    private Stack<String> m_elementStack = new Stack<String>();

    private String m_curVecID;

    private Kernel m_kernel;

    private List<String> m_datafields = new ArrayList<String>();

    private DoubleVector[] m_doubleVectors;

    private int m_counter;

    private String m_curSVMTargetCategory;

    private List<String> m_curSVMVectors = new ArrayList<String>();

    private List<Double> m_curSVMAlphas = new ArrayList<Double>();

    private double m_curSVMThreshold;

    private List<String> m_targetValues = new ArrayList<String>();

    private List<Svm> m_svms = new ArrayList<Svm>();

    private int[] m_curIndices;

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        // necessary for inside ArrayElement
        if (m_buffer == null) {
            m_buffer = new StringBuffer();
        }
        m_buffer.append(ch, start, length);

    }

    /**
     * @return the {@link Svm}s in the PMML file.
     */
    public List<Svm> getSVMs() {
        return m_svms;
    }

    /**
     * @return the used {@link Kernel}.
     */
    public Kernel getKernel() {
        return m_kernel;
    }

    /**
     *
     * @return the target values
     */
    public List<String>getTargetValues() {
        return m_targetValues;
    }

        /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        m_buffer = null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        // if Array -> read buffer out
        m_elementStack.pop();
        if (name.equals("REAL-Entries")
                && m_elementStack.peek().equals("REAL-SparseArray")) {
            String[] coords = m_buffer.toString().trim().split(" ");
            ArrayList<Double> suppValues = new ArrayList<Double>();
            for (int i = 0; i < coords.length; i++) {
                suppValues.add(Double.parseDouble(coords[i]));
            }
            ArrayList<Double> suppValuesCleaned = new ArrayList<Double>();
            for (int i = 0; i < m_curIndices.length; i++) {
                suppValuesCleaned.add(suppValues.get(m_curIndices[i]));
            }
            m_doubleVectors[m_counter] =
                    new DoubleVector(new RowKey(m_curVecID), suppValuesCleaned,
                            null);
            m_counter++;
        } else if (name.equals("Indices")
                && m_elementStack.peek().equals("REAL-SparseArray")) {
            String[] indices = m_buffer.toString().trim().split(" ");
            ArrayList<Integer> indValues = new ArrayList<Integer>();
            for (int i = 0; i < indices.length; i++) {
                indValues.add((Integer.parseInt(indices[i])) - 1);
            }
            m_curIndices = new int[indValues.size()];
            for (int i = 0; i < m_curIndices.length; i++) {
                m_curIndices[i] = indValues.get(i);
            }

        }  else if (name.equals("SupportVectorMachine")) {
            // collect the DoubleVectors that belong to this SVM.
            DoubleVector[] svmVecs = new DoubleVector[m_curSVMVectors.size()];
            for (int i = 0; i < svmVecs.length; i++) {
                String id = m_curSVMVectors.get(i);
                for (int j = 0; j < m_doubleVectors.length; j++) {
                    if (m_doubleVectors[j].getKey().getString().equals(id)) {
                        svmVecs[i] = m_doubleVectors[j];
                        String classvalue =
                                id.substring(0, id.lastIndexOf('_'));
                        svmVecs[i].setClassValue(classvalue);
                        break;
                    }
                }
            }
            double[] alpha = new double[m_curSVMAlphas.size()];
            for (int i = 0; i < alpha.length; i++) {
                alpha[i] = m_curSVMAlphas.get(i);
            }
            Svm curSVM =
                    new Svm(svmVecs, alpha, m_curSVMTargetCategory,
                            m_curSVMThreshold, m_kernel);
            m_svms.add(curSVM);

            // clean up variables
            m_curSVMVectors.clear();
            m_curSVMAlphas.clear();
            m_curSVMTargetCategory = "";
            m_curSVMThreshold = 0;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException {
        if ((name.equals("Indices") || name.equals("REAL-Entries"))
                && m_elementStack.peek().equals("REAL-SparseArray")) {
            m_buffer = new StringBuffer();
        } else if (name.equals("PolynomialKernelType")) {
            m_kernel = KernelFactory.getKernel(KernelType.Polynomial);
            double bias = Double.parseDouble(atts.getValue("coef0"));
            double power = Double.parseDouble(atts.getValue("degree"));
            double gamma = Double.parseDouble(atts.getValue("gamma"));
            m_kernel.setParameter(0, bias);
            m_kernel.setParameter(1, power);
            m_kernel.setParameter(2, gamma);
        } else if (name.equals("SigmoidKernelType")) {
            m_kernel = KernelFactory.getKernel(KernelType.HyperTangent);
            double kappa = Double.parseDouble(atts.getValue("gamma"));
            double delta = Double.parseDouble(atts.getValue("coef0"));
            m_kernel.setParameter(0, kappa);
            m_kernel.setParameter(1, delta);
        } else if (name.equals("RadialBasisKernelType")) {
            m_kernel = KernelFactory.getKernel(KernelType.RBF);
            double gamma = Double.parseDouble(atts.getValue("gamma"));
            double sigma = Math.sqrt(1.0 / (2.0 * gamma));
            m_kernel.setParameter(0, sigma);
        } else if (name.equals("VectorInstance")) {
            m_curVecID = atts.getValue("id");
        } else if (name.equals("FieldRef")) {
            m_datafields.add(atts.getValue("fields"));
        } else if (name.equals("VectorDictionary")) {
            int nrVectors = Integer.parseInt(atts.getValue("numberOfVectors"));
            m_doubleVectors = new DoubleVector[nrVectors];
            m_counter = 0;
        } else if (name.equals("SupportVectorMachine")) {
            m_curSVMTargetCategory = atts.getValue("targetCategory");
        } else if (name.equals("SupportVector")) {
            m_curSVMVectors.add(atts.getValue("vectorId"));
        } else if (name.equals("Coefficients")) {
            m_curSVMThreshold =
                    Double.parseDouble(atts.getValue("absoluteValue"));
        } else if (name.equals("Coefficient")) {
            m_curSVMAlphas.add(Double.parseDouble(atts.getValue("value")));
        } else if (name.equals("TargetValue")) {
            m_targetValues.add(atts.getValue("value"));
        } else if (name.equals("SupportVectorMachineModel")) {
            String functionName = atts.getValue("functionName");
            if (!functionName.equals("classification")) {
                throw new SAXException(
                        "KNIME SVM can only handle classification.");
            }
        }
        m_elementStack.push(name);

    }

}
