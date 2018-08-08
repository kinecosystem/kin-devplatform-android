package kin.devplatform.data.offer;

import android.support.annotation.NonNull;
import kin.devplatform.KinCallback;
import kin.devplatform.base.ObservableData;
import kin.devplatform.base.Observer;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.data.Callback;
import kin.devplatform.marketplace.model.NativeOffer;
import kin.devplatform.marketplace.model.NativeSpendOffer;
import kin.devplatform.network.model.OfferList;


public interface OfferDataSource {

	OfferList getCachedOfferList();

	void getOffers(KinCallback<OfferList> callback);

	void addNativeOfferClickedObserver(@NonNull Observer<NativeSpendOffer> observer);

	void removeNativeOfferClickedObserver(@NonNull Observer<NativeSpendOffer> observer);

	ObservableData<NativeSpendOffer> getNativeSpendOfferObservable();

	boolean addNativeOffer(@NonNull NativeOffer nativeOffer);

	boolean removeNativeOffer(@NonNull NativeOffer nativeOffer);

	interface Remote {

		void getOffers(Callback<OfferList, ApiException> callback);
	}
}
