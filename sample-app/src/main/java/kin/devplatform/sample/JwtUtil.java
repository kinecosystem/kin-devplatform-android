package kin.devplatform.sample;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Random;

public class JwtUtil {

	private static final long DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

	private static final String ALGORITHM_EC = "EC";
	private static final String JWT_CLAIM_OBJECT_OFFER_PART = "offer";
	private static final String JWT_CLAIM_OBJECT_SENDER_PART = "sender"; // Should be part of native SPEND jwt

	private static final String JWT_CLAIM_OBJECT_RECIPIENT_PART = "recipient"; // Should be part of native EARN jwt
	private static final String JWT_SUBJECT_REGISTER = "register";
	private static final String JWT_SUBJECT_SPEND = "spend";
	private static final String JWT_SUBJECT_EARN = "earn";

	private static final String JWT_SUBJECT_PAY_TO_USER = "pay_to_user";

	private static final String JWT_KEY_USER_ID = "user_id";
	private static final String JWT_HEADER_KID = "kid";

	private static final String JWT_HEADER_TYP = "typ";
	private static final String JWT = "jwt";

	public static String generateSignInExampleJWT(@NonNull String appID, @NonNull String userId) {

		String jwt = getBasicJWT(appID)
			.setSubject(JWT_SUBJECT_REGISTER)
			.claim(JWT_KEY_USER_ID, userId)
			.signWith(SignatureAlgorithm.ES256, getES256PrivateKey()).compact();
		return jwt;
	}

	public static String generateSpendOfferExampleJWT(@NonNull String appID, @NonNull String userID) {
		String jwt = getBasicJWT(appID)
			.setSubject(JWT_SUBJECT_SPEND)
			.claim(JWT_CLAIM_OBJECT_OFFER_PART, createOfferPartExampleObject())
			.claim(JWT_CLAIM_OBJECT_SENDER_PART, new JWTSenderPart(userID, "Bought a sticker", "Lion sticker"))
			.signWith(SignatureAlgorithm.ES256, getES256PrivateKey()).compact();
		return jwt;
	}

	public static String generateEarnOfferExampleJWT(@NonNull String appID, @NonNull String userID) {
		String jwt = getBasicJWT(appID)
			.setSubject(JWT_SUBJECT_EARN)
			.claim(JWT_CLAIM_OBJECT_OFFER_PART, createOfferPartExampleObject())
			.claim(JWT_CLAIM_OBJECT_RECIPIENT_PART,
				new JWTRecipientPart(userID, "Received Kin", "upload profile picture"))
			.signWith(SignatureAlgorithm.ES256, getES256PrivateKey()).compact();
		return jwt;
	}

	public static String generatePayToUserOfferExampleJWT(@NonNull String appID, @NonNull String userID,
		@NonNull String recipientUserID) {
		String jwt = getBasicJWT(appID)
			.setSubject(JWT_SUBJECT_PAY_TO_USER)
			.claim(JWT_CLAIM_OBJECT_OFFER_PART, createOfferPartExampleObject())
			.claim(JWT_CLAIM_OBJECT_SENDER_PART, new JWTSenderPart(userID, "Uploaded Profile Picture", "Lion sticker"))
			.claim(JWT_CLAIM_OBJECT_RECIPIENT_PART,
				new JWTRecipientPart(recipientUserID, "Received Kin", "Upload profile picture"))
			.signWith(SignatureAlgorithm.ES256, getES256PrivateKey()).compact();
		return jwt;
	}

	@NonNull
	private static String getPrivateKeyForJWT() {
		return BuildConfig.ES256_PRIVATE_KEY;
	}

	private static JwtBuilder getBasicJWT(String appID) {
		return Jwts.builder().setHeaderParam(JWT_HEADER_KID, BuildConfig.ES256_PRIVATE_KEY_ID)
			.setHeaderParam(JWT_HEADER_TYP, JWT)
			.setIssuedAt(new Date())
			.setIssuer(appID)
			.setExpiration(new Date(System.currentTimeMillis() + DAY_IN_MILLISECONDS));
	}

	@Nullable
	private static PrivateKey getES256PrivateKey() {
		try {
			KeyFactory kf = KeyFactory.getInstance(ALGORITHM_EC);
			byte[] bytes = Base64.decode(getPrivateKeyForJWT(), Base64.NO_WRAP);
			EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
			return kf.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static JWTOfferPart createOfferPartExampleObject() {
		int randomID = new Random().nextInt((9999 - 1) + 1) + 1;
		return new JWTOfferPart(String.valueOf(randomID), 10);
	}

	private static class JWTOfferPart {


		@JsonProperty("id")
		private String id;
		@JsonProperty("amount")
		private int amount;

		/**
		 * These fields are REQUIRED in order to succeed.
		 *
		 * @param id decided by you (internal)
		 * @param amount of KIN for this offer / (price)
		 */
		public JWTOfferPart(String id, int amount) {
			this.id = id;
			this.amount = amount;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public int getAmount() {
			return amount;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}
	}

	private static class JWTOrderPart {

		@JsonProperty("user_id")
		private String user_id; // Optional in case of spend order
		@JsonProperty("title")
		private String title;
		@JsonProperty("description")
		private String description;

		JWTOrderPart(String user_id, String title, String description) {
			this.user_id = user_id;
			this.title = title;
			this.description = description;
		}

		JWTOrderPart(String title, String description) {
			this.title = title;
			this.description = description;
		}

		public String getUser_id() {
			return user_id;
		}

		public void setUser_id(String user_id) {
			this.user_id = user_id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	private static class JWTSenderPart extends JWTOrderPart {

		// User Id is optional
		JWTSenderPart(String user_id, String title, String description) {
			super(user_id, title, description);
		}

		JWTSenderPart(String title, String description) {
			super(title, description);
		}
	}

	private static class JWTRecipientPart extends JWTOrderPart {

		JWTRecipientPart(String user_id, String title, String description) {
			super(user_id, title, description);
		}
	}
}
