package com.leavemanagement.client.view.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialTabItem;

public class MaterialTab extends Composite {

	private static MaterialTabUiBinder uiBinder = GWT.create(MaterialTabUiBinder.class);

	interface MaterialTabUiBinder extends UiBinder<Widget, MaterialTab> {
	}

	public MaterialTab() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@UiField
	MaterialColumn tab1;

	@UiField
	MaterialColumn tab2;

	@UiField
	MaterialColumn tab3;

	@UiField
	MaterialColumn tab4;

	@UiField
	MaterialColumn tab5;

	@UiField
	MaterialColumn tab6;

	@UiField
	MaterialColumn tab7;

	@UiField
	MaterialColumn tab8;

	@UiField
	MaterialColumn tab9;

	@UiField
	MaterialTabItem tabTimeReport;

	@UiField
	MaterialTabItem tabPendingLeave;

	@UiField
	MaterialTabItem tabEmployeeLeave;
	@UiField
	MaterialTabItem tabLeaveHistory;

	public MaterialColumn getTab1() {
		return tab1;
	}

	public MaterialColumn getTab2() {
		return tab2;
	}

	public MaterialColumn getTab3() {
		return tab3;
	}

	public MaterialColumn getTab4() {
		return tab4;
	}

	public MaterialColumn getTab5() {
		return tab5;
	}

	public MaterialColumn getTab6() {
		return tab6;
	}

	public MaterialColumn getTab7() {
		return tab7;
	}

	public MaterialColumn getTab8() {
		return tab8;
	}

	public MaterialTabItem getTabTimeReport() {
		return tabTimeReport;
	}

	public MaterialTabItem getTabPendingLeave() {
		return tabPendingLeave;
	}

	public MaterialTabItem getTabEmployeeLeave() {
		return tabEmployeeLeave;
	}

	public MaterialTabItem getTabLeaveHistory() {
		return tabLeaveHistory;
	}

	public MaterialColumn getTab9() {
		return tab9;
	}

	public void setTab9(MaterialColumn tab9) {
		this.tab9 = tab9;
	}

}
