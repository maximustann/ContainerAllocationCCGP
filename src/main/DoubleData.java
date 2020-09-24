package main;
import ec.gp.GPData;


public class DoubleData extends GPData {
    public Double x;

    public void copyTo(final GPData gpd){
        ((DoubleData)gpd).x = x;
    }

}
