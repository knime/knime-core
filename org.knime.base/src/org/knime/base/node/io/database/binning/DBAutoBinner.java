/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Oct 12, 2015 (Lara): created
 */
package org.knime.base.node.io.database.binning;

import java.text.NumberFormat;

import org.knime.base.node.preproc.autobinner3.AutoBinner;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Lara
 */
public class DBAutoBinner extends AutoBinner {

    /**
     * @param settings
     * @param spec
     * @throws InvalidSettingsException
     */
    public DBAutoBinner(final AutoBinnerLearnSettings settings, final DataTableSpec spec)
        throws InvalidSettingsException {
        super(settings, spec);
    }



    /**
     * @param edges
     * @param binNaming
     * @return
     */
    public String[] nameBins(final double[] edges, final AutoBinnerLearnSettings.BinNaming binNaming) {
        DBBinnerNumberFormat formatter = new DBBinnerNumberFormat();
        String[] binNames = new String[edges.length - 1];
        switch (binNaming) {
            case edges:
                binNames[0] = "'[" + formatter.format(edges[0]) + "," + formatter.format(edges[1]) + "]'";
                for (int i = 1; i < binNames.length; i++) {
                    binNames[i] = "'(" + formatter.format(edges[i]) + "," + formatter.format(edges[i + 1]) + "]'";
                }
                break;
            case numbered:
                for (int i = 0; i < binNames.length; i++) {
                    binNames[i] = "'Bin " + (i + 1) + "'";
                }
                break;
            case midpoints:
                binNames[0] = formatter.format((edges[1] - edges[0]) / 2 + edges[0]);
                for (int i = 1; i < binNames.length; i++) {
                    binNames[i] = "'" + formatter.format((edges[i + 1] - edges[i]) / 2 + edges[i]) + "'";
                }
                break;
            default:
                for (int i = 0; i < binNames.length; i++) {
                    binNames[i] = "'Bin " + (i + 1) + "'";
                }
                break;
        }

        return binNames;
    }

    /**
     *
     * @author Lara
     */
    private class DBBinnerNumberFormat extends AutoBinner.BinnerNumberFormat {

        @Override
        public String format(final double d) {
            if (d == 0.0) {
                return "0";
            }
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                return Double.toString(d);
            }
            NumberFormat format;
            double abs = Math.abs(d);
            if (abs < 0.0001) {
                format = getSmallFormat();
            } else {
                format = getDefaultFormat();
            }
            synchronized (format) {
                return format.format(d);

            }
        }

    }

}
