package com.leavemanagement.client.view;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import gwt.material.design.client.ui.MaterialRow;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import gwt.material.design.client.ui.MaterialColumn;
import com.leavemanagement.client.GreetingService;
import com.leavemanagement.client.GreetingServiceAsync;
import com.leavemanagement.shared.Job;
import com.leavemanagement.shared.TimeSheet;
import com.leavemanagement.shared.User;

public class TimeSheetView extends MaterialColumn{
	
	private GreetingServiceAsync rpcService = GWT.create(GreetingService.class);
	private User loggedInUser =null;
	private ListBox listMonth = new ListBox();
	FlexTable flex = new FlexTable();
	private int selectedMonth = 0;
	private Button btnSave = new Button("Save");
	private Button btnSubmit = new Button("Submit");
	
	public TimeSheetView(User loggedInUser){
		this.loggedInUser = loggedInUser;
		MaterialRow hpnl = new MaterialRow();
		Label lblJob = new Label("Job Name");
//		hpnl.add(lblJob);
		hpnl.add(flex);
		add(listMonth);
		add(hpnl);
		MaterialRow hpnlButtons = new MaterialRow();
		hpnlButtons.add(btnSave);
		hpnlButtons.add(btnSubmit);
		add(hpnlButtons);
		listMonth.addItem("Select Month", "0");
		listMonth.addItem("Jan", "1");
		listMonth.addItem("Feb", "2");
		listMonth.addItem("Mar", "3");
		listMonth.addItem("Apr", "4");
		listMonth.addItem("May", "5");
		listMonth.addItem("Jun", "6");
		listMonth.addItem("Jul", "7");
		listMonth.addItem("Aug", "8");
		listMonth.addItem("Sep", "9");
		listMonth.addItem("Oct", "10");
		listMonth.addItem("Nev", "11");
		listMonth.addItem("Dec", "12");
		
		btnSubmit.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				submitTimeSheet();
			}
		});
		
		btnSave.addClickHandler(new ClickHandler(){

			@Override
			public void onClick(ClickEvent event) {
				saveTimeSheet();
			}

			});
		
		listMonth.addChangeHandler(new ChangeHandler(){

			@Override
			public void onChange(ChangeEvent event) {
				selectedMonth = Integer.parseInt(listMonth.getValue(listMonth.getSelectedIndex()));
				fetchJobs();
			}});
	}
	
	public void fetchJobs(){
		flex.setWidget(0, 0, new Label("job"));
		for(int k=0; k<31;k++){
			Label heading = new Label(k+1+"");
			heading.setStyleName("bluebg");
			flex.setWidget(0, k+1, heading);
			flex.getFlexCellFormatter().setHorizontalAlignment(0, k+1, HasHorizontalAlignment.ALIGN_CENTER);
			
		}
		   rpcService.fetchJobsForTimeSheet(loggedInUser, new AsyncCallback<ArrayList<Job>>() {
			
			@Override
			public void onSuccess(ArrayList<Job> jobs) {
				
				for(int i=0; i< jobs.size(); i++){
					Job job = jobs.get(i);
					Label lblName =  new Label(job.getJobName());
					lblName.setWordWrap(false);
					flex.setWidget(i+1, 0, lblName);
					
					for(int j=0; j<31;j++){
						TextBox text = new TextBox();
						text.setWidth("30px");
						flex.setWidget(i+1, j+1, text);
						for(int m=0; m< job.getTimeSheets().size(); m++){
							TimeSheet timeSheet =  job.getTimeSheets().get(m);
							if(timeSheet.getMonth() == selectedMonth && timeSheet.getDay() == j+1){
								text.setText(timeSheet.getHours()+"");
							}
						}
					}
					Label lblJobId = new Label();
					lblJobId.setText(job.getJobId()+"");
					lblJobId.setStyleName("white");
					flex.setWidget(i+1, 32, lblJobId);
					
				}
			}
			
			

			@Override
			public void onFailure(Throwable caught) {
				Window.alert("fetch jobs list failed");
			}
		});
	}
	
	private void submitTimeSheet() {
		float totalHours =0;
		ArrayList<TimeSheet> timeSheetList = new ArrayList<TimeSheet>();
		for(int i=0; i< flex.getRowCount()-1; i++){
			for(int j=0;j<31; j++){
				TextBox text = (TextBox) flex.getWidget(i+1, j+1);
				text.setWidth("30px");
				if(text.getText()!=null && !text.getText().equals("")){
					TimeSheet timeSheet = new TimeSheet(); 
					timeSheet.setDay(j+1);
					timeSheet.setHours(Float.parseFloat(text.getText()));
					totalHours =totalHours +timeSheet.getHours();
					timeSheet.setMonth(selectedMonth);
					Label jobField = (Label) flex.getWidget(i+1, 32);
					Job job = new Job();
					job.setJobId(Integer.parseInt(jobField.getText()));
					timeSheet.setJobId(job);
					User user = new User();
					user.setUserId(loggedInUser.getUserId());
					timeSheet.setUserId(user);
					timeSheet.setStatus(1);
					timeSheetList.add(timeSheet);
					
				}
			}
			
		}
//		if(totalHours> )
		getMonthAllowedHours(totalHours, timeSheetList.get(0).getMonth(), timeSheetList);
		
	}
	
	private void saveTimeSheet() {
		float totalHours =0;
		ArrayList<TimeSheet> timeSheetList = new ArrayList<TimeSheet>();
		for(int i=0; i< flex.getRowCount()-1; i++){
			for(int j=0;j<31; j++){
				TextBox text = (TextBox) flex.getWidget(i+1, j+1);
				text.setWidth("30px");
				if(text.getText()!=null && !text.getText().equals("")){
					TimeSheet timeSheet = new TimeSheet(); 
					timeSheet.setDay(j+1);
					timeSheet.setHours(Float.parseFloat(text.getText()));
					totalHours =totalHours +timeSheet.getHours();
					timeSheet.setMonth(selectedMonth);
					Label jobField = (Label) flex.getWidget(i+1, 32);
					Job job = new Job();
					job.setJobId(Integer.parseInt(jobField.getText()));
					timeSheet.setJobId(job);
					User user = new User();
					user.setUserId(loggedInUser.getUserId());
					timeSheet.setUserId(user);
					timeSheet.setStatus(0);
					timeSheetList.add(timeSheet);
					
				}
			}
			
		}
		saveTimeSheetToDb(timeSheetList);
	}

	private void saveTimeSheetToDb(ArrayList<TimeSheet> timeSheetList) {
		rpcService.saveTimeSheet(timeSheetList, new AsyncCallback<String>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert("fail savetime");
			}

			@Override
			public void onSuccess(String result) {
				Window.alert("time sheet saved");
			}
		});
	}
	
	private void submitTimeSheetToDb(ArrayList<TimeSheet> timeSheetList) {
		rpcService.saveTimeSheet(timeSheetList, new AsyncCallback<String>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert("fail savetime");
			}

			@Override
			public void onSuccess(String result) {
				Window.alert("time sheet submitted");
			}
		});
	}
	
	public void getMonthAllowedHours(final float totalHours, int month, final ArrayList<TimeSheet> timeSheetList){
		rpcService.fetchMonthAllowedhours(month, new AsyncCallback<Integer>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert("fail : fetch monthallowedHours");
			}

			@Override
			public void onSuccess(Integer result) {
				if(totalHours>=result){
					submitTimeSheetToDb(timeSheetList);
				}else{
					Window.alert("Total Number of hours ("+ totalHours+ ") cannot be less than Total working hours(" +result+ ") of this month");
				}
			}
		});
	}

}
