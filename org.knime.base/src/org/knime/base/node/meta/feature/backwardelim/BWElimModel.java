/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.util.Pair;

/**
 * This the model that holds the result of a backward elimination loop. The
 * model consists of all levels (i.e. number of included features) together with
 * the corresponding error rate and a list of all columns included in the level.
 *
 * Note that this class is also its spec at the same time, because the stored
 * information is needed in the dialog of the filter node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimModel implements PortObject, PortObjectSpec {
    /** The type of ports that create or consume such a model. */
    public static final PortType TYPE = new PortType(BWElimModel.class);

    private final Collection<Pair<Double, Collection<String>>> m_featureLevels =
            new ArrayList<Pair<Double, Collection<String>>>();

    private final Collection<Pair<Double, Collection<String>>> m_unmodList =
            Collections.unmodifiableCollection(m_featureLevels);

    private final String m_targetColumn;

    /**
     * Creates a new model.
     *
     * @param targetColumn the target columns's name
     */
    public BWElimModel(final String targetColumn) {
        m_targetColumn = targetColumn;
    }

    /**
     * Adds a new feature level.
     *
     * @param error the resulting error rate
     * @param includedColumns a list with the included column names
     */
    public void addFeatureLevel(final double error,
            final Collection<String> includedColumns) {
        m_featureLevels.add(new Pair<Double, Collection<String>>(error,
                Collections.unmodifiableCollection(new ArrayList<String>(
                        includedColumns))));
    }

    /**
     * Returns an unmodifieable collection of the stored feature levels. Each
     * entry if a pair with the error rate as first part and a list of all
     * columns that were included in the iteration as second part.
     *
     * @return a collection with pairs
     */
    public Collection<Pair<Double, Collection<String>>> featureLevels() {
        return m_unmodList;
    }

    /**
     * Returns the target column's name.
     *
     * @return the name
     */
    public String targetColumn() {
        return m_targetColumn;
    }

    /**
     * {@inheritDoc}
     */
    public PortObjectSpec getSpec() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return m_featureLevels.size() + " different features";
    }

    /**
     * Returns a serializer object for this model.
     *
     * @return a port object serializer
     */
    public static PortObjectSerializer<BWElimModel> getPortObjectSerializer() {
        return ModelSerializer.INSTANCE;
    }

    /**
     * Returns a serializer object for this model's spec (which is the model
     * itself).
     *
     * @return a port object serializer
     */
    public static PortObjectSpecSerializer<BWElimModel> getPortObjectSpecSerializer() {
        return SpecSerializer.INSTANCE;
    }

    private static class SpecSerializer extends
            PortObjectSpecSerializer<BWElimModel> {
        static final SpecSerializer INSTANCE = new SpecSerializer();

        /** {@inheritDoc} */
        @Override
        public BWElimModel loadPortObjectSpec(
                final PortObjectSpecZipInputStream inStream) 
        throws IOException {
            inStream.getNextEntry();
            BufferedReader in = 
                new BufferedReader(new InputStreamReader(inStream));
            String line;
            BWElimModel model = new BWElimModel(in.readLine());
            while ((line = in.readLine()) != null) {
                int nrOfFeatures = Integer.parseInt(line);
                double error = Double.parseDouble(in.readLine());
                List<String> features = new ArrayList<String>();
                for (int i = 0; i < nrOfFeatures; i++) {
                    features.add(in.readLine());
                }
                model.addFeatureLevel(error, Collections
                        .unmodifiableCollection(features));
            }
            in.close();

            return model;
        }

        /** {@inheritDoc} */
        @Override
        public void savePortObjectSpec(final BWElimModel pos,
                final PortObjectSpecZipOutputStream outStream) 
        throws IOException {
            outStream.putNextEntry(new ZipEntry("spec.file"));
            PrintWriter out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(outStream)));

            out.println(pos.m_targetColumn);
            for (Pair<Double, Collection<String>> p : pos.m_featureLevels) {
                out.println(p.getSecond().size());
                out.println(p.getFirst());
                for (String s : p.getSecond()) {
                    out.println(s);
                }
            }
            out.close();
        }
    }

    private static class ModelSerializer extends
            PortObjectSerializer<BWElimModel> {
        static final ModelSerializer INSTANCE = new ModelSerializer();

        /**
         * {@inheritDoc}
         */
        @Override
        public BWElimModel loadPortObject(final PortObjectZipInputStream in,
                final PortObjectSpec spec, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
            return (BWElimModel)spec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void savePortObject(final BWElimModel portObject,
                final PortObjectZipOutputStream out, 
                final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
        }
    }
}
