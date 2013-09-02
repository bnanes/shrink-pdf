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
 * @author Benjamin Nanes
 */
public final class ShrinkPDF {
     
     // -- Methods --
     
     /**
      * Shrink using a Swing GUI to get parameters
      */
     public void runUI() {
          JFileChooser jfc = new JFileChooser();
          jfc.setDialogType(JFileChooser.OPEN_DIALOG);
          jfc.setDialogTitle("Select PDF to shrink...");
          if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
               return;
          final String compString = 
                  JOptionPane.showInputDialog(
                  "Pick a number between 0 (lowest quality) "
                  + "and 100 (largest size):", "85");
          final float comp;
          try {
               comp = Math.max(Math.min(Float.valueOf(compString), 100), 0)/100;
          } catch(NumberFormatException e) {
               JOptionPane.showMessageDialog(null, compString 
                       + " isn't any sort of number I recognize!",
                       "Shrunken heads, there's been an error!",
                       JOptionPane.ERROR_MESSAGE);
               return;
          }
          try {
               final PDDocument doc = shrinkMe(jfc.getSelectedFile(), comp);
               jfc.setDialogTitle("Select destination for shrunken PDF...");
               jfc.setDialogType(JFileChooser.SAVE_DIALOG);
               if(jfc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                    return;
               doc.save(jfc.getSelectedFile());
          } catch(Exception e) {
               JOptionPane.showMessageDialog(null, e,
                       "Shrunken heads, there's been an error!",
                       JOptionPane.ERROR_MESSAGE);
          }
          
     }
     
     // -- Helper methods --
     
     /**
      * Shrink a PDF
      * @param f {@code File} pointing to the PDF to shrink
      * @param compQual Compression quality parameter. 0 is
      *                 smallest file, 1 is highest quality.
      * @return The compressed {@code PDDocument}
      * @throws FileNotFoundException
      * @throws IOException 
      */
     private PDDocument shrinkMe(final File f, float compQual) 
             throws FileNotFoundException, IOException {
          final FileInputStream fis = new FileInputStream(f);
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
      * @param args the command line arguments
      */
     public static void main(String[] args) {
          ShrinkPDF shrinker = new ShrinkPDF();
          shrinker.runUI();
     }
}
