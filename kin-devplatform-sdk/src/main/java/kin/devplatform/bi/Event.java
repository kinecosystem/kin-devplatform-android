package kin.devplatform.bi;

import kin.devplatform.bi.events.Common;
import kin.devplatform.bi.events.User;

public interface Event {

	Common getCommon();

	void setCommon(Common common);

	User getUser();

	void setUser(User user);
}
