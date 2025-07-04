package com.xbenjii.RNBraintreeDropIn;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.DropInClient;
import com.braintreepayments.api.DropInListener;
import com.braintreepayments.api.DropInPaymentMethod;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.UserCanceledException;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.GooglePayCardNonce;
import com.braintreepayments.api.PostalAddress;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Promise;
import com.braintreepayments.api.DropInRequest;
import com.braintreepayments.api.DropInResult;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.ThreeDSecureInfo;
import com.braintreepayments.api.GooglePayRequest;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import java.util.Objects;

public class RNBraintreeDropInModule extends ReactContextBaseJavaModule {
  private boolean isVerifyingThreeDSecure = false;
  private static DropInClient dropInClient = null;
  private static String clientToken = null;
  public static final int GPAY_BILLING_ADDRESS_FORMAT_FULL = 1;

  public static void initDropInClient(FragmentActivity activity) {
    dropInClient = new DropInClient(activity, callback -> {
      if (clientToken != null) {
        callback.onSuccess(clientToken);
      } else {
        callback.onFailure(new Exception("Client token is null"));
      }
    });
  }

  public RNBraintreeDropInModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @ReactMethod
  private void collectDeviceData(final String clientToken, final Promise promise) {
    if (clientToken == null) {
      promise.reject("NO_CLIENT_TOKEN", "You must provide a client token");
      return;
    }
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject("NO_ACTIVITY", "There is no current activity");
      return;
    }

    BraintreeClient braintreeClient = new BraintreeClient(currentActivity, clientToken);
    DataCollector dataCollector = new DataCollector(braintreeClient);

    dataCollector.collectDeviceData(currentActivity, (deviceData, error) -> {
      String data = deviceData;
      if (data == null) {
        data = "";
      }
      WritableMap jsResult = Arguments.createMap();
      jsResult.putString("deviceData", data);
      promise.resolve(jsResult);
    });
  }

  @ReactMethod
  public void show(final ReadableMap options, final Promise promise) {
    isVerifyingThreeDSecure = false;

    if (!options.hasKey("clientToken")) {
      promise.reject("NO_CLIENT_TOKEN", "You must provide a client token");
      return;
    }

    FragmentActivity currentActivity = (FragmentActivity) getCurrentActivity();
    if (currentActivity == null) {
      promise.reject("NO_ACTIVITY", "There is no current activity");
      return;
    }

    DropInRequest dropInRequest = new DropInRequest();

    if(options.hasKey("vaultManager")) {
      dropInRequest.setVaultManagerEnabled(options.getBoolean("vaultManager"));
    }

    if(options.hasKey("vaultCardDefaultValue")) {
      dropInRequest.setVaultCardDefaultValue(options.getBoolean("vaultCardDefaultValue"));
    }

    if(options.hasKey("googlePay") && options.getBoolean("googlePay")){
      GooglePayRequest googlePayRequest = new GooglePayRequest();
      googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
          .setTotalPrice(Objects.requireNonNull(options.getString("orderTotal")))
          .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
          .setCurrencyCode(Objects.requireNonNull(options.getString("currencyCode")))
          .build());
      googlePayRequest.setBillingAddressRequired(true);
      googlePayRequest.setEmailRequired(true);
      googlePayRequest.setBillingAddressFormat(GPAY_BILLING_ADDRESS_FORMAT_FULL);
      googlePayRequest.setGoogleMerchantId(options.getString("googlePayMerchantId"));

      dropInRequest.setGooglePayDisabled(false);
      dropInRequest.setGooglePayRequest(googlePayRequest);
    }else{
        dropInRequest.setGooglePayDisabled(true);
    }

    if(options.hasKey("cardDisabled")) {
      dropInRequest.setCardDisabled(options.getBoolean("cardDisabled"));
    }

    if (options.hasKey("threeDSecure")) {
      final ReadableMap threeDSecureOptions = options.getMap("threeDSecure");
      if (threeDSecureOptions == null || !threeDSecureOptions.hasKey("amount")) {
        promise.reject("NO_3DS_AMOUNT", "You must provide an amount for 3D Secure");
        return;
      }

      isVerifyingThreeDSecure = true;

      String amount = String.valueOf(threeDSecureOptions.getDouble("amount"));

      ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
      threeDSecureRequest.setAmount(amount);
      threeDSecureRequest.setVersionRequested(ThreeDSecureRequest.VERSION_2);

      if (threeDSecureOptions.hasKey("email")) {
        threeDSecureRequest.setEmail(threeDSecureOptions.getString("email"));
      }

      if(threeDSecureOptions.hasKey("billingAddress")) {
        final ReadableMap threeDSecureBillingAddress = threeDSecureOptions.getMap("billingAddress");
        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();

        if(threeDSecureBillingAddress.hasKey("givenName")) {
          billingAddress.setGivenName(threeDSecureBillingAddress.getString("givenName"));
        }

        if(threeDSecureBillingAddress.hasKey("surname")) {
          billingAddress.setGivenName(threeDSecureBillingAddress.getString("surname"));
        }

        if(threeDSecureBillingAddress.hasKey("streetAddress")) {
          billingAddress.setGivenName(threeDSecureBillingAddress.getString("streetAddress"));
        }

        if(threeDSecureBillingAddress.hasKey("extendedAddress")) {
          billingAddress.setGivenName(threeDSecureBillingAddress.getString("extendedAddress"));
        }

        if(threeDSecureBillingAddress.hasKey("locality")) {
          billingAddress.setGivenName(threeDSecureBillingAddress.getString("locality"));
        }

        if(threeDSecureBillingAddress.hasKey("region")) {
          billingAddress.setGivenName(threeDSecureBillingAddress.getString("region"));
        }

        if(threeDSecureBillingAddress.hasKey("countryCodeAlpha2")) {
          billingAddress.setGivenName(threeDSecureBillingAddress.getString("countryCodeAlpha2"));
        }

        if(threeDSecureBillingAddress.hasKey("postalCode")) {
          billingAddress.setGivenName(threeDSecureBillingAddress.getString("postalCode"));
        }

        threeDSecureRequest.setBillingAddress(billingAddress);
      }

      dropInRequest.setThreeDSecureRequest(threeDSecureRequest);
    }

    dropInRequest.setPayPalDisabled(!options.hasKey("payPal") || !options.getBoolean("payPal"));

    clientToken = options.getString("clientToken");

    if (dropInClient == null) {
      promise.reject(
        "DROP_IN_CLIENT_UNINITIALIZED",
        "Did you forget to call RNBraintreeDropInModule.initDropInClient(this) in MainActivity.onCreate?"
      );
      return;
    }
    dropInClient.setListener(new DropInListener() {
      @Override
      public void onDropInSuccess(@NonNull DropInResult dropInResult) {
        PaymentMethodNonce paymentMethodNonce = dropInResult.getPaymentMethodNonce();

        if (isVerifyingThreeDSecure && paymentMethodNonce instanceof CardNonce) {
          CardNonce cardNonce = (CardNonce) paymentMethodNonce;
          ThreeDSecureInfo threeDSecureInfo = cardNonce.getThreeDSecureInfo();
          if (!threeDSecureInfo.isLiabilityShiftPossible()) {
            promise.reject("3DSECURE_NOT_ABLE_TO_SHIFT_LIABILITY", "3D Secure liability cannot be shifted");
          } else if (!threeDSecureInfo.isLiabilityShifted()) {
            promise.reject("3DSECURE_LIABILITY_NOT_SHIFTED", "3D Secure liability was not shifted");
          } else {
            resolvePayment(dropInResult, promise);
          }
        } else {
          resolvePayment(dropInResult, promise);
        }
      }

      @Override
      public void onDropInFailure(@NonNull Exception exception) {
        if (exception instanceof UserCanceledException) {
          promise.reject("USER_CANCELLATION", "The user cancelled");
        } else {
          promise.reject(exception.getMessage(), exception.getMessage());
        }
      }
    });
    dropInClient.launchDropIn(dropInRequest);
  }

  @ReactMethod
  public void getDeviceData(final String clientToken, final Promise promise) {
    BraintreeClient braintreeClient = new BraintreeClient(getCurrentActivity(), clientToken);
    DataCollector dataCollector = new DataCollector(braintreeClient);
    dataCollector.collectDeviceData(getCurrentActivity(), (deviceData, error) -> {
      if (error != null) {
        promise.reject("ERROR", "Error collecting device data");
      } else {
        promise.resolve(deviceData);
      }
    });
  }

  @ReactMethod
  public void fetchMostRecentPaymentMethod(final String clientToken, final Promise promise) {
    FragmentActivity currentActivity = (FragmentActivity) getCurrentActivity();

    if (currentActivity == null) {
      promise.reject("NO_ACTIVITY", "There is no current activity");
      return;
    }

    if (dropInClient == null) {
      promise.reject(
        "DROP_IN_CLIENT_UNINITIALIZED",
        "Did you forget to call RNBraintreeDropInModule.initDropInClient(this) in MainActivity.onCreate?"
      );
      return;
    }

    RNBraintreeDropInModule.clientToken = clientToken;

    dropInClient.fetchMostRecentPaymentMethod(currentActivity, (dropInResult, error) -> {
      if (error != null) {
        promise.reject(error.getMessage(), error.getMessage());
      } else if (dropInResult == null) {
        promise.reject("NO_DROP_IN_RESULT", "dropInResult is null");
      } else {
        resolvePayment(dropInResult, promise);
      }
    });
  }

  @ReactMethod
  public void tokenizeCard(final String clientToken, final ReadableMap cardInfo, final Promise promise) {
    if (clientToken == null) {
      promise.reject("NO_CLIENT_TOKEN", "You must provide a client token");
      return;
    }

    if (
      !cardInfo.hasKey("number") ||
      !cardInfo.hasKey("expirationMonth") ||
      !cardInfo.hasKey("expirationYear") ||
      !cardInfo.hasKey("cvv") ||
      !cardInfo.hasKey("postalCode")
    ) {
      promise.reject("INVALID_CARD_INFO", "Invalid card info");
      return;
    }

    Activity currentActivity = getCurrentActivity();

    if (currentActivity == null) {
      promise.reject("NO_ACTIVITY", "There is no current activity");
      return;
    }

    BraintreeClient braintreeClient = new BraintreeClient(getCurrentActivity(), clientToken);
    CardClient cardClient = new CardClient(braintreeClient);

    Card card = new Card();
    card.setNumber(cardInfo.getString("number"));
    card.setExpirationMonth(cardInfo.getString("expirationMonth"));
    card.setExpirationYear(cardInfo.getString("expirationYear"));
    card.setCvv(cardInfo.getString("cvv"));
    card.setPostalCode(cardInfo.getString("postalCode"));

    cardClient.tokenize(card, (cardNonce, error) -> {
      if (error != null) {
        promise.reject(error.getMessage(), error.getMessage());
      } else if (cardNonce == null) {
        promise.reject("NO_CARD_NONCE", "Card nonce is null");
      } else {
        promise.resolve(cardNonce.getString());
      }
    });
  }

  private void resolvePayment(DropInResult dropInResult, Promise promise) {
    String deviceData = dropInResult.getDeviceData();
    PaymentMethodNonce paymentMethodNonce = dropInResult.getPaymentMethodNonce();

    WritableMap jsResult = Arguments.createMap();

    if (paymentMethodNonce == null) {
      promise.resolve(null);
      return;
    }

    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject("NO_ACTIVITY", "There is no current activity");
      return;
    }

    DropInPaymentMethod dropInPaymentMethod = dropInResult.getPaymentMethodType();
    if (dropInPaymentMethod == null) {
      promise.reject("NO_PAYMENT_METHOD", "There is no payment method");
      return;
    }

    PostalAddress billingAddress = null;
    if(paymentMethodNonce instanceof PayPalAccountNonce) {
      PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce) paymentMethodNonce;
      jsResult.putString("firstName", payPalAccountNonce.getFirstName());
      jsResult.putString("lastName", payPalAccountNonce.getLastName());
      jsResult.putString("email", payPalAccountNonce.getEmail());
      billingAddress = payPalAccountNonce.getBillingAddress();
    } else if (paymentMethodNonce instanceof GooglePayCardNonce) {
      GooglePayCardNonce googlePayCardNonce = (GooglePayCardNonce) paymentMethodNonce;
      billingAddress = googlePayCardNonce.getBillingAddress();
      jsResult.putString("email", googlePayCardNonce.getEmail());
      if(billingAddress != null) {
        String name = billingAddress.getRecipientName();
        if(!name.equals("")) {
          short lastIndexOfSpace = (short) name.lastIndexOf(" ");
          if(lastIndexOfSpace == -1) {
            jsResult.putString("firstName", name.trim());
          } else {
            jsResult.putString("firstName", name.substring(0, lastIndexOfSpace));
            jsResult.putString("lastName", name.substring(lastIndexOfSpace));
          }
        }
      }
    }
    if(billingAddress != null) {
      jsResult.putString("addressLine1", billingAddress.getStreetAddress());
      jsResult.putString("addressLine2", billingAddress.getExtendedAddress());
      jsResult.putString("city", billingAddress.getLocality());
      jsResult.putString("state", billingAddress.getRegion());
      jsResult.putString("country", billingAddress.getCountryCodeAlpha2());
      jsResult.putString("zip1", billingAddress.getPostalCode());
    }

    jsResult.putString("nonce", paymentMethodNonce.getString());
    jsResult.putString("type", currentActivity.getString(dropInPaymentMethod.getLocalizedName()));
    jsResult.putString("description", dropInResult.getPaymentDescription());
    jsResult.putBoolean("isDefault", paymentMethodNonce.isDefault());
    jsResult.putString("deviceData", deviceData);

    promise.resolve(jsResult);
  }

  @NonNull
  @Override
  public String getName() {
    return "RNBraintreeDropIn";
  }
}
