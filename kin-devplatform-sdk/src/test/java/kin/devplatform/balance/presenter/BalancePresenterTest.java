package kin.devplatform.balance.presenter;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static kin.devplatform.balance.presenter.BalancePresenter.COMPLETED;
import static kin.devplatform.balance.presenter.BalancePresenter.DELAYED;
import static kin.devplatform.balance.presenter.BalancePresenter.EARN;
import static kin.devplatform.balance.presenter.BalancePresenter.FAILED;
import static kin.devplatform.balance.presenter.BalancePresenter.PENDING;
import static kin.devplatform.balance.presenter.BalancePresenter.SPEND;
import static kin.devplatform.main.ScreenId.MARKETPLACE;
import static kin.devplatform.main.ScreenId.ORDER_HISTORY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import kin.devplatform.BaseTestClass;
import kin.devplatform.balance.presenter.IBalancePresenter.BalanceClickListener;
import kin.devplatform.balance.view.IBalanceView;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.BalanceTapped;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.order.OrderRepository;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.network.model.Order;
import kin.devplatform.network.model.Order.Origin;
import kin.devplatform.network.model.Order.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class BalancePresenterTest extends BaseTestClass {

	@Mock
	private IBalanceView balanceView;

	@Mock
	private EventLogger eventLogger;

	@Mock
	private BlockchainSource blockchainSource;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private BalanceClickListener balanceClickListener;

	@Captor
	private ArgumentCaptor<Observer<Balance>> balanceObserverCaptor;

	@Captor
	private ArgumentCaptor<Boolean> sseCaptor;

	@Captor
	private ArgumentCaptor<Observer<Order>> orderObserverCaptor;

	@Mock
	private Order order;


	private BalancePresenter balancePresenter;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		balancePresenter = new BalancePresenter(balanceView, eventLogger, blockchainSource, orderRepository);
		verify(balanceView).attachPresenter(balancePresenter);

		when(order.getOrderId()).thenReturn("2");
		when(order.getOfferId()).thenReturn("1");

		balancePresenter.onAttach(balanceView);
		verify(balanceView).setWelcomeSubtitle();

		balancePresenter.onStart();
		verify(blockchainSource).addBalanceObserver(balanceObserverCaptor.capture(), sseCaptor.capture());
		verify(orderRepository).addOrderObserver(orderObserverCaptor.capture());
		assertTrue(sseCaptor.getValue());
	}

	@After
	public void tearDown() throws Exception {
		balancePresenter.onStop();
		verify(blockchainSource).removeBalanceObserver(balanceObserverCaptor.getValue(), true);
		verify(orderRepository).removeOrderObserver(orderObserverCaptor.getValue());

		balancePresenter.onDetach();
		assertNull(balancePresenter.getView());
	}

	@Test
	public void test_Balance_Clicked() throws Exception {
		balancePresenter.setClickListener(balanceClickListener);
		balancePresenter.balanceClicked();

		verify(balanceClickListener).onClick();
		verify(eventLogger).send(any(BalanceTapped.class));
	}

	@Test
	public void test_Update_Balance() throws Exception {
		Balance balance = new Balance();
		balanceObserverCaptor.getValue().onChanged(balance);
		verify(balanceView).updateBalance(0);

		balance.setAmount(new BigDecimal(30));
		balanceObserverCaptor.getValue().onChanged(balance);
		verify(balanceView).updateBalance(30);
	}

	@Test
	public void test_Subtitle_Pending_Earn() throws Exception {
		when(order.getOfferType()).thenReturn(OfferType.EARN);
		when(order.getOrigin()).thenReturn(Origin.MARKETPLACE);
		when(order.getStatus()).thenReturn(Status.PENDING);
		when(order.getAmount()).thenReturn(30);

		orderObserverCaptor.getValue().onChanged(order);
		verify(balanceView).updateSubTitle(30, PENDING, EARN);
	}

	@Test
	public void test_Subtitle_Pending_Spend() throws Exception {
		when(order.getOfferType()).thenReturn(OfferType.SPEND);
		when(order.getOrigin()).thenReturn(Origin.MARKETPLACE);
		when(order.getStatus()).thenReturn(Status.PENDING);
		when(order.getAmount()).thenReturn(30);

		orderObserverCaptor.getValue().onChanged(order);
		verify(balanceView).updateSubTitle(30, PENDING, SPEND);
	}

	@Test
	public void test_Subtitle_NOT_Pending_Earn_Current_Order() throws Exception {
		when(order.getOfferType()).thenReturn(OfferType.EARN);
		when(order.getOrigin()).thenReturn(Origin.MARKETPLACE);
		when(order.getAmount()).thenReturn(30);

		// Set current order, for every new pending.
		when(order.getStatus()).thenReturn(Status.PENDING);
		orderObserverCaptor.getValue().onChanged(order);

		when(order.getStatus()).thenReturn(Status.COMPLETED);
		orderObserverCaptor.getValue().onChanged(order);
		verify(balanceView).updateSubTitle(30, COMPLETED, EARN);

		when(order.getStatus()).thenReturn(Status.FAILED);
		orderObserverCaptor.getValue().onChanged(order);
		verify(balanceView).updateSubTitle(30, FAILED, EARN);

		when(order.getStatus()).thenReturn(Status.DELAYED);
		orderObserverCaptor.getValue().onChanged(order);
		verify(balanceView).updateSubTitle(30, DELAYED, EARN);
	}

	@Test
	public void test_Subtitle_NOT_Pending_Earn_NOT_Current_Order() throws Exception {
		InOrder updateSubtitleInOrder = inOrder(balanceView);

		// Set current order, for every new pending.
		when(order.getOfferType()).thenReturn(OfferType.EARN);
		when(order.getOrigin()).thenReturn(Origin.MARKETPLACE);
		when(order.getAmount()).thenReturn(30);
		when(order.getStatus()).thenReturn(Status.PENDING);
		orderObserverCaptor.getValue().onChanged(order);

		// Create another order
		Order newOrder = new Order().orderId("second");
		when(order.getStatus()).thenReturn(Status.COMPLETED);
		orderObserverCaptor.getValue().onChanged(newOrder);
		when(order.getStatus()).thenReturn(Status.FAILED);
		orderObserverCaptor.getValue().onChanged(newOrder);
		when(order.getStatus()).thenReturn(Status.DELAYED);
		orderObserverCaptor.getValue().onChanged(newOrder);

		updateSubtitleInOrder.verify(balanceView).updateSubTitle(30, PENDING, EARN);
		updateSubtitleInOrder.verify(balanceView, never()).updateSubTitle(anyInt(), anyInt(), anyInt());
	}

	@Test
	public void test_VisibleScreen_Marketplace_Status_Completed() throws Exception {
		when(order.getOfferType()).thenReturn(OfferType.EARN); // doesn't really matter
		when(order.getOrigin()).thenReturn(Origin.MARKETPLACE);

		// Update status to COMPLETED
		when(order.getStatus()).thenReturn(Status.PENDING);
		orderObserverCaptor.getValue().onChanged(order);
		when(order.getStatus()).thenReturn(Status.COMPLETED);
		orderObserverCaptor.getValue().onChanged(order);

		InOrder inOrder = Mockito.inOrder(balanceView);
		balancePresenter.visibleScreen(MARKETPLACE);
		inOrder.verify(balanceView).setWelcomeSubtitle();
	}

	@Test
	public void test_VisibleScreen_OrderHistory_Status_Completed() throws Exception {
		when(order.getOfferType()).thenReturn(OfferType.EARN); // doesn't really matter
		when(order.getOrigin()).thenReturn(Origin.MARKETPLACE);

		// Update status to COMPLETED
		when(order.getStatus()).thenReturn(Status.PENDING);
		orderObserverCaptor.getValue().onChanged(order);
		when(order.getStatus()).thenReturn(Status.COMPLETED);
		orderObserverCaptor.getValue().onChanged(order);

		InOrder inOrder = Mockito.inOrder(balanceView);
		balancePresenter.visibleScreen(ORDER_HISTORY);
		inOrder.verify(balanceView).clearSubTitle();
	}
}