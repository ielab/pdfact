package pdfact.cli;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import me.tongfei.progressbar.*;

public class PdfActCliBatch {

    public static void main(String[] args) {
//        String dir = "/Users/koo01a/ir.collections/agask-sources/reports";

        String dir = args[0];

        PdfActCli pdfActCli = new PdfActCli();

        String[] pdfFilesToProcess = new File(dir).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".pdf") &&
                        !(new File(dir, name +".json").exists());
            }
        });
        ProgressBar pb = new ProgressBar("PDF to Json", pdfFilesToProcess.length);

        System.out.println("Beginning process " + dir + " with " + pdfFilesToProcess.length + " files.");

        List<String> errorsList=new ArrayList<String>();
        for (String pdfFile : pdfFilesToProcess) {
            try {
                pdfActCli.start(new String[]{dir + "/" + pdfFile, "--format", "json", dir + "/" + pdfFile + ".json"});
            } catch (IllegalArgumentException illegalArgumentException) {
                errorsList.add(pdfFile);
            }
            pb.step();

        }

        if (errorsList.size() > 0) {
            System.out.println(errorsList.size() + " error found on the following documents");
            for(String pdf : errorsList) {
                System.out.println(pdf);
            }
        } else {
            System.out.println("Completed");
        }

    }
}
