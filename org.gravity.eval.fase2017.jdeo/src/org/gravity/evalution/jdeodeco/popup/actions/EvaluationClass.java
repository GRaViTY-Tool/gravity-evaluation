package org.gravity.evalution.jdeodeco.popup.actions;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.print.CancelablePrintJob;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import gr.uom.java.distance.Entity;
import gr.uom.java.distance.ExtractClassCandidateGroup;
import gr.uom.java.distance.ExtractClassCandidateRefactoring;

public class EvaluationClass implements IObjectActionDelegate {

	private Shell shell;
	
	/**
	 * Constructor for Action1.
	 */
	public EvaluationClass() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}
	
	
	/**
	 * Gets selected project from workspace
	 * @return
	 */
	private Set<IJavaProject> getSelectedProject() {
		Set<IJavaProject> javaProject = null;
			
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();

		if (selection instanceof IStructuredSelection) {
			javaProject = new HashSet<>();
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			for(Object element: ((IStructuredSelection) selection).toArray()){
				if(element instanceof IJavaProject) {
					javaProject.add((IJavaProject)element);
				}
			}
		}
		return javaProject;
	}


	/**
	 * 
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
	 
		// Get the currently selected projects
		for(IJavaProject sProject : getSelectedProject()){
			try {
				String projName = sProject.getProject().getName();
			
				// Set up time
				System.out.println("Start time measurement for project " +	projName + ".");
				long timePrev = System.currentTimeMillis();
				
				ExtractClassCandidateGroup[] candidateRefactoringTable = EvaluateGodClass.detect(sProject);
				
				// End time measurement
				long timeEnd = System.currentTimeMillis();
				String string = "Time taken for JDeodorant check for project " +	projName + " : " + (timeEnd - timePrev) + "ms";
				System.out.println(string);
				System.out.println("");

				List<Entity> extract = null;
				if(candidateRefactoringTable!=null){
				double min = Double.MAX_VALUE;
				for(ExtractClassCandidateGroup eccg : candidateRefactoringTable){
					ArrayList<ExtractClassCandidateRefactoring> candidates = eccg.getCandidates();
					for(ExtractClassCandidateRefactoring c : candidates){
						if(min > c.getEntityPlacement()){
							min = c.getEntityPlacement();
							extract = c.getExtractedEntities();
						}
					}
				}
				}
				IFolder gravity = sProject.getProject().getFolder("gravity");
				if(!gravity.exists()){
					try {
						gravity.create(true, true, new NullProgressMonitor());
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
				IFile jdeo = gravity.getFile("jdeo"+System.currentTimeMillis()+".txt");
				try {
					if(extract!=null){
						string += "\n Extract "+extract.size()+" entities: "+extract;
					}
					else{
						string += "\n Extract is null";
					}
					jdeo.create(new ByteArrayInputStream(string.getBytes()), true, new NullProgressMonitor());
				} catch (CoreException e) {
					e.printStackTrace();
				}
				
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}		
		
	}
	
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
