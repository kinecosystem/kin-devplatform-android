package kin.devplatform.poll.presenter;

import kin.devplatform.base.IBasePresenter;
import kin.devplatform.poll.view.IPollWebView;
import kin.devplatform.web.EcosystemWebPageListener;

public interface IPollWebViewPresenter extends IBasePresenter<IPollWebView>, EcosystemWebPageListener {

	void closeClicked();
}
