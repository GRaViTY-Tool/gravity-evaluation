package org.gravity.eval.icse2018;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveMethodDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.converter.IPGConverter;
import org.gravity.eval.icse2018.util.EclipseProjectUtil;
import org.gravity.eval.icse2018.util.sourcemeter.MetricCalculator;
import org.gravity.eval.icse2018.util.sourcemeter.SourcemeterMetricKeys;
import org.gravity.hulk.HAntiPatternDetection;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.HulkFactory;
import org.gravity.hulk.antipatterngraph.AntipatterngraphFactory;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HAntiPatternGraph;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.detection.HRelativeDetector;
import org.gravity.hulk.detection.HulkDetector;
import org.gravity.hulk.detection.antipattern.HBlobDetector;
import org.gravity.hulk.refactoringgraph.HBlobResolveAnnotation;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMember;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMembers;
import org.gravity.hulk.refactoringgraph.refactorings.HMoveMethod;
import org.gravity.hulk.refactoringgraph.refactorings.HRefactoring;
import org.gravity.hulk.resolve.antipattern.HAlternativeBlobresolver;
import org.gravity.typegraph.basic.TAbstractType;
import org.gravity.typegraph.basic.TClass;
import org.gravity.typegraph.basic.TMethodDefinition;
import org.gravity.typegraph.basic.TMethodSignature;
import org.gravity.typegraph.basic.TParameter;
import org.gravity.typegraph.basic.TParameterList;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moflon.core.dfs.DFSGraph;

import com.beust.jcommander.ParameterException;

import at.ac.tuwien.big.momot.TransformationResultManager;
import at.ac.tuwien.big.momot.problem.solution.variable.RuleApplicationVariable;
import at.ac.tuwien.big.momot.problem.solution.variable.TransformationPlaceholderVariable;
import at.ac.tuwien.big.momot.problem.solution.variable.UnitApplicationVariable;
import de.uni_hamburg.informatik.swt.accessanalysis.Analysis;
import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisException;
import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisFactory;
import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisFactory.AnalysisMode;
import de.uni_hamburg.informatik.swt.accessanalysis.results.ResultFormatter;
import momotFiles.SearchTypeGraph;

public class Eval {

	private IJavaProject java_project_copy;

	public void run(IJavaProject project, String apSuffix, boolean enableMetricRecording) throws FileNotFoundException, IOException {
		NullProgressMonitor monitor = new NullProgressMonitor();

		long currentTimeMillis = System.currentTimeMillis();

		java_project_copy = EclipseProjectUtil.copyJavaProject(project,
				project.getProject().getName() + "_" + apSuffix + "_" + currentTimeMillis);
		if (java_project_copy == null) {
			fail();
		}
		Hashtable<String, IType> types = null;
		try {
			types = getTypesForProject(java_project_copy);
		} catch (JavaModelException e1) {
			fail();
		}

		File project_folder = new File(project.getProject().getName());
		File folder = new File(project_folder, Long.toString(currentTimeMillis));
		folder.mkdirs();
		File initial = new File(project_folder, "initial");

		HAntiPatternDetection hulk = HulkFactory.eINSTANCE.createHAntiPatternResolving();
		HulkDetector hulkDetector = new HulkDetector(hulk, HulkDetector.getDefaultThresholds());
		File pgFile = new File(folder, "pg.xmi");
		Set<EClass> selection = new HashSet<>();
		selection.add(org.gravity.hulk.detection.antipattern.AntipatternPackage.eINSTANCE.getHBlobDetector());
		Set<HDetector> executed_detectors = detect(selection, monitor, hulk, hulkDetector, pgFile);

		StringBuilder hulkconfig = new StringBuilder();
		HBlobDetector blob = null;
		for (HDetector detector : executed_detectors) {
			if (detector instanceof HBlobDetector) {
				blob = (HBlobDetector) detector;
			}
			if (detector instanceof HRelativeDetector) {
				HRelativeDetector rel = (HRelativeDetector) detector;
				hulkconfig.append(rel.getClass().getSimpleName());
				hulkconfig.append(' ');
				hulkconfig.append(rel.getThreshold());
				hulkconfig.append('\n');
			}
		}

		MetricCalculator metrics = null;
		if (enableMetricRecording) {
			metrics = new MetricCalculator();
			if (!initial.exists()) {
				initial.mkdirs();
				Files.write(new File(initial, "hulkConfig.csv").toPath(), hulkconfig.toString().getBytes());
				printSourcemeterMetrics(metrics, initial);
				printBlobs(blob, initial);
				printAccessibilityMetric(initial, monitor);
			}
		}

		SearchTypeGraph search = null;
		try {
			search = new SearchTypeGraph();
		} catch (ParameterException e) {
			fail();
			return;
		}

		search.initializeFitnessFunctions();
		search.initializeConstraints();
		TransformationResultManager results = search.performSearch(pgFile.getAbsolutePath(), 10, folder);

		for (List<NondominatedPopulation> r : results.getResults().values()) {
			for (NondominatedPopulation nPop : r) {
				int sol = 0;
				for (Solution solution : nPop) {
					try (PrintWriter writer = new PrintWriter(new File(folder, "move_" + (sol++)))) {
						for (int i = 0; i < solution.getNumberOfVariables(); i++) {
							Variable var = solution.getVariable(i);
							TMethodSignature sig;
							TClass src = null, trg = null;
							if (var instanceof TransformationPlaceholderVariable) {
								continue;
							} else if (var instanceof RuleApplicationVariable) {
								sig = (TMethodSignature) ((RuleApplicationVariable) var).getParameterValue("methodSig");
								src = (TClass) ((RuleApplicationVariable) var).getParameterValue("sourceClass");
								trg = (TClass) ((RuleApplicationVariable) var).getParameterValue("targetClass");

							} else if (var instanceof UnitApplicationVariable) {
								sig = (TMethodSignature) ((UnitApplicationVariable) var).getParameterValue("methodSig");
								src = (TClass) ((UnitApplicationVariable) var).getParameterValue("sourceClass");
								trg = (TClass) ((UnitApplicationVariable) var).getParameterValue("targetClass");
							} else {
								System.err.println("Not found");
								continue;
							}

							try {
								writer.print(src.getFullyQualifiedName() + "." + sig.getSignatureString() + " -> "
										+ trg.getFullyQualifiedName());
								if (!moveMethod(src, trg, sig, monitor, types, java_project_copy)) {
									System.err.println("Couldn't move: " + src.getFullyQualifiedName() + "."
											+ sig.getSignatureString() + " to class " + trg.getFullyQualifiedName());
								} else {
									writer.println(": automatically moved");
								}
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}

		// refactor(monitor, resolver);

		if (enableMetricRecording) {
			Set<EClass> secondSelection = new HashSet<>();
			secondSelection.add(org.gravity.hulk.detection.antipattern.AntipatternPackage.eINSTANCE.getHBlobDetector());
			executed_detectors = detect(secondSelection, monitor, hulk, hulkDetector, null);

			for (HDetector detector : executed_detectors) {
				if (detector instanceof HBlobDetector) {
					blob = (HBlobDetector) detector;
				}
			}

			printSourcemeterMetrics(metrics, folder);
			printBlobs(blob, folder);
			printAccessibilityMetric(folder, monitor);
		}
	}

	public void printAccessibilityMetric(File folder, NullProgressMonitor monitor) {
		try {
			Analysis accessAnalysis = AnalysisFactory.analyzer(java_project_copy, AnalysisMode.ACCESS_QUIET);
			accessAnalysis.run(monitor);
			ResultFormatter formatter = accessAnalysis.getResults().get(0).getFormatter();
			Files.write(new File(folder, "accessMetrics.txt").toPath(),
					("igat = " + formatter.igat() + " igam = " + formatter.igam()).getBytes());
		} catch (AnalysisException | IOException e) {
			e.printStackTrace();
		}
	}

	private void refactor(NullProgressMonitor monitor, HAlternativeBlobresolver resolver) {
		LinkedList<HRefactoring> moves = new LinkedList<>();
		for (HAnnotation hAnnotation : resolver.getHAnnotation()) {
			if (hAnnotation instanceof HBlobResolveAnnotation) {
				moves.addAll(((HBlobResolveAnnotation) hAnnotation).getHRefactorings());
			}
		}

		try {
			Hashtable<String, IType> types = getTypesForProject(this.java_project_copy);

			for (HRefactoring move : moves) {
				if (move instanceof HMoveMembers) {
					HMoveMembers moveMembers = (HMoveMembers) move;
					for (HMoveMember moveMember : moveMembers.getHMoveMembers()) {
						if (moveMember instanceof HMoveMethod) {
							TClass tTargetClass = moveMembers.getTargetClass();
							TClass TSourceClass = moveMembers.getSourceClass();

							TMethodDefinition tMethod = (TMethodDefinition) ((HMoveMethod) moveMember).getTAnnotated();

							moveMethod(TSourceClass, tTargetClass, tMethod.getSignature(), monitor, types,
									java_project_copy);
						}
					}
				}
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private boolean moveMethod(TClass tSourceClass, TClass tTargetClass, TMethodSignature tMethod,
			NullProgressMonitor monitor, Hashtable<String, IType> types, IJavaProject project)
			throws JavaModelException {
		if (tSourceClass.isTLib() || tTargetClass.isTLib()) {
			System.err.println("Source or target class is library.");
			return false;
		}

		if (types == null) {
			types = getTypesForProject(project);
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
						return move2(monitor, trg, m);
					} else {
						return false;
					}
				}
			}
		}
		return false;
	}

	private static Hashtable<String, IType> getTypesForProject(IJavaProject project) throws JavaModelException {
		Hashtable<String, IType> types = new Hashtable<>();

		for (IPackageFragmentRoot element : project.getPackageFragmentRoots()) {
			Stack<IJavaElement> children = new Stack<IJavaElement>();
			children.addAll(Arrays.asList(element.getChildren()));
			while (!children.isEmpty()) {
				IJavaElement packageFragment = children.pop();
				if (packageFragment.getElementType() == IJavaElement.COMPILATION_UNIT) {
					for (IType type : ((ICompilationUnit) packageFragment).getAllTypes()) {
						types.put(type.getFullyQualifiedName(), type);
					}
				} else if (packageFragment.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
					children.addAll(Arrays.asList(((IPackageFragment) packageFragment).getChildren()));
				}
			}
		}
		return types;
	}

	@SuppressWarnings("restriction")
	private boolean move2(NullProgressMonitor monitor, IType trg, IMethod method) {
		// MoveInstanceMethodProcessor processorMM = new
		// MoveInstanceMethodProcessor(method,null);
		// try {
		// if(!processorMM.isApplicable()){
		// return;
		// }
		// } catch (CoreException e) {
		// e.printStackTrace();
		// }
		// processorMM.setMethodName(method.getElementName());
		// processorMM.setTargetName(method.getDeclaringType().toString());
		// IVariableBinding[] candidateTargets =
		// processorMM.getCandidateTargets();
		// IVariableBinding[] targets= processorMM.getPossibleTargets();
		//
		// processorMM.setTarget(targets[0]);

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
				.createDescriptor(IJavaRefactorings.MOVE_METHOD, java_project_copy.getProject().getName(),
						"move method", "", map, RefactoringDescriptor.MULTI_CHANGE);
		RefactoringStatus status = new RefactoringStatus();
		try {
			MoveRefactoring refactoring = (MoveRefactoring) refactoringDescriptor.createRefactoring(status);
			refactoring.checkAllConditions(monitor);
			MoveInstanceMethodProcessor processor = (MoveInstanceMethodProcessor) refactoring.getProcessor();
			boolean detected = false;
			for (IVariableBinding possibleTrg : processor.getPossibleTargets()) {
				String qualifiedName = possibleTrg.getType().getQualifiedName();
				if (trg.getFullyQualifiedName().equals(possibleTrg.getType().getQualifiedName())) {
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
			e.printStackTrace();
		}
		return false;
	}

	private void move1(NullProgressMonitor monitor, IType trg, IMethod m) throws CoreException {
		MoveDescriptor moveDescriptor = new MoveDescriptor();
		moveDescriptor.setMoveMembers(new IMember[] { m });
		moveDescriptor.setDestination(trg);
		moveDescriptor.setProject(java_project_copy.getProject().getName());
		RefactoringStatus status = moveDescriptor.validateDescriptor();
		if (status.isOK()) {
			RefactoringContext refactoringContext = moveDescriptor.createRefactoringContext(status);
			Refactoring refactoring = refactoringContext.getRefactoring();
			RefactoringStatus checkInitialConditions = refactoring.checkAllConditions(monitor);
			if (status.isOK()) {
				Change change = refactoring.createChange(monitor);
				change.perform(monitor);
			}
			// PerformRefactoringOperation perform = new
			// PerformRefactoringOperation(refactoringContext,
			// CheckConditionsOperation.ALL_CONDITIONS);
			// perform.run(monitor);
		} else {
			System.err.println("Not in OK Status");
		}
	}

	private void printSourcemeterMetrics(MetricCalculator metrics, File folder) {
		metrics.calculateMetrics(java_project_copy.getProject().getLocation().toFile(),
				new File(folder, "sourcemeter"));
		Hashtable<String, String> lcom5 = metrics.getMetrics(SourcemeterMetricKeys.LCOM5);
		double avgLCOM5 = 0;
		for (String value : lcom5.values()) {
			avgLCOM5 += Double.valueOf(value);
		}
		avgLCOM5 = avgLCOM5 / lcom5.size();
		Hashtable<String, String> cbo = metrics.getMetrics(SourcemeterMetricKeys.CBO);
		double avgCBO = 0;
		for (String value : cbo.values()) {
			avgCBO += Double.valueOf(value);
		}
		avgCBO = avgCBO / cbo.size();
		try {
			Files.write(new File(folder, "avgMetrics.txt").toPath(),
					("avg lcom5 = " + avgLCOM5 + "\navg CBO = " + avgCBO).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printBlobs(HBlobDetector blob, File folder) {
		String programName = blob.getHAntiPatternHandling().getApg().getPg().getTName();
		File out = new File(folder, programName + "_Blobs.txt");
		try (FileOutputStream fileOut = new FileOutputStream(out);
				BufferedOutputStream buffered = new BufferedOutputStream(fileOut);) {
			buffered.write(("Detected Blob APs on " + programName + ":\n").getBytes());
			for (HAnnotation annot : blob.getHAnnotation()) {
				HBlobAntiPattern hBlob = (HBlobAntiPattern) annot;
				buffered.write((((TAbstractType) hBlob.getTAnnotated()).getFullyQualifiedName() + "\n").getBytes());
			}
			buffered.flush();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	protected Set<HDetector> detect(Set<EClass> selection, NullProgressMonitor monitor, HAntiPatternDetection hulk,
			HulkDetector hulkDetector, File file) throws FileNotFoundException, IOException {
		IPath project_location = java_project_copy.getProject().getLocation();
		IPGConverter converter = GravityActivator.getDefault().getNewConverter(java_project_copy.getProject());

		long start = System.currentTimeMillis();
		boolean success = converter.convertProject(java_project_copy, Collections.emptySet(), monitor);
		long createPgInMS = System.currentTimeMillis() - start;
		if (!success || converter.getPG() == null) {
			fail("Creating PG from project failed: " + java_project_copy.getProject().getName());
		}

		if (file != null) {

			Files.write(new File(file.getParentFile(), "timeHulkInMS.txt").toPath(),
					Long.toString(createPgInMS).getBytes());
			converter.getPG().eResource().save(new FileOutputStream(file), Collections.EMPTY_MAP);
		}

		HAntiPatternGraph apg = AntipatterngraphFactory.eINSTANCE.createHAntiPatternGraph();
		apg.setPg(converter.getPG());

		hulk.getHDetector().clear();
		hulk.setApg(apg);
		hulk.setProgramlocation(project_location.toString());

		ResourceSet rs = converter.getResourceSet();
		rs.createResource(URI.createURI("Hulk.xmi")).getContents().add(hulk); //$NON-NLS-1$

		DFSGraph dependencies = hulk.getDependencyGraph();

		Resource res = rs.createResource(URI.createURI("SemllDependencyGraph.xmi")); //$NON-NLS-1$
		res.getContents().add(dependencies);

		Set<HDetector> selected_detectors = new HashSet<>();
		Set<HDetector> executed_detectors = new HashSet<>();
		assertTrue(hulkDetector.detectSelectedAntiPattern(selection, selected_detectors, executed_detectors));
		return executed_detectors;

	}
}
