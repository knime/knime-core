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
 * -------------------------------------------------------------------
 * 
 */

package org.knime.base.node.mine.cluster.hierarchical;

import org.knime.base.node.mine.cluster.hierarchical.distfunctions.DistanceFunction;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.EuclideanDist;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.ManhattanDist;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * A dialog to get the number of output clusters,
 * the distance function and the linkage type for cluster.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
class HierarchicalClusterNodeDialog extends DefaultNodeSettingsPane {
    
    /**
     * An array with the available distance functions names.
     */   
    public static final DistanceFunction[] DISTANCE_FUNCTIONS 
            = new DistanceFunction[]{
            EuclideanDist.EUCLIDEAN_DISTANCE, 
            ManhattanDist.MANHATTEN_DISTANCE};
    
    private static String[] linkageTypes;
    
    private static String[] distanceFunctionNames;
    
    /**
     * Puts the names of the linkage enum fields into a string array
     * and the names of the distance functions in another string array.
     */
    static {
        linkageTypes 
            = new String[HierarchicalClusterNodeModel.Linkage.values().length];
        int i = 0;
        for (HierarchicalClusterNodeModel.Linkage l 
                : HierarchicalClusterNodeModel.Linkage.values()) {
            linkageTypes[i++] = l.name();
        }
        i = 0;
        distanceFunctionNames 
            = new String[DistanceFunction.Names.values().length];
        for (DistanceFunction.Names n : DistanceFunction.Names.values()) {
            distanceFunctionNames[i++] = n.name();
        }
    }
         
    /**
     * Creates a new <code>NodeDialogPane</code> for hierarchical clustering 
     * in order to set the parameters.
     */
    HierarchicalClusterNodeDialog() {
        
        addDialogComponent(new DialogComponentNumber(
                createSettingsNumberOfClusters(),
                "Number output cluster:", 1));
        
        addDialogComponent(new DialogComponentStringSelection(
                createSettingsDistanceFunction(),
                        "Distance function:", distanceFunctionNames));        
        
        addDialogComponent(new DialogComponentStringSelection(
                createSettingsLinkageType(),
                        "Linkage type:", linkageTypes)); 

        addDialogComponent(new DialogComponentBoolean(
                createSettingsCacheKeys(), "Cache distances"));
        
        Class[] allowedTypes = {DoubleValue.class, IntValue.class};
        addDialogComponent(new DialogComponentColumnFilter(
                createSettingsColumns(), 0, allowedTypes));
    }
    
    static SettingsModelIntegerBounded createSettingsNumberOfClusters() {
        return new SettingsModelIntegerBounded(
          HierarchicalClusterNodeModel.NRCLUSTERS_KEY, 3, 1, Integer.MAX_VALUE);
    }
    
    static SettingsModelString createSettingsDistanceFunction() {
        return new SettingsModelString(
                HierarchicalClusterNodeModel.DISTFUNCTION_KEY,
                distanceFunctionNames[0]);   
    }
    
    static SettingsModelString createSettingsLinkageType() {
        return new SettingsModelString(
                HierarchicalClusterNodeModel.LINKAGETYPE_KEY,
                HierarchicalClusterNodeModel.Linkage.SINGLE.name());
    }
    
    static SettingsModelBoolean createSettingsCacheKeys() {
        return new SettingsModelBoolean(
                HierarchicalClusterNodeModel.USE_CACHE_KEY, true);
    }
    
    static SettingsModelFilterString createSettingsColumns() {
        return new SettingsModelFilterString(
                HierarchicalClusterNodeModel.SELECTED_COLUMNS_KEY);
    }
        
        

}
