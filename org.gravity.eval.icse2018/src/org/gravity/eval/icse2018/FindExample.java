package org.gravity.eval.icse2018;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.MoveMethodDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.JavaHelper;
import org.gravity.eclipse.converter.IPGConverter;
import org.gravity.eclipse.exceptions.NoConverterRegisteredException;
import org.gravity.hulk.HAntiPatternDetection;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.HulkFactory;
import org.gravity.hulk.antipatterngraph.AntipatterngraphFactory;
import org.gravity.hulk.antipatterngraph.HAntiPatternGraph;
import org.gravity.hulk.detection.HRelativeDetector;
import org.gravity.hulk.detection.HulkDetector;
import org.gravity.typegraph.basic.TAbstractType;
import org.gravity.typegraph.basic.TAccess;
import org.gravity.typegraph.basic.TFieldDefinition;
import org.gravity.typegraph.basic.TMember;
import org.gravity.typegraph.basic.TMethod;
import org.gravity.typegraph.basic.TMethodDefinition;
import org.gravity.typegraph.basic.TMethodSignature;
import org.gravity.typegraph.basic.TParameter;
import org.junit.Test;
import org.moflon.core.dfs.DFSGraph;

public class FindExample {

	@Test
	public void clalcBlos() throws NoConverterRegisteredException {
		Hashtable<String, String> defaultThresholds = HulkDetector.getDefaultThresholds();

		Hashtable<String, String> newThresholds = detectBlobs(defaultThresholds,
				ResourcesPlugin.getWorkspace().getRoot().getProject("JSSE_OpenJDK8"));

		newThresholds = detectBlobs(newThresholds,
				ResourcesPlugin.getWorkspace().getRoot().getProject("JSSE_OpenJDK8_Debug"));

		newThresholds = detectBlobs(newThresholds,
				ResourcesPlugin.getWorkspace().getRoot().getProject("JSSE_OpenJDK8_SSLEngine"));
	}

	@Test
	public void searchPG() throws NoConverterRegisteredException {

		 try (PrintWriter printer = new PrintWriter(new
		 File("possibleExamplePG.txt"))) {
		 IProject iProject =
		 ResourcesPlugin.getWorkspace().getRoot().getProject("mozilla-rhino");
		 IJavaProject iJavaProject = JavaCore.create(iProject);
		
		 IPGConverter converter =
		 GravityActivator.getDefault().getConverter(iProject);
		 converter.convertProject(iJavaProject, new NullProgressMonitor());
		 for (TMethod tMethod : converter.getPG().getMethods()) {
		 String tMethodName = tMethod.getTName();
		 if (!tMethodName.startsWith("set") && !tMethodName.startsWith("get"))
		 {
		 for (TMethodSignature tSignature : tMethod.getSignatures()) {
		 for (TMethodDefinition tMethodDefinition :
		 tSignature.getDefinitions()) {
		 TAbstractType tSourceClass = tMethodDefinition.getDefinedBy();
		 if (!tSourceClass.isTLib() &&
		 tMethodDefinition.getOverriddenBy().size() == 0 &&
		 tMethodDefinition.getOverriding() == null) {
		 Set<TAbstractType> targets = new HashSet<>();
		 for (TAccess tAccess : tMethodDefinition.getAccessedBy()) {
		 TAbstractType definedBy = tAccess.getTSource().getDefinedBy();
		 if (!definedBy.isTLib()) {
		 targets.add(definedBy);
		 }
		 }
		 for (TAccess tAccess : tMethodDefinition.getTAccessing()) {
		 TAbstractType definedBy = tAccess.getTTarget().getDefinedBy();
		 if (!definedBy.isTLib()) {
		 targets.add(definedBy);
		 }
		 }
		 if (targets.size() > 1) {
		 Set<TAbstractType> allowed = new HashSet<>();
		 allowed.add(tSignature.getReturnType());
		 for(TParameter tParameter : tSignature.getParamList().getEntries()){
		 allowed.add(tParameter.getType());
		 }
		 for(TMember tDefinition : tSourceClass.getDefines()){
		 if (tDefinition instanceof TFieldDefinition) {
		 allowed.add(((TFieldDefinition)
		 tDefinition).getSignature().getType());
		 }
		 }
		 targets.retainAll(allowed);
		 if(targets.size() > 1){
		 String source = tSourceClass.getFullyQualifiedName();
		 source = source.substring(0, source.lastIndexOf('.'));
		
		 boolean same = false, different = false;
		 for(TAbstractType tTargetClass : targets){
		 String target = tTargetClass.getFullyQualifiedName();
		 target = target.substring(0, target.lastIndexOf('.'));
		
		 if(target.startsWith(source)){
		 same = true;
		 }
		 else{
		 different = true;
		 }
		 }
		
		
		 if(same && different){
		 printer.println(tSourceClass.getFullyQualifiedName()+"."+tMethodDefinition.getSignatureString());
		 for(TAbstractType tTargetClass : targets){
		 printer.println("\t-> "+tTargetClass.getFullyQualifiedName());
		 }
		 }
		 }
		 }
		 }
		 }
		 }
		 }
		 }
		 } catch (FileNotFoundException e) {
		 // TODO Auto-generated catch block
		 e.printStackTrace();
		 }
	}

	private Hashtable<String, String> detectBlobs(Hashtable<String, String> defaultThresholds, IProject iProject) throws NoConverterRegisteredException {
		IJavaProject iJavaProject = JavaCore.create(iProject);
		IPGConverter converter = GravityActivator.getDefault().getConverter(iProject);
		converter.convertProject(iJavaProject, new NullProgressMonitor());
		HAntiPatternDetection hulk = HulkFactory.eINSTANCE.createHAntiPatternResolving();
		HulkDetector hulkDetector = new HulkDetector(hulk, defaultThresholds);
		Set<EClass> selection = new HashSet<>();
		selection.add(org.gravity.hulk.detection.antipattern.AntipatternPackage.eINSTANCE.getHBlobDetector());
		HAntiPatternGraph apg = AntipatterngraphFactory.eINSTANCE.createHAntiPatternGraph();
		apg.setPg(converter.getPG());
		hulk.getHDetector().clear();
		hulk.setApg(apg);
		hulk.setProgramlocation(iProject.toString());
		ResourceSet rs = converter.getResourceSet();
		rs.createResource(URI.createURI("Hulk.xmi")).getContents().add(hulk); //$NON-NLS-1$
		DFSGraph dependencies = hulk.getDependencyGraph();
		Set<HDetector> selected_detectors = new HashSet<>();
		Set<HDetector> executed_detectors = new HashSet<>();
		hulkDetector.detectSelectedAntiPattern(selection, selected_detectors, executed_detectors);
		System.err.println("Detected " + selected_detectors.iterator().next().getHAnnotation().size() + " Blobs");
		Hashtable<String, String> newThresholds = new Hashtable<String, String>();
		for (HDetector detector : executed_detectors) {
			if (detector instanceof HRelativeDetector) {
				newThresholds.put(detector.getClass().getName().replace("Impl", "").replace(".impl", ""),
						Double.toString(((HRelativeDetector) detector).getThreshold()));
			}
		}
		return newThresholds;
	}

	@Test
	public void search() throws JavaModelException {
		try (PrintWriter printer = new PrintWriter(new File("possibleExample.txt"))) {
			IProject iProject = ResourcesPlugin.getWorkspace().getRoot().getProject("JSSE_OpenJDK8");
			IJavaProject iJavaProject = JavaCore.create(iProject);
			Hashtable<String, IType> types = JavaHelper.getTypesForProject(iJavaProject);

			for (IType type : types.values()) {
				String qualifiedSourceName = type.getFullyQualifiedName().replace("." + type.getElementName(), "");
				for (IMethod method : type.getMethods()) {
					if (method.getElementName().startsWith("set") || method.getElementName().startsWith("get")) {
						continue;
					}
					Map<String, String> map = new HashMap<String, String>();
					map.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, method.getHandleIdentifier());
					map.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME, method.getElementName());
					map.put("deprecate", "false");
					map.put("remove", "true");
					map.put("inline", "true");
					map.put("getter", "true");
					map.put("setter", "true");
					map.put("targetIndex", "0");

					MoveMethodDescriptor refactoringDescriptor = (MoveMethodDescriptor) RefactoringCore
							.getRefactoringContribution(IJavaRefactorings.MOVE_METHOD)
							.createDescriptor(IJavaRefactorings.MOVE_METHOD, iJavaProject.getProject().getName(),
									"move method", "", map, RefactoringDescriptor.MULTI_CHANGE);
					RefactoringStatus status = new RefactoringStatus();
					try {
						MoveRefactoring refactoring = (MoveRefactoring) refactoringDescriptor.createRefactoring(status);
						refactoring.checkAllConditions(new NullProgressMonitor());
						MoveInstanceMethodProcessor processor = (MoveInstanceMethodProcessor) refactoring
								.getProcessor();
						boolean different = false, same = false;
						if (processor.getPossibleTargets().length > 1)
							for (IVariableBinding possibleTrg : processor.getPossibleTargets()) {
								String qualifiedName = possibleTrg.getType().getQualifiedName()
										.replace("." + possibleTrg.getType().getName(), "");
								if (qualifiedName.startsWith(qualifiedSourceName)) {
									same = true;
								} else {
									different = true;
								}
							}
						if (same && different) {
							printer.println(type.getFullyQualifiedName() + "." + method.getElementName() + "("
									+ Arrays.asList(method.getParameterTypes()) + "):" + method.getReturnType());
						}
					} catch (Exception e) {
					}
				}
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}
}
