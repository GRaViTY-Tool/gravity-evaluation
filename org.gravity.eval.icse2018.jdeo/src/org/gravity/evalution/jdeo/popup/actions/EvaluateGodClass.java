package org.gravity.evalution.jdeo.popup.actions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jdt.core.IJavaProject;

import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.distance.ExtractClassCandidateGroup;
import gr.uom.java.jdeodorant.refactoring.views.GodClass;


public class EvaluateGodClass {

	public static ExtractClassCandidateGroup[] detect(IJavaProject sProject)
			throws NoSuchFieldException {
		GodClass viewGodClass = new GodClass();
		
		
		// Set selectedProject and activeProject in the GodClass by Reflection
		Field selProject = viewGodClass.getClass().getDeclaredField("selectedProject");
		selProject.setAccessible(true);
		try {
			selProject.set(viewGodClass, sProject);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		
		Field actProject = viewGodClass.getClass().getDeclaredField("activeProject");
		actProject.setAccessible(true);
		try {
			actProject.set(viewGodClass, sProject);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		
		
		
		
		// Clear Cache
		CompilationUnitCache.getInstance().clearCache();
		
		
		// Probably not needed
		ExtractClassCandidateGroup[] candidateRefactoringTable;
		
		
		// Execute the search for code smells and refactoring options by getTable() via reflection
		Method getTable;
		try {
			getTable = viewGodClass.getClass().getDeclaredMethod("getTable");
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
		getTable.setAccessible(true);
		Object invoke;
		try {
			invoke = getTable.invoke(viewGodClass);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
		if(invoke == null){
			return null;
		}
		candidateRefactoringTable = (ExtractClassCandidateGroup[]) invoke;
		return candidateRefactoringTable;
	}
}