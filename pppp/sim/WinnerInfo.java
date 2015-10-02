package pppp.sim;

public class WinnerInfo {
	int winnerGroupId;
	int winnerScore;
	public WinnerInfo(int winnerGroupId, int winnerScore) {
		super();
		this.winnerGroupId = winnerGroupId;
		this.winnerScore = winnerScore;
	}
	
	/** find the Winners group Id and its Winning Score.
	 *  Needed for writing the score sheet
	 * 
	 */
	public static WinnerInfo getMaxValueAndIndex(int[] scoresOf9) {
		int maxValue = Integer.MIN_VALUE;
		int maxIndex = -1;
		for(int i = 0; i < scoresOf9.length; i++) {
		      if(scoresOf9[i] > maxValue) {
		    	  maxValue = scoresOf9[i];
		    	  maxIndex = i ;
		      }
		}
		return new WinnerInfo(maxIndex,maxValue);
	}
 

}
