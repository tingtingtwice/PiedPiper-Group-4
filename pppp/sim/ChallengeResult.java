package pppp.sim;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChallengeResult {
static final String NEW_LINE_SEPARATOR="\n";
	private Date startTime;
	private String description;
	private String northGroupId;
	private String eastGroupId;
	private String southGroupId;
	private String westGroupId;
	private long timeTakenSeconds;
	private String winnerGroupId;
	private int winnerScore;
	private int pipers;
	private int rats;
	private int side;
	private int[] scores;
	public ChallengeResult(Date startTime, String description,
			String northGroupId, String eastGroupId, String southGroupId,
			String westGroupId, long timeTakenSeconds, String winnerGroupId,int winnerScore,
			int pipers, int rats, int side, int[] scores) {
		super();
		this.startTime = startTime;
		this.description = description;
		this.northGroupId = northGroupId;
		this.eastGroupId = eastGroupId;
		this.southGroupId = southGroupId;
		this.westGroupId = westGroupId;
		this.timeTakenSeconds = timeTakenSeconds;
		this.winnerGroupId = winnerGroupId;
		this.winnerScore=winnerScore;
		this.pipers = pipers;
		this.rats = rats;
		this.side = side;
		this.scores = scores;
	}
	
	public static void writeHeaderToExcel(String fileName){

		SimpleDateFormat dt = new SimpleDateFormat("yyyy-mm-dd_hh:mm:ss"); 
		String line="StartTime|Description|PipersCount|ratsCount|side|North|East|south|west|winnerGroupId|winnerScore|timeTaken|" +
				"G1_Score|G2_Score|G3_Score|G4_Score|G5_Score|G6_Score|G7_Score|G8_Score|G9_Score|" ;
		FileWriter fileWriter = null;
		
		try {
			fileWriter = new FileWriter(fileName,true);

			
			//Add a new line separator after the header
			//Write a new student object list to the CSV file
			fileWriter.append(line);
			fileWriter.append(NEW_LINE_SEPARATOR);
			
		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {
			
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
			}
			
		}		
				
	}

	public void writeToExcel(String fileName){

		SimpleDateFormat dt = new SimpleDateFormat("yyyy-mm-dd_hh:mm:ss"); 
		String line=dt.format(startTime)  + "|"+description+ "|" +
				pipers + "|" +
				rats + "|" +
				side + "|" +
				northGroupId+" (" + scores[Integer.parseInt(northGroupId.replace("g", "")) ] +") |" + //northGroupId.replac
				eastGroupId+" (" + scores[Integer.parseInt(eastGroupId.replace("g", "") )] +") |" +
				southGroupId+" (" + scores[Integer.parseInt(southGroupId.replace("g", "")) ] +") |" +
				westGroupId+" (" + scores[Integer.parseInt(westGroupId.replace("g", "")) ] +") |" +
				winnerGroupId+" (" + winnerScore + ")|" +
				timeTakenSeconds+ "|" +
				scores[1] + "|" +
				scores[2] + "|" +
				scores[3] + "|" +
				scores[4] + "|" +
				scores[5] + "|" +
				scores[6] + "|" +
				scores[7] + "|" +
				scores[8] + "|" +
				scores[9] + "|" ;
		FileWriter fileWriter = null;
		
		try {
			fileWriter = new FileWriter(fileName,true);

			
			//Add a new line separator after the header
			//Write a new student object list to the CSV file
			fileWriter.append(line);
			fileWriter.append(NEW_LINE_SEPARATOR);
			
		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {
			
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
			}
			
		}		
				
	}
}
