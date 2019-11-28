package com.leavemanagement.database;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.leavemanagement.client.presenter.HeaderAndFooterPdfPageEventHelper;
import com.leavemanagement.shared.Activity;
import com.leavemanagement.shared.AllJobsReportDTO;
import com.leavemanagement.shared.Allocations;
import com.leavemanagement.shared.AttributeRating;
import com.leavemanagement.shared.Branches;
import com.leavemanagement.shared.Company;
import com.leavemanagement.shared.Countries;
import com.leavemanagement.shared.Domains;
import com.leavemanagement.shared.HibernateDetachUtility;
import com.leavemanagement.shared.Job;
import com.leavemanagement.shared.JobActivityEntity;
import com.leavemanagement.shared.JobAttributes;
import com.leavemanagement.shared.JobAttributesDTO;
import com.leavemanagement.shared.JobEmployees;
import com.leavemanagement.shared.JobUsersDTO;
import com.leavemanagement.shared.LeaveRecord;
import com.leavemanagement.shared.LeaveTypes;
import com.leavemanagement.shared.LeavesDTO;
import com.leavemanagement.shared.LeavesDTOForAllUsers;
import com.leavemanagement.shared.LineofService;
import com.leavemanagement.shared.Location;
import com.leavemanagement.shared.MonthAllowedHours;
import com.leavemanagement.shared.Phases;
import com.leavemanagement.shared.Roles;
import com.leavemanagement.shared.SubLineofService;
import com.leavemanagement.shared.TimeSheet;
import com.leavemanagement.shared.TimeSheetReportDTO;
import com.leavemanagement.shared.User;
import com.leavemanagement.shared.UserReportDTO;

public class MySQLRdbHelper {

	private SessionFactory sessionFactory;
	private final static Logger logger = Logger.getLogger("MySQLRdbHelper");
	String error = "";
	// Logger logger = Logger.getLogger(MySQLRdbHelper.class);

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public User getAuthentication(String userid, String password) throws Exception {
		// logger.setLevel(Level.INFO);
		User users = null;
		Session session = null;

		try {
			session = sessionFactory.openSession();

			Criteria crit = session.createCriteria(User.class);
			crit.add(Restrictions.eq("name", userid));
			crit.add(Restrictions.eq("password", password));
			crit.add(Restrictions.eq("status", "active"));
			crit.createAlias("roleId", "role");
			crit.createAlias("companyId", "company");
			List rsList = crit.list();
			for (Iterator it = rsList.iterator(); it.hasNext();) {
				users = (User) it.next();
				System.out.println(users.getName() + " Signed In on" + new Date());
				// BasicConfigurator.configure();
				// logger1.info("This is my first log4j's statement");
				logger.info(
						"inside Getting Authentication LOG MSG-->" + users.getName() + " Signed In on" + new Date());
			}

		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in getAuthentication", ex.getMessage()), ex);
			System.out.println("Exception occured in getAuthentication" + ex.getMessage());

			throw new Exception("Exception occured in getAuthentication");// Add
																			// this
																			// Line
																			// Accordingly
																			// in
																			// all
																			// method
		} finally {
			session.close();
		}

		return users;
	}

	public ArrayList<LeavesDTO> fetchAvailableLeaves(User userId) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			ArrayList<LeavesDTO> leavesDTOs = new ArrayList<LeavesDTO>();
			ArrayList<LeaveTypes> leaveTypes = fetchLeaveTypes();
			for (int i = 0; i < leaveTypes.size(); i++) {
				LeavesDTO leavesDTO = new LeavesDTO();
				int leaveTypeId = leaveTypes.get(i).getLeaveTypeId();
				long leaveAvailedforThisLeaveType = fetchCurrentUserLeaveforSelectedLeaveType(leaveTypeId,
						userId.getUserId(), session);
				leavesDTO.setLeaveType(leaveTypes.get(i));
				int allowed = 0;
				if (leaveTypeId == 4) {
					allowed = userId.getExamLeaves();
				} else {
					allowed = leaveTypes.get(i).getAllowed();
				}
				long available = allowed - leaveAvailedforThisLeaveType;
				leavesDTO.setLeavesAvaible(available);
				leavesDTOs.add(leavesDTO);
			}
			return leavesDTOs;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchAvailableLeaves", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchAvailableLeaves" + ex.getMessage());

			throw new Exception("Exception occured in fetchAvailableLeaves");// Add
																				// this
																				// Line
																				// Accordingly
																				// in
																				// all
																				// method
		} finally {
			session.close();
		}

	}

	private long fetchCurrentUserLeaveforSelectedLeaveType(int leaveTypeId, int userId, Session session)
			throws Exception {
		try {
			Criteria crit = session.createCriteria(LeaveRecord.class);
			crit.createAlias("userId", "user");
			crit.createAlias("user.roleId", "role");
			crit.createAlias("user.companyId", "company");
			crit.add(Restrictions.ne("user.status", "inActive"));
			crit.createAlias("leaveTypeId", "lType");
			crit.add(Restrictions.eq("user.userId", userId));
			crit.add(Restrictions.eq("lType.leaveTypeId", leaveTypeId));
			crit.add(Restrictions.eq("status", "Approved"));
			List rsList = crit.list();
			long totalDays = 0;
			for (Iterator it = rsList.iterator(); it.hasNext();) {
				LeaveRecord leaveRecord = (LeaveRecord) it.next();

				// long diff = Math.abs(leaveRecord.getStartDate().getTime() -
				// leaveRecord.getEndDate().getTime());
				//
				long diff = getDiffInDates(leaveRecord.getStartDate(), leaveRecord.getEndDate());
				// long diffDays = diff / (24 * 60 * 60 * 1000);
				// diffDays =diffDays+1;
				// long diffDays = (diff + 12 * 60 * 60 * 1000) / (24 * 60 * 60
				// * 1000)
				totalDays = totalDays + diff + 1;
			}
			return totalDays;
		} catch (Exception ex) {
			logger.warn(
					String.format("Exception occured in fetchCurrentUserLeaveforSelectedLeaveType", ex.getMessage()),
					ex);
			System.out.println("Exception occured in fetchCurrentUserLeaveforSelectedLeaveType" + ex.getMessage());

			throw new Exception("Exception occured in fetchCurrentUserLeaveforSelectedLeaveType");// Add
																									// this
																									// Line
																									// Accordingly
																									// in
																									// all
																									// method
		} finally {
			// session.close();
		}

	}

	public long getDiffInDates(Date date1, Date date2) throws Exception {
		// DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		// Date date1 = df.parse("10/08/2013");
		// Date date2 = df.parse("21/08/2013");
		long numberOfDays = 0;
		try {
			Calendar cal1 = Calendar.getInstance();
			Calendar cal2 = Calendar.getInstance();
			cal1.setTime(date1);
			cal2.setTime(date2);

			while (cal1.before(cal2)) {
				if ((Calendar.SATURDAY != cal1.get(Calendar.DAY_OF_WEEK))
						&& (Calendar.SUNDAY != cal1.get(Calendar.DAY_OF_WEEK))) {
					numberOfDays++;
					cal1.add(Calendar.DATE, 1);
				} else {
					cal1.add(Calendar.DATE, 1);
				}
			}

		} catch (Exception ex) {
			System.out.println(ex);
		}
		return numberOfDays;
	}

	public ArrayList<LeaveTypes> fetchLeaveTypes() throws Exception {

		Session session = null;
		ArrayList<LeaveTypes> leaveTypes = new ArrayList<LeaveTypes>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(LeaveTypes.class);
			List rsList = crit.list();
			for (Iterator it = rsList.iterator(); it.hasNext();) {
				LeaveTypes leaveType = (LeaveTypes) it.next();
				leaveTypes.add(leaveType);
			}
			return leaveTypes;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchLeaveTypes", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchLeaveTypes" + ex.getMessage());

			throw new Exception("Exception occured in getAuthentication");// Add
																			// this
																			// Line
																			// Accordingly
																			// in
																			// all
																			// method
		} finally {
			session.close();
		}
	}

	public User fetchUser(int userId) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(User.class);
			crit.add(Restrictions.eq("id", userId));
			crit.createAlias("roleId", "role");
			crit.createAlias("companyId", "company");
			User user = (User) crit.list().get(0);
			return user;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchUser", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchUser" + ex.getMessage());

			throw new Exception("Exception occured in fetchUser");// Add this
																	// Line
																	// Accordingly
																	// in all
																	// method
		} finally {
			session.close();
		}
	}

	public String fetchUsersEmail(int userId, Session session) {
		String email = "";
		try {
			Criteria crit = session.createCriteria(User.class);
			crit.add(Restrictions.eq("userId", userId));
			User user = (User) crit.list().get(0);
			email = user.getEmail();

		} catch (Exception ex) {
			System.out.println("fail fetchuseremail");
		}
		return email;
	}

	public String saveLeaveRequest(LeaveRecord leaveRecord) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			session.save(leaveRecord);
			User user = fetchUser(leaveRecord.getUserId().getReportingTo());
			session.flush();
			// 2019 july changing mail of sir faheem for hyphen again changed to
			// sir faheem's
			sendEmail(
					"Leave Request Received: from " + leaveRecord.getUserId().getName() + ", Reason : "
							+ leaveRecord.getReason(),
					user.getEmail(), "faheem.piracha@hyphenconsultancy.com", "Leave Request Received");

		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in saveLeaveRequest", ex.getMessage()), ex);
			System.out.println("Exception occured in saveLeaveRequest" + ex.getMessage());

			throw new Exception("Exception occured in saveLeaveRequest");// Add
																			// this
																			// Line
																			// Accordingly
																			// in
																			// all
																			// method
		} finally {
			session.close();
		}
		return "Leave Request Submitted";
	}

	public ArrayList<LeaveRecord> fetchLeaveRecord(int userId, int companyId) throws Exception {
		Session session = null;
		ArrayList<LeaveRecord> leaveRecords = new ArrayList<LeaveRecord>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(LeaveRecord.class);
			crit.createAlias("userId", "user");
			crit.createAlias("user.roleId", "role");
			crit.createAlias("user.companyId", "company");
			crit.add(Restrictions.ne("user.status", "inActive"));
			crit.createAlias("leaveTypeId", "lType");
			crit.add(Restrictions.eq("company.companyId", companyId));
			crit.add(Restrictions.ne("status", "pending"));
			crit.addOrder(Order.desc("startDate"));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				LeaveRecord leaveRecord = (LeaveRecord) it.next();
				leaveRecords.add(leaveRecord);
				long diff = Math.abs(leaveRecord.getStartDate().getTime() - leaveRecord.getEndDate().getTime());
				long days = diff / (24 * 60 * 60 * 1000);
				days = days + 1;
				leaveRecord.setDays(days + "");

			}
			return leaveRecords;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchLeaveRecord", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchLeaveRecord" + ex.getMessage());

			throw new Exception("Exception occured in fetchLeaveRecord");// Add
																			// this
																			// Line
																			// Accordingly
																			// in
																			// all
																			// method
		} finally {
			session.close();
		}

	}

	private boolean leavesAvailable(User userId, int leaveTypeId, int noOfDays) throws Exception {
		boolean leavesAvailable = false;
		try {
			ArrayList<LeavesDTO> leavesDTO = fetchAvailableLeaves(userId);
			if (leavesDTO != null) {
				for (int i = 0; i < leavesDTO.size(); i++) {
					if (leavesDTO.get(i).getLeaveType().getLeaveTypeId() == leaveTypeId
							&& leavesDTO.get(i).getLeavesAvaible() - noOfDays >= 0) {
						leavesAvailable = true;
						break;
					}
				}
			}
			return leavesAvailable;
		} catch (Exception ex) {
			System.out.println("error in leavesAvailable");
			throw new Exception("Exception occured in leavesAvailable");
		}

	}

	public String approveDeclineRequest(LeaveRecord leaveRecord) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			boolean notAvailable = leaveRecord.getStatus().equalsIgnoreCase("Approved")
					&& !leavesAvailable(leaveRecord.getUserId(), leaveRecord.getLeaveType().getLeaveTypeId(),
							Integer.parseInt(leaveRecord.getDays()));
			if (notAvailable) {

				return "Not enough required number of leaves available for this leave type";
			}

			session.update(leaveRecord);
			session.flush();
			sendEmail(
					"Your Leave Application for " + leaveRecord.getLeaveType().getLeaveTypeName() + " has been "
							+ leaveRecord.getStatus(),
					leaveRecord.getUserId().getEmail(), "", "Leave Application : " + leaveRecord.getStatus());

			if (leaveRecord.getStatus().equalsIgnoreCase("Approved")) {
				return "Request Approved";
			} else {
				return "Request Declined";
			}
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in approveRequest", ex.getMessage()), ex);
			System.out.println("Exception occured in approveRequest" + ex.getMessage());

			throw new Exception("Exception occured in approveRequest");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}

	}

	public String fetchOldPassword(int userId) throws Exception {
		Session session = null;
		String oldPassword = "";
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(User.class);
			crit.add(Restrictions.eq("id", userId));
			crit.createAlias("roleId", "role");
			crit.createAlias("companyId", "company");
			if (crit.list().size() > 0) {
				User user = (User) crit.list().get(0);
				oldPassword = user.getPassword();
			}
			return oldPassword;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchOldPassword", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchOldPassword" + ex.getMessage());

			throw new Exception("Exception occured in fetchOldPassword");// Add
																			// this
																			// Line
																			// Accordingly
																			// in
																			// all
																			// method
		} finally {
			session.close();
		}
	}

	public String updatePassword(User user) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			session.update(user);
			session.flush();
			return "Password Updated";
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in updatePassword", ex.getMessage()), ex);
			System.out.println("Exception occured in updatePassword" + ex.getMessage());

			throw new Exception("Exception occured in updatePassword");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}
	}

	public String addUser(User user) throws Exception {
		Session session = null;
		boolean newUser = false;
		if (user.getUserId() == 0) {
			newUser = true;
		}
		try {
			session = sessionFactory.openSession();
			user.setStatus("active");
			session.saveOrUpdate(user);
			session.flush();
			if (newUser) {
				return "User Added";
			} else {
				return "Record updated";
			}
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in updatePassword", ex.getMessage()), ex);
			System.out.println("Exception occured in updatePassword" + ex.getMessage());

			throw new Exception("Exception occured in updatePassword");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}
	}

	public boolean sendEmail(String body, String sendTo, String cc, String subject) {

		final String username = "hyphenconsult@gmail.com";
		final String password = "ilzhkshpmtqduzuc";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		javax.mail.Session sessionMail = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {

			Message message = new MimeMessage(sessionMail);
			message.setFrom(new InternetAddress("hyphenconsult@gmail.com"));
			if (cc.equals("")) {
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(sendTo));
			} else {
				//////
				String addresses[] = { sendTo, cc };

				InternetAddress[] addressTo = new InternetAddress[addresses.length];
				for (int i = 0; i < addresses.length; i++) {
					addressTo[i] = new InternetAddress(addresses[i]);
				}

				message.setRecipients(Message.RecipientType.TO, addressTo);
				/////
			}

			message.setSubject(subject);
			message.setContent(body, "text/html");

			Transport.send(message);

			System.out.println("email sent");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

		return false;
	}

	public ArrayList<User> fetchAllUsersExceptDirector(int companyId) throws Exception {
		Session session = null;
		ArrayList<User> users = new ArrayList<User>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(User.class);
			crit.createAlias("roleId", "role");
			crit.createAlias("companyId", "company");
			crit.add(Restrictions.eq("company.companyId", companyId));
			crit.add(Restrictions.ne("role.roleId", 5));
			crit.add(Restrictions.ne("status", "inActive"));
			crit.add(Restrictions.ne("status", "Closed"));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				User user = (User) it.next();
				users.add(user);
			}
			return users;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchAllUsers", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchAllUsers" + ex.getMessage());

			throw new Exception("Exception occured in updatePassword");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}

	}

	public ArrayList<User> fetchAllUsers(int companyId) throws Exception {
		Session session = null;
		ArrayList<User> users = new ArrayList<User>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(User.class);
			crit.createAlias("roleId", "role");
			crit.createAlias("companyId", "company");
			crit.add(Restrictions.ne("status", "inActive"));
			crit.add(Restrictions.ne("status", "Closed"));
			crit.add(Restrictions.eq("company.companyId", companyId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				User user = (User) it.next();
				users.add(user);
			}
			return users;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchAllUsers", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchAllUsers" + ex.getMessage());

			throw new Exception("Exception occured in updatePassword");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}

	}

	public ArrayList<User> fetchAllUsers() throws Exception {
		Session session = null;
		ArrayList<User> users = new ArrayList<User>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(User.class);
			crit.createAlias("roleId", "role");
			crit.createAlias("companyId", "company");
			crit.add(Restrictions.ne("status", "inActive"));
			crit.add(Restrictions.ne("status", "Closed"));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				User user = (User) it.next();
				users.add(user);
			}
			return users;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchAllUsers", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchAllUsers" + ex.getMessage());

			throw new Exception("Exception occured in updatePassword");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}

	}

	public ArrayList<LeaveRecord> fetchPendingLeavesRecordOfLoggedInUser(int userId, int companyId) throws Exception {
		Session session = null;
		ArrayList<LeaveRecord> leaveRecords = new ArrayList<LeaveRecord>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(LeaveRecord.class);
			crit.createAlias("userId", "user");

			crit.createAlias("user.roleId", "role");
			crit.createAlias("user.companyId", "company");
			crit.createAlias("leaveTypeId", "lType");
			crit.add(Restrictions.eq("user.userId", userId));
			crit.add(Restrictions.ne("user.status", "inActive"));
			crit.addOrder(Order.desc("startDate"));
			// crit.add(Restrictions.eq("status", "pending"));
			crit.add(Restrictions.eq("company.companyId", companyId));
			List rsList = crit.list();
			for (Iterator it = rsList.iterator(); it.hasNext();) {

				LeaveRecord leaveRecord = (LeaveRecord) it.next();
				leaveRecords.add(leaveRecord);
				long days = getDiffInDates(leaveRecord.getStartDate(), leaveRecord.getEndDate());

				days = days + 1;
				leaveRecord.setDays(days + "");

			}
			return leaveRecords;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchLeaveRecord", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchLeaveRecord" + ex.getMessage());

			throw new Exception("Exception occured in fetchLeaveRecord");// Add
																			// this
																			// Line
																			// Accordingly
																			// in
																			// all
																			// method
		} finally {
			session.close();
		}
	}

	public ArrayList<LeaveRecord> fetchPendingLeavesRecord(int companyId, int loggedInUserId) throws Exception {
		Session session = null;
		ArrayList<LeaveRecord> leaveRecords = new ArrayList<LeaveRecord>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(LeaveRecord.class);
			crit.createAlias("userId", "user");
			crit.createAlias("user.roleId", "role");
			crit.createAlias("user.companyId", "company");
			crit.add(Restrictions.ne("user.status", "inActive"));
			crit.createAlias("leaveTypeId", "lType");
			crit.addOrder(Order.desc("startDate"));
			crit.add(Restrictions.eq("status", "pending"));
			crit.add(Restrictions.eq("company.companyId", companyId));
			crit.add(Restrictions.eq("user.reportingTo", loggedInUserId));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				LeaveRecord leaveRecord = (LeaveRecord) it.next();
				leaveRecords.add(leaveRecord);
				long days = getDiffInDates(leaveRecord.getStartDate(), leaveRecord.getEndDate());

				days = days + 1;
				leaveRecord.setDays(days + "");

			}
			return leaveRecords;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchLeaveRecord", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchLeaveRecord" + ex.getMessage());

			throw new Exception("Exception occured in fetchLeaveRecord");// Add
																			// this
																			// Line
																			// Accordingly
																			// in
																			// all
																			// method
		} finally {
			session.close();
		}
	}

	public ArrayList<LeavesDTOForAllUsers> fetchLeavesRemainingForAllUsers(int companyId) throws Exception {
		Session session = null;
		ArrayList<User> users = fetchAllUsersExceptDirector(companyId);
		try {
			session = sessionFactory.openSession();
			ArrayList<LeavesDTOForAllUsers> leavesDTOs = new ArrayList<LeavesDTOForAllUsers>();
			ArrayList<LeaveTypes> leaveTypes = fetchLeaveTypes();
			for (int j = 0; j < users.size(); j++) {
				LeavesDTOForAllUsers leavesDTOForAllUsers = new LeavesDTOForAllUsers();
				for (int i = 0; i < leaveTypes.size(); i++) {
					LeavesDTO leavesDTO = new LeavesDTO();
					int leaveTypeId = leaveTypes.get(i).getLeaveTypeId();
					long leaveAvailedforThisLeaveType = fetchCurrentUserLeaveforSelectedLeaveType(leaveTypeId,
							users.get(j).getUserId(), session);
					leavesDTO.setLeaveType(leaveTypes.get(i));
					leavesDTOForAllUsers.setUser(users.get(j));
					int allowed = 0;
					if (leaveTypeId == 4) {
						allowed = users.get(j).getExamLeaves();
					} else {
						allowed = leaveTypes.get(i).getAllowed();
					}
					long available = allowed - leaveAvailedforThisLeaveType;
					leavesDTO.setLeavesAvaible(available);
					leavesDTO.setAllowed(allowed);
					leavesDTO.setLeavesAvailed((int) leaveAvailedforThisLeaveType);
					leavesDTOForAllUsers.getLeavesDTO().add(leavesDTO);

				}
				leavesDTOs.add(leavesDTOForAllUsers);
			}
			return leavesDTOs;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchAvailableLeaves", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchAvailableLeaves" + ex.getMessage());

			throw new Exception("Exception occured in fetchAvailableLeaves");// Add
																				// this
																				// Line
																				// Accordingly
																				// in
																				// all
																				// method
		} finally {
			session.close();
		}
	}

	public ArrayList<Roles> fetchAllRoles() throws Exception {
		Session session = null;
		ArrayList<Roles> roles = new ArrayList<Roles>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Roles.class);

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Roles role = (Roles) it.next();
				roles.add(role);
			}
			return roles;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchAllRoles", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchAllRoles" + ex.getMessage());

			throw new Exception("Exception occured in updatePassword");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}
	}

	public String addCompany(Company company, User user) throws Exception {
		Session session = null;

		try {
			session = sessionFactory.openSession();
			session.saveOrUpdate(company);
			user.setCompanyId(company);
			session.save(user);
			user.setReportingTo(user.getUserId());
			session.update(user);

			session.flush();
			return "company added";
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in updatePassword", ex.getMessage()), ex);
			System.out.println("Exception occured in updatePassword" + ex.getMessage());

			throw new Exception("Exception occured in updatePassword");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}
	}

	public String deleteUser(int userId) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			User user = (User) session.get(User.class, userId);
			user.setStatus("inActive");
			session.update(user);
			session.flush();
			return "user deleted";
		}

		catch (Exception ex) {
			logger.warn(String.format("Exception occured in updatePassword", ex.getMessage()), ex);
			System.out.println("Exception occured in updatePassword" + ex.getMessage());

			throw new Exception("Exception occured in updatePassword");// Add
																		// this
																		// Line
																		// Accordingly
																		// in
																		// all
																		// method
		} finally {
			session.close();
		}
	}

	public JobAttributesDTO getJobAttributes() throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			ArrayList<LineofService> lineofServices = getLineOfServices();
			ArrayList<Countries> countries = getCountries(session);
			ArrayList<User> users = fetchAllUsers();
			ArrayList<SubLineofService> subLineofServices = fetchSubLineOfServices(1);
			ArrayList<Domains> domains = fetchDomains();

			JobAttributesDTO jobAttributesDTO = new JobAttributesDTO();
			jobAttributesDTO.setCountries(countries);
			jobAttributesDTO.setDomains(domains);
			jobAttributesDTO.setLineofService(lineofServices);
			jobAttributesDTO.setSubLineofService(subLineofServices);
			jobAttributesDTO.setUsers(users);

			return jobAttributesDTO;

		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in getJobAttributes", ex.getMessage()), ex);
			System.out.println("Exception occured in getJobAttributes" + ex.getMessage());

			throw new Exception("Exception occured in getJobAttributes");
		} finally {

		}
	}

	public ArrayList<LineofService> getLineOfServices() throws Exception {
		ArrayList<LineofService> lineofServices = new ArrayList<LineofService>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(LineofService.class);
			crit.createAlias("domainId", "domain");
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				LineofService lineofService = (LineofService) it.next();
				lineofServices.add(lineofService);
			}

			return lineofServices;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in getLineOfServices", ex.getMessage()), ex);
			System.out.println("Exception occured in getLineOfServices" + ex.getMessage());

			throw new Exception("Exception occured in getLineOfServices");
		} finally {

		}
	}

	public ArrayList<SubLineofService> fetchSubLineOfServices(int domainId) throws Exception {
		ArrayList<SubLineofService> subLineofServices = new ArrayList<SubLineofService>();
		Session session;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(SubLineofService.class);
			crit.createAlias("domainId", "domain");
			// crit.createAlias("domain.lineofServiceId", "domainline");
			crit.add(Restrictions.eq("domain.domainId", domainId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				SubLineofService lineofService = (SubLineofService) it.next();
				subLineofServices.add(lineofService);
			}

			return subLineofServices;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in getSubLineOfServices", ex.getMessage()), ex);
			System.out.println("Exception occured in getSubLineOfServices" + ex.getMessage());

			throw new Exception("Exception occured in getSubLineOfServices");
		} finally {

		}
	}

	private ArrayList<Countries> getCountries(Session session) throws Exception {
		ArrayList<Countries> countries = new ArrayList<Countries>();

		try {
			Criteria crit = session.createCriteria(Countries.class);
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Countries country = (Countries) it.next();
				countries.add(country);
			}

			return countries;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in getCountries", ex.getMessage()), ex);
			System.out.println("Exception occured in getCountries" + ex.getMessage());

			throw new Exception("Exception occured in getCountries");
		} finally {

		}
	}

	public ArrayList<Domains> fetchDomains(int lineofServiceId) throws Exception {
		ArrayList<Domains> domains = new ArrayList<Domains>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Domains.class);
			crit.createAlias("lineofServiceId", "lineofService");
			// crit.add(Restrictions.eq("lineofService.lineofServiceId",
			// lineofServiceId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Domains domain = (Domains) it.next();
				if (domain.getDomainId() != 6) {
					domains.add(domain);
				}
			}

			return domains;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in getDomains", ex.getMessage()), ex);
			System.out.println("Exception occured in getDomains" + ex.getMessage());

			throw new Exception("Exception occured in getDomains");
		} finally {

		}
	}

	public ArrayList<Domains> fetchDomains() throws Exception {
		ArrayList<Domains> domains = new ArrayList<Domains>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Domains.class);
			/// crit.createAlias("lineofServiceId", "lineofService");
			//////////// crit.add(Restrictions.eq("lineofService.lineofServiceId",
			/// lineofServiceId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Domains domain = (Domains) it.next();
				if (domain.getDomainId() != 6) {
					domains.add(domain);
				}

			}

			return domains;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in getDomains", ex.getMessage()), ex);
			System.out.println("Exception occured in getDomains" + ex.getMessage());

			throw new Exception("Exception occured in getDomains");
		} finally {

		}
	}

	public String saveJob(Job job) throws Exception {
		Session session = null;
		Transaction tr = null;
		if (job.getJobId() == 0) {
			int year = Year.now().getValue();
			job.setDateYear(year);
		}
		String result = "Job Created";
		if (job != null && job.getJobId() != 0)
			result = "Job Updated";
		try {

			session = sessionFactory.openSession();
			tr = session.beginTransaction();
			job.setStatus("Active");
			session.saveOrUpdate(job);

			// session.flush();
			// saveEmployeeJob(job.getJobEmployeesList(),job.getSupervisorId().getUserId(),
			// job.getPrincipalConsultantId().getUserId(), job.getJobId(),
			// session);
			// ArrayList<JobEmployees> jobEmployeeList = new
			// ArrayList<JobEmployees>();
			if (job.getUsersList() != null) {
				ArrayList<JobEmployees> jobUserList = new ArrayList<JobEmployees>();
				// for(int i=0; i< job.getJobActivities().size(); i++){
				// if(job.getJobActivities().get(i).getTotalHours()>0 ){
				// JobEmployees jobEmployee = new JobEmployees();
				// jobEmployee.setEmployeeId(job.getJobActivities().get(i).getUserId());
				// jobEmployee.setJobId(job.getJobId());
				// jobEmployeeList.add(jobEmployee);
				//
				// }
				// }

				for (int i = 0; i < job.getUsersList().size(); i++) {
					// if(job.getUsersList().get(i).getTotalHours()>0 ){
					JobEmployees jobEmployee = new JobEmployees();
					jobEmployee.setEmployeeId(job.getUsersList().get(i));
					jobEmployee.setJobId(job.getJobId());
					jobUserList.add(jobEmployee);

					// }
				}

				// jobUserList.add((JobEmployees) job.getUsersList());
				// saveEmployeeJob(jobEmployeeList, job.getJobId(), session);
				saveEmployeeJob(jobUserList, job.getJobId(), session);

				session.flush();
				setJobActivities(job.getJobActivities(), session, job);
				// addPhase(job, session);

			}
			tr.commit();
			//
			// for(int i=0; i< job.getJobEmployeesList().size(); i++){
			// String email =
			// fetchUsersEmail(job.getJobEmployeesList().get(i).getEmployeeId().getUserId(),
			// session);
			// String body= "Dear "+
			// job.getJobEmployeesList().get(i).getEmployeeId().getName()+" : "+
			// "A new job has been created named (" + job.getJobName() +") and
			// assigned to you";
			// sendEmail(body, email, "", "Job Created");
			// }
			// String email = fetchUsersEmail(job.getSupervisorId().getUserId(),
			// session);
			// String body= "Dear "+ job.getSupervisorId().getName()+" : "+ "A
			// new job has been created named (" + job.getJobName() +") and
			// assigned to you";
			//
			// sendEmail(body, email, "", "Job Created");///////??

			return result;
		} catch (Exception ex) {
			tr.rollback();
			logger.warn(String.format("Exception occured in saveJob", ex.getMessage()), ex);
			System.out.println("Exception occured in saveJob" + ex.getMessage());

			throw new Exception("Exception occured in saveJob");
		} finally {
			session.close();
		}

	}

	private void setJobActivities(ArrayList<JobActivityEntity> jobActivityList, Session session, Job job) {
		try {
			// ArrayList<Activity> default = new ArrayList<>(Activity);
			for (int i = 0; i < jobActivityList.size(); i++) {

				JobActivityEntity jobActivity = jobActivityList.get(i);

				/*
				 * jobActivity.setUserId(jobActivityList.get(i).getUserId());
				 * jobActivity.setPlanning(jobActivityList.get(i).getPlanning())
				 * ;
				 * jobActivity.setExecution(jobActivityList.get(i).getExecution(
				 * ));
				 * jobActivity.setFollowup(jobActivityList.get(i).getFollowup())
				 * ;
				 * jobActivity.setReporting(jobActivityList.get(i).getReporting(
				 * ));
				 */

				jobActivity.setJobId(job);
				session.saveOrUpdate(jobActivity);
				session.flush();
			}

		} catch (Exception ex) {
			System.out.println(ex);
		}

	}

	/*
	 * private void saveEmployeeJob(ArrayList<JobEmployees> jobEmployeesList,
	 * int jobId, Session session) { try{ for(int i=0; i<
	 * jobEmployeesList.size(); i++){ JobEmployees jobEmployees =
	 * jobEmployeesList.get(i); jobEmployees.setJobId(jobId); //
	 * if(employeeJobAlreadySaved(jobId,
	 * jobEmployees.getEmployeeId().getUserId(), session)){ //
	 * session.update(jobEmployees); // session.flush(); // }else{
	 * session.saveOrUpdate(jobEmployees); session.flush(); // } }
	 * 
	 * }catch(Exception ex){ System.out.println(ex); }
	 * 
	 * }
	 */

	private void saveEmployeeJob(ArrayList<JobEmployees> jobEmployeesList, int jobId, Session session) {
		try {

			// first del this job users...
			Criteria crit = session.createCriteria(JobEmployees.class);
			crit.add(Restrictions.eq("jobId", jobId));
			ArrayList<JobEmployees> jobEmployee = (ArrayList<JobEmployees>) crit.list();
			for (JobEmployees job : jobEmployee) {
				session.delete(job);
				session.flush();
			}
			////////////////////////

			for (int i = 0; i < jobEmployeesList.size(); i++) {
				JobEmployees jobEmployees = jobEmployeesList.get(i);
				jobEmployees.setJobId(jobId);
				if (employeeJobAlreadySaved(jobId, jobEmployees.getEmployeeId().getUserId(), session)) {
					System.out.print("a");
					// session.update(jobEmployees);
					// session.flush();
				} else {
					session.saveOrUpdate(jobEmployees);
					session.flush();
				}
			}

		} catch (Exception ex) {
			System.out.println(ex);
		}

	}

	private boolean employeeJobAlreadySaved(int jobId, int userId, Session session) {
		Criteria crit = session.createCriteria(JobEmployees.class);
		crit.createAlias("employeeId", "employee");
		crit.add(Restrictions.eq("employee.userId", userId));
		crit.add(Restrictions.eq("jobId", jobId));
		if (crit.list().size() > 0) {
			return true;
		} else {
			return false;
		}

	}

	// private void addPhase(Job job, Session session)throws Exception {
	// try{
	// for(int i=0; i< job.getJobPhases().size(); i++){
	// Phases phase = job.getJobPhases().get(i);
	// phase.setJobId(job);
	// session.saveOrUpdate(phase);
	// session.flush();
	// }
	// }catch(Exception ex){
	// logger.warn(String.format("Exception occured in addPhase",
	// ex.getMessage()), ex);
	// System.out.println("Exception occured in addPhase"+ ex.getMessage());
	//
	// throw new Exception("Exception occured in addPhase");
	// }
	// finally{
	//
	// }
	// }

	public ArrayList<Job> fetchJobs(User loggedInUser) throws Exception {
		ArrayList<Job> jobs = new ArrayList<Job>();
		Session session = null;

		try {

			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			error = "below session.createcriteria1275";
			crit.createAlias("lineofServiceId", "lineofService");
			// crit.createAlias("subLineofServiceId", "subLineofService");
			// crit.createAlias("subLineofService.domainId", "sublineDomain");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "ddomainlineofservice");

			crit.createAlias("domainId", "domain");
			crit.createAlias("lineofService.domainId", "domainlineofservice");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "subdomainlineofservice");

			crit.createAlias("countryId", "count");
			// crit.createAlias("userId", "user");
			// crit.createAlias("user.roleId", "role");
			// crit.createAlias("user.companyId", "company");

			// crit.createAlias("supervisorId", "supervisor");
			// crit.createAlias("supervisor.roleId", "roles");
			// crit.createAlias("supervisor.companyId", "companys");

			// crit.createAlias("principalConsultantId", "principalConsultant");
			// crit.createAlias("principalConsultant.roleId", "rolep");
			// crit.createAlias("principalConsultant.companyId", "companyp");
			error = "below criterias 1299";
			crit.add(Restrictions.ne("status", "InActive"));
			crit.add(Restrictions.ne("status", "office"));
			error = "below restrictions 1302";
			// crit.add(Restrictions.ne("status", "Closed"));
			crit.addOrder(Order.asc("company"));
			error = "below ascendingorder 1305";
			if (loggedInUser.getRoleId().getRoleId() != 5) {
				error = "below roleidcheck 1307";
				ArrayList<Integer> jobIds = getUserJobs(loggedInUser.getUserId(), session);
				error = "below getuserjobfunction 1309";
				Disjunction disc = Restrictions.disjunction();
				error = "1311";
				for (int i = 0; i < jobIds.size(); i++) {
					disc.add(Restrictions.eq("jobId", jobIds.get(i)));
					error = "1314";
				}
				crit.add(disc);
				error = "1317";

			}
			List rsList = crit.list();
			error = "1321";
			for (Iterator it = rsList.iterator(); it.hasNext();) {
				error = "1323";
				Job job = (Job) it.next();
				error = "1326";
				// job.setJobPhases(fetchJobPhases(job.getJobId()));
				job.setJobEmployeesList(fetchJobEmployees(session, job.getJobId()));
				error = "1329";
				job.setJobAttributes(fetchjobAttributes(session, job.getJobId()));
				error = "1331";
				job.setTimeSheets(fetchJobTimeSheets(session, job.getJobId(), loggedInUser.getRoleId().getRoleId(),
						loggedInUser.getUserId()));
				error = "1332";
				HibernateDetachUtility.nullOutUninitializedFields(job,
						HibernateDetachUtility.SerializationType.SERIALIZATION);
				HibernateDetachUtility.nullOutUninitializedFields(job.getLineofServiceId(),
						HibernateDetachUtility.SerializationType.SERIALIZATION);
				HibernateDetachUtility.nullOutUninitializedFields(job.getDomainId(),
						HibernateDetachUtility.SerializationType.SERIALIZATION);
				error = "1339";
				// HibernateDetachUtility.nullOutUninitializedFields(job.getPrincipalConsultantId(),
				// HibernateDetachUtility.SerializationType.SERIALIZATION);
				// HibernateDetachUtility.nullOutUninitializedFields(job.getSubLineofServiceId(),
				// HibernateDetachUtility.SerializationType.SERIALIZATION);
				HibernateDetachUtility.nullOutUninitializedFields(job.getCountryId(),
						HibernateDetachUtility.SerializationType.SERIALIZATION);
				// HibernateDetachUtility.nullOutUninitializedFields(job.getSupervisorId(),
				// HibernateDetachUtility.SerializationType.SERIALIZATION);
				error = "1348";
				job.setJobActivities(fetchJobActivities(session, job.getJobId()));
				error = "1350";
				jobs.add(job);
				error = "1352";
			}
			error = "1352";
			return jobs;

		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchJobs", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchJobs" + ex.getMessage());
			error = error + "1362" + ex.getLocalizedMessage() + "'" + ex.fillInStackTrace();
			throw new Exception("Exception occured in fetchJobs .." + ex + error);
		} finally {
			session.close();
		}
	}

	public ArrayList<Job> fetchJobsForTimeSheet(User loggedInUser, boolean chargeable, int selectedMonth)
			throws Exception {
		ArrayList<Job> jobs = new ArrayList<Job>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			crit.createAlias("lineofServiceId", "lineofService");
			// crit.createAlias("subLineofServiceId", "subLineofService");
			// crit.createAlias("subLineofService.domainId", "sublineDomain");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "ddomainlineofservice");

			crit.createAlias("domainId", "domain");
			crit.createAlias("lineofService.domainId", "domainlineofservice");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "subdomainlineofservice");

			crit.createAlias("countryId", "count");
			// crit.createAlias("userId", "user");
			// crit.createAlias("user.roleId", "role");
			// crit.createAlias("user.companyId", "company");

			// crit.createAlias("supervisorId", "supervisor");
			// crit.createAlias("supervisor.roleId", "roles");
			// crit.createAlias("supervisor.companyId", "companys");
			//
			//
			// crit.createAlias("principalConsultantId", "principalConsultant");
			// crit.createAlias("principalConsultant.roleId", "rolep");
			// crit.createAlias("principalConsultant.companyId", "companyp");
			crit.addOrder(Order.asc("company"));
			crit.add(Restrictions.ne("status", "InActive"));
			crit.add(Restrictions.ne("status", "Closed"));
			if (chargeable) {
				crit.add(Restrictions.eq("allocation", 1));
			}
			// crit.add(Restrictions.ne("client", "office"));

			// if(loggedInUser.getRoleId().getRoleId()!=5){
			ArrayList<Integer> jobIds = getUserJobs(loggedInUser.getUserId(), session);
			Disjunction disc = Restrictions.disjunction();
			for (int i = 0; i < jobIds.size(); i++) {
				disc.add(Restrictions.eq("jobId", jobIds.get(i)));
			}
			crit.add(disc);

			// }
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();
				// if (job.getJobId() == 281) {
				// jobs.set(0, job);
				// }

				job.setActivityLists(
						fetchActivities(session, job, loggedInUser.getRoleId().getRoleId(), loggedInUser.getUserId()));
				// job.setJobPhases(fetchJobPhases(job.getJobId()));
				// job.setFetchDefaultActivityList(fetchActivitiesDefault(session));
				job.setJobEmployeesList(fetchJobEmployees(session, job.getJobId()));
				job.setJobAttributes(fetchjobAttributes(session, job.getJobId()));
				job.setTimeSheets(fetchJobTimeSheets(session, job.getJobId(), loggedInUser.getRoleId().getRoleId(),
						loggedInUser.getUserId()));
				// if (job.getJobId() != 281) {
				jobs.add(job);
				// }
			}

			return jobs;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchJobsForTimeSheet", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchJobsForTimeSheet" + ex.getMessage());

			throw new Exception("Exception occured in fetchJobsForTimeSheet");
		} finally {
			session.close();
		}
	}

	public ArrayList<Job> fetchSelectedJobForTimeSheet(User loggedInUser, boolean chargeable, int jobId)
			throws Exception {
		ArrayList<Job> jobs = new ArrayList<Job>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			crit.createAlias("lineofServiceId", "lineofService");
			crit.createAlias("domainId", "domain");
			crit.createAlias("lineofService.domainId", "domainlineofservice");
			crit.createAlias("countryId", "count");

			crit.add(Restrictions.ne("status", "InActive"));
			crit.add(Restrictions.ne("status", "Closed"));
			if (chargeable) {
				crit.add(Restrictions.eq("allocation", 1));
			}
			// crit.add(Restrictions.ne("client", "office"));

			crit.add(Restrictions.eq("jobId", jobId));

			// }
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();
				// if (job.getJobId() == 281) {
				// // jobs.set(0, job);
				// }

				job.setActivityLists(
						fetchActivities(session, job, loggedInUser.getRoleId().getRoleId(), loggedInUser.getUserId()));
				// job.setJobPhases(fetchJobPhases(job.getJobId()));
				// job.setFetchDefaultActivityList(fetchActivitiesDefault(session));
				job.setJobEmployeesList(fetchJobEmployees(session, job.getJobId()));
				job.setJobAttributes(fetchjobAttributes(session, job.getJobId()));
				job.setTimeSheets(fetchJobTimeSheets(session, job.getJobId(), loggedInUser.getRoleId().getRoleId(),
						loggedInUser.getUserId()));
				// if (job.getJobId() != 281) {
				jobs.add(job);
				// }
			}

			return jobs;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchSelectedJobForTimeSheet", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchSelectedJobForTimeSheet" + ex.getMessage());

			throw new Exception("Exception occured in fetchSelectedJobForTimeSheet");
		} finally {
			session.close();
		}
	}

	private ArrayList<Activity> fetchActivities(Session session, Job job, int roleId, int userId) throws Exception {
		ArrayList<Activity> listActivities = new ArrayList<Activity>();
		try {
			session = sessionFactory.openSession();
			int line = 74;
			Criteria crit = session.createCriteria(Activity.class);
			// crit.createAlias("activityId", "activity");
			crit.createAlias("lineofServiceId", "lineOfService");
			crit.createAlias("lineOfService.domainId", "domainId");
			crit.add(Restrictions.eq("lineOfService.lineofServiceId", job.getLineofServiceId().getLineofServiceId()));
			// crit.add(Restrictions.eq("lineOfService.lineofServiceId", line));
			// crit.add(Restrictions.eq("domainId",
			// job.getLineofServiceId().getDomainId().getDomainId()));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Activity activity = (Activity) it.next();

				activity.setTimeSheets(fetchActivityTimeSheets(session, activity.getActivityId(), roleId, userId));

				listActivities.add(activity);
			}
			return listActivities;
		} catch (Exception ex) {
			System.out.println("fail : fetchActivities: " + ex);
			throw ex;
		}
	}

	private ArrayList<Activity> fetchActivitiesDefault(Session session) throws Exception {
		ArrayList<Activity> listActivities = new ArrayList<Activity>();
		try {
			session = sessionFactory.openSession();
			int line = 74;
			Criteria crit = session.createCriteria(Activity.class);
			// crit.createAlias("activityId", "activity");
			// crit.createAlias("lineofServiceId", "lineOfService");
			// crit.createAlias("lineOfService.domainId", "domainId");
			// crit.add(Restrictions.eq("lineOfService.lineofServiceId",
			// job.getLineofServiceId().getLineofServiceId()));
			// // crit.add(Restrictions.eq("lineOfService.lineofServiceId",
			// line));
			// // crit.add(Restrictions.eq("domainId",
			// // job.getLineofServiceId().getDomainId().getDomainId()));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Activity activity = (Activity) it.next();
				// if (activity.getActivityId() == 254)
				// {
				// listActivities.add(activity);
				// }
				if (activity.getLineofServiceId().getLineofServiceId() == 74) {
					listActivities.add(activity);
				}
			}
			return listActivities;
		} catch (Exception ex) {
			System.out.println("fail : fetchActivities: " + ex);
			throw ex;
		}
	}

	private ArrayList<JobActivityEntity> fetchJobActivities(Session session, int jobId) throws Exception {
		ArrayList<JobActivityEntity> listJobActivity = new ArrayList<JobActivityEntity>();
		try {
			Criteria crit = session.createCriteria(JobActivityEntity.class);
			crit.createAlias("jobId", "job");
			crit.createAlias("userId", "user");
			crit.add(Restrictions.eq("job.jobId", jobId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				JobActivityEntity jobActivityEntity = (JobActivityEntity) it.next();
				listJobActivity.add(jobActivityEntity);
			}
			return listJobActivity;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchJobActivities", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchJobActivities" + ex.getMessage());

			throw new Exception("Exception occured in fetchJobActivities");
		}

	}

	private ArrayList<TimeSheet> fetchJobTimeSheets(Session session, int jobId, int roleId, int userId)
			throws Exception {
		ArrayList<TimeSheet> listTimeSheet = new ArrayList<TimeSheet>();
		try {
			Criteria crit = session.createCriteria(TimeSheet.class);
			crit.createAlias("userId", "user");
			crit.createAlias("user.roleId", "role");
			crit.createAlias("user.companyId", "company");
			crit.createAlias("jobId", "job");
			crit.createAlias("job.lineofServiceId", "lineofService1");
			// crit.createAlias("job.subLineofServiceId", "subLineofService1");
			// crit.createAlias("job.supervisorId", "supervisor");
			crit.createAlias("job.domainId", "domain1");
			crit.createAlias("job.countryId", "count1");
			crit.createAlias("activity", "activity");
			// crit.createAlias("job.principalConsultantId",
			// "principalConsultant");
			crit.add(Restrictions.eq("job.jobId", jobId));
			crit.add(Restrictions.eq("user.userId", userId));
			crit.add(Restrictions.ne("activity.activityId", 0));

			if (roleId == 5) {
				// crit.add(Restrictions.eq("status", 1));
			}

			crit.addOrder(Order.asc("activity.activityId"));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				TimeSheet timeSheet = (TimeSheet) it.next();
				listTimeSheet.add(timeSheet);
			}
			return listTimeSheet;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchJobTimeSheets", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchJobTimeSheets" + ex.getMessage());

			throw new Exception("Exception occured in fetchJobTimeSheets");
		}
	}

	//
	private ArrayList<TimeSheet> fetchActivityTimeSheets(Session session, int activityId, int roleId, int userId)
			throws Exception {
		ArrayList<TimeSheet> listTimeSheet = new ArrayList<TimeSheet>();
		try {
			Criteria crit = session.createCriteria(TimeSheet.class);
			crit.createAlias("userId", "user");
			crit.createAlias("user.roleId", "role");
			crit.createAlias("user.companyId", "company");
			crit.createAlias("jobId", "job");
			crit.createAlias("job.lineofServiceId", "lineofService1");
			// crit.createAlias("job.subLineofServiceId", "subLineofService1");
			// crit.createAlias("job.supervisorId", "supervisor");
			crit.createAlias("job.domainId", "domain1");
			crit.createAlias("job.countryId", "count1");
			crit.createAlias("activity", "activity");
			// crit.createAlias("job.principalConsultantId",
			// "principalConsultant");
			// crit.add(Restrictions.eq("job.jobId", jobId));
			crit.add(Restrictions.eq("user.userId", userId));
			crit.add(Restrictions.eq("activity.activityId", activityId));

			if (roleId == 5) {
				// crit.add(Restrictions.eq("status", 1));
			}

			crit.addOrder(Order.asc("activity.activityId"));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				TimeSheet timeSheet = (TimeSheet) it.next();
				listTimeSheet.add(timeSheet);
			}
			return listTimeSheet;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchJobTimeSheets", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchJobTimeSheets" + ex.getMessage());

			throw new Exception("Exception occured in fetchJobTimeSheets");
		}
	}

	private ArrayList<JobAttributes> fetchjobAttributes(Session session, int jobId) throws Exception {
		ArrayList<JobAttributes> jobAttributesList = new ArrayList<JobAttributes>();
		try {
			Criteria crit = session.createCriteria(JobAttributes.class);
			crit.add(Restrictions.eq("jobId", jobId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				JobAttributes jobAttributes = (JobAttributes) it.next();
				jobAttributesList.add(jobAttributes);
			}
			return jobAttributesList;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchjobAttributes", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchjobAttributes" + ex.getMessage());

			throw new Exception("Exception occured in fetchjobAttributes");
		}

	}

	private ArrayList<Integer> getUserJobs(int userId, Session session) {
		ArrayList<Integer> jobIds = new ArrayList<Integer>();
		try {
			Criteria crit = session.createCriteria(JobEmployees.class);
			crit.createAlias("employeeId", "employee");
			crit.add(Restrictions.eq("employee.userId", userId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				JobEmployees jobEmployees = (JobEmployees) it.next();
				int jobId = jobEmployees.getJobId();
				jobIds.add(jobId);
			}

		} catch (Exception ex) {
			System.out.println("getUserJobs failed");
		}
		return jobIds;

	}

	private ArrayList<JobEmployees> fetchJobEmployees(Session session, int jobId) throws Exception {
		try {
			ArrayList<JobEmployees> jobEmployeesList = new ArrayList<JobEmployees>();
			error = "1721";
			Criteria crit = session.createCriteria(JobEmployees.class);
			error = "1723";
			crit.createAlias("employeeId", "employee");
			error = "1725";
			crit.createAlias("employee.companyId", "employeeComp");
			crit.createAlias("employee.roleId", "employeeRole");
			error = "1728";
			crit.add(Restrictions.eq("jobId", jobId));
			error = "1730";
			List rsList = crit.list();
			error = "1732";
			for (Iterator it = rsList.iterator(); it.hasNext();) {
				error = "1734";
				JobEmployees jobEmployees = (JobEmployees) it.next();
				error = "1736";
				jobEmployeesList.add(jobEmployees);
				error = "1738";
			}
			error = "1740";
			return jobEmployeesList;

		} catch (Exception ex) {
			error = error + "1743" + ex.getLocalizedMessage() + "'" + ex.fillInStackTrace() + "'" + ex.getMessage();
			System.out.println("fetchJobEmployees failed");

			throw new Exception("Exception occured in fetchJobEmployees .." + ex + error);
		}
	}

	private ArrayList<Phases> fetchJobPhases(int jobId) throws Exception {
		Session session = null;
		ArrayList<Phases> phasesList = new ArrayList<Phases>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Phases.class);
			crit.createAlias("jobId", "job");
			crit.createAlias("job.lineofServiceId", "lineofService1");
			crit.createAlias("job.subLineofServiceId", "subLineofService1");
			crit.createAlias("job.supervisorId", "supervisor");
			crit.createAlias("job.domainId", "domain1");
			crit.createAlias("job.countryId", "count1");
			crit.createAlias("job.principalConsultantId", "principalConsultant");

			// crit.createAlias("job.userId", "user1");
			// crit.createAlias("user1.roleId", "role");
			// crit.createAlias("user1.companyId", "company");
			crit.createAlias("domain1.lineofServiceId", "domain1lineofservice");

			crit.createAlias("supervisor.roleId", "roles");
			crit.createAlias("supervisor.companyId", "companys");

			crit.createAlias("principalConsultant.roleId", "rolep");
			crit.createAlias("principalConsultant.companyId", "companyp");

			crit.add(Restrictions.eq("job.jobId", jobId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Phases phase = (Phases) it.next();
				phasesList.add(phase);
			}
			return phasesList;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchJobPhases", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchJobPhases" + ex.getMessage());

			throw new Exception("Exception occured in fetchJobPhases");
		} finally {
			session.close();
		}

	}

	public String updatePhase(Phases phase) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			session.update(phase);
			session.flush();
			return "phase updated";
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in updatePhase", ex.getMessage()), ex);
			System.out.println("Exception occured in updatePhase" + ex.getMessage());

			throw new Exception("Exception occured in updatePhase");
		} finally {
			session.close();
		}

	}

	public String deletePhase(Phases phase) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			session.delete(phase);
			session.flush();
			return "phase deleted";
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in updatePhase", ex.getMessage()), ex);
			System.out.println("Exception occured in updatePhase" + ex.getMessage());

			throw new Exception("Exception occured in updatePhase");
		} finally {
			session.close();
		}
	}

	public String deleteJobEmployee(int jobEmployeeId) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			JobEmployees jobEmployee = (JobEmployees) session.get(JobEmployees.class, jobEmployeeId);
			session.delete(jobEmployee);
			session.flush();
			return "jobEmployee deleted";
		} catch (Exception ex) {
			throw ex;
		} finally {
			session.close();
		}

	}

	public String deleteJob(int jobId) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			crit.add(Restrictions.eq("jobId", jobId));
			Job job = (Job) crit.list().get(0);
			job.setStatus("InActive");
			session.update(job);
			session.flush();
			return "job deleted";
		} catch (Exception ex) {
			throw ex;
		} finally {
			session.close();
		}
	}

	public String deleteJobAttribute(int jobAttributeId) {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			JobAttributes jobAttribute = (JobAttributes) session.get(JobAttributes.class, jobAttributeId);
			session.delete(jobAttribute);
			session.flush();
		} catch (Exception ex) {
			System.out.println("fail job delte ");
		}
		return "job deleted";
	}

	public String saveJobAttribute(JobAttributes jobAttribute) {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			session.saveOrUpdate(jobAttribute);
			session.flush();
		} catch (Exception ex) {
			System.out.println("fail job attributes save");
		} finally {

		}
		return "job attributes saved";
	}

	public String saveTimeSheet(ArrayList<TimeSheet> timeSheet) {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			deletePreviousTimeSheet(timeSheet.get(0).getMonth(), timeSheet.get(0).getUserId().getUserId(),
					timeSheet.get(0).getJobId().getJobId(), session);

			for (int i = 0; i < timeSheet.size(); i++) {
				if (timeSheet.get(i).getHours() > 0) {
					timeSheet.get(i).setActivity(timeSheet.get(i).getActivity());
					session.saveOrUpdate(timeSheet.get(i));
					session.flush();
				}
			}

		} catch (Exception ex) {
			System.out.println("fail saveTimeSheet");
		} finally {

		}
		return null;

	}

	private void deletePreviousTimeSheet(int month, int userId, int job, Session session) {
		try {
			Criteria crit = session.createCriteria(TimeSheet.class);
			crit.createAlias("userId", "user");
			crit.createAlias("jobId", "job");
			crit.add(Restrictions.eq("user.userId", userId));
			crit.add(Restrictions.eq("month", month));
			crit.add(Restrictions.eq("job.jobId", job));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				TimeSheet timeSheet = (TimeSheet) it.next();
				session.delete(timeSheet);
				session.flush();

			}

		} catch (Exception ex) {
			System.out.println("fail job delte");
		}
	}

	public void generatExcelTimeReport(HashMap<String, Integer> reportData, String rootDir) {
		try {

			// reportData.get("");
			// fetchJobWiseReport(reportData, rootDir);
			// fetchAllReport(reportData, rootDir);

		} catch (Exception ex) {

			System.out.println("fail generatExcelTimeReport");
		}

	}

	public String fetchAllReport(HashMap<String, Integer> reportData, String rootDir) {
		ArrayList<AllJobsReportDTO> jobReports = new ArrayList<AllJobsReportDTO>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			int month = 4;

			// send all the listboxes values from clientside in a hashmap
			// HashMap<String, Integer>

			if (reportData.get("jobId") != null && reportData.get("jobId") != 0) { // EXAMPLE
																					// FOR
																					// adding
																					// a
																					// filter
																					// for
																					// job
																					// name
																					// listbox,
																					// need
																					// to
																					// do
																					// this
																					// for
																					// all
																					// listboxes.
				crit.add(Restrictions.eq("jobId", reportData.get("jobId")));
			}

			if (reportData.get("lineOfServiceId") != null && reportData.get("lineOfServiceId") != 0) { // Example
																										// for
																										// line
																										// of
																										// service
				crit.createAlias("lineofServiceId", "lineofService");
				crit.add(Restrictions.eq("lineofService.lineofServiceId", reportData.get("lineOfServiceId")));
			}

			if (reportData.get("companyId") != null && reportData.get("companyId") != 0) {
				crit.add(Restrictions.eq("company", reportData.get("companyId")));
			}
			if (reportData.get("monthId") != null && reportData.get("monthId") != 0) {
				month = reportData.get("monthId");
			}
			if (reportData.get("allocationId") != null && reportData.get("allocationId") != 0) {
				crit.add(Restrictions.eq("allocation", reportData.get("allocationId")));

			}

			if (reportData.get("domainId") != null && reportData.get("domainId") != 0) {
				crit.createAlias("domainId", "domain");
				crit.add(Restrictions.eq("domain.domainId", reportData.get("domainId")));
			}
			if (reportData.get("activityId") != null && reportData.get("activityId") != 0) {
				crit.createAlias("activityId", "activity");
				crit.add(Restrictions.eq("activity.activityId", reportData.get("activityId")));
			}
			if (reportData.get("yearId") != null && reportData.get("yearId") != 0) {
				crit.add(Restrictions.eq("dateYear", reportData.get("yearId")));

			}

			// Dont add filter for Users here , its already added at bottom..

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();
				AllJobsReportDTO reportDTO = new AllJobsReportDTO();
				reportDTO.setJobName(job.getJobName());
				reportDTO.setYear(job.getDateYear() + "");

				// reportDTO.setCompanyName(Branches.ALSUHAIMI.name());

				for (Branches branch : Branches.values()) {
					if (job.getCompany() == branch.getValue()) {
						reportDTO.setCompanyName(branch.getName());
					}
				}

				for (Allocations allocation : Allocations.values()) {
					if (job.getAllocation() == allocation.getValue()) {
						reportDTO.setAllocation(allocation.getName());
					}
				}
				for (Location location : Location.values()) {
					if (job.getLocation() == location.getValue()) {
						reportDTO.setLocation(location.getName());
					}
				}
				float totalHours = 0;
				reportDTO.setDomain(job.getDomainId().getName());
				reportDTO.setYear(job.getDateYear() + "");
				reportDTO.setLineOfService(job.getLineofServiceId().getName());

				reportDTO.setListTimeSheet(
						getActualHours(job.getJobId(), session, reportData.get("monthId"), reportData.get("userId")));
				for (int i = 0; i < getActualHours(job.getJobId(), session, reportData.get("monthId"),
						reportData.get("userId")).size(); i++) {
					reportDTO.setActivity(

							getActualHours(job.getJobId(), session, reportData.get("monthId"), reportData.get("userId"))
									.get(i).getActivity().getActivityName());

					totalHours = totalHours + getActualHours(job.getJobId(), session, reportData.get("monthId"),
							reportData.get("userId")).get(i).getHours();

					reportDTO.setUser(
							getActualHours(job.getJobId(), session, reportData.get("monthId"), reportData.get("userId"))
									.get(i).getUserId().getName());
					// budhethours is being used for month
					reportDTO.setBudgetedHours(
							getActualHours(job.getJobId(), session, reportData.get("monthId"), reportData.get("userId"))
									.get(i).getMonth());

				}
				reportDTO.setTotalHours(totalHours);
				reportDTO.setHoursVariance(reportDTO.getBudgetedHours() - reportDTO.getHoursWorked());

				jobReports.add(reportDTO);

				// }

			}

			FileOutputStream fileOut = new FileOutputStream(rootDir + "/FullReport/report.xls");
			HSSFWorkbook workbook = new HSSFWorkbook();
			allJobReport(jobReports, workbook, reportData.get("userId"), reportData.get("jobId"), rootDir);

			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();

		} catch (Exception ex) {

			System.out.println("fail fetchAllReport");
		}
		session.close();
		return "All report file generated";

	}

	//
	public String fetchAllReportPDF(HashMap<String, Integer> reportData, String rootDir) {
		ArrayList<AllJobsReportDTO> jobReports = new ArrayList<AllJobsReportDTO>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			int month = 0;

			// send all the listboxes values from clientside in a hashmap
			// HashMap<String, Integer>

			if (reportData.get("jobId") != null && reportData.get("jobId") != 0) { // EXAMPLE
																					// FOR
																					// adding
																					// a
																					// filter
																					// for
																					// job
																					// name
																					// listbox,
																					// need
																					// to
																					// do
																					// this
																					// for
																					// all
																					// listboxes.
				crit.add(Restrictions.eq("jobId", reportData.get("jobId")));
			}

			if (reportData.get("lineOfServiceId") != null && reportData.get("lineOfServiceId") != 0) { // Example
																										// for
																										// line
																										// of
																										// service
				crit.createAlias("lineofServiceId", "lineofService");
				crit.add(Restrictions.eq("lineofService.lineofServiceId", reportData.get("lineOfServiceId")));
			}

			if (reportData.get("companyId") != null && reportData.get("companyId") != 0) {
				crit.add(Restrictions.eq("company", reportData.get("companyId")));

			}
			if (reportData.get("monthId") != null && reportData.get("monthId") != 0) {
				month = reportData.get("monthId");
			}
			if (reportData.get("allocationId") != null && reportData.get("allocationId") != 0) {
				crit.add(Restrictions.eq("allocation", reportData.get("allocationId")));

			}

			if (reportData.get("domainId") != null && reportData.get("domainId") != 0) {
				crit.createAlias("domainId", "domain");
				crit.add(Restrictions.eq("domain.domainId", reportData.get("domainId")));
			}
			if (reportData.get("activityId") != null && reportData.get("activityId") != 0) {
				crit.createAlias("activityId", "activity");
				crit.add(Restrictions.eq("activity.activityId", reportData.get("activityId")));
			}
			// new for year
			if (reportData.get("yearId") != null && reportData.get("yearId") != 0) {
				crit.add(Restrictions.eq("dateYear", reportData.get("yearId")));

			}

			// Dont add filter for Users here , its already added at bottom..

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();
				AllJobsReportDTO reportDTO = new AllJobsReportDTO();
				reportDTO.setJobName(job.getJobName());
				reportDTO.setYear(job.getDateYear() + "");
				// reportDTO.setCompanyName(Branches.ALSUHAIMI.name());

				for (Branches branch : Branches.values()) {
					if (job.getCompany() == branch.getValue()) {
						reportDTO.setCompanyName(branch.getName());
					}
				}

				for (Allocations allocation : Allocations.values()) {
					if (job.getAllocation() == allocation.getValue()) {
						reportDTO.setAllocation(allocation.getName());
					}
				}
				for (Location location : Location.values()) {
					if (job.getLocation() == location.getValue()) {
						reportDTO.setLocation(location.getName());
					}
				}
				float totalHours = 0;
				reportDTO.setDomain(job.getDomainId().getName());
				reportDTO.setLineOfService(job.getLineofServiceId().getName());

				reportDTO.setListTimeSheet(
						getActualHours(job.getJobId(), session, reportData.get("monthId"), reportData.get("userId")));
				for (int i = 0; i < getActualHours(job.getJobId(), session, reportData.get("monthId"),
						reportData.get("userId")).size(); i++) {
					reportDTO.setActivity(

							getActualHours(job.getJobId(), session, reportData.get("monthId"), reportData.get("userId"))
									.get(i).getActivity().getActivityName());

					totalHours = totalHours + getActualHours(job.getJobId(), session, reportData.get("monthId"),
							reportData.get("userId")).get(i).getHours();

					reportDTO.setUser(
							getActualHours(job.getJobId(), session, reportData.get("monthId"), reportData.get("userId"))
									.get(i).getUserId().getName());
					// budhethours is being used for month
					reportDTO.setBudgetedHours(
							getActualHours(job.getJobId(), session, reportData.get("monthId"), reportData.get("userId"))
									.get(i).getMonth());

				}
				reportDTO.setTotalHours(totalHours);
				reportDTO.setHoursVariance(reportDTO.getBudgetedHours() - reportDTO.getHoursWorked());

				jobReports.add(reportDTO);

				// }

			}

			FileOutputStream fileOut = new FileOutputStream(rootDir + "/FullReport/report.xls");
			HSSFWorkbook workbook = new HSSFWorkbook();
			allJobReportPDF(jobReports, workbook, reportData.get("userId"), reportData.get("jobId"), rootDir);

		} catch (Exception ex) {

			System.out.println("fail fetchAllReport");
		}
		session.close();
		return "All report file generated";

	}

	private void allJobReport(ArrayList<AllJobsReportDTO> jobReports, HSSFWorkbook workbook, Integer userId,
			Integer jobId, String rootDir) throws DocumentException, IOException {
		HSSFSheet worksheet = workbook.createSheet("All Jobs Report");
		HSSFRow rowUserName = worksheet.createRow((short) 0);
		HSSFRow rowHeading = worksheet.createRow((short) 1);

		if (userId != 0) {
			User user;
			try {
				user = fetchUser(userId);
				if (jobId != 0) {
					rowUserName.createCell((short) 0).setCellValue(
							"Report For " + user.getName() + "For Job    " + jobReports.get(0).getJobName());

				} else {
					rowUserName.createCell((short) 0).setCellValue("    Report For " + user.getName());

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (jobId != 0) {
			rowUserName.createCell((short) 0).setCellValue("Report For " + jobReports.get(0).getJobName());

		} else {
			rowUserName.createCell((short) 0).setCellValue("Report For All Jobs  Report For All Users");

		}

		rowHeading.createCell((short) 0).setCellValue("Sr.");
		rowHeading.createCell((short) 1).setCellValue("Job Name");
		rowHeading.createCell((short) 2).setCellValue("Company Name");
		// rowHeading.createCell((short) 3).setCellValue("Hours Worked");
		// rowHeading.createCell((short) 4).setCellValue("Budgeted Hours");
		rowHeading.createCell((short) 4).setCellValue("Month");
		// rowHeading.createCell((short) 5).setCellValue("Hours variance");
		rowHeading.createCell((short) 5).setCellValue("  Day");
		rowHeading.createCell((short) 6).setCellValue("  Day");
		rowHeading.createCell((short) 7).setCellValue("Allocation");
		rowHeading.createCell((short) 8).setCellValue("Line Of Service");
		rowHeading.createCell((short) 10).setCellValue("Domain");
		rowHeading.createCell((short) 12).setCellValue("Users");
		rowHeading.createCell((short) 13).setCellValue("Activity");
		rowHeading.createCell((short) 14).setCellValue("Hours Worked");
		rowHeading.createCell((short) 15).setCellValue("Activity in Month");
		rowHeading.createCell((short) 16).setCellValue("Activity on Job");
		rowHeading.createCell((short) 17).setCellValue("Total Hours on the Job");

		int rowNum = 2;
		int count = 0;
		HashMap<Integer, HashMap<Integer, Float>> mapActivityCount = getAcitivityCount(jobReports);
		HashMap<Integer, HashMap<Integer, Float>> mapActivityCountJob = getAcitivityCountJob(jobReports);

		for (int i = 0; i < jobReports.size(); i++) {

			for (TimeSheet timeSheet : jobReports.get(i).getListTimeSheet()) {

				if (timeSheet.getHours() > 0) {
					count++;
					rowNum = rowNum + 1;
					HSSFRow row = worksheet.createRow((short) rowNum == 1 ? 2 : rowNum == 2 ? 3 : rowNum);

					row.createCell((short) 0).setCellValue(count);
					row.createCell((short) 1).setCellValue(jobReports.get(i).getJobName());
					row.createCell((short) 2).setCellValue(jobReports.get(i).getCompanyName());
					String month = null;
					month = getMonth(timeSheet, i, month);

					row.createCell((short) 4).setCellValue(month);
					row.createCell((short) 5).setCellValue(timeSheet.getDay());
					row.createCell((short) 6).setCellValue(jobReports.get(i).getYear());
					row.createCell((short) 7).setCellValue(jobReports.get(i).getAllocation());
					row.createCell((short) 8).setCellValue(jobReports.get(i).getLineOfService());
					row.createCell((short) 10).setCellValue(jobReports.get(i).getDomain());
					row.createCell((short) 12).setCellValue(timeSheet.getUserId().getName());
					row.createCell((short) 13).setCellValue(timeSheet.getActivity().getActivityName());
					row.createCell((short) 14).setCellValue(timeSheet.getHours());
					HashMap<Integer, Float> mapMonth = mapActivityCount.get(timeSheet.getActivity().getActivityId());
					HashMap<Integer, Float> mapActivityPerJob = mapActivityCountJob
							.get(timeSheet.getActivity().getActivityId());
					float sum = 0.0f;
					for (float f : mapMonth.values()) {
						sum += f;
					}
					row.createCell((short) 15).setCellValue(mapMonth.get(timeSheet.getMonth()));
					row.createCell((short) 16).setCellValue(mapActivityPerJob.get(timeSheet.getJobId().getJobId()));
					row.createCell((short) 17).setCellValue(jobReports.get(i).getTotalHours());

				}
			}

		}

	}

	private void allJobReportPDF(ArrayList<AllJobsReportDTO> jobReports, HSSFWorkbook workbook, Integer userId,
			Integer jobId, String rootDir) throws DocumentException, IOException {
		Rectangle pagesize = new Rectangle(612, 861);
		Document document = new Document(PageSize.A3);
		String title = null;
		if (userId != 0) {
			User user;
			try {
				user = fetchUser(userId);
				if (jobId != 0) {

					title = "Report For " + user.getName() + "For Job    " + jobReports.get(0).getJobName();
				} else {
					title = " Report For " + user.getName();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (jobId != 0) {
			title = "Report For " + jobReports.get(0).getJobName();
		} else {
			title = "Report For All Jobs  Report For All Users";
		}

		PdfPTable table = new PdfPTable(new float[] { 1, 2, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2 });

		table.setWidthPercentage(100);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(new Phrase("Sr", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Job Name", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Company Name", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Month", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Day", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Year", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Allocation", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Line Of Service", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Domain", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("User", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Activity", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Hours Worked", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Activity in Month", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Avtivity on Job", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
		table.addCell(new Phrase("Total Hours on the Job", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));

		table.setHeaderRows(1);
		PdfPCell[] cells = table.getRow(0).getCells();
		for (int j = 0; j < cells.length; j++) {
			cells[j].setBackgroundColor(BaseColor.LIGHT_GRAY);

		}
		int count = 0;
		HashMap<Integer, HashMap<Integer, Float>> mapActivityCount = getAcitivityCount(jobReports);
		HashMap<Integer, HashMap<Integer, Float>> mapActivityCountJob = getAcitivityCountJob(jobReports);
		for (int i = 0; i < jobReports.size(); i++) {

			for (TimeSheet timeSheet : jobReports.get(i).getListTimeSheet()) {

				if (timeSheet.getHours() > 0) {

					/// pdf

					count++;
					table.addCell(new Phrase(count + "", FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(
							new Phrase(jobReports.get(i).getJobName(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(new Phrase(jobReports.get(i).getCompanyName(),
							FontFactory.getFont(FontFactory.HELVETICA, 8)));
					String month = null;
					month = getMonth(timeSheet, i, month);
					table.addCell(new Phrase(month + "", FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(new Phrase(timeSheet.getDay() + "", FontFactory.getFont(FontFactory.HELVETICA, 8)));

					table.addCell(
							new Phrase(jobReports.get(i).getYear(), FontFactory.getFont(FontFactory.HELVETICA, 8)));

					table.addCell(new Phrase(jobReports.get(i).getAllocation(),

							FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(new Phrase(jobReports.get(i).getLineOfService(),
							FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(
							new Phrase(jobReports.get(i).getDomain(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(
							new Phrase(timeSheet.getUserId().getName(), FontFactory.getFont(FontFactory.HELVETICA, 8)));

					table.addCell(new Phrase(timeSheet.getActivity().getActivityName(),
							FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(new Phrase(timeSheet.getHours() + "", FontFactory.getFont(FontFactory.HELVETICA, 8)));
					HashMap<Integer, Float> mapMonth = mapActivityCount.get(timeSheet.getActivity().getActivityId());
					HashMap<Integer, Float> mapActivityPerJob = mapActivityCountJob
							.get(timeSheet.getActivity().getActivityId());

					float sum = 0.0f;
					for (float f : mapMonth.values()) {
						sum += f;
					}
					table.addCell(new Phrase(mapMonth.get(timeSheet.getMonth()) + "",
							FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(new Phrase(mapActivityPerJob.get(timeSheet.getJobId().getJobId()) + "",
							FontFactory.getFont(FontFactory.HELVETICA, 8)));
					table.addCell(new Phrase(jobReports.get(i).getTotalHours() + "",
							FontFactory.getFont(FontFactory.HELVETICA, 8)));

				}
			}

		}

		FileOutputStream pdfFile = new FileOutputStream(rootDir + "/FullReport/report.pdf");
		// PdfWriter.getInstance(document, pdfFile);
		PdfWriter pdfWriter = PdfWriter.getInstance(document, pdfFile);
		HeaderAndFooterPdfPageEventHelper headerAndFooter = new HeaderAndFooterPdfPageEventHelper();
		pdfWriter.setPageEvent(headerAndFooter);
		document.open();

		Paragraph paragraph = new Paragraph(title,
				FontFactory.getFont(FontFactory.TIMES_BOLD, 16, Font.BOLD, BaseColor.BLUE));

		Paragraph p = new Paragraph();
		document.add(paragraph);
		document.add(new Paragraph(
				"________________________________________________________________________________________________________________________"));
		document.add(table);
		document.close();

		System.out.println("Done");
	}

	private HashMap<Integer, HashMap<Integer, Float>> getAcitivityCountJob(ArrayList<AllJobsReportDTO> jobReports) {

		HashMap<Integer, HashMap<Integer, Float>> map = new HashMap<Integer, HashMap<Integer, Float>>();
		for (int i = 0; i < jobReports.size(); i++) {

			for (TimeSheet timeSheet : jobReports.get(i).getListTimeSheet()) {

				if (timeSheet.getHours() > 0) {

					try {

						HashMap<Integer, Float> jobMap = map.get(timeSheet.getActivity().getActivityId());
						if (jobMap != null) {

							float alreadySavedJobHours = jobMap.get(timeSheet.getJobId().getJobId()) == null ? 0
									: jobMap.get(timeSheet.getJobId().getJobId());

							jobMap.put(timeSheet.getJobId().getJobId(), timeSheet.getHours() + alreadySavedJobHours);
						} else {
							jobMap = new HashMap<Integer, Float>();
							jobMap.put(timeSheet.getJobId().getJobId(), timeSheet.getHours());
						}
						map.put(timeSheet.getActivity().getActivityId(), jobMap);

					} catch (Exception ex) {
						// month.put(jobReports.get(i).getBudgetedHours(),
						// timeSheet.getHours());
						// map.put(timeSheet.getActivity().getActivityId(),
						// month);

					}
				}
			}
		}
		return map;
	}

	private HashMap<Integer, HashMap<Integer, Float>> getAcitivityCount(ArrayList<AllJobsReportDTO> jobReports) {

		HashMap<Integer, HashMap<Integer, Float>> map = new HashMap<Integer, HashMap<Integer, Float>>();

		for (int i = 0; i < jobReports.size(); i++) {

			for (TimeSheet timeSheet : jobReports.get(i).getListTimeSheet()) {

				if (timeSheet.getHours() > 0) {

					try {

						HashMap<Integer, Float> month = map.get(timeSheet.getActivity().getActivityId());
						if (month != null) {

							float alreadySavedMonthHours = month.get(timeSheet.getMonth()) == null ? 0
									: month.get(timeSheet.getMonth());

							month.put(timeSheet.getMonth(), timeSheet.getHours() + alreadySavedMonthHours);
						} else {
							month = new HashMap<Integer, Float>();
							month.put(timeSheet.getMonth(), timeSheet.getHours());
						}
						map.put(timeSheet.getActivity().getActivityId(), month);

					} catch (Exception ex) {
						// month.put(jobReports.get(i).getBudgetedHours(),
						// timeSheet.getHours());
						// map.put(timeSheet.getActivity().getActivityId(),
						// month);

					}
				}
			}
		}
		return map;
	}

	private String getMonth(TimeSheet timeSheet, int i, String month) {
		if (timeSheet.getMonth() == 1) {
			month = "January";
		}
		if (timeSheet.getMonth() == 2) {
			month = "Februrary";
		}
		if (timeSheet.getMonth() == 3) {
			month = "March";
		}
		if (timeSheet.getMonth() == 4) {
			month = "April";
		}
		if (timeSheet.getMonth() == 5) {
			month = "May";
		}
		if (timeSheet.getMonth() == 6) {
			month = "June";
		}
		if (timeSheet.getMonth() == 7) {
			month = "July";
		}
		if (timeSheet.getMonth() == 8) {
			month = "August";
		}
		if (timeSheet.getMonth() == 9) {
			month = "September";
		}
		if (timeSheet.getMonth() == 10) {
			month = "October";
		}
		if (timeSheet.getMonth() == 11) {
			month = "November";
		}
		if (timeSheet.getMonth() == 12) {
			month = "December";
		}
		return month;
	}

	private void specificUserReport(HSSFWorkbook workbook, int userId, List rsList, Session session) {

		ArrayList<AllJobsReportDTO> jobReports = new ArrayList<AllJobsReportDTO>();
		try {
			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();
				AllJobsReportDTO reportDTO = new AllJobsReportDTO();
				reportDTO.setJobName(job.getJobName());

				for (Branches branch : Branches.values()) {
					if (job.getCompany() == branch.getValue()) {
						reportDTO.setCompanyName(branch.getName());
					}
				}

				for (Allocations allocation : Allocations.values()) {
					if (job.getAllocation() == allocation.getValue()) {
						reportDTO.setAllocation(allocation.getName());
					}
				}
				for (Location location : Location.values()) {
					if (job.getLocation() == location.getValue()) {
						reportDTO.setLocation(location.getName());
					}
				}
				int month = 0;
				float totalHours = 0;
				// reportDTO.setDomain(job.getDomainId().getName());
				// reportDTO.setLineOfService(job.getLineofServiceId().getName());
				// reportDTO.setBudgetedHours(getBudgetedHours(job.getJobId(),
				// session, month));
				// reportDTO.setHoursWorked(getAcitualHours(job.getJobId(),
				// session, month));
				// reportDTO.setListTimeSheet(getActualHours(job.getJobId(),
				// session, month));
				// for (int i = 0; i < getActualHours(job.getJobId(), session,
				// month).size(); i++) {
				// reportDTO.setActivity(
				//
				// getActualHours(job.getJobId(), session,
				// month).get(i).getActivity().getActivityName());
				//
				// totalHours = totalHours + getActualHours(job.getJobId(),
				// session, month).get(i).getHours();
				// // totalHours = getActualHours(job.getJobId(), session,
				// // month).get(i).getHours();
				//
				// reportDTO.setUser(getActualHours(job.getJobId(), session,
				// month).get(i).getUserId().getName());
				// // budhethours is being used for month
				// reportDTO.setBudgetedHours(getActualHours(job.getJobId(),
				// session, month).get(i).getMonth());
				// // reportDTO.setListTimeSheet(getActualHours(job.getJobId(),
				// // session, month).get(i));
				// }
				// reportDTO.setCompanyName(Branches.ALSUHAIMI.name());
				// reportDTO.setAllocation(Allocations.CHARGEABLE.getName());
				reportDTO.setDomain(job.getDomainId().getName());
				// reportDTO.setLocation(Location.LOCAL.getName());
				reportDTO.setLineOfService(job.getLineofServiceId().getName());
				reportDTO.setBudgetedHours(getBudgetedHoursForUser(job.getJobId(), session, userId));
				reportDTO.setHoursWorked(getActualHoursForUser(job.getJobId(), session, userId));
				reportDTO.setHoursVariance(reportDTO.getBudgetedHours() - reportDTO.getHoursWorked());
				jobReports.add(reportDTO);
			}

			HSSFSheet worksheet = workbook.createSheet("User Report");

			HSSFRow rowUserName = worksheet.createRow((short) 0);
			HSSFRow rowHeading = worksheet.createRow((short) 1);

			User user = (User) session.get(User.class, userId);
			if (user != null)
				rowUserName.createCell((short) 0).setCellValue("Report for : " + user.getName());
			rowHeading.createCell((short) 0).setCellValue("Sr.");
			rowHeading.createCell((short) 1).setCellValue("Job Name");
			rowHeading.createCell((short) 2).setCellValue("Company Name");
			rowHeading.createCell((short) 3).setCellValue("Location");
			// rowHeading.createCell((short) 4).setCellValue("Month");
			rowHeading.createCell((short) 4).setCellValue("Hours Worked");
			rowHeading.createCell((short) 5).setCellValue("Budgeted Hours");
			rowHeading.createCell((short) 6).setCellValue("Hours variance");
			rowHeading.createCell((short) 7).setCellValue("Allocation");
			rowHeading.createCell((short) 8).setCellValue("Line Of Service");
			rowHeading.createCell((short) 9).setCellValue("Domain");

			// rowHeading.createCell((short) 0).setCellValue("Sr.");
			// rowHeading.createCell((short) 1).setCellValue("Job Name");
			// rowHeading.createCell((short) 2).setCellValue("Company Name");
			// // rowHeading.createCell((short) 3).setCellValue("Hours Worked");
			// // rowHeading.createCell((short) 4).setCellValue("Budgeted
			// Hours");
			// rowHeading.createCell((short) 4).setCellValue("Month");
			// // rowHeading.createCell((short) 5).setCellValue("Hours
			// variance");
			// rowHeading.createCell((short) 6).setCellValue("Allocation");
			// rowHeading.createCell((short) 7).setCellValue("Line Of Service");
			// rowHeading.createCell((short) 9).setCellValue("Domain");
			// rowHeading.createCell((short) 11).setCellValue("Usere");
			// rowHeading.createCell((short) 12).setCellValue("Activity");
			// rowHeading.createCell((short) 13).setCellValue("Hours Worked");
			// rowHeading.createCell((short) 14).setCellValue("Total Hours");

			for (int i = 0; i < jobReports.size(); i++) {
				HSSFRow row = worksheet.createRow((short) i + 2);
				row.createCell((short) 0).setCellValue(i + 1 + "");
				row.createCell((short) 1).setCellValue(jobReports.get(i).getJobName());
				row.createCell((short) 2).setCellValue(jobReports.get(i).getCompanyName());
				row.createCell((short) 3).setCellValue(jobReports.get(i).getLocation());
				// row.createCell((short)
				// 4).setCellValue(jobReports.get(i).getMonth());
				row.createCell((short) 4).setCellValue(jobReports.get(i).getHoursWorked());
				row.createCell((short) 5).setCellValue(jobReports.get(i).getBudgetedHours());
				row.createCell((short) 6).setCellValue(jobReports.get(i).getHoursVariance());
				row.createCell((short) 7).setCellValue(jobReports.get(i).getAllocation());
				row.createCell((short) 8).setCellValue(jobReports.get(i).getLineOfService());
				row.createCell((short) 9).setCellValue(jobReports.get(i).getDomain());
			}

			// int rowNum = 1;
			// for (int i = 0; i < jobReports.size(); i++) {
			// for (int j = 0; j < jobReports.get(i).getListTimeSheet().size();
			// j++) {
			// TimeSheet timeSheet =
			// jobReports.get(i).getListTimeSheet().get(j);
			// if (timeSheet.getHours() > 0) {
			// rowNum = rowNum + 2;
			// HSSFRow row = worksheet.createRow((short) rowNum == 1 ? 2 :
			// rowNum == 2 ? 3 : rowNum);
			// row.createCell((short) 0).setCellValue(i + 1 + "");
			// row.createCell((short)
			// 1).setCellValue(jobReports.get(i).getJobName());
			// row.createCell((short)
			// 2).setCellValue(jobReports.get(i).getCompanyName());
			// // row.createCell((short)
			// // 3).setCellValue(jobReports.get(i).getHoursWorked());
			// row.createCell((short)
			// 4).setCellValue(jobReports.get(i).getBudgetedHours());
			// // row.createCell((short)
			// // 5).setCellValue(jobReports.get(i).getHoursVariance());
			// row.createCell((short)
			// 6).setCellValue(jobReports.get(i).getAllocation());
			// row.createCell((short)
			// 7).setCellValue(jobReports.get(i).getLineOfService());
			// row.createCell((short)
			// 9).setCellValue(jobReports.get(i).getDomain());
			// // row.createCell((short)
			// // 11).setCellValue(jobReports.get(i).getUser());
			// row.createCell((short)
			// 11).setCellValue(timeSheet.getUserId().getName());
			// row.createCell((short)
			// 12).setCellValue(timeSheet.getActivity().getActivityName());
			// row.createCell((short) 13).setCellValue(timeSheet.getHours());
			// row.createCell((short)
			// 14).setCellValue(jobReports.get(i).getTotalHours());
			//
			// }
			// }
			// }
		} catch (Exception ex) {
			System.out.println("fail specific user report" + ex.getLocalizedMessage());
		}
	}

	private float getBudgetedHours(int jobId, Session session, int month) {
		Criteria crit = session.createCriteria(JobActivityEntity.class);
		crit.createAlias("jobId", "job");
		crit.add(Restrictions.eq("job.jobId", jobId));

		List rsList = crit.list();
		int totalHours = 0;
		for (Iterator it = rsList.iterator(); it.hasNext();) {

			JobActivityEntity jobActivityEntity = (JobActivityEntity) it.next();
			int total = jobActivityEntity.getPlanning() + jobActivityEntity.getExecution()
					+ jobActivityEntity.getReporting() + jobActivityEntity.getFollowup();
			totalHours = totalHours + total;
		}
		return totalHours;
	}

	private ArrayList<TimeSheet> getActualHours(int jobId, Session session, int month, int userId) {
		Criteria crit = session.createCriteria(TimeSheet.class);
		ArrayList<TimeSheet> listTimeSheet = new ArrayList<TimeSheet>();
		crit.createAlias("jobId", "job");
		crit.createAlias("userId", "user");
		// crit.createAlias("month", "month");
		crit.add(Restrictions.eq("job.jobId", jobId));

		/// for specific user
		if (userId != 0) {
			crit.add(Restrictions.eq("user.userId", userId));
		}
		if (month != 0) {
			crit.add(Restrictions.eq("month", month));
		}

		///

		// if (month != 0) {
		// crit.add(Restrictions.eq("month", month));
		// }
		List rsList = crit.list();
		float totalHours = 0;
		for (Iterator it = rsList.iterator(); it.hasNext();) {

			TimeSheet timeSheet = (TimeSheet) it.next();
			totalHours = totalHours + timeSheet.getHours();
			if (!(totalHours == 0)) {
				listTimeSheet.add(timeSheet);
			}

		}
		// listTimeSheet.add(totalHours);
		return listTimeSheet;
	}

	//
	private float getBudgetedHoursForUser(int jobId, Session session, int userId) {
		Criteria crit = session.createCriteria(JobActivityEntity.class);
		crit.createAlias("jobId", "job");
		crit.add(Restrictions.eq("job.jobId", jobId));
		crit.createAlias("userId", "user");
		crit.add(Restrictions.eq("user.userId", userId));
		List rsList = crit.list();
		int totalHours = 0;
		for (Iterator it = rsList.iterator(); it.hasNext();) {

			JobActivityEntity jobActivityEntity = (JobActivityEntity) it.next();
			int total = jobActivityEntity.getPlanning() + jobActivityEntity.getExecution()
					+ jobActivityEntity.getReporting() + jobActivityEntity.getFollowup();
			totalHours = totalHours + total;
		}
		return totalHours;
	}

	private float getActualHoursForUser(int jobId, Session session, int userId) {
		Criteria crit = session.createCriteria(TimeSheet.class);
		crit.createAlias("jobId", "job");
		crit.add(Restrictions.eq("job.jobId", jobId));
		crit.createAlias("userId", "user");
		crit.add(Restrictions.eq("user.userId", userId));
		List rsList = crit.list();
		float totalHours = 0;
		for (Iterator it = rsList.iterator(); it.hasNext();) {

			TimeSheet timeSheet = (TimeSheet) it.next();
			totalHours = totalHours + timeSheet.getHours();

		}
		return totalHours;
	}

	public String fetchJobWiseReport(HashMap<String, Integer> reportData, String rootDir) {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			ArrayList<JobActivityEntity> jobActivities = new ArrayList<JobActivityEntity>();
			ArrayList<TimeSheet> timeSheets = new ArrayList<TimeSheet>();
			int jobId = reportData.get("jobId"); // value from job name listbox

			Job job = (Job) session.get(Job.class, jobId);

			/*
			 * Criteria critJob = session.createCriteria(Job.class);
			 * critJob.add(Restrictions.eq("jobId", jobId));
			 * if(reportData.get("companyId") != null &&
			 * reportData.get("companyId") != 0){
			 * critJob.add(Restrictions.eq("companyId",
			 * reportData.get("companyId"))); }
			 */

			Criteria crit = session.createCriteria(JobActivityEntity.class);
			crit.createAlias("jobId", "job");
			crit.add(Restrictions.eq("job.jobId", jobId));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {

				JobActivityEntity jobActivityEntity = (JobActivityEntity) it.next();
				jobActivities.add(jobActivityEntity);

			}

			Criteria crit1 = session.createCriteria(TimeSheet.class);
			crit1.createAlias("jobId", "job");
			crit1.add(Restrictions.eq("job.jobId", jobId));
			List rsList1 = crit1.list();

			for (Iterator it = rsList1.iterator(); it.hasNext();) {

				TimeSheet timeSheet = (TimeSheet) it.next();
				timeSheets.add(timeSheet);

			}

			FileOutputStream fileOut = new FileOutputStream(rootDir + "/JobWiseReport/report.xls");// "D:\\POI111.xls"
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet worksheet = workbook.createSheet("Job wise Report");
			HSSFRow rowJobName = worksheet.createRow((short) 0);
			HSSFRow rowHeading = worksheet.createRow((short) 2);
			HSSFRow rowHeading2 = worksheet.createRow((short) 3);
			rowJobName.createCell((short) 0).setCellValue("Job Name :");
			rowJobName.createCell((short) 1).setCellValue(job.getJobName());

			String companyName = "";
			for (Branches branch : Branches.values()) {
				if (job.getCompany() == branch.getValue()) {
					companyName = branch.getName();
				}
			}

			String locationName = "";
			for (Location location : Location.values()) {
				if (job.getLocation() == location.getValue()) {
					locationName = location.getName();
				}
			}

			rowJobName.createCell((short) 3).setCellValue("Job Location :");
			rowJobName.createCell((short) 4).setCellValue(locationName);

			rowJobName.createCell((short) 5).setCellValue("Company Name :");
			rowJobName.createCell((short) 6).setCellValue(companyName);

			// row.createCell((short) 0).setCellValue("Job Start Date :");
			// row.createCell((short) 1).setCellValue(Location.valueOf(arg0));

			rowHeading.createCell((short) 1).setCellValue("Planning");
			rowHeading.createCell((short) 3).setCellValue("Execution");
			rowHeading.createCell((short) 5).setCellValue("Reporting");
			rowHeading.createCell((short) 7).setCellValue("Follow up ");
			rowHeading.createCell((short) 9).setCellValue("Total hours");

			rowHeading2.createCell((short) 1).setCellValue("Budget");
			rowHeading2.createCell((short) 2).setCellValue("Actual");
			rowHeading2.createCell((short) 3).setCellValue("Budget");
			rowHeading2.createCell((short) 4).setCellValue("Actual");
			rowHeading2.createCell((short) 5).setCellValue("Budget");
			rowHeading2.createCell((short) 6).setCellValue("Actual");
			rowHeading2.createCell((short) 7).setCellValue("Budget");
			rowHeading2.createCell((short) 8).setCellValue("Actual");
			rowHeading2.createCell((short) 9).setCellValue("Budget");
			rowHeading2.createCell((short) 10).setCellValue("Actual");
			rowHeading2.createCell((short) 11).setCellValue("Variance");

			int currentUserId = 0;
			HSSFRow row1 = null;

			///////// TIME ACTIVITY
			for (int i = 0; i < jobActivities.size(); i++) {
				float totalBudgeted = 0;
				row1 = worksheet.createRow((short) i + 4);
				currentUserId = jobActivities.get(i).getUserId().getUserId();
				String name = jobActivities.get(i).getUserId().getName();
				row1.createCell((short) 0).setCellValue(name);
				// planning
				totalBudgeted = totalBudgeted + jobActivities.get(i).getPlanning();
				row1.createCell((short) 1).setCellValue(jobActivities.get(i).getPlanning() + "");
				// Execution
				totalBudgeted = totalBudgeted + jobActivities.get(i).getExecution();
				row1.createCell((short) 3).setCellValue(jobActivities.get(i).getExecution() + "");
				// Reporting
				totalBudgeted = totalBudgeted + jobActivities.get(i).getReporting();
				row1.createCell((short) 5).setCellValue(jobActivities.get(i).getReporting() + "");
				// Followup
				totalBudgeted = totalBudgeted + jobActivities.get(i).getFollowup();
				row1.createCell((short) 7).setCellValue(jobActivities.get(i).getFollowup() + "");
				// Total
				row1.createCell((short) 9).setCellValue(totalBudgeted + "");

				///////// TIME SHEET
				for (int j = 0; j < timeSheets.size(); j++) {
					float totalActual = 0;

					int userId = timeSheets.get(j).getUserId().getUserId();

					if (userId == currentUserId) {
						if (row1 != null) {
							if (row1.getCell((short) 2) == null) {// planning
								totalActual = totalActual + timeSheets.get(j).getHours();
								row1.createCell((short) 2).setCellValue(timeSheets.get(j).getHours() + "");
							} else if (row1.getCell((short) 4) == null) {// execution
								totalActual = totalActual + timeSheets.get(j).getHours();
								row1.createCell((short) 4).setCellValue(timeSheets.get(j).getHours() + "");
							} else if (row1.getCell((short) 6) == null) { // reporting
								totalActual = totalActual + timeSheets.get(j).getHours();
								row1.createCell((short) 6).setCellValue(timeSheets.get(j).getHours() + "");
							} else if (row1.getCell((short) 8) == null) { // follow
																			// up
								totalActual = totalActual + timeSheets.get(j).getHours();
								row1.createCell((short) 8).setCellValue(timeSheets.get(j).getHours() + "");
							}

						}
					}

					row1.createCell((short) 10).setCellValue(totalActual + "");
					row1.createCell((short) 11).setCellValue(totalBudgeted - totalActual + "");
					// currentUserId = userId;
				}
			}
			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();

		} catch (Exception ex) {
			System.out.println("fail fetch job wise report");

		}
		session.close();
		return "job WIse report generated";
	}

	/*
	 * public String exportToExcel(ArrayList<ExcelDataDTO> excelDataList, String
	 * rootDir) { try {
	 * 
	 * FileOutputStream fileOut = new FileOutputStream(rootDir +
	 * "/InternalAuditReport/report.xls");// "D:\\POI111.xls" HSSFWorkbook
	 * workbook = new HSSFWorkbook(); HSSFSheet worksheet =
	 * workbook.createSheet("Orders Worksheet"); HSSFRow row =
	 * worksheet.createRow((short) 0); row.createCell((short)
	 * 0).setCellValue("Objective"); row.createCell((short)
	 * 1).setCellValue("Auditable Unit"); row.createCell((short)
	 * 2).setCellValue("Domain"); row.createCell((short)
	 * 3).setCellValue("Division"); row.createCell((short)
	 * 4).setCellValue("Risk Assesment");
	 * 
	 * for (int i = 0; i < excelDataList.size(); i++) { HSSFRow row1 =
	 * worksheet.createRow((short) i + 1); row1.createCell((short)
	 * 0).setCellValue(excelDataList.get(i).getObjective());
	 * row1.createCell((short)
	 * 1).setCellValue(excelDataList.get(i).getAuditableUnit());
	 * row1.createCell((short)
	 * 2).setCellValue(excelDataList.get(i).getDomain());
	 * row1.createCell((short)
	 * 3).setCellValue(excelDataList.get(i).getDivision());
	 * row1.createCell((short)
	 * 4).setCellValue(excelDataList.get(i).getRiskAssesment());
	 * 
	 * } workbook.write(fileOut); fileOut.flush(); fileOut.close();
	 * 
	 * logger.info(String.
	 * format("(Inside exportToExcel)exporting ToExcel for rootDir" + rootDir +
	 * "for excel data" + excelDataList + "" + new Date()));
	 * 
	 * } catch (FileNotFoundException e) { e.printStackTrace(); } catch
	 * (IOException e) { e.printStackTrace(); }
	 * System.out.println("excel sheet: downloaded"); return "exported"; }
	 */
	public ArrayList<TimeSheetReportDTO> fetchTimeReport(int selectedJob, int selectedMonth, int selectedUser,
			int selectedJobType, String rootDir) {
		Session session = null;
		int numberOfHours = 0;

		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put("jobId", selectedJob);
		generatExcelTimeReport(map, rootDir);

		ArrayList<TimeSheetReportDTO> timeSheetlist = new ArrayList<TimeSheetReportDTO>();
		try {
			session = sessionFactory.openSession();

			Criteria crit = session.createCriteria(TimeSheet.class);

			crit.createAlias("userId", "user");
			crit.createAlias("user.roleId", "role");
			crit.createAlias("user.companyId", "company");
			crit.createAlias("jobId", "job");
			crit.createAlias("job.lineofServiceId", "lineofService1");
			// crit.createAlias("job.subLineofServiceId", "subLineofService1");
			// crit.createAlias("job.supervisorId", "supervisor");
			crit.createAlias("job.domainId", "domain1");
			crit.createAlias("job.countryId", "count1");
			// crit.createAlias("job.principalConsultantId",
			// "principalConsultant");

			if (selectedJob != 0) {
				crit.add(Restrictions.eq("job.jobId", selectedJob));
			}
			if (selectedMonth != 0) {
				crit.add(Restrictions.eq("month", selectedMonth));
			}
			if (selectedUser != 0) {
				crit.add(Restrictions.eq("user.userId", selectedUser));
			}

			if (selectedJobType != 0) {
				crit.add(Restrictions.eq("lineofService1.lineofServiceId", selectedJobType));
			}

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				boolean matchFound = false;
				TimeSheet timeSheet = (TimeSheet) it.next();
				TimeSheetReportDTO timeSheetReportDTO = new TimeSheetReportDTO();
				timeSheetReportDTO.setJobName(timeSheet.getJobId().getJobName());
				// timeSheetReportDTO.setFee(timeSheet.getJobId().getClientFee());
				timeSheetReportDTO.setJobType(timeSheet.getJobId().getLineofServiceId().getName());
				timeSheetReportDTO.setJobId(timeSheet.getJobId().getJobId());
				timeSheetReportDTO.setTotal(timeSheet.getHours());
				int userCost = timeSheet.getUserId().getRoleId().getChargeRate();

				for (int i = 0; i < timeSheetlist.size(); i++) {

					if (timeSheetlist.get(i).getJobId() == timeSheet.getJobId().getJobId()) {

						if (timeSheetlist.get(i).getUsersList() == null
								|| timeSheetlist.get(i).getUsersList().size() < 1) {
							addNewuserTotheJob(timeSheet, userCost, timeSheetlist.get(i));
						} else {
							boolean userMatchFound = false;
							float totalTimeCost = 0;
							for (int j = 0; j < timeSheetlist.get(i).getUsersList().size(); j++) {
								if (timeSheetlist.get(i).getUsersList().get(j).getUserId() == timeSheet.getUserId()
										.getUserId()) {
									timeSheetlist.get(i).getUsersList().get(j)
											.setActualHours(timeSheetlist.get(i).getUsersList().get(j).getActualHours()
													+ timeSheet.getHours());
									timeSheetlist.get(i).getUsersList().get(j).setTimeCost(
											userCost * timeSheetlist.get(i).getUsersList().get(j).getActualHours());
									timeSheetlist.get(i)
											.setTotal(timeSheetlist.get(i).getTotal() + timeSheet.getHours());
									totalTimeCost = totalTimeCost
											+ timeSheetlist.get(i).getUsersList().get(j).getTimeCost();
									timeSheetlist.get(i).setTimeCost(totalTimeCost);
									timeSheetlist.get(i).setActualRecoveryRate(
											timeSheetlist.get(i).getFee() / timeSheetlist.get(i).getTotal());

									userMatchFound = true;
									break;
								}
								// else{
								// addNewuserTotheJob(timeSheet, userCost,
								// timeSheetlist.get(i));
								// }
							}
							if (!userMatchFound) {
								addNewuserTotheJob(timeSheet, userCost, timeSheetlist.get(i));
								timeSheetlist.get(i).setTotal(timeSheetlist.get(i).getTotal() + timeSheet.getHours());
								timeSheetlist.get(i).setActualRecoveryRate(
										timeSheetlist.get(i).getFee() / timeSheetlist.get(i).getTotal());

							}
						}
						matchFound = true;
					}
				}
				if (!matchFound) {
					timeSheetlist.add(timeSheetReportDTO);
					addNewuserTotheJob(timeSheet, userCost, timeSheetReportDTO);
				}

				// numberOfHours = numberOfHours+ timeSheet.getHours();

			}

		} catch (Exception ex) {
			System.out.println("fail job delte");
		}
		// return "Total Number of hours worked: " + numberOfHours;
		return timeSheetlist;
	}

	private void addNewuserTotheJob(TimeSheet timeSheet, int userCost, TimeSheetReportDTO timeSheetReportDTO) {
		UserReportDTO userReportDTO = new UserReportDTO();
		userReportDTO.setActualHours(timeSheet.getHours());
		// userReportDTO.setBudgetedHours(budgetedHours);
		userReportDTO.setTimeCost(timeSheet.getHours() * userCost);
		userReportDTO.setUserId(timeSheet.getUserId().getUserId());
		userReportDTO.setUserName(timeSheet.getUserId().getName());
		timeSheetReportDTO.setTimeCost(userReportDTO.getTimeCost());
		timeSheetReportDTO.getUsersList().add(userReportDTO);
	}

	// public String saveRating(int attributeId, String rating, int score, int
	// userId)throws Exception {
	// Session session = null;
	// try{
	// session = sessionFactory.openSession();
	// JobAttributes jobAttributes = (JobAttributes)
	// session.get(JobAttributes.class, attributeId);
	// jobAttributes.setScore(score);
	// jobAttributes.setRating(rating);
	// jobAttributes.setUserId(userId);
	// session.update(jobAttributes);
	// session.flush();
	// return "rating saved";
	// }catch(Exception ex){
	// System.out.println("fail saveRating");
	// throw ex;
	// }
	// }

	public String saveRating(AttributeRating attributeRating) throws Exception {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(AttributeRating.class);
			crit.add(Restrictions.eq("attributeId", attributeRating.getAttributeId()));
			crit.add(Restrictions.eq("userId", attributeRating.getUserId()));

			List rsList = crit.list();

			if (rsList.size() > 0) {
				AttributeRating attributeRat = (AttributeRating) rsList.get(0);
				attributeRat.setRating(attributeRating.getRating());
				attributeRat.setScore(attributeRating.getScore());
				attributeRat.setLevel(attributeRating.getScore());
				session.update(attributeRat);

			} else {
				session.save(attributeRating);
			}
			session.flush();

			// boolean found = false;
			// JobAttributes jobAttributes = null;
			// for(Iterator it=rsList.iterator();it.hasNext();)
			// {
			// jobAttributes = (JobAttributes)it.next();
			// jobAttributes.setScore(score);
			// jobAttributes.setRating(rating);
			// if(jobAttributes.getUserId() == userId){
			// found = true;
			// }
			// }
			//
			// if(found){
			// session.update(jobAttributes);
			// session.flush();
			// }else{
			// int jobId = jobAttributes.getJobId();
			// int levelSaved = jobAttributes.getLevel();
			// int scoreSaved = jobAttributes.getScore();
			//
			// String nameSaved = jobAttributes.getName();
			// String ratingSaved = jobAttributes.getRating();
			//
			// saveNewRating(jobId, levelSaved,score,userId,nameSaved, rating );
			// }
			//

			return "rating saved";
		} catch (Exception ex) {
			System.out.println("fail saveRating");
			throw ex;
		}
	}

	private void saveNewRating(int jobId, int levelSaved, int scoreSaved, int userSaved, String nameSaved,
			String ratingSaved) {
		Session session = null;
		try {
			session = sessionFactory.openSession();

			JobAttributes jobAttributes = new JobAttributes();
			jobAttributes.setScore(scoreSaved);
			jobAttributes.setRating(ratingSaved);
			jobAttributes.setUserId(userSaved);
			jobAttributes.setName(nameSaved);
			jobAttributes.setLevel(levelSaved);
			jobAttributes.setJobId(jobId);
			session.save(jobAttributes);
			session.flush();
		} catch (Exception ex) {
			System.out.println("error saving new attribute" + ex);
		}

	}

	public Integer fetchMonthAllowedhours(int month) throws Exception {
		Session session = null;
		int allowedhours = 0;
		try {

			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(MonthAllowedHours.class);
			crit.add(Restrictions.eq("month", month));
			MonthAllowedHours monthAllowedHours = (MonthAllowedHours) crit.list().get(0);
			allowedhours = monthAllowedHours.getHours();
			return allowedhours;
		} catch (Exception ex) {
			throw ex;
		}

	}

	public ArrayList<Job> tim(User loggedInUser) throws Exception {
		ArrayList<Job> jobs = new ArrayList<Job>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			crit.createAlias("lineofServiceId", "lineofService");
			// crit.createAlias("subLineofServiceId", "subLineofService");
			// crit.createAlias("subLineofService.domainId", "sublineDomain");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "ddomainlineofservice");
			//
			crit.createAlias("domainId", "domain");
			crit.createAlias("domain.lineofServiceId", "domainlineofservice");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "subdomainlineofservice");

			crit.createAlias("countryId", "count");
			// crit.createAlias("userId", "user");
			// crit.createAlias("user.roleId", "role");
			// crit.createAlias("user.companyId", "company");

			// crit.createAlias("supervisorId", "supervisor");
			// crit.createAlias("supervisor.roleId", "roles");
			// crit.createAlias("supervisor.companyId", "companys");
			//

			// crit.createAlias("principalConsultantId", "principalConsultant");
			// crit.createAlias("principalConsultant.roleId", "rolep");
			// crit.createAlias("principalConsultant.companyId", "companyp");

			crit.add(Restrictions.ne("status", "InActive"));
			// crit.add(Restrictions.eq("client", "office"));

			if (loggedInUser.getRoleId().getRoleId() != 5) {
				ArrayList<Integer> jobIds = getUserJobs(loggedInUser.getUserId(), session);
				Disjunction disc = Restrictions.disjunction();
				for (int i = 0; i < jobIds.size(); i++) {
					disc.add(Restrictions.eq("jobId", jobIds.get(i)));
				}
				crit.add(disc);

			}
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();
				// job.setJobPhases(fetchJobPhases(job.getJobId()));
				job.setJobEmployeesList(fetchJobEmployees(session, job.getJobId()));
				job.setJobAttributes(fetchjobAttributes(session, job.getJobId()));
				job.setTimeSheets(fetchJobTimeSheets(session, job.getJobId(), loggedInUser.getRoleId().getRoleId(),
						loggedInUser.getUserId()));
				// job.setUsersList(fetchUserList(session, job.getJobId()));
				jobs.add(job);
			}

			return jobs;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchJobForTimeSheets", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchJobForTimeSheets" + ex.getMessage());

			throw new Exception("Exception occured in fetchJobForTimeSheets");
		} finally {
			session.close();
		}
	}

	public ArrayList<JobUsersDTO> fetchUsersWithJobs() throws Exception {
		ArrayList<Job> jobs = new ArrayList<Job>();
		Session session = null;
		ArrayList<JobUsersDTO> jobUsersDTOs = new ArrayList<JobUsersDTO>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			crit.createAlias("lineofServiceId", "lineofService");
			// crit.createAlias("subLineofServiceId", "subLineofService");
			// crit.createAlias("subLineofService.domainId", "sublineDomain");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "ddomainlineofservice");
			crit.createAlias("domainId", "domain");
			crit.createAlias("lineofService.domainId", "domainlineofservice");
			crit.createAlias("countryId", "count");
			// crit.createAlias("supervisorId", "supervisor");
			// crit.createAlias("supervisor.roleId", "roles");
			// crit.createAlias("supervisor.companyId", "companys");
			// crit.createAlias("principalConsultantId", "principalConsultant");
			// crit.createAlias("principalConsultant.roleId", "rolep");
			// crit.createAlias("principalConsultant.companyId", "companyp");
			crit.add(Restrictions.ne("status", "InActive"));
			crit.add(Restrictions.ne("status", "Closed"));

			List rsList = crit.list();
			//
			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();
				ArrayList<User> users = fetchUsersonSelectedJob(job.getJobId());
				JobUsersDTO jobUsersDTO = new JobUsersDTO();
				jobUsersDTO.setJob(job);
				jobUsersDTO.setUsers(users);
				// job.setJobPhases(fetchJobPhases(job.getJobId()));
				job.setJobEmployeesList(fetchJobEmployees(session, job.getJobId()));
				job.setJobAttributes(fetchjobAttributes(session, job.getJobId()));
				// job.setTimeSheets(fetchJobTimeSheets(session, job.getJobId(),
				// loggedInUser.getRoleId().getRoleId()));
				// jobs.add(job);
				jobUsersDTOs.add(jobUsersDTO);

			}

			return jobUsersDTOs;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchUsersWithJobs", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchUsersWithJobs" + ex.getMessage());

			throw new Exception("Exception occured in fetchUsersWithJobs");
		} finally {
			session.close();
		}
	}

	public ArrayList<User> fetchUsersonSelectedJob(int jobId) throws Exception {
		Session session = null;
		ArrayList<User> users = new ArrayList<User>();
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(JobEmployees.class);
			crit.createAlias("employeeId", "employee");
			crit.createAlias("employee.companyId", "employeeComp");
			crit.createAlias("employee.roleId", "employeeRole");
			crit.add(Restrictions.eq("jobId", jobId));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				JobEmployees JobEmployees = (JobEmployees) it.next();
				User user = JobEmployees.getEmployeeId();
				users.add(user);
			}
			return users;
		} catch (Exception ex) {
			System.out.println("fail : fetchUsersonSelectedJob: " + ex);
			throw ex;
		}

	}

	public ArrayList<AttributeRating> fetchjobUserRating(int userId, int jobId) throws Exception {
		ArrayList<AttributeRating> jobAttributesList = new ArrayList<AttributeRating>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(AttributeRating.class);
			crit.add(Restrictions.eq("jobId", jobId));
			crit.add(Restrictions.eq("userId", userId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				AttributeRating attributeRating = (AttributeRating) it.next();
				jobAttributesList.add(attributeRating);
			}
			return jobAttributesList;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchjobAttributes", ex.getMessage()), ex);
			System.out.println("Exception occured in AttributeRating" + ex.getMessage());

			throw new Exception("Exception occured in AttributeRating");
		}

	}

	public ArrayList<Job> fetchAllJobs() throws Exception {
		ArrayList<Job> jobs = new ArrayList<Job>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			crit.createAlias("lineofServiceId", "lineofService");
			// crit.createAlias("subLineofServiceId", "subLineofService");
			// crit.createAlias("subLineofService.domainId", "sublineDomain");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "ddomainlineofservice");

			crit.createAlias("domainId", "domain");
			crit.createAlias("lineofService.domainId", "domainlineofservice");
			// crit.createAlias("sublineDomain.lineofServiceId",
			// "subdomainlineofservice");

			crit.createAlias("countryId", "count");
			// crit.createAlias("userId", "user");
			// crit.createAlias("user.roleId", "role");
			// crit.createAlias("user.companyId", "company");

			// crit.createAlias("supervisorId", "supervisor");
			// crit.createAlias("supervisor.roleId", "roles");
			// crit.createAlias("supervisor.companyId", "companys");
			//
			//
			// crit.createAlias("principalConsultantId", "principalConsultant");
			// crit.createAlias("principalConsultant.roleId", "rolep");
			// crit.createAlias("principalConsultant.companyId", "companyp");

			// crit.add(Restrictions.ne("status", "InActive"));
			// crit.add(Restrictions.ne("client", "office"));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();
				// job.setJobPhases(fetchJobPhases(job.getJobId()));
				job.setJobEmployeesList(fetchJobEmployees(session, job.getJobId()));
				job.setJobAttributes(fetchjobAttributes(session, job.getJobId()));
				// job.setTimeSheets(fetchJobTimeSheets(session, job.getJobId(),
				// loggedInUser.getRoleId().getRoleId()));
				HibernateDetachUtility.nullOutUninitializedFields(job,
						HibernateDetachUtility.SerializationType.SERIALIZATION);

				jobs.add(job);
			}

			return jobs;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchALLJobs", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchAllJobs" + ex.getMessage());

			throw new Exception("Exception occured in fetchAllJobs");
		} finally {
			session.close();
		}
	}

	public String closeJob(int jobId) {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			crit.add(Restrictions.eq("jobId", jobId));
			Job job = (Job) crit.list().get(0);
			job.setStatus("Closed");
			session.update(job);
			session.flush();
			return "job closed";
		} catch (Exception ex) {
			throw ex;
		} finally {
			session.close();
		}
	}

	public ArrayList<LineofService> fetchLineOfService(int domainId) throws Exception {
		ArrayList<LineofService> listLineOfService = new ArrayList<LineofService>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(LineofService.class);
			crit.createAlias("domainId", "domain");
			crit.add(Restrictions.eq("domain.domainId", domainId));
			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				LineofService lineOfService = (LineofService) it.next();
				listLineOfService.add(lineOfService);
			}

			return listLineOfService;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in getLineOfService", ex.getMessage()), ex);
			System.out.println("Exception occured in getLineOfService" + ex.getMessage());

			throw new Exception("Exception occured in getLineOfService");
		} finally {

		}

	}
	// public String fetchJobWiseReport(HashMap<String,Integer> map) {
	//
	// ArrayList<AllJobsReportDTO> jobReports = new
	// ArrayList<AllJobsReportDTO>();
	// Session session = null;
	// try{
	// session = sessionFactory.openSession();
	// Criteria crit = session.createCriteria(Job.class);
	//
	// //send all the listboxes values from clientside in a hashmap
	// HashMap<String, Integer>
	//
	// if(map.get("jobId") != null && map.get("jobId") != 0){ // EXAMPLE FOR
	// adding a filter for job name listbox, need to do this for all listboxes.
	// crit.add(Restrictions.eq("jobId", map.get("jobId")));
	// }
	////
	//// if(reportData.get("lineOfServiceId") != null && reportData.get("jobId")
	// != 0){ //Example for line of service
	//// crit.createAlias("lineofServiceId", "lineofService");
	//// crit.add(Restrictions.eq("lineofService.lineofServiceId",
	// reportData.get("lineOfServiceId")));
	//// }
	//
	// //Dont add filter for Users here , its already added at bottom..
	//
	// List rsList = crit.list();
	//
	// for(Iterator it=rsList.iterator();it.hasNext();)
	// {
	// Job job = (Job)it.next();
	// AllJobsReportDTO reportDTO = new AllJobsReportDTO();
	// reportDTO.setJobName(job.getJobName());
	// reportDTO.setCompanyName(Branches.ALSUHAIMI.name());
	// reportDTO.setAllocation(Allocations.CHARGEABLE.getName());
	// reportDTO.setDomain(job.getDomainId().getName());
	// reportDTO.setLocation(Location.LOCAL.getName());
	// reportDTO.setLineOfService(job.getLineofServiceId().getName());
	// reportDTO.setBudgetedHours(getBudgetedHours(job.getJobId(), session));
	// reportDTO.setHoursWorked(getActualHours(job.getJobId(), session));
	// reportDTO.setHoursVariance(reportDTO.getBudgetedHours() -
	// reportDTO.getHoursWorked());
	// jobReports.add(reportDTO);
	// }
	//
	// FileOutputStream fileOut = new FileOutputStream(rootDir +
	// "/FullReport/report.xls");
	// HSSFWorkbook workbook = new HSSFWorkbook();
	// allJobReport(jobReports, workbook);
	//
	// if(reportData.get("userId") != null){ //Example for line of service
	// specificUserReport(workbook, reportData.get("userId"), rsList, session);
	// }
	// specificUserReport(workbook, 1, rsList, session);
	//
	// workbook.write(fileOut);
	// fileOut.flush();
	// fileOut.close();
	//
	//
	//
	// }catch(Exception ex){
	//
	// System.out.println("fail fetchAllReport");

	// }
	// session.close();
	//
	// }

	public ArrayList<Activity> fetchActivityReport(int lineOfServiceId) {
		ArrayList<Activity> listActivities = new ArrayList<Activity>();
		Session session = null;
		try {
			session = sessionFactory.openSession();

			Criteria crit = session.createCriteria(Activity.class);
			// crit.createAlias("activityId", "activity");
			crit.createAlias("lineofServiceId", "lineOfService");
			crit.createAlias("lineOfService.domainId", "domainId");
			crit.add(Restrictions.eq("lineOfService.lineofServiceId", lineOfServiceId));
			// crit.add(Restrictions.eq("lineOfService.lineofServiceId", line));
			// crit.add(Restrictions.eq("domainId",
			// job.getLineofServiceId().getDomainId().getDomainId()));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Activity activity = (Activity) it.next();

				// activity.setTimeSheets(fetchActivityTimeSheets(session,
				// activity.getActivityId(), roleId, userId));

				listActivities.add(activity);
			}
			return listActivities;
		} catch (Exception ex) {
			System.out.println("fail : fetchActivities: " + ex);
			throw ex;
		}
	}

	public ArrayList<Job> fetchJobsWithStatus(String status) throws Exception {

		ArrayList<Job> jobs = new ArrayList<Job>();
		Session session = null;
		try {
			session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(Job.class);
			// crit.createAlias("lineofServiceId", "lineofService");
			// // crit.createAlias("subLineofServiceId", "subLineofService");
			// // crit.createAlias("subLineofService.domainId",
			// "sublineDomain");
			// // crit.createAlias("sublineDomain.lineofServiceId",
			// // "ddomainlineofservice");
			//
			// crit.createAlias("domainId", "domain");
			// crit.createAlias("lineofService.domainId",
			// "domainlineofservice");
			// // crit.createAlias("sublineDomain.lineofServiceId",
			// // "subdomainlineofservice");
			//
			// crit.createAlias("countryId", "count");
			// crit.createAlias("userId", "user");
			// crit.createAlias("user.roleId", "role");
			// crit.createAlias("user.companyId", "company");

			// crit.createAlias("supervisorId", "supervisor");
			// crit.createAlias("supervisor.roleId", "roles");
			// crit.createAlias("supervisor.companyId", "companys");
			//
			//
			// crit.createAlias("principalConsultantId", "principalConsultant");
			// crit.createAlias("principalConsultant.roleId", "rolep");
			// crit.createAlias("principalConsultant.companyId", "companyp");

			// crit.add(Restrictions.ne("status", "InActive"));
			// crit.add(Restrictions.ne("client", "office"));

			List rsList = crit.list();

			for (Iterator it = rsList.iterator(); it.hasNext();) {
				Job job = (Job) it.next();

				HibernateDetachUtility.nullOutUninitializedFields(job,
						HibernateDetachUtility.SerializationType.SERIALIZATION);
				// job.setJobPhases(fetchJobPhases(job.getJobId()));
				// job.setJobEmployeesList(fetchJobEmployees(session,
				// job.getJobId()));
				// job.setJobAttributes(fetchjobAttributes(session,
				// job.getJobId()));
				// // job.setTimeSheets(fetchJobTimeSheets(session,
				// job.getJobId(),
				// // loggedInUser.getRoleId().getRoleId()));
				// HibernateDetachUtility.nullOutUninitializedFields(job,
				// HibernateDetachUtility.SerializationType.SERIALIZATION);
				if (job.getStatus().equalsIgnoreCase(status)) {
					jobs.add(job);
				}
			}

			return jobs;
		} catch (Exception ex) {
			logger.warn(String.format("Exception occured in fetchJobsWithStatus", ex.getMessage()), ex);
			System.out.println("Exception occured in fetchJobsWithStatus" + ex.getMessage());

			throw new Exception("Exception occured in fetchJobsWithStatus");
		} finally {
			session.close();
		}

	}

}
