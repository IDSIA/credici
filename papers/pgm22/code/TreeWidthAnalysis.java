package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class TreeWidthAnalysis {
    public static void main(String[] args) throws IOException {


        Path prj_dir  = Path.of("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici");
        Path modelsPath = Path.of("papers/pgm22/models/synthetic/1000/set4/");


        String files[] = Files.list(prj_dir.resolve(modelsPath))
                .map(f->f.toString())
                .filter(f->f.endsWith(".uai"))
                .collect(Collectors.toList()).toArray(String[]::new);

        String[][] info = new String[files.length+1][];


        info[0] = new String[]{"name", "treewidth", "endo_treewidth"};
        for(int i = 0; i< files.length; i++){


            String fullpath = files[i];
            StructuralCausalModel model = (StructuralCausalModel) IO.read(fullpath);

            if(model == null)
                System.out.println();


            String name = fullpath.substring(fullpath.lastIndexOf("/")+1, fullpath.length());

            String tw = "", exotw = "";

            try {
                tw = String.valueOf(model.getTreewidth());
            }catch (Exception e){

            }

            try {
                exotw = String.valueOf(model.getEndogenousTreewidth());
            }catch (Exception e){

            }
            info[i+1] = new String[]{name, tw, exotw};

            System.out.println(tw+", "+exotw+" : "+name);

        }


        String fullpath = prj_dir.resolve(modelsPath).toString()+"/treewidth.csv";

        System.out.println("Saving info to "+fullpath);
        DataUtil.toCSV(fullpath, info);


    }
}
