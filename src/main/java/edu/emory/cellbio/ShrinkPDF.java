package edu.emory.cellbio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;

/**
 * App for shrinking PDF files by applying jpeg compression
 * 
 * @author Benjamin Nanes, bnanes@emory.edu
 */
public final class ShrinkPDF {
    
     // -- Fields --
     
     private File input;
     private File output;
     private float compQual = -1;
     private static float compQualDefault = 0.85f;
     private boolean headless = false;
     
     // -- Methods --
     
     /**
      * Shrink using a Swing GUI to get parameters
      */
     public void runUI() {
          try {
              selectInputUI();
              selectCompQualUI();
              selectOutputUI();
          } catch(ShrinkerException e) {
              return;
          }
          try {
               final PDDocument doc = shrinkMe();
               doc.save(output);
          } catch(Exception e) {
               JOptionPane.showMessageDialog(null, e.getMessage(),
                       "Shrunken heads, there's been an error!",
                       JOptionPane.ERROR_MESSAGE);
          }
     }
     
     /**
      * Shrink from the shell environment. Unless {@link #setHeadless(boolean) headless}
      * was set to {@code true}, falls back to GUI mode if either the
      * input file or the output file are not set.
      */
     public void runShell() {
         final boolean allThere = !(input == null || output == null);
         if(headless && !allThere) {
             System.out.println("Forced (shrunken) headless, but missing parameters!");
             return;
         }
         else try {
             if(input == null)
                 selectInputUI();
             if(compQual < 0)
                 compQual = compQualDefault;
             if(output == null)
                 selectOutputUI();
         } catch(ShrinkerException e) {
             return;
         }
         try {
             final PDDocument doc = shrinkMe();
             doc.save(output);
         } catch(Exception e) {
             if(headless || allThere)
                 System.out.println(e);
             else
                 JOptionPane.showMessageDialog(null, e.getMessage(),
                    "Shrunken heads, there's been an error!",
                    JOptionPane.ERROR_MESSAGE);
         }
     }
     
     /**
      * Set the input file.
      * @throws ShrinkerException if the file does not exist or
      *             cannot be read.
      */
     public void setInput (final File f) throws ShrinkerException {
         if(f == null || !f.canRead())
             throw new ShrinkerException("Can't read input file: " + f==null ? "<null>" : f.toString());
         this.input = f;
     }
     
     /**
      * Set the output file.
      * @throws ShrinkerException if the file does not exist and cannot
      *             be created or exists and cannot be written to
      */
     public void setOutput(final File f) throws ShrinkerException {
         try {
             if(f == null || (!f.createNewFile() && !f.canWrite()))
                throw new ShrinkerException("Can't write to output file: " + f==null ? "<null>" : f.toString());
         } catch(IOException e) {
             throw new ShrinkerException(e);
         }
         this.output = f;
     }
     
     /**
      * Set the compression quality parameter.
      * @param compQual Number between 0 (low quality, small file size) 
      *                 and 1 (high quality, large file size)
      * @throws ShrinkerException if {@code compQual} is out of bounds
      */
     public void setCompQual(final float compQual) throws ShrinkerException {
         if(0 > compQual || compQual > 1)
             throw new ShrinkerException("Compression quality must be between 0 and 1");
         this.compQual = compQual;
     }
     
     /**
      * Set {@code true} to prohibit the use of dialogs.
      */
     public void setHeadless(final boolean headless) {
         this.headless = headless;
     }
     
     // -- Helper methods --
     
     /**
      * Use a Swing-based dialog to select the input file.
      * @throws ShrinkerException if the dialog is canceled
      */
     private void selectInputUI() throws ShrinkerException {
        input = null;
        while(input == null) {
            JFileChooser jfc = new JFileChooser();
            jfc.setDialogType(JFileChooser.OPEN_DIALOG);
            jfc.setDialogTitle("Select PDF to shrink...");
            if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                  throw new ShrinkerException("Canceled by user.");
            input = jfc.getSelectedFile();
            try { 
                setInput(input);
            } catch(ShrinkerException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), 
                        "Shrunken heads, there's been an error!", 
                        JOptionPane.ERROR_MESSAGE);
            }
        }
     }
     
     /**
      * Use a Swing-based dialog to select the compression
      * quality parameter.
      * @throws ShrinkerException if the dialog is canceled
      */
     private void selectCompQualUI() throws ShrinkerException {
         compQual = -1;
         while(compQual < 0 || compQual >1) {
             final String compString = 
                  JOptionPane.showInputDialog(
                  "Pick a number between 0 (lowest quality) "
                  + "and 100 (largest size):", "85");
             if(compString == null || compString.isEmpty())
                 throw new ShrinkerException("Canceled by user.");
             try {
                 compQual = Math.max(Math.min(Float.valueOf(compString), 100), 0)/100;
             } catch(NumberFormatException e) {
                 JOptionPane.showMessageDialog(null, compString 
                         + " isn't any sort of number I recognize!",
                         "Shrunken heads, there's been an error!",
                         JOptionPane.ERROR_MESSAGE);
                 return;
             }
         }
     }
     
     /**
      * Use a Swing-based dialog to select an output file.
      * @throws ShrinkerException if the dialog is canceled
      */
     private void selectOutputUI() throws ShrinkerException {
         output = null;
         while(output == null) {
            JFileChooser jfc = new JFileChooser();
            jfc.setDialogType(JFileChooser.SAVE_DIALOG);
            jfc.setDialogTitle("Select destination for shrunken PDF...");
            if(jfc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                  throw new ShrinkerException("Canceled by user.");
            output = jfc.getSelectedFile();
            try { 
                setOutput(output);
            } catch(ShrinkerException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), 
                        "Shrunken heads, there's been an error!", 
                        JOptionPane.ERROR_MESSAGE);
            }
         }
     }
     
     /**
      * Shrink a PDF
      * @param f {@code File} pointing to the PDF to shrink
      * @param compQual Compression quality parameter. 0 is
      *                 smallest file, 1 is highest quality.
      * @return The compressed {@code PDDocument}
      * @throws FileNotFoundException
      * @throws IOException 
      */
     private PDDocument shrinkMe() 
             throws FileNotFoundException, IOException {
          if(compQual < 0)
              compQual = compQualDefault;
          final FileInputStream fis = new FileInputStream(input);
          final PDFParser parser = new PDFParser(fis);
          parser.parse();
          final PDDocument doc = parser.getPDDocument();
          List pages = doc.getDocumentCatalog().getPages().getKids();
          for(Object p : pages) {
               if(!(p instanceof PDPage))
                    continue;
               PDPage page = (PDPage) p;
               Map<String, PDXObject> xObs = page.getResources().getXObjects();
               for(String k : xObs.keySet()) {
                    final PDXObject xObj = xObs.get(k);
                    if(!(xObj instanceof PDXObjectImage))
                         continue;
                    PDXObjectImage img = (PDXObjectImage) xObj;
                    final Iterator<ImageWriter> jpgWriters = 
                            ImageIO.getImageWritersByFormatName("jpeg");
                    final ImageWriter jpgWriter = jpgWriters.next();
                    final ImageWriteParam iwp = jpgWriter.getDefaultWriteParam();
                    iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    iwp.setCompressionQuality(compQual);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    jpgWriter.setOutput(ImageIO.createImageOutputStream(baos));
                    jpgWriter.write(null,
                            new IIOImage(img.getRGBImage(), null, null), iwp);
                    ByteArrayInputStream bais = 
                            new ByteArrayInputStream(baos.toByteArray());
                    PDJpeg jpg = new PDJpeg(doc, bais);
                    xObs.put(k, jpg);
               }
               page.getResources().setXObjects(xObs);
          }
          return doc;
     }
     
     // -- Main methods --

     /**
      * @param args the command line arguments: <br />
      * <code>[&lt;input&gt; [&lt;output&gt;]] [-q &lt;quality&gt;] [-h | --headless]</code>
      */
     public static void main(String[] args) {
         ShrinkPDF shrinker = new ShrinkPDF();
         if(args == null || args.length == 0) {
             shrinker.runUI();
             return;
         }
         int state = 0;
         for(String arg : args) {
             if(arg.trim().isEmpty())
                 continue;
             // 0: input; 1: output
             if(state == 0 || state == 1) {
                     if(!arg.startsWith("-")) {
                         try {
                             if(state==0)
                                shrinker.setInput(new File(arg));
                             else if(state==1)
                                 shrinker.setOutput(new File(arg));
                         } catch(ShrinkerException e) {
                             System.out.println(e.getMessage());
                             return;
                         }
                         state++;
                         continue;
                     }
                     else state = 2; //flag
             }
             // 2: flag
             if(state == 2) {
                 if(arg.equals("-q"))
                     state = 3; //compQual
                 else if(arg.equals("-h") || arg.equals("--headless"))
                     shrinker.setHeadless(true);
                 else {
                     System.out.println("Unrecognized command flag: " + arg);
                     return;
                 }
                 continue;
             }
             // 3: compQual
             if(state == 3) {
                 try {
                    shrinker.setCompQual(Float.valueOf(arg));
                 } catch(ShrinkerException e) {
                     System.out.println(e.getMessage());
                     return;
                 } catch(NumberFormatException e) {
                     System.out.println("Invalid compression quality parameter.");
                     return;
                 }
                 state = 2;
                 continue;
             }
         }
         shrinker.runShell();
     }
}
