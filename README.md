# Psef
## Portable scholarly edition format (psef) archiving service
This readme describes the *final* functionality of the Psef service. As yet only a few parts are written, but functionality will be gradually added to in the near future. This is a key technology in Ecdosis and will be supported. Psef is written in the form of a Java web application, for inclusion in Tomcat. But it can also be run as a standalone web service using Jetty.

## Rationale
Digital scholarly editions are notiously non-interoperable. They usually consist of source documents in XML, XSLT scripts, CSS stylesheets, javascript and typically run within a content management system. Any software written for them is usually customised from scratch for each project. Functionality is split between the client and server halves of the edition, making archiving impossible. Psef tries to change all that by creating a portable, interoperable and archivable format that preserves the entire website as an archive that can be viewed, and is fully functional in any modern web-browser.

### XML-free
Psef is XML-free, because XML creates non-interoperability problems due to the idiosyncratic and interpretative use of markup for historical documents. All source documents are represented instead in plain text with separate external JSON markup. Documents with more than one version are merged into multi-version documents. Javascript software to read MVDs, and to format the standoff markup and text as HTML is provided within the archive so this causes no interoperability or archiving issues. A Psef archive is just a .tar.gz compressed file of a project stored in a single top-level directory. That directory stores only static resources such as images, HTML, CSS, JSON, MVDs and javascript. 

These resources have local paths that are rewritten from their original positions on the website or in the database. So a resource like http://mysite.com/mycms/node/4 would become /static/node/4 at the top level of the psef-archive. An ecdosis database resource such as /cortex/english/harpur/h642 would be stored in the same path within the Psef archive. Images go in /corpix, CSS in /corform, and markup in /corcode.

The javascript is modified so that all calls to jQuery.get are replaced by a plugin-function jQuery.fetch, which reads the resource from its modified path within the archive, instead of from the server. To restore the original javascript just replace .fetch with .get.

All functionality provided by the original server code is replaced by equivalent functionality in Javascript. So the services only provide get and set functions. Since a Psef archive is read-only, no setting is required. This is only possible because Psef archives use Ecdosis services exclusively. The services are written to enable this kind of archiving of the GUI functionality. As a result Psef *will not work* with arbitrary websites.

### Reading Psef archives
External resources are also saved within the archive and "frozen" in the state they were in when downloaded. Hence to view a Psef archive all that one needs is to decompress it, then open the index.html file at the root (corresponding to the home directory contents) in a modern web-browser. All links work because they have been replaced with equivalent links to files within the archive.

### Interoperability
Psef archives only assume a HTML5 compatible browser. No other software is needed.

### Archiving
So long as browsers continue to support HTML (1992), Javascript (1995), JSON (2002) and CSS (1994), which they have done historically for a long time by supporting backwards-compatibility, Psef archives will continue to be readable. The fact is, digital scholary editions are not just the source files: they include the GUI functioanlity, the CMS web-pages, the stylesheets etc. It's everything, and to preserve just the non-interoperable sources doesn't save anything of interest. A future user would likely recode the source documents and rewrite the program code from scratch. But with Psef the edition can live on in the form as originally designed.

### Sharing
Psef archives can be stored on a USB stick, or transferred through online services like dropbox etc. The functionality is all in the Javascript and can be reused or downloaded afresh from the Ecdosis website.
