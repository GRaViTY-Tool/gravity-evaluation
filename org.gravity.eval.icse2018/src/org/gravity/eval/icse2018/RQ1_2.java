package org.gravity.eval.icse2018;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;

public class RQ1_2 {

	private static final String AP_SUFFIX = "blob";

	private static final boolean ENABLE_METRIC_RECORDING = true;

	@Test
	public void resolveBlob() throws FileNotFoundException, IOException, CoreException {
		String env = System.getenv("project");	
		if(env == null || env.trim().length() == 0){
			throw new RuntimeException("variable 'project' hasn't been set.");
		}
		IProject iProject = ResourcesPlugin.getWorkspace().getRoot().getProject(env);
		if(iProject == null || !iProject.exists()){
			throw new RuntimeException("The project '"+env+"' doesn't exist.");
		}
		if (iProject.getNature(JavaCore.NATURE_ID) != null) {
			IJavaProject project = JavaCore.create(iProject);
			new Eval().run(project, AP_SUFFIX, ENABLE_METRIC_RECORDING);
		}
		else{
			throw new RuntimeException("Project is no Java project.");
		}
	}
}
