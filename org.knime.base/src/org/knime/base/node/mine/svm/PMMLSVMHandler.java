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
 *   21.08.2008 (cebron): created
 */
package org.knime.base.node.mine.svm;

import static org.knime.core.node.port.pmml.PMMLPortObject.CDATA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.data.RowKey;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * {@link PMMLContentHandler} for a SVM PMML model.
 *
 * @author cebron, University of Konstanz
 */
public class PMMLSVMHandler extends PMMLContentHandler {

    private StringBuffer m_buffer;

    private final Stack<String> m_elementStack = new Stack<String>();

    private String m_curVecID;

    private Kernel m_kernel;

    private final List<String> m_datafields = new ArrayList<String>();

    private DoubleVector[] m_doubleVectors;

    private int m_counter;

    private String m_curSVMTargetCategory;

    private String m_altSVMTargetCategory;

    private final List<String> m_curSVMVectors = new ArrayList<String>();

    private final List<Double> m_curSVMAlphas = new ArrayList<Double>();

    private double m_curSVMThreshold;

    private final List<String> m_targetValues;

    private final List<Svm> m_svms;

    private int[] m_curIndices;



    /**
     * Creates a new empty svm handler. The initialization has to
     * be performed by registering the handler to a parser.
     */
    public PMMLSVMHandler() {
        super();
        m_svms = new ArrayList<Svm>();
        m_targetValues = new ArrayList<String>();
    }

    /**
     * Creates an initialized svm handler that can be used to
     * output the svm model by invoking
     * {@link #addPMMLModel(org.w3c.dom.DocumentFragment, PMMLPortObjectSpec)}.
     * @param targetValues  the values of the target column
     * @param svms the svms
     * @param kernel the kernel
     */
    public PMMLSVMHandler(final List<String> targetValues,
            final List<Svm> svms, final Kernel kernel) {
        super();
        m_targetValues = targetValues;
        m_kernel = kernel;
        m_svms = svms;
    }


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
        if (m_svms.size() == 1) {
            /* Binary classification case. Only one SVM is needed. */
            Svm first = m_svms.get(0);
            Svm second = new Svm(first.getSupportVectors().clone(),
                    first.getAlphas(), m_altSVMTargetCategory,
                    first.getThreshold() * -1, getKernel());
            m_svms.add(second);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        // if Array -> read buffer out
        m_elementStack.pop();
        /* "Entries" was wrong. This element does not exist in PMML. It is only
         * kept here for compatibility reasons.
         */
        if ((name.equals("REAL-Entries") || name.equals("Entries"))
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
        if ((name.equals("Indices") || name.equals("REAL-Entries")
                || name.equals("Entries"))
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
            // optional and only set in case of binary classification
            m_altSVMTargetCategory = atts.getValue("alternateTargetCategory");
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getSupportedVersions() {
        TreeSet<String> versions = new TreeSet<String>();
        versions.add(PMMLPortObject.PMML_V3_0);
        versions.add(PMMLPortObject.PMML_V3_1);
        versions.add(PMMLPortObject.PMML_V3_2);
        versions.add(PMMLPortObject.PMML_V4_0);
        return versions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addPMMLModelContent(final TransformerHandler handler,
            final PMMLPortObjectSpec spec) throws SAXException {
        // create the SVM model
        // with the attributes...
        AttributesImpl atts = new AttributesImpl();
        // modelName
        atts.addAttribute(null, null, "modelName", CDATA, "SVM");
        // functionName
        atts.addAttribute(null, null, "functionName", CDATA, "classification");
        // algorithmName
        atts.addAttribute(null, null, "algorithmName", CDATA,
                "Sequential Minimal Optimization (SMO)");
        // svmRepresentation
        atts.addAttribute(null, null, "svmRepresentation", CDATA,
                "SupportVectors");
        handler.startElement(null, null, "SupportVectorMachineModel", atts);
        PMMLPortObjectSpec.writeMiningSchema(spec, handler);
        addTargets(handler, spec.getTargetFields().iterator().next(),
                m_targetValues);

        //adding empty local transformations that can be filled later
        handler.startElement(null, null, "LocalTransformations", null);
        handler.endElement(null, null, "LocalTransformations");

        addKernel(handler, m_kernel);
        addVectorDictionary(handler, m_svms, spec.getLearningFields());
        addSVMs(handler, m_svms);
        handler.endElement(null, null, "SupportVectorMachineModel");
    }

    /**
     * Writes the PMML target attributes.
     *
     * @param handler to write to
     * @param classcolName name of the class column.
     * @param targetVals the target values of the SVMs
     * @throws SAXException if something goes wrong.
     */
    protected static void addTargets(final TransformerHandler handler,
            final String classcolName, final List<String> targetVals)
            throws SAXException {
        // open targets schema
        handler.startElement(null, null, "Targets", null);
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "field", CDATA, classcolName);
        atts.addAttribute(null, null, "optype", CDATA, "categorical");
        handler.startElement(null, null, "Target", atts);
        atts = new AttributesImpl();
        for (String target : targetVals) {
            // add target values
            atts.addAttribute(null, null, "value", CDATA, target.toString());
            handler.startElement(null, null, "TargetValue", atts);
            handler.endElement(null, null, "TargetValue");
        }
        // close targets schema
        handler.endElement(null, null, "Target");
        handler.endElement(null, null, "Targets");
    }

    /**
     * Writes the PMML kernel attributes.
     *
     * @param handler to write to.
     * @param kernel the used Kernel of the SVM.
     * @throws SAXException if something goes wrong.
     */
    protected static void addKernel(final TransformerHandler handler,
            final Kernel kernel) throws SAXException {
        switch (kernel.getType()) {
        case Polynomial:
            addPolynomialKernel(handler, kernel);
            return;
        case HyperTangent:
            addHypertangentKernel(handler, kernel);
            return;
        case RBF:
            addRBFKernel(handler, kernel);
            return;
        default:
            throw new SAXException("Can not add Kernel "
                    + kernel.getType().toString());
        }
    }

    /**
     * Writes the PMML polynomial kernel attributes.
     *
     * @param handler to write to.
     * @param kernel the used Kernel of the SVM.
     * @throws SAXException if something goes wrong.
     */
    protected static void addPolynomialKernel(final TransformerHandler handler,
            final Kernel kernel) throws SAXException {

        double bias = kernel.getParameter(0);
        double power = kernel.getParameter(1);
        double gamma = kernel.getParameter(2);
        AttributesImpl atts = new AttributesImpl();

        atts.addAttribute(null, null, "gamma", CDATA, "" + gamma);
        atts.addAttribute(null, null, "coef0", CDATA, "" + bias);
        atts.addAttribute(null, null, "degree", CDATA, "" + power);

        handler.startElement(null, null, "PolynomialKernelType", atts);
        handler.endElement(null, null, "PolynomialKernelType");
    }

    /**
     * Writes the PMML sigmoid kernel attributes.
     *
     * @param handler to write to.
     * @param kernel the used Kernel of the SVM.
     * @throws SAXException if something goes wrong.
     */
    protected static void addHypertangentKernel(
            final TransformerHandler handler,
            final Kernel kernel) throws SAXException {
        double kappa = kernel.getParameter(0);
        double delta = kernel.getParameter(1);

        AttributesImpl atts = new AttributesImpl();

        atts.addAttribute(null, null, "gamma", CDATA, "" + kappa);
        atts.addAttribute(null, null, "coef0", CDATA, "" + delta);

        handler.startElement(null, null, "SigmoidKernelType", atts);
        handler.endElement(null, null, "SigmoidKernelType");
    }

    /**
     * Writes the PMML RBF kernel attributes.
     *
     * @param handler to write to.
     * @param kernel the used Kernel of the SVM.
     * @throws SAXException if something goes wrong.
     */
    protected static void addRBFKernel(final TransformerHandler handler,
            final Kernel kernel) throws SAXException {
        double sigma = kernel.getParameter(0);
        double gamma = 1.0 / (2.0 * (sigma * sigma));
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "gamma", CDATA, "" + gamma);
        handler.startElement(null, null, "RadialBasisKernelType", atts);
        handler.endElement(null, null, "RadialBasisKernelType");
    }

    /**
     * Writes the PMML VectorDictionary containing all support vectors
     * for all support vector machines.
     *
     * @param handler to write to.
     * @param svms the trained Support Vector Machines.
     * @param colNames the column names in the training data.
     * @throws SAXException if something goes wrong.
     */
    protected static void addVectorDictionary(final TransformerHandler handler,
            final List<Svm> svms, final List<String> colNames)
    throws SAXException {
        Set<DoubleVector> supVecs = new LinkedHashSet<DoubleVector>();
        for (Svm svm : svms) {
            supVecs.addAll(Arrays.asList(svm.getSupportVectors()));
        }
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "numberOfVectors", CDATA,
                "" + supVecs.size());

        handler.startElement(null, null, "VectorDictionary", atts);
        atts = new AttributesImpl();
        atts.addAttribute(null, null, "numberOfFields", CDATA,
                "" + colNames.size());
        handler.startElement(null, null, "VectorFields", atts);
        for (String colname : colNames) {
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "field", CDATA, "" + colname);
            handler.startElement(null, null, "FieldRef", atts);
            handler.endElement(null, null, "FieldRef");
        }
        handler.endElement(null, null, "VectorFields");

        for (DoubleVector vector : supVecs) {
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "id", CDATA, vector.getClassValue()
                    + "_" + vector.getKey().getString());
            handler.startElement(null, null, "VectorInstance", atts);
            int nrValues = vector.getNumberValues();
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "n", CDATA, "" + nrValues);
            handler.startElement(null, null, "REAL-SparseArray", atts);
            atts = new AttributesImpl();
            handler.startElement(null, null, "Indices", atts);
            StringBuffer buff = new StringBuffer();
            for (int x = 1; x <= nrValues; x++) {
                buff.append(x);
                if (x < nrValues) {
                    buff.append(" ");
                }
            }
            char[] chars = buff.toString().toCharArray();
            handler.characters(chars, 0, chars.length);
            handler.endElement(null, null, "Indices");
            handler.startElement(null, null, "REAL-Entries", null);
            buff = new StringBuffer();
            for (int x = 0; x < nrValues; x++) {
                double d = vector.getValue(x);
                buff.append(d);
                if (x < nrValues - 1) {
                    buff.append(" ");
                }
            }
            chars = buff.toString().toCharArray();
            handler.characters(chars, 0, chars.length);
            handler.endElement(null, null, "REAL-Entries");
            handler.endElement(null, null, "REAL-SparseArray");
            handler.endElement(null, null, "VectorInstance");

        }
        handler.endElement(null, null, "VectorDictionary");
    }

    /**
     * Writes the PMML support vector machines.
     *
     * @param handler to write to.
     * @param svms the trained support vector machines.
     * @throws SAXException if something goes wrong.
     */
    protected void addSVMs(final TransformerHandler handler,
            final List<Svm> svms)
            throws SAXException {
        if (svms.size() == 0) {
            // create empty support vector machine model
            for (String target : m_targetValues) {
                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute(null, null, "targetCategory", CDATA, target);
                handler.startElement(null, null, "SupportVectorMachine", atts);

                // add an empty support vector
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "numberOfSupportVectors", CDATA,
                        "" + 0);
                handler.startElement(null, null, "SupportVectors", atts);
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "vectorId", CDATA, "dummy");
                handler.startElement(null, null, "SupportVector", atts);
                handler.endElement(null, null, "SupportVector");
                handler.endElement(null, null, "SupportVectors");

                // add an empty coefficient
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "numberOfCoefficients", CDATA,
                        "" + 0);
                handler.startElement(null, null, "Coefficients", atts);
                atts = new AttributesImpl();
                handler.startElement(null, null, "Coefficient", atts);
                handler.endElement(null, null, "Coefficient");
                handler.endElement(null, null, "Coefficients");
                handler.endElement(null, null, "SupportVectorMachine");
            }
        }

        for (Svm svm : svms) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "targetCategory", CDATA, svm
                    .getPositive());
            final boolean binaryClassification = (svms.size() == 2);
            if (binaryClassification) {
                atts.addAttribute(null, null, "alternateTargetCategory", CDATA,
                    svms.get(1).getPositive());
            }
            handler.startElement(null, null, "SupportVectorMachine", atts);

            // add support vectors
            DoubleVector[] supVecs = svm.getSupportVectors();
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "numberOfAttributes", CDATA, ""
                    + supVecs[0].getNumberValues());
            atts.addAttribute(null, null, "numberOfSupportVectors", CDATA, ""
                    + supVecs.length);
            handler.startElement(null, null, "SupportVectors", atts);
            for (int v = 0; v < supVecs.length; v++) {
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "vectorId", CDATA, supVecs[v]
                        .getClassValue()
                        + "_" + supVecs[v].getKey().getString());
                handler.startElement(null, null, "SupportVector", atts);
                handler.endElement(null, null, "SupportVector");
            }

            handler.endElement(null, null, "SupportVectors");

            // add coefficients
            atts = new AttributesImpl();
            double[] alphas = svm.getAlphas();
            atts.addAttribute(null, null, "numberOfCoefficients", CDATA, ""
                    + alphas.length);
            atts.addAttribute(null, null, "absoluteValue", CDATA, ""
                    + svm.getThreshold());
            handler.startElement(null, null, "Coefficients", atts);
            for (int a = 0; a < alphas.length; a++) {
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "value", CDATA, "" + alphas[a]);
                handler.startElement(null, null, "Coefficient", atts);
                handler.endElement(null, null, "Coefficient");
            }

            handler.endElement(null, null, "Coefficients");
            handler.endElement(null, null, "SupportVectorMachine");
            if (binaryClassification) {
                /* Binary classification case. Only one SVM is needed. */
                break;
            }
        }

    }

}
