package org.historyresearchenvironment.client.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Handler to shut down HRE.
 * 
 * @version 2018-06-22
 * @author Michael Erichsen, &copy; History Research Environment Ltd., 2018
 *
 */
public class QuitHandler {
	/**
	 * @param workbench
	 * @param shell
	 */
	@Execute
	public void execute(IWorkbench workbench, Shell shell) {
		if (MessageDialog.openConfirm(shell, "Confirmation", "Do you want to exit?")) {
			workbench.close();
		}
	}
}
