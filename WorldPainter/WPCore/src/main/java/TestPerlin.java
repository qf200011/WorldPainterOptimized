import org.pepsoft.util.PerlinNoise;

import java.io.IOException;
import java.util.Random;

public class TestPerlin {
    public static void main(String[] args)  {
        Random random = new Random();
        PerlinNoise perlinNoise = new PerlinNoise(123456789L);

        double test=perlinNoise.getPerlinNoise(-44.4010735,-122.956818,15.6135645);
        System.out.println(test);

        /*long startTime=System.nanoTime();
        for (int i=0; i<33554432; i++){
            double output=perlinNoise.getPerlinNoise(random.nextInt()/16.411,random.nextInt()/16.411,random.nextInt()/16.411);
        }
        long endTime=System.nanoTime();
        long totalTime = endTime-startTime;
        System.out.println(totalTime);*/



    }
}
