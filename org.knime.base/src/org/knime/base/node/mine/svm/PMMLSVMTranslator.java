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
 *   16.05.2011 (morent): created
 */
package org.knime.base.node.mine.svm;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml40.CoefficientDocument.Coefficient;
import org.dmg.pmml40.CoefficientsDocument.Coefficients;
import org.dmg.pmml40.MININGFUNCTION;
import org.dmg.pmml40.OPTYPE;
import org.dmg.pmml40.PMMLDocument;
import org.dmg.pmml40.PMMLDocument.PMML;
import org.dmg.pmml40.PolynomialKernelTypeDocument.PolynomialKernelType;
import org.dmg.pmml40.REALSparseArrayDocument.REALSparseArray;
import org.dmg.pmml40.RadialBasisKernelTypeDocument.RadialBasisKernelType;
import org.dmg.pmml40.SVMREPRESENTATION;
import org.dmg.pmml40.SigmoidKernelTypeDocument.SigmoidKernelType;
import org.dmg.pmml40.SupportVectorDocument.SupportVector;
import org.dmg.pmml40.SupportVectorMachineDocument.SupportVectorMachine;
import org.dmg.pmml40.SupportVectorMachineModelDocument.SupportVectorMachineModel;
import org.dmg.pmml40.SupportVectorsDocument.SupportVectors;
import org.dmg.pmml40.TargetDocument.Target;
import org.dmg.pmml40.TargetValueDocument.TargetValue;
import org.dmg.pmml40.VectorDictionaryDocument.VectorDictionary;
import org.dmg.pmml40.VectorFieldsDocument.VectorFields;
import org.dmg.pmml40.VectorInstanceDocument.VectorInstance;
import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * A SVM translator class between KNIME and PMML.
 *
 * @author Dominik Morent, KNIME.com GmbH, Zurich, Switzerland
 * @author wenlin, Zementis, Apr 2011
 *
 */
public class PMMLSVMTranslator implements PMMLTranslator {
    /**
     *
     */
    private static final String CLASS_KEY_SEPARATOR = "_";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PMMLSVMTranslator.class);

    private Kernel m_kernel;

    private final List<String> m_targetValues;

    private final List<Svm> m_svms;

    private DerivedFieldMapper m_nameMapper;

    /**
     * Creates a new empty PMML SVM translator. The initialization has to be
     * performed by calling the {@link #initializeFrom(PMMLDocument)} method.
     */
    public PMMLSVMTranslator() {
        super();
        m_svms = new ArrayList<Svm>();
        m_targetValues = new ArrayList<String>();
    }

    /**
     * @param targetValues the values of the target column
     * @param svms the svms
     * @param kernel the kernel
     */
    public PMMLSVMTranslator(final List<String> targetValues,
            final List<Svm> svms, final Kernel kernel) {
        super();
        m_targetValues = targetValues;
        m_kernel = kernel;
        m_svms = svms;
    }

    /**
     * /**
     *
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
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        SupportVectorMachineModel[] models =
                pmmlDoc.getPMML().getSupportVectorMachineModelArray();
        if (models.length == 0) {
            throw new IllegalArgumentException(
                    "No support vector machine model" + " provided.");
        } else if (models.length > 1) {
            LOGGER.warn("Multiple support vector machine models found. "
                    + "Only the first model is considered.");
        }
        SupportVectorMachineModel svmModel = models[0];

        initKernel(svmModel);

        initSVMs(svmModel);
    }

    /**
     * @param svmModel
     */
    private void initKernel(final SupportVectorMachineModel svmModel) {
        PolynomialKernelType polynomialKernel =
                svmModel.getPolynomialKernelType();
        SigmoidKernelType sigmoidKernel = svmModel.getSigmoidKernelType();
        RadialBasisKernelType radialBasisKernel =
                svmModel.getRadialBasisKernelType();

        if (polynomialKernel != null) {
            m_kernel = KernelFactory.getKernel(KernelType.Polynomial);
            m_kernel.setParameter(0, polynomialKernel.getCoef0());
            m_kernel.setParameter(1, polynomialKernel.getDegree());
            m_kernel.setParameter(2, polynomialKernel.getGamma());
        } else if (sigmoidKernel != null) {
            m_kernel = KernelFactory.getKernel(KernelType.HyperTangent);
            m_kernel.setParameter(0, sigmoidKernel.getGamma()); // kappa
            m_kernel.setParameter(1, sigmoidKernel.getCoef0()); // delta
        } else if (radialBasisKernel != null) {
            m_kernel = KernelFactory.getKernel(KernelType.RBF);
            double gamma = radialBasisKernel.getGamma();
            double sigma = Math.sqrt(1.0 / (2.0 * gamma));
            m_kernel.setParameter(0, sigma);
        } else {
            LOGGER.error("No supported kernel type found. Supported types are "
                    + KernelType.values());
        }
    }

    /**
     * @param svmModel
     */
    private void initSVMs(final SupportVectorMachineModel svmModel) {
        final Map<String, ArrayList<Double>> vectors =
                new HashMap<String, ArrayList<Double>>();
        for (VectorInstance vectorInstance : svmModel.getVectorDictionary()
                .getVectorInstanceArray()) {
            REALSparseArray sparseArray = vectorInstance.getREALSparseArray1();
            ArrayList<Double> values = new ArrayList<Double>();
            for (Object realEntry : sparseArray.getREALEntries()) {
                values.add((Double)realEntry);
            }
            vectors.put(vectorInstance.getId(), values);
        }

        for (SupportVectorMachine supportVectorMachine : svmModel
                .getSupportVectorMachineArray()) {
            // collect support vectors
            SupportVectors svs = supportVectorMachine.getSupportVectors();
            DoubleVector[] supportVectors =
                   new DoubleVector[svs.getNumberOfSupportVectors().intValue()];
            SupportVector[] supportVectorArray = svs.getSupportVectorArray();
            for (int i = 0; i < supportVectorArray.length; i++) {
                SupportVector supportVector = supportVectorArray[i];
                String vectorId = supportVector.getVectorId();
                String classValue = getClassValue(vectorId);
                supportVectors[i] =
                        new DoubleVector(vectors.get(vectorId), classValue);
            }
            Coefficients coef = supportVectorMachine.getCoefficients();
            double threshold = coef.getAbsoluteValue();

            // collect coefficients
            Coefficient[] coefficientArray = coef.getCoefficientArray();
            double[] alpha = new double[coefficientArray.length];
            for (int i = 0; i < coefficientArray.length; i++) {
                /** The alpha in KNIME is always positive. When calculating the
                 * distance it is multiplied with a target factor that is 1 or
                 * -1 depending on whether we have a positive example or not.
                 * (see {@link SVM#getTargetAlphas()} for details)
                 */
                alpha[i] = Math.abs(coefficientArray[i].getValue());
            }
            m_svms.add(new Svm(supportVectors, alpha, supportVectorMachine
                    .getTargetCategory(), threshold, m_kernel));

            /* The KNIME internal representation requires two SVMs for the
             * binary classification case. Therefore add a second SVM with the
             * same configuration as the first one except for the negative
             * threshold. */
            if (svmModel.getSupportVectorMachineArray().length == 1) {
                m_svms.add(new Svm(supportVectors.clone(), alpha,
                        supportVectorMachine.getAlternateTargetCategory(),
                        threshold * -1, m_kernel));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc,
            final PMMLPortObjectSpec spec) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        PMML pmml = pmmlDoc.getPMML();
        SupportVectorMachineModel svmModel =
                pmml.addNewSupportVectorMachineModel();
        PMMLMiningSchemaTranslator.writeMiningSchema(spec, svmModel);

        // add support vector machine model attributes
        svmModel.setModelName("SVM");
        svmModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);
        svmModel.setAlgorithmName("Sequential Minimal Optimization (SMO)");
        svmModel.setSvmRepresentation(SVMREPRESENTATION.SUPPORT_VECTORS);

        addTargets(svmModel, spec.getTargetFields().get(0));

        addKernel(svmModel);

        addVectorDictionary(svmModel, spec.getLearningFields());

        addSVMs(svmModel);

        return SupportVectorMachineModel.type;
    }

    /**
     * Writes the PMML target attributes.
     *
     * @param svmModel the SVM model to add the targets to
     * @param classColName name of the class column.
     */
    private void addTargets(final SupportVectorMachineModel svmModel,
            final String classColName) {

        Target target = svmModel.addNewTargets().addNewTarget();
        target.setField(classColName);
        target.setOptype(OPTYPE.CATEGORICAL);

        // add target values
        for (String tv : m_targetValues) {
            TargetValue targetValue = target.addNewTargetValue();
            targetValue.setValue(tv);
        }
    }

    /**
     * Adds the Kernel to the SVM model.
     *
     * @param svmModel the SVM model to add the kernel to
     */
    private void addKernel(final SupportVectorMachineModel svmModel) {
        switch (m_kernel.getType()) {
        case Polynomial:
            PolynomialKernelType pmmlPolynomialKernelType =
                    svmModel.addNewPolynomialKernelType();
            pmmlPolynomialKernelType.setCoef0(m_kernel.getParameter(0));
            pmmlPolynomialKernelType.setDegree(m_kernel.getParameter(1));
            pmmlPolynomialKernelType.setGamma(m_kernel.getParameter(2));
            break;
        case HyperTangent:
            SigmoidKernelType pmmlSigmoidKernelType =
                    svmModel.addNewSigmoidKernelType();
            pmmlSigmoidKernelType.setGamma(m_kernel.getParameter(0));
            pmmlSigmoidKernelType.setCoef0(m_kernel.getParameter(1));
            break;
        case RBF:
            double sigma = m_kernel.getParameter(0);
            double gamma = 1.0 / (2.0 * (sigma * sigma));
            RadialBasisKernelType pmmlRadialBasisKernelType =
                    svmModel.addNewRadialBasisKernelType();
            pmmlRadialBasisKernelType.setGamma(gamma);
            break;
        }
    }

    /**
     * Adds the vector dictionary to the SVM model.
     *
     * @param svmModel the SVM model to add the dictionary to
     * @param learningCol a list with the names of the learning columns
     */
    private void addVectorDictionary(final SupportVectorMachineModel svmModel,
            final List<String> learningCol) {
        VectorDictionary dict = svmModel.addNewVectorDictionary();

        Set<DoubleVector> supportVectors = new LinkedHashSet<DoubleVector>();
        for (Svm svm : m_svms) {
            supportVectors.addAll(Arrays.asList(svm.getSupportVectors()));
        }
        dict.setNumberOfVectors(BigInteger.valueOf(supportVectors.size()));

        VectorFields vectorFields = dict.addNewVectorFields();
        vectorFields.setNumberOfFields(BigInteger.valueOf(learningCol.size()));

        for (String field : learningCol) {
            vectorFields.addNewFieldRef().setField(
                    m_nameMapper.getDerivedFieldName(field));
        }

        for (DoubleVector vector : supportVectors) {
            VectorInstance vectorInstance = dict.addNewVectorInstance();
            vectorInstance.setId(vector.getClassValue() + CLASS_KEY_SEPARATOR
                    + vector.getKey().getString());
            REALSparseArray pmmlRealSparseArray =
                    vectorInstance.addNewREALSparseArray1();
            int nrValues = vector.getNumberValues();
            pmmlRealSparseArray.setN(BigInteger.valueOf(nrValues));

            // set Indices and Entries
            List<String> indicesList = new ArrayList<String>();
            List<String> entriesList = new ArrayList<String>();
            for (int i = 1; i <= nrValues; i++) {
                indicesList.add(String.valueOf(i));
                entriesList.add(String.valueOf(vector.getValue(i - 1)));
            }
            pmmlRealSparseArray.setIndices(indicesList);
            pmmlRealSparseArray.setREALEntries(entriesList);
        }
    }

    /**
     * @param svmModel the SVM model to add the SVMs to
     */
    private void addSVMs(final SupportVectorMachineModel svmModel) {
        if (m_svms.size() == 0) {
            svmModel.addNewSupportVectorMachine();
            // TODO Review what is necessary for the case of an empty model
//            for (String target : m_targetValues) {
//                SupportVectorMachine svm =
//                        svmModel.addNewSupportVectorMachine();
//                svm.setTargetCategory(target);
//                SupportVectors supportVectors = svm.addNewSupportVectors();
//                supportVectors.setNumberOfSupportVectors(
//                      BigInteger.valueOf(0));
//            }
        } else {
            for (Svm svm : m_svms) {
                SupportVectorMachine pmmlSvm =
                        svmModel.addNewSupportVectorMachine();
                pmmlSvm.setTargetCategory(svm.getPositive());

                final boolean binaryClassification = (m_svms.size() == 2);
                if (binaryClassification) {
                    pmmlSvm.setAlternateTargetCategory(m_svms.get(1)
                            .getPositive());
                }

                // add support vectors
                SupportVectors pmmlSupportVectors =
                        pmmlSvm.addNewSupportVectors();

                DoubleVector[] supVectors = svm.getSupportVectors();
                pmmlSupportVectors.setNumberOfAttributes(BigInteger
                        .valueOf(supVectors[0].getNumberValues()));
                pmmlSupportVectors.setNumberOfSupportVectors(BigInteger
                        .valueOf(supVectors.length));

                for (int i = 0; i < supVectors.length; i++) {
                    SupportVector pmmlSupportVector =
                            pmmlSupportVectors.addNewSupportVector();
                    pmmlSupportVector
                            .setVectorId(getSupportVectorId(supVectors[i]));
                }

                // ----------------------------------------
                // add coefficients
                Coefficients pmmlCoefficients = pmmlSvm.addNewCoefficients();
                double[] alphas = svm.getTargetAlphas();
                pmmlCoefficients.setNumberOfCoefficients(BigInteger
                        .valueOf(alphas.length));
                pmmlCoefficients.setAbsoluteValue(svm.getThreshold());

                for (int i = 0; i < alphas.length; i++) {
                    Coefficient pmmlCoefficient =
                            pmmlCoefficients.addNewCoefficient();
                    /* KNIME defines the winner as the positive side of the
                     * threshold, but the DMG defines the winner as the negative
                     * side of the threshold. Therefore, to avoid changing KNIME
                     * algorithm, we need to add an additional minus sign for
                     * each svm output. When importing the PMML into KNIME
                     * the absolute value of the alphas is read. Hence the
                     * negative sign in the PMML alpha has no influence on
                     * the KNIME model.*/
                    pmmlCoefficient.setValue(-1 * alphas[i]);
                }
                if (binaryClassification) {
                    /* Binary classification case. Only one SVM is needed. */
                    break;
                }
            }
        }

    }

    private static String getSupportVectorId(final DoubleVector supportVector) {
        return supportVector.getClassValue() + CLASS_KEY_SEPARATOR
                + supportVector.getKey().getString();
    }

    private static String getClassValue(final String vectorId) {
        return vectorId.substring(0, vectorId.lastIndexOf(CLASS_KEY_SEPARATOR));
    }

}
