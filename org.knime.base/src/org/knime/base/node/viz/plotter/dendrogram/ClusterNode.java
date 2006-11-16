package org.knime.base.node.viz.plotter.dendrogram;

import java.util.Vector;

import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Represents a Node in the hirarchie tree (Dendrogram)
 * of a hierarchical clustering.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ClusterNode {
    
    /**
     * The data row which represents the leaf of a hierarchie tree.
     * Is null if this node is not a leaf.
     */    
    private DataRow m_leafDataPoint;
    
    private int m_rowIdx = -1;
    
    private double m_dist;

    /**
     * Holds the child nodes of a hierarchie sub tree.
     * Is empty if this node is a leaf node.
     * In this implementation a parent has two child nodes. (Binary tree)
     */        
    private ClusterNode[] m_hierarchieNodes;
    
    /**
     * Indicates wheather this is a leaf node or not.
     */        
    private boolean m_isLeaf;
    
    /**
     * Constructs a new leaf node from a data row.
     * 
     * @param row data row to create a node for
     * @param rowIdx the row index for later reconstruction in 
     * load/save internals.
     */
    public ClusterNode(final DataRow row, final int rowIdx) {
        m_rowIdx = rowIdx;
        m_leafDataPoint = row;
        m_isLeaf = true;
    }
    
    /**
     * 
     * @return the index of the row if the node is a leaf node -1 otherwise.
     */
    public int getRowIndex() {
        return m_rowIdx;
    }
    
     /**
      * Constructs a new parent node from two child nodes.
      * 
      * @param node1 the first node to create a parent node for
      * @param node2 the second node to create a parent node for
      * @param dist the distance to the node.
      */
     public ClusterNode(final ClusterNode node1, 
                        final ClusterNode node2, final double dist) {
        m_hierarchieNodes = new ClusterNode[2];
        m_hierarchieNodes[0] = node1;
        m_hierarchieNodes[1] = node2;
        m_dist = dist;
         
     }
     

    /**
     * Returns all data row (leaf nodes) this sub tree.
     * 
     * @return the array of data rows which are included in this sub tree.
     */   
    public DataRow[] getAllDataRows() {
          
        // if already a leaf node    
        if (m_isLeaf) {
            
            DataRow[] rows = {m_leafDataPoint};
        
            return rows;
        }
        
        // else recursivly get the rows
        
        Vector<DataRow> rowVector = new Vector<DataRow>();
        getDataRows(this, rowVector);
        
        DataRow[] rows = new DataRow[rowVector.size()];
        rows = rowVector.toArray(rows);
        
        return rows;
    }

    /**
     * Puts all data rows of a node in a vector if the node is a leaf.
     * Otherwise the method is invoked with the nodes child nodes.
     * This is a recursive tree traversing method.
     * 
     * @param clusterNode the node to get the data rows from.
     * @param rowVector the vector to store the found data rows in.
     */       
    private void getDataRows(final ClusterNode clusterNode,
                             final Vector<DataRow> rowVector) {
        
        // rekursives auslesen aller data rows
        ClusterNode subNode1 = clusterNode.getFirstSubnode();
        ClusterNode subNode2 = clusterNode.getSecondSubnode();
       
        if (subNode1.m_isLeaf) {
        
            rowVector.add(subNode1.m_leafDataPoint);
        } else {
            
            getDataRows(subNode1, rowVector);
        }
        
        if (subNode2.m_isLeaf) {
        
            rowVector.add(subNode2.m_leafDataPoint);
        } else {
            
            getDataRows(subNode2, rowVector);
        }

    }

    /**
     * Returns the first subnode of this node.
     * This method is implemented because of the binary characteristique
     * of this tree.
     * 
     * @return the first sub node.
     */   
    public ClusterNode getFirstSubnode() {
        if (m_hierarchieNodes == null) {
            return null;
        }
        return m_hierarchieNodes[0];
    }
    
    /**
     * 
     * @return the distance to the next level.
     */
    public double getDist() {
        if (isLeaf()) {
            return 0;
        }
        return m_dist;
    }
    
    /**
     * 
     * @return true if the node is a leaf node.
     */
    public boolean isLeaf() {
        return m_isLeaf;
    }
    
    /**
     * @param node the node
     * @return the maximum distance to the leaf node.
     */
    public double getMaxDistance(final ClusterNode node) {
        if (node.getFirstSubnode() == null && node.getSecondSubnode() == null) {
            return m_dist;
        }
        double dist1 = getMaxDistance(node.getFirstSubnode());
        double dist2 = getMaxDistance(node.getSecondSubnode());
        double dist3 = Math.max(dist1, dist2);
        return Math.max(dist3, node.getDist());
    }

    
    /**
     * 
     * @return the name of this node (the row keys it contains).
     */
    public String getName() {
        if (isLeaf()) {
            return m_leafDataPoint.getKey().getId().toString();
        } else {
            return getFirstSubnode().getName() + getSecondSubnode().getName();
        }
    }
    
    
    /**
     * 
     * @return the leaf data point
     */
    public DataRow getLeafDataPoint() {
        return m_leafDataPoint;
    }
    

    /**
     * Returns the second subnode of this node.
     * This method is implemented because of the binary characteristique
     * of this tree.
     * 
     * @return the second sub node.
     */        
    public ClusterNode getSecondSubnode() {
        if (m_hierarchieNodes == null) {
            return null;
        }
        return m_hierarchieNodes[1];
    }
    
    
    /**
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("dist: " + m_dist);
        if (m_isLeaf) {
            buffer.append("leaf: " + m_leafDataPoint.getKey().getId());
        } else {
            buffer.append("left: " + getFirstSubnode() 
                    + " right: " + getSecondSubnode());
        }
        return buffer.toString();
    }
    
    private static final String CFG_DISTANCE = "distance";
    private static final String CFG_LEAF = "isLeaf";
    private static final String CFG_LEFTCHILD = "leftChild";
    private static final String CFG_RIGHTCHILD = "rightChild";
    private static final String CFG_ROW_IDX = "row";
    
    private ClusterNode() {
        
    }
    
    /**
     * Saves the tree structure into the config. Stores the distance,
     * the rowy key (if its a leaf) and the left and right child.
     * @param settings the config to save to.
     */
    public void saveToXML(final NodeSettingsWO settings) {
        // each node stores its distance
        settings.addDouble(CFG_DISTANCE, m_dist);
        settings.addBoolean(CFG_LEAF, m_isLeaf);
        // if leaf node store the referring data point
        if (isLeaf()) {
            // TODO: store the whole data row
            settings.addInt(CFG_ROW_IDX, m_rowIdx);
        }
        // and the left and the right child
        NodeSettingsWO left = (NodeSettingsWO)settings.addConfig(
                CFG_LEFTCHILD);
        NodeSettingsWO right = (NodeSettingsWO)settings.addConfig(
                CFG_RIGHTCHILD);
        if (getFirstSubnode() != null && getSecondSubnode() != null) {
            getFirstSubnode().saveToXML(left);
            getSecondSubnode().saveToXML(right);
        }
    }
    
    /**
     * Loads a cluster node from the settings.
     * @param settings the config to load from
     * @param orgTable the original talbe containing the rows in the same order!
     * @return a cluster node
     * @throws InvalidSettingsException if not stored properly.
     */
    public static ClusterNode loadFromXML(final NodeSettingsRO settings, 
            final DataArray orgTable) 
        throws InvalidSettingsException {
        ClusterNode node = new ClusterNode();
        double dist = settings.getDouble(CFG_DISTANCE);
        node.m_dist = dist;
        boolean isLeaf = settings.getBoolean(CFG_LEAF);
        node.m_isLeaf = isLeaf;
        node.m_hierarchieNodes = new ClusterNode[2];
        if (isLeaf) {
            node.m_isLeaf = true;
            int rowIdx = settings.getInt(CFG_ROW_IDX);
            node.m_leafDataPoint = orgTable.getRow(rowIdx);
            node.m_rowIdx = rowIdx;
        } else {
            NodeSettingsRO leftSettings = settings.getNodeSettings(
                    CFG_LEFTCHILD);
            NodeSettingsRO rightSettings = settings.getNodeSettings(
                    CFG_RIGHTCHILD);
            node.m_hierarchieNodes[0] = loadFromXML(leftSettings, orgTable);
            node.m_hierarchieNodes[1] = loadFromXML(rightSettings, orgTable);
        }
        return node;
    }
}
