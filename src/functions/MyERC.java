package functions;

import ec.EvolutionState;
import ec.Problem;
import ec.app.regression.func.RegERC;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import main.DoubleData;

/**
 * A constant terminal.
 */
public class MyERC extends RegERC {

    /**
     * Creates a random constant in the interval (0,1)
     *
     * @param state
     * @param thread
     */
    @Override
    public void resetNode(final EvolutionState state, final int thread) {
        value = state.random[thread].nextDouble();
    }

    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem) {
        DoubleData rd = ((DoubleData) (input));
        rd.x = value;
    }
}
