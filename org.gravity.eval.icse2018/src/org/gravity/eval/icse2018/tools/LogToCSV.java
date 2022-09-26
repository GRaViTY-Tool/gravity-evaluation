package org.gravity.eval.icse2018.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LogToCSV {

	private static final File CSV = new File("csv");
	private static final File LOGS = new File("logs");

	public static void main(String[] args) {
		if(LOGS.exists()){
			for(File f : LOGS.listFiles()){
				if(f.getName().endsWith(".txt") && f.canRead()&& !f.getName().startsWith("processed_")){ 
					try {
						Result result = transform(f);
						if(result == null || !result.isValid()){
							File n = new File(LOGS, "error_"+f.getName());
							f.renameTo(n);
							f = n;
						}
						else { 
							File out = new File(CSV, result.getProject()+".csv");
							BufferedWriter writer;
							if(!out.exists()){
								out.createNewFile();
								writer = new BufferedWriter(new FileWriter(out, true));
								writer.append("modisco, tgg, createPG, detect, resolve, total, moves\n");
							}
							else{
								writer = new BufferedWriter(new FileWriter(out, true));
							}
							writer.append(result.getModisco()+", "+result.getTgg()+", "+(result.getModisco()+result.getPreprocessing()+result.getTgg())+", "+result.getDetect()+", "+result.getResolve()+", "+result.getTotal()+", "+result.getMoves()+"\n");
							writer.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					f.renameTo(new File(LOGS, "processed_"+f.getName()));
				}
			}
		}
		else{
			System.err.println("No logs folder.");
		}
	}

	private static Result transform(File f) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		
		Result result = null;
		int counter = 0;
		
		String line;
		while((line = reader.readLine())!=null){
			if(result == null){
				String key_start = "MoDisco discover project: ";
				int start = line.indexOf(key_start);
				if(start >=0 ){
					String sub = line.substring(start+key_start.length());
					int stop = sub.indexOf("_tmp");
					result = new Result(sub.substring(0, stop));
				}
			}
			else if(!result.modiscoIsSet()){
				String key_start = "MoDisco discover project - done ";
				int start = line.indexOf(key_start);
				if(start >=0 ){
					String sub = line.substring(start+key_start.length());
					int stop = sub.indexOf("ms");
					result.setModisco(Integer.parseInt(sub.substring(0, stop)));
				}
			}
			else if(!result.preprocessingIsSet()){
				String key_start = "MoDisco preprocessing - done ";
				int start = line.indexOf(key_start);
				if(start >=0 ){
					String sub = line.substring(start+key_start.length());
					int stop = sub.indexOf("ms");
					result.setPreprocessing(Integer.parseInt(sub.substring(0, stop)));
				}
			}
			else if(!result.tggIsSet()){
				String key_start = " eMoflon TGG fwd trafo - done ";
				int start = line.indexOf(key_start);
				if(start >=0 ){
					String sub = line.substring(start+key_start.length());
					int stop = sub.indexOf("ms");
					result.setTgg(Integer.parseInt(sub.substring(0, stop)));
				}
			}
			else {
				if(counter == 4 && line.contains("moved method")){
					result.incrementMoves();
				}
				if(counter < 5){
					String key_start;
					switch (counter) {
					case 0:
						key_start = " Hulk Calculate the Method accesses from Blob to DataClass - done ";
						break;
					case 1:
						key_start = " Hulk Calculate the In-Blob-Method accesses - done ";
						break;
					case 2:
						key_start = " Hulk Afferent Coupling - done ";
						break;
					case 3:
						key_start = " Hulk Efferent Coupling - done ";
						break;
					case 4:
						key_start = " Hulk Resolve Blob [Anti-Pattern] - done ";
						break;
					default:
						throw new RuntimeException();
					}
					int start = line.indexOf(key_start);
					if(start >=0 ){
						String sub = line.substring(start+key_start.length());
						int stop = sub.indexOf("ms");
						int resolve = result.getResolve();
						if(resolve < 0){
							resolve = 0;
						}
						result.setResolve(resolve+Integer.parseInt(sub.substring(0, stop)));
						counter++;
					}
				}
				else if(!result.detectIsSet()){
					String key_start = "Hulk Anti-Pattern Detection - done ";
					int start = line.indexOf(key_start);
					if(start >=0 ){
						String sub = line.substring(start+key_start.length());
						int stop = sub.indexOf("ms");
						result.setDetect(Integer.parseInt(sub.substring(0, stop))-result.getResolve());
					}
				}
				else if(!result.totalIsSet()){
					String key_start = "Hulk Anti-Pattern Detection - Done ";
					int start = line.indexOf(key_start);
					if(start >=0 ){
						String sub = line.substring(start+key_start.length());
						int stop = sub.indexOf("ms");
						result.setTotal(Integer.parseInt(sub.substring(0, stop)));
					}
				}
			}
		}
		reader.close();
		return result;
	}
	
	private static class Result {
		private String project = null;
		private int modisco = -1;
		private int preprocessing = -1;
		private int tgg = -1;
		private int detect = -1;
		private int resolve = -1;
		private int total = -1;
		private int moves = 0;
		
		Result(String project){
			this.setProject(project);
			this.setMoves(0);
		}
		
		public boolean isValid(){
			return project != null && modiscoIsSet() && preprocessingIsSet() && tggIsSet() && detectIsSet() && resolveIsSet() && totalIsSet();
		}

		public boolean detectIsSet() {
			return detect >= 0;
		}

		public boolean totalIsSet() {
			return total >= 0;
		}

		public boolean resolveIsSet() {
			return resolve >= 0;
		}

		public boolean preprocessingIsSet() {
			return preprocessing >= 0;
		}

		public boolean tggIsSet() {
			return tgg >= 0;
		}

		public boolean modiscoIsSet() {
			return modisco >= 0;
		}

		public String getProject() {
			return project;
		}

		public void setProject(String project) {
			this.project = project;
		}

		public int getModisco() {
			return modisco;
		}

		public void setModisco(int modisco) {
			this.modisco = modisco;
		}

		public int getTgg() {
			return tgg;
		}

		public void setTgg(int tgg) {
			this.tgg = tgg;
		}

		public int getPreprocessing() {
			return preprocessing;
		}

		public void setPreprocessing(int preprocessing) {
			this.preprocessing = preprocessing;
		}

		public int getDetect() {
			return detect;
		}

		public void setDetect(int detect) {
			this.detect = detect;
		}

		public int getResolve() {
			return resolve;
		}

		public void setResolve(int resolve) {
			this.resolve = resolve;
		}

		public int getTotal() {
			return total;
		}

		public void setTotal(int total) {
			this.total = total;
		}

		public int getMoves() {
			return moves;
		}

		public void setMoves(int moves) {
			this.moves = moves;
		}
		
		public void incrementMoves(){
			moves++;
		}
	}
}
