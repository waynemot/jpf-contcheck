package gov.nasa.jpf.contcheck;

import java.util.Iterator;

import gov.nasa.jpf.*;
import gov.nasa.jpf.jvm.bytecode.JVMFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.NEW;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StateSet;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

public class ContChkListener extends PropertyListenerAdapter implements PublisherExtension {
    
	public ContChkListener() {	
    }
	
	public ContChkListener(Config conf, JPF jpf) {
        //jpf.addPublisherExtension(ConsolePublisher.class,this);
    	System.out.println("constuctor of contcheck");
    }
    @SuppressWarnings("deprecation")
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
    	if (!vm.getSystemState().isIgnored()) {
    		System.out.println("instructionExecuted fired");
			Instruction insn = executedInstruction;
			System.out.println("instruction: "+insn.toString());
			System.out.println("source line: "+insn.getSourceLine());
			System.out.println("line #: "+insn.getLineNumber());
			System.out.println("method name: "+insn.getMethodInfo().toString());
			if(insn.hasAttr()) {
				Iterator it = insn.attrIterator();
				while(it.hasNext()) {
					Object o = it.next();
					String str = o.toString();
				}
				
			}
			Object attrs = insn.getAttr();
			String methname = insn.getMethodInfo().getName();
			System.out.println("methodname: "+methname);
			if(insn.getMethodInfo().hasTypeAnnotatedLocalVars()) {
				String[] varnames = insn.getMethodInfo().getLocalVariableNames();
				if(varnames.length > 0) {
					for(String vn : varnames) {
						System.out.println("varname: "+vn);
					}
				}
			}
			ClassInfo lci = currentThread.getExecutingClassInfo();
			try {
				FieldInfo[] fi = lci.getDeclaredStaticFields();
				if(fi != null) {
					for(int i = 0; i < fi.length; i++) {
						FieldInfo tfi = fi[i];
						String tfi_type = tfi.getType();
						System.out.println("field "+i+" "+tfi_type);
					}
				}
			} catch (NullPointerException npe) {
				System.out.println("npe on getDeclaredStaticFields");
			}
			StateSet ss = vm.getStateSet();
			Heap h = vm.getCurrentThread().getHeap();
			Iterator<ElementInfo> iter = h.iterator();
			int elem_cnt = h.size();
			System.out.println("heap size "+elem_cnt);
			//while(iter.hasNext()) {
			//	ElementInfo ei = iter.next();
				
				//int ei_idx = ei.getObjectRef();
				//System.out.println("heap element idx "+ei_idx);
				//Class clazz = ei.getClass();
				//System.out.println("classname: "+clazz.getName());
			//}
			if(attrs != null) {
				System.out.println("attrs "+attrs.toString());
			}
			
			boolean schedrel = insn.isSchedulingRelevant(vm.getSystemState(), vm.getKernelState(), vm.getCurrentThread());
			if(schedrel) {
				System.out.println("IS scheduling relevant");
			}
			ThreadInfo ti = currentThread;
			Config conf = vm.getConfig();
			if(insn instanceof NEW) {
				NEW ninsn = (NEW)insn;
				Class cl = ninsn.getClass();
				String cname = ninsn.getClassName();
				if(cl.isArray()) {
					System.out.println("class "+cname+" is an array class");
				}
			}
			else if(insn instanceof JVMFieldInstruction) {
				System.out.println("JVMFieldInstruction type");
			}
    	}
    }
    public void threadStarted(VM vm) {}   // new Thread entered run() method
    public void threadTerminated(VM vm) {}   // Thread exited run() method
    
    // detect when a container class is loaded here
    // and initialize a data structure to contain the
    // usage operations on it
    public void classLoaded(VM vm) {
    	System.out.println("class loaded thread: "+vm.getThreadName());
    }      // new class was loaded
    
    // search the active container class structs for this object
    public void objectCreated(VM vm) {
    	System.out.println("object created ");
    }   // new object was created
    
    // close or otherwise finalize the container class struct
    // when it is released
    public void objectReleased(VM vm){
    	System.out.println("object release");
    }  // object was garbage collected
    // detect when a container class is GC'd 
    public void gcBegin(VM vm){
    	System.out.println("GC begins");
    }  // garbage collection mark phase started
    // may need to detect what was GC'd if not reported otherwise 
    public void gcEnd(VM vm){
    	System.out.println("GC ends");
    }   // garbage collection sweep phase terminated
    // detect if a violation of a container access/usage was performed
    public void  exceptionThrown(VM vm){
    	System.out.println("exeception thrown");
    }   // exception was thrown
    
    // random choice has been presented, we may not care?
    public void nextChoice(VM vm){
    	System.out.println("next choice fired");
    }   // choice generator returned new value
}
