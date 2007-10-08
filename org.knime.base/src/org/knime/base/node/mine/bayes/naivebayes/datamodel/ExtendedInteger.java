package org.knime.base.node.mine.bayes.naivebayes.datamodel;

/**Simple class which could be used to count the members of an attribute in a
 * <code>Map</code> or <code>Collection</code>.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public final class ExtendedInteger {
    
    private int m_value;
    
    /**Constructor initialise the value with 0.
     */
    public ExtendedInteger() {
        this.m_value = 0;
    }
    
    /**Constructor for class ExtendedInteger.
     * @param value the starting value
     */
    public ExtendedInteger(final int value) {
        this.m_value = value;
    }
    
    
    /**Increases the number of members by one.*/
    public void increment() {
        m_value++;
    }
    
    /**Decreases the number of members by one.*/
    public void decrement() {
        if (m_value < 1) {
            throw new IllegalStateException("Couldn't decrease to a " 
                    + "negative value.");
        }
        m_value--;
    }
    
    /**Resets the number of members to zero.*/
    public void reset() {
        m_value = 0;
    }
    
    /**
     * @return the int value
     */
    public int intValue() {
        return m_value;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Integer.toString(m_value);
    }
}
