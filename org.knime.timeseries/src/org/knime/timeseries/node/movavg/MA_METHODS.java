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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.timeseries.node.movavg;

import java.util.LinkedList;
import java.util.List;

import org.knime.timeseries.node.movavg.maversions.CumulativeMA;
import org.knime.timeseries.node.movavg.maversions.DoubleExpMA;
import org.knime.timeseries.node.movavg.maversions.ExponentialMA;
import org.knime.timeseries.node.movavg.maversions.ExponentialOldMA;
import org.knime.timeseries.node.movavg.maversions.HarmonicMeanMA;
import org.knime.timeseries.node.movavg.maversions.MovingAverage;
import org.knime.timeseries.node.movavg.maversions.SimpleMA;
import org.knime.timeseries.node.movavg.maversions.TripleExponentialMA;
import org.knime.timeseries.node.movavg.maversions.WeightedMA;

/** enum constants for weight functions.
 * to introduce new methods you have to do  some steps.
 *
 * 1. insert the method name and the label.
 * 2. if it is a method which is only taking previous rows into account,
 * add it to the list in getbackwardcompmethods
 * 3. if it is method which value should be mapped to the centered, add
 * it to the list in getcenteredmethods.
 * 4. otherwise your method is a strictly  forward method (we only offer,
 * backward, forward and center)
 * 5. return your instance of the moving average class in getMAObject(int)
 *
 * done.
 *
 * @author Iris Adae, University of Konstanz, Germany
 */
public enum MA_METHODS {

     /** no weight function. Backward*/
     Simple("Backward simple"),
     /** no weight function. Centered*/
     Center("Center simple"),
     /** no weight function. Forward*/
     Forward("Forward simple"),
     /** Gaussian weighted function. Backward*/
     SimpleG("Backward Gaussian"),
     /** no weight function. Centered*/
     CenterG("Center Gaussian"),
     /** no weight function. Forward*/
     ForwardG("Forward Gaussian"),
     /** the harmonic mean, fixed in the center of window*/
     HarmonicC("Harmonic Mean Center"),
     /** cumulative Moving average, (overall). */
     Cumulative("Cumulative simple"),
     /** exponential moving average. */
     Exponential("Simple exponential"),
     /** Double exponential  smoothing moving average. */
     DoubleExponential("Double exponential"),
     /** Triple exponential  smoothing moving average. */
     TripleExponential("Triple exponential"),
     /** the old exponential behavior. */
     OldExponential("Old Exponential");

     private final String m_label;

     /**
      * @return the label of the method.
      */
     public String getLabel() {
         return m_label;
     }

     /**
      * @return the labels of all available options
      */
     public static String[] getLabels() {
         final MA_METHODS[] values = values();
         final String[] labels = new String[values.length];
         for (int i = 0, length = values.length; i < length; i++) {
             labels[i] = values[i].getLabel();
         }
         return labels;
     }
     /**
      *
      * @param label the label of the method.
      */
     MA_METHODS(final String label) {
         m_label = label;
     }

     /**
      * @param label the label of the {@link MA_METHODS}
      * @return the {@link MA_METHODS} with the given label
      */
     public static MA_METHODS getPolicy4Label(final String label) {
         for (final MA_METHODS namePolicy : values()) {
             if (namePolicy.getLabel().equals(label)) {
                 return namePolicy;
             }
         }
         // needed for upwards compatibility
         if (label.equals("Simple")) {
             return Simple;
         } else if (label.equals("Exponential")) {
             return OldExponential;
         }
         throw new IllegalArgumentException(
         		"Unknown movering average method \"" + label + "\"");
     }

     /**This methods returns all methods which can be used with the
      * CellFactory, as they don't look to the future rows.
      *
      * @return an list containing all backward compatible methods.
      */
     public static List<MA_METHODS> getBackwardCompMethods() {
         List<MA_METHODS> ret = new LinkedList<MA_METHODS>();
         ret.add(Simple);
         ret.add(SimpleG);
         ret.add(Cumulative);
         ret.add(Exponential);
         ret.add(OldExponential);
         ret.add(DoubleExponential);
         ret.add(TripleExponential);
         return ret;
     }

     /** This methods returns all methods which are centered.
      * @return an list containing all centered methods.
      */
     public static List<MA_METHODS> getCenteredMethods() {
         List<MA_METHODS> ret = new LinkedList<MA_METHODS>();
         ret.add(Center);
         ret.add(CenterG);
         ret.add(HarmonicC);
         return ret;
     }

     /**
     * @param winLength the length of the window.
     * @return the MovingAverage Object.
     */
    public MovingAverage getMAObject(final int winLength) {
        switch(this) {
            case Center:
                return new SimpleMA(winLength);
            case CenterG:
                return WeightedMA.getGaussianWeightedInstance(
                        winLength);
            case Cumulative:
                return new CumulativeMA();
            case Exponential:
                return new ExponentialMA(winLength);
            case Forward:
                return new SimpleMA(winLength);
            case ForwardG:
                return WeightedMA.getForwardGaussianWeightedInstance(
                        winLength);
            case Simple:
                return new SimpleMA(winLength);
            case SimpleG:
                return WeightedMA.getBackwardGaussianWeightedInstance(
                        winLength);
            case OldExponential:
                return new ExponentialOldMA(winLength);
            case DoubleExponential:
                return new DoubleExpMA(winLength);
            case TripleExponential:
                return new TripleExponentialMA(winLength);
            case HarmonicC:
                return new HarmonicMeanMA(winLength);
            default:
                return new SimpleMA(winLength);
        }
     }
}
