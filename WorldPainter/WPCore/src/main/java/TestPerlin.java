import org.pepsoft.util.PerlinNoise;

import java.io.IOException;
import java.util.Random;

public class TestPerlin {
    public static void main(String[] args)  {
        Random random = new Random();
        PerlinNoise perlinNoise = new PerlinNoise(random.nextLong());

        long startTime=System.nanoTime();
        for (int i=0; i<33554432; i++){
            double output=perlinNoise.getPerlinNoise(random.nextInt()/16.411,random.nextInt()/16.411,random.nextInt()/16.411);
        }
        long endTime=System.nanoTime();
        long totalTime = endTime-startTime;
        System.out.println(totalTime);



    }
}
