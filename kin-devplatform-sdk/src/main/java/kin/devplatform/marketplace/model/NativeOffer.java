package kin.devplatform.marketplace.model;

import kin.devplatform.network.model.Offer;

public class NativeOffer extends Offer {

	NativeOffer(String id) {
		this.setId(id);
		this.setContentType(ContentTypeEnum.EXTERNAL);
	}
}
