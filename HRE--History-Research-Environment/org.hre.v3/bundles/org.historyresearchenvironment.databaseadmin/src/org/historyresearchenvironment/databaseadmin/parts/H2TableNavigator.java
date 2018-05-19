package org.historyresearchenvironment.databaseadmin.parts;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.h2.tools.Csv;
import org.h2.tools.SimpleResultSet;
import org.historyresearchenvironment.databaseadmin.HreDbadminConstants;
import org.historyresearchenvironment.databaseadmin.models.H2TableModel;
import org.historyresearchenvironment.databaseadmin.providers.H2TableProvider;

/**
 * Create a view part with a table. Create a column for each columns in the
 * catalog for the given table. Populate the table with data from H2.
 * 
 * @version 2018-05-20
 * @author Michael Erichsen, &copy; History Research Environment Ltd., 2018
 *
 */

public class H2TableNavigator {
	private static TableViewer tableViewer;
	@Inject
	private EPartService partService;
	@Inject
	private EModelService modelService;
	@Inject
	private MApplication application;
	@Inject
	private IEventBroker eventBroker;
	@Inject
	private ECommandService commandService;
	@Inject
	private EHandlerService handlerService;

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private Table table;
	private Composite parent;
	private String tableName;

	/**
	 * Constructor
	 *
	 */
	public H2TableNavigator() {
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parentC Shell
	 */
	@PostConstruct
	public void createControls(Composite parentC) {
		this.parent = parentC;

		tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		table = tableViewer.getTable();
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				String recordNum = "0";

				// Open an editor
				final ParameterizedCommand command = commandService.createCommand(
						"org.historyresearchenvironment.databaseadmin.v010.command.opentableeditorcommand", null);
				handlerService.executeHandler(command);
				LOGGER.info("Navigator opened editor");

				eventBroker.post(
						org.historyresearchenvironment.databaseadmin.HreDbadminConstants.TABLENAME_UPDATE_TOPIC,
						tableName);
				LOGGER.info("Navigator posted tablename " + tableName);

				final TableItem[] selectedRows = table.getSelection();

				if (selectedRows.length > 0) {
					final TableItem selectedRow = selectedRows[0];
					recordNum = selectedRow.getText(0);
				}

				eventBroker.post(
						org.historyresearchenvironment.databaseadmin.HreDbadminConstants.RECORDNUM_UPDATE_TOPIC,
						recordNum);
				LOGGER.info("Navigator posted record number " + recordNum);
				eventBroker.post("MESSAGE", tableName + " editor has been opened");
			}
		});
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		final Composite compositeButtons = new Composite(parent, SWT.NONE);
		compositeButtons.setLayout(new RowLayout(SWT.HORIZONTAL));

		final Button btnImport = new Button(compositeButtons, SWT.NONE);
		btnImport.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events
			 * .SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				final FileDialog dialog = new FileDialog(btnImport.getShell(), SWT.OPEN);
				final String[] extensions = { "*.csv", "*.*" };
				dialog.setFilterExtensions(extensions);
				dialog.open();

				final String shortName = dialog.getFileName();
				final String fileName = dialog.getFilterPath() + "/" + shortName;

				if (fileName != null) {
					final H2TableProvider provider = new H2TableProvider(tableName);
					final int rowCount = provider.importCsv(fileName);

					eventBroker.post("MESSAGE", rowCount + " rows has been imported from " + fileName);
				}

				eventBroker.post(HreDbadminConstants.DATABASE_UPDATE_TOPIC, "Dummy");
				updateGui();
			}
		});
		btnImport.setText("Import Table...");

		final Button btnExport = new Button(compositeButtons, SWT.NONE);
		btnExport.addSelectionListener(new SelectionAdapter() {

			/**
			 * @param fileName
			 * @param tableName
			 */
			private void exportCsv(String fileName, String tableName) {
				final H2TableProvider provider = new H2TableProvider(tableName);
				final List<H2TableModel> modelList = provider.getModelList();
				final List<List<Object>> rows = provider.selectAll();

				final SimpleResultSet rs = new SimpleResultSet();

				for (int i = 0; i < provider.getCount(); i++) {
					switch (modelList.get(i).getNumericType()) {
					case HreDbadminConstants.DOUBLE:
					case HreDbadminConstants.VARBINARY:
					case HreDbadminConstants.SMALLINT:
					case HreDbadminConstants.INTEGER:
					case HreDbadminConstants.BIGINT:
						rs.addColumn(modelList.get(i).getName(), modelList.get(i).getNumericType(),
								modelList.get(i).getPrecision(), modelList.get(i).getScale());
						break;
					case HreDbadminConstants.VARCHAR:
						rs.addColumn(modelList.get(i).getName(), modelList.get(i).getNumericType(), 0,
								modelList.get(i).getScale());
						break;
					default:
						rs.addColumn(modelList.get(i).getName(), modelList.get(i).getNumericType(), 0, 0);
						break;
					}
				}

				Object[] oa;

				for (int i = 0; i < rows.size(); i++) {
					oa = new Object[provider.getCount()];
					for (int j = 0; j < oa.length; j++) {
						oa[j] = rows.get(i).get(j);
					}

					rs.addRow(oa);
				}

				final Csv csvFile = new Csv();
				csvFile.setFieldSeparatorWrite(";");
				try {
					csvFile.write(fileName, rs, "UTF-8");
					eventBroker.post("MESSAGE", "Table " + tableName + " has been exported to " + fileName);
				} catch (final SQLException e) {
					eventBroker.post("MESSAGE", e.getMessage());
					e.printStackTrace();
					LOGGER.severe(e.getMessage());
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events
			 * .SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				final FileDialog dialog = new FileDialog(btnImport.getShell(), SWT.SAVE);
				final String[] extensions = { "*.csv", "*.*" };
				dialog.setFilterExtensions(extensions);
				dialog.open();

				final String shortName = dialog.getFileName();
				final String fileName = dialog.getFilterPath() + "\\" + shortName;

				if (fileName != null) {
					exportCsv(fileName, tableName);
				}
			}
		});
		btnExport.setText("Export Table...");

		final Button btnEmptyTable = new Button(compositeButtons, SWT.NONE);
		btnEmptyTable.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events
			 * .SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				final H2TableProvider provider = new H2TableProvider(tableName);
				provider.deleteAll();

				eventBroker.post(HreDbadminConstants.DATABASE_UPDATE_TOPIC, "Dummy");
				eventBroker.post("MESSAGE", "All rows have been deleted from " + tableName);
				updateGui();
			}
		});
		btnEmptyTable.setText("Empty Table");

		final Button btnClose = new Button(compositeButtons, SWT.NONE);
		btnClose.addSelectionListener(new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events
			 * .SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				final List<MPartStack> stacks = modelService.findElements(application, null, MPartStack.class, null);
				final MPart part = (MPart) stacks.get(stacks.size() - 1).getSelectedElement();
				partService.hidePart(part, true);
			}

		});
		btnClose.setText("Close");

		updateGui();
	}

	/**
	 * 
	 */
	@PreDestroy
	public void dispose() {
	}

	/**
	 * 
	 */
	@Focus
	public void setFocus() {
	}

	/**
	 * @param tableName
	 */
	@Inject
	@Optional
	private void subscribeNameUpdateTopic(
			@UIEventTopic(org.historyresearchenvironment.databaseadmin.HreDbadminConstants.TABLENAME_UPDATE_TOPIC) String tableName) {
		this.tableName = tableName;
		final List<MPartStack> stacks = modelService.findElements(application, null, MPartStack.class, null);
		final MPart part = (MPart) stacks.get(stacks.size() - 1).getSelectedElement();
		part.setLabel(tableName);

		updateGui();
	}

	/**
	 * 
	 */
	private void updateGui() {
		if ((tableName == null) || (tableName == "")) {
			return;
		}

		final H2TableProvider provider = new H2TableProvider(tableName);

		final int count = provider.getCount();
		parent.setLayout(new GridLayout());

		if (table.getColumnCount() == 0) {
			final TableViewerColumn[] tvc = new TableViewerColumn[count];
			final TableColumn[] tc = new TableColumn[count];

			for (int i = 0; i < count; i++) {
				tvc[i] = new TableViewerColumn(tableViewer, SWT.NONE);
				tc[i] = tvc[i].getColumn();
				tc[i].setWidth(100);
				tc[i].setText(provider.getModelList().get(i).getName());
			}
		}

		final List<List<Object>> rowList = provider.selectAll();

		table.removeAll();

		for (int i = 0; i < rowList.size(); i++) {
			final TableItem item = new TableItem(table, SWT.NONE);
			final List<Object> row = rowList.get(i);
			for (int j = 0; j < row.size(); j++) {
				if (row.get(j) != null) {
					item.setText(j, (String) row.get(j));
				} else {
					item.setText(j, "");
				}
			}
		}
	}
}