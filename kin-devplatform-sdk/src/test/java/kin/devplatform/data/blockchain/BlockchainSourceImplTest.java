package kin.devplatform.data.blockchain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import kin.core.BlockchainEvents;
import kin.core.EventListener;
import kin.core.KinAccount;
import kin.core.KinClient;
import kin.core.Request;
import kin.core.ResultCallback;
import kin.core.TransactionId;
import kin.core.exception.OperationFailedException;
import kin.devplatform.BaseTestClass;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.SpendTransactionBroadcastToBlockchainFailed;
import kin.devplatform.bi.events.SpendTransactionBroadcastToBlockchainSucceeded;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.Payment;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.network.model.OpenOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class BlockchainSourceImplTest extends BaseTestClass {

	private static final String PUBLIC_ADDRESS = "public_address";
	private static final String APP_ID = "appID";
	private static final String ORDER_ID = "orderID";
	private static final String MEMO_EXAMPLE = "1-" + APP_ID + "-" + ORDER_ID;


	@Mock
	private EventLogger eventLogger;

	@Mock
	private KinClient kinClient;

	@Mock
	private BlockchainSource.Local local;

	@Mock
	private KinAccount kinAccount;

	@Mock
	private BlockchainEvents blockchainEvents;

	@Mock
	private Request<kin.core.Balance> getBalanceReq;

	@Captor
	private ArgumentCaptor<ResultCallback<kin.core.Balance>> getBalanceCaptor;

	@Mock
	private kin.core.Balance balanceObj;


	private BlockchainSourceImpl blockchainSource;
	private Balance balance;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		when(kinClient.addAccount()).thenReturn(kinAccount);
		when(kinAccount.blockchainEvents()).thenReturn(blockchainEvents);
		when(kinAccount.getBalance()).thenReturn(getBalanceReq);
		when(balanceObj.value()).thenReturn(new BigDecimal(20));
		when(kinAccount.getPublicAddress()).thenReturn(PUBLIC_ADDRESS);

		doNothing().when(kinAccount).activateSync();

		resetInstance();

		// Account Creation
		verify(kinClient).addAccount();

		// init Balance
		verify(getBalanceReq).run(getBalanceCaptor.capture());
		getBalanceCaptor.getValue().onResult(balanceObj);
		verify(local).setBalance(balanceObj.value().intValue());

		when(kinClient.getAccount(0)).thenReturn(kinAccount);
		balance = new Balance();
	}


	private void resetInstance() throws Exception {
		Field instance = BlockchainSourceImpl.class.getDeclaredField("instance");
		instance.setAccessible(true);
		instance.set(null, null);
		BlockchainSourceImpl.init(eventLogger, kinClient, local);
		blockchainSource = BlockchainSourceImpl.getInstance();
	}

	@Test
	public void init_once_and_one_account() throws Exception {
		BlockchainSourceImpl.init(eventLogger, kinClient, local);
		BlockchainSourceImpl.init(eventLogger, kinClient, local);
		assertEquals(blockchainSource, BlockchainSourceImpl.getInstance());
		verify(kinClient).addAccount();
	}

	@Test
	public void set_app_id_memo_generated_correctly() {
		blockchainSource.setAppID(APP_ID);
		assertEquals(MEMO_EXAMPLE, blockchainSource.generateMemo(ORDER_ID));
	}

	@Test
	public void get_public_address() {
		assertEquals(PUBLIC_ADDRESS, blockchainSource.getPublicAddress());
	}

	@Test
	public void extract_order_id() {
		// without app id set
		assertNull(blockchainSource.extractOrderId("123"));
		assertNull(blockchainSource.extractOrderId(MEMO_EXAMPLE));

		// with app id
		blockchainSource.setAppID(APP_ID);
		assertEquals(ORDER_ID, blockchainSource.extractOrderId(MEMO_EXAMPLE));
	}

	@Test
	public void send_transaction_succeeded() throws InterruptedException, OperationFailedException {
		String toAddress = "some_pub_address";
		final BigDecimal amount = new BigDecimal(10);
		final String orderID = "someID";

		when(kinAccount.sendTransactionSync(anyString(), (BigDecimal) any(), anyString())).thenReturn(
			new TransactionId() {
				@Override
				public String id() {
					return "transactionID";
				}
			});
		blockchainSource.setAppID(APP_ID);
		OpenOrder openOrder = new OpenOrder();
		openOrder.setId(orderID);
		openOrder.setOfferId("offerID");
		openOrder.setOfferType(OfferType.SPEND);
		blockchainSource.sendTransaction(toAddress, amount, openOrder);
		verify(eventLogger).send(any(SpendTransactionBroadcastToBlockchainSucceeded.class));
	}

	@Test
	public void send_transaction_failed() throws OperationFailedException {
		String toAddress = "some_pub_address";
		BigDecimal amount = new BigDecimal(10);
		final String orderID = "someID";

		final OperationFailedException exception = new OperationFailedException("failed");
		when(kinAccount.sendTransactionSync(any(String.class), any(BigDecimal.class), any(String.class)))
			.thenThrow(exception);

		blockchainSource.setAppID(APP_ID);
		OpenOrder openOrder = new OpenOrder();
		openOrder.setId(orderID);
		openOrder.setOfferType(OfferType.SPEND);

		blockchainSource.addPaymentObservable(new Observer<Payment>() {
			@Override
			public void onChanged(Payment value) {
				assertFalse(value.isSucceed());
				assertEquals(orderID, value.getOrderID());
				assertEquals(exception, value.getException());
				assertEquals(Payment.UNKNOWN, value.getType());
			}
		});

		try {
			blockchainSource.sendTransaction(toAddress, amount, openOrder);
		} catch (OperationFailedException e) {
			e.printStackTrace();
		}
		verify(eventLogger).send(any(SpendTransactionBroadcastToBlockchainFailed.class));
	}

	@Test
	public void add_balance_observer_get_onChanged() {
		kin.core.Balance innerBalance = mock(kin.core.Balance.class);
		blockchainSource.addBalanceObserver(new Observer<Balance>() {
			@Override
			public void onChanged(Balance value) {
				balance = value;
			}
		});
		assertEquals(new BigDecimal(20), balance.getAmount());

		InOrder inOrder = Mockito.inOrder(local);
		BigDecimal value = new BigDecimal(25);
		when(innerBalance.value()).thenReturn(value);
		blockchainSource.setBalance(innerBalance);
		assertEquals(value, balance.getAmount());

		value = new BigDecimal(50);
		when(innerBalance.value()).thenReturn(value);
		blockchainSource.setBalance(innerBalance);
		assertEquals(value, balance.getAmount());

		value = new BigDecimal(50);
		when(innerBalance.value()).thenReturn(value);
		blockchainSource.setBalance(innerBalance);
		assertEquals(value, balance.getAmount());

		inOrder.verify(local).setBalance(25);
		inOrder.verify(local).setBalance(50);
		inOrder.verify(local, never()).setBalance(any(Integer.class));
	}

	@Test
	public void add_balance_observer_and_start_listen() throws Exception {
		ArgumentCaptor<EventListener<kin.core.Balance>> balanceEventListener = forClass(EventListener.class);

		blockchainSource.addBalanceObserverAndStartListen(new Observer<Balance>() {
			@Override
			public void onChanged(Balance value) {
				balance = value;
			}
		});

		verify(blockchainEvents).addBalanceListener(balanceEventListener.capture());
		BigDecimal value = new BigDecimal(123);

		when(balanceObj.value()).thenReturn(value);
		balanceEventListener.getValue().onEvent(balanceObj);

		assertEquals(value, balance.getAmount());
		verify(local).setBalance(value.intValue());

	}

	@Test
	public void get_KeyStoreProvider_is_not_null() {
		assertNotNull(blockchainSource.getKeyStoreProvider());
	}
}