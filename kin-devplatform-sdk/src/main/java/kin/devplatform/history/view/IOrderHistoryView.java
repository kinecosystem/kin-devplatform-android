package kin.devplatform.history.view;

import android.support.annotation.NonNull;
import java.util.List;
import kin.devplatform.base.IBaseView;
import kin.devplatform.history.presenter.ICouponDialogPresenter;
import kin.devplatform.history.presenter.OrderHistoryPresenter;
import kin.devplatform.network.model.Order;

public interface IOrderHistoryView extends IBaseView<OrderHistoryPresenter> {

	void updateOrderHistoryList(List<Order> orders);

	void onItemInserted();

	void onItemUpdated(int index);

	void showCouponDialog(@NonNull final ICouponDialogPresenter presenter);
}
