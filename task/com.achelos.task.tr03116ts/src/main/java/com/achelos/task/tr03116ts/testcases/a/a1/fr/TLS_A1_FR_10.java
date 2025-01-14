package com.achelos.task.tr03116ts.testcases.a.a1.fr;


import java.util.Arrays;

import com.achelos.task.abstracttestsuite.AbstractTestCase;
import com.achelos.task.commandlineexecution.applications.dut.DUTExecutor;
import com.achelos.task.commandlineexecution.applications.tlstesttool.TlsTestToolExecutor;
import com.achelos.task.commandlineexecution.applications.tlstesttool.configuration.TlsTestToolConfigurationHandshakeType;
import com.achelos.task.commandlineexecution.applications.tlstesttool.messagetextresources.TestToolResource;
import com.achelos.task.commandlineexecution.applications.tshark.TSharkExecutor;
import com.achelos.task.commons.enums.TlsCipherSuite;
import com.achelos.task.commons.enums.TlsTestToolTlsLibrary;
import com.achelos.task.commons.enums.TlsVersion;
import com.achelos.task.configuration.TlsTestToolCertificateTypes;
import com.achelos.task.logging.BasicLogger;
import com.achelos.task.tr03116ts.testfragments.TFConnectionCloseCheck;
import com.achelos.task.tr03116ts.testfragments.TFDUTClientNewConnection;
import com.achelos.task.tr03116ts.testfragments.TFLocalServerClose;
import com.achelos.task.tr03116ts.testfragments.TFServerCertificate;
import com.achelos.task.tr03116ts.testfragments.TFTLSServerHello;


/**
 * Test case TLS_A1_FR_10 - Session resumption with session ID
 * <p>
 * Positive test verifying the session resumption through the Session ID.
 */
public class TLS_A1_FR_10 extends AbstractTestCase {

	private static final String TEST_CASE_ID = "TLS_A1_FR_10";
	private static final String TEST_CASE_DESCRIPTION = "Session resumption with session ID";
	private static final String TEST_CASE_PURPOSE
			= "Positive test verifying the session resumption through the Session ID.";

	private TlsTestToolExecutor testTool = null;
	private TSharkExecutor tShark = null;
	private DUTExecutor dutExecutor = null;
	private final TFTLSServerHello tfServerHello;
	private final TFServerCertificate tfserverCertificate;
	private final TFLocalServerClose tfLocalServerClose;
	private final TFDUTClientNewConnection tFDutClientNewConnection;
	private final TFConnectionCloseCheck tFConnectionCloseCheck;

	public TLS_A1_FR_10() {
		setTestCaseId(TEST_CASE_ID);
		setTestCaseDescription(TEST_CASE_DESCRIPTION);
		setTestCasePurpose(TEST_CASE_PURPOSE);

		tfServerHello = new TFTLSServerHello(this);
		tfserverCertificate = new TFServerCertificate(this);
		tfLocalServerClose = new TFLocalServerClose(this);
		tFDutClientNewConnection = new TFDUTClientNewConnection(this);
		tFConnectionCloseCheck = new TFConnectionCloseCheck(this);
	}

	@Override
	protected final void prepareEnvironment() throws Exception {
		testTool = new TlsTestToolExecutor(getTestCaseId(), logger);
		tShark = new TSharkExecutor(getTestCaseId(), logger);
		tShark.start();
		dutExecutor = new DUTExecutor(getTestCaseId(), logger, configuration.getDutCallCommandGenerator());
	}

	/**
	 * <h2>Precondition</h2>
	 * <ul>
	 * <li>The test TLS server is waiting for incoming TLS connections on [URL].
	 * </ul>
	 */
	@Override
	protected final void preProcessing() throws Exception {}

	/**
	 * <h2>TestStep 1</h2>
	 * <h3>Command</h3>
	 * <ol>
	 * <li>The tester causes the DUT to connect to the TLS server on [URL].
	 * </ol>
	 * <h3>ExpectedResult</h3>
	 * <ul>
	 * <li>The TLS server receives a ClientHello handshake message from the DUT.
	 * <li>The TLS ClientHello indicates support for session resumption via empty Session ID extension.
	 * </ul>
	 * <h2>TestStep 2</h2>
	 * <h3>Command</h3>
	 * <ol>
	 * <li>The TLS server answers the DUT choosing a TLS version and a cipher suite that is contained in the
	 * ClientHello.
	 * </ol>
	 * <h3>Description</h3>
	 * <ol>
	 * <li>The TLS server supplies the certificate chain [CERT_DEFAULT].
	 * <li>The TLS server further generates a new Session ID and supplies it to the DUT.
	 * </ol>
	 * <h3>ExpectedResult</h3>
	 * <ul>
	 * <li>The TLS protocol is executed without errors and the channel is established.
	 * </ul>
	 * <h2>TestStep 3</h2>
	 * <h3>Command</h3>
	 * <ol>
	 * <li>Close TLS connection.
	 * </ol>
	 * <h3>ExpectedResult</h3>
	 * <ul>
	 * <li>TRUE
	 * </ul>
	 * <h2>TestStep 4</h2>
	 * <h3>Command</h3>
	 * <ol>
	 * <li>The tester causes the DUT to connect to the TLS server of [URL] for the second time.
	 * </ol>
	 * <h3>ExpectedResult</h3>
	 * <ul>
	 * <li>The TLS server receives a ClientHello handshake message from the DUT.
	 * <li>The TLS ClientHello initiates session resumption via Session ID extension with the correct value.
	 * </ul>
	 * <h2>TestStep 5</h2>
	 * <h3>Command</h3>
	 * <ol>
	 * <li>The TLS server accepts session resumption.
	 * </ol>
	 * <h3>Description</h3>
	 * <ol>
	 * <li>Session resumption is performed.
	 * </ol>
	 * <h3>ExpectedResult</h3>
	 * <ul>
	 * <li>Session resumption is executed without errors and the channel is established.
	 * </ul>
	 */
	@Override
	protected final void executeUsecase() throws Exception {

		logger.info("START: " + getTestCaseId());
		logger.info(getTestCaseDescription());

		/** highest supported TLS version */
		TlsVersion tlsVersion = configuration.getHighestSupportedTlsVersion();
		if (tlsVersion == null) {
			logger.error("No supported TLS versions found.");
			return;
		}
		logger.debug("TLS version: " + tlsVersion.name());

		/** one supported cipher suite */
		TlsCipherSuite cipherSuite = configuration.getSingleSupportedCipherSuite(tlsVersion);
		if (cipherSuite == null) {
			logger.error("No supported cipher suite is found.");
			return;
		}
		logger.debug("Supported CipherSuite: " + cipherSuite.name());

		step(1, "Setting TLS version: " + tlsVersion.getName() + " and Cipher suite: "
				+ cipherSuite, null);

		tfserverCertificate.executeSteps("2", "The TLS server supplies the certificate chain [CERT_DEFAULT].",
				Arrays.asList(), testTool, tlsVersion, cipherSuite, TlsTestToolCertificateTypes.CERT_DEFAULT);
		
		tfServerHello.executeSteps("3", "Server started and waits for new client connection", Arrays.asList(), testTool,
				tlsVersion, cipherSuite, TlsTestToolTlsLibrary.OpenSSL);
		TlsTestToolConfigurationHandshakeType sessionResumptionWithSessionID
				= TlsTestToolConfigurationHandshakeType.SessionResumptionWithSessionID;
		testTool.setSessionHandshakeType(sessionResumptionWithSessionID);
		tFDutClientNewConnection.executeSteps("4",
				"The TLS server receives a ClientHello handshake message from the DUT.", Arrays.asList(), testTool,
				dutExecutor);

		step(5, "Check if the TLS ClientHello indicates support for session resumption via empty Session ID extension.",
				"TLS ClientHello indicates support for session resumption via empty Session ID extension.");
		String sessionID = testTool.getValue(TestToolResource.ClientHello_session_id);
		if (sessionID == null) {
			logger.info(
					"The TLS ClientHello indicates support for session resumption via empty "
							+ "Session ID extension.");
		} else if (sessionID.length() > 0) {
			logger.warning(
					"The TLS ClientHello sends non empty Session ID with length: " + sessionID.length());
		}
		step(6, "The TLS server further generates a new Session ID and supplies it to the DUT.", null);
		String serverHelloSessionID = testTool.getValue(TestToolResource.ServerHello_session_id);
		logger.info(
				"The TLS server generated following new Session ID and supplies it to the DUT: "
						+ serverHelloSessionID);

		step(7, "Check if the TLS protocol is executed without errors and the channel is established.",
				"The TLS protocol is executed without errors and the channel is established.");
		boolean handShakeSuccessful = testTool.assertMessageLogged(TestToolResource.Handshake_successful);
		if (!handShakeSuccessful) {
			logger.error("The test case is aborted because initial handshake is failed.");
			return;
		}

		testTool.assertMessageLogged(TestToolResource.Initial_handshake_finished_Wait_for_resumption_handshake);

		tFConnectionCloseCheck.executeSteps("8", "The Tls connection is closed.", Arrays.asList(), testTool);

		testTool.saveInitialHandshakeLogs();

		tFDutClientNewConnection.executeSteps("9",
				"The tester causes the DUT to connect to the TLS server for the second time.", Arrays.asList(),
				testTool,
				sessionResumptionWithSessionID, dutExecutor);
		step(10, "Check if the TLS ClientHello initiates session resumption via Session ID extension with the correct "
				+ "value.",
				"The TLS ClientHello initiates session resumption via Session ID extension with the correct value.");
		sessionID = testTool.getValue(TestToolResource.ClientHello_session_id);
		if (serverHelloSessionID.equalsIgnoreCase(sessionID)) {
			logger.info(
					"The TLS ClientHello initiates session resumption via Session ID extension with the correct "
							+ "value.");
		} else {
			logger.error(
					"The TLS ClientHello does not initiate session resumption via Session ID extension with the "
							+ "correct value.");
		}


		step(11, "Check if the session resumption is executed without errors and the channel is established.",
				"Session resumption is executed without errors and the channel is established.");
		boolean serverHelloDoneLogged
		= testTool.assertMessageLogged(TestToolResource.ServerHelloDone_transmitted, BasicLogger.INFO);
		boolean handshakeSuccessful = testTool.assertMessageLogged(TestToolResource.Handshake_successful);
		if (!serverHelloDoneLogged && handshakeSuccessful) {
			logger.info("The DUT has accepted the session resumption.");
		} else {
			logger.error("The DUT has refused the session resumption.");
		}

		testTool.assertMessageLogged(TestToolResource.Server_handled_all_connections);

		tfLocalServerClose.executeSteps("12", "Server closed successfully", Arrays.asList(),
				testTool);

	}

	@Override
	protected void postProcessing() throws Exception {

	}

	@Override
	protected final void cleanAndExit() {
		testTool.cleanAndExit();
		tShark.cleanAndExit();
		dutExecutor.cleanAndExit();
	}
}
