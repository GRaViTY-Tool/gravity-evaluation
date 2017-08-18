package org.gravity.eval.icse2018;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.detection.antipattern.HBlobDetector;
import org.junit.Test;

public class RQ1_2_Continue {

	@Test
	public void calculateMissing() {
		File base = new File("./");
		try {
			Files.walk(base.toPath()).filter(path -> {
				return !Files.isDirectory(path) && "FAIL".equals(path.getFileName().toString());
			}).forEach(path -> {

				int blobs = -1, refactorings = -1;
				try {
					for (String line : Files.readAllLines(path)) {
						if (line.startsWith("blobs=")) {
							blobs = Integer.valueOf(line.substring("blobs=".length()));
						} else if (line.startsWith("refactorings=")) {
							refactorings = Integer.valueOf(line.substring("refactorings=".length()));
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Path runFolder = path.getParent();
				Path timestampFolder = runFolder.getParent();
				Path projectFolder = timestampFolder.getParent();
				String env = projectFolder.getFileName().toString() + "_" + timestampFolder.getFileName().toString()
						+ "_" + runFolder.getFileName().toString();
				IProject iProject = ResourcesPlugin.getWorkspace().getRoot().getProject(env);
				if (iProject == null || !iProject.exists()) {
					throw new RuntimeException("The project '" + env + "' doesn't exist.");
				}
				if (!iProject.getFile("TODO.txt").exists())
					try {
						if (iProject.getNature(JavaCore.NATURE_ID) != null) {
							IJavaProject project = JavaCore.create(iProject);
							try {
								path.toFile().delete();
								if (IMarker.SEVERITY_ERROR == project.getUnderlyingResource().findMaxProblemSeverity(
										IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)
										|| IMarker.SEVERITY_ERROR == project.getUnderlyingResource()
												.findMaxProblemSeverity(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER,
														true, IResource.DEPTH_INFINITE)) {

									try {
										Files.write(new File(runFolder.toFile(), "FAIL").toPath(),
												"The java project has build errors".getBytes());
									} catch (IOException e) {
										e.printStackTrace();
									}
								} else {
									Set<EClass> secondSelection = new HashSet<>();
									secondSelection
											.add(org.gravity.hulk.detection.antipattern.AntipatternPackage.eINSTANCE
													.getHBlobDetector());
									Hashtable<String, String> hulkConfigTable = new Hashtable<String, String>();

									for (String line : Files.readAllLines(
											new File(projectFolder.toFile(), "initial/hulkConfig.csv").toPath(),
											Charset.defaultCharset())) {
										String[] entries = line.split(" ");
										if (entries.length != 2) {
											throw new RuntimeException("invalid threshold csv");
										}
										hulkConfigTable.put(entries[0], entries[1]);
									}

									Eval eval = new Eval();
									Set<HDetector> executed_detectors = eval.detect(project, hulkConfigTable,
											new File(runFolder.toFile(), "pg.xmi"), new NullProgressMonitor());

									HBlobDetector blob = null;
									for (HDetector detector : executed_detectors) {
										if (detector instanceof HBlobDetector) {
											blob = (HBlobDetector) detector;
										}
									}

									Hashtable<String, String> sourcemeter = eval.printSourcemeterMetrics(project,
											runFolder.toFile());
									eval.printBlobs(blob, runFolder.toFile());
									Hashtable<String, String> accessibility = eval.printAccessibilityMetric(project,
											runFolder.toFile(), new NullProgressMonitor());

									for (Resource r : blob.eResource().getResourceSet().getResources()) {
										r.unload();

									}

									try (PrintWriter printer = new PrintWriter(
											new File(runFolder.toFile(), "stats.csv"))) {
										printer.print(project.getProject().getName());
										printer.print(' ');
										if (refactorings > 0) {
											printer.print(Integer.toString(refactorings));
										} else {
											printer.print("X");
										}
										printer.print(' ');
										printer.print(sourcemeter.get("lcom"));
										printer.print(' ');
										printer.print(sourcemeter.get("cbo"));
										printer.print(' ');
										if (blobs > 0) {
											printer.print(Integer.toString(blobs - blob.getHAnnotation().size()));
										} else {
											printer.print("X-" + Integer.toString(blob.getHAnnotation().size()));
										}
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
					} catch (CoreException | IOException e) {
						e.printStackTrace();
					}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
