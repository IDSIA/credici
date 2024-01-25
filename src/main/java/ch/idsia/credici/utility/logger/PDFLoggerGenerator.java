package ch.idsia.credici.utility.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ch.idsia.credici.model.StructuralCausalModel;

public class PDFLoggerGenerator implements Function<String, BiConsumer<String, Info>> {
	
	private static class PDFLogger implements BiConsumer<String, Info> {
		private String name;
		private List<String> files;
		private int number;
		
		private Path temp;
		private File target; 
		
		public PDFLogger(File target, String name) throws IOException {
			this.target = target;
			this.name = name;
			this.number = 0;
			this.files = new ArrayList<>();
			temp = Files.createTempDirectory(name);
		}

		
		@Override
		public void accept(String t, Info u) {
			File f = new File(temp.toFile(), "m" + (number++) + ".png");
			DetailedDotSerializer.saveModel(f.toString(), u);
			files.add(f.toString());
			
			savePDF();
		}
		
		private void savePDF() {
			try {
				File f = new File(target, name + ".pdf");
				
				List<String> commands = new ArrayList<>();
				commands.add("/opt/homebrew/bin/convert");
				commands.addAll(files);
				commands.add(f.toString());
				
				ProcessBuilder b = new ProcessBuilder(commands);
				Process p = b.start();
				p.waitFor();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	private String path;

	public PDFLoggerGenerator(String outputPath) {
		this.path = outputPath;
	}
	
	@Override
	public BiConsumer<String, Info> apply(String t) {		
		try {
			return new PDFLogger(new File(path), t);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
