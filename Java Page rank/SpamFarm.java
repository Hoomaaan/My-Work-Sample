import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SpamFarm {
	private int target;
	private String graphFile;

	
	public SpamFarm(String graphFile, int target) {
		this.graphFile = graphFile;
		this.target = target;
	}
	
	public void CreateSpam (String fileName) {
		
		try {
						
			File output = new File(fileName);
			FileReader input = new FileReader(graphFile);
			BufferedReader br = new BufferedReader(input);
			String line = br.readLine();
			int n = Integer.parseInt(line);
			
			PrintWriter printer = new PrintWriter(output);
			int N = n + (int) Math.floor(n / 10);
			
			printer.write(N + "\n"); 
			
			while ((line = br.readLine()) != null) {
				printer.write(line + "\n");
			}
			
			for (int i = 0; i < Math.floor(n/10); i++) {
				int m = n + i + 1;
				String vertex = Integer.toString(m);
				String spam = target +  " " + vertex;
				printer.write(spam + "\n");
				String spam2 = vertex + " " + target;
				printer.write(spam2 + "\n");
			}
			
			printer.close();
			br.close();
			input.close();
			
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		}
		
	}
}
