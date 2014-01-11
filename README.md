The PDF Shrinker
================

Reduce PDF file size by applying jpeg compression to all images embedded in the PDF.

Project site: <http://cellbio.emory.edu/bnanes/shrink-pdf.html>

Release notes
-------------

### beta-2

- Support for command line arguments

### beta-1

- Extracts each image object in the PDF, applies jpeg compression, and replaces the original image

- Does not check original image format. If original image format is already compressed (jpeg, jbig2, or jpeg2000), it will be recompressed as jpeg. This will always result in quality loss, but depending on the compression settings, it may *increase* file size.

Dependencies
------------

Uses [Apache PDFBox](http://pdfbox.apache.org/) to read, parse, and write PDF files (included in the pre-built app download)

Contact
-------

**Benjamin Nanes**    
<http://cellbio.emory.edu/bnanes/>
