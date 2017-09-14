package gov.nasa.jpf.contcheck;

import gov.nasa.jpf.*;
import gov.nasa.jpf.report.PublisherExtension;

public class ContChkListener extends PropertyListenerAdapter implements PublisherExtension {
    public ContChkListener(Config conf, JPF jpf) {
        //jpf.addPublisherExtension(ConsolePublisher.class,this);
    }
}
