/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
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
 * -------------------------------------------------------------------
 *
 * History
 *   27.10.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.rprop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.knime.base.data.neural.Architecture;
import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.base.data.neural.methods.RProp;
import org.knime.base.node.mine.neural.mlp.PMMLNeuralNetworkPortObject;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * RPropNodeModel trains a MultiLayerPerceptron with resilient backpropagation.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class RPropNodeModel extends GenericNodeModel {
    /**
     * Inport of the NodeModel for the examples.
     */
    public static final int INPORT = 0;

    /**
     * The maximum number of possible iterations.
     */
    public static final int MAXNRITERATIONS = 1000000;

    /**
     * The default number of iterations.
     */
    public static final int DEFAULTITERATIONS = 100;

    /**
     * The default number of iterations.
     */
    public static final int DEFAULTHIDDENLAYERS = 1;

    /**
     * The default number of iterations.
     */
    public static final int DEFAULTNEURONSPERLAYER = 10;

    /**
     * Key to store the number of maximum iterations.
     */
    public static final String MAXITER_KEY = "maxiter";

    /**
     * Key to store whether missing values should be ignoted.
     */
    public static final String IGNOREMV_KEY = "ignoremv";

    /**
     * Key to store the number of hidden layer.
     */
    public static final String HIDDENLAYER_KEY = "hiddenlayer";

    /**
     * Key to store the number of neurons per hidden layer.
     */
    public static final String NRHNEURONS_KEY = "nrhiddenneurons";

    /**
     * Key to store the class column.
     */
    public static final String CLASSCOL_KEY = "classcol";

    /*
     * Number of iterations.
     */
    private final SettingsModelIntegerBounded m_nrIterations =
            new SettingsModelIntegerBounded(
            /* config-name: */RPropNodeModel.MAXITER_KEY,
            /* default */DEFAULTITERATIONS,
            /* min: */1,
            /* max: */RPropNodeModel.MAXNRITERATIONS);

    /*
     * Number of hidden layers.
     */
    private final SettingsModelIntegerBounded m_nrHiddenLayers =
            new SettingsModelIntegerBounded(
            /* config-name: */RPropNodeModel.HIDDENLAYER_KEY,
            /* default */DEFAULTHIDDENLAYERS,
            /* min: */1,
            /* max: */100);

    /*
     * Number of hidden neurons per layer.
     */
    private final SettingsModelIntegerBounded m_nrHiddenNeuronsperLayer =
            new SettingsModelIntegerBounded(
            /* config-name: */RPropNodeModel.NRHNEURONS_KEY,
            /* default */DEFAULTNEURONSPERLAYER,
            /* min: */1,
            /* max: */100);

    /*
     * The class column.
     */
    private final SettingsModelString m_classcol = new SettingsModelString(
    /* config-name: */RPropNodeModel.CLASSCOL_KEY, null);

    /*
     * Flag whether to ignore missing values
     */
    private final SettingsModelBoolean m_ignoreMV = new SettingsModelBoolean(
    /* config-name: */RPropNodeModel.IGNOREMV_KEY,
    /* default */false);

    /*
     * Flag for regression
     */
    private boolean m_regression;

    /*
     * The internal Neural Network.
     */
    private MultiLayerPerceptron m_mlp;

    /*
     * The architecture of the Neural Network.
     */
    private Architecture m_architecture;

    /*
     * Maps the values from the classes to the output neurons.
     */
    private HashMap<DataCell, Integer> m_classmap;

    /*
     * Maps the values from the inputs to the input neurons.
     */
    private HashMap<String, Integer> m_inputmap;

    /*
     * Used to plot the error.
     */
    // private ErrorPlot m_errorplot;
    /*
     * The error values at each iteration
     */
    private double[] m_errors;

    /**
     * The RPropNodeModel has 2 inputs, one for the positive examples and one
     * for the negative ones. The output is the model of the constructed and
     * trained neural network.
     *
     */
    public RPropNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{PMMLNeuralNetworkPortObject.TYPE});
        m_architecture = new Architecture();
        m_mlp = new MultiLayerPerceptron();
    }

    /**
     * returns null.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_classcol.getStringValue() != null) {
            Set<String> learningCols = new HashSet<String>();
            Set<String> ignoredCols = new HashSet<String>();
            Set<String> targetCols = new HashSet<String>();
            boolean classcolinspec = false;
            for (DataColumnSpec colspec : (DataTableSpec)inSpecs[INPORT]) {
                if (!(colspec.getName().toString().compareTo(
                        m_classcol.getStringValue()) == 0)) {
                    if (!colspec.getType().isCompatible(DoubleValue.class)) {
                        throw new InvalidSettingsException(
                                "Only double columns for input");
                    } else {
                        learningCols.add(colspec.getName());
                        DataColumnDomain domain = colspec.getDomain();
                        if (domain.hasBounds()) {
                            double lower =
                                    ((DoubleValue)domain.getLowerBound())
                                            .getDoubleValue();
                            double upper =
                                    ((DoubleValue)domain.getUpperBound())
                                            .getDoubleValue();
                            if (lower < 0 || upper > 1) {
                                setWarningMessage("Input data not normalized."
                                        + " Please consider using the "
                                        + "Normalizer Node first.");
                            }
                        }
                    }
                } else {
                    targetCols.add(colspec.getName());
                    classcolinspec = true;
                    // check for regression
                    if (colspec.getType().isCompatible(DoubleValue.class)) {
                        // check if the values are in range [0,1]
                        DataColumnDomain domain = colspec.getDomain();
                        if (domain.hasBounds()) {
                            double lower =
                                    ((DoubleValue)domain.getLowerBound())
                                            .getDoubleValue();
                            double upper =
                                    ((DoubleValue)domain.getUpperBound())
                                            .getDoubleValue();
                            if (lower < 0 || upper > 1) {
                                throw new InvalidSettingsException(
                                        "Domain range for regression in column "
                                                + colspec.getName()
                                                + " not in range [0,1]");
                            }
                        }
                    }
                }
            }
            if (!classcolinspec) {
                throw new InvalidSettingsException("Class column "
                        + m_classcol.getStringValue()
                        + " not found in DataTableSpec");
            }
            PMMLPortObjectSpec pmmlspec =
                    new PMMLPortObjectSpec((DataTableSpec)inSpecs[0],
                            learningCols, ignoredCols, targetCols);
            return new PortObjectSpec[]{pmmlspec};
        } else {
            throw new InvalidSettingsException("Class column not set");
        }
    }

    /**
     * The execution consists of three steps:
     * <ol>
     * <li>A neural network is build with the inputs and outputs according to
     * the input datatable, number of hidden layers as specified.</li>
     * <li>Input DataTables are converted into double-arrays so they can be
     * attached to the neural net.</li>
     * <li>The neural net is trained.</li>
     * </ol>
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        // If class column is not set, it is the last column.
        DataTableSpec posSpec = (DataTableSpec)inData[INPORT].getSpec();
        if (m_classcol.getStringValue() == null) {
            m_classcol.setStringValue(posSpec.getColumnSpec(
                    posSpec.getNumColumns() - 1).getName());
        }
        Set<String> learningCols = new HashSet<String>();
        Set<String> ignoredCols = new HashSet<String>();
        Set<String> targetCols = new HashSet<String>();

        // Determine the number of inputs and the number of outputs. Make also
        // sure that the inputs are double values.
        int nrInputs = 0;
        int nrOutputs = 0;
        m_inputmap = new HashMap<String, Integer>();
        for (DataColumnSpec colspec : posSpec) {
            // check for class column
            if (colspec.getName().toString().compareTo(
                    m_classcol.getStringValue()) == 0) {
                targetCols.add(colspec.getName());
                if (colspec.getType().isCompatible(DoubleValue.class)) {
                    // check if the values are in range [0,1]
                    DataColumnDomain domain = colspec.getDomain();
                    if (domain.hasBounds()) {
                        double lower =
                                ((DoubleValue)domain.getLowerBound())
                                        .getDoubleValue();
                        double upper =
                                ((DoubleValue)domain.getUpperBound())
                                        .getDoubleValue();
                        if (lower < 0 || upper > 1) {
                            throw new InvalidSettingsException(
                                    "Domain range for regression in column "
                                            + colspec.getName()
                                            + " not in range [0,1]");
                        }
                    }
                    nrOutputs = 1;
                    m_classmap = new HashMap<DataCell, Integer>();
                    m_classmap.put(new StringCell(colspec.getName()), 0);
                    m_regression = true;
                } else {
                    m_regression = false;
                    DataColumnDomain domain = colspec.getDomain();
                    if (domain.hasValues()) {
                        Set<DataCell> allvalues = domain.getValues();
                        int outputneuron = 0;
                        m_classmap = new HashMap<DataCell, Integer>();
                        for (DataCell value : allvalues) {
                            m_classmap.put(value, outputneuron);
                            outputneuron++;
                        }
                        nrOutputs = allvalues.size();
                    } else {
                        throw new Exception("Could not find domain values in"
                                + "nominal column "
                                + colspec.getName().toString());
                    }
                }
            } else {
                if (!colspec.getType().isCompatible(DoubleValue.class)) {
                    throw new Exception("Only double columns for input");
                }
                m_inputmap.put(colspec.getName(), nrInputs);
                learningCols.add(colspec.getName());
                nrInputs++;
            }
        }
        assert targetCols.size() == 1 : "Only one class column allowed.";
        m_architecture.setNrInputNeurons(nrInputs);
        m_architecture.setNrHiddenLayers(m_nrHiddenLayers.getIntValue());
        m_architecture.setNrHiddenNeurons(m_nrHiddenNeuronsperLayer
                .getIntValue());
        m_architecture.setNrOutputNeurons(nrOutputs);
        m_mlp = new MultiLayerPerceptron(m_architecture);
        if (m_regression) {
            m_mlp.setMode(MultiLayerPerceptron.REGRESSION_MODE);
        } else {
            m_mlp.setMode(MultiLayerPerceptron.CLASSIFICATION_MODE);
        }
        // Convert inputs to double arrays. Values from the class column are
        // encoded as bitvectors.
        int classColNr = posSpec.findColumnIndex(m_classcol.getStringValue());
        int nrposRows = 0;
        RowIterator rowIt = ((BufferedDataTable)inData[INPORT]).iterator();
        while (rowIt.hasNext()) {
            rowIt.next();
            nrposRows++;
        }
        Vector<Double[]> samples = new Vector<Double[]>();
        Vector<Double[]> outputs = new Vector<Double[]>();
        Double[] sample = new Double[nrInputs];
        Double[] output = new Double[nrOutputs];
        rowIt = ((BufferedDataTable)inData[INPORT]).iterator();
        int rowcounter = 0;
        while (rowIt.hasNext()) {
            boolean add = true;
            output = new Double[nrOutputs];
            sample = new Double[nrInputs];
            DataRow row = rowIt.next();
            int nrCells = row.getNumCells();
            int index = 0;
            for (int i = 0; i < nrCells; i++) {
                if (i != classColNr) {
                    if (!row.getCell(i).isMissing()) {
                        DoubleValue dc = (DoubleValue)row.getCell(i);
                        sample[index] = dc.getDoubleValue();
                        index++;
                    } else {
                        if (m_ignoreMV.getBooleanValue()) {
                            add = false;
                            break;
                        } else {
                            throw new Exception("Missing values in input"
                                    + " datatable");
                        }
                    }
                } else {
                    if (row.getCell(i).isMissing()) {
                        add = false;
                        if (!m_ignoreMV.getBooleanValue()) {
                            throw new Exception("Missing value in class"
                                    + " column");
                        }
                        break;
                    }
                    if (m_regression) {
                        DoubleValue dc = (DoubleValue)row.getCell(i);
                        output[0] = dc.getDoubleValue();
                    } else {
                        for (int j = 0; j < nrOutputs; j++) {
                            if (m_classmap.get(row.getCell(i)) == j) {
                                output[j] = new Double(1.0);
                            } else {
                                output[j] = new Double(0.0);
                            }
                        }
                    }
                }
            }
            if (add) {
                samples.add(sample);
                outputs.add(output);
                rowcounter++;
            }
        }
        Double[][] samplesarr = new Double[rowcounter][nrInputs];
        Double[][] outputsarr = new Double[rowcounter][nrInputs];
        for (int i = 0; i < samplesarr.length; i++) {
            samplesarr[i] = samples.get(i);
            outputsarr[i] = outputs.get(i);
        }
        // Now finally train the network.
        m_mlp.setClassMapping(m_classmap);
        m_mlp.setInputMapping(m_inputmap);
        RProp myrprop = new RProp();
        m_errors = new double[m_nrIterations.getIntValue()];
        for (int iteration = 0; iteration <
                        m_nrIterations.getIntValue(); iteration++) {
            exec.setProgress((double)iteration
                    / (double)m_nrIterations.getIntValue(), "Iteration "
                    + iteration);
            myrprop.train(m_mlp, samplesarr, outputsarr);
            double error = 0;
            for (int j = 0; j < outputsarr.length; j++) {
                double[] myoutput = m_mlp.output(samplesarr[j]);
                for (int o = 0; o < outputsarr[0].length; o++) {
                    error +=
                            (myoutput[o] - outputsarr[j][o])
                                    * (myoutput[o] - outputsarr[j][o]);
                }
            }
            m_errors[iteration] = error;
            exec.checkCanceled();
        }
        PMMLPortObjectSpec pmmlspec =
                new PMMLPortObjectSpec(posSpec, learningCols, ignoredCols,
                        targetCols);
        PMMLNeuralNetworkPortObject nnpmml =
                new PMMLNeuralNetworkPortObject(pmmlspec, m_mlp);
        return new PortObject[]{nnpmml};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_errors = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_nrIterations.saveSettingsTo(settings);
        m_nrHiddenLayers.saveSettingsTo(settings);
        m_nrHiddenNeuronsperLayer.saveSettingsTo(settings);
        m_classcol.saveSettingsTo(settings);
        m_ignoreMV.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_nrIterations.validateSettings(settings);
        m_nrHiddenLayers.validateSettings(settings);
        m_nrHiddenNeuronsperLayer.validateSettings(settings);
        m_classcol.validateSettings(settings);
        m_ignoreMV.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        m_nrIterations.loadSettingsFrom(settings);
        m_nrHiddenLayers.loadSettingsFrom(settings);
        m_nrHiddenNeuronsperLayer.loadSettingsFrom(settings);
        m_classcol.loadSettingsFrom(settings);
        m_ignoreMV.loadSettingsFrom(settings);
    }

    /**
     * @return error plot.
     */
    public double[] getErrors() {
        return m_errors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "RProp");
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
        int iterations = in.readInt();
        m_errors = new double[iterations];
        for (int i = 0; i < iterations; i++) {
            m_errors[i] = in.readDouble();
            exec.setProgress((double)i / (double)iterations);
        }
        in.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "RProp");
        ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(f));
        int iterations = m_errors.length;
        out.writeInt(iterations);
        for (int i = 0; i < iterations; i++) {
            out.writeDouble(m_errors[i]);
            exec.setProgress((double)i / (double)iterations);
        }
        out.close();
    }
}
