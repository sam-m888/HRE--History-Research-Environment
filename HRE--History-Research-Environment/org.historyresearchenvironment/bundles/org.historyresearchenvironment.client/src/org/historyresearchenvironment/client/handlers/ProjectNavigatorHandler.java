
package org.historyresearchenvironment.client.handlers;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

/**
 * Handler to open the project navigator.
 * 
 * @version 2018-07-15
 * @author Michael Erichsen, &copy; History Research Environment Ltd., 2018
 *
 */
public class ProjectNavigatorHandler {
	private final static String contributionURI = "bundleclass://org.historyresearchenvironment.client/org.historyresearchenvironment.client.parts.ProjectNavigator";

	/**
	 * @param partService
	 * @param application
	 * @param modelService
	 */
	@Execute
	public void execute(EPartService partService, MApplication application, EModelService modelService) {
		final List<MPartStack> stacks = modelService.findElements(application, null, MPartStack.class, null);
		MPart part = MBasicFactory.INSTANCE.createPart();

		for (final MPartStack mPartStack : stacks) {
			final List<MStackElement> a = mPartStack.getChildren();

			for (int i = 0; i < a.size(); i++) {
				part = (MPart) a.get(i);
				if (part.getContributionURI().equals(contributionURI)) {
					partService.showPart(part, PartState.ACTIVATE);
					return;
				}
			}
		}

		part.setLabel("Projects");
		part.setContainerData("650");
		part.setCloseable(true);
		part.setVisible(true);
		part.setContributionURI(contributionURI);
		stacks.get(0).getChildren().add(part);
		partService.showPart(part, PartState.ACTIVATE);
	}

}