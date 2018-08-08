package kin.devplatform.marketplace.presenter;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import kin.devplatform.base.BasePresenter;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.BackButtonOnMarketplacePageTapped;
import kin.devplatform.bi.events.EarnOfferTapped;
import kin.devplatform.bi.events.MarketplacePageViewed;
import kin.devplatform.bi.events.NotEnoughKinPageViewed;
import kin.devplatform.bi.events.SpendOfferTapped;
import kin.devplatform.data.KinCallbackAdapter;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.offer.OfferDataSource;
import kin.devplatform.data.order.OrderDataSource;
import kin.devplatform.main.INavigator;
import kin.devplatform.marketplace.model.NativeSpendOffer;
import kin.devplatform.marketplace.view.IMarketplaceView;
import kin.devplatform.network.model.Offer;
import kin.devplatform.network.model.Offer.ContentTypeEnum;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.network.model.OfferInfo;
import kin.devplatform.network.model.OfferList;
import kin.devplatform.network.model.Order;
import kin.devplatform.poll.view.PollWebViewActivity.PollBundle;

public class MarketplacePresenter extends BasePresenter<IMarketplaceView> implements IMarketplacePresenter {

	private static final int NOT_FOUND = -1;

	private final OfferDataSource offerRepository;
	private final OrderDataSource orderRepository;
	private final BlockchainSource blockchainSource;
	private final INavigator navigator;
	private final EventLogger eventLogger;

	private List<Offer> spendList;
	private List<Offer> earnList;

	private Observer<Order> orderObserver;


	private final Gson gson;

	public MarketplacePresenter(@NonNull final IMarketplaceView view, @NonNull final OfferDataSource offerRepository,
		@NonNull final OrderDataSource orderRepository, @Nullable final BlockchainSource blockchainSource,
		@NonNull INavigator navigator, @NonNull EventLogger eventLogger) {
		this.view = view;
		this.offerRepository = offerRepository;
		this.orderRepository = orderRepository;
		this.blockchainSource = blockchainSource;
		this.navigator = navigator;
		this.eventLogger = eventLogger;
		this.gson = new Gson();

		this.view.attachPresenter(this);
	}

	@Override
	public void onAttach(IMarketplaceView view) {
		super.onAttach(view);
		getCachedOffers();
		getOffers();
		listenToOrders();
		eventLogger.send(MarketplacePageViewed.create());
	}

	private void getCachedOffers() {
		if (earnList == null && spendList == null) {
			earnList = new ArrayList<>();
			spendList = new ArrayList<>();
			OfferList cachedOfferList = offerRepository.getCachedOfferList();
			if (cachedOfferList != null && cachedOfferList.getOffers() != null) {
				splitOffersByType(cachedOfferList.getOffers(), this.earnList, this.spendList);
			}
		}
		setOfferLists();
	}

	private void setOfferLists() {
		if (this.view != null) {
			this.view.setEarnList(earnList);
			this.view.setSpendList(spendList);

			setEarnEmptyViewState();
			setSpendEmptyViewState();
		}
	}

	private void listenToOrders() {
		orderObserver = new Observer<Order>() {
			@Override
			public void onChanged(Order order) {
				switch (order.getStatus()) {
					case PENDING:
						removeOfferFromList(order.getOfferId(), order.getOfferType());
						break;
					case FAILED:
					case COMPLETED:
						getOffers();
						break;
				}

			}
		};
		orderRepository.addOrderObserver(orderObserver);
	}

	private void removeOfferFromList(String offerId, OfferType offerType) {
		if (offerType == OfferType.EARN) {
			for (int i = 0; i < earnList.size(); i++) {
				Offer offer = earnList.get(i);
				if (offer.getId().equals(offerId)) {
					earnList.remove(i);
					notifyEarnItemRemoved(i);
					setEarnEmptyViewState();
					return;
				}
			}
		} else {
			for (int i = 0; i < spendList.size(); i++) {
				Offer offer = spendList.get(i);
				if (offer.getId().equals(offerId)) {
					spendList.remove(i);
					notifySpendItemRemoved(i);
					setSpendEmptyViewState();
					return;
				}
			}
		}
	}

	private void setEarnEmptyViewState() {
		boolean isEarnListEmpty = earnList.isEmpty();
		if (view != null) {
			view.updateEarnSubtitle(isEarnListEmpty);
		}
	}

	private void setSpendEmptyViewState() {
		boolean isSpendListEmpty = spendList.isEmpty();
		if (view != null) {
			view.updateSpendSubtitle(isSpendListEmpty);
		}
	}

	private void notifyEarnItemRemoved(int index) {
		if (view != null) {
			view.notifyEarnItemRemoved(index);
		}
	}

	private void notifyEarnItemInserted(int index) {
		if (view != null) {
			view.notifyEarnItemInserted(index);
		}
	}

	private void notifySpendItemRemoved(int index) {
		if (view != null) {
			view.notifySpendItemRemoved(index);
		}
	}

	private void notifySpendItemInserted(int index) {
		if (view != null) {
			view.notifySpendItemInserted(index);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		release();
	}

	private void release() {
		orderRepository.removeOrderObserver(orderObserver);
	}

	@Override
	public void getOffers() {
		this.offerRepository.getOffers(new KinCallbackAdapter<OfferList>() {
			@Override
			public void onResponse(OfferList offerList) {
				syncOffers(offerList);
			}
		});
	}

	private void syncOffers(OfferList offerList) {
		if (offerList != null && offerList.getOffers() != null) {
			List<Offer> newEarnOffers = new ArrayList<>();
			List<Offer> newSpendOffers = new ArrayList<>();

			splitOffersByType(offerList.getOffers(), newEarnOffers, newSpendOffers);
			syncList(newEarnOffers, earnList, OfferType.EARN);
			syncList(newSpendOffers, spendList, OfferType.SPEND);
		}
	}

	private void syncList(List<Offer> newList, List<Offer> oldList, OfferType offerType) {
		// check if offer should be removed (index changed / removed from list).
		if (newList.size() > 0) {
			for (int i = 0; i < oldList.size(); i++) {
				Offer offer = oldList.get(i);
				int index = newList.indexOf(offer);
				if (index == NOT_FOUND || index != i) {
					oldList.remove(i);
					notifyItemRemoved(i, offerType);
				}
			}
		}

		// Add missing offers, the order matters
		for (int i = 0; i < newList.size(); i++) {
			Offer offer = newList.get(i);
			if (i < oldList.size()) {
				if (!oldList.get(i).equals(offer)) {
					oldList.add(i, offer);
					notifyItemInserted(i, offerType);
				}
			} else {
				oldList.add(offer);
				notifyItemInserted(i, offerType);
			}
		}

		if (offerType == OfferType.EARN) {
			setEarnEmptyViewState();
		} else {
			setSpendEmptyViewState();
		}
	}

	private void notifyItemRemoved(int index, OfferType offerType) {
		if (isSpend(offerType)) {
			notifySpendItemRemoved(index);
		} else {
			notifyEarnItemRemoved(index);
		}
	}

	private void notifyItemInserted(int index, OfferType offerType) {
		if (isSpend(offerType)) {
			notifySpendItemInserted(index);
			setSpendEmptyViewState();
		} else {
			notifyEarnItemInserted(index);
			setEarnEmptyViewState();
		}
	}

	private boolean isSpend(OfferType offerType) {
		return offerType == OfferType.SPEND;
	}

	private void splitOffersByType(List<Offer> list, List<Offer> earnList, List<Offer> spendList) {
		for (Offer offer : list) {
			if (offer.getOfferType() == OfferType.EARN) {
				earnList.add(offer);
			} else {
				spendList.add(offer);
			}
		}
	}

	@Override
	public void onItemClicked(int position, OfferType offerType) {
		final Offer offer;
		if (offerType == OfferType.EARN) {
			offer = earnList.get(position);
			sendEranOfferTapped(offer);
			if (this.view != null) {
				PollBundle pollBundle = new PollBundle()
					.setJsonData(offer.getContent())
					.setOfferID(offer.getId())
					.setContentType(offer.getContentType().getValue())
					.setAmount(offer.getAmount())
					.setTitle(offer.getTitle());
				this.view.showOfferActivity(pollBundle);
			}
		} else {
			offer = spendList.get(position);
			sendSpendOfferTapped(offer);
			if (offer.getContentType() == ContentTypeEnum.EXTERNAL) {
				nativeSpendOfferClicked(offer);
				return;
			}
			int balance = blockchainSource.getBalance().getAmount().intValue();
			final BigDecimal amount = new BigDecimal(offer.getAmount());

			if (balance < amount.intValue()) {
				eventLogger.send(NotEnoughKinPageViewed.create());
				showToast("You don't have enough Kin");
				return;
			}

			OfferInfo offerInfo = deserializeOfferInfo(offer.getContent());
			if (offerInfo != null) {
				showSpendDialog(offerInfo, offer);
			} else {
				showSomethingWentWrong();
			}
		}
	}

	private void sendEranOfferTapped(Offer offer) {
		EarnOfferTapped.OfferType offerType;
		try {
			offerType = EarnOfferTapped.OfferType.fromValue(offer.getContentType().getValue());
			double amount = (double) offer.getAmount();
			eventLogger.send(EarnOfferTapped.create(offerType, amount, offer.getId()));
		} catch (IllegalArgumentException | NullPointerException ex) {
			//TODO: add general error event
		}
	}

	private void sendSpendOfferTapped(Offer offer) {
		double amount = (double) offer.getAmount();
		eventLogger.send(SpendOfferTapped.create(amount, offer.getId(), null));
	}

	private void nativeSpendOfferClicked(Offer offer) {
		offerRepository.getNativeSpendOfferObservable().postValue((NativeSpendOffer) offer);
	}

	private void showSomethingWentWrong() {
		if (view != null) {
			view.showSomethingWentWrong();
		}
	}

	@Override
	public void showOfferActivityFailed() {
		showSomethingWentWrong();
	}

	@Override
	public void backButtonPressed() {
		eventLogger.send(BackButtonOnMarketplacePageTapped.create());
	}

	@Override
	public INavigator getNavigator() {
		return navigator;
	}

	private void showSpendDialog(@NonNull final OfferInfo offerInfo, @NonNull final Offer offer) {
		if (this.view != null) {
			this.view.showSpendDialog(createSpendDialogPresenter(offerInfo, offer));
		}
	}

	private ISpendDialogPresenter createSpendDialogPresenter(@NonNull final OfferInfo offerInfo,
		@NonNull final Offer offer) {
		return new SpendDialogPresenter(offerInfo, offer, blockchainSource, orderRepository, eventLogger);
	}

	private OfferInfo deserializeOfferInfo(final String content) {
		try {
			return gson.fromJson(content, OfferInfo.class);
		} catch (JsonSyntaxException t) {
			return null;
		}
	}

	private void showToast(String msg) {
		if (view != null) {
			view.showToast(msg);
		}
	}
}
