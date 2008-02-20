/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   23.03.2007 (berthold): created
 */
package org.knime.core.node.workflow;

public class ScopeObjectStackTest {
    
    private static final class ScopeContext1 extends ScopeContext {
    }
    private static final class ScopeContext2 extends ScopeContext {
    }
    
    public void testConstructor() {
        ScopeContext1 s1_1 = new ScopeContext1();
//        ScopeContext1 s1_2 = new ScopeContext1();
        ScopeContext2 s2_1 = new ScopeContext2();
        ScopeObjectStack stack1 = 
            new ScopeObjectStack(new NodeID(1));
        stack1.push(s1_1);
        stack1.push(new ScopeVariable("stack1_a", "Stack 1, Var 1"));
        ScopeObjectStack stack3 = 
            new ScopeObjectStack(new NodeID(3), stack1);
        stack1.push(new ScopeVariable("stack1_b", "Stack 1, Var 2"));
        stack1.push(s2_1);
        stack1.push(new ScopeVariable("stack1_c", "Stack 1, Var 3"));
        ScopeObjectStack stack2 = 
            new ScopeObjectStack(new NodeID(2));
        stack2.push(new ScopeVariable("stack2_a", "Stack 2, Var 1"));
        stack2.push(new ScopeVariable("stack2_b", "Stack 2, Var 2"));
        stack2.push(new ScopeVariable("stack2_c", "Stack 2, Var 3"));
        stack2.push(new ScopeVariable("stack2_d", "Stack 2, Var 4"));
        stack3.push(new ScopeVariable("stack3_a", "Stack 3, Var 1"));
        stack3.push(new ScopeContext2());
        stack3.push(new ScopeVariable("stack3_b", "Stack 3, Var 2"));
        stack3.pop(ScopeContext2.class);
        stack3.push(new ScopeVariable("stack3_c", "Stack 3, Var 3"));
        ScopeObjectStack result = new ScopeObjectStack(
                new NodeID(4), stack1, stack2, stack3);
        System.out.println(result);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new ScopeObjectStackTest().testConstructor();
    }

}
