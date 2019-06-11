package org.gravity.eval.icse2018;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.converter.IPGConverter;
import org.gravity.eclipse.exceptions.NoConverterRegisteredException;
import org.gravity.eclipse.util.JavaProjectUtil;
import org.gravity.goblin.momot.SearchTypeGraph;
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
import org.gravity.metrics.sourcemeter.MetricPrinter;
import org.gravity.metrics.sourcemeter.SourcemeterMetricKeys;
import org.gravity.refactorings.ui.EclipseMoveMethodRefactoring;
import org.gravity.typegraph.basic.TAbstractType;
import org.gravity.typegraph.basic.TClass;
import org.gravity.typegraph.basic.TMethodSignature;
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

public class Eval {

	private boolean enableMetricRecording;
	private Hashtable<String, String> hulkConfigTable;
	private int blobs;

	public void run(IJavaProject project, String apSuffix, boolean enableMetricRecording)
			throws FileNotFoundException, IOException, NoConverterRegisteredException, CoreException {
		NullProgressMonitor monitor = new NullProgressMonitor();
		this.enableMetricRecording = enableMetricRecording;

		long currentTimeMillis = System.currentTimeMillis();

		File project_folder = new File(project.getProject().getName());
		File folder = new File(project_folder, Long.toString(currentTimeMillis));
		folder.mkdirs();
		File initial = new File(project_folder, "initial");

		File pgFile = new File(folder, "pg.xmi");
		Set<HDetector> executed_detectors = detect(project, HulkDetector.getDefaultThresholds(), pgFile, monitor);

		StringBuilder hulkconfig = new StringBuilder();
		hulkConfigTable = new Hashtable<String, String>();
		HBlobDetector blob = null;
		for (HDetector detector : executed_detectors) {
			if (detector instanceof HBlobDetector) {
				blob = (HBlobDetector) detector;
			}
			if (detector instanceof HRelativeDetector) {
				HRelativeDetector rel = (HRelativeDetector) detector;
				hulkconfig.append(rel.getClass().getName().replace("Impl", "").replace(".impl", ""));
				hulkconfig.append(' ');
				hulkconfig.append(rel.getThreshold());
				hulkconfig.append('\n');

				hulkConfigTable.put(rel.getClass().getName().replace("Impl", "").replace(".impl", ""),
						Double.toString(rel.getThreshold()));
			}
		}
		blobs = blob.getHAnnotation().size();

		if (enableMetricRecording) {
			if (!initial.exists()) {
				initial.mkdirs();
				Files.write(new File(initial, "hulkConfig.csv").toPath(), hulkconfig.toString().getBytes());
				MetricPrinter.printSourcemeterMetrics(project.getProject(), initial);
				printBlobs(blob, initial);
				printAccessibilityMetric(project, initial, monitor);
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

		refactor(monitor, folder, results, project, Long.toString(currentTimeMillis));

		// refactor(monitor, resolver);
	}

	private void refactor(NullProgressMonitor monitor, File folder, TransformationResultManager results,
			IJavaProject project, String suffix) throws FileNotFoundException, NoConverterRegisteredException {
		for (List<NondominatedPopulation> r : results.getResults().values()) {
			for (NondominatedPopulation nPop : r) {
				int sol = 0;
				for (Solution solution : nPop) {
					IJavaProject java_project_copy = JavaProjectUtil.copyJavaProject(project,
							project.getProject().getName() + "_" + suffix + "_" + sol);

					if (java_project_copy == null) {
						fail();
					}
					EclipseMoveMethodRefactoring refactor = null;
					try {
						refactor = new EclipseMoveMethodRefactoring(java_project_copy);
					} catch (JavaModelException e1) {
						fail();
					}

					boolean success = true;
					StringBuilder todo = new StringBuilder();
					int refactorings = 0;

					File solutionFolder = new File(folder, Integer.toString(sol));
					solutionFolder.mkdirs();
					try (PrintWriter writer = new PrintWriter(new File(solutionFolder, "move_" + (sol++)))) {
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

							refactorings++;

							try {
								String refactoring = src.getFullyQualifiedName() + "." + sig.getSignatureString()
										+ " -> " + trg.getFullyQualifiedName();
								writer.print(refactoring);
								if (refactor.moveMethod(src, trg, sig, monitor)) {
									writer.println(": automatically moved");
								} else {
									System.err.println("Couldn't move: " + src.getFullyQualifiedName() + "."
											+ sig.getSignatureString() + " to class " + trg.getFullyQualifiedName());
									writer.println();
									todo.append(refactoring);
									todo.append('\n');
									success = false;
								}
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
						}
					}

					if (success) {
						if (enableMetricRecording) {
							try {
								if (IMarker.SEVERITY_ERROR == project.getUnderlyingResource().findMaxProblemSeverity(
										IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)
										|| IMarker.SEVERITY_ERROR == project.getUnderlyingResource()
												.findMaxProblemSeverity(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER,
														true, IResource.DEPTH_INFINITE)) {

									try {
										Files.write(new File(solutionFolder, "FAIL").toPath(),
												("The java project has build errors\nblobs=" + blobs + "\nrefactorings="
														+ refactorings).getBytes());
									} catch (IOException e) {
										e.printStackTrace();
									}
								} else {
									Set<EClass> secondSelection = new HashSet<>();
									secondSelection
											.add(org.gravity.hulk.detection.antipattern.AntipatternPackage.eINSTANCE
													.getHBlobDetector());
									Set<HDetector> executed_detectors = detect(java_project_copy, hulkConfigTable,
											new File(solutionFolder, "pg.xmi"), monitor);

									HBlobDetector blob = null;
									for (HDetector detector : executed_detectors) {
										if (detector instanceof HBlobDetector) {
											blob = (HBlobDetector) detector;
										}
									}
									Hashtable<String, String> sourcemeter = MetricPrinter.printSourcemeterMetrics(java_project_copy.getProject(),
											solutionFolder);
									printBlobs(blob, solutionFolder);
									Hashtable<String, String> accessibility = printAccessibilityMetric(
											java_project_copy, solutionFolder, monitor);

									try (PrintWriter printer = new PrintWriter(new File(solutionFolder, "stats.csv"))) {
										printer.print(java_project_copy.getProject().getName());
										printer.print(' ');
										printer.print(Integer.toString(refactorings));
										printer.print(' ');
										printer.print(sourcemeter.get(SourcemeterMetricKeys.LCOM5));
										printer.print(' ');
										printer.print(sourcemeter.get(SourcemeterMetricKeys.CBO));
										printer.print(' ');
										printer.print(Integer.toString(blobs - blob.getHAnnotation().size()));
										printer.print(' ');
										printer.print(accessibility.get("igam"));
										printer.println();
									} catch (Exception e) {
									}
								}
							} catch (CoreException e) {
								e.printStackTrace();
							}
						}
					} else {
						try {
							java_project_copy.getProject().getFile("TODO.txt")
									.create(new ByteArrayInputStream(todo.toString().getBytes()), true, monitor);
							Files.write(new File(solutionFolder, "FAIL").toPath(),
									("Not all refactorings have been performed sucessfully\nblobs=" + blobs
											+ "\nrefactorings=" + refactorings).getBytes());
						} catch (IOException | CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	Hashtable<String, String> printAccessibilityMetric(IJavaProject project, File folder, NullProgressMonitor monitor) {
		Hashtable<String, String> results = new Hashtable<String, String>();
		try {
			Analysis accessAnalysis = AnalysisFactory.analyzer(Arrays.asList(project), AnalysisMode.ACCESS_QUIET);
			accessAnalysis.run(monitor);
			ResultFormatter formatter = accessAnalysis.getResults().get(0).getFormatter();
			Files.write(new File(folder, "accessMetrics.txt").toPath(),
					("igat = " + formatter.igat() + " igam = " + formatter.igam()).getBytes());

			results.put("igat", formatter.igat());
			results.put("igam", formatter.igam());
		} catch (AnalysisException | IOException e) {
			e.printStackTrace();
		}
		return results;
	}

	void printBlobs(HBlobDetector blob, File folder) {
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

	protected Set<HDetector> detect(IJavaProject project, Hashtable<String, String> thresholds, File file,
			NullProgressMonitor monitor) throws NoConverterRegisteredException, CoreException {
		IPath project_location = project.getProject().getLocation();
		IPGConverter converter = GravityActivator.getDefault().getNewConverter(project.getProject());

		long start = System.currentTimeMillis();
		boolean success = converter.convertProject(project, Collections.emptySet(), monitor);
		long createPgInMS = System.currentTimeMillis() - start;
		if (!success || converter.getPG() == null) {
			fail("Creating PG from project failed: " + project.getProject().getName());
		}

		HAntiPatternDetection hulk = HulkFactory.eINSTANCE.createHAntiPatternResolving();
		HulkDetector hulkDetector = new HulkDetector(hulk, thresholds);
		Set<EClass> selection = new HashSet<>();
		selection.add(org.gravity.hulk.detection.antipattern.AntipatternPackage.eINSTANCE.getHBlobDetector());
		if (file != null) {
			try {
				Files.write(new File(file.getParentFile(), "timeHulkInMS.txt").toPath(),
						Long.toString(createPgInMS).getBytes());
				converter.getPG().eResource().save(new FileOutputStream(file), Collections.EMPTY_MAP);
			} catch (IOException e) {
				e.printStackTrace();
			}
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
