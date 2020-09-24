package main;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ReadConfigures {

    /**
     * Read one test case from
     * @param filePath
     * @return an arrayList of double[]
     */
    public ArrayList<Double[]> readTestCase(String filePath){

        ArrayList<Double[]> data = new ArrayList<Double[]>();
        try {
            Reader reader = Files.newBufferedReader(Paths.get(filePath));
            CSVReader csvReader = new CSVReader(reader);
            String[] nextRecord;

            // keep reading from the file
            while((nextRecord = csvReader.readNext()) != null){
                 Double[] record = new Double[nextRecord.length];
                 for(int i = 0; i < nextRecord.length; ++i){
                    record[i] = Double.parseDouble(nextRecord[i]);
                }
                data.add(record);
            }
            reader.close();
            csvReader.close();
        } catch(IOException e1){
            e1.printStackTrace();
        }


        return data;
    }

    /**
     * Read test case from
     * @param folderPath the folder
     * @param item  String value to differentiate from pm/os/vm/container datasets
     * @param start the number of test cases. Start from
     * @param end the number of test cases End in
     * @return
     */
    public ArrayList<ArrayList> testCases(String folderPath, String item, int start, int end){
        // store the data in arrayList
        ArrayList<ArrayList> testCase = new ArrayList<>();

        // start to read files from testCaseNum[0]
        for(int i = 0; i + start < end; ++i){
            // each testCase will be store in an arrayList
            ArrayList<Double[]> data;


            String filePath = folderPath + "testCase" + (i + start) + "/" + item + ".csv";
            // read one test case
            data = readTestCase(filePath);


            testCase.add(data);
        }
        return testCase;
    }
}
