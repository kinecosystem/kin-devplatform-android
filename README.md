# Kin Android SDK #

## What is the Kin SDK? ##

The Kin SDK allows you to quickly and easily integrate with the Kin platform. This enables you to provide your users with new opportunities to earn and spend the Kin digital currency from inside your app or from the Kin Marketplace offer wall. For each user, the SDK will create wallet and an account on Kin blockchain. By calling the appropriate SDK functions, your application can performs earn and spend transactions. Your users can also view their account balance and their transaction history.

## Playground and Production Environments ##

Kin provides two working environments:

- **Playground** – a staging and testing environment using test servers and a blockchain test network.
- **Production** – uses production servers and the main blockchain network.

Use the Playground environment to develop, integrate and test your app. Transition to the Production environment when you’re ready to go live with your Kin-integrated app.

When your app calls ```Kin.start(…)```, you specify which environment to work with.

>* When working with the Playground environment, you can only register up to 1000 users. An attempt to register additional users will result in an error.
>* In order to switch between environments, you’ll need to clear the application data.

## Setting Up the Sample App ##

Kin SDK Sample App demonstrates how to perform common workflows such as creating a user account and creating Spend and Earn offers. You can build the Sample App from the `app` module in the Kin SDK [github repository]("https://github.com/kinecosystem/kin-devplatform-android").  
We recommend building and running the Sample App as a good way to get started with the Kin SDK and familiarize yourself with its functions.

>**NOTE:** The Sample App is for demonstration only, and should not be used for any other purpose.

The Sample App is pre-configured with the default whitelist credentials `appId='test'` and
`apiKey='AyINT44OAKagkSav2vzMz'` and with a default ES256 JWT private key. These credentials can be used for integration testing in any app, but authorization will fail if you attempt to use them in a production environment.

You can also request unique apiKey and appId values from Kin, and override the default settings, working either in whitelist or JWT authentication mode.

### Override the default credential settings

Create or edit a local `credential.properties` file in the `app` module directory and add the lines below, using the credentials values and method you received.

```gradle
APP_ID="YOUR_APP_ID" // Your unique application id, required for both whitelist and JWT. Default = 'test'.

API_KEY="YOUR_API_KEY" // For whitelist registration. Default = 'AyINT44OAKagkSav2vzMz'.

ES256_PRIVATE_KEY="YOUR_ES256_PRIVATE_KEY" // Optional. Only required when testing JWT on the sample app. For production, JWT is created by server side with ES256 signature.

ES256_PRIVATE_KEY_ID="YOUR ES256 KEY ID" //required when using jwt, `kid` param is sent with every jwt for identify the key you signed with.

IS_JWT_REGISTRATION = false // Optional. To test sample app JWT registration, set this property to true. If not specified, default=false.
```

The Sample App Gradle build loads the `credential.properties` setting and uses it to create the `SignInData` object used for registration.

## Integrating with the Kin SDK

1. Add the following lines to your project module's ```build.gradle``` file.
```
 repositories {
     ...
     maven {
         url 'https://jitpack.io'
     }
 }
```
Add the following lines to the app module's ```build.gradle``` file.

```gradle
 dependencies {
     ...
     implementation 'com.github.kinecosystem:kin-devplatform-android:<latest_version>'
 }
```

latest version can be found in [github releases]("https://github.com/kinecosystem/kin-devplatform-android/releases").

## Creating or Accessing a User’s Kin Account ###

If your app presents Kin Spend and Earn offers to your users, then each user needs a Kin wallet and account in order to take advantage of those offers. During initialization and before any other Kin sdk API calls, your app must call the SDK’s `Kin.start(...)` function while passing a unique ID for the current user. If that user already has a Kin account, the function only accesses the existing account. Otherwise, the function creates a new wallet and account for the user.

### Initialize Android SDK <a name="Init"></a>

Call `Kin.start(...)`, passing the android context, the desired environment (playground/production) and your chosen authentication credentials (either whitelist or JWT credentials).

#### Whitelist:

```java
whitelistData = new WhitelistData(<userID>, <appID>, <apiKey>);
try {
   Kin.start(getApplicationContext(), whitelistData,
             Environment.getPlayground());
} 
catch (ClientException | BlockchainException e) {
   // Handle exception…
}
```

userID - your application unique identifier for the user  
appID - your application unique identifier as provided by Kin.  
apiKey - your secret apiKey as provided by Kin.


#### JWT:

Request a [registration JWT]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt#register-payload-a-name-registerpayload") from your server, once the client received this token, you can now start the sdk using this token. (See [Authentication and JWT]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt.html") for more details about JWT Authentication )

```java
try {
    String registrationJWT = getRegistrationJwtFromServer();
    Kin.start(getApplicationContext(), registrationJWT, Environment.getPlayground());
}
catch (ClientException | BlockchainException e) {
   // Handle exception…
}
```

## Account Balance

A user’s balance is the number of Kin units in his or her account (can also contain a fraction). You may want to retrieve the balance in response to a user request or to check whether a user has enough funding to perform a Spend request. When you request a user’s balance, you receive a ```Balance``` object in response, which contains the balance as a decimal-point number.

> If no account was found for the user, you will receive a balance of 0 for that user.

There are 3 ways you can retrieve the user’s balance:

* Get the cached balance (the last balance that was received on the client side). The cached balance is updated upon SDK initialization and for every transation. Usually, this will be the same balance as the one stored in the Kin blockchain. But in some situations it might not be current, for instance due to network connection issues.
* Get the balance from the Kin blockchain. This is the definitive balance value. This is an asynchronous call that requires you to implement callback functions.
* Create an ```Observer``` object that receives notifications when the user’s balance changes.

### Cached balance

Call ```Kin.getCachedBalance()```.

```java
try {
        Balance cachedBalance = Kin.getCachedBalance();
    } catch (ClientException e) {
        e.printStackTrace();
}
```

### Blockchain Balance

Call `Kin.getBalance(…)`, and implement the 2 response callback functions.

```java
Kin.getBalance(new KinCallback<Balance>() {
                    @Override
                    public void onResponse(Balance balance) {
                        // Got the balance from the network
                    }
    
                    @Override
                    public void onFailure(KinEcosystemException exception) {
                        // Got an error from the blockchain network
                    }
    });
```

### Observing Balance Updates

Create an `Observer` object and implements its `onChanged()` function.

>* The `Observer` object sends a first update with the last known balance, and then opens a connection to the blockchain network to receive subsequent live updates. 
>* When performing cleanup upon app exit, don’t forget to remove the Observer object in order to close the network connection.

```
    // Add balance observer
    balanceObserver = new Observer<Balance>() {
                    @Override
                    public void onChanged(Balance value) {
                        showToast("Balance - " + 
                                   value.getAmount().intValue());
                    }
                };
    
    try {
        Kin.addBalanceObserver(balanceObserver);
    } catch (TaskFailedException e) {
        e.printStackTrace();
    }
    
    // Remove the balance observer
    try {
        Kin.removeBalanceObserver(balanceObserver);
    } catch (TaskFailedException e) {
        e.printStackTrace();
    }
```

## Custom Spend Offer <a name="CreateCustomSpendOffer"></a>

A custom Spend offer allows your users to unlock unique spend opportunities that you define within your app, Custom offers are created by your app, as opposed to built-in offers displayed in the Kin Marketplace offer wall.  
Your app displays the offer, request user approval, and then performing the purchase using the `purchase` API.

### Purchase Payment

1. Create a JWT that represents a [Spend offer JWT]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt#spend-payload-a-name-spendpayload") signed by your application server. The fastest way for building JWT tokens is to use the [JWT Service]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt-service").  
Once you have the JWT Service set up, perform a [Spend query]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt-service#spend-a-name-spend"),
the service will response with the generated signed JWT token.

2. Call `purchase` method, while passing the JWT you built and a callback function that will receive purchase confirmation.

> The following snippet is taken from the SDK Sample App, in which the JWT is created and signed by the Android client side for presentation purposes only. Do not use this method in production! In production, the JWT must be signed by the server, with a secure private key.

```java
try {
    Kin.purchase(offerJwt, new KinCallback<OrderConfirmation>() {
        @Override public void onResponse(OrderConfirmation orderConfirmation) {
            // OrderConfirmation will be called once Kin received the payment transaction from user.
            // OrderConfirmation can be kept on digital service side as a receipt proving user received his Kin.
            // Send confirmation JWT back to the server in order prove that the user completed
            // the blockchain transaction and purchase can be unlocked for this user.
            System.out.println("Succeed to create native spend.\n jwtConfirmation: " + orderConfirmation.getJwtConfirmation());                
        }

        @Override
        public void onFailure(KinEcosystemException exception) {
            System.out.println("Failed - " + error.getMessage());
        }
    });
} catch (ClientException e) {
    e.printStackTrace();
}
```

3.	Complete the purchase after you receive confirmation from the Kin Server that the funds were transferred successfully.

### Adding to the Marketplace 
The Kin Marketplace offer wall displays built-in offers, which are served by Kin.  
Their purpose is to provide users with opportunities to earn initial Kin funding, which they can later spend on spend offers provided by hosting apps.

You can also choose to display a banner for your custom offer in the Kin Marketplace offer wall. This serves as additional "real estate" in which to let the user know about custom offers within your app. When the user clicks on your custom Spend offer in the Kin Marketplace, your app is notified, and then it continues to manage the offer activity in its own UX flow.

>**NOTE:** You will need to actively launch the Kin Marketplace offer wall so your user can see the offers you added to it.


1. Create a `NativeSpendOffer` object as in the example below.

```java
NativeSpendOffer nativeOffer =
        new NativeSpendOffer("The offerID") // An unique offer id
            .title("Offer Title") // Title to display with offer
            .description("Offer Description") // Description to display with offer
            .amount(100) // Purchase amount in Kin
            .image("Image URL"); // Image to display with offer
```

2.	Create a `NativeOfferObserver` object to be notified when the user clicks on your offer in the Kin Marketplace.

>**NOTE:** You can remove the Observer by calling `Kin.removeNativeOfferClickedObserver(...)`.

```java
private void addNativeOfferClickedObserver() {
    try {
        Kin.addNativeOfferClickedObserver(getNativeOfferClickedObserver());
    } catch (TaskFailedException e) {
        showToast("Could not add native offer callback");
    }
}

private Observer<NativeSpendOffer> getNativeOfferClickedObserver() {
    if (nativeSpendOfferClickedObserver == null) {
        nativeSpendOfferClickedObserver = new Observer<NativeSpendOffer>() {
            @Override
            public void onChanged(NativeSpendOffer value) {
                Intent nativeOfferIntent = NativeOfferActivity.createIntent(MainActivity.this, value.getTitle());
                startActivity(nativeOfferIntent);
            }
        };
    }
    return nativeSpendOfferClickedObserver;
}
```

3. Call `Kin.addNativeOffer(...)`.

>**NOTE:** Each new offer is added as the first offer in Spend Offers list the Marketplace displays.

```java
try {
    if (Kin.addNativeOffer(nativeSpendOffer)) {
        // Native offer added
    } else {
        // Native offer already in Kin Marketplace
    }
} catch (ClientException error) {
    ...
}
```

### Removing from Marketplace

To remove a custom Spend offer from the Kin Marketplace, call `Kin.removeNativeOffer(...)`, passing the offer you want to remove.  

```java
try {
    if (Kin.removeNativeOffer(nativeSpendOffer)) {
        // Native offer removed
    } else {
        // Native offer isn't in Kin Marketplace
    }
} catch (ClientException e) {
    ...
}
```

## Displaying the Kin Marketplace Offer Wall <a name="AddingToMP"></a>

Optionally, your app can launch the Kin Marketplace offer wall. It displays Earn and Spend offers, which can be added to it by your app or by the Kin Server. When a user selects one of these offers, the Kin Marketplace notifies the app that created the offer. The app can then launch the Earn or Spend activity for the user to complete. 

You may choose to add your custom Earn and Spend offers to the Kin Marketplace so that there is a convenient, visible place where the user can access all offers. Some offers displayed in-app might require that the user choose to navigate to a specific page, and therefore might not be so readily visible.

>**NOTE:** The launchMarketplace function is not a one-time initialization function; you must call it each time you want to display the Kin Marketplace offer wall.

For launching the Kin Marketplace offer wall, use `launchMarketplace` with an `Activity` object.

```java
try {
    Kin.launchMarketplace(activity);
    System.out.println("Public address : " + Kin.getPublicAddress());
} catch (ClientException e) {
    // handle exception...
}
```

## Requesting an Order Confirmation

In the normal flow of a transaction, you will receive an order confirmation from the Kin Server through the offers APIs callback function. This indicates that the transaction was completed. But if you missed this notification for any reason, for example, because the user closed the app before it arrived, or the app closed due to some error, you can request confirmation for an order according to its ID.

<block class="android" />

Call `Kin.getOrderConfirmation(...)`, while passing the order’s ID and implementing the appropriate callback functions.

```java
try {
    Kin.getOrderConfirmation("your_offer_id", new KinCallback<OrderConfirmation>() {
            @Override
            public void onResponse(OrderConfirmation orderConfirmation) {
                if(orderConfirmation.getStatus() == Status.COMPLETED ){
                    String jwtConfirmation = orderConfirmation.getJwtConfirmation()
                }
            }

            @Override
            public void onFailure(KinEcosystemException exception) {
                ...
            }
    });
} catch (ClientException exception) {
    ...
}
```

## Custom Earn Offer

A custom Earn offer allows your users to earn Kin as a reward for performing tasks you want to incentives, such as setting a profile picture or rating your app. Custom offers are created by your app, as opposed to built-in offers displayed in the Kin Marketplace offer wall.  
Once the user has completed the task associated with the Earn offer, you request Kin payment for the user.

### Request A Payment

1. Create a JWT that represents a [Earn offer JWT]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt#earn-payload-a-name-earnpayload") signed by your application server. The fastest way for building JWT tokens is to use the [JWT Service]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt-service.html").  
Once you have the JWT Service set up, perform a [Earn query]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt-service#earn-a-name-earn"),
the service will response with the generated signed JWT token.

2. Call `requestPayment` while passing the JWT you built and a callback function that will receive purchase confirmation.

>* The following snippet is taken from the SDK Sample App, in which the JWT is created and signed by the Android client side for presentation purposes only. Do not use this method in production! In production, the JWT must be signed by the server, with a secure private key.     

```java
try {
    Kin.requestPayment(offerJwt, new KinCallback<OrderConfirmation>() {
        @Override
            public void onResponse(OrderConfirmation orderConfirmation) {
                // OrderConfirmation will be called once payment transaction to the user completed successfully.
                // OrderConfirmation can be kept on digital service side as a receipt proving user received his Kin.
                System.out.println("Succeed to create native earn.\n jwtConfirmation: " + orderConfirmation.getJwtConfirmation());
            }

            @Override
            public void onFailure(KinEcosystemException exception) {
                System.out.println("Failed - " + exception.getMessage());
            }
        });
}
catch (ClientException exception) {
    exception.printStackTrace();
}
```

## Custom Pay To User Offer

A custom pay to user offer allows your users to unlock unique spend opportunities that you define within your app offered by other users.
(Custom offers are created by your app, as opposed to built-in offers displayed in the Kin Marketplace offer wall.  
Your app displays the offer, request user approval, and then performing the purchase using the `payToUser` API.

### Pay to user

*To request payment for a custom Pay To User offer:*

1. Create a JWT that represents a [Pay to User offer JWT]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt#paytouser-payload-a-name-paytouserpayload") signed by your application server. The fastest way for building JWT tokens is to use the [JWT Service]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt-service.html").  
Once you have the JWT Service set up, perform a [Pay To User query]("https://kinecosystem.github.io/kin-ecosystem-sdk-docs/docs/jwt-service#pay-to-user-a-name-paytouser"),
the service will response with the generated signed JWT token.


2.	Call `Kin.payToUser(...)`, while passing the JWT you built and a callback function that will receive purchase confirmation.

> The following snippet is taken from the SDK Sample App, in which the JWT is created and signed by the Android client side for presentation purposes only. Do not use this method in production! In production, the JWT must be signed by the server, with a secure private key. 

```java
try {
    Kin.payToUser(offerJwt, new KinCallback<OrderConfirmation>() {
        @Override public void onResponse(OrderConfirmation orderConfirmation) {
            // OrderConfirmation will be called once Kin received the payment transaction from user.
            // OrderConfirmation can be kept on digital service side as a receipt proving user received his Kin.
            // Send confirmation JWT back to the server in order prove that the user completed
            // the blockchain transaction and purchase can be unlocked for this user.
            System.out.println("Succeed to create native spend.\n jwtConfirmation: " + orderConfirmation.getJwtConfirmation());
        }

        @Override
        public void onFailure(KinEcosystemException exception) {
            System.out.println("Failed - " + error.getMessage());
        }
    });
} catch (ClientException e) {
    e.printStackTrace();
}
```

3.	Complete the pay to user offer after you receive confirmation from the Kin Server that the funds were transferred successfully.

## Common Errors

Most of the errors are derived from the `KinEcosystemException` exception, Exception has an error code - `getCode()` and a detailed message - `getMessage()`.  

* `ClientException` - Represents an error in local client SDK, error code might be:
    - `SDK_NOT_STARTED` - SDK method was called before init (`start) method was called. see [Initialize Android SDK](#Init).
    - `BAD_CONFIGURATION` - Bad or missing configuration parameters.
    - `INTERNAL_INCONSISTENCY` - Some unexpected error occured.
* `ServiceException`- Represents an error communicating with Kin server, error code might be:
    - `SERVICE_ERROR` - Some internal server error happened.
    - `NETWORK_ERROR` - Error accessing the server.
    - `TIMEOUT_ERROR` - Timeout occured.
    - `USER_NOT_FOUND_ERROR` - Operation required with a non existing user.
    - `USER_NOT_ACTIVATED` - Operation required with a non activated user (User that did not launched the marketplace yet)
* `BlockchainException` - Represents an error originated with Kin blockchain, error code might be:
    - `ACCOUNT_CREATION_FAILED` - Error trying to create a new account for this user.
    - `ACCOUNT_NOT_FOUND` - Blockchain operation required on non-existing account.
    - `ACCOUNT_ACTIVATION_FAILED` - Activating an account failed.
    - `INSUFFICIENT_KIN` - Not enough Kin balance to perform the operation.
    - `TRANSACTION_FAILED` - Transaction has failed.

## License

The `kin-devplatform-android` library is licensed under the MIT license.
