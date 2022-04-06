package com.example.mis_rfdtester;


import androidx.annotation.NonNull;

import java.util.List;

public class MIS_RFD {
    private final double Acc_threshold = 20.0;

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
        if(unfit(AccX) || unfit(AccY) ||unfit(AccZ) ||unfit(RotX) ||unfit(RotY) ||unfit(RotZ)){
            return "Invalid Data";
        }
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

        System.out.println(AccX.size() + " " + AccY.size() + " " + AccZ.size() + " " + RotX.size() + " " + RotY.size() + " " + RotZ.size());
        for(int i = 0; i < AccX.size(); ++i){
            if(AccX.get(i)*AccX.get(i) + AccY.get(i)*AccY.get(i) + AccZ.get(i)*AccZ.get(i) > Acc_threshold*Acc_threshold)
                return "Fall";
        }
        return "Not Fall";
    }
}
