package gov.nasa.jpf.contcheck;

import gov.nasa.jpf.*;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.vm.VM;

public class ContChkListener extends PropertyListenerAdapter implements PublisherExtension {
    public ContChkListener(Config conf, JPF jpf) {
        //jpf.addPublisherExtension(ConsolePublisher.class,this);
    }
    public void instructionExecuted(VM vm) {} // VM has executed next instruction
    public void threadStarted(VM vm) {}   // new Thread entered run() method
    public void threadTerminated(VM vm) {}   // Thread exited run() method
    
    // detect when a container class is loaded here
    // and initialize a data structure to contain the
    // usage operations on it
    public void classLoaded(VM vm) {}      // new class was loaded
    
    // search the active container class structs for this object
    public void objectCreated(VM vm) {}   // new object was created
    
    // close or otherwise finalize the container class struct
    // when it is released
    public void objectReleased(VM vm){}  // object was garbage collected
    // detect when a container class is GC'd 
    public void gcBegin(VM vm){}  // garbage collection mark phase started
    // may need to detect what was GC'd if not reported otherwise 
    public void gcEnd(VM vm){}   // garbage collection sweep phase terminated
    // detect if a violation of a container access/usage was performed
    public void  exceptionThrown(VM vm){}   // exception was thrown
    
    // random choice has been presented, we may not care?
    public void nextChoice(VM vm){}   // choice generator returned new value
}
