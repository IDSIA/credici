package ch.idsia.credici.utility.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Logger that saves images of networks in a multipave PDF file.
 * In EQMC a logger is a function that creates consumers of info suppliers. 
 * The function will be called for each component. The generated consumer will accept 
 * info suppliers. We adopted this stragedy as this will allow the implementation to perform 
 * complex logging preprocessing triggered only by concrete consumers and not by the default null
 * consumer.
 */
public class PDFLoggerGenerator implements Function<Integer, Consumer<Supplier<Info>>> {
	
	private static class PDFLogger implements Consumer<Supplier<Info>> {
		private String name;
		private List<String> files;
		private int number;
		
		private Path temp;
		private File target; 
		
		public PDFLogger(File target, String base, Integer component) throws IOException {
			this.target = target;
			this.name = base + " " + (component == null ? "EQMC" : "C" + component);
			this.number = 0;
			this.files = new ArrayList<>();
			temp = Files.createTempDirectory(name);
		}

		
		@Override
		public void accept(Supplier<Info> ugen) {
			// generate the info
			Info u = ugen.get();
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
	private String basename;

	public PDFLoggerGenerator(String outputPath, String basename) {
		this.path = outputPath;
		this.basename = basename;
	}
	
	@Override
	public Consumer<Supplier<Info>> apply(Integer t) {		
		try {
			return new PDFLogger(new File(path), basename, t);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
