package org.point85.app.dashboard;

import org.point85.app.AppUtils;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.uom.UnitOfMeasure;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

public class ProductionEditorController extends EventEditorController {
	private OeeEvent productionEvent;

	private EquipmentMaterial equipmentMaterial;

	@FXML
	private RadioButton rbGood;

	@FXML
	private RadioButton rbReject;

	@FXML
	private RadioButton rbStartup;

	@FXML
	private TextField tfAmount;

	@FXML
	private Label lbUOM;

	public void initializeEditor(OeeEvent event) throws Exception {
		productionEvent = event;

		equipmentMaterial = null;
		rbGood.setSelected(false);
		rbReject.setSelected(false);
		rbStartup.setSelected(false);
		tfAmount.clear();
		tfAmount.setDisable(true);
		lbUOM.setText(null);

		// images for buttons
		setImages();

		getDialogStage().setOnShown((we) -> {
			try {
				displayAttributes();
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});
	}

	@Override
	protected void saveRecord() throws Exception {
		// time period
		setTimePeriod(productionEvent);

		// amount
		Double amount = AppUtils.stringToDouble(tfAmount.getText());

		if (amount == null || amount <= 0d) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.amount"));
		}
		productionEvent.setAmount(amount);
		productionEvent.setInputValue(String.valueOf(amount));

		PersistenceService.instance().save(productionEvent);
	}

	private EquipmentMaterial getEquipmentMaterial() throws Exception {
		// get from equipment material
		Equipment equipment = productionEvent.getEquipment();
		OeeEvent lastSetup = PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.MATL_CHANGE);

		Material material = null;
		if (lastSetup == null) {
			material = equipment.getDefaultEquipmentMaterial().getMaterial();
		} else {
			material = lastSetup.getMaterial();
		}

		if (material == null) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.material", equipment.getName()));
		}

		equipmentMaterial = equipment.getEquipmentMaterial(material);

		if (equipmentMaterial == null) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.design.speed", equipment.getName(),
					material.getDisplayString()));
		}
		return equipmentMaterial;
	}

	void displayAttributes() throws Exception {
		// start date and time
		super.displayAttributes(productionEvent);

		// amount
		if (productionEvent.getAmount() != null) {
			tfAmount.setText(Double.toString(productionEvent.getAmount()));
		}

		// UOM
		UnitOfMeasure uom = productionEvent.getUOM();
		if (uom != null) {
			lbUOM.setText(productionEvent.getUOM().getSymbol());
		}
	}

	@FXML
	private void onSelectProductionType() {
		try {
			UnitOfMeasure uom = null;
			OeeEventType type = null;
			if (rbGood.isSelected()) {
				// good production
				uom = getEquipmentMaterial().getRunRateUOM().getDividend();
				type = OeeEventType.PROD_GOOD;
			} else if (rbReject.isSelected()) {
				// reject or rework production
				uom = getEquipmentMaterial().getRejectUOM();
				type = OeeEventType.PROD_REJECT;
			} else {
				// startup loss
				uom = getEquipmentMaterial().getRunRateUOM().getDividend();
				type = OeeEventType.PROD_STARTUP;
			}
			productionEvent.setUOM(uom);
			productionEvent.setEventType(type);
			productionEvent.setMaterial(getEquipmentMaterial().getMaterial());

			tfAmount.setDisable(false);
			lbUOM.setText(uom.getSymbol());
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
