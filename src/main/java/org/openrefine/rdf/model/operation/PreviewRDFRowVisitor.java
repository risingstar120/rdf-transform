package org.openrefine.rdf.model.operation;

import java.util.List;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.ResourceNode;
import org.openrefine.rdf.model.Util;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.apache.jena.iri.IRI;
import org.apache.jena.riot.system.StreamRDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewRDFRowVisitor extends RDFRowVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:PrevRDFRowV");

    private int iLimit = Util.getSampleLimit();
    private int iCount = 0;

    public PreviewRDFRowVisitor(RDFTransform theTransform, StreamRDF theWriter) {
        super(theTransform, theWriter);
        this.iLimit = Util.getSampleLimit();
        if ( Util.isDebugMode() ) PreviewRDFRowVisitor.logger.info("DEBUG: Created...");
    }

    public boolean visit(Project theProject, int iRowIndex, Row theRow) {
        // Test for end of sample...
        if ( this.iLimit > 0 && this.iCount >= this.iLimit ) {
            return true; // ...stop visitation process
        }
        try {
            if ( Util.isDebugMode() ) PreviewRDFRowVisitor.logger.info("DEBUG: Visiting Row: " + iRowIndex + " on count: " +  this.iCount);
            IRI baseIRI = this.getRDFTransform().getBaseIRI();
            List<ResourceNode> listRoots = this.getRDFTransform().getRoots();
            for ( ResourceNode root : listRoots ) {
                root.createStatements(baseIRI, this.theModel, theProject, iRowIndex );

                if ( Util.isDebugMode() ) {
                    PreviewRDFRowVisitor.logger.info("DEBUG:   " +
                        "Root: " + root.getNodeName() + "(" + root.getNodeType() + ")  " +
                        "Model Size: " + this.theModel.size()
                    );
                }
            }
            this.iCount += 1;

            // Flush all statements...
            this.flushStatements();
        }
        catch (Exception ex) {
            PreviewRDFRowVisitor.logger.error("ERROR: Visit Issue: " + ex.getMessage(), ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
            return true; // ...stop visitation process
        }

        return false;
    }
}
