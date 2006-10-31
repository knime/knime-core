/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import java.util.Set;
import java.util.Vector;

import javax.swing.JPanel;

import org.knime.base.data.neural.Architecture;
import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.base.data.neural.methods.RProp;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.scatterplot.ScatterProps;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * RPropNodeModel trains a MultiLayerPerceptron with resilient backpropagation.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class RPropNodeModel extends NodeModel {
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
    private int m_nrIterations;

    /*
     * Number of hidden layers.
     */
    private int m_nrHiddenLayers;

    /*
     * Number of hidden neurons per layer.
     */
    private int m_nrHiddenNeuronsperLayer;

    /*
     * The class column.
     */
    private String m_classcol;

    /*
     * Flag for regression
     */
    private boolean m_regression;

    /*
     * Flag whether to ignore missing values
     */
    private boolean m_ignoreMV;

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
    //private ErrorPlot m_errorplot;

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
        super(1, 0, 0, 1);
        m_nrIterations = DEFAULTITERATIONS;
        m_nrHiddenLayers = DEFAULTHIDDENLAYERS;
        m_nrHiddenNeuronsperLayer = DEFAULTNEURONSPERLAYER;
        m_architecture = new Architecture();
        m_mlp = new MultiLayerPerceptron();
    }

    /**
     * returns null.
     * 
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_classcol != null) {
            boolean classcolinspec = false;
            for (DataColumnSpec colspec : inSpecs[INPORT]) {
                if (!(colspec.getName().toString().compareTo(
                        m_classcol.toString()) == 0)) {
                    if (!colspec.getType().isCompatible(DoubleValue.class)) {
                        throw new InvalidSettingsException(
                                "Only double columns for input");
                    }
                } else {
                    classcolinspec = true;
                    // check for regression
                    if (colspec.getType().isCompatible(DoubleValue.class)) {
                        // check if the values are in range [0,1]
                        DataColumnDomain domain = colspec.getDomain();
                        if (domain.hasBounds()) {
                            double lower = ((DoubleValue)domain.getLowerBound())
                                    .getDoubleValue();
                            double upper = ((DoubleValue)domain.getUpperBound())
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
                throw new InvalidSettingsException("Class column " + m_classcol
                        + " not found in DataTableSpec");
            }
        } else {
            throw new InvalidSettingsException("Class column not set");
        }
        return null;
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
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // If class column is not set, it is the last column.
        DataTableSpec posSpec = inData[INPORT].getDataTableSpec();
        if (m_classcol == null) {
            m_classcol = posSpec.getColumnSpec(posSpec.getNumColumns() - 1)
                    .getName();
        }
        // Determine the number of inputs and the number of outputs. Make also
        // sure that the inputs are double values.
        int nrInputs = 0;
        int nrOutputs = 0;
        m_inputmap = new HashMap<String, Integer>();
        for (DataColumnSpec colspec : posSpec) {
            // check for class column
            if (colspec.getName().toString().compareTo(
                    m_classcol.toString()) == 0) {
                if (colspec.getType().isCompatible(DoubleValue.class)) {
                    // check if the values are in range [0,1]
                    DataColumnDomain domain = colspec.getDomain();
                    if (domain.hasBounds()) {
                        double lower = ((DoubleValue)domain.getLowerBound())
                                .getDoubleValue();
                        double upper = ((DoubleValue)domain.getUpperBound())
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
                nrInputs++;
            }
        }
        m_architecture.setNrInputNeurons(nrInputs);
        m_architecture.setNrHiddenLayers(m_nrHiddenLayers);
        m_architecture.setNrHiddenNeurons(m_nrHiddenNeuronsperLayer);
        m_architecture.setNrOutputNeurons(nrOutputs);
        m_mlp = new MultiLayerPerceptron(m_architecture);
        if (m_regression) {
            m_mlp.setMode(MultiLayerPerceptron.REGRESSION_MODE);
        } else {
            m_mlp.setMode(MultiLayerPerceptron.CLASSIFICATION_MODE);
        }
        // Convert inputs to double arrays. Values from the class column are
        // encoded as bitvectors.
        int classColNr = posSpec.findColumnIndex(m_classcol);
        int nrposRows = 0;
        RowIterator rowIt = inData[INPORT].iterator();
        while (rowIt.hasNext()) {
            rowIt.next();
            nrposRows++;
        }
        Vector<Double[]> samples = new Vector<Double[]>();
        Vector<Double[]> outputs = new Vector<Double[]>();
        Double[] sample = new Double[nrInputs];
        Double[] output = new Double[nrOutputs];
        rowIt = inData[INPORT].iterator();
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
                        if (m_ignoreMV) {
                            add = false;
                            break;
                        } else {
                            throw new Exception("Missing values in input"
                                    + " datatable");
                        }
                    }
                } else {
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
        m_errors = new double[m_nrIterations];
        for (int iteration = 0; iteration < m_nrIterations; iteration++) {
            exec.setProgress((double)iteration / (double)m_nrIterations,
                    "Iteration " + iteration);
            myrprop.train(m_mlp, samplesarr, outputsarr);
            double error = 0;
            for (int j = 0; j < outputsarr.length; j++) {
                double[] myoutput = m_mlp.output(samplesarr[j]);
                for (int o = 0; o < outputsarr[0].length; o++) {
                    error += (myoutput[o] - outputsarr[j][o])
                            * (myoutput[o] - outputsarr[j][o]);
                }
            }
            m_errors[iteration] = error;
            exec.checkCanceled();
        }
        return new BufferedDataTable[]{};
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_errors = null;
    }
    
    /**
     * Stores the model of the trained neural network for later use.
     * 
     * @see NodeModel#saveModelContent(int,ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        if (index != 0) {
            throw new InvalidSettingsException("Wrong Model port");
        }
        m_mlp.savePredictorParams(predParams);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(MAXITER_KEY, m_nrIterations);
        settings.addInt(HIDDENLAYER_KEY, m_nrHiddenLayers);
        settings.addInt(NRHNEURONS_KEY, m_nrHiddenNeuronsperLayer);
        settings.addString(CLASSCOL_KEY, m_classcol);
        settings.addBoolean(IGNOREMV_KEY, m_ignoreMV);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.containsKey(MAXITER_KEY)) {
            int tempmaxiter = settings.getInt(MAXITER_KEY);
            if (tempmaxiter <= 0 || tempmaxiter > MAXNRITERATIONS) {
                throw new InvalidSettingsException(
                        "Invalid number of maximum iterations: " + tempmaxiter);
            }
        }
        if (settings.containsKey(HIDDENLAYER_KEY)) {
            int temphlayers = settings.getInt(HIDDENLAYER_KEY);
            if (temphlayers <= 0) {
                throw new InvalidSettingsException(
                        "Invalid number of hidden layers: " + temphlayers);
            }
        }
        if (settings.containsKey(NRHNEURONS_KEY)) {
            int temphneurons = settings.getInt(NRHNEURONS_KEY);
            if (temphneurons <= 0) {
                throw new InvalidSettingsException(
                        "Invalid number of hidden neurons: " + temphneurons);
            }
        }
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.containsKey(MAXITER_KEY)) {
            m_nrIterations = settings.getInt(MAXITER_KEY);
        }
        if (settings.containsKey(HIDDENLAYER_KEY)) {
            m_nrHiddenLayers = settings.getInt(HIDDENLAYER_KEY);
        }
        if (settings.containsKey(NRHNEURONS_KEY)) {
            m_nrHiddenNeuronsperLayer = settings.getInt(NRHNEURONS_KEY);
        }
        if (settings.containsKey(CLASSCOL_KEY)) {
            m_classcol = settings.getString(CLASSCOL_KEY);
        }
        if (settings.containsKey(IGNOREMV_KEY)) {
            m_ignoreMV = settings.getBoolean(IGNOREMV_KEY);
        }
    }

    /**
     * @return error plot.
     */
    public JPanel getErrorPlot() {
        if (m_errors == null) {
            return new JPanel();
        }
        DataColumnSpec[] colspecs = new DataColumnSpec[2];
        DataColumnSpecCreator colspecCreator = new DataColumnSpecCreator(
                "Iteration", DoubleCell.TYPE);
        colspecs[0] = colspecCreator.createSpec();
        colspecCreator = new DataColumnSpecCreator("Error", DoubleCell.TYPE);
        colspecs[1] = colspecCreator.createSpec();
        DataTableSpec spec = new DataTableSpec(colspecs);
        DataContainer con = new DataContainer(spec);
        for (int x = 0; x < m_errors.length; x++) {
            DataCell[] cells = new DataCell[]{new DoubleCell(x),
                    new DoubleCell(m_errors[x])};
            con.addRowToTable(new DefaultRow(new StringCell("" + x), cells));
        }
        con.close();
        DefaultDataArray darray = new DefaultDataArray(con.getTable(), 1, con
                .size());
        return new ErrorPlot(darray, 300, new ScatterProps());

    }

     /**
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
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
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "RProp");
        ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(f));
        int iterations = m_errors.length;
        out.writeInt(iterations);
        for (int i = 0; i < iterations; i++) {
            out.writeDouble(m_errors[i]);
            exec.setProgress((double)i / (double)iterations);
        }
        out.close();
    }
}
