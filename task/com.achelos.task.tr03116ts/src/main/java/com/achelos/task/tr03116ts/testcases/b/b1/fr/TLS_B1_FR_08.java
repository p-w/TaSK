package com.achelos.task.tr03116ts.testcases.b.b1.fr;

import java.util.Arrays;

import com.achelos.task.abstracttestsuite.AbstractTestCase;
import com.achelos.task.commandlineexecution.applications.tlstesttool.TlsTestToolExecutor;
import com.achelos.task.commandlineexecution.applications.tlstesttool.messagetextresources.TestToolResource;
import com.achelos.task.commandlineexecution.applications.tshark.TSharkExecutor;
import com.achelos.task.commons.enums.TlsAlertDescription;
import com.achelos.task.commons.enums.TlsAlertLevel;
import com.achelos.task.commons.enums.TlsCipherSuite;
import com.achelos.task.commons.enums.TlsVersion;
import com.achelos.task.commons.tools.StringTools;
import com.achelos.task.tr03116ts.testfragments.TFAlertMessageCheck;
import com.achelos.task.tr03116ts.testfragments.TFTCPIPCloseConnection;
import com.achelos.task.tr03116ts.testfragments.TFTCPIPNewConnection;
import com.achelos.task.tr03116ts.testfragments.TFTLSClientHello;


/**
 * <p>
 * This test verifies the correct behaviour of the DUT if the client sends heartbeat messages.
 */
public class TLS_B1_FR_08 extends AbstractTestCase {

	private static final String TEST_CASE_ID = "TLS_B1_FR_08";
	private static final String TEST_CASE_DESCRIPTION = "Heartbeat-Message is ignored";
	private static final String TEST_CASE_PURPOSE
			= "This test verifies the correct behaviour of the DUT if the client sends heartbeat messages.";

	private TlsTestToolExecutor testTool = null;
	private TSharkExecutor tShark = null;
	private final TFTCPIPNewConnection tFTCPIPNewConnection;
	private final TFTCPIPCloseConnection tFTCPIPCloseConnection;
	private final TFAlertMessageCheck tfAlertMessageCheck;
	private final TFTLSClientHello tfClientHello;

	public TLS_B1_FR_08() {
		setTestCaseId(TEST_CASE_ID);
		setTestCaseDescription(TEST_CASE_DESCRIPTION);
		setTestCasePurpose(TEST_CASE_PURPOSE);

		tFTCPIPNewConnection = new TFTCPIPNewConnection(this);
		tFTCPIPCloseConnection = new TFTCPIPCloseConnection(this);
		tfAlertMessageCheck = new TFAlertMessageCheck(this);
		tfClientHello = new TFTLSClientHello(this);
	}


	@Override
	protected final void prepareEnvironment() throws Exception {
		testTool = new TlsTestToolExecutor(getTestCaseId(), logger);
		tShark = new TSharkExecutor(getTestCaseId(), logger);
		tShark.start();
	}


	/**
	 * <h2>Precondition</h2>
	 * <ul>
	 * <li>DUT services are online without any known disturbances.
	 * <li>The DUT is accepting TLS connections.
	 * </ul>
	 */
	@Override
	protected final void preProcessing() throws Exception {}

	/**
	 * <h2>TestStep 1</h2>
	 * <h3>Command</h3>
	 * <ol>
	 * <li>The tester connects to the DUT.
	 * </ol>
	 * <h3>Description</h3>
	 * <ol>
	 * <li>The TLS ClientHello offers the highest TLS version supported according to the ICS.
	 * <li>The TLS ClientHello offers a cipher suite that is supported according to the ICS.
	 * <li>In case the cipher suite is based on ECC, the TLS ClientHello offers valid elliptic curves in the appropriate
	 * extension according to the ICS.
	 * <li>The presence of the signature_algorithms extension depends on the used TLS version. In case it is sent, all
	 * algorithms that are supported according to the ICS are listed.
	 * <li>The TLS ClientHello does not contain further extensions which are not required to conduct the TLS handshake.
	 * </ol>
	 * <h3>ExpectedResult</h3>
	 * <ul>
	 * <li>The DUT accepts the TLS connection.
	 * <li>The TLS protocol is executed without errors and the channel is established.
	 * </ul>
	 * <h2>TestStep 2</h2>
	 * <h3>Command</h3>
	 * <ol>
	 * <li>The TLS client sends a ""heartbeat_request"".
	 * </ol>
	 * <h3>ExpectedResult</h3>
	 * <ul>
	 * <li>The DUT ignores the request.
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

		/** any supported algorithm cipher suite */
		TlsCipherSuite cipherSuite = configuration.getSingleSupportedCipherSuite(tlsVersion);
		logger.debug("Supported Cipher suites:" + cipherSuite);
		if (cipherSuite == null) {
			logger.error("No supported cipher suite found.");
			return;
		}

		step(1, "The tester connects to the DUT.", "");

		tfClientHello.executeSteps("2", "The TLS ClientHello offers the TLS version " + tlsVersion.getName()
				+ ", cipher suite " + cipherSuite.name() + " .", null, testTool, tlsVersion, cipherSuite);

		// prepare the Heartbeat Request
		final byte payloadByte31 = 0x31;
		final byte payloadByte32 = 0x32;
		final byte payloadByte33 = 0x33;
		final byte payloadByte34 = 0x34;
		byte[] payload = {payloadByte31, payloadByte32, payloadByte33, payloadByte34};
		testTool.manipulateSendHeartbeatRequest(TestToolResource.After_Handshake.getInternalToolOutputMessage(),
				String.valueOf(payload.length), StringTools.toHexString(payload));
		testTool.start();

		tFTCPIPNewConnection.executeSteps("3", "", Arrays.asList(),
				testTool);

		step(4, "Send Heartbeat package (request).", "Expected result is checked in the following steps.");

		tfAlertMessageCheck.executeSteps("5", "",
				Arrays.asList("level=warning/fatal"), testTool);

		tFTCPIPCloseConnection.executeSteps("6", "", Arrays.asList(),
				testTool);
	}


	@Override
	protected void postProcessing() throws Exception {

	}


	@Override
	protected final void cleanAndExit() {
		testTool.cleanAndExit();
		tShark.cleanAndExit();
	}

}
