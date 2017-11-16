package gov.nasa.jpf.contcheck;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.jvm.bytecode.LRETURN;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.MySymbolicListener.MyTraceData;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.bytecode.INVOKESTATIC;
import gov.nasa.jpf.symbc.concolic.PCAnalyzer;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicConstraintsGeneral;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.util.StringSetMatcher;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DynamicElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;


public class LinkedListListener extends ListenerAdapter {

	StringSetMatcher includeVars = null;
	private Map<String,MethodSummary> allSummaries;
	private java.util.Stack<MyTraceData> stackSummary;
    private String currentMethodName = "";
    private String target_container;
    private String read_method;
    private String write_method;
    private int inc;

    public LinkedListListener(Config conf, JPF jpf) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		allSummaries = new HashMap<String, MethodSummary>();
		stackSummary = new java.util.Stack<MyTraceData>();
		target_container = conf.getString("contarget");
		read_method = conf.getString("rmethod");
		write_method = conf.getString("wmethod");
		this.inc = 0;
	}

	@Override
	 public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {

		Instruction insn = executedInstruction;
		int tid = currentThread.getId();
		Config conf = vm.getConfig();		
		ThreadInfo ti = currentThread;
		if (!vm.getSystemState().isIgnored()) {
			if (insn instanceof JVMInvokeInstruction) {
				JVMInvokeInstruction md = (JVMInvokeInstruction) insn;
				String methodName = md.getInvokedMethodName();
				int numberOfArgs = md.getArgumentValues(ti).length;

				MethodInfo mi = md.getInvokedMethod();
				ClassInfo ci = mi.getClassInfo();
				String className = ci.getName();
				System.out.println(tid+" InvokeInsn: "+md.getInvokedMethodClassName()+" "+methodName);
				MyTraceData mtd = new MyTraceData(ti.getId()+":"+ti.getName()+":"+ti.getLine(), null, null, this.inc);
				stackSummary.push(mtd);
				}
			} else if (insn instanceof JVMReturnInstruction){
				MethodInfo mi = insn.getMethodInfo();
				ClassInfo ci = mi.getClassInfo();
				if (null != ci){
					String className = ci.getName();
					String methodName = mi.getName();
					String longName = mi.getLongName();
					int numberOfArgs = mi.getNumberOfArguments();
					
					if (((BytecodeUtils.isClassSymbolic(config, className, mi, methodName))
							|| BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null))){
					
						ChoiceGenerator <?>cg = vm.getChoiceGenerator();
						if (!(cg instanceof PCChoiceGenerator)){
							ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
							while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
								prev_cg = prev_cg.getPreviousChoiceGenerator();
							}
							cg = prev_cg;
						}
						if ((cg instanceof PCChoiceGenerator) &&(
								(PCChoiceGenerator) cg).getCurrentPC() != null){
							PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
							//pc.solve(); //we only solve the pc
							if (SymbolicInstructionFactory.concolicMode) { //TODO: cleaner
								SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
								PCAnalyzer pa = new PCAnalyzer();
								pa.solve(pc,solver);
							}
							else
								pc.solve();

							if (!PathCondition.flagSolved) {
							  return;
							}

							//after the following statement is executed, the pc loses its solution

							String pcString = pc.toString();//pc.stringPC();
							Pair<String,String> pcPair = null;

							String returnString = "";


							Expression result = null;

							if (insn instanceof IRETURN){
								IRETURN ireturn = (IRETURN)insn;
								int returnValue = ireturn.getReturnValue();
								IntegerExpression returnAttr = (IntegerExpression) ireturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}else{ // concrete
									returnString = "Return Value: " + String.valueOf(returnValue);
									result = new IntegerConstant(returnValue);
								}
							}
							else if (insn instanceof LRETURN) {
								LRETURN lreturn = (LRETURN)insn;
								long returnValue = lreturn.getReturnValue();
								IntegerExpression returnAttr = (IntegerExpression) lreturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}else{ // concrete
									returnString = "Return Value: " + String.valueOf(returnValue);
									result = new IntegerConstant((int)returnValue);
								}
							}
							else if (insn instanceof DRETURN) {
								DRETURN dreturn = (DRETURN)insn;
								double returnValue = dreturn.getReturnValue();
								RealExpression returnAttr = (RealExpression) dreturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}else{ // concrete
									returnString = "Return Value: " + String.valueOf(returnValue);
									result = new RealConstant(returnValue);
								}
							}
							else if (insn instanceof FRETURN) {
								FRETURN freturn = (FRETURN)insn;
								double returnValue = freturn.getReturnValue();
								RealExpression returnAttr = (RealExpression) freturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}else{ // concrete
									returnString = "Return Value: " + String.valueOf(returnValue);
									result = new RealConstant(returnValue);
								}
							}
							else if (insn instanceof ARETURN){
								ARETURN areturn = (ARETURN)insn;
								IntegerExpression returnAttr = (IntegerExpression) areturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}
								else {// concrete
									DynamicElementInfo val = (DynamicElementInfo)areturn.getReturnValue(ti);
									
									//System.out.println("string "+val.asString());
									returnString = "Return Value: " + val.asString();
									//DynamicElementInfo val = (DynamicElementInfo)areturn.getReturnValue(ti);
									String tmp = val.asString();
									tmp = tmp.substring(tmp.lastIndexOf('.')+1);
									result = new SymbolicInteger(tmp);
								}
							}
							else //other types of return
								returnString = "Return Value: --";

							pcString = pc.toString();
							pcPair = new Pair<String,String>(pcString,returnString);
							MethodSummary methodSummary = allSummaries.get(longName);
							Vector<Pair> pcs = methodSummary.getPathConditions();
							if ((!pcs.contains(pcPair)) && (pcString.contains("SYM"))) {
								methodSummary.addPathCondition(pcPair);
							}
							
							if(allSummaries.get(longName)!=null) // recursive call
								longName = longName;// + methodSummary.hashCode(); // differentiate the key for recursive calls
							allSummaries.put(longName,methodSummary);
							if (SymbolicInstructionFactory.debugMode) {
							    System.out.println("*************Summary***************");
							    System.out.println("PC is:"+pc.toString());
							    if(result!=null){
								System.out.println("Return is:  "+result);
								System.out.println("***********************************");
							    }
							}
						}
					}
				}
			} // end of instruction type checks
			if(!stackSummary.isEmpty()) {
				int idx;
				int stackcount = 0;
				for(idx = 0; idx < stackSummary.size(); idx++) {
					stackcount += stackSummary.get(idx).getIncrement();
					if(stackcount < 0) { // check along the way when it may have violated
						System.out.println("VIOLATION: CONTAINER READ ON EMPTY OBJECT: "+idx);
					}
				}
				//if(stackcount < 0) {
				//	System.out.println("VIOLATION, CONTAINER READ ON EMPTY OBJECT");
				//}
			}
		}
	}
	
	  protected class MyTraceData {
		  private String threadName;
		  private String methodName;
		  private MethodSummary summary;
		  private int increment;
		  
		  public MyTraceData() {
			  
		  }
		  public MyTraceData(String tname, String name, MethodSummary ms, int inc) {
			  this.threadName = tname;
			  this.methodName = name;
			  this.summary = ms;
			  this.increment = inc;
		  }
		  public void setTI(String t) {
			  this.threadName = t;
		  }
		  public void setName(String name) {
			  this.methodName = name;
		  }
		  public void setSummary(MethodSummary m) {
			  this.summary = m;
		  }
		  public void setIncrement(int inc) {
			  this.increment = inc;
		  }
		  public String getTI() {
			  return this.threadName;
		  }
		  public String getName() {
			  return this.methodName;
		  }
		  public MethodSummary getMS() {
			  return this.summary;
		  }
		  public int getIncrement() {
			  return this.increment;
		  }
	  }

	  protected class MethodSummary{
			private String methodName = "";
			private String argTypes = "";
			private String argValues = "";
			private String symValues = "";
			private Vector<Pair> pathConditions;

			public MethodSummary(){
			 	pathConditions = new Vector<Pair>();
			}

			public void setMethodName(String mName){
				this.methodName = mName;
			}

			public String getMethodName(){
				return this.methodName;
			}

			public void setArgTypes(String args){
				this.argTypes = args;
			}

			public String getArgTypes(){
				return this.argTypes;
			}

			public void setArgValues(String vals){
				this.argValues = vals;
			}

			public String getArgValues(){
				return this.argValues;
			}

			public void setSymValues(String sym){
				this.symValues = sym;
			}

			public String getSymValues(){
				return this.symValues;
			}

			public void addPathCondition(Pair pc){
				pathConditions.add(pc);
			}

			public Vector<Pair> getPathConditions(){
				return this.pathConditions;
			}

	  }
	  
	  
}
