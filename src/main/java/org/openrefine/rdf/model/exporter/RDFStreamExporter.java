package org.openrefine.rdf.model.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Properties;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.operation.ExportRDFRecordVisitor;
import org.openrefine.rdf.model.operation.ExportRDFRowVisitor;
import org.openrefine.rdf.model.operation.RDFVisitor;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.StreamExporter;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

import org.apache.commons.io.output.WriterOutputStream;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class RDFStreamExporter
 *
 *  An exporter used to transform OpenRefine project data to RDF *at scale* meaning discreet data chunks
 *  are processed and dumped to persistent storage sequentially until complete.  Therefore, only discreet
 *  memory and processing are perform no matter how large the project data.  Additionally, the memory can
 *  be optimized for a predetermined size to minimize the number of memory to persistent storage writes.
 */
public class RDFStreamExporter extends RDFExporter implements WriterExporter, StreamExporter {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFStreamExporter");

    private OutputStream outputStream = null;

    public RDFStreamExporter(RDFFormat format, String strName) {
        super(format, strName);
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        OutputStream outputStream)
            throws IOException {
        if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG: Exporting " + this.strName + " via OutputStream");
        this.outputStream = outputStream;
        this.export(theProject, options, theEngine);
    }

    public void export(Project theProject, Properties options, Engine theEngine,
                        Writer theWriter)
             throws IOException {
        if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("DEBUG: Exporting " + this.strName + " via Writer");
        this.outputStream = new WriterOutputStream(theWriter, Charset.forName("UTF-8"));
        this.export(theProject, options, theEngine);
    }

    private void export(Project theProject, Properties options, Engine theEngine)
            throws IOException {
        StreamRDF theStreamer = null;
        // TODO: Report Jena Bug:
        //      The Jena code says getWriterStream() will return null if the RDFFormat
        //      doesn't have a writer registered.  It lies!  It throws a RiotException.
        try {
            // TODO: Report Jena Bug:
            //      The following code without the end null should work but, instead,
            //      it hangs (locks up) processing.  With the null, it succeeds.
            //theWriter = StreamRDFWriter.getWriterStream(this.outputStream, this.format);
            theStreamer = StreamRDFWriter.getWriterStream(this.outputStream, this.format, null);
        }
        catch (RiotException ex) { // ...an error occurred setting the streamer...
            theStreamer = null;
        }

        RDFTransform theTransform = RDFTransform.getRDFTransform(theProject);
        try {
            if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("  Starting RDF Export...");
            theStreamer.start();

            // Process all records/rows of data for statements...
            RDFVisitor theVisitor = null;
            if ( theProject.recordModel.hasRecords() ) {
                if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("    Process by Record Visitor...");
                theVisitor = new ExportRDFRecordVisitor(theTransform, theStreamer);
            }
            else {
                if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("    Process by Row Visitor...");
                theVisitor = new ExportRDFRowVisitor(theTransform, theStreamer);
            }
            theVisitor.buildModel(theProject, theEngine);

            theStreamer.finish();
            if ( Util.isDebugMode() ) RDFStreamExporter.logger.info("  ...Ended RDF Export.");
        }
        catch (Exception ex) {
            if ( Util.isDebugMode() ) RDFStreamExporter.logger.error("DEBUG: Error exporting " + this.strName, ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
