package functions;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import main.DoubleData;
import main.MyEvolutionState;

public class CoCPU extends GPNode {
    public String toString() {return "coCpu";}
    public int expectedChildren() { return 0; }




    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem){
        DoubleData rd = (DoubleData)(input);

//        ContainerAllocationProblem p = (ContainerAllocationProblem) problem;
//        rd.x = p.normalizedContainerCpu;
        rd.x = ((MyEvolutionState)state).normalizedContainerCpu;
    }
}
