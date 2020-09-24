package main;

import ec.EvolutionState;
import ec.gp.GPIndividual;

/**
 *  My(Max's) own version of GPIndividual,
 *  The only difference is the output form
 *  The original output has one style from [Graphviz, latex, c, lisp]
 *
 *  My version include three styles: Graphviz, lisp, and c
 *
 *  Special thank to J.Park
 */
public class MYGPIndividual extends GPIndividual {
    @Override
    public void printTrees(EvolutionState state, int log) {
        for(int x = 0; x < this.trees.length; ++x) {
            // graph representation
            state.output.println("Tree " + x + ":", log);
            state.output.println("\n", log);
            state.output.println(this.trees[x].child.makeGraphvizTree(), log);
            // lisp tree
            state.output.println("lisp style: ", log);
            this.trees[x].child.printRootedTreeForHumans(state, log, 0, 0);
            state.output.println("\n", log);
            // c tree
            state.output.println("c style: ",  log);
            state.output.println(this.trees[x].child.makeCTree(true,
                    this.trees[x].printTerminalsAsVariablesInC, this.trees[x].printTwoArgumentNonterminalsAsOperatorsInC), log);

        } // end for
    } // end printTrees
}
