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
 * -------------------------------------------------------------------
 * 
 * History
 *   26.10.2005 (cebron): created
 */
package org.knime.base.data.neural.methods;

import org.knime.base.data.neural.Architecture;
import org.knime.base.data.neural.Layer;
import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.base.data.neural.Perceptron;

/**
 * Implementation of the RProp Algorithm, as proposed by M. Riedmiller, H.Braun:
 * 'A Direct Adaptive Method for Faster backpropagation Learning: The RPROP
 * Algorithm', Proc. of the IEEE Intl. Conf. on Neural Networks 1993.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class RProp {
    /*
     * Lower limit for update-values. (As proposed in the paper)
     */
    private static final double DELTA_MIN = 1E-6;

    /*
     * Upper limit for update-values. (As proposed in the paper)
     */
    private static final double DELTA_MAX = 50.0;

    /*
     * eta minus update value.
     */
    private double m_etaMinus;

    /*
     * eta plus update value.
     */
    private double m_etaPlus;

    /*
     * initial update value.
     */
    private double m_etaNull;

    /*
     * Array holding the output of each neuron.
     */
    private double[][] m_output;

    /*
     * Array holding the delta value of each neuron.
     */
    private double[][] m_delta;

    /*
     * Eta values
     */
    private double[][][] m_etaIJ;

    /*
     * Eta values for thresholds
     */
    private double[][] m_thrEtaIJ;

    /*
     * Error derivations
     */
    private double[][][] m_errDers;

    /*
     * Error derivations for thresholds.
     */
    private double[][] m_thrErrDers;

    /*
     * Error derivations of former phase.
     */
    private double[][][] m_oldErrDers;

    /*
     * Threshold error derivations of former phase.
     */
    private double[][] m_oldThrErrDers;

    /*
     * Architecture of the MLP.
     */
    private Architecture m_architecture;

    /*
     * Layers of the MLP.
     */
    private Layer[] m_layers;

    /*
     * The MultiLayerPerceptron network.
     */
    private MultiLayerPerceptron m_nn;

    /*
     * The samples to train on.
     */
    private Double[][] m_samples;

    /*
     * The desired outputs for the samples avove.
     */
    private Double[][] m_outputs;

    /*
     * Indicates whether the RProp is in the first phase or not.
     */
    private boolean m_newPhase = true;

    /**
     * Constructor, uses default learning rate of 0.1, increase parameter 1.2
     * and decrease parameter 0.5 as proposed in the paper.
     */
    public RProp() {
        this(1.2, 0.5, 0.1);
    }

    /**
     * @param etaPlus increase parameter
     * @param etaMinus decrease parameter
     * @param etaNull initial learning rate
     */
    public RProp(final double etaPlus, final double etaMinus,
            final double etaNull) {
        m_etaPlus = etaPlus;
        m_etaMinus = etaMinus;
        m_etaNull = etaNull;
    }

    /**
     * Train the neural network once.
     * 
     * @param nn neural net to train
     * @param samples the samples
     * @param outputs the desired outputs for these samples
     */
    public void train(final MultiLayerPerceptron nn, final Double[][] samples,
            final Double[][] outputs) {

        m_nn = nn;
        m_samples = samples;
        m_outputs = outputs;
        init();

        double sum = 0.0;
        double y = 0.0;
        double errDer;
        double thrErrDer;
        double oldErrDer;
        double oldThrErrDer;

        /*
         * For all samples
         */
        for (int s = 0; s < samples.length; s++) {
            Double[][] sample = new Double[2][samples[0].length];
            sample[0] = m_samples[s];
            sample[1] = m_outputs[s];

            /*
             * Compute Gradient
             */

            /*
             * Forward wave
             */
            for (int i = 0; i < m_output.length; i++) {
                for (int j = 0; j < m_output[i].length; j++) {
                    if (i == 0) { // input neuron
                        m_output[i][j] = sample[0][j];
                    } else { // non-input neuron
                        Perceptron p = nn.getLayer(i).getPerceptron(j);
                        sum = 0.0;
                        for (int k = 0; k < m_output[i - 1].length; k++) {
                            sum += m_output[i - 1][k] * p.getWeight(k);
                        }
                        m_output[i][j] = p.activationFunction(sum
                                - p.getThreshold());
                    }
                }
            }
            /*
             * Backward wave
             */
            for (int i = m_delta.length - 1; i >= 0; i--) {
                for (int j = 0; j < m_delta[i].length; j++) {
                    y = m_output[i][j];
                    if (i == m_delta.length - 1) { // output neuron
                        m_delta[i][j] = (sample[1][j] - y) * y * (1 - y);
                    } else { // non-output neuron
                        sum = 0.0;
                        for (int k = 0; k < m_delta[i + 1].length; k++) {
                            sum += m_delta[i + 1][k]
                                    * nn.getLayer(i + 1).getPerceptron(k)
                                            .getWeight(j);
                        }
                        m_delta[i][j] = y * (1 - y) * sum;
                    }
                }
            }
            // Now compute error derivations
            for (int i = 0; i < m_errDers.length; i++) {
                for (int j = 0; j < m_errDers[i].length; j++) {
                    for (int k = 0; k < m_errDers[i][j].length; k++) {
                        m_errDers[i][j][k] += m_output[i][k]
                                * -m_delta[i + 1][j];
                    }
                }
            }
            for (int i = 0; i < m_thrErrDers.length; i++) {
                for (int j = 0; j < m_thrErrDers[i].length; j++) {
                    m_thrErrDers[i][j] += m_delta[i + 1][j];
                }
            }
        }
        // STEP 2: for all weights set delta_w
        for (int i = 1; i < nn.getLayers().length; i++) {
            for (int j = 0; j < nn.getLayer(i).getPerceptrons().length; j++) {
                for (int k = 0; 
                k < nn.getLayer(i - 1).getPerceptrons().length; k++) {
                    // Compute error derivation
                    errDer = m_errDers[i - 1][j][k];
                    // Also get old error derivation
                    oldErrDer = m_oldErrDers[i - 1][j][k];

                    if ((errDer * oldErrDer) > 0.0) {
                        m_etaIJ[i - 1][j][k] = Math.min(m_etaIJ[i - 1][j][k]
                                * getEtaPlus(), DELTA_MAX);
                        double deltaW = -sgn(errDer) * m_etaIJ[i - 1][j][k];
                        nn.getLayer(i).getPerceptron(j).setWeight(
                                k,
                                nn.getLayer(i).getPerceptron(j).getWeight(k)
                                        + deltaW);
                        m_oldErrDers[i - 1][j][k] = errDer;
                    } else if ((errDer * oldErrDer) < 0.0) {
                        m_etaIJ[i - 1][j][k] = Math.max(m_etaIJ[i - 1][j][k]
                                * getEtaMinus(), DELTA_MIN);
                        m_oldErrDers[i - 1][j][k] = 0;
                    } else if ((errDer * oldErrDer) == 0) {
                        double deltaW = -sgn(errDer) * m_etaIJ[i - 1][j][k];
                        nn.getLayer(i).getPerceptron(j).setWeight(
                                k,
                                nn.getLayer(i).getPerceptron(j).getWeight(k)
                                        + deltaW);
                        m_oldErrDers[i - 1][j][k] = errDer;
                    }
                }
            }
        }
        // Thresholds
        for (int i = 1; i < nn.getLayers().length; i++) {
            for (int j = 0; j < nn.getLayer(i).getPerceptrons().length; j++) {
                // Compute error derivation
                thrErrDer = m_thrErrDers[i - 1][j];
                // Also get old error derivation
                oldThrErrDer = m_oldThrErrDers[i - 1][j];
                if ((thrErrDer * oldThrErrDer) > 0.0) {
                    m_thrEtaIJ[i - 1][j] = Math.min(m_thrEtaIJ[i - 1][j]
                            * getEtaPlus(), DELTA_MAX);
                    double deltaThr = -sgn(thrErrDer) * m_thrEtaIJ[i - 1][j];
                    nn.getLayer(i).getPerceptron(j).setThreshold(
                            nn.getLayer(i).getPerceptron(j).getThreshold()
                                    + deltaThr);
                    m_oldThrErrDers[i - 1][j] = thrErrDer;
                } else if ((thrErrDer * oldThrErrDer) < 0.0) {
                    m_thrEtaIJ[i - 1][j] = Math.max(m_thrEtaIJ[i - 1][j]
                            * getEtaMinus(), DELTA_MIN);
                    m_oldThrErrDers[i - 1][j] = 0;
                } else if ((thrErrDer * oldThrErrDer) == 0.0) {
                    double deltaThr = -sgn(thrErrDer) * m_thrEtaIJ[i - 1][j];
                    nn.getLayer(i).getPerceptron(j).setThreshold(
                            nn.getLayer(i).getPerceptron(j).getThreshold()
                                    + deltaThr);
                    m_oldThrErrDers[i - 1][j] = thrErrDer;
                }
            }

        }
    }

    private void init() {
        if (m_newPhase) {
            m_architecture = m_nn.getArchitecture();
            m_layers = m_nn.getLayers();

            // Initialize output
            m_output = new double[m_layers.length][];
            m_output[0] = new double[m_architecture.getNrInputNeurons()];
            for (int i = 1; i < m_layers.length - 1; i++) {
                m_output[i] = new double[m_architecture.getNrHiddenNeurons()];
            }
            m_output[m_layers.length - 1] = new double[m_architecture
                    .getNrOutputNeurons()];
            // Initialize error term
            m_delta = new double[m_layers.length][];
            m_delta[0] = new double[m_architecture.getNrInputNeurons()];
            for (int i = 1; i < m_layers.length - 1; i++) {
                m_delta[i] = new double[m_architecture.getNrHiddenNeurons()];
            }
            m_delta[m_layers.length - 1] = new double[m_architecture
                    .getNrOutputNeurons()];
            // initialize eta_ij with etaNull
            m_etaIJ = new double[m_nn.getLayers().length - 1][][];
            for (int i = 0; i < m_etaIJ.length; i++) {
                m_etaIJ[i] = 
                    new double[m_nn.getLayer(i + 1).getPerceptrons().length][];
                for (int j = 0; j < m_etaIJ[i].length; j++) {
                    m_etaIJ[i][j] = new double[m_nn.getLayer(i)
                            .getPerceptrons().length];
                    for (int k = 0; k < m_etaIJ[i][j].length; k++) {
                        m_etaIJ[i][j][k] = getEtaNull();
                    }
                }
            }

            // initialize thr_eta_ij with etaNull
            m_thrEtaIJ = new double[m_nn.getLayers().length - 1][];
            for (int i = 0; i < m_etaIJ.length; i++) {
                m_thrEtaIJ[i] = new double[m_nn.getLayer(i + 1)
                        .getPerceptrons().length];
                for (int j = 0; j < m_thrEtaIJ[i].length; j++) {
                    m_thrEtaIJ[i][j] = getEtaNull();
                }

            }
        }
        // initialize err_ders
        m_errDers = new double[m_nn.getLayers().length - 1][][];
        for (int i = 0; i < m_errDers.length; i++) {
            m_errDers[i] = new double[
                           m_nn.getLayer(i + 1).getPerceptrons().length][];
            for (int j = 0; j < m_errDers[i].length; j++) {
                m_errDers[i][j] = new double[
                                 m_nn.getLayer(i).getPerceptrons().length];
            }
        }

        // initialize thr_err_ders
        m_thrErrDers = new double[m_nn.getLayers().length - 1][];
        for (int i = 0; i < m_thrErrDers.length; i++) {
            m_thrErrDers[i] = new double[
                             m_nn.getLayer(i + 1).getPerceptrons().length];
        }
        if (m_newPhase) {
            // initialize old_err_der
            m_oldErrDers = new double[m_nn.getLayers().length - 1][][];
            for (int i = 0; i < m_oldErrDers.length; i++) {
                m_oldErrDers[i] = new double[m_nn.getLayer(i + 1)
                        .getPerceptrons().length][];
                for (int j = 0; j < m_oldErrDers[i].length; j++) {
                    m_oldErrDers[i][j] = new double[m_nn.getLayer(i)
                            .getPerceptrons().length];
                }
            }
            // initialize old_thr_err_ders
            m_oldThrErrDers = new double[m_nn.getLayers().length - 1][];
            for (int i = 0; i < m_oldThrErrDers.length; i++) {
                m_oldThrErrDers[i] = new double[m_nn.getLayer(i + 1)
                        .getPerceptrons().length];
            }
            m_newPhase = false;
        }
    }

    /**
     * Method computes the sign of a double number.
     * 
     * @param d the number
     * @return sgn(d)
     */
    public static double sgn(final double d) {
        return (d > 0.0) ? 1.0 : ((d < 0.0) ? -1.0 : 0.0);
    }

    /**
     * Get negative learning rate.
     * 
     * @return current negative learning rate
     */
    public double getEtaMinus() {
        return m_etaMinus;
    }

    /**
     * Get positive learning rate.
     * 
     * @return current positive learning rate
     */
    public double getEtaPlus() {
        return m_etaPlus;
    }

    /**
     * Set negative learning rate.
     * 
     * @param etaMinus new negative learning rate
     */
    public void setEtaMinus(final double etaMinus) {
        m_etaMinus = etaMinus;
    }

    /**
     * Set positive learning rate.
     * 
     * @param etaPlus new positive learning rate
     */
    public void setEtaPlus(final double etaPlus) {
        m_etaPlus = etaPlus;
    }

    /**
     * Get starting value for eta.
     * 
     * @return current starting value for eta
     */
    public double getEtaNull() {
        return m_etaNull;
    }

    /**
     * set starting value for eta.
     * 
     * @param etaNull new starting value
     */
    public void setEtaNull(final double etaNull) {
        m_etaNull = etaNull;
    }

    /**
     * Evaluates input and returns output of output neurons.
     * 
     * @param in input for the net
     * @return output of the output neurons after having processed a forward
     *         wave through the net
     */
    public double[] evaluate(final double[] in) {
        return m_nn.output(in);
    }
}
