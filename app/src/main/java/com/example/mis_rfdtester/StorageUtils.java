package com.example.mis_rfdtester;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class StorageUtils {

    public static void createFile(File file){
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addFileHeader(File file, String[] header){
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            // adding header to csv
            writer.writeNext(header);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void writeCommentData(File dataFile, Timestamp ts, String comment, boolean true_positive)
    {

        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(dataFile, true);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            String correctness = true_positive? "TP":"FP";

            // add data to csv
            String[] commentData = {ts.toString(), comment, correctness};
            writer.writeNext(commentData);

            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void writeMotionData(File dataFile, Timestamp ts, ArrayList<Float> AccX, ArrayList<Float> AccY, ArrayList<Float> AccZ, ArrayList<Float> RotX, ArrayList<Float> RotY, ArrayList<Float> RotZ)
    {

        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(dataFile, true);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            // add data to csv
            for(int i = 0; i < AccX.size(); ++i) {
                String[] motionData = {ts.toString(), AccX.get(i).toString(), AccY.get(i).toString(), AccZ.get(i).toString(), RotX.get(i).toString(), RotY.get(i).toString(), RotZ.get(i).toString()};
                writer.writeNext(motionData);
            }
            String[] space = {"", "", "", "", "", "", ""};
            writer.writeNext(space);
            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
