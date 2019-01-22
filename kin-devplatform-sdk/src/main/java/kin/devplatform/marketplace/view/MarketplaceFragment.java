package kin.devplatform.marketplace.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import kin.devplatform.R;
import kin.devplatform.base.BaseRecyclerAdapter;
import kin.devplatform.base.BaseRecyclerAdapter.OnItemClickListener;
import kin.devplatform.exception.ClientException;
import kin.devplatform.marketplace.presenter.IMarketplacePresenter;
import kin.devplatform.marketplace.presenter.ISpendDialogPresenter;
import kin.devplatform.network.model.Offer;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.poll.view.PollWebViewActivity;
import kin.devplatform.poll.view.PollWebViewActivity.PollBundle;


public class MarketplaceFragment extends Fragment implements IMarketplaceView {

	public static MarketplaceFragment newInstance() {
		return new MarketplaceFragment();
	}

	private IMarketplacePresenter marketplacePresenter;

	private TextView spendSubTitle;
	private TextView earnSubTitle;
	private SpendRecyclerAdapter spendRecyclerAdapter;
	private EarnRecyclerAdapter earnRecyclerAdapter;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.kinecosystem_fragment_marketplce, container, false);
		initViews(root);
		marketplacePresenter.onAttach(this);
		return root;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		marketplacePresenter.onDetach();
	}

	@Override
	public void attachPresenter(IMarketplacePresenter presenter) {
		marketplacePresenter = presenter;
	}

	protected void initViews(View root) {
		spendSubTitle = root.findViewById(R.id.spend_subtitle);
		earnSubTitle = root.findViewById(R.id.earn_subtitle);

		//Space item decoration for both of the recyclers
		int margin = getResources().getDimensionPixelOffset(R.dimen.kinecosystem_main_margin);
		int space = getResources().getDimensionPixelOffset(R.dimen.kinecosystem_offer_item_list_space);
		SpaceItemDecoration itemDecoration = new SpaceItemDecoration(margin, space);

		//Spend Recycler
		RecyclerView spendRecycler = root.findViewById(R.id.spend_recycler);
		spendRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
		spendRecycler.addItemDecoration(itemDecoration);
		spendRecyclerAdapter = new SpendRecyclerAdapter();
		spendRecyclerAdapter.bindToRecyclerView(spendRecycler);
		spendRecyclerAdapter.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(BaseRecyclerAdapter adapter, View view, int position) {
				marketplacePresenter.onItemClicked(position, OfferType.SPEND);
			}
		});
		spendRecyclerAdapter.setEmptyView(new OffersEmptyView(getContext()));

		//Earn Recycler
		RecyclerView earnRecycler = root.findViewById(R.id.earn_recycler);
		earnRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
		earnRecycler.addItemDecoration(itemDecoration);
		earnRecyclerAdapter = new EarnRecyclerAdapter();
		earnRecyclerAdapter.bindToRecyclerView(earnRecycler);
		earnRecyclerAdapter.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(BaseRecyclerAdapter adapter, View view, int position) {
				marketplacePresenter.onItemClicked(position, OfferType.EARN);
			}
		});
		earnRecyclerAdapter.setEmptyView(new OffersEmptyView(getContext()));

	}


	@Override
	public void setSpendList(List<Offer> spendList) {
		spendRecyclerAdapter.setNewData(spendList);
	}

	@Override
	public void setEarnList(List<Offer> earnList) {
		earnRecyclerAdapter.setNewData(earnList);
	}

	@Override
	public void showOfferActivity(PollBundle pollBundle) {
		try {
			Intent intent = PollWebViewActivity.createIntent(getContext(), pollBundle);
			startActivity(intent);
			getActivity()
				.overridePendingTransition(R.anim.kinecosystem_slide_in_right, R.anim.kinecosystem_slide_out_left);
		} catch (ClientException e) {
			marketplacePresenter.showOfferActivityFailed();
		}

	}

	@Override
	public void showSpendDialog(ISpendDialogPresenter spendDialogPresenter) {
		SpendDialog spendDialog = new SpendDialog(getActivity(), marketplacePresenter.getNavigator(),
			spendDialogPresenter);
		spendDialog.show();
	}

	@Override
	public void showMigrationErrorDialog() {
		final AlertDialog dialog = new Builder(getContext())
			.setTitle(getString(R.string.kinecosystem_dialog_migration_is_needed_title))
			.setMessage(getString(R.string.kinecosystem_dialog_migration_is_needed_kin_saved_message))
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setCancelable(false)
			.create();
		dialog.show();
	}

	@Override
	public void showToast(String msg) {
		Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void notifyEarnItemRemoved(int index) {
		earnRecyclerAdapter.itemRemoved(index);
	}

	@Override
	public void notifyEarnItemInserted(int index) {
		earnRecyclerAdapter.itemInserted(index);
	}

	@Override
	public void notifySpendItemRemoved(int index) {
		spendRecyclerAdapter.itemRemoved(index);
	}

	@Override
	public void notifySpendItemInserted(int index) {
		spendRecyclerAdapter.itemInserted(index);
	}

	@Override
	public void showSomethingWentWrong() {
		showToast(getString(R.string.kinecosystem_something_went_wrong));
	}

	@Override
	public void updateEarnSubtitle(boolean isEmpty) {
		earnSubTitle.setText(isEmpty ? R.string.kinecosystem_empty_tomorrow_more_opportunities
			: R.string.kinecosystem_complete_tasks_and_earn_kin);
	}

	@Override
	public void updateSpendSubtitle(boolean isEmpty) {
		spendSubTitle.setText(isEmpty ? R.string.kinecosystem_empty_tomorrow_more_opportunities
			: R.string.kinecosystem_use_your_kin_to_enjoy_stuff_you_like);
	}
}
