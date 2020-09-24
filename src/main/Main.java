package main;

import ec.Evolve;

public class Main {
    public static void main(String[] args){
        String pathToFiles = "./";
        int numberOfRuns = 1;
        String param = args[0];
        String seed = args[1];
        System.out.println(param);
        System.out.println(seed);

//        String param = "/home/tanboxi/IdeaProjects/MaxTan/VMSelectionCreationGP/out/artifacts/VMSelectionCreationGP_jar/src/params/fake.params";

        // Set up the parameters. Include the parameter settings, output file names,
        // and output form
        String[] runConfig = new String[]{
                Evolve.A_FILE, param,
                "-p", ("stat.file=$" + pathToFiles + "out.stat"),
                "-p", ("jobs=" + numberOfRuns),
                "-p", ("seed.0=" + seed),
        };
        Evolve.main(runConfig);
    }
}
