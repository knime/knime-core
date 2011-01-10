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
 *   Sep 9, 2008 (wiswedel): created
 */
package org.knime.base.data.normalize;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * Configuration object for a {@link AffineTransTable}.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class AffineTransConfiguration {

    private static final String CFG_COLUMNS = "columns";
    private static final String CFG_SUMMARY = "summary";
    private static final String CFG_NAME = "column_name";
    private static final String CFG_SCALE = "column_scale";
    private static final String CFG_TRANSLATE = "column_translate";
    private static final String CFG_MIN = "column_min";
    private static final String CFG_MAX = "column_max";

    private final String[] m_includeNames;
    private final double[] m_scales; 
    private final double[] m_translations;
    private final double[] m_min; 
    private final double[] m_max;
    private final String m_summary;
    
    /** Default, no normalization on columns. */
    public AffineTransConfiguration() {
        this(new String[0], new double[0], new double[0], 
                new double[0], new double[0], "No normalization");
    }
    
    /**
     * @param names The names of the columns being normalized
     * @param scales the scale parameters (same order as <code>names</code>)
     * @param translations the translation parameters
     * @param min the minimum values (for sanity checks). If the normalized
     *            value is slightly off the desired minimum value because of
     *            rounding errors, it is set to this bounding value. Can be
     *            {@link Double#NaN}, then no checks are performed.
     * @param max the maximum values (for sanity checks). If the normalized
     *            value is slightly off the desired maximum value because of
     *            rounding errors, it is set to this bounding value. Can be
     *            {@link Double#NaN}, then no checks are performed.
     * @param summary Port summary
     * @throws NullPointerException if any argument is <code>null</code>
     * @throws IllegalArgumentException if the arrays don't have the same
     *             length or the scales or translations arguments contain NaN.
     */
    public AffineTransConfiguration(final String[] names,
            final double[] scales, final double[] translations,
            final double[] min, final double[] max, final String summary) {
        if (names.length != scales.length
                || names.length != translations.length
                || names.length != max.length
                || names.length != min.length) {
            throw new IllegalArgumentException("Lengths must match: "
                    + names.length + " vs. " + scales.length + " vs. "
                    + translations.length + " vs. " + min.length 
                    + " vs. " + max.length);
        }
        for (int i = 0; i < scales.length; i++) {
            if (Double.isNaN(scales[i]) || Double.isNaN(translations[i])) {
                throw new IllegalArgumentException("Cannot transform with NaN");
            }
        }
        m_scales = Arrays.copyOf(scales, scales.length);
        m_includeNames = Arrays.copyOf(names, names.length);
        m_translations = Arrays.copyOf(translations, translations.length);
        m_min = Arrays.copyOf(min, min.length);
        m_max = Arrays.copyOf(max, max.length);
        m_summary = summary;
    }
    
    /** @return The names of the columns being normalized (in order of the
     * other array getters). 
     */
    public String[] getNames() {
        return m_includeNames;
    }

    /**
     * @return the scales
     */
    public double[] getScales() {
        return m_scales;
    }

    /**
     * @return the translations
     */
    public double[] getTranslations() {
        return m_translations;
    }

    /**
     * @return the min
     */
    public double[] getMin() {
        return m_min;
    }

    /**
     * @return the max
     */
    public double[] getMax() {
        return m_max;
    }
    
    /**
     * @return the summary
     */
    public String getSummary() {
        return m_summary;
    }
    
    /** Saves this object to the argument model content.
     * @param model To write to.
     */
    protected void save(final ModelContentWO model) {
        ModelContentWO colSub = model.addModelContent(CFG_COLUMNS);
        for (int i = 0; i < m_includeNames.length; i++) {
            String name = m_includeNames[i];
            double scale = m_scales[i];
            double transl = m_translations[i];
            double min = m_min[i];
            double max = m_max[i];
            if (name == null) {
                assert Double.isNaN(scale) && Double.isNaN(transl);
                continue;
            }
            ModelContentWO sub = colSub.addModelContent(name);
            sub.addString(CFG_NAME, name);
            sub.addDouble(CFG_SCALE, scale);
            sub.addDouble(CFG_TRANSLATE, transl);
            sub.addDouble(CFG_MIN, min);
            sub.addDouble(CFG_MAX, max);
        }
        model.addString(CFG_SUMMARY, m_summary);
    }
    

    /** Restores content.
     * @param settings To load from.
     * @return A new configuration object
     * @throws InvalidSettingsException If that fails.
     */
    public static AffineTransConfiguration load(
            final ModelContentRO settings) throws InvalidSettingsException {
        ModelContentRO colSub = settings.getModelContent(CFG_COLUMNS); 
        int colCount = colSub.keySet().size();
        double[] scales = new double[colCount];
        double[] translations = new double[colCount];
        double[] mins = new double[colCount];
        double[] maxs = new double[colCount];
        Arrays.fill(scales, Double.NaN);
        Arrays.fill(translations, Double.NaN);
        Arrays.fill(mins, Double.NaN);
        Arrays.fill(maxs, Double.NaN);
        String[] names = new String[colCount];
        int index = 0;
        for (String key : colSub.keySet()) {
            ModelContentRO sub = colSub.getModelContent(key);
            String name = sub.getString(CFG_NAME);
            double scale = sub.getDouble(CFG_SCALE);
            double trans = sub.getDouble(CFG_TRANSLATE);
            double min = sub.getDouble(CFG_MIN);
            mins[index] = min;
            double max = sub.getDouble(CFG_MAX);
            maxs[index] = max;
            scales[index] = scale;
            translations[index] = trans;
            names[index] = name;
            index++;
        }
        String summary = settings.getString(CFG_SUMMARY);
        return new AffineTransConfiguration(
                names, scales, translations, mins, maxs, summary);
    }

}
