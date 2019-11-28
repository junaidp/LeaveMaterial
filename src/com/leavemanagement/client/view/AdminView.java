package com.leavemanagement.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.leavemanagement.client.presenter.AdminPresenter.Display;
import com.leavemanagement.client.view.widget.MaterialTab;
import com.leavemanagement.shared.User;

import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialRow;

public class AdminView extends MaterialColumn implements IsWidget, Display {

	private MaterialColumn vpnlContainer = new MaterialColumn();
	private Anchor logOff = new Anchor("Log off");
	private MaterialButton btnBack = new MaterialButton("Back");
	private Anchor addUser = new Anchor("Add User");
	private Anchor changePassword = new Anchor("Change Password");
	private Anchor addCompany = new Anchor("Add Company");
	private Anchor UserView = new Anchor("User View");

	private MaterialColumn vpnlLeaveRecords = new MaterialColumn();
	private MaterialColumn vpnlEMployeeLeavesRecord = new MaterialColumn();
	private MaterialColumn vpnlJobCreation = new MaterialColumn();
	private MaterialColumn vpnlJobPlanning = new MaterialColumn();
	private JobCreationView jobCreationView;

	public AdminView(User loggedInUser) {
		jobCreationView = new JobCreationView(null, loggedInUser, null);
		addCompany.setVisible(false);
		// UserView.setVisible(false);
		MaterialRow hpnlWelcome = new MaterialRow();
		hpnlWelcome.setWidth("100%");
		MaterialRow hpnlSpace = new MaterialRow();
		hpnlSpace.setWidth("80%");
		hpnlWelcome.add(hpnlSpace);
		hpnlWelcome.add(logOff);
		MaterialRow hpnlHeader = new MaterialRow();
		hpnlHeader.setWidth("100%");
		setWidth("100%");
		Label lblHeader = new Label("People Management System");
		lblHeader.setStyleName("headerSignin");
		hpnlHeader.setStyleName("headerSignin");
		hpnlHeader.add(lblHeader);
		hpnlHeader.add(hpnlWelcome);

		// add(hpnlHeader);
		MaterialColumn vpnlLeave = new MaterialColumn();
		vpnlLeave.setWidth("100%");
		// add(vpnlLeave);
		MaterialRow rowAddUser = new MaterialRow();
		rowAddUser.add(addUser);
		vpnlLeave.add(rowAddUser);
		MaterialRow rowChangePAssword = new MaterialRow();
		rowChangePAssword.add(changePassword);
		vpnlLeave.add(rowChangePAssword);
		MaterialRow rowAddCompany = new MaterialRow();
		rowAddCompany.add(addCompany);
		vpnlLeave.add(rowAddCompany);
		MaterialRow rowUser = new MaterialRow();
		rowUser.add(UserView);
		vpnlLeave.add(rowUser);
		/*
		 * TabPanel tabPanel = new TabPanel(); tabPanel.add(vpnlLeaveRecords ,
		 * "Pending Leave Requests"); tabPanel.add(vpnlContainer,
		 * "Leaves History"); tabPanel.add(vpnlEMployeeLeavesRecord,
		 * "e Record"); tabPanel.add(jobCreationView, "Job Creation");
		 * tabPanel.add(new AttributesView(loggedInUser), "Attributes");
		 * tabPanel.add(new RatingView(loggedInUser), "Rating");
		 * tabPanel.add(new TimeSheetView(loggedInUser), "Time Sheet");
		 * if(loggedInUser.getReportingTo()==5){ tabPanel.add(new
		 * TimeSheetReportView(loggedInUser), "Time Report"); }
		 */

		MaterialTab tab = new MaterialTab();
		add(new HTML("&nbsp"));
		add(tab);

		tab.getTab1().add(new HTML("&nbsp"));
		tab.getTab1().add(vpnlLeaveRecords);
		tab.getTab2().add(new HTML("&nbsp"));
		tab.getTab2().add(vpnlContainer);
		tab.getTab3().add(new HTML("&nbsp"));
		tab.getTab3().add(vpnlEMployeeLeavesRecord);
		tab.getTab4().add(new HTML("&nbsp"));
		tab.getTab4().add(jobCreationView);
		tab.getTab5().add(new HTML("&nbsp"));
		tab.getTab5().add(new AttributesView(loggedInUser));
		tab.getTab6().add(new HTML("&nbsp"));
		tab.getTab6().add(new RatingView(loggedInUser));
		tab.getTab7().add(new HTML("&nbsp"));
		// tab.getTab7().add(new TimeSheetView(loggedInUser));
		tab.getTab7().add(new TimeSheetTree(loggedInUser));
		tab.getTab8().add(new HTML("&nbsp"));
		tab.getTab8().add(new TimeSheetReportView(loggedInUser));
		tab.getTab9().add(new TimeSheetReportView(loggedInUser));

		if (loggedInUser.getRoleId().getRoleId() == 5) {
			tab.getTabTimeReport().setVisible(true);
			tab.getTabLeaveHistory().setVisible(true);
			tab.getTabEmployeeLeave().setVisible(true);
		} else {
			// 2:14 checking leave history
			tab.getTabTimeReport().setVisible(false);
			tab.getTabLeaveHistory().setVisible(false);
			tab.getTabEmployeeLeave().setVisible(false);

		}

		logOff.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				History.newItem("login");
			}
		});
	}

	public MaterialColumn getVpnlContainer() {
		return vpnlContainer;
	}

	public void setVpnlContainer(MaterialColumn vpnlContainer) {
		this.vpnlContainer = vpnlContainer;
	}

	public MaterialButton getBtnBack() {
		return btnBack;
	}

	public void setBtnBack(MaterialButton btnBack) {
		this.btnBack = btnBack;
	}

	public Anchor getAddUser() {
		return addUser;
	}

	public void setAddUser(Anchor addUser) {
		this.addUser = addUser;
	}

	public MaterialColumn getVpnlLeaveRecords() {
		return vpnlLeaveRecords;
	}

	public void setVpnlLeaveRecords(MaterialColumn vpnlLeaveRecords) {
		this.vpnlLeaveRecords = vpnlLeaveRecords;
	}

	public MaterialColumn getVpnlEMployeeLeavesRecord() {
		return vpnlEMployeeLeavesRecord;
	}

	public void setVpnlEMployeeLeavesRecord(MaterialColumn vpnlEMployeeLeavesRecord) {
		this.vpnlEMployeeLeavesRecord = vpnlEMployeeLeavesRecord;
	}

	public Anchor getChangePassword() {
		return changePassword;
	}

	public void setChangePassword(Anchor changePassword) {
		this.changePassword = changePassword;
	}

	public Anchor getAddCompany() {
		return addCompany;
	}

	public void setAddCompany(Anchor addCompany) {
		this.addCompany = addCompany;
	}

	public MaterialColumn getVpnlJobCreation() {
		return vpnlJobCreation;
	}

	public void setVpnlJobCreation(MaterialColumn vpnlJobCreation) {
		this.vpnlJobCreation = vpnlJobCreation;
	}

	public JobCreationView getJobCreationView() {
		return jobCreationView;
	}

	public void setJobCreationView(JobCreationView jobCreationView) {
		this.jobCreationView = jobCreationView;
	}

}
