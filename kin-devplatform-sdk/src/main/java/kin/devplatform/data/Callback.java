package kin.devplatform.data;

public interface Callback<T, E> {

	void onResponse(T response);

	void onFailure(E error);
}
