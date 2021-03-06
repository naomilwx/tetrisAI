import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

//Create an new object for each value, follows instrcution  in main

public class Statistics 
{
    ArrayList<Double> data;   
    String game;
    int count;
    int max;
    int min;

    public Statistics(String game) 
    {
    	this.game = game;
    	count = 0;
    	data = new ArrayList<Double>();
    }   
    
    public void addResult(int result) {
    	data.add(result * 1.0);
    	count++;
    }
    
    public void output() throws IOException {
    	getMinAndMax();
		System.out.print("Statistic of " + game +" for " + count + " runs;\n");
		System.out.print("Mean: " + getMean() +"\n");
        System.out.println("Max: " + max);
        System.out.println("Min: " + min);
		System.out.print("Variance: " + getVariance()  +"\n");
		System.out.print("StandardDev: " + getStdDev()  +"\n");
		System.out.print("Median: " + median()  +"\n");
    }
    private double getMean()
    {
        double sum = 0.0;
        for(double a : data)
            sum += a;
            return sum/data.size();
    }
    private void getMinAndMax(){
        max = (int)Integer.MAX_VALUE;
        min = -1;
        for(double a :data){
            if (a > max){
                max = (int)a;
            }
            if (a < min){
                min = (int)a;
            }
        }
    }
    private double getVariance()
        {
            double mean = getMean();
            double temp = 0;
            for(double a :data)
                temp += (mean-a)*(mean-a);
                return temp/data.size();
        }

    private double getStdDev()
        {
            return Math.sqrt(getVariance());
        }

    private double median() 
        {
    			ArrayList<Double> b= new ArrayList<Double>(data);
    			Collections.sort(b);
               if (b.size() % 2 == 0) 
               {
                  return (b.get((b.size() / 2) - 1) + b.get(b.size() / 2)) / 2.0;
               } 
               else 
               {
                  return b.get(b.size() / 2);
               }
        }
    
    //example
    
    
    public static void main(String[] args) throws IOException {
    		Statistics stats = new Statistics("game");
    		for(int i = 0; i < 1000; i++) { // play a game with a certain parameter for 1000times
        		State s = new State();
        		//new TFrame(s);
                double[] arr = {-15.102872968497385, -6.01796988891534, -4.826617711953205, -4.185635183663881, -3.6184477329302336, -10.49441256846234};
                PlayerSkeleton p = new PlayerSkeleton(arr);
                while(!s.hasLost()) {
                    s.makeMove(p.pickMove(s,s.legalMoves()));
                }
    			int var = s.getRowsCleared();
                
    			stats.addResult(var);
    			
    		}
    		stats.output();
    }
}