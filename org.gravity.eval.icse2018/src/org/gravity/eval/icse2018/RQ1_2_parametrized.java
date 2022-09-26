package org.gravity.eval.icse2018;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RQ1_2_parametrized {

	private static final String AP_SUFFIX = "blob";

	private static final boolean ENABLE_METRIC_RECORDING = true;

	// @Rule
	// public ToFileLogger logToFile = new ToFileLogger(Paths.get(new
	// File("logs").toURI())); //$NON-NLS-1$

	private IJavaProject project;

	private IJavaProject java_project_copy;

	public RQ1_2_parametrized(IJavaProject project) {
		this.project = project;
	}

	@Parameters(name = "{index}: ICSE2018 Eval RQ1+Q2: {0}")
	public static Collection<Object[]> data() {
		List<Object[]> testcases = new ArrayList<>();

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

		for (IProject test : projects) {
			if (!test.exists() || !test.isOpen()) {
				continue;
			}
			try {
				if (test.getNature(JavaCore.NATURE_ID) != null) {
					if (test.getName().endsWith("_tmp_" + AP_SUFFIX)) {
						test.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT,
								new NullProgressMonitor());
					} else {
						testcases.add(new Object[] { JavaCore.create(test) });
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return testcases;
	}

	@Test
	public void resolveBlob() throws Exception {
		new Eval().run(project, AP_SUFFIX, ENABLE_METRIC_RECORDING);
	}

	@After
	public void after() {
		try {
			if (java_project_copy != null && java_project_copy.exists()) {
				java_project_copy.getProject().delete(false, true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
