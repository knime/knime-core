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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

import javax.swing.JComponent;

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
    public static PortObjectSpecSerializer<BWElimModel> 
            getPortObjectSpecSerializer() {
        return SpecSerializer.INSTANCE;
    }

    private static class SpecSerializer extends
            PortObjectSpecSerializer<BWElimModel> {
        private static final SpecSerializer INSTANCE = new SpecSerializer();

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
        private static final ModelSerializer INSTANCE = new ModelSerializer();

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

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return null;
    }
    
    
}
