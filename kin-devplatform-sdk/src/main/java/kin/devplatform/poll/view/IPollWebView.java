package kin.devplatform.poll.view;

import android.support.annotation.NonNull;
import kin.devplatform.base.IBaseView;
import kin.devplatform.poll.presenter.PollWebViewPresenter;

public interface IPollWebView extends IBaseView<PollWebViewPresenter> {

	void showToast(String msg);

	void loadUrl();

	void renderJson(@NonNull final String pollJsonString);

	void close();

	void showToolbar();

	void hideToolbar();

	void setTitle(String title);
}
