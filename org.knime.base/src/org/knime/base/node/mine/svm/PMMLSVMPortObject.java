/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   15.08.2008 (cebron): created
 */
package org.knime.base.node.mine.svm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A {@link PMMLPortObject} responsible for transforming a SupportVectorMachine
 * {@link Svm} into a PMML file.
 *
 * @author cebron, University of Konstanz
 */
public class PMMLSVMPortObject extends PMMLPortObject {

    /* DataColumnSpec of the class column. */
    private String[] m_targetValues;

    /* The underlying kernel function */
    private Kernel m_kernel;

    /* The Svm's (one for each class) */
    private Svm[] m_svms;

    /**
     * PMML SVM port type.
     */
    public static final PortType TYPE = new PortType(PMMLSVMPortObject.class);

    /**
     * @return the kernel
     */
    public Kernel getKernel() {
        return m_kernel;
    }

    /**
     * @param kernel the kernel to set
     */
    public void setKernel(final Kernel kernel) {
        m_kernel = kernel;
    }

    /**
     * @return the svms
     */
    public Svm[] getSvms() {
        return m_svms;
    }

    /**
     * @param svms the svms to set
     */
    public void setSvms(final Svm[] svms) {
        m_svms = svms;
    }

    /**
     * Empty constructor.
     */
    public PMMLSVMPortObject() {
        // I'm empty.
    }

    /**
     * Constructor.
     * @param spec the {@link PMMLPortObjectSpec} of the training table.
     * @param kernel the {@link Kernel} of the {@link Svm}.
     * @param svms the trained {@link Svm}s.
     */
    public PMMLSVMPortObject(final PMMLPortObjectSpec spec,
            final Kernel kernel, final Svm... svms) {
        super(spec, PMMLModelType.SupportVectorMachineModel);
        m_kernel = kernel;

        ArrayList<String> usedColsList = new ArrayList<String>();
        for (DataColumnSpec colspec : spec.getLearningCols()) {
            if (colspec.getType().isCompatible(DoubleValue.class)) {
                usedColsList.add(colspec.getName());
            }
        }

        Set<DataColumnSpec> targetCols = spec.getTargetCols();
        assert (targetCols.size() == 1)
            : "Only one target column allowed in SVM";
        DataColumnSpec classcolspec = targetCols.iterator().next();
        Set<DataCell> possVals = classcolspec.getDomain().getValues();
        m_targetValues = new String[possVals.size()];
        int counter = 0;
        for (DataCell cell : possVals) {
            m_targetValues[counter] = ((StringValue)cell).getStringValue();
            counter++;
        }
        m_svms = svms;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writePMMLModel(final TransformerHandler handler)
            throws SAXException {
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
        PMMLPortObjectSpec.writeMiningSchema(getSpec(), handler);
        addTargets(handler, getSpec().getTargetFields().iterator().next(),
                m_targetValues);
        addKernel(handler, m_kernel);
        addVectorDictionary(handler, m_svms, getSpec().getLearningFields());
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
    protected void addTargets(final TransformerHandler handler,
            final String classcolName, final String[] targetVals)
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
    protected void addKernel(final TransformerHandler handler,
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
    protected void addPolynomialKernel(final TransformerHandler handler,
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
    protected void addHypertangentKernel(final TransformerHandler handler,
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
    protected void addRBFKernel(final TransformerHandler handler,
            final Kernel kernel) throws SAXException {
        double sigma = kernel.getParameter(0);
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "gamma", CDATA, "" + sigma);
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
    protected void addVectorDictionary(final TransformerHandler handler,
            final Svm[] svms, final Set<String> colNames) throws SAXException {
        DoubleVector[][] supVecs = new DoubleVector[svms.length][];
        int totalNumber = 0;
        for (int i = 0; i < svms.length; i++) {
            supVecs[i] = svms[i].getSupportVectors();
            totalNumber += supVecs[i].length;
        }
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "numberOfVectors", CDATA, ""
                + totalNumber);

        handler.startElement(null, null, "VectorDictionary", atts);
        atts = new AttributesImpl();
        atts.addAttribute(null, null, "numberOfFields", CDATA, "" + 1);
        handler.startElement(null, null, "VectorFields", atts);
        for (String colname : colNames) {
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "field", CDATA, "" + colname);
            handler.startElement(null, null, "FieldRef", atts);
            handler.endElement(null, null, "FieldRef");
        }
        handler.endElement(null, null, "VectorFields");

        for (int i = 0; i < supVecs.length; i++) {
            for (int j = 0; j < supVecs[i].length; j++) {
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "id", CDATA, svms[i]
                        .getPositive()
                        + "_" + supVecs[i][j].getKey().getString());
                handler.startElement(null, null, "VectorInstance", atts);
                int nrValues = supVecs[i][j].getNumberValues();
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
                handler.startElement(null, null, "Entries", null);
                buff = new StringBuffer();
                for (int x = 0; x < nrValues; x++) {
                    double d = supVecs[i][j].getValue(x);
                    buff.append(d);
                    if (x < nrValues - 1) {
                        buff.append(" ");
                    }
                }
                chars = buff.toString().toCharArray();
                handler.characters(chars, 0, chars.length);
                handler.endElement(null, null, "Entries");
                handler.endElement(null, null, "REAL-SparseArray");
                handler.endElement(null, null, "VectorInstance");

            }
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
    protected void addSVMs(final TransformerHandler handler, final Svm[] svms)
            throws SAXException {
        if (svms.length == 0) {
            // create empty support vector machine model
            for (String target : m_targetValues) {
                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute(null, null, "targetCategory", CDATA, target);
                handler.startElement(null, null, "SupportVectorMachine", atts);

                // add coefficients
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
        for (int s = 0; s < svms.length; s++) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "targetCategory", CDATA, svms[s]
                    .getPositive());
            handler.startElement(null, null, "SupportVectorMachine", atts);

            // add support vectors
            DoubleVector[] supVecs = svms[s].getSupportVectors();
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "numberOfAttributes", CDATA, ""
                    + supVecs[0].getNumberValues());
            atts.addAttribute(null, null, "numberOfSupportVectors", CDATA, ""
                    + supVecs.length);
            handler.startElement(null, null, "SupportVectors", atts);
            for (int v = 0; v < supVecs.length; v++) {
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "vectorId", CDATA, svms[s]
                        .getPositive()
                        + "_" + supVecs[v].getKey().getString());
                handler.startElement(null, null, "SupportVector", atts);
                handler.endElement(null, null, "SupportVector");
            }

            handler.endElement(null, null, "SupportVectors");

            // add coefficients
            atts = new AttributesImpl();
            double[] alphas = svms[s].getAlphas();
            atts.addAttribute(null, null, "numberOfCoefficients", CDATA, ""
                    + alphas.length);
            atts.addAttribute(null, null, "absoluteValue", CDATA, ""
                    + svms[s].getThreshold());
            handler.startElement(null, null, "Coefficients", atts);
            for (int a = 0; a < alphas.length; a++) {
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "value", CDATA, "" + alphas[a]);
                handler.startElement(null, null, "Coefficient", atts);
                handler.endElement(null, null, "Coefficient");
            }

            handler.endElement(null, null, "Coefficients");
            handler.endElement(null, null, "SupportVectorMachine");
        }

    }

    /** {@inheritDoc} */
    @Override
    public void loadFrom(final PMMLPortObjectSpec spec, final InputStream in,
            final String version)
            throws ParserConfigurationException, SAXException, IOException {
        PMMLSVMHandler hdl = new PMMLSVMHandler();
        super.addPMMLContentHandler("SVM", hdl);
        super.loadFrom(spec, in, version);
        hdl = (PMMLSVMHandler)super.getPMMLContentHandler("SVM");
        ArrayList<Svm> svmlist = hdl.getSVMs();
        m_svms = new Svm[svmlist.size()];
        m_svms = svmlist.toArray(m_svms);
        m_kernel = hdl.getKernel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("PMML SVM Object.\n");
        buffer.append("Number of SVM's: " + m_svms.length + "\n");
        buffer.append("Kernel: " + m_kernel.getType().toString());
        return buffer.toString();
    }

}
