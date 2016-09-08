/**
 *
 */
package com.wellsfargo.isg.wireless.web.mba.controllers.deposit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.ArrayUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;

import com.miteksystems.DepositCheckResult;
import com.miteksystems.PhoneVendorServiceSoap;
import com.wellsfargo.isg.shared.system.Domain;
import com.wellsfargo.isg.shared.system.context.Context;
import com.wellsfargo.isg.shared.system.context.CtxtField;
import com.wellsfargo.isg.shared.system.context.RequestCtxt;
import com.wellsfargo.isg.wireless.app.AppDescriptor;
import com.wellsfargo.isg.wireless.app.AppDescriptor.AppId;
import com.wellsfargo.isg.wireless.dao.FeaturesDao;
import com.wellsfargo.isg.wireless.dao.deposit.DepositDao;
import com.wellsfargo.isg.wireless.dao.deposit.DepositLimitsDao;
import com.wellsfargo.isg.wireless.delegates.aps.AuthorizeAcctListForDesktopDepositDelegate;
import com.wellsfargo.isg.wireless.delegates.aps.AuthorizeAcctListForDesktopDepositDelegate.AuthorizeAcctListForDesktopDepositDTO;
import com.wellsfargo.isg.wireless.delegates.aps.AuthorizeAcctListForDesktopDepositDelegate.AuthorizeAcctListForDesktopDepositInputDTO;
import com.wellsfargo.isg.wireless.delegates.aps.AuthorizeAcctListForDesktopDepositDelegate.ForEachCustAcctAcsResponseListTypeDTO;
import com.wellsfargo.isg.wireless.delegates.tms.DepositDelegate;
import com.wellsfargo.isg.wireless.delegates.tms.DepositDelegate.DepositInputDTO;
import com.wellsfargo.isg.wireless.delegates.tms.DepositDelegate.DepositOutputDTO;
import com.wellsfargo.isg.wireless.impl.dao.deposit.DepositDaoWithExceptionHandler;
import com.wellsfargo.isg.wireless.impl.delegates.tms.DepositDelegateImpl;
import com.wellsfargo.isg.wireless.impl.model.AddressImpl;
import com.wellsfargo.isg.wireless.impl.model.ProductMarketCode;
import com.wellsfargo.isg.wireless.impl.model.TransactionSummaryImpl;
import com.wellsfargo.isg.wireless.impl.model.ViewImpl;
import com.wellsfargo.isg.wireless.impl.service.AccountServiceImpl;
import com.wellsfargo.isg.wireless.impl.service.FeaturesServiceImpl;
import com.wellsfargo.isg.wireless.impl.service.deposit.CheckImageServiceImpl;
import com.wellsfargo.isg.wireless.impl.service.deposit.DepositLimitsServiceImpl;
import com.wellsfargo.isg.wireless.impl.service.deposit.DepositServiceImpl;
import com.wellsfargo.isg.wireless.impl.service.deposit.EnrollmentServiceImpl;
import com.wellsfargo.isg.wireless.impl.service.deposit.MemoPostServiceImpl;
import com.wellsfargo.isg.wireless.impl.service.deposit.MitekServiceImpl;
import com.wellsfargo.isg.wireless.impl.service.deposit.TimeOnBookServiceImpl;
import com.wellsfargo.isg.wireless.mock.model.AccountMock;
import com.wellsfargo.isg.wireless.mock.model.CustomerMock;
import com.wellsfargo.isg.wireless.model.Account;
import com.wellsfargo.isg.wireless.model.AccountId;
import com.wellsfargo.isg.wireless.model.Address;
import com.wellsfargo.isg.wireless.model.ApplicationMode;
import com.wellsfargo.isg.wireless.model.ApsAccountProduct;
import com.wellsfargo.isg.wireless.model.Enrollment;
import com.wellsfargo.isg.wireless.model.Features;
import com.wellsfargo.isg.wireless.model.MobileContext;
import com.wellsfargo.isg.wireless.model.TransactionAmount;
import com.wellsfargo.isg.wireless.model.TransactionAmountFormatIndicator;
import com.wellsfargo.isg.wireless.model.TransactionSummary;
import com.wellsfargo.isg.wireless.model.View;
import com.wellsfargo.isg.wireless.model.deposit.DepositAccount;
import com.wellsfargo.isg.wireless.model.deposit.DepositData;
import com.wellsfargo.isg.wireless.service.DelegateImplUnpackResponseInvoker;
import com.wellsfargo.isg.wireless.service.Services;
import com.wellsfargo.isg.wireless.service.deposit.DepositLimitsService;
import com.wellsfargo.isg.wireless.util.DepositCheckEnd2EndTests;
import com.wellsfargo.isg.wireless.web.mba.controllers.deposit.CheckDepositException.ExceptionReason;
import com.wellsfargo.isg.wireless.web.mba.model.HttpSessionMobileCache;
import com.wellsfargo.isg.wireless.web.mba.model.MobileCache;
import com.wellsfargo.isg.wireless.web.mba.model.UserProfile;
import com.wellsfargo.isg.wireless.web.mba.model.UserSessionAttributes;
import com.wellsfargo.mwf.testingframework.springframework.CleverApplicationContext;
import com.wellsfargo.service.provider.tms.mobilebanking.message._2015._01.DepositResponseType;

/**
 * @author Karthiga Baskaran
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Services.class, Context.class })
// To overcome LinkageError and NoSuchAlgorithm Exceptions due to inclusion of
// PowerMock,
// Need to add PowerMockIgnore the affected packages,
// This is related to the SSLCOntext loading stuff from the upstream classloader
// - which is Power Mock's (i.e BusinessEventJAXBBuilder) one when running the
// test.
// References:http://mathieuhicauber-java.blogspot.com/2013/07/powermock-and-ssl-context.html
@PowerMockIgnore({ "com.sun.xml.internal.messaging.saaj.soap.*",
		"javax.xml.namespace.*", "javax.xml.soap.*",
		"com.sun.xml.internal.ws.model.*", "javax.xml.datatype.*",
		"org.apache.xerces.jaxp.datatype.*", "javax.xml.bind.*",
		"javax.xml.transform.*", "org.xml.*", "org.w3c.*",
		"javax.management.*", "sun.security.ssl.*", "javax.xml.ws.spi.*",
		"javax.xml.stream.*", "javax.xml.parsers.*", "javax.xml.validation.*",
		"javax.crypto.*", "javax.net.ssl.*", "com.sun.xml.internal.ws.spi.*",
		"com.wellsfargo.secure.connect.mq.*", "com.ibm.mq.*", "javax.xml.ws.*",
		"javax.xml.ws.handler.*" })
public class DepositControllerTest {
	// Exception Rule to capture exceptions and validate
	@Rule
	public ExpectedException assertException = ExpectedException.none();

	// Declaration of Static Fields: Start
	private static CleverApplicationContext context;
	private static DepositController depositController;
	private static String DEPOSIT_INFO = "DepositInfo";
	private static String DEPOSIT_DATA = "depositData";
	private static String DEPOSIT_FORM = "depositForm";
	private static String FILE_PATH = "C:\\Users\\U476998\\Projects\\MRDC\\Cheque Images\\";
	// Declaration of Static Fields: End

	// Declaration of Local Private Fields which gets instantiated on every test
	// run : Start
	private String mibi;
	private HttpSessionMobileCache mobileCache;
	private MockHttpServletRequest mockRequest;
	private MockHttpSession mocksession;
	private DepositDaoWithExceptionHandler depositDaoWithExceptionHandler;
	private PhoneVendorServiceSoap phoneVendorServiceSoap;
	private DepositDao depositDao;
	private ModelMap map;
	private DepositData depositData;
	private DepositDelegate depositDelegate;
	private MessageSource messageSource;

	private EnrollmentServiceImpl mrdcEnrollmentService;

	private FeaturesServiceImpl featuresService;

	private FeaturesDao featuresDao;

	private AccountServiceImpl accountService;

	private AuthorizeAcctListForDesktopDepositDelegate authorizeAcctListForDesktopDepositDelegate;

	private DepositLimitsServiceImpl depositLimitsService;

	private TimeOnBookServiceImpl timeOnBookService;

	private DepositLimitsDao depositLimitsDao;

	// Declaration of Local Private Fields which gets instantiated on every test
	// run : End

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		context = new CleverApplicationContext(new String[] {
				"classpath:/applicationContext-mba-config.xml",
				"classpath:/applicationContext-service.xml",
				"classpath:/applicationContext-service-enrollment.xml",
				"classpath:/applicationContext-mba-schedulerhelper.xml",
				"classpath:/applicationContext-downloadableApp.xml",
				"classpath:/applicationContext-cas-sso.xml",
				"classpath:/applicationContext-service-crashReport.xml",
				"classpath:/mba-service-config.xml",
				"classpath:/service-config.xml",
				"classpath:/applicationContext-mba-security.xml",
				"classpath:/applicationContext-frontPorch.xml",
				"classpath:/applicationContext-twoFA.xml",
				"classpath:/dispatcher-servlet.xml",
				"classpath:/applicationContext-mba-forbiddenwords.xml" });
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		depositData = new DepositData();
		map = new ModelMap();

		depositController = (DepositController) context
				.getBean("depositController");

		// Mock Objects: Start
		mockRequest = new MockHttpServletRequest();
		mocksession = new MockHttpSession();
		depositDelegate = Mockito.mock(DepositDelegateImpl.class);
		depositDaoWithExceptionHandler = Mockito
				.mock(DepositDaoWithExceptionHandler.class);
		depositDao = Mockito.mock(DepositDao.class);
		depositLimitsDao = Mockito.mock(DepositLimitsDao.class);
		featuresDao = Mockito.mock(FeaturesDao.class);

		// Mitek Service delegate where the images are ebing processed using OCR
		// (Mitek third party services)
		phoneVendorServiceSoap = Mockito.mock(PhoneVendorServiceSoap.class);
		// Mock Objects: End

		mockRequest.setSession(mocksession);
		mobileCache = new HttpSessionMobileCache(mocksession);
		messageSource = (MessageSource) context.getBean("messageSource");
		featuresService = (FeaturesServiceImpl) context
				.getBean("featuresService");
		accountService = (AccountServiceImpl) context.getBean("accountService");
		mrdcEnrollmentService = (EnrollmentServiceImpl) context
				.getBean("mrdcEnrollmentService");
		authorizeAcctListForDesktopDepositDelegate = Mockito
				.mock(AuthorizeAcctListForDesktopDepositDelegate.class);

		timeOnBookService = Mockito.mock(TimeOnBookServiceImpl.class);
		context.overrideBean("timeOnBookService", timeOnBookService);

		// Mock DAO calls to override actual DB call to mock call
		context.overrideBean("depositDao", depositDao);
		depositDaoWithExceptionHandler.setDepositDao(depositDao);
		context.overrideBean("depositLimitsDao", depositLimitsDao);
		context.overrideBean("depositDaoWithExceptionHandler",
				depositDaoWithExceptionHandler);
		context.overrideBean("featuresDao", featuresDao);
		context.overrideBean("authorizeAcctListForDesktopDepositDelegate",
				authorizeAcctListForDesktopDepositDelegate);
		depositLimitsService = (DepositLimitsServiceImpl) context
				.getBean("depositLimitsService");

		depositLimitsService.setDepositLimitsDao(depositLimitsDao);
		// Mock Status Services Class
		PowerMockito.mockStatic(Services.class);

		// Overide featuresService class
		featuresService.setFeaturesDao(featuresDao);
		context.overrideBean("featuresService", featuresService);

		// Mock Services to return mrdcEnrollmentService
		mrdcEnrollmentService
				.setAuthorizeAcctListForDesktopDepositDelegate(authorizeAcctListForDesktopDepositDelegate);
		PowerMockito.when(Services.getMrdcEnrollmentService()).thenReturn(
				mrdcEnrollmentService);

		// Override the Memo service with mocked DB objects and delegates
		MemoPostServiceImpl memoPostDepositService = (MemoPostServiceImpl) context
				.getBean("memoPostDepositService");
		memoPostDepositService.setDepositDao(depositDao);
		memoPostDepositService
				.setDepositDaoWithExceptionHandler(depositDaoWithExceptionHandler);
		memoPostDepositService.setDepositDelegate(depositDelegate);
		context.overrideBean("memoPostDepositService", memoPostDepositService);

		// Override the mitek service with mocked delegates
		MitekServiceImpl mitekService = (MitekServiceImpl) context
				.getBean("mitekService");
		mitekService.setPhoneVendorServiceSoap(phoneVendorServiceSoap);
		context.overrideBean("mitekService", mitekService);

		// Override the checkimage service with mocked delegates and Db objects
		CheckImageServiceImpl checkImageService = (CheckImageServiceImpl) context
				.getBean("checkImageService");
		checkImageService.setMitekService(mitekService);
		checkImageService
				.setDepositDaoWithExceptionHandler(depositDaoWithExceptionHandler);
		context.overrideBean("checkImageService", checkImageService);

		// Override the deposit service with mocked delegates and Db objects
		DepositServiceImpl depositService = (DepositServiceImpl) context
				.getBean("depositService");
		depositService
				.setDepositDaoWithExceptionHandler(depositDaoWithExceptionHandler);
		depositService.setDepositDao(depositDao);
		depositService.setCheckImageService(checkImageService);
		depositService.setMemoPostDepositService(memoPostDepositService);
		context.overrideBean("depositService", depositService);

		// Mock the static services call to return overriden account service
		PowerMockito.when(Services.getAccountService()).thenReturn(
				accountService);

		// Mock the static services call to return overriden account service
		PowerMockito.when(Services.getDepositLimitsService()).thenReturn(
				depositLimitsService);

		// Mock the static services call to return overriden timeonbook service
		PowerMockito.when(Services.getTimeOnBookService()).thenReturn(
				timeOnBookService);

		// Mock the static services call to return overriden deposit service
		PowerMockito.when(Services.getDepositService()).thenReturn(
				depositService);

		mibi = "{\"CaptureMode\":\"1\",\"Device\":\"iPhone\",\"Document\":\"CheckFront\",\"ImageHeight\":\"532\",\"ImageWidth\":\"1262\",\"Manufacturer\":\"Apple\",\"MiSnapResultCode\":\"SuccessStillCamera\",\"MibiVersion\":\"1.1\",\"Model\":\"iPhone6,   1\",\"OS\":\"8.1.1\",\"Orientation\":\"Landscape\",\"Parameters\":{\"AutoCaptureFailoverToStillCapture\":\"1\",\"CameraBrightness\":\"400\",\"CameraDegreesThreshold\":\"150\",\"CameraGuideImageEnabled\":\"1\",\"CameraInitialTimeoutInSeconds\":\"20\",\"CameraMaxBrightness\":\"700\",\"CameraMaxTimeouts\":\"0\",\"CameraSharpness\":\"300\",\"CameraTimeoutInSeconds\":\"30\",\"CameraViewfinderMinFill\":\"500\",\"CameraViewfinderMinHorizontalFill\":\"800\",\"CameraVignetteImageEnabled\":\"0\",\"CaptureMode\":\"3\",\"GhostImageAlwaysOn\":\"0\",\"MiSnapVersion\":\"2.3.4\",\"Name\":\"CheckFront\",\"RequiredCompressionLevel\":\"50\",\"ShortDescription\":\"DEFAULT ACH Enrollment\",\"SmartBubbleAppearanceDelay\":\"3000\",\"SmartBubbleCumulativeErrorThreshold\":\"3\",\"SmartBubbleEnabled\":\"1\",\"TorchMode\":\"1\",\"TutorialAcknowledgeEnabled\":\"1\",\"UnnecessaryScreenTouchLimit\":\"3\"},\"Platform\":\"iOS\",\"ServerType\":\"UNKNOWN_SERVER_TYPE\",\"ServerVersion\":\"UNKNOWN_SERVER\",\"Torch\":\"AUTO\",\"UXP\":[{\"BF\":\"(379, 12)\"},{\"BF\":\"(704, 369)\"},{\"BF\":\"(786,183)\"},{\"BF\":\"(867,168)\"},{\"BF\":\"(1007,249)\"},{\"NF\":\"(1971,74)\"},{\"NF\":\"(2252,78)\"},{\"FO\":\"(2383)\"},{\"NF\":\"(2516,74)\"},{\"NF\":\"(5516,61)\"},{\"NF\":\"(8153,73)\"},{\"NF\":\"(8992,0)\"},{\"NF\":\"(14491,0)\"},{\"NF\":\"(14565,0)\"},{\"NF\":\"(14640,0)\"},{\"BF\":\"(14715,399)\"},{\"BF\":\"(15087,396)\"},{\"DR\":\"(29293)\"},{\"ST\":\"(29293)\"}]}";
	}

	/**
	 * Clean data after executing the test case
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		mocksession.clearAttributes();
		map.clear();
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily limit exceeded and appropriate
	 * error message
	 * </p>
	 * 
	 * @input - Appropriate session info and deposit amount greater than daily
	 *        limit
	 * @output - Assert Daily limit exceeded
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_VerifyDailyLimitExceeded_MRCC_14()
			throws Exception {
		try {
			// Set User Profile and User Session Attributes
			setupUserProfileAndSessionAttrs(mockRequest, true,
					ProductMarketCode.CONSUMER, false, false);
			// Mock DB Calls
			mockDBCalls();
			// mock daily limit with the total transaction
			BigDecimal totalDailyTransaction = new BigDecimal("2600.00");
			Mockito.when(
					depositLimitsDao.getTotalAmountOnDepositTransactions(
							Mockito.any(AccountId.class),
							Mockito.any(DepositLimitsService.LimitsType.class)))
					.thenReturn(totalDailyTransaction);
			// mock mobile context
			mockMobileContext();
			// mock authorizeAcctListForDesktopDCepositDelegate
			mockAuthorizeAcctListForDesktopDepositDelegate();

			// Set Deposit Session Info
			DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
			List<Account> accounts = new ArrayList<Account>();
			// Create an Account with dummy subcode
			accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
					ApsAccountProduct.MMC, false, true, false));
			depositSessionInfo.setEligibleAccounts(accounts);
			depositSessionInfo.setUserEnrolled(true);
			depositSessionInfo.setTermsAndConditionUptoDate(true);
			map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
			mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
			// Mock the customer account open date less than to 180 days
			mockCustomerAccountOpenDate(Calendar.MONTH, -3);
			/*
			 * depositLimitsDao.getTotalAmountOnDepositTransactions(accountId,
			 * DepositLimitsService.LimitsType.THIRTYDAYLIMIT);
			 */
			depositController.getCommand(mockRequest);
			fail("Expected a CheckDeposit Exception for Limit Exceded");
		} catch (Exception ex) {
			assertThat(ex, instanceOf(CheckDepositException.class));
			assertEquals(((CheckDepositException) ex).getExceptionReason(),
					ExceptionReason.ALL_ACCOUNTS_LIMIT_EXCEEDED);
		}
	}

	/**
	 * <p>
	 * Summary : This test is to test the 30 days limit exceeded and appropriate
	 * error message
	 * </p>
	 * 
	 * @input - Appropriate session info and deposit amount greater than 30 days
	 *        limit
	 * @output - Assert 30 Days limit exceeded
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_Verify30DaysLimitExceeded_MRCC_10()
			throws Exception {
		try {
			// Set User Profile and User Session Attributes
			setupUserProfileAndSessionAttrs(mockRequest, true,
					ProductMarketCode.CONSUMER, false, false);
			// Mock DB Calls
			mockDBCalls();
			// mock daily limit with the total transaction
			BigDecimal totalDailyTransaction = new BigDecimal("5100.00");
			Mockito.when(
					depositLimitsDao.getTotalAmountOnDepositTransactions(
							Mockito.any(AccountId.class),
							Mockito.any(DepositLimitsService.LimitsType.class)))
					.thenReturn(totalDailyTransaction);
			// mock mobile context
			mockMobileContext();
			// mock authorizeAcctListForDesktopDCepositDelegate
			mockAuthorizeAcctListForDesktopDepositDelegate();

			// Set Deposit Session Info
			DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
			List<Account> accounts = new ArrayList<Account>();
			// Create an Account with dummy subcode
			accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
					ApsAccountProduct.MMC, false, true, false));
			depositSessionInfo.setEligibleAccounts(accounts);
			depositSessionInfo.setUserEnrolled(true);
			depositSessionInfo.setTermsAndConditionUptoDate(true);
			map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
			mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
			// Mock the customer account open date less than to 180 days
			mockCustomerAccountOpenDate(Calendar.MONTH, -3);
			/*
			 * depositLimitsDao.getTotalAmountOnDepositTransactions(accountId,
			 * DepositLimitsService.LimitsType.THIRTYDAYLIMIT);
			 */
			depositController.getCommand(mockRequest);
			fail("Expected a CheckDeposit Exception for Limit Exceded");
		} catch (Exception ex) {
			assertThat(ex, instanceOf(CheckDepositException.class));
			assertEquals(((CheckDepositException) ex).getExceptionReason(),
					ExceptionReason.ALL_ACCOUNTS_LIMIT_EXCEEDED);
		}
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Consumer
	 * customer with the bank for Greater than Equal to 180 days and having an
	 * account that is in PMA Account
	 * </p>
	 * 
	 * @input - Consumer Customer, >= 180 Days, PMA Acct , APS ProductCode CHK
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_CONSCust_Grt180_PMAAcct_CHK_MRCC_18()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, false);
		// Mock DB Calls
		mockDBCalls();

		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date more than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Consumer
	 * customer with the bank for Greater than Equal to 180 days and having an
	 * account that is in PMA Account
	 * </p>
	 * 
	 * @input - Consumer Customer, >= 180 Days, PMA Acct , APS ProductCode SAV
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_CONSCust_Grt180_PMAAcct_SAV_MRCC_18()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.SAV, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date more than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Consumer
	 * customer with the bank for less than 180 days and having an account that
	 * is in PMA Account
	 * </p>
	 * 
	 * @input - Consumer Customer, < 180 Days, PMA Acct , APS ProductCode MMC
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_CONSCust_Less180_PMAAcct_MMC_MRCC_19()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.MMC, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Consumer
	 * customer with the bank for Greater than Equal to 180 days and having an
	 * account that is in Business Account
	 * </p>
	 * 
	 * @input - Consumer Customer, >= 180 Days, Business Acct , APS ProductCode
	 *        SAV
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_CONSCust_Grt180_BUSIAcct_SAV_MRCC_20AND21AND11()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.SAV, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date more than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Consumer
	 * customer with the bank for Greater than Equal to 180 days and having an
	 * account that is in Business Account
	 * </p>
	 * 
	 * @input - Consumer Customer, >= 180 Days, Business Acct , APS ProductCode
	 *        CHK
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_CONSCust_Grt180_BUSIAcct_CHK_MRCC_20AND21AND11()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date more than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Consumer
	 * customer with the bank for less than 180 days and having an account that
	 * is in Business Account
	 * </p>
	 * 
	 * @input - Consumer Customer, < 180 Days, Business Acct , APS ProductCode
	 *        CHK
	 * @output - DepositForm should not be null, Daily limit = 5000 & 30 Day
	 *         Limit = 10000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_CONSCust_Less180_BUSIAcct_CHK_MRCC_22()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("10000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for PCG
	 * customer with the bank for Greater than Equal to 180 days and having an
	 * account that is in Business Account
	 * </p>
	 * 
	 * @input - PCG Customer, >= 180 Days, Business Acct , APS ProductCode SAV
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 75000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_PCGCust_Grt180_BUSIAcct_SAV_MRCC_25()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, true);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.SAV, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date more than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for PCG
	 * customer with the bank for Greater than Equal to 180 days and having an
	 * account that is in Business Account
	 * </p>
	 * 
	 * @input - PCG Customer, >= 180 Days, Business Acct , APS ProductCode CHK
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 75000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_PCGCust_Grt180_BUSIAcct_CHK_MRCC_25()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, true);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date more than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for PCG
	 * customer with the bank for less than 180 days and having an account that
	 * is in Business Account
	 * </p>
	 * 
	 * @input - PCG Customer, < 180 Days, Business Acct , APS ProductCode CHK
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 25000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_PCGCust_Less180_BUSIAcct_CHK_MRCC_24()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, true);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for PCG
	 * customer with the bank for less than 180 days and having an account that
	 * is in PMA Account
	 * </p>
	 * 
	 * @input - PCG Customer, >= 180 Days, PMA Acct , APS ProductCode CHK
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 75000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_PCGCust_Grt180_PMAAcct_CHK_MRCC_13()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, true);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date greater than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for PCG
	 * customer with the bank for less than 180 days and having an account that
	 * is in PMA Account
	 * </p>
	 * 
	 * @input - PCG Customer, >= 180 Days, PMA Acct , APS ProductCode SAV
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 75000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_PCGCust_Grt180_PMAAcct_SAV_MRCC_13()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, true);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.SAV, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date greater than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for PCG
	 * customer with the bank for less than 180 days and having an account that
	 * is in PMA Account
	 * </p>
	 * 
	 * @input - PCG Customer, < 180 Days, PMA Acct , APS ProductCode CHK
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 25000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_PCGCust_Less180_PMAAcct_CHK_MRCC_23()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, true);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for PCG
	 * customer with the bank for less than 180 days and having an account that
	 * is in PMA Account
	 * </p>
	 * 
	 * @input - PCG Customer, < 180 Days, PMA Acct , APS ProductCode SAV
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 25000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_PCGCust_Less180_PMAAcct_SAV_MRCC_23()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, true);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.SAV, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for PCG
	 * customer with the bank for less than 180 days and having an account that
	 * is in Business Account
	 * </p>
	 * 
	 * @input - PCG Customer, < 180 Days, Business Acct , APS ProductCode SAV
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 25000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_PCGCust_Less180_BUSIAcct_SAV_MRCC_24()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, false, true);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.SAV, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for TBP
	 * customer with the bank for Greater than Equal to 180 days and having an
	 * account that is in CMD Account
	 * </p>
	 * 
	 * @input - Consumer Customer, >= 180 Days, CMD Acct , APS ProductCode CHK
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 75000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_TBPCust_Grt180_CMDAcct_CHK_MRCC_26AND12()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date more than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for TBP
	 * customer with the bank for Greater than Equal to 180 days and having an
	 * account that is in CMD Account
	 * </p>
	 * 
	 * @input - TBP Customer, >= 180 Days, CMD Acct , APS ProductCode SAV
	 * @output - DepositForm should not be null, Daily limit = 25000 & 30 Day
	 *         Limit = 750000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_TBPCust_Grt180_CMDAcct_SAV_MRCC_26AND12()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.SAV, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date more than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Consumer
	 * customer with the bank for less than 180 days and having an account that
	 * is in PMA Account
	 * </p>
	 * 
	 * @input - Consumer Customer, < 180 Days, PMA Acct , APS ProductCode CHK
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_TBPCust_Less180_CMDdAcct_CHK_MRCC_27()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.CHK, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Business
	 * customer with the bank for less than 180 days and having an account that
	 * is in Command Account
	 * </p>
	 * 
	 * @input - Business Customer, < 180 Days, Command Acct , APS ProductCode
	 *        CHK
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_BUSICust_Less180_CMDdAcct_CHK_MRCC_17()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.BUSINESS, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.BUSINESS,
				ApsAccountProduct.CHK, true, false, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Business
	 * customer with the bank for less than 180 days and having an account that
	 * is in Command Account
	 * </p>
	 * 
	 * @input - Business Customer, < 180 Days, Command Acct , APS ProductCode
	 *        SAV
	 * @output - DepositForm should not be null, Daily limit = 2000 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_BUSICust_Less180_CMDdAcct_SAV_MRCC_17()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.BUSINESS, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.BUSINESS,
				ApsAccountProduct.SAV, true, false, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Business
	 * customer with the bank for less than 180 days and having an account that
	 * is in Business Account
	 * </p>
	 * 
	 * @input - Business Customer, >= 180 Days, Business Acct , APS ProductCode
	 *        CHK
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_BUSICust_Grt180_BUSIAcct_CHK_MRCC_15()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.BUSINESS, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.BUSINESS,
				ApsAccountProduct.CHK, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date greater than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Business
	 * customer with the bank for less than 180 days and having an account that
	 * is in Business Account
	 * </p>
	 * 
	 * @input - Business Customer, >= 180 Days, Business Acct , APS ProductCode
	 *        SAV
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 7500
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_BUSICust_Grt180_BUSIAcct_SAV_MRCC_15()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.BUSINESS, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.BUSINESS,
				ApsAccountProduct.SAV, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date greater than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -7);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("2500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("7500"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Business
	 * customer with the bank for less than 180 days and having an account that
	 * is in Business Account
	 * </p>
	 * 
	 * @input - Business Customer, < 180 Days, Business Acct , APS ProductCode
	 *        CHK
	 * @output - DepositForm should not be null, Daily limit = 5000 & 30 Day
	 *         Limit = 10000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_BUSICust_Less180_BUSIAcct_CHK_MRCC_16()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.BUSINESS, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("BA", ProductMarketCode.BUSINESS,
				ApsAccountProduct.CHK, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("10000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Business
	 * customer with the bank for less than 180 days and having an account that
	 * is in Business Account
	 * </p>
	 * 
	 * @input - Business Customer, < 180 Days, Business Acct , APS ProductCode
	 *        SAV
	 * @output - DepositForm should not be null, Daily limit = 5000 & 30 Day
	 *         Limit = 10000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_BUSICust_Less180_BUSIAcct_SAV_MRCC_16()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.BUSINESS, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.BUSINESS,
				ApsAccountProduct.SAV, false, false, true));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("5000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("10000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test the daily and 30 day limits for Consumer
	 * customer with the bank for less than 180 days and having an account that
	 * is in PMA Account
	 * </p>
	 * 
	 * @input - Consumer Customer, < 180 Days, PMA Acct , APS ProductCode SAV
	 * @output - DepositForm should not be null, Daily limit = 2500 & 30 Day
	 *         Limit = 5000
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	@Test
	@Category({ DepositLimitTests.class })
	public void testgetCommand_TBPCust_Less180_CMDdAcct_SAV_MRCC_27()
			throws Exception {

		// Set User Profile and User Session Attributes
		setupUserProfileAndSessionAttrs(mockRequest, true,
				ProductMarketCode.CONSUMER, true, false);
		// Mock DB Calls
		mockDBCalls();
		// mock mobile context
		mockMobileContext();
		// mock authorizeAcctListForDesktopDCepositDelegate
		mockAuthorizeAcctListForDesktopDepositDelegate();

		// Set Deposit Session Info
		DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
		List<Account> accounts = new ArrayList<Account>();
		// Create an Account with dummy subcode
		accounts.add(setupAccount("NA", ProductMarketCode.CONSUMER,
				ApsAccountProduct.SAV, false, true, false));
		depositSessionInfo.setEligibleAccounts(accounts);
		map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
		mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);
		// Mock the customer account open date less than to 180 days
		mockCustomerAccountOpenDate(Calendar.MONTH, -3);

		DepositForm form = depositController.getCommand(mockRequest);
		assertNotNull(form);
		assertNotNull(mocksession.getAttribute(DEPOSIT_INFO));
		assertNotNull(((DepositSessionInfo) mocksession
				.getAttribute(DEPOSIT_INFO)).getDepositAccounts());
		assertEquals(new BigDecimal("25000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailableDailyLimit());
		assertEquals(new BigDecimal("75000"),
				((DepositSessionInfo) mocksession.getAttribute(DEPOSIT_INFO))
						.getDepositAccounts().get(0).getAvailable30DayLimit());
	}

	/**
	 * <p>
	 * Summary : This test is to test upload front check image
	 * </p>
	 * 
	 * @input - Front check images
	 * @output - The response on successful upload
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckImageUploadTests.class })
	public void testsubmitFrontImage_goGood() throws Exception {
		DepositResponse response = submitImage("frontImage", FILE_PATH
				+ "CheckFront3100_ProperFormat.JPG", false);

		assertNotNull(response);
		assertEquals("success", response.getStatus().toLowerCase());
		assertEquals("", response.getErrorMsg());
	}

	/**
	 * <p>
	 * Summary : This test is to test upload front check image with filesize
	 * exceeded
	 * </p>
	 * 
	 * @input - Front check images and filesize exceeded attribute to true
	 * @output - System exception with file size message being thrown
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckImageUploadTests.class })
	public void testsubmitFrontImage_FileSizeExceeded() throws Exception {
		DepositResponse response = null;
		try {
			response = submitImage("frontImage", FILE_PATH
					+ "CheckFront3100_ProperFormat.JPG", true);
		} catch (Exception e) {
			assertThat(e.getMessage(),
					equalTo("Uplaoded Front image size exceeded"));
		}
		assertNull(response);
	}

	/**
	 * <p>
	 * Summary : This test is to test upload front check image with proper
	 * extension i.e JPEG
	 * </p>
	 * 
	 * @input - Front check images and filesize exceeded attribute to false
	 * @output - Status should be success
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 07/18/2016
	 * 
	 */
	@SuppressWarnings("null")
	//@Ignore
	@Test
	@Category({ DepositCheckImageUploadTests.class })
	public void testsubmitFrontImage_FileWithJPEGExt() throws Exception {
		DepositResponse response = null;
		try {
			response = submitImage("frontImage", FILE_PATH
					+ "CheckFront3100_ProperFormat.JPEG", false);
		} catch (Exception e) {
			assertThat(e.getMessage(),
					equalTo("Uplaoded Front image size exceeded"));
		}
		assertNotNull(response);
		assertEquals("success", response.getStatus().toLowerCase());
		assertEquals("", response.getErrorMsg());
	}

	/**
	 * <p>
	 * Summary : This test is to test upload back check image
	 * </p>
	 * 
	 * @input - Back check images
	 * @output - The response on successful upload
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckImageUploadTests.class })
	public void testsubmitBackImage_goGood() throws Exception {

		DepositResponse response = submitImage("backImage", FILE_PATH
				+ "CheckBack3100_ProperFormat.JPG", false);

		assertNotNull(response);
		assertEquals("success", response.getStatus().toLowerCase());
		assertEquals("", response.getErrorMsg());
	}

	/**
	 * <p>
	 * Summary : This test is to test upload back check image with filesize
	 * exceeded
	 * </p>
	 * 
	 * @input - back check images and filesize exceeded attribute to true
	 * @output - System exception with file size message being thrown
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckImageUploadTests.class })
	public void testsubmitBackImage_FileSizeExceeded() throws Exception {
		DepositResponse response = null;
		try {
			response = submitImage("backImage", FILE_PATH
					+ "CheckBack3100_ProperFormat.JPG", true);
		} catch (Exception e) {
			assertThat(e.getMessage(),
					equalTo("Uplaoded back image size exceeded"));
		}
		assertNull(response);
	}

	/**
	 * <p>
	 * Summary : This test is to test upload back check image with proper
	 * extension i.e JPEG
	 * </p>
	 * 
	 * @input - Back check images and filesize exceeded attribute to false
	 * @output - Status should be success
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 07/18/2016
	 * 
	 */
	@SuppressWarnings("null")
	//@Ignore
	@Test
	@Category({ DepositCheckImageUploadTests.class })
	public void testsubmitBackImage_FileWithJPEGExt() throws Exception {
		DepositResponse response = null;
		try {
			response = submitImage("backImage", FILE_PATH
					+ "CheckBack3100_ProperFormat.JPEG", false);
		} catch (Exception e) {
			assertThat(e.getMessage(),
					equalTo("Uplaoded back image size exceeded"));
		}
		assertNotNull(response);
		assertEquals("success", response.getStatus().toLowerCase());
		assertEquals("", response.getErrorMsg());
	}

	/**
	 * <p>
	 * Summary : This test is to deposit the valid check images (front & back)
	 * with available daily limit less than deposit amount.
	 * </p>
	 * 
	 * @input - Front and Back valid check images along with Account and Amount
	 *        to be deposited where it is greater than available daily limit
	 * @output - The response from the deposit process should be failure and
	 *         with appropriate error message
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckEnd2EndTests.class })
	public void testsubmitDeposit_DepositAmtGrtThnAvlDailyLmt()
			throws Exception {
		DepositResponse response = new DepositResponse();
		mockRequest.setMethod("POST");
		if (uploadCheckImages(FILE_PATH + "CheckFront3100_ProperFormat.JPG",
				FILE_PATH + "CheckBack3100_ProperFormat.JPG", false)) {

			// Set User Profile and User Session Attributes
			setupUserProfileAndSessionAttrs(mockRequest, true,
					ProductMarketCode.CONSUMER, false, false);

			// Set Deposit Session Info
			DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
			List<DepositAccount> depositAccounts = new ArrayList<DepositAccount>();
			DepositAccount depositaccount = setupDespoitAccount("3000.00");
			depositAccounts.add(depositaccount);
			depositSessionInfo.setDepositAccounts(depositAccounts);
			map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
			mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);

			// Set Deposit Data
			depositData = (DepositData) mocksession.getAttribute(DEPOSIT_DATA);
			depositData.setAmount(new BigDecimal("3100.00"));
			depositData.setDepositRequestSubmitted(false);
			depositData.setSelectedAccount(depositaccount);
			map.addAttribute(DEPOSIT_DATA, depositData);
			mocksession.setAttribute(DEPOSIT_DATA, depositData);

			// Set Deposit Form
			DepositForm depositForm = new DepositForm();
			depositForm.setCameraMode("STILL");
			depositForm.setAccount("0");
			depositForm.setAmount("3100.00");
			depositForm.setBackImageId(depositData.getBackImageId());
			depositForm.setFrontImageId(depositData.getFrontImageId());
			map.addAttribute(DEPOSIT_FORM, depositForm);
			mocksession.setAttribute(DEPOSIT_FORM, depositForm);

			mockVendorServiceSOAPCall("depositCheckResponse-happyPath.xml");
			mockDBCalls();
			mockDepositDelegate("getMemoPostDepositResponse.xml");
			response = depositController.submitDeposit(mockRequest, map,
					depositForm, Mockito.mock(BindingResult.class),
					Mockito.mock(SessionStatus.class));

			assertNotNull(response);
			assertEquals("failed", response.getStatus().toLowerCase());
			assertEquals(messageSource.getMessage(
					"MRDC.SUBMISSION_LIMIT_EXCEEDED", null, null),
					response.getErrorMsg());
		} else {
			fail("Failure in Processing check images.");
		}
	}

	/**
	 * <p>
	 * Summary : This test is to deposit the valid check images (front & back)
	 * with duplicate check deposited.
	 * </p>
	 * 
	 * @input - Front and Back valid check images along with Account and Amount
	 *        but a duplicate check which was processed before
	 * @output - The response from the deposit process should be failure and
	 *         with appropriate error message
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckEnd2EndTests.class })
	public void testsubmitDeposit_DuplicateCheckDeposited() throws Exception {
		DepositResponse response = new DepositResponse();
		mockRequest.setMethod("POST");
		if (uploadCheckImages(FILE_PATH + "CheckFront3100_ProperFormat.JPG",
				FILE_PATH + "CheckBack3100_ProperFormat.JPG", false)) {

			// Set User Profile and User Session Attributes
			setupUserProfileAndSessionAttrs(mockRequest, true,
					ProductMarketCode.CONSUMER, false, false);

			// Set Deposit Session Info
			DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
			List<DepositAccount> depositAccounts = new ArrayList<DepositAccount>();
			DepositAccount depositaccount = setupDespoitAccount("3000.00");
			depositAccounts.add(depositaccount);
			depositSessionInfo.setDepositAccounts(depositAccounts);
			map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
			mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);

			// Set Deposit Data
			depositData = (DepositData) mocksession.getAttribute(DEPOSIT_DATA);
			depositData.setAmount(new BigDecimal("3100.00"));
			depositData.setDepositRequestSubmitted(false);
			depositData.setSelectedAccount(depositaccount);
			map.addAttribute(DEPOSIT_DATA, depositData);
			mocksession.setAttribute(DEPOSIT_DATA, depositData);

			// Set Deposit Form
			DepositForm depositForm = new DepositForm();
			depositForm.setCameraMode("STILL");
			depositForm.setAccount("0");
			depositForm.setAmount("3100.00");
			depositForm.setBackImageId(depositData.getBackImageId());
			depositForm.setFrontImageId(depositData.getFrontImageId());
			map.addAttribute(DEPOSIT_FORM, depositForm);
			mocksession.setAttribute(DEPOSIT_FORM, depositForm);
			// Mock list of deposit records to simulate duplicate check
			// available already
			List<DepositData> depositRecords = new ArrayList<DepositData>();
			DepositData depositRecordsWithDuplicateCheckExists = new DepositData();
			depositRecordsWithDuplicateCheckExists.setAmount(new BigDecimal(
					"3100.00"));
			depositRecordsWithDuplicateCheckExists.setMicrLine(depositData
					.getMicrLine());
			depositRecords.add(depositRecordsWithDuplicateCheckExists);
			Mockito.when(
					depositDao.searchCheckRecord(Mockito.any(DepositData.class)))
					.thenReturn(depositRecords);
			mockVendorServiceSOAPCall("depositCheckResponse-happyPath.xml");
			mockDBCalls();
			mockDepositDelegate("getMemoPostDepositResponse.xml");
			response = depositController.submitDeposit(mockRequest, map,
					depositForm, Mockito.mock(BindingResult.class),
					Mockito.mock(SessionStatus.class));

			assertNotNull(response);
			assertEquals("failed", response.getStatus().toLowerCase());
			assertEquals(messageSource.getMessage("MRDC.DUPLICATE_DEPOSIT",
					null, null), response.getErrorMsg());
		} else {
			fail("Failure in Processing check images.");
		}
	}

	/**
	 * <p>
	 * Summary : This test is to deposit the valid check images (front & back)
	 * and gets processed without any failures.
	 * </p>
	 * 
	 * @input - Front and Back valid check images along with Account and Amount
	 *        to be deposited
	 * @output - The response from the deposit process should be success and
	 *         without any error messages
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckEnd2EndTests.class })
	public void testsubmitDeposit_goGood() throws Exception {
		DepositResponse response = new DepositResponse();
		mockRequest.setMethod("POST");
		if (uploadCheckImages(FILE_PATH + "CheckFront3100_ProperFormat.JPG",
				FILE_PATH + "CheckBack3100_ProperFormat.JPG", false)) {

			// Set User Profile and User Session Attributes
			setupUserProfileAndSessionAttrs(mockRequest, true,
					ProductMarketCode.CONSUMER, false, false);

			// Set Deposit Session Info
			DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
			List<DepositAccount> depositAccounts = new ArrayList<DepositAccount>();
			DepositAccount depositaccount = setupDespoitAccount("3200.00");
			depositAccounts.add(depositaccount);
			depositSessionInfo.setDepositAccounts(depositAccounts);
			map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
			mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);

			// Set Deposit Data
			depositData = (DepositData) mocksession.getAttribute(DEPOSIT_DATA);
			depositData.setAmount(new BigDecimal("3100.00"));
			depositData.setDepositRequestSubmitted(false);
			depositData.setSelectedAccount(depositaccount);
			map.addAttribute(DEPOSIT_DATA, depositData);
			mocksession.setAttribute(DEPOSIT_DATA, depositData);

			// Set Deposit Form
			DepositForm depositForm = new DepositForm();
			depositForm.setCameraMode("STILL");
			depositForm.setAccount("0");
			depositForm.setAmount("3100.00");
			depositForm.setBackImageId(depositData.getBackImageId());
			depositForm.setFrontImageId(depositData.getFrontImageId());
			map.addAttribute(DEPOSIT_FORM, depositForm);
			mocksession.setAttribute(DEPOSIT_FORM, depositForm);

			mockVendorServiceSOAPCall("depositCheckResponse-happyPath.xml");
			mockDBCalls();
			mockDepositDelegate("getMemoPostDepositResponse.xml");
			response = depositController.submitDeposit(mockRequest, map,
					depositForm, Mockito.mock(BindingResult.class),
					Mockito.mock(SessionStatus.class));

			assertNotNull(response);
			assertEquals("success", response.getStatus().toLowerCase());
			assertEquals("", response.getErrorMsg());
		} else {
			fail("Failure in Processing check images.");
		}
	}

	/**
	 * <p>
	 * Summary : This test is when check images swapped i.e Front with Back and
	 * Back with Front.
	 * </p>
	 * 
	 * @input - Front and Back check images swapped but correct with Account and
	 *        Amount to be deposited
	 * @output - The response from the deposit process should be success and
	 *         without any error messages
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckEnd2EndTests.class })
	public void testsubmitDeposit_FrontAndBackImageSwapped() throws Exception {
		DepositResponse response = new DepositResponse();
		mockRequest.setMethod("POST");
		if (uploadCheckImages(FILE_PATH + "CheckBack3100_ProperFormat.JPG",
				FILE_PATH + "CheckFront3100_ProperFormat.JPG", false)) {

			// Set User Profile and User Session Attributes
			setupUserProfileAndSessionAttrs(mockRequest, true,
					ProductMarketCode.CONSUMER, false, false);

			// Set Deposit Session Info
			DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
			List<DepositAccount> depositAccounts = new ArrayList<DepositAccount>();
			DepositAccount depositaccount = setupDespoitAccount("3200.00");
			depositAccounts.add(depositaccount);
			depositSessionInfo.setDepositAccounts(depositAccounts);
			map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
			mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);

			// Set Deposit Data
			depositData = (DepositData) mocksession.getAttribute(DEPOSIT_DATA);
			depositData.setAmount(new BigDecimal("3100.00"));
			depositData.setDepositRequestSubmitted(false);
			depositData.setSelectedAccount(depositaccount);
			map.addAttribute(DEPOSIT_DATA, depositData);
			mocksession.setAttribute(DEPOSIT_DATA, depositData);

			// Set Deposit Form
			DepositForm depositForm = new DepositForm();
			depositForm.setCameraMode("STILL");
			depositForm.setAccount("0");
			depositForm.setAmount("3100.00");
			depositForm.setBackImageId(depositData.getBackImageId());
			depositForm.setFrontImageId(depositData.getFrontImageId());
			map.addAttribute(DEPOSIT_FORM, depositForm);
			mocksession.setAttribute(DEPOSIT_FORM, depositForm);

			mockVendorServiceSOAPCall("depositCheckResponse-imagesswapped.xml");
			mockDBCalls();
			mockDepositDelegate("getMemoPostDepositResponse.xml");
			response = depositController.submitDeposit(mockRequest, map,
					depositForm, Mockito.mock(BindingResult.class),
					Mockito.mock(SessionStatus.class));

			assertNotNull(response);
			assertEquals("", response.getStatus().toLowerCase());
			assertEquals("", response.getErrorMsg());
		} else {
			fail("Failure in Processing check images.");
		}
	}

	/**
	 * <p>
	 * Summary : This test is to validate if appropriate error message being
	 * displayed when user entered amount doesnt match with amount in the check
	 * image
	 * </p>
	 * 
	 * @input - Front and Back valid check images along with Account and with
	 *        incorrect Amount other than the one in check image
	 * @output - The response from the deposit process should be success and
	 *         throws error message as 'Amount doesnt not match'
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	//@Ignore
	@Test
	@Category({ DepositCheckEnd2EndTests.class })
	public void testsubmitDeposit_IncorrectAmountEntered() throws Exception {
		DepositResponse response = new DepositResponse();
		mockRequest.setMethod("POST");

		if (uploadCheckImages(FILE_PATH + "CheckFront3100_ProperFormat.JPG",
				FILE_PATH + "CheckBack3100_ProperFormat.JPG", false)) {

			// Set User Profile and User Session Attributes
			setupUserProfileAndSessionAttrs(mockRequest, true,
					ProductMarketCode.CONSUMER, false, false);

			// Set Deposit Session Info
			DepositSessionInfo depositSessionInfo = new DepositSessionInfo();
			List<DepositAccount> depositAccounts = new ArrayList<DepositAccount>();
			DepositAccount depositaccount = setupDespoitAccount("3200.00");
			depositAccounts.add(depositaccount);
			depositSessionInfo.setDepositAccounts(depositAccounts);
			map.addAttribute(DEPOSIT_INFO, depositSessionInfo);
			mocksession.setAttribute(DEPOSIT_INFO, depositSessionInfo);

			// Set Deposit Data
			depositData = (DepositData) mocksession.getAttribute(DEPOSIT_DATA);
			depositData.setAmount(new BigDecimal("100.00"));
			depositData.setDepositRequestSubmitted(false);
			depositData.setSelectedAccount(depositaccount);
			map.addAttribute(DEPOSIT_DATA, depositData);
			mocksession.setAttribute(DEPOSIT_DATA, depositData);

			// Set Deposit Form
			DepositForm depositForm = new DepositForm();
			depositForm.setCameraMode("STILL");
			depositForm.setAccount("0");
			depositForm.setAmount("100.00");
			depositForm.setBackImageId(depositData.getBackImageId());
			depositForm.setFrontImageId(depositData.getFrontImageId());
			map.addAttribute(DEPOSIT_FORM, depositForm);
			mocksession.setAttribute(DEPOSIT_FORM, depositForm);

			mockVendorServiceSOAPCall("depositCheckResponse-happyPath.xml");
			mockDBCalls();
			mockDepositDelegate("getMemoPostDepositResponse.xml");

			response = depositController.submitDeposit(mockRequest, map,
					depositForm, Mockito.mock(BindingResult.class),
					Mockito.mock(SessionStatus.class));

			assertNotNull(response);
			assertEquals(messageSource.getMessage(
					"MBA.Deposit.error.AmountMisMatch", null, null),
					response.getErrorMsg());
		} else {
			fail("Failure in Processing check images.");
		}
	}

	/* Private and Utility functions: Start */

	/**
	 * <p>
	 * Summary : Util function to upload multipart files (check images) and get
	 * the processed the session and map attributes
	 * </p>
	 * 
	 * @input - Front and Back valid check images along with mibi data
	 * @output - The successful response and processed the session and map
	 *         attributes to hold deposit data for further processing
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	private DepositResponse submitImage(String positionOrmultiPartFileName,
			String filePath, boolean fileSizeExceeded) throws Exception {

		String boundary = "q1w2e3r4t5y6u7i8o9";
		String fileName = (new File(filePath)).getName();
		DepositResponse response = new DepositResponse();
		DepositForm depositForm = new DepositForm();
		Path path = Paths.get(filePath);
		byte[] data = Files.readAllBytes(path);

		MockMultipartFile file = new MockMultipartFile(
				positionOrmultiPartFileName, fileName, "image/jpg", data);
		MockMultipartHttpServletRequest mockMultiPartRequest = new MockMultipartHttpServletRequest();
		mockMultiPartRequest.setContentType("multipart/form-data; boundary="
				+ boundary);
		mockMultiPartRequest.setContent(createFileContent(data, boundary,
				"image/jpg", fileName));
		mockMultiPartRequest.addFile(file);
		mockMultiPartRequest.setMethod("POST");
		mockMultiPartRequest.setParameter("imageCaptureResult", mibi);
		mockMultiPartRequest.setAttribute("fileSizeExceeded", fileSizeExceeded);

		if (positionOrmultiPartFileName.toLowerCase().contains("frontimage")) {
			depositData.setFrontOriginalImage(createFileContent(data, boundary,
					"image/jpg", fileName));
			response = depositController.submitFrontImage(mockMultiPartRequest,
					map, depositForm, Mockito.mock(BindingResult.class),
					Mockito.mock(SessionStatus.class));
			if ("success".equals(response.getStatus().toLowerCase())) {
				depositData.setFrontImageId(response.getFrontImageId());
			}
		} else {
			depositData.setBackOriginalImage(createFileContent(data, boundary,
					"image/jpg", fileName));
			response = depositController.submitBackImage(mockMultiPartRequest,
					map, depositForm, Mockito.mock(BindingResult.class),
					Mockito.mock(SessionStatus.class));
			if ("success".equals(response.getStatus().toLowerCase())) {
				depositData.setBackImageId(response.getBackImageId());
			}
		}

		map.addAttribute(DEPOSIT_DATA, depositData);
		mocksession.setAttribute(DEPOSIT_DATA, depositData);
		return response;
	}

	/**
	 * <p>
	 * Summary : Util function to append boundary for the file content to be
	 * processed with its meta information
	 * </p>
	 * 
	 * @input - Front and Back valid check images
	 * @output - The byte[] with the appended text
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	private byte[] createFileContent(byte[] data, String boundary,
			String contentType, String fileName) {
		String start = "--"
				+ boundary
				+ "\r\n Content-Disposition: form-data; name=\"file\"; filename=\""
				+ fileName + "\"\r\n" + "Content-type: " + contentType
				+ "\r\n\r\n";
		;

		String end = "--" + boundary + "--";
		return ArrayUtils.addAll(start.getBytes(),
				ArrayUtils.addAll(data, end.getBytes()));
	}

	/**
	 * <p>
	 * Summary : Util function to setup mock User Profile information which
	 * holds account and customer
	 * </p>
	 * 
	 * @input - Servlet request
	 * @output - The User Profile with mocked account and customer information
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	private UserProfile setupDespoitAccountForSearch(
			MockHttpServletRequest request,
			boolean enableCustElgbleChkWithErollmentRiskCode,
			ProductMarketCode productMarketCode, boolean isTPBCustomer,
			boolean isPCGCustomer) {
		MobileCache cache = depositController.getMobileCache(request);
		UserProfile userProfile = new UserProfile();
		userProfile.setCurrentView(new ViewImpl(null, null, null));
		userProfile.setAllViews(new ArrayList<View>());
		List<TransactionSummary> transactions = new ArrayList<TransactionSummary>();
		transactions.add(new TransactionSummaryImpl(new Date(),
				"test transaction 123456789", new TransactionAmount(BigDecimal
						.valueOf(101010.99),
						TransactionAmountFormatIndicator.AMOUNT_WITH_PENDING)));
		transactions.add(new TransactionSummaryImpl(new Date(),
				"test transaction <script>alert(\"foobar\")</script>",
				new TransactionAmount(BigDecimal.valueOf(-101010.99),
						TransactionAmountFormatIndicator.AMOUNT_WITH_PENDING)));
		transactions.add(new TransactionSummaryImpl(new Date(),
				"acount 98765432", new TransactionAmount(BigDecimal
						.valueOf(-101010.99),
						TransactionAmountFormatIndicator.AMOUNT_WITH_PENDING)));

		AccountMock currentAccount = new AccountMock(new AccountId("123456789",
				ApsAccountProduct.DDA, "114"), "Mock Account", "XXXXX6789",
				ApsAccountProduct.DDA, new BigDecimal(5000.00), new BigDecimal(
						5000.00));

		CustomerMock customer = new CustomerMock("123456789", "MRDC WB User");
		Address address = Mockito.mock(AddressImpl.class);
		Mockito.when(address.getPostalCode()).thenReturn("90001-96162");
		Mockito.when(address.getStateCode()).thenReturn("CA");
		customer.setAddress(address);
		customer.setPrivateBankingCustomer(isTPBCustomer);
		customer.setPrivateBankingPCGCustomer(isPCGCustomer);
		if (productMarketCode.equals(ProductMarketCode.CONSUMER))
			customer.setLob("CONSUMER");
		else
			customer.setLob("BUSINESS");
		currentAccount.setProductMarketCode(productMarketCode);
		currentAccount.setTransactionHistory(transactions);
		currentAccount.setCustomer(customer);
		userProfile.setCurrentAccount(currentAccount);
		userProfile.setCustomer(customer);
		Enrollment enrollment = new Enrollment();
		enrollment.setId(new Integer("12345"));
		// Customer eligibility is set with Enrollment risk code. If riskcode is
		// other MS then the customer is eligible else not
		if (enableCustElgbleChkWithErollmentRiskCode) {
			enrollment.setRiskCode("NA");
		} else {
			enrollment.setRiskCode("MS");
		}
		userProfile.setEnrollment(enrollment);
		cache.setProfile(userProfile);

		return userProfile;
	}

	/**
	 * <p>
	 * Summary : Util function to setup mock User Profile and session attributes
	 * into mobilecache
	 * </p>
	 * 
	 * @input - Servlet request
	 * @output -
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	private void setupUserProfileAndSessionAttrs(
			MockHttpServletRequest request,
			boolean enableCustElgbleChkWithErollmentRiskCode,
			ProductMarketCode productMarketCode, boolean isTPBCustomer,
			boolean isPCGCustomer) {
		UserProfile userProfile = setupDespoitAccountForSearch(request,
				enableCustElgbleChkWithErollmentRiskCode, productMarketCode,
				isTPBCustomer, isPCGCustomer);
		mobileCache.setProfile(userProfile);
		UserSessionAttributes userSessionAttributes = new UserSessionAttributes();
		AppDescriptor appClient = new AppDescriptor();
		appClient.setAppId(AppId.iPhoneApp);
		ApplicationMode applicationMode = new ApplicationMode();
		applicationMode.setFeaturesTableLastModified(Calendar.getInstance());
		userSessionAttributes.setApplicationMode(applicationMode);
		userSessionAttributes.setAppClient(appClient);
		mobileCache.setUserSessionAttributes(userSessionAttributes);
	}

	/**
	 * <p>
	 * Summary : Util function to setup mock Account Information of User
	 * </p>
	 * 
	 * @input - Account info, AvailableDailyLimit
	 * @output - Return the Account created
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	private DepositAccount setupDespoitAccount(String avaialbleDailyLimit) {
		DepositAccount depositaccount = new DepositAccount();
		depositaccount.setAccount(mobileCache.getProfile().getCurrentAccount());
		depositaccount.setIsEndorsmentRequired(false);
		depositaccount.setAvailableDailyLimit(new BigDecimal(
				avaialbleDailyLimit));
		return depositaccount;
	}

	/**
	 * <p>
	 * Summary : Util function to setup mock Command Account Information of User
	 * </p>
	 * 
	 * @input - Account info, AvailableDailyLimit
	 * @output - Return the Account created
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	private Account setupAccount(String productSubCode,
			ProductMarketCode productMarketCode,
			ApsAccountProduct apsAccountProduct, boolean isCommandDDA,
			boolean isPortfolioManaged, boolean isBusinessAccount) {
		AccountMock account = new AccountMock(accountService, mobileCache
				.getProfile().getCustomer(), new AccountId("2004212200",
				apsAccountProduct, "114"), "Mock Account", "XXXXX2200",
				apsAccountProduct, new BigDecimal(5000.00), new BigDecimal(
						5000.00), true, "SC");
		account.setProductMarketCode(productMarketCode);
		account.setProductSubCode(productSubCode);
		account.setCommandDDA(isCommandDDA);
		account.setBusinessAccount(isBusinessAccount);
		account.setPortfolioManaged(isPortfolioManaged);
		account.setCustomer(mobileCache.getProfile().getCustomer());
		return account;
	}

	/**
	 * <p>
	 * Summary : Util function that Takes an XML snippet and a class that
	 * corresponds to the element in the XML, and creates an object of that
	 * class pre-loaded with the content of the XML.
	 * </p>
	 * 
	 * @input - The class corresponding to the XML and the filename of the XML
	 *        with content to load into an object of the class Note that it's
	 *        the base filename (no path; the path is automatically determined
	 *        by the name of the JUnit test case)
	 * @output - an object of that class, with content specified by the XML
	 * @author - Karthiga Baskaran
	 * @date - 07/18/2016
	 * 
	 */
	private <T> T loadModelFromXmlSnippet(Class<T> c, String file)
			throws Exception {
		Map<Class, JAXBContext> mapOfJaxbContexts = new HashMap<Class, JAXBContext>();
		InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(file);
		JAXBContext jaxbContext = null;
		if (mapOfJaxbContexts.get(c) != null) {
			jaxbContext = mapOfJaxbContexts.get(c);
		} else {
			jaxbContext = JAXBContext.newInstance(c.getPackage().getName());
		}
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		JAXBElement<T> j = unmarshaller.unmarshal(new StreamSource(is), c);
		T model = j.getValue();
		return model;
	}

	/**
	 * <p>
	 * Summary : Util function to check if deposit check images has been
	 * uploaded successfully
	 * </p>
	 * 
	 * @input - Front and Back Image path and filename
	 * @output - The image response
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 07/18/2016
	 * 
	 */
	private boolean uploadCheckImages(String frontImagePath,
			String backImagePath, boolean fileSizeExceeded) throws Exception {
		DepositResponse frontImageResponse = submitImage("frontImage",
				frontImagePath, fileSizeExceeded);
		DepositResponse backImageResponse = submitImage("backImage",
				backImagePath, fileSizeExceeded);
		if (null != frontImageResponse
				&& null != backImageResponse
				&& "success"
						.equals(backImageResponse.getStatus().toLowerCase())
				&& "success".equals(frontImageResponse.getStatus()
						.toLowerCase())) {
			return true;
		}
		return false;
	}

	/**
	 * <p>
	 * Summary : Util function to mock DB calls
	 * </p>
	 * 
	 * @input -
	 * @output -
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 07/18/2016
	 * 
	 */
	private void mockDBCalls() throws Exception {
		// Mock DB Calls: Start
		Mockito.when(
				depositDaoWithExceptionHandler.insertDepositTransaction(Mockito
						.any(Integer.class))).thenReturn(-1);
		depositDaoWithExceptionHandler.insertDepositOriginalImages(Mockito
				.any(DepositData.class));
		Mockito.when(depositDao.getMemoPostCreditSequenceNumber()).thenReturn(
				new Integer(1));
		Mockito.when(
				depositDaoWithExceptionHandler.insertDepositStatus(
						Mockito.any(Integer.class), Mockito.any(String.class)))
				.thenReturn(-1);
		Mockito.when(
				depositDao.insertDepositTransaction(Mockito.any(Integer.class)))
				.thenReturn(-1);
		Mockito.when(
				depositDelegate.deposit(Mockito.any(DepositInputDTO.class)))
				.thenReturn(new DepositOutputDTO());
		Mockito.when(featuresDao.getFeatures()).thenReturn(
				createListOfFeatures(new String[] { "MRDC,On,90001-96162",
						"MRDC_TIER2_LIMITS,LIMITED,90001-96162" }));
		// Update Cache to load features
		featuresService.updateCache();
		// Mock DB Calls: End
	}

	/**
	 * <p>
	 * Summary : Util function to mock Vendor Service SOAP call (where actual
	 * check image is being process and returns the reponse in XML
	 * </p>
	 * 
	 * @input -
	 * @output -
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 07/18/2016
	 * 
	 */
	private void mockVendorServiceSOAPCall(String mockResponseFileName)
			throws Exception {
		Mockito.when(
				phoneVendorServiceSoap.depositCheck(Mockito.any(String.class),
						Mockito.any(String.class), Mockito.any(String.class),
						Mockito.any(String.class), Mockito.any(String.class),
						Mockito.any(String.class), Mockito.any(Long.class),
						Mockito.any(String.class), Mockito.any(String.class),
						Mockito.any(String.class), Mockito.any(String.class)))
				.thenReturn(
						loadModelFromXmlSnippet(DepositCheckResult.class,
								mockResponseFileName));
	}

	/**
	 * <p>
	 * Summary : Util function to mock Deposit Delegate
	 * </p>
	 * 
	 * @input -
	 * @output -
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 07/18/2016
	 * 
	 */
	private void mockDepositDelegate(String mockResponseFileName)
			throws Exception {
		DepositOutputDTO depositOutputDTO = new DepositOutputDTO();

		DelegateImplUnpackResponseInvoker.unpackResponseforMockTest(
				DepositDelegate.class,
				depositOutputDTO,
				loadModelFromXmlSnippet(DepositResponseType.class,
						"getMemoPostDepositResponse.xml"));
	}

	/**
	 * <p>
	 * Summary : Util function to mock Features
	 * </p>
	 * 
	 * @input -
	 * @output -
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	private Features createFeatures(String featureName, String status,
			String zipCodeRange) {
		Features f = new Features();
		f.setFeatureId(new BigDecimal(100));
		f.setFeatureName(featureName);
		f.setStatus(status);
		f.setTncVersion(new BigDecimal(1.2));
		f.setZipCode(zipCodeRange);

		return f;
	}

	/**
	 * <p>
	 * Summary : Util function to mock List of Features
	 * </p>
	 * 
	 * @input -
	 * @output -
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	private List<Features> createListOfFeatures(String[] listOfFeatures) {
		List<Features> features = new ArrayList<Features>();
		for (int i = 0; i < listOfFeatures.length; i++) {
			String[] featureProp = listOfFeatures[i].split(",");
			features.add(createFeatures(featureProp[0], featureProp[1],
					featureProp[2]));
		}

		return features;
	}

	/**
	 * <p>
	 * Summary : Util function to mock Context call
	 * </p>
	 * 
	 * @input -
	 * @output -
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	private void mockMobileContext() {
		PowerMockito.mockStatic(Context.class);
		EnumMap<CtxtField, String> values = new EnumMap<>(CtxtField.class);
		for (int i = 0; i < CtxtField.values().length; i++) {
			values.put(CtxtField.values()[i], "1234567");
		}

		MobileContext wellsContext = new MobileContext(
				Domain.getDefaultDomain(), new RequestCtxt(values));
		PowerMockito.when(Context.getCurrent()).thenReturn(wellsContext);

	}

	/**
	 * <p>
	 * Summary : Util function to mock
	 * authorizeAcctListForDesktopDepositDelegate
	 * </p>
	 * 
	 * @input -
	 * @output -
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	private void mockAuthorizeAcctListForDesktopDepositDelegate() {
		AuthorizeAcctListForDesktopDepositDTO authorizeAcctListForDesktopDepositDTO = new AuthorizeAcctListForDesktopDepositDTO();
		ForEachCustAcctAcsResponseListTypeDTO forEachCustAcctAcsResponseListTypeDTO = new ForEachCustAcctAcsResponseListTypeDTO();
		authorizeAcctListForDesktopDepositDTO.forEachCustAcctAcsResponseListTypeDTO = forEachCustAcctAcsResponseListTypeDTO;
		Mockito.when(
				authorizeAcctListForDesktopDepositDelegate.authorizeAcctListForDesktopDeposit200709(Mockito
						.any(AuthorizeAcctListForDesktopDepositInputDTO.class)))
				.thenReturn(authorizeAcctListForDesktopDepositDTO);

	}

	/**
	 * <p>
	 * Summary : Util function to mock customer account open date
	 * </p>
	 * 
	 * @input - # no of days wrt to current day,
	 * @output -
	 * @author - Karthiga Baskaran
	 * @throws Exception
	 * @date - 08/10/2016
	 * 
	 */
	private void mockCustomerAccountOpenDate(int addToMonthOrDay,
			int noOfDaysOrMonthToBeAdded) {
		Calendar cal = Calendar.getInstance();
		cal.add(addToMonthOrDay, noOfDaysOrMonthToBeAdded);
		Date date = cal.getTime();
		Mockito.when(
				timeOnBookService.getOldestOpenDate(Mockito.any(Account.class)))
				.thenReturn(date);
	}

	/* Private and Utility functions: End */
}
