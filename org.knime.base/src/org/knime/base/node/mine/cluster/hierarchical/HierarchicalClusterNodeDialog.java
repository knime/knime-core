/*
 * ------------------------------------------------------------------- 
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
