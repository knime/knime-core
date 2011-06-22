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
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 17, 2011 (morent): created
 */

package org.knime.base.data.normalize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.dmg.pmml40.DATATYPE;
import org.dmg.pmml40.DerivedFieldDocument.DerivedField;
import org.dmg.pmml40.ExtensionDocument.Extension;
import org.dmg.pmml40.LinearNormDocument.LinearNorm;
import org.dmg.pmml40.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml40.NormContinuousDocument.NormContinuous;
import org.dmg.pmml40.OPTYPE;
import org.dmg.pmml40.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocTranslator;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class PMMLNormalizeTranslator implements PMMLPreprocTranslator {
    private final List<String> m_fields;
    private final List<Double> m_scales;
    private final List<Double> m_translations;
    private static final String SUMMARY = "summary";
    private String m_summary;
    private AffineTransConfiguration m_affineTrans;
    private DerivedFieldMapper m_mapper;

    private static final int MAX_NUM_SEGMENTS = 2;

    /**
     * Creates a new empty translator to be initialized by the
     * {@link #initializeFrom(LocalTransformations, boolean)} or
     * {@link #initializeFrom(TransformationDictionary, boolean)} method.
     */
    public PMMLNormalizeTranslator() {
        m_fields = new ArrayList<String>();
        m_scales = new ArrayList<Double>();
        m_translations = new ArrayList<Double>();
    }

    /**
     * Creates an initialized translator that can export its configuration.
     *
     * @param trans the affine trans configuration * @param mapper mapping data
     *            column names to PMML derived field names and vice versa
     * @param mapper mapping data column names to PMML derived field names and
     *      vice versa
     */
    public PMMLNormalizeTranslator(final AffineTransConfiguration trans,
            final DerivedFieldMapper mapper) {
        this();
        m_affineTrans = trans;
        m_mapper = mapper;
    }

    /**
     * Builds a configuration object for a {@link AffineTransTable}.
     * @return the affine trans configuration
     */
    public AffineTransConfiguration getAffineTransConfig() {
        if (m_affineTrans  == null) {
            double[] nanArray = new double[m_fields.size()];
            Arrays.fill(nanArray, Double.NaN);
            double[] s = new double[m_scales.size()];
            double[] t = new double[m_translations.size()];
            for (int i = 0; i < t.length; i++) {
                s[i] = m_scales.get(i);
                t[i] = m_translations.get(i);
            }
            m_affineTrans =  new AffineTransConfiguration(
                    m_fields.toArray(new String[m_fields.size()]), s, t,
                    nanArray, nanArray, m_summary);
        }
        return m_affineTrans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationDictionary exportToTransDict() {
        TransformationDictionary dictionary = TransformationDictionary.Factory
                .newInstance();
        dictionary.setDerivedFieldArray(createDerivedFields());
        return dictionary;
    }

    private Extension[] createSummaryExtension() {
        Extension extension = Extension.Factory.newInstance();
        extension.setName(SUMMARY);
        extension.setExtender(PMMLPortObjectSpec.KNIME);
        extension.setValue(m_affineTrans.getSummary());
        return new Extension[] {extension};
    }

    private void parseExtensionArray(final Extension ... extensions) {
        if (extensions == null) {
            return;
        }
        /* Try to read the summary which might be stored as first extension.*/
        for (Extension ext : extensions) {
            if (!PMMLPortObjectSpec.KNIME.equals(ext.getExtender())) {
                continue; // skip non KNIME extensions
            }
            if (SUMMARY.equals(ext.getName())) {
                m_summary = ext.getValue();
                break;
            }
        }
    }

    private DerivedField[] createDerivedFields() {
        int num = m_affineTrans.getNames().length;
        DerivedField[] derivedFields = new DerivedField[num];

        for (int i = 0; i < num; i++) {
            DerivedField df = DerivedField.Factory.newInstance();
            df.setExtensionArray(createSummaryExtension());
            String name = m_affineTrans.getNames()[i];
            df.setDisplayName(name);
            /* The field name must be retrieved before creating a new derived
             * name for this derived field as the map only contains the
             * current mapping. */
            String fieldName = m_mapper.getDerivedFieldName(name);
            df.setName(m_mapper.createDerivedFieldName(name));
            df.setOptype(OPTYPE.CONTINUOUS);
            df.setDataType(DATATYPE.DOUBLE);
            NormContinuous cont = df.addNewNormContinuous();
            cont.setField(fieldName);
            double trans = m_affineTrans.getTranslations()[i];
            double scale = m_affineTrans.getScales()[i];

            LinearNorm firstNorm = cont.addNewLinearNorm();
            firstNorm.setOrig(0.0);
            firstNorm.setNorm(trans);

            LinearNorm secondNorm = cont.addNewLinearNorm();
            secondNorm.setOrig(1.0);
            secondNorm.setNorm(scale + trans);
            derivedFields[i] = df;
        }
        return derivedFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalTransformations exportToLocalTrans() {
        LocalTransformations localtrans
                = LocalTransformations.Factory.newInstance();
        localtrans.setDerivedFieldArray(createDerivedFields());
        return localtrans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> initializeFrom(final DerivedField[] derivedFields) {
        if (derivedFields == null) {
            return Collections.EMPTY_LIST;
        }
        m_mapper = new DerivedFieldMapper(derivedFields);
        int num = derivedFields.length;
        List<Integer> consumed = new ArrayList<Integer>(num);

        if (num > 0) {
            parseExtensionArray(derivedFields[0].getExtensionArray());
        }

        for (int i = 0; i < derivedFields.length; i++) {
            DerivedField df = derivedFields[i];
            /** This field contains the name of the column in KNIME that
             * corresponds to the derived field in PMML. This is necessary if
             * derived fields are defined on other derived fields and the
             * columns in KNIME are replaced with the preprocessed values.
             * In this case KNIME has to know the original names (e.g. A) while
             * PMML references to A', A'' etc. */
            String displayName = df.getDisplayName();

            if (!df.isSetNormContinuous()) {
                //only reading norm continuous other entries are skipped
                continue;
            }
            consumed.add(i);
            NormContinuous normContinuous = df.getNormContinuous();
            if (normContinuous.getLinearNormArray().length > 2) {
                throw new IllegalArgumentException("Only two LinearNorm "
                        + "elements are supported per NormContinuous");
            }
            //String field = normContinuous.getField();
            double[] orig = new double[MAX_NUM_SEGMENTS];
            double[] norm = new double[MAX_NUM_SEGMENTS];

            LinearNorm[] norms = normContinuous.getLinearNormArray();
            for (int j = 0; j < norms.length; j++) {
                orig[j] = norms[j].getOrig();
                norm[j] = norms[j].getNorm();
            }
            double scale = (norm[1] - norm[0]) / (orig[1] - orig[0]);
            m_scales.add(scale);
            m_translations.add(norm[0] - scale * orig[0]);
            if (displayName != null) {
                m_fields.add(displayName);
            } else {
                m_fields.add(m_mapper.getColumnName(normContinuous.getField()));
            }
        }
        return consumed;
    }
}
