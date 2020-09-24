package main;

import com.opencsv.CSVReader;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.ADFStack;
import ec.gp.GPIndividual;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class MainAllocationProcessVM {

    public final double PMCPU;
    public final double PMMEM;
    public final double PMENERGY;

    // A list of containers
    private ArrayList<ArrayList<Double[]>> inputX = new ArrayList<>();

    // A list of candidate VMs, each vm has an array which includes its CPU and Mem capacity
    private ArrayList<Double[]> vmTypeList = new ArrayList<>();

    // An array of OS probability
    private ArrayList<Double> OSPro = new ArrayList<>();

    // An array of benchmark accumulated energy
    private ArrayList<Double> benchmark = new ArrayList<>();


    // Initialization data
    ArrayList <ArrayList> initPm;
    ArrayList <ArrayList> initVm;
    ArrayList <ArrayList> initOs;
    ArrayList <ArrayList> initContainer;


    private final double vmCpuOverheadRate;
    private final double vmMemOverhead;

    public double containerCpu;
    public double containerMem;
    public int containerOs;
    public double containerOsPro;


    public double normalizedContainerCpu;
    public double normalizedContainerMem;
    public double normalizedVmCpuCapacity;
    public double normalizedVmMemCapacity;


    public double currentPmCpuRemain;
    public double currentPmMemRemain;

    public double normalizedVmCpuOverhead;
    public double normalizedVmMemOverhead;

    public double normalizedGlobalCpuWaste;
    public double normalizedGlobalMemWaste;

    public double normalizedPmCpuRemain;
    public double normalizedPmMemRemain;

    public double normalizedPmActualCpuUsed;
    public double normalizedPmActualMemUsed;

    public double normalizedVmActualCpuUsed;
    public double normalizedVmActualMemUsed;



    public boolean newVmFlag = false;


    private final double k;
//    private final int start;
//    private final int end;


    private ContainerAllocationProblem containerAllocationProblem;
//    private final String testCasePath;
//    private final String osPath;
//    private final String vmConfigPath;
//    private final String osProPath;




    // Constructor
    public MainAllocationProcessVM(
                                    ContainerAllocationProblem containerAllocationProblem,
                                    EvolutionState state,
                                    double PMCPU,
                                    double PMMEM,
                                    double PMENERGY,
                                    double vmCpuOverheadRate,
                                    double vmMemOverhead,
                                    double k)
    {

        this.containerAllocationProblem = containerAllocationProblem;
//        this.testCasePath = testCasePath;
//        this.osPath = osPath;
//        this.vmConfigPath = vmConfigPath;
//        this.osProPath = osProPath;
        this.PMCPU = PMCPU;
        this.PMMEM = PMMEM;
        this.PMENERGY = PMENERGY;
        this.vmCpuOverheadRate = vmCpuOverheadRate;
        this.vmMemOverhead = vmMemOverhead;
        this.k = k;
//        this.start = (int) start;
//        this.end = (int) end;

//        System.out.println(testCasePath);
//        System.out.println(vmConfigPath);
//        System.out.println(osPath);

        // Initialize state
        MyEvolutionState myEvolutionState = (MyEvolutionState) state;
        myEvolutionState.PMCPU = PMCPU;
        myEvolutionState.PMMEM = PMMEM;
        myEvolutionState.PMENERGY = PMENERGY;

//
//        readEnvData(initEnvPath);
//        readFromFiles(this.start, this.end - 1);
//        readVMConfig();
//        readOSPro();

    }

//    private void readEnvData(String initEnvPath){
//        ReadConfigures readEnvConfig = new ReadConfigures();
//
//        initPm = readEnvConfig.testCases(initEnvPath, "pm", start, end);
//        initVm = readEnvConfig.testCases(initEnvPath, "vm", start, end);
//        initOs = readEnvConfig.testCases(initEnvPath, "os", start, end);
//        initContainer = readEnvConfig.testCases(initEnvPath, "container", start, end);
//
//    }
    private void initializeDataCenter(int testCase,
                                      ArrayList<Double[]> pmResourceList,
                                      ArrayList<Double[]> pmActualUsageList,
                                      ArrayList<Double[]> vmResourceList,
                                      ArrayList<Double[]> vmTypeList,
                                      ArrayList<ArrayList> initPm,
                                      ArrayList<ArrayList> initVm,
                                      ArrayList<ArrayList> initContainer,
                                      ArrayList<ArrayList> initOs,
                                      HashMap<Integer, Integer> VMPMMapping,
                                      HashMap<Integer, Integer> vmIndexTypeMapping){

        ArrayList<Double[]> initPmList = initPm.get(testCase);
        ArrayList<Double[]> initVmList = initVm.get(testCase);
        ArrayList<Double[]> containerList = initContainer.get(testCase);
        ArrayList<Double[]> osList = initOs.get(testCase);


        int globalVmCounter = 0;
        // for each PM, we have an array of VM: vms[]
        for(Double[] vms:initPmList){

            // Create a new PM
            pmResourceList.add(new Double[]{
                    PMCPU,
                    PMMEM });

            pmActualUsageList.add(new Double[]{
                    PMCPU,
                    PMMEM
            });

            // for this each VM
            for(int vmCounter = 0; vmCounter < vms.length; ++vmCounter){

                // Get the type of this VM
                int vmType = vms[vmCounter].intValue() - 1;

                // Get the OS type
                Double[] os = osList.get(vmCounter + globalVmCounter);

                // Create this VM
                vmResourceList.add(new Double[]{
                        vmTypeList.get(vmType)[0] - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
                        vmTypeList.get(vmType)[1] - vmMemOverhead,
                        new Double(os[0])
                });


                // get the containers allocated on this VM
                Double[] containers = initVmList.get(vmCounter + globalVmCounter);

                // Allocate the VM to this PM,
                // Allocation includes two part, first, pmResourceList indicates the left resource of PM (subtract entire VMs' size)
                // pmIndex denotes the last PM. pmIndex should be at least 0.
                int pmIndex = pmResourceList.size() - 1;

                // update the pm left resources
                pmResourceList.set(pmIndex, new Double[]{
                        pmResourceList.get(pmIndex)[0] - vmTypeList.get(vmType)[0],
                        pmResourceList.get(pmIndex)[1] - vmTypeList.get(vmType)[1]
                });

                // The second part of allocation,
                // We update the actual usage of PM's resources
                pmActualUsageList.set(pmIndex, new Double[]{
                        pmActualUsageList.get(pmIndex)[0] - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
                        pmActualUsageList.get(pmIndex)[1] - vmMemOverhead
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
                    int vmIndex = vmResourceList.size() - 1;
                    Double[] vmCpuMem = vmResourceList.get(vmIndex);

                    // update the vm
                    vmResourceList.set(vmIndex, new Double[] {
                            vmCpuMem[0] - cpuMem[0],
                            vmCpuMem[1] - cpuMem[1],
                            new Double(os[0])
                    });

                    // Whenever we create a new VM, map its index in the VMResourceList to its type for future purpose
                    vmIndexTypeMapping.put(vmResourceList.size() - 1, vmType);

                    // Add the Actual usage to the PM
                    // Here, we must consider the overhead
                    Double[] pmCpuMem = pmActualUsageList.get(pmIndex);

                    // update the pm
                    pmActualUsageList.set(pmIndex, new Double[]{
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

    // Read two column from the testCase file
//    private ArrayList<Double[]> readFromFile(String path, String osPath){
//        ArrayList<Double[]> data = new ArrayList<>();
//        try {
//            Reader reader = Files.newBufferedReader(Paths.get(path));
//            Reader readerOS = Files.newBufferedReader(Paths.get(osPath));
//            CSVReader csvReader = new CSVReader(reader);
//            CSVReader csvReaderOS = new CSVReader(readerOS);
//            String[] nextRecord;
//            String[] nextRecordOS;
//            while((nextRecord = csvReader.readNext()) != null && (nextRecordOS = csvReaderOS.readNext()) != null){
//                // [0] is for cpu, [1] is for mem
//                Double[] container = new Double[3];
//                container[0] = Double.parseDouble(nextRecord[0]);
//                container[1] = Double.parseDouble(nextRecord[1]);
//                container[2] = Double.parseDouble(nextRecordOS[0]);
//                data.add(container);
//            }
//            reader.close();
//            readerOS.close();
//            csvReader.close();
//            csvReaderOS.close();
//        } catch (IOException e1){
//            e1.printStackTrace();
//        }
//        return data;
//    }
//
//    private void readOSPro(){
//        try {
//            Reader reader = Files.newBufferedReader(Paths.get(osProPath));
//            CSVReader csvReader = new CSVReader(reader);
//            String[] nextRecord;
//            while((nextRecord = csvReader.readNext()) != null){
//                Double pro;
//                pro = Double.parseDouble(nextRecord[0]);
//                OSPro.add(pro);
//            }
//        } catch (IOException e1){
//            e1.printStackTrace();
//        }
//    }
//
//    private void readVMConfig(){
//        try {
//            Reader reader = Files.newBufferedReader(Paths.get(vmConfigPath));
//            CSVReader csvReader = new CSVReader(reader);
//            String[] nextRecord;
//            while((nextRecord = csvReader.readNext()) != null){
//                Double[] vm = new Double[2];
//                vm[0] = Double.parseDouble(nextRecord[0]);
//                vm[1] = Double.parseDouble(nextRecord[1]);
//                vmTypeList.add(vm);
//            }
//        } catch (IOException e1){
//            e1.printStackTrace();
//        }
//    }
//
//    // we read containers from file
//    private void readFromFiles(int start, int end){
//
//        for(int i = start; i <= end; ++i){
//            String path = testCasePath + i + ".csv";
//            String pathOS = osPath + i + ".csv";
//            inputX.add(readFromFile(path, pathOS));
//        }
//
//    }


    /**
     * Calculate the energy consumption using the following equation:
     * Energy = k * MaxEnergy + (1 - k)  * MaxEnergy * utilization_of_a_PM
     * @param pmActualUsageList
     * @return the energy consumption
     */
    private Double energyCalculation(ArrayList<Double[]> pmActualUsageList){
        Double energy = 0.0;
        for(Double[] pmActualResource:pmActualUsageList){
            energy += ((PMCPU - pmActualResource[0]) / PMCPU) * PMENERGY * (1 - k) + k * PMENERGY;
        }
        return energy;
    }

    private Double pmRemain(ArrayList<Double[]> pmResourceList){
//        Double meanPmUtil = 0.0;
//        for(Double[] pmActualResource:pmActualUsageList){
//            meanPmUtil += pmActualResource[0] / PMCPU;
//        }
        Double meanPmResource = 0.0;
        for(Double[] pmResource:pmResourceList){
            meanPmResource += (pmResource[0] / PMCPU);
        }
//        meanPmUtil /= pmActualUsageList.size();
        meanPmResource /= pmResourceList.size();
//        System.out.println("meanPmUtil = " + meanPmUtil);
//        System.out.println("meanPmResource = " + meanPmResource);


        return meanPmResource;
    }

    public ArrayList<Double> evaluate(
                                DoubleData input,
                                EvolutionState state,
                                ArrayList<ArrayList<Double[]>> inputX,
                                ArrayList<ArrayList> initVm,
                                ArrayList<ArrayList> initContainer,
                                ArrayList<ArrayList> initOs,
                                ArrayList<ArrayList> initPm,
                                ArrayList<Double[]> vmTypeList,
                                GPIndividual vmSelectionCreationRule,
                                GPIndividual vmAllocationRule,
                                int threadnum,
                                ADFStack stack){

        MyEvolutionState myEvolutionState = (MyEvolutionState) state;
        // testCaseNum equals the current generation
        int testCase = state.generation;
        // initialize the resource lists
        ArrayList<Double> resultList = new ArrayList<>();
        ArrayList<Double> comparedResultList = new ArrayList<>();

        // Loop through the testCases
//        for (int testCase = 0; testCase <= end - start - 1; ++testCase) {


            double globalCPUWaste = 0;
            double globalMEMWaste = 0;
            ArrayList<Double[]> pmResourceList = new ArrayList<>();
            ArrayList<Double[]> pmActualUsageList = new ArrayList<>();
            ArrayList<Double[]> vmResourceList = new ArrayList<>();
            HashMap<Integer, Integer> VMPMMapping = new HashMap<>();
            HashMap<Integer, Integer> vmIndexTypeMapping = new HashMap<>();

            // Initialize data center
            initializeDataCenter(testCase,
                    pmResourceList,
                    pmActualUsageList,
                    vmResourceList,
                    vmTypeList,
                    initPm,
                    initVm,
                    initContainer,
                    initOs,
                    VMPMMapping,
                    vmIndexTypeMapping);

            // the total energy
            Double Energy = energyCalculation(pmActualUsageList);
//            Double pmFit = pmUtilRemain(pmActualUsageList, pmResourceList);


            // No data center initialization
//                Double Energy = 0.0;
            // Get all the containers
            ArrayList<Double[]> containers = inputX.get(testCase);



            // Start simulation
            for (Double[] container:containers) {

                containerCpu = container[0];
                containerMem = container[1];
                containerOs = container[2].intValue();

                // update myEvolutionState
                myEvolutionState.containerCpu = containerCpu;
                myEvolutionState.containerMem = containerMem;
                myEvolutionState.containerOs = containerOs;




                Integer chosenVM;
                Integer currentVmNum = vmResourceList.size();

                // calculate the current wasted CPU and Mem
//                    if(!pmActualUsageList.isEmpty()) {
//                        for (Double[] pm : pmActualUsageList) {
//                            globalCPUWaste += PMCPU - pm[0];
//                            globalMEMWaste += PMMEM - pm[1];
//                        }
//                    }

                // select or create a VM
                chosenVM = VMSelectionCreation(
                        input,
                        state,
                        vmSelectionCreationRule,
                        threadnum,
                        stack,
                        vmResourceList,
                        pmResourceList,
                        pmActualUsageList,
                        vmTypeList,
                        vmIndexTypeMapping,
                        containerCpu,
                        containerMem,
                        containerOs,
                        globalCPUWaste,
                        globalMEMWaste);

                // check if the VM exists, if chosenVM < currentVmNum is true, it means
                // the chosenVM exists, we just need to update its resources
                if (chosenVM < currentVmNum) {
                    // update the VM resources, allocating this container into this VM
                    vmResourceList.set(chosenVM, new Double[]{
                            vmResourceList.get(chosenVM)[0] - containerCpu,
                            vmResourceList.get(chosenVM)[1] - containerMem,
                            new Double(containerOs)
                    });

                    // Find the pmIndex in the mapping
                    int pmIndex = VMPMMapping.get(chosenVM);

                    // update the PM actual resources
                    pmActualUsageList.set(pmIndex, new Double[]{
                            pmActualUsageList.get(pmIndex)[0] - containerCpu,
                            pmActualUsageList.get(pmIndex)[1] - containerMem
                    });

                    // Else, we need to create this new VM
                } else {

                    // Retrieve the type of select VM
                    int vmType = chosenVM - currentVmNum;

                    // create this new VM
                    vmResourceList.add(new Double[]{
                            vmTypeList.get(vmType)[0] - containerCpu - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
                            vmTypeList.get(vmType)[1] - containerMem - vmMemOverhead,
                            new Double(containerOs)
                    });

                    // Whenever we create a new VM, map its index in the VMResourceList to its type for future purpose
                    vmIndexTypeMapping.put(vmResourceList.size() - 1, vmType);

                    // After creating a VM, we will choose a PM to allocate
                    Integer chosenPM = VMAllocation(
                                            input,
                                            myEvolutionState,
                                            vmAllocationRule,
                                            threadnum,
                                            stack,
                                            pmResourceList,
                                            pmActualUsageList,
                                            vmTypeList.get(vmType)[0],
                                            vmTypeList.get(vmType)[1],
                                  containerCpu + vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
                                  containerMem + vmMemOverhead
                                            );

                    // If we cannot choose a PM
                    if (chosenPM == null) {

                        // Add the VM to the newly created PM
                        // We don't need to consider the overhead here.
                        pmResourceList.add(new Double[]{
                                PMCPU - vmTypeList.get(vmType)[0],
                                PMMEM - vmTypeList.get(vmType)[1]
                        });

                        // Add the Actual usage to the PM
                        // Here, we must consider the overhead
                        pmActualUsageList.add(new Double[]{
                                PMCPU - containerCpu - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
                                PMMEM - containerMem - vmMemOverhead
                        });

                        // Map the VM to the PM
                        VMPMMapping.put(vmResourceList.size() - 1, pmResourceList.size() - 1);


                        // If there is an existing PM, we allocate it to an existing PM
                    } else {

                        currentPmCpuRemain = pmResourceList.get(chosenPM)[0] - vmTypeList.get(vmType)[0];
                        currentPmMemRemain = pmResourceList.get(chosenPM)[1] - vmTypeList.get(vmType)[1];

                        // update the PM resources
                        // pm resources - vm size
                        pmResourceList.set(chosenPM, new Double[]{
                                currentPmCpuRemain,
                                currentPmMemRemain
                        });

                        // update the actual resources
                        // Actual usage - container required - vm overhead
                        pmActualUsageList.set(chosenPM, new Double[]{
                                pmActualUsageList.get(chosenPM)[0] - containerCpu - vmTypeList.get(vmType)[0] * vmCpuOverheadRate,
                                pmActualUsageList.get(chosenPM)[1] - containerMem - vmMemOverhead
                        });

                        // Map the VM to the PM
                        VMPMMapping.put(vmResourceList.size() - 1, chosenPM);

                    } // End of allocating a VM to an existing PM

                } // End of creating a new VM


                // After each allocation of container, we evaluate the energy in the data-center
                // And add up the energy consumption to calculate the area under the curve
//                double increment = energyCalculation(pmActualUsageList);
//                Energy += increment;
                Energy = energyCalculation(pmActualUsageList);
//                pmFit += pmUtilRemain(pmActualUsageList, pmResourceList);
//                System.out.println(pmFit);

//            } // End of one testCase
//                ind.printIndividualForHumans(state, 0);
            // calculate the average increase of allocating a container for a taskCase,
            // We want to minimize this value
//            comparedResultList.add(Energy);
//                System.out.println("energy - sub = " + (Energy - benchmark.get(testCase)));
//            resultList.add(Energy / containers.size());
                resultList.add(Energy);
//                resultList.add(pmFit);
//                resultList.add(energyCalculation(pmActualUsageList));

        } // End of all test cases

//        Double pmFit = pmRemain(pmResourceList);
//        resultList.add(pmFit);

        return resultList;
    }


//    /**
//     * VMAllocation allocates a VM to a PM
//     * @param pmResourceList is a list of remaining resources of PM, not the acutal used resources
//     * @param vmCpuCapacity the cpu capacity of a VM
//     * @param vmMemCapacity the memory capacity of a VM
//     * @return an index of a PM
//     */
//    private Integer VMAllocation(ArrayList<Double[]> pmResourceList, double vmCpuCapacity, double vmMemCapacity){
//        Integer chosenPM = null;
////        Double BestScore = null;
//
//        // Loop through the PMs in the existing PM list
//        for(int pmCount = 0; pmCount < pmResourceList.size(); ++pmCount){
//            double pmCpuRemain = pmResourceList.get(pmCount)[0];
//            double pmMemRemain = pmResourceList.get(pmCount)[1];
//
//            // First Fit
//            if (pmCpuRemain >= vmCpuCapacity && pmMemRemain >= vmMemCapacity) {
//                chosenPM = pmCount;
//                break;
//            } // End if
//        }
//
//        return chosenPM;
//    }


    /*
     * Basically, VMAllocation still use the BestFit framework
     */
    private Integer VMAllocation(
            DoubleData input,
            final EvolutionState state,
            final Individual ind,
            final int threadnum,
            final ADFStack stack,
            ArrayList<Double[]> pmResourceList,
            ArrayList<Double[]> pmActualResourceList,
            double vmCpuCapacity,
            double vmMemCapacity,
            double vmUsedCpu,
            double vmUsedMem
            ){

        Integer chosenPM = null;
        Double BestScore = null;
        int pmCount = 0;

        // Loop through the tempResourceList
        for(Double[] pm:pmResourceList){

            // Get the remaining PM resources
            double pmCpuRemain = pm[0];
            double pmMemRemain = pm[1];

            double pmActualCpuUsed = pmActualResourceList.get(pmCount)[0];
            double pmActualMemUsed = pmActualResourceList.get(pmCount)[1];




            // If the remaining resource is enough for the container
            // And the OS is compatible
            if (pmCpuRemain >= vmCpuCapacity &&
                    pmMemRemain >= vmMemCapacity) {

                Double pmScore = EvolveVmAllocationMethod(
                        input,
                        state,
                        ind,
                        threadnum,
                        stack,
                        pmCpuRemain,
                        pmMemRemain,
                        vmCpuCapacity,
                        vmMemCapacity,
                        pmActualCpuUsed,
                        pmActualMemUsed,
                        vmUsedCpu,
                        vmUsedMem
                        );

                // Core of BestFit, score the bigger the better
                if (chosenPM == null || pmScore > BestScore) {
                    chosenPM = pmCount;
                    BestScore = pmScore;
                }
            } // End if
            // If there is no suitable PM (no PM has enough resources), then we just return null.
            pmCount ++;
        }
        return chosenPM;
    }

    private Double EvolveVmAllocationMethod(
                    final DoubleData input,
                    final EvolutionState state,
                    final Individual ind,
                    final int threadnum,
                    final ADFStack stack,
                    double pmCpuRemain,
                    double pmMemRemain,
                    double vmCpuCapacity,
                    double vmMemCapacity,
                    double pmActualCpuUsed,
                    double pmActualMemUsed,
                    double vmActualCpuUsed,
                    double vmActualMemUsed
    ){

        currentPmCpuRemain = pmCpuRemain;
        currentPmMemRemain = pmMemRemain;
        normalizedPmCpuRemain = currentPmCpuRemain / PMCPU;
        normalizedPmMemRemain = currentPmMemRemain / PMMEM;
        normalizedVmCpuCapacity = vmCpuCapacity / PMCPU;
        normalizedVmMemCapacity = vmMemCapacity / PMMEM;
        normalizedPmActualCpuUsed = pmActualCpuUsed / PMCPU;
        normalizedPmActualMemUsed = pmActualMemUsed / PMMEM;
        normalizedVmActualCpuUsed = vmActualCpuUsed / PMCPU;
        normalizedVmActualMemUsed = vmActualMemUsed / PMMEM;

        // update state in myEvolutionState
        MyEvolutionState myEvolutionState = (MyEvolutionState) state;
        myEvolutionState.currentPmCpuRemain = currentPmCpuRemain;
        myEvolutionState.currentPmMemRemain = currentPmMemRemain;
        myEvolutionState.normalizedPmCpuRemain = normalizedPmCpuRemain;
        myEvolutionState.normalizedPmMemRemain = normalizedPmMemRemain;
        myEvolutionState.normalizedVmCpuCapacity = normalizedVmCpuCapacity;
        myEvolutionState.normalizedVmMemCapacity = normalizedVmMemCapacity;
        myEvolutionState.normalizedPmActualCpuUsed = normalizedPmActualCpuUsed;
        myEvolutionState.normalizedPmActualMemUsed = normalizedPmActualMemUsed;
        myEvolutionState.normalizedVmActualCpuUsed = normalizedVmActualCpuUsed;
        myEvolutionState.normalizedPmActualMemUsed = normalizedPmActualMemUsed;

        // Evaluate the GP rule
        ((GPIndividual) ind).trees[0].child.eval(
                state, threadnum, input, stack, (GPIndividual) ind, containerAllocationProblem);
        return input.x;

    }


    private Double EvolveSelectionCreationMethod(
            DoubleData input,
            final EvolutionState state,
            final Individual ind,
            final int threadnum,
            final ADFStack stack,
            double vmCpuRemain,
            double vmMemRemain,
            double vmCpuCapacity,
            double vmMemCapacity,
            ArrayList<Double[]> pmResourceList,
            ArrayList<Double[]> actualPmResourceList){
        MyEvolutionState myEvolutionState = (MyEvolutionState)state;

        // allocated flag indicates whether the existing PM can host a newly created VM
        // true means YES, otherwise we must create new PM to host the VM
        boolean allocated = false;

//        DoubleData input = (DoubleData) (this.input);

        // The resource is normalized by the PM's capacity.
//        normalizedVmCPURemain = vmCpuRemain / PMCPU;
//        normalizedVmMemRemain = vmMemRemain / PMMEM;
        normalizedContainerCpu = containerCpu / PMCPU;
        normalizedContainerMem = containerMem / PMMEM;

        // update the data in myState
//        myEvolutionState.normalizedVmCpuCapacity = vmCpuRemain / PMCPU;
//        myEvolutionState.normalizedVmMemCapacity = vmMemRemain / PMMEM;
        myEvolutionState.normalizedVmCpuRemain = vmCpuRemain / PMCPU;
        myEvolutionState.normalizedVmMemRemain = vmMemRemain / PMMEM;
        myEvolutionState.normalizedContainerCpu = normalizedContainerCpu;
        myEvolutionState.normalizedContainerMem = normalizedContainerMem;


        // we only consider the overhead of new VM
        if(newVmFlag) {
            normalizedVmCpuOverhead = vmCpuCapacity * vmCpuOverheadRate / PMCPU;
            normalizedVmMemOverhead = vmMemOverhead / PMMEM;
        } else {
            normalizedVmCpuOverhead = 0;
            normalizedVmMemOverhead = 0;
        }

        // update the data in myState
        myEvolutionState.normalizedVmCpuOverhead = normalizedVmCpuOverhead;
        myEvolutionState.normalizedVmMemOverhead = normalizedVmMemOverhead;


        // Evaluate the GP rule
        ((GPIndividual) ind).trees[0].child.eval(
                state, threadnum, input, stack, (GPIndividual) ind, containerAllocationProblem);


        return input.x;
    }


    /**
     *
     * @return
     */
    private Integer VMSelectionCreation(
                                        final DoubleData input,
                                        final EvolutionState state,
                                        final Individual ind,
                                        final int threadnum,
                                        final ADFStack stack,
                                        ArrayList<Double[]> vmResourceList,
                                        ArrayList<Double[]> pmResourceList,
                                        ArrayList<Double[]> actualPmResourceList,
                                        ArrayList<Double[]> vmTypeList,
                                        HashMap<Integer, Integer> vmIndexTypeMapping,
                                        Double containerCpu,
                                        Double containerMem,
                                        int containerOS,
                                        double globalCpuWaste,
                                        double globalMemWaste
    ){
        Integer chosenVM = null;
        Double BestScore = null;
        int vmNum = vmResourceList.size();
        int vmCount = 0;

        // make a copy of vmResourceList
        ArrayList<Double[]> tempVMResourceList = (ArrayList<Double[]>) vmResourceList.clone();
        for(Double[] vm:vmTypeList){
            // add this new VM into the tempList
            tempVMResourceList.add(new Double[]{
                    vm[0] - vm[0] * vmCpuOverheadRate,
                    vm[1] - vmMemOverhead,
                    new Double(containerOS)
            });
        }


        // Loop through the tempResourceList
        for(Double[] vm:tempVMResourceList){

            // Check if the vm exists
            newVmFlag = vmCount >= vmNum;

            // Get the remaining VM resources and OS
            double vmCpuRemain = vm[0];
            double vmMemRemain = vm[1];
            int vmOS = vm[2].intValue();
            int vmType;
            if(vmCount < vmNum)
                vmType = vmIndexTypeMapping.get(vmCount);
            else
                vmType = vmCount - vmNum;

            // If the remaining resource is enough for the container
            // And the OS is compatible
            if (vmCpuRemain >= containerCpu &&
                    vmMemRemain >= containerMem &&
                    vmOS == containerOS) {

                Double vmScore = EvolveSelectionCreationMethod(
                        input,
                        state,
                        ind,
                        threadnum,
                        stack,
                        vmCpuRemain,
                        vmMemRemain,
                        vmTypeList.get(vmType)[0],
                        vmTypeList.get(vmType)[1],
//                        globalCpuWaste,globalMemWaste,
                        pmResourceList,
                        actualPmResourceList);

                // Core of BestFit, score the bigger the better
                if (chosenVM == null || vmScore > BestScore) {
                    chosenVM = vmCount;
                    BestScore = vmScore;
                }

            } // End if

            // Increment the VM counter
            vmCount += 1;
        }

        newVmFlag = false;
        return chosenVM;

    }

}
