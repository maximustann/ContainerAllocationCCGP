package main;

import ec.EvolutionState;
import functions.VMMEMOverhead;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * SubJustFit_FF represents that the algorithm contains three rules,
 * 1. Best-Fit with sub rule for allocating containers to VMs
 * 2. JustFit for VM creation, justFit finds the smallest VM for the container to create
 * 3. First-Fit for VM allocation
 */
public class SubJustFit_FF {
    private double pmCpu;
    private double pmMem;
    private double pmMaxEnergy;
    private double k;

    private double vmCpuOverheadRate;
    private double vmMemOverhead;



    private ArrayList<ArrayList> initVm;
    private ArrayList<ArrayList> initPm;
    private ArrayList<ArrayList> initOs;
    private ArrayList<ArrayList> initContainer;
    private ArrayList<Double[]> vmTypeList;


    private ArrayList<ArrayList<Double[]>> inputX;

    public SubJustFit_FF(
            double pmCpu,
            double pmMem,
            double pmMaxEnergy,
            double k,
            double vmCpuOverheadRate,
            double vmMemOverhead,
            ArrayList<ArrayList<Double[]>> inputX,
            ArrayList<ArrayList> initVm,
            ArrayList<ArrayList> initContainer,
            ArrayList<ArrayList> initOs,
            ArrayList<ArrayList> initPm,
            ArrayList<Double[]> vmTypeList
    ){
        this.pmCpu = pmCpu;
        this.pmMem = pmMem;
        this.pmMaxEnergy = pmMaxEnergy;
        this.k = k;
        this.vmCpuOverheadRate = vmCpuOverheadRate;
        this.vmMemOverhead = vmMemOverhead;
        this.initVm = initVm;
        this.initContainer = initContainer;
        this.initOs = initOs;
        this.initPm = initPm;
        this.inputX = inputX;
        this.vmTypeList = vmTypeList;
    }



    public double allocate(
            int testCase
    ){
        // testCaseNum equals the current generation
        ArrayList<Double[]> containers = inputX.get(testCase);
        double ae = 0;

        // pmList, the real utilization of PM
        ArrayList<Double[]> pmList = new ArrayList<>();

        // pmStatusList, the VM boundary of PM
        ArrayList<Double[]> pmStatusList = new ArrayList<>();
//    private ArrayList<Integer[]> vmPmIndexMapping;
        ArrayList<Double[]> vmList = new ArrayList<>();
        HashMap<Integer, Integer> VMPMMapping = new HashMap<>();
        HashMap<Integer, Integer> vmIndexTypeMapping = new HashMap<>();

        // Initialize data center
        initializeDataCenter(
                testCase,
                initPm,
                initVm,
                initContainer,
                initOs,
                pmList,
                pmStatusList,
                vmList,
                VMPMMapping,
                vmIndexTypeMapping
                );
//        Double Energy = energyCalculation(pmActualUsageList);
        ArrayList<Double> accumulateEnergy = new ArrayList<>();
        double energy = calEnergy(pmList);

        for(Double[] container:containers){

            double containerCpu = container[0];
            double containerMem = container[1];
            int containerOs = container[2].intValue();

            if(!bestFit(container, pmList, vmList, VMPMMapping)){
                int newVmIndex = justFit(container);
                Double[] newVM = new Double[]{vmTypeList.get(newVmIndex)[0], vmTypeList.get(newVmIndex)[1], container[2]};
                newVM[0] -= (container[0] + newVM[0] * vmCpuOverheadRate);
                newVM[1] -= (container[1] + vmMemOverhead);
                vmList.add(newVM);
                vmIndexTypeMapping.put(vmList.size() - 1, newVmIndex);
//                System.out.println(vmList.size());
                if(!firstFit(newVmIndex, container, pmList, pmStatusList, vmList, VMPMMapping)){
                    Double[] newPM = new Double[]{pmCpu, pmMem};
                    Double[] newPmStatus = new Double[]{pmCpu, pmMem};
                    newPM[0] -= (vmTypeList.get(newVmIndex)[0] * vmCpuOverheadRate + container[0]);
                    newPM[1] -= (vmMemOverhead + container[1]);
                    newPmStatus[0] -= vmTypeList.get(newVmIndex)[0];
                    newPmStatus[1] -= vmTypeList.get(newVmIndex)[1];
                    pmList.add(newPM);
                    pmStatusList.add(newPmStatus);
//                    vmPmIndexMapping.add(new Integer[]{vmList.size() - 1, pmList.size() - 1});
                    VMPMMapping.put(vmList.size() - 1, pmList.size() - 1);
//                    System.out.println(vmList.size() + " : " + pmList.size());
                }
            }
            accumulateEnergy.add(calEnergy(pmList));
//            double increment = calEnergy(pmList);
//            energy += increment;
        }
        double energyAll = 0;
        for(int i = 0; i < accumulateEnergy.size(); i++){
            energyAll += accumulateEnergy.get(i);
        }
//        averageEnergy /= containers.size();
//        double energy = 0;



        return energyAll;
    }

    private void initializeDataCenter(
            int testCase,
            ArrayList<ArrayList> initPm,
            ArrayList<ArrayList> initVm,
            ArrayList<ArrayList> initContainer,
            ArrayList<ArrayList> initOs,
            ArrayList<Double[]> pmList,
            ArrayList<Double[]> pmStatusList,
            ArrayList<Double[]> vmList,
            HashMap<Integer, Integer> VMPMMapping,
            HashMap<Integer, Integer> vmIndexTypeMapping
            ){

        ArrayList<Double[]> initPmList = initPm.get(testCase);
        ArrayList<Double[]> initVmList = initVm.get(testCase);
        ArrayList<Double[]> containerList = initContainer.get(testCase);
        ArrayList<Double[]> osList = initOs.get(testCase);


        int globalVmCounter = 0;
        // for each PM, we have an array of VM: vms[]
        for(Double[] vms:initPmList){

            // Create a new PM
            pmStatusList.add(new Double[]{
                    pmCpu,
                    pmMem });

            pmList.add(new Double[]{
                    pmCpu,
                    pmMem
            });

            // for this each VM
            for(int vmCounter = 0; vmCounter < vms.length; ++vmCounter){

                // Get the type of this VM
                int vmType = vms[vmCounter].intValue() - 1;

                // Get the OS type
                Double[] os = osList.get(vmCounter + globalVmCounter);

                // Create this VM
                vmList.add(new Double[]{
                        vmTypeList.get(vmType)[0] - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
                        vmTypeList.get(vmType)[1] - vmMemOverhead,
                        new Double(os[0])
                });


                // get the containers allocated on this VM
                Double[] containers = initVmList.get(vmCounter + globalVmCounter);

                // Allocate the VM to this PM,
                // Allocation includes two part, first, pmResourceList indicates the left resource of PM (subtract entire VMs' size)
                // pmIndex denotes the last PM. pmIndex should be at least 0.
                int pmIndex = pmStatusList.size() - 1;

                // update the pm left resources
                pmStatusList.set(pmIndex, new Double[]{
                        pmStatusList.get(pmIndex)[0] - vmTypeList.get(vmType)[0],
                        pmStatusList.get(pmIndex)[1] - vmTypeList.get(vmType)[1]
                });


                // The second part of allocation,
                // We update the actual usage of PM's resources
                pmList.set(pmIndex, new Double[]{
                        pmList.get(pmIndex)[0] - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
                        pmList.get(pmIndex)[1] - vmMemOverhead
                });

                // Map the VM to the PM
                VMPMMapping.put(vmCounter + globalVmCounter, pmIndex);
                // for each container
                for(int conContainer = containers[0].intValue() - 1;
                    conContainer < containers[containers.length - 1].intValue();
                    ++conContainer){

                    // Get the container's cpu and memory
                    Double[] cpuMem = containerList.get(conContainer);

                    //Create this container
                    // get the left resources of this VM
                    int vmIndex = vmList.size() - 1;
                    Double[] vmCpuMem = vmList.get(vmIndex);

                    // update the vm
                    vmList.set(vmIndex, new Double[] {
                            vmCpuMem[0] - cpuMem[0],
                            vmCpuMem[1] - cpuMem[1],
                            new Double(os[0])
                    });

                    // Whenever we create a new VM, map its index in the VMResourceList to its type for future purpose
//                    vmIndexTypeMapping.put(vmResourceList.size() - 1, vmType);
                    vmIndexTypeMapping.put(vmList.size() - 1, vmType);

                    // Add the Actual usage to the PM
                    // Here, we must consider the overhead
                    Double[] pmCpuMem = pmList.get(pmIndex);

                    // update the pm
                    pmList.set(pmIndex, new Double[]{
                            pmCpuMem[0] - cpuMem[0],
                            pmCpuMem[1] - cpuMem[1]
                    });


//                    vm.addContainer(container);
                } // Finish allocate containers to VMs
            } // End  of each VM
            // we must update the globalVmCounter
            globalVmCounter += vms.length;

        } // End of each PM


//        double energy = energyCalculation(pmActualUsageList);
    }

    private boolean firstFit(
            int vmIndex,
            Double[] container,
            ArrayList<Double[]> pmList,
            ArrayList<Double[]> pmStatusList,
            ArrayList<Double[]> vmList,
            HashMap<Integer, Integer> VMPMMapping){
        boolean allocated = false;
        for(int i = 0; i < pmList.size(); i++){
            Double[] pm = pmList.get(i);
            Double[] pmStatus = pmStatusList.get(i);
            if(pmStatus[0] >= vmTypeList.get(vmIndex)[0] &&
                    pmStatus[1] >= vmTypeList.get(vmIndex)[1]){
                allocated = true;
                pm[0] -= vmTypeList.get(vmIndex)[0] * vmCpuOverheadRate + container[0];
                pm[1] -= vmMemOverhead + container[1];
                pmStatus[0] -= vmTypeList.get(vmIndex)[0];
                pmStatus[1] -= vmTypeList.get(vmIndex)[1];
                VMPMMapping.put(vmList.size() - 1, i);
//                System.out.println(vmList.size()-1 + " : " + i);
                break;
            }
        }
        return allocated;
    }

    // select the smallest (memory) VM type
    private int justFit(Double[] container){
        int selectIndex = 0;
        int vmCounter = 0;
        double bestValue = pmMem;
        for(Double[] vmType:vmTypeList){
            if(vmType[0] >= container[0] + vmType[0] * vmCpuOverheadRate && vmType[1] >= container[1] + vmMemOverhead){
                double leftMem = vmType[1] - container[1];
                if(leftMem < bestValue) {
                    selectIndex = vmCounter;
                    bestValue = leftMem;
                }
            }
            vmCounter++;
        }
        return selectIndex;
    }

    private boolean bestFit(
            Double[] container,
            ArrayList<Double[]> pmList,
            ArrayList<Double[]> vmList,
            HashMap<Integer, Integer> VMPMMapping){

        boolean allocated = false;
        int vmCounter = 0;
        int bestVmIndex = 0;
        double bestValue = 50000;
        for(Double[] vm:vmList){
            double vmCpu = vm[0];
            double vmMem = vm[1];
            int vmOs = vm[2].intValue();

            double containerCpu = container[0];
            double containerMem = container[1];
            int containerOs = container[2].intValue();

            if(vmCpu >= containerCpu && vmMem >= containerMem && vmOs == containerOs){
                allocated = true;
                double cpuLeft = vm[0] - container[0];
                double memLeft = vm[1] - container[1];
                double subValue = Math.abs(cpuLeft - memLeft);
                if(subValue < bestValue){
                    bestValue = subValue;
                    bestVmIndex = vmCounter;
                }
            }
            vmCounter++;
        }

        if(allocated){
            Double[] vm = vmList.get(bestVmIndex);
            vm[0] -= container[0];
            vm[1] -= container[1];

//            for(Integer[] vmPmIndex:vmPmIndexMapping){
//                if(vmPmIndex[0] == bestVmIndex){
//                    Double[] pm = pmList.get(vmPmIndex[1]);
//                    pm[0] -= container[0];
//                    pm[1] -= container[1];
//                }
//            }
//            System.out.println("bestVmIndex: " + bestVmIndex);
            int pmIndex = VMPMMapping.get(bestVmIndex);
            Double[] pm = pmList.get(pmIndex);
            pm[0] -= container[0];
            pm[1] -= container[1];
        }
        return allocated;
    }



    private double calEnergy(ArrayList<Double[]> pmList){
        double energy = 0;
        for(Double[] pm:pmList){
            energy += k * pmMaxEnergy + (1 - k) * pm[0] / pmCpu;
        }
        return energy;
    }
}
