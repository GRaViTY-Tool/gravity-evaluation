package org.gravity.eval.fase2017;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.MoveMethodDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.converter.IPGConverter;
import org.gravity.eval.fase2017.util.EclipseProjectUtil;
import org.gravity.eval.fase2017.util.ToFileLogger;
import org.gravity.eval.fase2017.tools.JavaHelper;
import org.gravity.hulk.HAntiPatternDetection;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.HulkFactory;
import org.gravity.hulk.antipatterngraph.AntipatterngraphFactory;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HAntiPatternGraph;
import org.gravity.hulk.detection.HulkDetector;
import org.gravity.hulk.refactoringgraph.HBlobResolveAnnotation;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMember;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMembers;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMethod;
import org.gravity.hulk.refactoringgraph.refactorings.HRefactoring;
import org.gravity.hulk.resolve.antipattern.AntipatternPackage;
import org.gravity.hulk.resolve.antipattern.HAlternativeBlobresolver;
import org.gravity.typegraph.basic.TClass;
import org.gravity.typegraph.basic.TMethodDefinition;
import org.gravity.typegraph.basic.TMethodSignature;
import org.gravity.typegraph.basic.TParameter;
import org.gravity.typegraph.basic.TParameterList;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.moflon.core.dfs.DFSGraph;

@RunWith(Parameterized.class)
public class RQ1_2 {

	@Rule
	public ToFileLogger logToFile = new ToFileLogger(Paths.get(new File("logs").toURI())); //$NON-NLS-1$

	private IJavaProject project;

	private IJavaProject java_project_copy;

	public RQ1_2(IJavaProject project) {
		this.project = project;
	}

	@Parameters(name = "{index}: FASE2017 Eval RQ1+Q2: {0}")
	public static Collection<Object[]> data() {
		List<Object[]> testcases = new ArrayList<>();

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

		for (IProject test : projects) {
			try {
				if (test.getNature(JavaCore.NATURE_ID) != null) {
					testcases.add(new Object[] { JavaCore.create(test) });
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return testcases;
	}

	@Test
	public void resolveBlob() {
		HashSet<EClass> set = new HashSet<>();
		set.add(AntipatternPackage.eINSTANCE.getHBlobResolver());
		assertTrue(perform(set, "blob"));
	}

	private boolean perform(Set<EClass> selection, String ap) {

		NullProgressMonitor monitor = new NullProgressMonitor();

		java_project_copy = EclipseProjectUtil.copyJavaProject(this.project,
				project.getProject().getName() + "_tmp_" + ap);
		if (java_project_copy == null) {
			return false;
		}

		HAntiPatternDetection hulk = HulkFactory.eINSTANCE.createHAntiPatternResolving();
		Set<HDetector> selected_detectors = new HashSet<>();
		Set<HDetector> executed_detectors = new HashSet<>();

		long t0 = System.currentTimeMillis();
		System.out.println(t0 + " Hulk Anti-Pattern Detection");
		System.out.println(t0 + " Init Model");

		IPath project_location = java_project_copy.getProject().getLocation();
		IPGConverter converter = GravityActivator.getDefault().getNewConverter(java_project_copy.getProject());

		boolean success = converter.convertProject(java_project_copy, Collections.emptySet(), monitor);
		if (!success || converter.getPG() == null) {
			fail("Creating PG from project failed: " + java_project_copy.getProject().getName());
			return false;
		}

		long t1 = System.currentTimeMillis();
		System.out.println(t1 + " Init Hulk");
		HAntiPatternGraph apg = AntipatterngraphFactory.eINSTANCE.createHAntiPatternGraph();
		apg.setPg(converter.getPG());

		hulk.setApg(apg);
		hulk.setProgramlocation(project_location.toString());

		ResourceSet rs = converter.getResourceSet();
		rs.createResource(URI.createURI("Hulk.xmi")).getContents().add(hulk); //$NON-NLS-1$

		DFSGraph dependencies = hulk.getDependencyGraph();

		Resource res = rs.createResource(URI.createURI("SemllDependencyGraph.xmi")); //$NON-NLS-1$
		res.getContents().add(dependencies);
		long t2 = System.currentTimeMillis();
		System.out.println(t2 + " Init Hulk - done " + (t2 - t1) + "ms");
		System.out.println(t2 + " Init Model - done " + (t2 - t0) + "ms");

		// long t3 = System.currentTimeMillis();
		// System.out.println(t3 + " Sync Bwd");
		// converter.syncProjectBwd(IPGConverter -> {

		System.out.println(System.currentTimeMillis() + " Hulk Detect AP");
		assertTrue(new HulkDetector(hulk, HulkDetector.getDefaultThresholds()).detectSelectedAntiPattern(selection,
				selected_detectors, executed_detectors));
		System.out.println(System.currentTimeMillis() + " Hulk Detect AP - done");

		// }, monitor);

		// IFolder folder =
		// java_project.getProject().getFolder("src/org/gravity/hulk/annotations");
		// //$NON-NLS-1$
		// if (folder.exists()) {
		// try {
		// folder.delete(true, monitor);
		// } catch (CoreException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }

		long t4 = System.currentTimeMillis();
		System.out.println(t4 + " Hulk Anti-Pattern Detection - Done " + (t4 - t0) + "ms");

		return true;

	}

	@After
	public void after() {
		try {
			if (java_project_copy != null && java_project_copy.exists()) {
				java_project_copy.getProject().delete(true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void refactor(NullProgressMonitor monitor, HAlternativeBlobresolver resolver, IJavaProject project) {
		LinkedList<HRefactoring> moves = new LinkedList<>();
		for (HAnnotation hAnnotation : resolver.getHAnnotation()) {
			if (hAnnotation instanceof HBlobResolveAnnotation) {
				moves.addAll(((HBlobResolveAnnotation) hAnnotation).getHRefactorings());
			}
		}

		try {
			Hashtable<String, IType> types = JavaHelper.getTypesForProject(project);

			for (HRefactoring move : moves) {
				if (move instanceof HMoveMembers) {
					HMoveMembers moveMembers = (HMoveMembers) move;
					for (HMoveMember moveMember : moveMembers.getHMoveMembers()) {
						if (moveMember instanceof HMoveMethod) {
							TClass tTargetClass = moveMembers.getTargetClass();
							TClass tSourceClass = moveMembers.getSourceClass();

							TMethodDefinition tMethod = (TMethodDefinition) ((HMoveMethod) moveMember).getTAnnotated();

							moveMethod(project, tSourceClass, tTargetClass, tMethod.getSignature(), monitor, types);
						}
					}
				}
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	private boolean moveMethod(IJavaProject project, TClass tSourceClass, TClass tTargetClass, TMethodSignature tMethod,
			NullProgressMonitor monitor, Hashtable<String, IType> types) throws JavaModelException {
		if (tSourceClass.isTLib() || tTargetClass.isTLib()) {
			System.err.println("Source or target class is library.");
			return false;
		}

		if (types == null) {
			types = JavaHelper.getTypesForProject(project);
		}

		IType src = types.get(tSourceClass.getFullyQualifiedName());
		IType trg = types.get(tTargetClass.getFullyQualifiedName());

		TParameterList tParamList = tMethod.getParamList();
		String tName = tMethod.getMethod().getTName();
		for (IMethod m : src.getMethods()) {
			if (m.getElementName().equals(tName)) {
				if (m.getNumberOfParameters() == tParamList.getEntries().size()) {
					boolean equal = true;
					TParameter tParam = tParamList.getFirst();
					for (ILocalVariable param : m.getParameters()) {
						if (!(equal = tParam.getType().getFullyQualifiedName()
								.endsWith(Signature.toString(param.getTypeSignature())))) {
							break;
						}
						tParam = tParam.getNext();
					}
					if (equal) {
						System.out.println(m);
						return move2(project, monitor, trg, m);
					} else {
						return false;
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings("restriction")
	private boolean move2(IJavaProject project, NullProgressMonitor monitor, IType trg, IMethod method) {
		Map<String, String> map = new HashMap<String, String>();
		map.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, method.getHandleIdentifier());
		map.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME, method.getElementName());
		map.put("deprecate", "false");
		map.put("remove", "true");
		map.put("inline", "true");
		map.put("getter", "true");
		map.put("setter", "true");
		map.put("targetName", trg.getElementName());
		map.put("targetIndex", "0");

		MoveMethodDescriptor refactoringDescriptor = (MoveMethodDescriptor) RefactoringCore
				.getRefactoringContribution(IJavaRefactorings.MOVE_METHOD)
				.createDescriptor(IJavaRefactorings.MOVE_METHOD, project.getProject().getName(), "move method", "", map,
						RefactoringDescriptor.MULTI_CHANGE);
		RefactoringStatus status = new RefactoringStatus();
		try {
			MoveRefactoring refactoring = (MoveRefactoring) refactoringDescriptor.createRefactoring(status);
			refactoring.checkAllConditions(monitor);
			MoveInstanceMethodProcessor processor = (MoveInstanceMethodProcessor) refactoring.getProcessor();
			boolean detected = false;
			for (IVariableBinding possibleTrg : processor.getPossibleTargets()) {
				String qualifiedName = possibleTrg.getType().getQualifiedName();
				if (trg.getFullyQualifiedName().equals(qualifiedName)) {
					processor.setTarget(possibleTrg);
					detected = true;
					break;
				}
			}
			if (!detected) {
				return false;
			}
			Change change = refactoring.createChange(monitor);
			change.perform(monitor);
			return true;

		} catch (Exception e) {
		}
		return false;
	}

}
