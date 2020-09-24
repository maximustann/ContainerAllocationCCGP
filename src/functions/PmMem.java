package functions;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import main.DoubleData;
import main.ContainerAllocationProblem;
import main.MyEvolutionState;

public class PmMem extends GPNode{
    public String toString() {return "pmMem";}
    public int expectedChildren() { return 0; }


    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem){
        DoubleData rd = (DoubleData)(input);
        MyEvolutionState myState = (MyEvolutionState) state;

//        ContainerAllocationProblem p = (ContainerAllocationProblem) problem;
//        rd.x = p.currentPmCpuRemain;
        rd.x = myState.normalizedPmMemRemain - myState.normalizedVmMemCapacity;
    }
}
