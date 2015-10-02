package pppp.sim;

import java.util.Random;

import pppp.g4.Utils;

 
	class ShuffleTest
	{
	  public static void main(String args[])
	  {
	    int[] solutionArray = { 1, 2, 3,  5, 6, 7, 8, 9, 1};

	    Utils.shuffleArray(solutionArray);
	    for (int i = 0; i < solutionArray.length; i++)
	    {
	      System.out.print(solutionArray[i] + " ");
	    }
	    System.out.println();
	  }
 
	}
 