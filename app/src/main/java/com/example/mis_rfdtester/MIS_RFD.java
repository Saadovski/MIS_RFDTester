package com.example.mis_rfdtester;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MIS_RFD {
    Module MIS_RFD_model;
    private final double Acc_threshold = 20.0;

    public MIS_RFD(Context context){
        System.out.println("Creating Algorithm class");
        try {
            MIS_RFD_model = Module.load(assetFilePath(context, "MIS_RFD_model_quantized.pt"));
        }catch(Exception e){
            Log.e("PytorchHelloWorld", "Error reading assets", e);
        }
        System.out.println("Successfully created");
    }

    private void reduce(@NonNull List<Float> data, int goal){
        if(data.size() <= goal) return;

        int idx_reduce = 0;
        boolean beginning = true;

        while(data.size() > goal){
            if(beginning) {
                Float val1 = data.remove(idx_reduce);
                Float val2 = data.remove(idx_reduce);
                Float avg = (val1 + val2) / 2;
                data.add(idx_reduce, avg);

                beginning = false;
            }else {
                Float val1 = data.remove(data.size() - idx_reduce - 1);
                Float val2 = data.remove(data.size() - idx_reduce - 1);
                Float avg = (val1 + val2) / 2;
                data.add(data.size() - idx_reduce, avg);

                ++idx_reduce;

                beginning = true;
            }
        }
    }

    private void extend(@NonNull List<Float> data, int goal){
        if(data.size() >= goal) return;

        int idx_insert = 1;
        boolean beginning = true;

        while(data.size() < goal){
            if(beginning){
                data.add(idx_insert, (data.get(idx_insert - 1) + data.get(idx_insert))/2);
                beginning = false;
            }else{
                data.add(data.size() - idx_insert, data.get(data.size() - idx_insert) + data.get(data.size() - idx_insert - 1) / 2);
                idx_insert += 2;
                beginning = true;
            }

            if(idx_insert > data.size() - 1) idx_insert = 1;
        }

    }

    private boolean unfit(List<Float> data){
        if(data.size()  > 360 || data.size() < 320){
            return true;
        }
        return false;
    }

    public String getDetectedAction(List<Float> AccX, List<Float> AccY, List<Float> AccZ, List<Float> RotX, List<Float> RotY, List<Float> RotZ){
        System.out.println("Analyzing data");

        if(unfit(AccX) || unfit(AccY) ||unfit(AccZ) ||unfit(RotX) ||unfit(RotY) ||unfit(RotZ)){
            return "Invalid Data";
        }
        System.out.println("Data is fit");
        System.out.println("Cleaning data");


        extend(AccX, 350);
        extend(AccY, 350);
        extend(AccZ, 350);
        extend(RotX, 350);
        extend(RotY, 350);
        extend(RotZ, 350);

        reduce(AccX, 350);
        reduce(AccY, 350);
        reduce(AccZ, 350);
        reduce(RotX, 350);
        reduce(RotY, 350);
        reduce(RotZ, 350);

        try {
            System.out.println("Applying MIS_RFD Algorithm");
            return model_based_algorithm(AccX, AccY, AccZ, RotX, RotY, RotZ);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "error";
    }

    //simple algorithm that only detects high accelerations
    private String simple_threshold_algorithm(List<Float> AccX, List<Float> AccY, List<Float> AccZ){
        System.out.println(AccX.size() + " " + AccY.size() + " " + AccZ.size());
        for(int i = 0; i < AccX.size(); ++i){
            if(AccX.get(i)*AccX.get(i) + AccY.get(i)*AccY.get(i) + AccZ.get(i)*AccZ.get(i) > Acc_threshold*Acc_threshold)
                return "Fall";
        }
        return "Not Fall";
    }

    private String model_based_algorithm(List<Float> AccX, List<Float> AccY, List<Float> AccZ, List<Float> RotX, List<Float> RotY, List<Float> RotZ) throws IOException {
        System.out.println("Preparing data for model");

        float[][] inputArray = new float[][]{ListToArray(AccX), ListToArray(AccY), ListToArray(AccZ), ListToArray(RotX), ListToArray(RotY), ListToArray(RotZ)};
        float[] concat1 = concat(ListToArray(AccX), ListToArray(AccY));
        float[] concat2 = concat(ListToArray(AccZ), ListToArray(RotX));
        float[] concat3 = concat(ListToArray(RotY), ListToArray(RotZ));
        float[] concat4 = concat(concat1, concat2);
        float[] concat5 = concat(concat4, concat3);

        Tensor inputTensor = Tensor.fromBlob(concat5, new long[]{1, 6, 350});
        System.out.println("Input shape: " + inputTensor.shape());

        Tensor outputTensor = MIS_RFD_model.forward(IValue.from(inputTensor)).toTensor();
        System.out.println("Output shape: " + outputTensor.shape());

        float[] scores = outputTensor.getDataAsFloatArray();
        System.out.println("Scores obtained: " + scores);

        int prediction = getMaxScoreIdx(scores);

        if(prediction == 0)
            return "Fall";
        else if(prediction == 1)
            return "Fall like body motion";
        else if(prediction == 2)
            return "Fall like phone motion";
        else
            return "Normal activity";
    }

    private static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private float[] ListToArray(List<Float> floatList){
        float[] floatArray = new float[floatList.size()];
        for(int i = 0; i< floatList.size(); ++i){
            floatArray[i] = floatList.get(i);
        }
        return floatArray;
    }

    private int getMaxScoreIdx(float[] scores){
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }

        return  maxScoreIdx;
    }

    private float[] concat(float[] array1, float[] array2) {
        int aLen = array1.length;
        int bLen = array2.length;
        float[] result = new float[aLen + bLen];

        System.arraycopy(array1, 0, result, 0, aLen);
        System.arraycopy(array2, 0, result, aLen, bLen);

        return result;
    }
}
